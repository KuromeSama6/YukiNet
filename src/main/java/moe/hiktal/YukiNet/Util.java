package moe.hiktal.YukiNet;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Supplier;
import org.apache.commons.lang.SystemUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;

public class Util {
    public static boolean IsSupportedOperatingSystem() {
        return SystemUtils.IS_OS_LINUX || SystemUtils.IS_OS_AIX || SystemUtils.IS_OS_UNIX;
    }

    public static int GrabScreenSessionId(String name) throws IOException {
        String[] command = { "/bin/sh", "-c", "screen -list | grep " + name };
        Process process = Runtime.getRuntime().exec(command);

        BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.contains(name)) {
                String[] parts = line.trim().split("\\.");
                return Integer.parseInt(parts[0]);
            }
        }
        return -1; // session not found
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

    public static int NextUsablePort(int start) {
        for (int i = start; i < 65535; i++) {
            if (i == start && !CheckPortUsable(start)) System.out.println("port in use: %s".formatted(start));
            if (CheckPortUsable(start)) return i;
        }

        return -1;
    }

    public static ProcessHandle GrabProcHandle(int pid) {
        return ProcessHandle.of(pid).orElse(null);
    }

}
