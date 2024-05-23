package moe.hiktal.yukinet.server;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.server.impl.Deployment;
import moe.hiktal.yukinet.server.impl.LocalServer;
import moe.hiktal.yukinet.http.EndpointHttpResponse;
import moe.hiktal.yukinet.util.FileUtil;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.util.Util;
import org.bukkit.World;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.text.DecimalFormat;
import java.util.*;

public class ServerManager {
    @Getter
    private static boolean isShuttingDown;
    public static Server proxy;
    public static List<Server> staticServers = new ArrayList<>();
    public static List<Deployment> deployments = new ArrayList<>();
    public static List<Server> dynamicServers = new ArrayList<>();
    public final static List<Server> stoppedServers = new ArrayList<>();
    public static int receivedRemote = 0;
    public final static List<Server> restartQueue = new ArrayList<>();
    public static boolean isRunning;

    public static List<Server> GetAllServers() {
        List<Server> ret = new ArrayList<>();
        ret.addAll(staticServers);
        ret.addAll(dynamicServers);
        return ret;
    }

    public static void StartBoot() throws IOException {
        WalkAndCopyServers(YukiNet.cwd);

        if (!YukiNet.isDeployment && YukiNet.expectDeployments > 0) {
            YukiNet.getLogger().info("Waiting for %s deployments to message us...".formatted(YukiNet.expectDeployments));
            return;
        }

        if (YukiNet.isDeployment) {
            String thisIp = YukiNet.cfg.getString("http.this.ip");
            int thisPort = YukiNet.cfg.getInt("http.master.port", 3982);
            String ip = YukiNet.cfg.getString("http.master.ip", "0.0.0.0");
            int port = YukiNet.cfg.getInt("http.master.port", 3982);
            YukiNet.getLogger().info("Sending our info to master @ %s:%s".formatted(ip, port));

            // construct our json object
            ObjectMapper mapper = new ObjectMapper();
            ObjectNode ret = mapper.createObjectNode();

            ret.put("ip", thisIp);
            ret.put("port", thisPort);

            ArrayNode servers = mapper.createArrayNode();
            for (Server server : GetAllServers()) {
                ObjectNode pth = mapper.createObjectNode();
                pth.put("id", server.getId());
                pth.put("port", server.getPort());
                servers.add(pth);
            }
            ret.set("servers", servers);

            EndpointHttpResponse res = HttpUtil.HttpPostSync("http://%s:%s/internal/start/clearance".formatted(ip, port), ret.toString());
            if (!res.suc) YukiNet.getLogger().error("Master rejected our clearance. This deployment will not start.");
            else YukiNet.getLogger().info("Master received and processed out request. Waiting for clearance to start.");
            return;
        }

        RewriteBungeecordConfig();
    }

