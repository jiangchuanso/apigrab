package com.gitee.apigrab.admin.scheduler;

import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.quartz.*;
import org.springframework.context.annotation.Configuration;

import javax.annotation.Resource;

@Configuration
public class QuartzConfig {

    @Resource
    private Scheduler scheduler;

    public void registerTask(CollectTaskEntity task) {
        try {
            JobDataMap dataMap = new JobDataMap();
            dataMap.put("taskId", task.getId());

            JobDetail job = JobBuilder.newJob(CollectJob.class)
                .withIdentity("task_" + task.getId(), "collector")
                .usingJobData(dataMap)
                .storeDurably(false)
                .build();

            CronTrigger trigger = TriggerBuilder.newTrigger()
                .withIdentity("trigger_" + task.getId(), "collector")
                .withSchedule(CronScheduleBuilder.cronSchedule(task.getCronExpression()))
                .build();

            scheduler.scheduleJob(job, trigger);
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to register Quartz task", e);
        }
    }

    public void unregisterTask(Long taskId) {
        try {
            scheduler.deleteJob(JobKey.jobKey("task_" + taskId, "collector"));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to unregister Quartz task", e);
        }
    }

    public void pauseTask(Long taskId) {
        try {
            scheduler.pauseJob(JobKey.jobKey("task_" + taskId, "collector"));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to pause Quartz task", e);
        }
    }

    public void resumeTask(Long taskId) {
        try {
            scheduler.resumeJob(JobKey.jobKey("task_" + taskId, "collector"));
        } catch (SchedulerException e) {
            throw new RuntimeException("Failed to resume Quartz task", e);
        }
    }
}
