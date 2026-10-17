package com.gitee.apigrab.admin.scheduler;

import com.gitee.apigrab.core.collector.CollectorEngine;
import com.gitee.apigrab.core.collector.CollectResult;
import org.junit.jupiter.api.Test;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.JobExecutionContext;

import static org.mockito.Mockito.*;

class CollectJobTest {

    @Test
    void testExecuteSuccess() throws Exception {
        CollectJob job = new CollectJob();
        CollectorEngine mockEngine = mock(CollectorEngine.class);

        java.lang.reflect.Field field = CollectJob.class.getDeclaredField("collectorEngine");
        field.setAccessible(true);
        field.set(job, mockEngine);

        CollectResult result = new CollectResult(100, 98, 2, 5);
        when(mockEngine.executeByTaskId(1L)).thenReturn(result);

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("taskId", 1L);

        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(dataMap);

        job.execute(context);
        verify(mockEngine).executeByTaskId(1L);
    }

    @Test
    void testExecuteFailure() throws Exception {
        CollectJob job = new CollectJob();
        CollectorEngine mockEngine = mock(CollectorEngine.class);

        java.lang.reflect.Field field = CollectJob.class.getDeclaredField("collectorEngine");
        field.setAccessible(true);
        field.set(job, mockEngine);

        when(mockEngine.executeByTaskId(1L)).thenThrow(new RuntimeException("DB error"));

        JobExecutionContext context = mock(JobExecutionContext.class);
        JobDetail jobDetail = mock(JobDetail.class);
        JobDataMap dataMap = new JobDataMap();
        dataMap.put("taskId", 1L);

        when(context.getJobDetail()).thenReturn(jobDetail);
        when(jobDetail.getJobDataMap()).thenReturn(dataMap);

        // 不应抛出异常，应内部 catch 处理
        job.execute(context);
        verify(mockEngine).executeByTaskId(1L);
    }
}
