package moe.hiktal.yukinet;


import lombok.Getter;
import moe.hiktal.yukinet.http.HttpHost;
import moe.hiktal.yukinet.io.Console;
import moe.hiktal.yukinet.server.Config;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.util.FileUtil;
import moe.hiktal.yukinet.util.Util;
import moe.icegame.coreutils.DevUtil;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.YamlConfiguration;

import javax.swing.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class YukiNet {
    @Getter
    private static final Logger logger = LogManager.getLogger(YukiNet.class);
    @Getter
    private static Thread ioThread;
    public static FileConfiguration cfg, commands = new YamlConfiguration();
    public static final String cwdStr = Paths.get("").toAbsolutePath().normalize().toString();
    public static final File cwd = new File(cwdStr);
    public static HttpHost httpHost;
    public static boolean isDeployment;
    public static int expectDeployments;
    private static final BlockingQueue<Runnable> taskQueue = new LinkedBlockingQueue<>();

    public static void main(String[] args) throws IOException, InterruptedException {
        logger.info("Starting YukiNet.");

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
                logger.warn("GNU Screen is not installed. Features may be limited.");
            }

        } else logger.warn("This operating system is not linux. Features may be limited.");

        StartSetup();
    }

    private static void StartSetup() throws IOException{
        logger.info(String.format("Current directory: %s", cwdStr));
        final boolean isFirstTime = !Files.exists(Paths.get(cwdStr + "/config.yml"));

        // directories
        FileUtil.MkdirSoft(new File(cwd + "/static").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/static/proxy").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template/.global").toPath());
        FileUtil.MkdirSoft(new File(cwd + "/template/lobby").toPath());

        // save config files
        FileUtil.UpdateCofig(cwd, new YukiNet(), "/configs/proxy.yml", "/static/proxy/.yuki.yml");
        FileUtil.UpdateCofig(cwd, new YukiNet(), "/configs/server.yml", "/template/lobby/.yuki.yml");
        FileUtil.UpdateCofig(cwd, new YukiNet(), "/config.yml");

        // quit
        if (isFirstTime) {
            logger.error("Looks like this is your first time running YukiNet. Please complete configurating and run YukiNet again. Refer to REAME.md for help.");
            System.exit(0);
            return;
        }

        cfg = YamlConfiguration.loadConfiguration(new File(cwd + "/config.yml"));

        try {
            commands.loadFromString(DevUtil.ReadResourceFile(YukiNet.class, "commands.yml"));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

        isDeployment = cfg.getBoolean("isDeployment");
        expectDeployments = cfg.getInt("expect");
        int port = cfg.getInt("http.this.port");
        httpHost = new HttpHost(port);

        // more directories
        logger.info("Clearing /live directory for new deployment.");
        FileUtil.MkdirSoft(new File(cwd + "/live").toPath());

        ioThread = new Console();
        ioThread.start();

        StartBoot();
    }

    private static void StartBoot() throws IOException{
        logger.info("Starting deployment workflow.");

        logger.info("");
        logger.info("Send a GET request to http://%s:%s/help for help on commands.".formatted(cfg.getString("http.this.ip"), cfg.getInt("http.this.port")));
        logger.info("For example:");
        logger.info("   $ curl %s:%s/help".formatted(cfg.getString("http.this.ip"), cfg.getInt("http.this.port")));
        logger.info("");

        ServerManager.StartBoot();

    }

}