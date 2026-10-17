package com.gitee.apigrab.admin.scheduler;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.core.collector.CollectorEngine;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.Job;
import org.quartz.JobExecutionContext;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@DisallowConcurrentExecution
@Component
@Slf4j
public class CollectJob implements Job {

    @Resource
    private CollectorEngine collectorEngine;

    @Override
    public void execute(JobExecutionContext context) {
        Long taskId = context.getJobDetail().getJobDataMap().getLong("taskId");
        log.info("Quartz triggered task: {}", taskId);
        try {
            collectorEngine.executeByTaskId(taskId);
            log.info("Quartz task {} completed", taskId);
        } catch (Exception e) {
            log.error("Quartz task {} failed", taskId, e);
        }
    }
}
