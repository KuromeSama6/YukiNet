package moe.hiktal.yukinet.service;

import moe.hiktal.yukinet.YukiNet;
import moe.hiktal.yukinet.job.Job;

import java.util.Date;
import java.util.TimerTask;

public class JobSchedulerTask extends TimerTask {
    @Override
    public void run() {
        for (Job job : JobScheduler.getInstance().getJobs().values()) {
            if(job.getTime().isSatisfiedBy(new Date())) {
                try {
                    job.Execute();
                } catch (Exception e) {
                    YukiNet.getLogger().error("An error occured whilst executing job %s:".formatted(job.getName()));
                    e.printStackTrace();
                }
            }
        }
    }
}
