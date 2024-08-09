package moe.hiktal.yukinet.server;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import lombok.Getter;
import lombok.Setter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.io.FileProvider;
import moe.hiktal.yukinet.server.impl.LocalServer;
import moe.hiktal.yukinet.http.EndpointHttpResponse;
import moe.hiktal.yukinet.util.FileUtil;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.util.Util;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.security.CodeSource;
import java.text.DecimalFormat;
import java.util.*;

public class ServerManager {
    @Getter
    private boolean isShuttingDown;
    @Getter
    private Server proxy;
    private List<Server> staticServers = new ArrayList<>();
    @Getter
    private List<Deployment> deployments = new ArrayList<>();
    @Getter
    private List<Server> dynamicServers = new ArrayList<>();
    @Getter
    private final List<Server> stoppedServers = new ArrayList<>();
    @Getter @Setter
    private int receivedRemote = 0;
    @Getter
    private final List<Server> restartQueue = new ArrayList<>();
    private boolean isRunning;
    private FileProvider fileProvider;

    public List<Server> GetAllServers() {
        List<Server> ret = GetAllLocalServers();
        for (Deployment deployment : deployments) ret.addAll(deployment.servers);
        return ret;
    }

    public List<Server> GetAllLocalServers() {
        List<Server> ret = new ArrayList<>();
        ret.addAll(staticServers);
        ret.addAll(dynamicServers);
        return ret;
    }

    public void Start() throws IOException {
        // download
//        YukiNet.getLogger().info("Download files from core");
//        FileDownloader downloader = new FileDownloader(this);
//        downloader.Download();
        fileProvider = new FileProvider(this);

        // files
        WalkAndCopyServers(YukiNet.CWD);

        // clearance
        if (!YukiNet.getInstance().isDeployment() && YukiNet.getInstance().getExpectDeployments() > 0) {
            YukiNet.getLogger().info("Waiting for %s deployments to message us...".formatted(YukiNet.getInstance().getExpectDeployments()));
            return;
        }

        if (YukiNet.getInstance().isDeployment()) {
            String thisIp = YukiNet.getCfg().getString("http.this.ip");
            int thisPort = YukiNet.getCfg().getInt("http.master.port", 3982);
            String ip = YukiNet.getCfg().getString("http.master.ip", "0.0.0.0");
            int port = YukiNet.getCfg().getInt("http.master.port", 3982);
            YukiNet.getLogger().info("Sending our info to master @ %s:%s".formatted(ip, port));

            // construct our json object
            JsonObject ret = new JsonObject();

            ret.addProperty("ip", thisIp);
            ret.addProperty("port", thisPort);

            JsonArray servers = new JsonArray();
            for (Server server : GetAllLocalServers()) {
                JsonObject pth = new JsonObject();
                pth.addProperty("id", server.getId());
                pth.addProperty("port", server.getPort());
                servers.add(pth);
            }
            ret.add("servers", servers);

            EndpointHttpResponse res = HttpUtil.HttpPostSync("http://%s:%s/internal/start/clearance".formatted(ip, port), ret.toString());
            if (!res.suc) YukiNet.getLogger().error("Master rejected our clearance. This deployment will not start.");
            else YukiNet.getLogger().info("Master received and processed out request. Waiting for clearance to start.");
            return;
        }

        // bungeecord
        RewriteBungeecordConfig();
    }

    public void WalkAndCopyServers(File cwd) throws IOException {
        YukiNet.getLogger().info("Copying statics...");

        int nextPort = YukiNet.getCfg().getInt("portMin", 12000);
        File[] statics = Objects.requireNonNull(new File(cwd + "/static").listFiles());
        for (File dir : statics) {
            if (!dir.isDirectory() || dir.getName().startsWith(".") || dir.getName().equals("proxy")) continue;
            if (!new File(dir + "/.yuki.yml").exists()) continue;

            String[] args = dir.getName().split("\\.");

            YukiNet.getLogger().info(String.format("Copying static %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            String id = cfg.getString("id");
            int port = Util.AllocatePort(nextPort);
            Server server = new LocalServer(id, 0, port, true);
            staticServers.add(server);
            server.SetProxy(dir);

            nextPort = port + 1;
        }

        YukiNet.getLogger().info("Copying templates...");

        File[] toCopy = Objects.requireNonNull(new File(cwd + "/template").listFiles());
        Arrays.sort(toCopy);

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
                int port = Util.AllocatePort(nextPort);
                LocalServer server = new LocalServer(id, i + 1, port, false);
                File targetDir = new File(cwd + String.format("/live/%s/%s", id, server.getId()));

                CollectAndCopyFiles(id, cwd, targetDir);

                dynamicServers.add(server);
                server.SetCwd(cwd);

                nextPort = port + 1;
            }

        }

    }

    public boolean AllowingServerReboot() {
        return !isShuttingDown &&YukiNet.getCfg().getBoolean("restartServersOnStop");
    }

