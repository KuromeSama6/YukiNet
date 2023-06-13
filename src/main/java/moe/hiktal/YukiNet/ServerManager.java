package moe.hiktal.YukiNet;

import moe.hiktal.YukiNet.classes.Server;
import oracle.jdbc.logging.annotations.Log;
import org.apache.commons.io.FileUtils;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ServerManager {
    public static Server proxy;
    public static List<Server> staticServers = new ArrayList<>();
    public static List<Server> dynamicServers = new ArrayList<>();

    public static void WalkAndCopyServers(File cwd) throws IOException {
        Logger.Info("Copying templates...");

        File[] toCopy = Objects.requireNonNull(new File(cwd + "/template").listFiles());
        for (File dir : toCopy) {
            if (!dir.isDirectory() || dir.getName().startsWith(".")) continue;

            Logger.Info(String.format("Copying template %s", dir.getName()));

            YamlConfiguration cfg = YamlConfiguration.loadConfiguration(new File(dir + "/.yuki.yml"));
            int amount = cfg.getInt("count", 1);
            String id = cfg.getString("id");
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

                Server server = new Server(id, i + 1, port);
                File targetDir = new File(cwd + String.format("/live/%s/%s", id, server.getId()));
                FileUtils.copyDirectory(dir, targetDir);
                Files.deleteIfExists(new File(targetDir + "/.yuki.yml").toPath());
                dynamicServers.add(server);

                //TODO Add different priorities and global template

            }

        }

    }
}
