package moe.hiktal.YukiNet;

import org.joda.time.DateTime;

import java.util.logging.Level;

public class Logger {
    public static void Log(Level level, String str) {
        String color = "";
        if (level.equals(Level.SEVERE)) {
            color = "\u001B[31m";
        } else if (level.equals(Level.WARNING)) {
            color = "\u001B[33m";
        }

        System.out.printf("%s[%s][%s %s] %s\u001B[0m%n", color, level, DateTime.now().toLocalDate(), DateTime.now().toLocalTime(), str);
    }

    public static void Info(String str) {
        Log(Level.INFO, str);
    }

    public static void Warning(String str) {
        Log(Level.WARNING, str);
    }

    public static void Error(String str) {
        Log(Level.SEVERE, str);
    }

}