    public boolean AllowsServerAutoRestart(Server server) {
        return !isShuttingDown && !stoppedServers.contains(server) && GetAllLocalServers().contains(server) &&YukiNet.getCfg().getBoolean("restartServersOnStop");
    }

    public void RewriteBungeecordConfig() throws IOException{
        YukiNet.getLogger().info("Rewriting BungeeCord config...");

        File cwd = new File(YukiNet.CWD +YukiNet.getCfg().getString("proxyDir"));
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
        for (Server server : GetAllLocalServers()) {
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

        // copy jar
        CodeSource codeSource = YukiNet.class.getProtectionDomain().getCodeSource();
        try {
            File jarFile = new File(codeSource.getLocation().toURI().getPath());
            File target = new File(cwd + "/plugins/" + jarFile.getName());
            Files.copy(jarFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);

        } catch (URISyntaxException e) {
            throw new RuntimeException(e);
        }

        proxy.Start(suc -> {
            if (suc) {
                YukiNet.getLogger().info("Proxy start success. Starting servers...");
                YukiNet.getLogger().info("Dynamically writing servers.");

                for (Server server : GetAllServers()) {
                    try {
                        YukiNet.getLogger().info("Add %s [%s]".formatted(server.getId(), server.getClass().getSimpleName()));
                        proxy.SendInput("addserver %s %s %d".formatted(
                                server.getId(),
                                server.getDeployment() == null ? "127.0.0.1" : server.getDeployment().ip,
                                server.getPort()
                        ));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

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

    public void StartAllServers() throws IOException{
        isRunning = true;
        final List<Server> servers = GetAllLocalServers().stream().filter(c -> c instanceof LocalServer).toList();
        final DecimalFormat formatter = new DecimalFormat("#.##");
        int delay =YukiNet.getCfg().getInt("serverStartInterval", 30000);
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

    public void RestartServer(Server server) throws IOException{
        dynamicServers.remove(server);
        server.Interrupt(true);

        Server newServer = server.Clone();
        dynamicServers.add(newServer);
        newServer.Start();
    }

    private void WriteOneServer(YamlConfiguration cfg, Server server) {
        String id = server.getId();
        YamlConfiguration sec = new YamlConfiguration();
        sec.set("motd", "a YukiNet server");
        sec.set("address", String.format("%s:%s", server.GetHost(), server.getPort()));
        sec.set("restricted", false);
        cfg.set(String.format("servers.%s", id), sec);
    }

    public void CollectAndCopyFiles(String groupId, File cwd, File target) throws IOException{
        if (!target.exists()) Files.createDirectories(target.toPath());

        // global files
        File globalDir = new File(cwd + "/template/.global");
        if (globalDir.exists()) FileUtil.RecursiveCopy(globalDir, target);

        // subgroup files
        YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(cwd + "/template/" + groupId + "/.yuki.yml"));
        List<String> copyBefore = cfg.getStringList("copyTree");
        for (String path : copyBefore) {
            YukiNet.getLogger().info("Copying tree %s".formatted(path));

            FileUtil.RecursiveCopy(fileProvider, "/template/" + path, target);
        }

        // group files
        FileUtil.RecursiveCopy(fileProvider, "/template/" + groupId, target);

    }

    public boolean AddDeployment(Deployment deployment) {
        deployments.add(deployment);
        return true;
    }

    public void Shutdown() throws IOException{
        YukiNet.getLogger().info("Gracefully shutting down servers...");
        isShuttingDown = true;
        YukiNet.getInstance().getIoThread().interrupt();

        if (isRunning) {
            if (proxy != null) proxy.Interrupt();
            for (Server server : staticServers) server.Interrupt();
            for (Server server : dynamicServers) server.Interrupt();

            YukiNet.getLogger().info("Allowing 10 seconds for servers to shut down themselves before forcefully shutting down - DO NOT SEND ^C");
            YukiNet.getLogger().info("DO NOT SEND ^C!!!!!!");

            for (int i = 0; i < 10; i++) {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                }
                if (GetAllLocalServers().stream().noneMatch(Server::IsAlive)) break;
            }

            YukiNet.getLogger().info("Initial shutdown finished.");

            List<Server> survivers = GetAllLocalServers().stream().filter(Server::IsAlive).toList();
            if (proxy != null && proxy.IsAlive()) proxy.Interrupt(true);
            if (survivers.size() > 0) {
                YukiNet.getLogger().warn("Killing %s survivers...".formatted(survivers.size()));
                for (Server server : survivers) {
                    server.Interrupt(true);
                }
            }
        }

        YukiNet.getLogger().info("Wiping dead screens...");
        Process process = new ProcessBuilder("screen", "-wipe").start();
        try {
            process.waitFor();
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        YukiNet.getLogger().info("ServerManager shutdown complete.");
    }

}
