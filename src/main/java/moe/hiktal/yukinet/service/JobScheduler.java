package moe.hiktal.yukinet.service;

import lombok.Getter;
import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.job.Job;
import org.bukkit.configuration.file.YamlConfiguration;

import java.util.HashMap;
import java.util.Map;
import java.util.Timer;

public class JobScheduler {
    @Getter
    private static JobScheduler instance;
    @Getter
    private final Map<String, Job> jobs = new HashMap<>();
    @Getter
    private final YamlConfiguration data;
    private final Timer timer = new Timer();

    public JobScheduler(YamlConfiguration data) {
        instance = this;
        this.data = data;

        LoadJobs();

        timer.schedule(new JobSchedulerTask(), 0, 1000);
    }

    public void LoadJobs() {
        jobs.clear();

        YukiNet.getLogger().info("Loading jobs...");
        for (String key : data.getKeys(false)) {
            if (key.startsWith("_")) {
                YukiNet.getLogger().info("Skipping %s (name starts with underscore)".formatted(key));
                continue;
            }

            try {
                Job job = new Job(key, data.getConfigurationSection(key));
                jobs.put(key, job);
                YukiNet.getLogger().info("Loaded job %s".formatted(key));
            } catch (Exception e) {
                YukiNet.getLogger().error("An exception occured while loading the job %s".formatted(key));
                e.printStackTrace();
            }
        }

        YukiNet.getLogger().info("Loaded %s jobs".formatted(jobs.size()));
    }
}
