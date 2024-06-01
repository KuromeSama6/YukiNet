package moe.hiktal.yukinet.util;

import moe.hiktal.yukinet.YukiNet;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class Util {
    public static boolean IsSupportedOperatingSystem() {
        return SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_AIX || SystemUtils.IS_OS_UNIX;
    }

    public static int GrabScreenSessionId(String name) throws IOException {
        String[] command = { "/bin/sh", "-c", "screen -list | grep " + name };
        Process process = Runtime.getRuntime().exec(command);


        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains(name)) {
                    String[] parts = line.trim().split("\\.");
                    return Integer.parseInt(parts[0]);
                }
            }
        }
        return -1; // session not found
    }

    public static int GetPidByPort(int port) throws IOException {
        Process process = new ProcessBuilder(
                "lsof", "-i", ":%d".formatted(port)
        ).start();

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.contains("(LISTEN)")) {
                    String[] args = line.split("\\s+");
                    return Integer.parseInt(args[1]);
                }
            }
        }

        return -1;
    }

    public static boolean KillProcess(int pid) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(
                "kill", "%d".formatted(pid)
        ).start();
        return process.waitFor() == 0;
    }


    public static String FormatMilliseconds(long millis) {
        return String.format("%02d:%02d:%02d",
                TimeUnit.MILLISECONDS.toHours(millis),
                TimeUnit.MILLISECONDS.toMinutes(millis) -
                TimeUnit.HOURS.toMinutes(TimeUnit.MILLISECONDS.toHours(millis)), // The change is in this line
                TimeUnit.MILLISECONDS.toSeconds(millis) -
                TimeUnit.MINUTES.toSeconds(TimeUnit.MILLISECONDS.toMinutes(millis)));
    }

    public static boolean CheckPortUsable(int port) {
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            // Port is free, able to bind to it
            return true;
        } catch (IOException e) {
            // Port is already in use
            return false;
        }
    }

    public static int AllocatePort(int start) {
        for (int i = start; i < 65535; i++) {
            boolean usable = CheckPortUsable(start);

            if (i == start && !usable) {
                YukiNet.getLogger().warn("Port %d is occupied. Attempting to kill what is running on that port.");
                try {
                    int pid = GetPidByPort(i);
                    if (pid == -1) {
                        YukiNet.getLogger().warn("Could not get what is on that port. Skipping.");
                        continue;
                    }

                    boolean res = KillProcess(pid);
                    if (!res) {
                        YukiNet.getLogger().warn("Unable to kill that process. Skipping.");
                        continue;
                    }

                } catch (IOException | InterruptedException e) {
                    throw new RuntimeException(e);
                }

                continue;
            }

            if (CheckPortUsable(i)) return i;
        }

        return -1;
    }

    public static ProcessHandle GrabProcHandle(int pid) {
        return ProcessHandle.of(pid).orElse(null);
    }

    public static String FormatList(List<?> list, int maxSize) {
        StringBuilder result = new StringBuilder("[");

        int size = list.size();

        for (int i = 0; i < Math.min(size, maxSize); i++) {
            result.append(list.get(i).toString());
            if (i < Math.min(size, maxSize) - 1) {
                result.append(", ");
            }
        }

        if (size > maxSize) {
            result.append(", and ").append(size - maxSize).append(" more...");
        }

        result.append("]");
        return result.toString();
    }


}
