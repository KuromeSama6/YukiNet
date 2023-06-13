package moe.hiktal.YukiNet;

public class Config {
    private static boolean linuxAvailable;

    static void SetIsWorkingLinux(boolean working) {
        linuxAvailable = working;
    }

    public static boolean IsWorkingLinux() {
        return linuxAvailable;
    }

}
