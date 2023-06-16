package moe.hiktal.YukiNet.classes;

import moe.hiktal.YukiNet.Config;
import moe.hiktal.YukiNet.Logger;
import moe.hiktal.YukiNet.enums.EServerStatus;
import oracle.jdbc.logging.annotations.Log;
import org.apache.commons.lang.SystemUtils;
import org.apache.commons.lang.time.StopWatch;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.concurrent.CompletableFuture;

public class Server {
    private final String groupId;
    private final int groupIndex;
    private final String id;
    private volatile Process proc;
    private Thread procListener;
    private YamlConfiguration config;
    private EServerStatus status;
    private StopWatch sw = new StopWatch();
    private File cwd;
    private final int port;

    public Server(String groupId, int groupIndex, int port, File parent) {
        this.groupId = groupId;
        this.groupIndex = groupIndex;
        this.port = port;//
        this.id = String.format("%s%s", groupId, groupIndex);

        Logger.Info(String.format("Server %s initialized on port %s", id, port));
    }

    public void SetCwd(File parent) {
        if (cwd != null) return;
        cwd = new File(parent + String.format("/live/%s/%s", groupId, id));
        config = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));
    }

    public EServerStatus getStatus() {return status;}

    public String getId() {return id;}

    public int getPid() {return proc == null ? -1 : (int)proc.pid();} //

    public String getGroupId() {return groupId;}

    public int getPort() {return port;}

    public File getWorkingDirectory() {return cwd;}

    public boolean IsRunning() {return status.IsRunning();}

    public Process Start() throws IOException {
        String cmd = config.getString("cmd", "java");
        String jarFile = config.getString("jarFile", "spigot.jar");
        String args = String.join(" ", config.getStringList("args"));

        try {
            if (Config.IsWorkingLinux()) {
                String statement = String.format("screen -mdS %s /bin/sh -c 'cd %s && %s -server %s -jar %s --port %S'",
                    id, cwd.getAbsolutePath(), cmd, args, jarFile, port
                );
                System.out.println(statement);
                Logger.Info(String.format("Screen session created for %s.", id));
                proc = new ProcessBuilder(
                        "/bin/sh", "-c",
                        statement
                ).directory(cwd).start();

            } else {
                String statement = String.format("%s %s -jar %s --port %s", cmd, args, jarFile, port);
                proc = new ProcessBuilder(statement.split(" ")).directory(cwd).start();
            }

            status = EServerStatus.RUNNING;
            Logger.Info(String.format("RUNNING | %s | PID %s | PORT %s", id, getPid(), port));
            StartListener();

        } catch (IOException e) {
            Logger.Error(String.format("Unable to start server %s: %s", id, e.getMessage()));
        }

        return proc;
    }

    private void StartListener() throws IOException {
        procListener = new Thread(() -> {
            while (proc == null) Thread.onSpinWait();
            while (proc.isAlive()) Thread.onSpinWait();

            sw.stop();
            Logger.Warning(String.format("Server %s stopped after %s", id, sw.toString()));
            try {
                if (SystemUtils.IS_OS_WINDOWS) Runtime.getRuntime().exec(new String[] {"taskkill", "/F", "/PID", Long.toString(proc.pid())});
                else Runtime.getRuntime().exec(new String[] {"kill", Long.toString(proc.pid())});
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
            proc = null;
        });

        sw.reset();
        sw.start();
        procListener.start();
    }

}