    public static void WalkAndCopyServers(File cwd) throws IOException {
        YukiNet.getLogger().info("Copying statics...");

        int nextPort = YukiNet.cfg.getInt("portMin", 12000);
        File[] statics = Objects.requireNonNull(new File(cwd + "/static").listFiles());
        for (File dir : statics) {
            if (!dir.isDirectory() || dir.getName().startsWith(".") || dir.getName().equals("proxy")) continue;
            if (!new File(dir + "/.yuki.yml").exists()) continue;

            String[] args = dir.getName().split("\\.");

            YukiNet.getLogger().info(String.format("Copying static %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            String id = cfg.getString("id");
            int port = Util.NextUsablePort(nextPort);
            Server server = new LocalServer(id, 0, port, true);
            staticServers.add(server);
            server.SetProxy(dir);

            nextPort = port + 1;
        }

        YukiNet.getLogger().info("Copying templates...");

        File[] toCopy = Objects.requireNonNull(new File(cwd + "/template").listFiles());

        for (File dir : toCopy) {
            if (!dir.isDirectory() || dir.getName().startsWith(".")) continue;
            if (!new File(dir + "/.yuki.yml").exists()) continue;

            String[] args = dir.getName().split("\\.");

            YukiNet.getLogger().info(String.format("Copying template %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            int amount = cfg.getInt("count", 1);
            String id = cfg.getString("id");//
            for (int i = 0; i < amount; i++) {
                // allocate a port
                int port = Util.NextUsablePort(nextPort);
                LocalServer server = new LocalServer(id, i + 1, port, false);
                File targetDir = new File(cwd + String.format("/live/%s/%s", id, server.getId()));
                CollectAndCopyFiles(id, cwd, targetDir);
                dynamicServers.add(server);
                server.SetCwd(cwd);

                nextPort = port + 1;
            }

        }

    }

    public static boolean AllowingServerReboot() {
        return !isShuttingDown && YukiNet.cfg.getBoolean("restartServersOnStop");
    }

    public static boolean AllowsServerAutoRestart(Server server) {
        return !isShuttingDown && !stoppedServers.contains(server) && GetAllServers().contains(server) && YukiNet.cfg.getBoolean("restartServersOnStop");
    }

    public static void RewriteBungeecordConfig() throws IOException{
        YukiNet.getLogger().info("Rewriting BungeeCord config...");

        File cwd = new File(YukiNet.cwd + YukiNet.cfg.getString("proxyDir"));
        if (!cwd.exists()) {
            YukiNet.getLogger().error(String.format("Proxy directory (%s) not found! Aborting startup.", cwd));
            return;
        }
        YamlConfiguration yukiCfg = YamlConfiguration.loadConfiguration(new File(cwd + "/.yuki.yml"));

        // get the config file
        File configFile = new File(cwd + yukiCfg.getString("configPath"));
        if (!cwd.exists()) {
            YukiNet.getLogger().error(String.format("Proxy configuration (%s) not found! Aborting startup.", configFile));
            return;
        }

        // back up config file
        File configBackup = new File(cwd + "/config-backup.yml");
        if (!configBackup.exists()) {
            YukiNet.getLogger().info("A backup of your config file has been made.");
            Files.copy(configFile.toPath(), configBackup.toPath());
        }

        // read
        YamlConfiguration config = YamlConfiguration.loadConfiguration(configFile);
        config.set("servers", null);

        List<String> priorities = new ArrayList<>();
        for (Server server : GetAllServers()) {
            WriteOneServer(config, server);
            if (server.getConfig() == null || server.getConfig().getBoolean("writePriorities")) priorities.add(server.getId());
        }
        LinkedHashMap<String, Object> listener = (LinkedHashMap<String, Object>) config.getList("listeners").get(0);
        listener.put("priorities", priorities);
        List<LinkedHashMap<String, Object>> listeners = new ArrayList<>();
        listeners.add(listener);
        config.set("listeners", listeners);

        config.save(configFile);

        YukiNet.getLogger().info("Done rewriting Bungeecord. Starting proxy...");

        proxy = new LocalServer("__proxy", 0, 0, true);
        proxy.SetProxy(cwd);
        proxy.Start(suc -> {
            if (suc) {
                YukiNet.getLogger().info("Proxy start success. Starting servers...");
                try {

                    if (receivedRemote > 0) {
                        YukiNet.getLogger().info("Sending clearance to deployments...");
                        for (Deployment deployment : deployments) deployment.SendStartClearance();
                    }

                    StartAllServers();

                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }
        });

    }

    public static void StartAllServers() throws IOException{
        isRunning = true;
        final List<Server> servers = GetAllServers().stream().filter(c -> c instanceof LocalServer).toList();
        final DecimalFormat formatter = new DecimalFormat("#.##");
        int delay = YukiNet.cfg.getInt("serverStartInterval", 30000);
        final Timer timer = new Timer();

        YukiNet.getLogger().info("Starting 1 server every %s seconds.".formatted(formatter.format((double)delay / 1000.0)));

        YukiNet.getLogger().info("Estimated time: %s".formatted(Util.FormatMilliseconds((long)delay * servers.size())));

        timer.schedule(new TimerTask() {
            int count = 0;

            @Override
            public void run() {
                if (isShuttingDown) {
                    timer.cancel();
                    return;
                }

                count++;
                YukiNet.getLogger().info("Starting servers %s/%s (%s%%)".formatted(count, servers.size(), formatter.format((double)count / servers.size() * 100.0)));
                YukiNet.getLogger().info("Estimated time remaining: %s".formatted(Util.FormatMilliseconds((long)delay * (servers.size() - count))));

                try {
                    servers.get(count - 1).Start();
                } catch (IOException e) {
                    e.printStackTrace();
                }

                if (count >= servers.size()) {
                    YukiNet.getLogger().info("All servers have been started.");
                    timer.cancel();
                }
            }

        }, 0, delay);

        new Timer().schedule(new TimerTask() {

            @Override
            public void run() {
                if (restartQueue.size() == 0 || isShuttingDown) return;
                Server server = restartQueue.get(0);
                restartQueue.remove(0);
                YukiNet.getLogger().info("Server %s restarting".formatted(server.getId()));
                try {
                    server.Start();
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            }

        }, 0, 10000);
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
        sec.set("address", String.format("%s:%s", server.GetHost(), server.getPort()));
        sec.set("restricted", false);
        cfg.set(String.format("servers.%s", id), sec);
    }

    public static void CollectAndCopyFiles(String groupId, File cwd, File target) throws IOException{
        if (!target.exists()) Files.createDirectories(target.toPath());

        // global files
        File globalDir = new File(cwd + "/template/.global");
        if (globalDir.exists()) FileUtil.RecursiveCopy(globalDir, target);

        // subgroup files
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(cwd + "/template/" + groupId + "/.yuki.yml"));
        List<String> copyBefore = cfg.getStringList("copyTree");
        for (String path : copyBefore) {
            YukiNet.getLogger().info("Copying tree %s".formatted(path));
            File dir = new File(cwd + "/template/" + path);
            if (!dir.exists()) {
                YukiNet.getLogger().warn("Subfolder %s not found! Skipping.".formatted(dir.getAbsolutePath()));
                continue;
            }

            FileUtil.RecursiveCopy(dir, target);
        }

        // group files
        FileUtil.RecursiveCopy(new File(cwd + "/template/" + groupId), target);

    }

    public static boolean AddDeployment(Deployment deployment) {

        deployments.add(deployment);
        return true;
    }

    public static void Shutdown() throws IOException{
        YukiNet.getLogger().info("Gracefully shutting down servers...");
        isShuttingDown = true;
        YukiNet.getIoThread().interrupt();

        if (isRunning) {
            if (proxy != null) proxy.Interrupt();

            for (Server server : staticServers) server.Interrupt();
            for (Server server : dynamicServers) server.Interrupt();

            YukiNet.getLogger().info("Allowing 10 seconds for servers to shut down themselves before forcefully shutting down - DO NOT SEND ^C");
            YukiNet.getLogger().info("DO NOT SEND ^C!!!!!!");

            try {
                int counter = 0;
                while (counter < 10000) {
                    counter += 100;
                    Thread.sleep(100);
                    if (ServerManager.GetAllServers().stream().noneMatch(Server::IsAlive)) break;
                }

                List<Server> survivers = ServerManager.GetAllServers().stream().filter(Server::IsAlive).toList();
                if (survivers.size() > 0) {
                    YukiNet.getLogger().warn("Killing %s survivers...".formatted(survivers.size()));
                    for (Server server : survivers) {
                        server.Interrupt(true);
                    }
                }

            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }

        YukiNet.getLogger().info("Shutdown done, goodbye.");
        System.exit(0);
    }

}
