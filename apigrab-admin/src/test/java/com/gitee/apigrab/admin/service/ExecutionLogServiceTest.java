package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.dao.ExecutionStageLogDao;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExecutionLogServiceTest {

    @Mock private ExecutionLogDao executionLogDao;
    @Mock private ExecutionStageLogDao executionStageLogDao;
    @InjectMocks private ExecutionLogService service;

    private ExecutionLogEntity log;

    @BeforeEach
    void setUp() {
        log = new ExecutionLogEntity();
        log.setId(1L);
        log.setTaskId(1L);
        log.setStatus(ExecutionStatus.SUCCESS);
        log.setTotalRecords(100);
    }

    @Test
    void testGetById() {
        when(executionLogDao.getById(1L)).thenReturn(log);
        ExecutionLogEntity result = service.getById(1L);
        assertEquals(ExecutionStatus.SUCCESS, result.getStatus());
        assertEquals(100, result.getTotalRecords());
    }

    @Test
    void testGetByIdNotFound() {
        when(executionLogDao.getById(999L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.getById(999L));
    }

    @Test
    void testList() {
        when(executionLogDao.list(any())).thenReturn(Collections.singletonList(log));
        List<ExecutionLogEntity> result = service.list(new ExecutionLogEntity());
        assertEquals(1, result.size());
    }

    @Test
    void testListStages() {
        ExecutionStageLogEntity stage = new ExecutionStageLogEntity();
        stage.setExecutionId(1L);
        stage.setStage(Stage.WRITE);
        when(executionStageLogDao.getByExecutionId(1L)).thenReturn(Collections.singletonList(stage));

        List<ExecutionStageLogEntity> result = service.listStages(1L);

        assertEquals(1, result.size());
        assertEquals(Stage.WRITE, result.get(0).getStage());
    }
}
