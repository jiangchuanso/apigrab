package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.service.DataPreviewService;
import com.gitee.apigrab.admin.service.ExecutionLogService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ExecutionLogControllerTest {

    private ExecutionLogService mockService = mock(ExecutionLogService.class);
    private DataPreviewService mockPreviewService = mock(DataPreviewService.class);
    private ExecutionLogController controller = new ExecutionLogController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = ExecutionLogController.class.getDeclaredField("executionLogService");
        field.setAccessible(true);
        field.set(controller, mockService);
        java.lang.reflect.Field previewField = ExecutionLogController.class.getDeclaredField("dataPreviewService");
        previewField.setAccessible(true);
        previewField.set(controller, mockPreviewService);
    }

    @Test
    void testGetById() throws Exception {
        injectService();
        ExecutionLogEntity entity = new ExecutionLogEntity();
        entity.setId(1L);
        entity.setStatus(ExecutionStatus.SUCCESS);
        entity.setTotalRecords(500);
        when(mockService.getById(1L)).thenReturn(entity);

        ResultEntity<ExecutionLogEntity> result = controller.getById(1L);
        assertTrue(result.isSuccess());
        assertEquals(ExecutionStatus.SUCCESS, result.getData().getStatus());
        assertEquals(500, result.getData().getTotalRecords());
    }

    @Test
    void testGetByIdNotFound() throws Exception {
        injectService();
        when(mockService.getById(999L)).thenReturn(null);

        ResultEntity<ExecutionLogEntity> result = controller.getById(999L);
        assertTrue(result.isSuccess());
        assertNull(result.getData());
    }

    @Test
    void testListStages() throws Exception {
        injectService();
        ExecutionStageLogEntity stage = new ExecutionStageLogEntity();
        stage.setExecutionId(1L);
        stage.setStage(Stage.WRITE);
        stage.setFailCount(1);
        when(mockService.listStages(1L)).thenReturn(Collections.singletonList(stage));

        ResultEntity<List<ExecutionStageLogEntity>> result = controller.listStages(1L);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals(Stage.WRITE, result.getData().get(0).getStage());
    }

    @Test
    void testPreviewData() throws Exception {
        injectService();
        Map<String, Object> row = new java.util.LinkedHashMap<>();
        row.put("id", 1);
        when(mockPreviewService.preview(1L, 100)).thenReturn(Collections.singletonList(row));

        ResultEntity<List<Map<String, Object>>> result = controller.previewData(1L, 100);

        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }
}
