package moe.hiktal.yukinet.server;

public class Config {
    private static boolean linuxAvailable;

    public static void SetIsWorkingLinux(boolean working) {
        linuxAvailable = working;
    }

    public static boolean IsWorkingLinux() {
        return linuxAvailable;
    }

}
