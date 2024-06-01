package moe.hiktal.yukinet.job;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.service.Console;
import org.apache.logging.log4j.core.util.CronExpression;
import org.bukkit.configuration.ConfigurationSection;

import java.util.List;

public class Job {
    @Getter
    private final String name;
    @Getter
    private final CronExpression time;
    @Getter
    private final List<String> commands;

    public Job(String name, ConfigurationSection cfg) {
        this.name = name;
        try {
            time = new CronExpression(cfg.getString("time"));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }

        commands = cfg.getStringList("cmd");
        if (commands.isEmpty())
            throw new IllegalStateException("Attepted to register job '%s' without any commands!".formatted(name));
    }

    public void Execute() {
        for (String cmd : commands) {
            try {
                Console.getInstance().ExecuteCommand(cmd);
            } catch (Exception e) {
                YukiNet.getLogger().error("Job executed interrupted due to exception on command '%s':".formatted(cmd));
                e.printStackTrace();
                return;
            }
        }

        YukiNet.getLogger().info("Successfully ran job %s".formatted(name));
    }
}
