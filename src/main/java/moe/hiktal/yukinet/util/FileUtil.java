package moe.hiktal.yukinet.util;

import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.*;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.Collections;
import java.util.Iterator;
import java.util.Objects;
import java.util.Scanner;

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

    public static void RecursiveCopy(File source, File target) throws IOException {
        if (source.isFile()) {
            Files.copy(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
            return;
        }
        if (!target.exists()) {
            target.mkdir();
        }
        for (File file : source.listFiles()) {
            File newFile = new File(target.getAbsolutePath() + File.separator + file.getName());
            if (file.isDirectory()) {
                RecursiveCopy(file, newFile);
            } else {
                //Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                boolean same = CompareFile(file, newFile);
                if (!same) {
                    Files.copy(file.toPath(), newFile.toPath(), StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
    }

    public static boolean CompareFile(File f1, File f2) throws IOException {
        if (!f2.exists()) return false;
        return Files.mismatch(f1.toPath(), f2.toPath()) == -1L;//
    }

    public static boolean MCompareFile(File f1, File f2) throws IOException {
        if (!f2.exists()) return false;

        try (RandomAccessFile randomAccessFile1 = new RandomAccessFile(f1, "r");
             RandomAccessFile randomAccessFile2 = new RandomAccessFile(f2, "r")) {

            FileChannel ch1 = randomAccessFile1.getChannel();
            FileChannel ch2 = randomAccessFile2.getChannel();
            if (ch1.size() != ch2.size()) {
                return false;
            }
            long size = ch1.size();
            MappedByteBuffer m1 = ch1.map(FileChannel.MapMode.READ_ONLY, 0L, size);
            MappedByteBuffer m2 = ch2.map(FileChannel.MapMode.READ_ONLY, 0L, size);
            ch1.close();
            ch2.close();
            return m1.equals(m2);
        }
    }

    public static void FastCopy(File in, File out) throws IOException {
        if (out.exists()) out.delete();
        FileChannel inChannel = new  FileInputStream(in).getChannel();
        FileChannel outChannel = new FileOutputStream(out).getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } catch (IOException e) {
            throw e;
        } finally {
            if (inChannel != null) inChannel.close();
            if (outChannel != null) outChannel.close();
        }
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
            String resource = ReadResourceFile(pl.getClass(), relPath.substring(1));
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
                newest.loadFromString(ReadResourceFile(pl.getClass(), relPath.substring(1)));
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

    public static String ReadResourceFile(Class cls, String path) {
        // Get the input stream for the SQL file
        InputStream inputStream = cls.getClassLoader().getResourceAsStream(path);

        if (inputStream == null) throw new RuntimeException(String.format("Unable to find resource %s", path));

        // Read the input stream into a string using a scanner
        Scanner scanner = new Scanner(inputStream).useDelimiter("\\A");
        String ret = scanner.hasNext() ? scanner.next() : "";

        // Close the input stream and the scanner
        try {
            inputStream.close();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        scanner.close();

        return ret;
    }

}
