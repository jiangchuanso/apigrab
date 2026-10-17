package com.gitee.apigrab.admin.scheduler;

import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.mockito.Mockito.*;

class TaskSchedulerManagerTest {

    @Test
    void testRegisterTask() throws Exception {
        TaskSchedulerManager manager = new TaskSchedulerManager();
        QuartzConfig mockQuartzConfig = Mockito.mock(QuartzConfig.class);

        java.lang.reflect.Field field = TaskSchedulerManager.class.getDeclaredField("quartzConfig");
        field.setAccessible(true);
        field.set(manager, mockQuartzConfig);

        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setCronExpression("0 0 2 * * ?");

        manager.registerTask(task);
        verify(mockQuartzConfig).registerTask(task);
    }

    @Test
    void testUnregisterTask() throws Exception {
        TaskSchedulerManager manager = new TaskSchedulerManager();
        QuartzConfig mockQuartzConfig = Mockito.mock(QuartzConfig.class);

        java.lang.reflect.Field field = TaskSchedulerManager.class.getDeclaredField("quartzConfig");
        field.setAccessible(true);
        field.set(manager, mockQuartzConfig);

        manager.unregisterTask(1L);
        verify(mockQuartzConfig).unregisterTask(1L);
    }

    @Test
    void testPauseTask() throws Exception {
        TaskSchedulerManager manager = new TaskSchedulerManager();
        QuartzConfig mockQuartzConfig = Mockito.mock(QuartzConfig.class);

        java.lang.reflect.Field field = TaskSchedulerManager.class.getDeclaredField("quartzConfig");
        field.setAccessible(true);
        field.set(manager, mockQuartzConfig);

        manager.pauseTask(1L);
        verify(mockQuartzConfig).pauseTask(1L);
    }

    @Test
    void testResumeTask() throws Exception {
        TaskSchedulerManager manager = new TaskSchedulerManager();
        QuartzConfig mockQuartzConfig = Mockito.mock(QuartzConfig.class);

        java.lang.reflect.Field field = TaskSchedulerManager.class.getDeclaredField("quartzConfig");
        field.setAccessible(true);
        field.set(manager, mockQuartzConfig);

        manager.resumeTask(1L);
        verify(mockQuartzConfig).resumeTask(1L);
    }
}
