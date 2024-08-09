package moe.hiktal.yukinet;

import com.google.gson.JsonObject;
import lombok.Getter;
import moe.hiktal.yukinet.http.HttpHost;
import moe.hiktal.yukinet.service.Console;
import moe.hiktal.yukinet.server.Config;
import moe.hiktal.yukinet.server.ServerManager;
import moe.hiktal.yukinet.service.FileServer;
import moe.hiktal.yukinet.service.JobScheduler;
import moe.hiktal.yukinet.util.FileUtil;
import moe.hiktal.yukinet.util.HttpUtil;
import moe.hiktal.yukinet.util.Util;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.bukkit.configuration.InvalidConfigurationException;
import org.bukkit.configuration.file.FileConfiguration;

import org.bukkit.configuration.file.YamlConfiguration;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

public class YukiNet {
    public static final String CWD_STRING = Paths.get("").toAbsolutePath().normalize().toString();
    public static final File CWD = new File(CWD_STRING);

    @Getter
    private static YukiNet instance;
    @Getter
    private static ServerManager serverManager;
    @Getter
    private static String masterIp = "0.0.0.0";

    @Getter
    private static final Logger logger = LogManager.getLogger(YukiNet.class);
    @Getter
    private Thread ioThread;
    @Getter
    private static FileConfiguration cfg, commands = new YamlConfiguration();
    @Getter
    private HttpHost httpHost;
    @Getter
    private boolean isDeployment;
    @Getter
    private int expectDeployments;

    public static void main(String[] args) throws IOException, InterruptedException {
        new YukiNet();
    }

    public YukiNet() throws IOException, InterruptedException {
        instance = this;

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

        } else {
            logger.error("Windows and non-Unix operating system support are no longer available. This program may only run on a Unix-based OS.");
            System.exit(2);
            return;
        }

        StartSetup();
    }


    private void StartSetup() throws IOException{
        logger.info(String.format("Current directory: %s", CWD_STRING));
        final boolean isFirstTime = !Files.exists(Paths.get(CWD_STRING + "/config.yml"));

        // directories
        FileUtil.MkdirSoft(new File(CWD + "/static").toPath());
        FileUtil.MkdirSoft(new File(CWD + "/static/proxy").toPath());
        FileUtil.MkdirSoft(new File(CWD + "/template").toPath());
        FileUtil.MkdirSoft(new File(CWD + "/template/.global").toPath());

        // save config files
        FileUtil.UpdateCofig(CWD, this, "/configs/proxy.yml", "/static/proxy/.yuki.yml");
        FileUtil.UpdateCofig(CWD, this, "/configs/server.yml", "/.yuki.yml");
        FileUtil.UpdateCofig(CWD, this, "/config.yml");

        // resources directory
//        {
//            File resources = new File(CWD + "/resources");
//            if (!resources.exists()) resources.mkdir();
//        }

        // quit
        if (isFirstTime) {
            FileUtil.MkdirSoft(new File(CWD + "/template/lobby").toPath());
            FileUtil.UpdateCofig(CWD, this, "/configs/server.yml", "/template/lobby/.yuki.yml");
            logger.error("Looks like this is your first time running YukiNet. Please complete configurating and run YukiNet again. Refer to REAME.md for help.");
            System.exit(0);
            return;
        }

        cfg = YamlConfiguration.loadConfiguration(new File(CWD + "/config.yml"));
        masterIp = cfg.getString("http.master.ip", "0.0.0.0");

        try {
            commands.loadFromString(FileUtil.ReadResourceFile(YukiNet.class, "commands.yml"));
        } catch (InvalidConfigurationException e) {
            e.printStackTrace();
        }

        isDeployment = cfg.getBoolean("isDeployment");
        expectDeployments = cfg.getInt("expect");
        int port = cfg.getInt("http.this.port");
        httpHost = new HttpHost(port);

        // send info to master
        if (isDeployment) {
            logger.info("Sending deployment info to master");

        }

        // more directories
        logger.info("Clearing /live directory for new deployment.");
        FileUtil.MkdirSoft(new File(CWD + "/live").toPath());

        ioThread = new Console();
        ioThread.start();

        new FileServer();

        // jobs file
        {
            File jobsFile = new File(CWD + "/jobs.yml");
            if (!jobsFile.exists()) {
                Files.createFile(jobsFile.toPath());
                Files.writeString(jobsFile.toPath(), FileUtil.ReadResourceFile(YukiNet.class, "job.yml"), StandardCharsets.UTF_8);
            }
            new JobScheduler(YamlConfiguration.loadConfiguration(jobsFile));
        }


        StartBoot();
    }

    private void StartBoot() throws IOException {
        logger.info("Starting deployment.");
        Start();
    }

    public void Start() throws IOException {
        if (serverManager != null)
            throw new IllegalStateException("A server manager is already created!");

        serverManager = new ServerManager();
        serverManager.Start();
    }

    public void Shutdown() throws IOException {
        if (serverManager != null) {
            serverManager.getDeployments().forEach(c -> c.NotifyShutdown(false));
            serverManager.Shutdown();
        }
        System.exit(0);
    }

    public void Reboot() throws IOException {
        if (serverManager != null) {
            serverManager.getDeployments().forEach(c -> c.NotifyShutdown(true));
            serverManager.Shutdown();
            serverManager = null;
        }

        logger.info("Creating new ServerManager instance and rebooting...");
        serverManager = new ServerManager();

        int duration = cfg.getInt("rebootDelay", 10000);
        logger.info("Systems going back up in %d".formatted(duration));
        try {
            Thread.sleep(duration);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        serverManager.Start();
    }

}