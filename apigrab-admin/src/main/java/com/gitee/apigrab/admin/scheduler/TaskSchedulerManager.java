package com.gitee.apigrab.admin.scheduler;

import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class TaskSchedulerManager {

    @Resource
    private QuartzConfig quartzConfig;

    public void registerTask(CollectTaskEntity task) {
        quartzConfig.registerTask(task);
    }

    public void unregisterTask(Long taskId) {
        quartzConfig.unregisterTask(taskId);
    }

    public void pauseTask(Long taskId) {
        quartzConfig.pauseTask(taskId);
    }

    public void resumeTask(Long taskId) {
        quartzConfig.resumeTask(taskId);
    }
}
