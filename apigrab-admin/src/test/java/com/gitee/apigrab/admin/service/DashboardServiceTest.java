package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.persistence.dao.ApiConfigDao;
import com.gitee.apigrab.persistence.dao.CollectTaskDao;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DashboardServiceTest {

    @Mock private ApiConfigDao apiConfigDao;
    @Mock private DatasourceConfigDao datasourceConfigDao;
    @Mock private CollectTaskDao collectTaskDao;
    @Mock private ExecutionLogDao executionLogDao;
    @InjectMocks private DashboardService service;

    @Test
    void testGetStatisticsWithData() {
        when(apiConfigDao.list(any(ApiConfigEntity.class)))
            .thenReturn(Arrays.asList(new ApiConfigEntity(), new ApiConfigEntity()));
        when(datasourceConfigDao.list(any(DatasourceConfigEntity.class)))
            .thenReturn(Collections.singletonList(new DatasourceConfigEntity()));

        CollectTaskEntity runningTask = new CollectTaskEntity();
        runningTask.setId(1L);
        runningTask.setStatus(TaskStatus.RUNNING);
        when(collectTaskDao.list(any(CollectTaskEntity.class)))
            .thenReturn(Arrays.asList(runningTask, new CollectTaskEntity())) // all tasks
            .thenReturn(Collections.singletonList(runningTask));             // running tasks

        ExecutionLogEntity successLog = new ExecutionLogEntity();
        successLog.setStatus(ExecutionStatus.SUCCESS);
        successLog.setTotalRecords(500);
        ExecutionLogEntity failedLog = new ExecutionLogEntity();
        failedLog.setStatus(ExecutionStatus.FAILED);
        failedLog.setTotalRecords(100);
        ExecutionLogEntity runningLog = new ExecutionLogEntity();
        runningLog.setStatus(ExecutionStatus.RUNNING);
        ExecutionLogEntity partialLog = new ExecutionLogEntity();
        partialLog.setStatus(ExecutionStatus.PARTIAL);
        partialLog.setTotalRecords(50);
        when(executionLogDao.list(any(ExecutionLogEntity.class)))
            .thenReturn(Arrays.asList(successLog, failedLog, runningLog, partialLog));

        Map<String, Object> stats = service.getStatistics();
        assertEquals(2, stats.get("apiConfigCount"));
        assertEquals(1, stats.get("datasourceCount"));
        assertEquals(2, stats.get("taskCount"));
        assertEquals(1, stats.get("runningTaskCount"));
        assertEquals(4, stats.get("totalExecutions"));
        assertEquals(1, stats.get("successCount"));
        assertEquals(1, stats.get("failedCount"));
        assertEquals(1, stats.get("runningCount"));
        assertEquals(1, stats.get("partialCount"));
        assertEquals(650, stats.get("totalRecords"));
        // 成功率仅按已结束执行（1 成功 / 3 结束）计算，排除运行中
        assertEquals(33.3, stats.get("successRate"));
    }

    @Test
    void testGetStatisticsEmpty() {
        when(apiConfigDao.list(any(ApiConfigEntity.class))).thenReturn(Collections.emptyList());
        when(datasourceConfigDao.list(any(DatasourceConfigEntity.class))).thenReturn(Collections.emptyList());
        when(collectTaskDao.list(any(CollectTaskEntity.class)))
            .thenReturn(Collections.emptyList())
            .thenReturn(Collections.emptyList());
        when(executionLogDao.list(any(ExecutionLogEntity.class))).thenReturn(Collections.emptyList());

        Map<String, Object> stats = service.getStatistics();
        assertEquals(0, stats.get("taskCount"));
        assertEquals(0, stats.get("totalExecutions"));
        assertEquals(0, stats.get("successCount"));
        assertEquals(0.0, stats.get("successRate"));
    }
}
