package moe.hiktal.YukiNet;


import moe.hiktal.YukiNet.http.HttpHost;
import org.bukkit.configuration.file.FileConfiguration;

import moe.hiktal.YukiNet.FileUtil;
import org.bukkit.configuration.file.YamlConfiguration;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;

public class Main {
    public static FileConfiguration cfg;
    public static final String cwdStr = Paths.get("").toAbsolutePath().normalize().toString();
    public static final File cwd = new File(cwdStr);
    public static HttpHost httpHost;
    public static boolean isDeployment;
    public static int expectDeployments;

    public static void main(String[] args) throws IOException, InterruptedException {
        Logger.Info("Starting YukiNet.");

        // system check
        // operating system check
        boolean isLinux = Util.IsSupportedOperatingSystem();
        if (isLinux) {
            // command check
            try {
                Process proc = Runtime.getRuntime().exec(new String[] {"screen", "-version"});
                proc.waitFor();
                Config.SetIsWorkingLinux(true);

            } catch (IOException e) {
                Logger.Warning("GNU Screen is not installed. Features may be limited.");
            }

        } else Logger.Warning("This operating system is not linux. Features may be limited.");

        StartSetup();
    }

    private static void StartSetup() throws IOException{
        Logger.Info(String.format("Current directory: %s", cwdStr));
        final boolean isFirstTime = !Files.exists(Paths.get(cwdStr + "/config.yml"));

        // directories
        FileUtil.MkdirSoft(new File(cwd + "/static").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/static/proxy").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template/.global").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template/lobby").toPath());
        
        // save config files
        FileUtil.UpdateCofig(cwd, new Main(), "/configs/proxy.yml", "/static/proxy/.yuki.yml");
        FileUtil.UpdateCofig(cwd, new Main(), "/configs/server.yml", "/template/lobby/.yuki.yml");
        FileUtil.UpdateCofig(cwd, new Main(), "/config.yml");

        // quit
        if (isFirstTime) {
            Logger.Error("Looks like this is your first time running YukiNet. Please complete configurating and run YukiNet again. Refer to REAME.md for help.");
            System.exit(0);
            return;
        }

        cfg = YamlConfiguration.loadConfiguration(new File(cwd + "/config.yml"));
        isDeployment = cfg.getBoolean("isDeployment");
        expectDeployments = cfg.getInt("expect");
        int port = cfg.getInt("http.this.port");
        httpHost = new HttpHost(port);

        // more directories
        Logger.Info("Clearing /live directory for new deployment.");
        FileUtil.MkdirSoft(new File(cwd + "/live").toPath());
        StartBoot();
    }

    private static void StartBoot() throws IOException{
        Logger.Info("Starting deployment workflow.");

        Logger.Info("");
        Logger.Info("Send a GET request to http://%s:%s/help for help on commands.".formatted(cfg.getString("http.this.ip"), cfg.getInt("http.this.port")));
        Logger.Info("For example:");
        Logger.Info("   $ curl %s:%s/help".formatted(cfg.getString("http.this.ip"), cfg.getInt("http.this.port")));
        Logger.Info("");

        ServerManager.StartBoot();

    }

}