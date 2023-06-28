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

public class Server {
    private final String groupId;
    private final int groupIndex;
    private final String id;
    private final boolean isStatic;
    private volatile Process proc;
    private Thread procListener;
    private YamlConfiguration config;
    private EServerStatus status;
    private StopWatch sw = new StopWatch();
    private File cwd;
    private final int port;
    private int pid;
    private Timer keepAliveTimer = new Timer();
    private ProcessHandle procHandle;

    public Server(String groupId, int groupIndex, int port, boolean isStatic) {
        this.groupId = groupId;
        this.groupIndex = groupIndex;
        this.port = port;//
        this.isStatic = isStatic;
        this.id = isStatic ? groupId : String.format("%s%s", groupId, groupIndex);
    }

    public Server Clone() {
        Server ret = new Server(groupId, groupIndex, port, isStatic);
        ret.cwd = cwd;
        ret.config = config;
        return ret;
    }

    public void SetCwd(File parent) {
        if (cwd != null) return;
        cwd = new File(parent + String.format("/live/%s/%s", groupId, id));
        config = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));
    }

    public void SetProxy(File parent) {
        if (cwd != null) return;
        cwd = parent;
        config = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));
    }

    public EServerStatus getStatus() {return status;}

    public String getId() {return id;}

    public int getPid() {return proc == null ? -1 : (int)proc.pid();} //

    public String getGroupId() {return groupId;}

    public int getPort() {return port;}

    public boolean getIsStatic() {return isStatic;}

    public YamlConfiguration getConfig() {return config;}

    public String getHost() {
        return "127.0.0.1";
    }

    public File getWorkingDirectory() {return cwd;}

    public boolean IsRunning() {return status.IsRunning();}

    public String getScreenSessionName() {
        return String.format("%s__yukinet%s", id, isStatic ? "static" : "");
    }

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

    public void Start() throws IOException {Start(null);}

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

    private void StartListener(Consumer<Boolean> callback) throws IOException {
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
                                Server.this.procHandle = procHandle;
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

    public boolean IsAlive() {
        return procHandle != null && procHandle.isAlive();
    }

    public void Stop() throws IOException {
        ServerManager.stoppedServers.add(this);
        if (status.IsRunning()) Interrupt();
    }

    public void Interrupt() throws IOException {Interrupt(false);}

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
