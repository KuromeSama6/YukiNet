package moe.hiktal.yukinet.server.impl;

import moe.hiktal.yukinet.*;
import moe.hiktal.yukinet.server.Server;
import moe.hiktal.yukinet.enums.EServerStatus;
import moe.hiktal.yukinet.server.Config;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.util.FileUtil;
import moe.hiktal.yukinet.util.Util;
import org.apache.commons.lang.SystemUtils;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joda.time.DateTime;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.function.Consumer;

public class LocalServer extends Server {
    private boolean isFirstStart = true;

    public LocalServer(String groupId, int groupIndex, int port, boolean isStatic, File cwd, YamlConfiguration cfg) {
        this(groupId, groupIndex, port, isStatic);
        this.cwd = cwd;
        this.config = cfg;
    }

    public LocalServer(String groupId, int groupIndex, int port, boolean isStatic) {
        this.groupId = groupId;
        this.groupIndex = groupIndex;
        this.port = port;//
        this.isStatic = isStatic;
        this.id = isStatic ? groupId : String.format("%s%s", groupId, groupIndex);
    }

    @Override
    public Server Clone() {
        Server ret = new LocalServer(groupId, groupIndex, port, isStatic, cwd, config);
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
    public String GetHost() {
        return "127.0.0.1";
    }

    @Override
    public String GetScreenSessionName() {
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

        ret.set("global-cfg", YukiNet.cfg);

        return ret;
    }

    @Override
    public void Start() throws IOException {Start(null);}

    @Override
    public void Start(Consumer<Boolean> callback) throws IOException {
        ServerManager.stoppedServers.remove(this);

        if (!isFirstStart) {
            // repull from template
            ServerManager.CollectAndCopyFiles(groupId, YukiNet.cwd, cwd);
        }
        isFirstStart = false;

        // random copy
        if (config.getBoolean("randomCopy", false)) {
            File randomDir = new File(YukiNet.cwd + String.format("/template/%s/.random", groupId));
            if (!randomDir.exists()) YukiNet.getLogger().warn("The .random directory does not exist under %s!".formatted(randomDir));
            else {
                List<File> files = Arrays.stream(Objects.requireNonNull(randomDir.listFiles()))
                        .filter(File::isDirectory)
                        .toList();

                File file = files.get(new Random().nextInt(0, files.size()));
                FileUtil.RecursiveCopy(file, cwd);
                YukiNet.getLogger().info("Copied random directory for %s".formatted(getId()));
            }
        }

        // info dump
        GetInfoDump().save(new File(cwd + "/.yuki-info.yml"));

        String cmd = config.getString("cmd", "java");
        String jarFile = config.getString("jarFile", "spigot.jar");
        String args = String.join(" ", config.getStringList("args"));

        try {
            if (Config.IsWorkingLinux()) {
                String statement = String.format("screen -mdS %s /bin/sh -c 'cd %s && %s -server %s -jar %s --port %S'",
                    GetScreenSessionName(), cwd.getAbsolutePath(), cmd, args, jarFile, port
                );
//                YukiNet.getLogger().info(statement);
                YukiNet.getLogger().info(String.format("Screen session created for %s.", id));
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
                YukiNet.getLogger().info(String.format("RUNNING | %s | PID %s | PORT %s", id, getPid(), port));
                pid = getPid();
                procHandle = proc.toHandle();
                if (callback != null) callback.accept(true);
            } else {
                status = EServerStatus.SCREEN_READY;
                YukiNet.getLogger().info(String.format("SCREEN START | %s | PID - (%s) | PORT %s", id, getPid(), port));
            }
            StartListener(suc -> {if (callback != null) callback.accept(suc);});

        } catch (IOException e) {
            YukiNet.getLogger().error(String.format("Unable to start server %s: %s. Call /start/<REGEX> later to restart it.", id, e.getMessage()));
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
                    YukiNet.getLogger().warn(String.format("Server %s stopped after %s", id, sw.toString()));
                    Interrupt();
                } else {
                    // sever is actually up - grab the screen session pid
                    int pid = Util.GrabScreenSessionId(GetScreenSessionName());
                    if (pid == -1) {
                        YukiNet.getLogger().error(String.format("Unable to grab screen session PID for server %s", id));
                        Interrupt();
                    } else {
                        this.pid = pid;
                        sw.reset();
                        sw.start();
                        status = EServerStatus.RUNNING;
                        YukiNet.getLogger().info(String.format("RUNNING | %s | PID %s | PORT %s", id, pid, port));
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
                                    YukiNet.getLogger().warn(String.format("Server %s stopped after %s", id, sw.toString()));
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
                    "screen", "-S", GetScreenSessionName(), "-X", "stuff", "\"^C\""
            });
        }

        status = EServerStatus.STOPPED;

        if (ServerManager.AllowsServerAutoRestart(this)) {
            ServerManager.restartQueue.add(this);
        }

        if (force && !SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec(new String[] {"kill", "-SIGKILL", "%s".formatted(getPid())});

    }

}
