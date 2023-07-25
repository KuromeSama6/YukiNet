package moe.hiktal.YukiNet.classes;

import moe.hiktal.YukiNet.*;
import moe.hiktal.YukiNet.enums.EServerStatus;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.Timer;
import java.util.TimerTask;
import java.util.function.Consumer;

public class LocalServer extends Server{
    public LocalServer(String groupId, int groupIndex, int port, boolean isStatic) {
        this.groupId = groupId;
        this.groupIndex = groupIndex;
        this.port = port;//
        this.isStatic = isStatic;
        this.id = isStatic ? groupId : String.format("%s%s", groupId, groupIndex);
    }

    @Override
    public Server Clone() {
        Server ret = new LocalServer(groupId, groupIndex, port, isStatic);
        ret.cwd = cwd;
        ret.config = config;
        return ret;
    }

    @Override
    public void SetCwd(File parent) {
        if (cwd != null) return;
        cwd = new File(parent + String.format("/live/%s/%s", groupId, id));
        config = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));
    }

    @Override
    public void SetProxy(File parent) {
        if (cwd != null) return;
        cwd = parent;
        config = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));
    }

    @Override
    public String getScreenSessionName() {
        return String.format("%s__yukinet%s", id, isStatic ? "static" : "");
    }

    @Override
    public YamlConfiguration GetInfoDump() {
        YamlConfiguration ret = new YamlConfiguration();

        ret.set("DO-NOT-CHANGE-THIS-FILE", "DO-NOT-CHANGE-THIS-FILE");
        ret.set("CHANGES-ARE-NOT-SAVED", "This file is regenerated each time the server starts. Any data is not saved.");

        ret.set("generation-time", DateTime.now().toLocalDateTime().toString());
        ret.set("generation-timestamp", DateTime.now().getMillis());
        ret.set("server-id", id);
        ret.set("group-id", groupId);
        ret.set("port", port);
        ret.set("is-static", isStatic);

        ret.set("global-cfg", Main.cfg);

        return ret;
    }

    @Override
    public void Start() throws IOException {Start(null);}

    @Override
    public void Start(Consumer<Boolean> callback) throws IOException {
        ServerManager.stoppedServers.remove(this);

        // info dump
        GetInfoDump().save(new File(cwd + "/.yuki-info.yml"));

        String cmd = config.getString("cmd", "java");
        String jarFile = config.getString("jarFile", "spigot.jar");
        String args = String.join(" ", config.getStringList("args"));

        try {
            if (Config.IsWorkingLinux()) {
                String statement = String.format("screen -mdS %s /bin/sh -c 'cd %s && %s -server %s -jar %s --port %S'",
                    getScreenSessionName(), cwd.getAbsolutePath(), cmd, args, jarFile, port
                );
//                Logger.Info(statement);
                Logger.Info(String.format("Screen session created for %s.", id));
                proc = new ProcessBuilder(
                        "/bin/sh", "-c",
                        statement
                ).directory(cwd).start();

            } else {
                String statement = String.format("%s %s -jar %s --port %s", cmd, args, jarFile, port);
                proc = new ProcessBuilder(statement.split(" ")).directory(cwd).start();
            }

            if (SystemUtils.IS_OS_WINDOWS) {
                status = EServerStatus.RUNNING;
                Logger.Info(String.format("RUNNING | %s | PID %s | PORT %s", id, getPid(), port));
                pid = getPid();
                procHandle = proc.toHandle();
                if (callback != null) callback.accept(true);
            } else {
                status = EServerStatus.SCREEN_READY;
                Logger.Info(String.format("SCREEN START | %s | PID - (%s) | PORT %s", id, getPid(), port));
            }
            StartListener(suc -> {if (callback != null) callback.accept(suc);});

        } catch (IOException e) {
            Logger.Error(String.format("Unable to start server %s: %s. Call /start/<REGEX> later to restart it.", id, e.getMessage()));
            if (callback != null) callback.accept(false);
            Stop();
        }

    }

    @Override
    protected void StartListener(Consumer<Boolean> callback) throws IOException {
        procListener = new Thread(() -> {
            while (proc == null) Thread.onSpinWait();
            while (proc.isAlive()) Thread.onSpinWait();

            sw.stop();
            try {
                if (SystemUtils.IS_OS_WINDOWS) {
                    Logger.Warning(String.format("Server %s stopped after %s", id, sw.toString()));
                    Interrupt();
                } else {
                    // sever is actually up - grab the screen session pid
                    int pid = Util.GrabScreenSessionId(getScreenSessionName());
                    if (pid == -1) {
                        Logger.Error(String.format("Unable to grab screen session PID for server %s", id));
                        Interrupt();
                    } else {
                        this.pid = pid;
                        sw.reset();
                        sw.start();
                        status = EServerStatus.RUNNING;
                        Logger.Info(String.format("RUNNING | %s | PID %s | PORT %s", id, pid, port));
                        callback.accept(true);

                        while (Util.GrabProcHandle(pid) == null) Thread.onSpinWait();

                        // start keepalive
                        keepAliveTimer = new Timer();
                        keepAliveTimer.schedule(new TimerTask() {
                            final ProcessHandle procHandle = Util.GrabProcHandle(pid);
                            @Override
                            public void run() {
                                LocalServer.this.procHandle = procHandle;
                                if (procHandle == null || !procHandle.isAlive()) {
                                    Logger.Warning(String.format("Server %s stopped after %s", id, sw.toString()));
                                    keepAliveTimer.cancel();
                                    try {
                                        Interrupt();
                                    } catch (IOException e) {
                                        throw new RuntimeException(e);
                                    }
                                }
                            }
                        }, 0, 1000);
                    }
                }
            } catch (IOException e) {
                callback.accept(false);
                if (!status.IsRunning()) {
                    try {
                        Stop();
                    } catch (IOException ex) {
                        throw new RuntimeException(ex);
                    }
                }
                throw new RuntimeException(e);
            }
            proc = null;
            procHandle = null;
        });

        sw.reset();
        sw.start();
        procListener.start();
    }

    @Override
    public boolean IsAlive() {
        return procHandle != null && procHandle.isAlive();
    }

    @Override
    public void Stop() throws IOException {
        ServerManager.stoppedServers.add(this);
        if (status.IsRunning()) Interrupt();
    }

    @Override
    public void Interrupt() throws IOException {Interrupt(false);}

    @Override
    public void Interrupt(boolean force) throws IOException {
        for (int i = 0; i <(force ? 2 : 1); i++) {
            if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec(new String[] {"taskkill", "/F", "/PID", Long.toString(proc.pid())});
            else Runtime.getRuntime().exec(new String[] {
                    "screen", "-S", getScreenSessionName(), "-X", "stuff", "\"^C\""
            });
        }

        status = EServerStatus.STOPPED;

        if (ServerManager.AllowsServerAutoRestart(this)) {
            Logger.Info("Server %s restarting in 10 seconds".formatted(id));
            new Timer().schedule(new TimerTask() {
                @Override
                public void run() {
                    try {
                        Logger.Info("Server %s restarting".formatted(id));
                        Start();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }, 10000);
        }
    }

}
