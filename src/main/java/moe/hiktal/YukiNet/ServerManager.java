package moe.hiktal.YukiNet;

import moe.hiktal.YukiNet.classes.Server;
import oracle.jdbc.logging.annotations.Log;
import org.bukkit.configuration.file.YamlConfiguration;
import org.joda.time.Duration;
import org.joda.time.Period;
import org.joda.time.format.DateTimeFormat;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.PeriodFormat;
import org.joda.time.format.PeriodFormatter;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;

public class ServerManager {
    private static boolean isShuttingDown;
    public static Server proxy;
    public static List<Server> staticServers = new ArrayList<>();
    public static List<Server> dynamicServers = new ArrayList<>();
    public final static List<Server> stoppedServers = new ArrayList<>();

    public static List<Server> GetAllServers() {
        List<Server> ret = new ArrayList<>();
        ret.addAll(staticServers);
        ret.addAll(dynamicServers);
        return ret;
    }

    public static void StartBoot() throws IOException {
        WalkAndCopyServers(Main.cwd);
        RewriteBungeecordConfig();
    }

    public static void WalkAndCopyServers(File cwd) throws IOException {
        Logger.Info("Copying templates...");

        File[] toCopy = Objects.requireNonNull(new File(cwd + "/template").listFiles());

        int nextPort = Main.cfg.getInt("portMin", 12000);
        for (File dir : toCopy) {
            if (!dir.isDirectory() || dir.getName().startsWith(".")) continue;
            if (!new File(dir + "/.yuki.yml").exists()) continue;

            String[] args = dir.getName().split("\\.");

            Logger.Info(String.format("Copying template %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            int amount = cfg.getInt("count", 1);
            String id = cfg.getString("id");//
            for (int i = 0; i < amount; i++) {
                // allocate a port
                int port = Util.NextUsablePort(nextPort);
                Server server = new Server(id, i + 1, port, false);
                File targetDir = new File(cwd + String.format("/live/%s/%s", id, server.getId()));
                CollectAndCopyFiles(id, cwd, targetDir);
                dynamicServers.add(server);
                server.SetCwd(cwd);

                nextPort = port + 1;
            }

        }

    }

    public static boolean AllowingServerReboot() {
        return !isShuttingDown && Main.cfg.getBoolean("restartServersOnStop");
    }

    public static boolean AllowsServerAutoRestart(Server server) {
        return !isShuttingDown && !stoppedServers.contains(server) && GetAllServers().contains(server) && Main.cfg.getBoolean("restartServersOnStop");
    }

    private static void RewriteBungeecordConfig() throws IOException{
        Logger.Info("Rewriting BungeeCord config...");

        File cwd = new File(Main.cwd + Main.cfg.getString("proxyDir"));
        if (!cwd.exists()) {
            Logger.Error(String.format("Proxy directory (%s) not found! Aborting startup.", cwd));
            return;
        }
        YamlConfiguration yukiCfg = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));

        // get the config file
        File configFile = new File(cwd + yukiCfg.getString("configPath"));
        if (!cwd.exists()) {
            Logger.Error(String.format("Proxy configuration (%s) not found! Aborting startup.", configFile));
            return;
        }

        // back up config file
        File configBackup = new File(cwd + "/config-backup.yml");
        if (!configBackup.exists()) {
            Logger.Info("A backup of your config file has been made.");
            Files.copy(configFile.toPath(), configBackup.toPath());
        }

        // read
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("servers", null);

        List<String> priorities = new ArrayList<>();
        for (Server server : GetAllServers()) {
            WriteOneServer(config, server);
            if (server.getConfig().getBoolean("writePriorities")) priorities.add(server.getId());
        }
        LinkedHashMap<String, Object> listener = (LinkedHashMap<String, Object>) config.getList("listeners").get(0);
        listener.put("priorities", priorities);
        List<LinkedHashMap<String, Object>> listeners = new ArrayList<>();
        listeners.add(listener);
        config.set("listeners", listeners);

        config.save(configFile);

        Logger.Info("Done rewriting Bungeecord. Starting proxy...");

        proxy = new Server("__proxy", 0, 0, true);
        proxy.SetProxy(cwd);
        proxy.Start(suc -> {
            if (suc) {
                Logger.Info("Proxy start success. Starting servers...");
                try {
                    StartAllServers();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

        // shutdown
        Runtime.getRuntime().addShutdownHook(new Thread() {
            @Override
            public void run() {
                try {
                    Shutdown();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    public static void StartAllServers() throws IOException{
        final List<Server> servers = GetAllServers();
        final DecimalFormat formatter = new DecimalFormat("#.##");
        int delay = Main.cfg.getInt("serverStartInterval", 30000);
        final Timer timer = new Timer();

        Logger.Info("Starting 1 server every %s seconds.".formatted(formatter.format((double)delay / 1000.0)));

        Logger.Info("Estimated time: %s".formatted(Util.FormatMilliseconds((long)delay * servers.size())));

        timer.schedule(new TimerTask() {
            int count = 0;

            @Override
            public void run() {
                count++;
                Logger.Info("Starting servers %s/%s (%s%%)".formatted(count, servers.size(), formatter.format((double)count / servers.size() * 100.0)));
                Logger.Info("Estimated time remaining: %s".formatted(Util.FormatMilliseconds((long)delay * (servers.size() - count))));

                try {
                    servers.get(count - 1).Start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (count >= servers.size()) {
                    Logger.Info("All servers have been started.");
                    timer.cancel();
                }
            }

        }, 0, delay);
    }

    public static void RestartServer(Server server) throws IOException{
        dynamicServers.remove(server);
        server.Interrupt(true);

        Server newServer = server.Clone();
        dynamicServers.add(newServer);
        newServer.Start();
    }

    private static void WriteOneServer(YamlConfiguration cfg, Server server) {
        String id = server.getId();
        YamlConfiguration sec = new YamlConfiguration();
        sec.set("motd", "a YukiNet server");
        sec.set("address", String.format("%s:%s", server.getHost(), server.getPort()));
        sec.set("restricted", false);
        cfg.set(String.format("servers.%s", id), sec);
    }

    private static void CollectAndCopyFiles(String groupId, File cwd, File target) throws IOException{
        if (!target.exists()) Files.createDirectories(target.toPath());

        // global files
        File globalDir = new File(cwd + "/template/.global");
        if (globalDir.exists()) FileUtil.RecursiveCopy(globalDir, target);

        // subgroup files
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(target + "/.yuki.yml"));
        List<String> copyBefore = cfg.getStringList("copyTree");
        for (String path : copyBefore) {
            File dir = new File(cwd + "/template/" + path);
            if (!dir.exists()) {
                Logger.Warning("Subfolder %s not found! Skipping.".formatted(dir.getAbsolutePath()));
                continue;
            }

            FileUtil.RecursiveCopy(dir, target);
        }

        // group files
        System.out.println(new File(cwd + "/template/" + groupId).getAbsolutePath());
        FileUtil.RecursiveCopy(new File(cwd + "/template/" + groupId), target);

    }

    private static void Shutdown() throws IOException{
        Logger.Info("Gracefully shutting down servers...");
        isShuttingDown = true;
        proxy.Interrupt();

        for (Server server : staticServers) server.Interrupt();
        for (Server server : dynamicServers) server.Interrupt();

        Logger.Info("Allowing 10 seconds for servers to shut down themselves before forcefully shutting down - DO NOT SEND ^C");
        Logger.Info("DO NOT SEND ^C!!!!!!");

        try {
            int counter = 0;
            while (counter < 10000) {
                counter += 100;
                Thread.sleep(100);
                if (ServerManager.GetAllServers().stream().noneMatch(Server::IsAlive)) break;
            }

            List<Server> survivers = ServerManager.GetAllServers().stream().filter(Server::IsAlive).toList();
            if (survivers.size() > 0) {
                Logger.Warning("Killing %s survivers...".formatted(survivers.size()));
                for (Server server : survivers) {
                    server.Interrupt(true);
                }
            }

        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        Logger.Info("Shutdown done, goodbye.");
    }

}
