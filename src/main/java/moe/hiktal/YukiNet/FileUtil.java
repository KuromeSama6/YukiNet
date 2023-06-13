package moe.hiktal.YukiNet;

import moe.icegame.coreutils.DevUtil;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;

public class FileUtil {
    /**
     * Fails and returns false when the directory already exists.
     */
    public static boolean MkdirSoft(Path pth) {
        if (Files.exists(pth)) return false;

        try {
            Files.createDirectories(pth);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return true;

    }

    /**
     * Deletes the directory if it already exists and makes a new one.
     */
    public static void MkdirHard(Path pth) {
        try {
            UnlinkRecursive(pth.toFile());
            Files.createDirectories(pth);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

    }

    public static void UpdateCofig(File dataFolder, Object pl, String relPath) {
        UpdateCofig(dataFolder, pl, relPath, relPath);
    }

    public static void UnlinkRecursive(File dir) throws IOException{
        if (dir == null || !dir.exists()) return;;
        if (dir.isFile()) {
            Files.deleteIfExists(dir.toPath());
            return;
        }

        for (File file : Objects.requireNonNull(dir.listFiles())) UnlinkRecursive(file);
        Files.deleteIfExists(dir.toPath());

    }

    public static void UpdateCofig(File dataFolder, Object pl, String relPath, String outPath) {
        if (!Files.exists(Paths.get(dataFolder.getAbsolutePath()), new LinkOption[0])) {
            try {
                Files.createDirectory(Paths.get(dataFolder.getAbsolutePath()));
            } catch (IOException var9) {
                throw new RuntimeException(var9);
            }
        }

        if (!Files.exists(Paths.get(dataFolder.getAbsolutePath() + outPath), new LinkOption[0])) {
            String resource = DevUtil.ReadResourceFile(pl.getClass(), relPath.substring(1));
            Path path = Paths.get(dataFolder.getAbsolutePath() + outPath);

            try {
                Files.write(path, Collections.singleton(resource), StandardCharsets.UTF_8, StandardOpenOption.CREATE);
            } catch (IOException var8) {
                throw new RuntimeException(var8);
            }
        }

        try {
            if ((new File(dataFolder + outPath)).exists()) {
                boolean changesMade = false;
                YamlConfiguration current = new YamlConfiguration();
                current.load(dataFolder + outPath);
                YamlConfiguration newest = new YamlConfiguration();
                newest.loadFromString(DevUtil.ReadResourceFile(pl.getClass(), relPath.substring(1)));
                Iterator var6 = newest.getKeys(true).iterator();

                while(var6.hasNext()) {
                    String str = (String)var6.next();
                    if (!current.getKeys(true).contains(str)) {
                        current.set(str, newest.get(str));
                        changesMade = true;
                    }
                }

                if (current.getKeys(false).contains("__p")) {
                    current.set("__p", (Object)null);
                    changesMade = true;
                }

                if (changesMade) {
                    current.save(dataFolder + outPath);
                }
            }
        } catch (InvalidConfigurationException | IOException var10) {
            var10.printStackTrace();
        }

    }

}
