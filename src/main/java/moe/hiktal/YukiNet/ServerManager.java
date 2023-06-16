package moe.hiktal.YukiNet;

import moe.hiktal.YukiNet.classes.Server;
import oracle.jdbc.logging.annotations.Log;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.*;
import java.util.stream.Collectors;

public class ServerManager {
    public static Server proxy;
    public static List<Server> staticServers = new ArrayList<>();
    public static List<Server> dynamicServers = new ArrayList<>();

    public static void WalkAndCopyServers(File cwd) throws IOException {
        Logger.Info("Copying templates...");

        File[] toCopy = Objects.requireNonNull(new File(cwd + "/template").listFiles());

        for (File dir : toCopy) {
            if (!dir.isDirectory() || dir.getName().startsWith(".") ) continue;
            String[] args = dir.getName().split("\\.");
            if (args.length != 1) continue;

            Logger.Info(String.format("Copying template %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            int amount = cfg.getInt("count", 1);
            String id = cfg.getString("id");//
            int portMin = Math.max(0, cfg.getInt("portMin", 10000));
            int portMax = Math.min(65536, cfg.getInt("portMax", 25565));

            for (int i = 0; i < amount; i++) {
                // allocate a port
                int port = -1;
                List<Server> currentServers = dynamicServers.stream()
                        .filter(c -> c.getGroupId().equals(id))
                        .toList();

                if (!currentServers.stream().map(Server::getGroupId).toList().contains(id)) {
                    port = portMin;

                } else {
                    List<Integer> ports = currentServers.stream().map(Server::getPort).toList();
                    for (int j = portMin; j < portMax; j++) {
                        if (!ports.contains(j)) {
                            port = j;
                            break;
                        }
                    }
                }

                if (port == -1) {
                    Logger.Warning(String.format("Unable to start server #%s for group %s: Maximum port number reached.", i, id));
                    continue;
                }

                Server server = new Server(id, i + 1, port, cwd);
                File targetDir = new File(cwd + String.format("/live/%s/%s", id, server.getId()));
                CollectAndCopyFiles(id, cwd, targetDir);
                dynamicServers.add(server);
                server.SetCwd(cwd);
                server.Start();

            }

        }

    }

    private static void CollectAndCopyFiles(String groupId, File cwd, File target) throws IOException{
        if (!target.exists()) Files.createDirectories(target.toPath());

        // global files
        File globalDir = new File(cwd + "/template/.global");
        if (globalDir.exists()) FileUtil.RecursiveCopy(globalDir, target);

        // group files
        FileUtil.RecursiveCopy(new File(cwd + "/template/" + groupId), target);

        // subgroup files
        List<File> candidates = Arrays.stream(Objects.requireNonNull(new File(cwd + "/template").listFiles()))
                .filter(c -> c.isDirectory() && !c.getName().startsWith("\\."))
                .filter(c -> {
                    String[] args = c.getName().split("\\.");
                    if (args.length < 2 || !args[0].equals(groupId)) return false;
                    try {
                        Integer.parseInt(args[1]);
                        return true;
                    } catch (NumberFormatException e) { return false; }
                })
                .sorted(Comparator.comparingInt(c -> Integer.parseInt(c.getName().split("\\.")[1])))
                .toList();

        for (File dir : candidates) FileUtil.RecursiveCopy(dir, target);

    }

}
