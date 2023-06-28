package moe.hiktal.yuki.YukiNetSpigot;

import org.bukkit.Bukkit;
import org.bukkit.configuration.file.YamlConfiguration;
import org.bukkit.plugin.java.JavaPlugin;

import java.io.File;
import java.util.logging.Level;

public class Main extends JavaPlugin {
    public YamlConfiguration info;

    @Override
    public void onEnable() {
        getLogger().info("Starting YukiNet.");

        getLogger().info("Reading .yuki.yml");

        File parentDir = getDataFolder().getAbsoluteFile().getParentFile().getParentFile();

        File info = new File(parentDir, "/.yuki-info.yml");
        if (!info.exists()) {
            getLogger().log(Level.SEVERE, String.format(".yuki-info.yml is not found in %s! Perhaps this server is not running under YukiNet?", parentDir.getAbsolutePath()));
            getServer().getPluginManager().disablePlugin(this);
            return;
        }
        this.info = YamlConfiguration.loadConfiguration(info);

        getLogger().info("YukiNet up.");

        if (this.info.getBoolean("global-cfg.ident.enable")) {
            getLogger().info("Your have enabled auto-ident in config.yml under your YukiNet folder. Scheduling auto-ident after sever boot completes.");
            ScheduleAutoident(this.info.getString("global-cfg.ident.cmd"));
        }

    }

    @Override
    public void onDisable(){
        getLogger().info("Disabling YukiNet plugin.");
    }

    private void ScheduleAutoident(String cmd) {
        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            String serverId = this.info.getString("server-id");
            String command = cmd.replace("{}", serverId);

            getLogger().info(String.format("Executing autoident for server %s", serverId));
            getLogger().info(String.format("§dRunning command: %s", command));

            Bukkit.getServer().dispatchCommand(Bukkit.getConsoleSender(), command);
        }, 1);
    }

}
