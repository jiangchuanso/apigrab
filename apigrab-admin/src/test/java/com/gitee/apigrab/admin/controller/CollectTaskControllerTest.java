package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.TaskDetailResponse;
import com.gitee.apigrab.admin.dto.TaskExecuteResponse;
import com.gitee.apigrab.admin.dto.TaskSaveRequest;
import com.gitee.apigrab.admin.dto.request.TaskInput;
import com.gitee.apigrab.admin.dto.request.TaskListQuery;
import com.gitee.apigrab.admin.service.CollectTaskService;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.model.PageResult;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class CollectTaskControllerTest {

    private CollectTaskService mockService = mock(CollectTaskService.class);
    private CollectTaskController controller = new CollectTaskController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = CollectTaskController.class.getDeclaredField("collectTaskService");
        field.setAccessible(true);
        field.set(controller, mockService);
    }

    @Test
    void testGetById() throws Exception {
        injectService();
        CollectTaskEntity entity = new CollectTaskEntity();
        entity.setId(1L);
        entity.setName("测试任务");
        ApiConfigEntity apiConfig = new ApiConfigEntity();
        apiConfig.setId(10L);
        apiConfig.setUrl("https://api.example.com/x");
        when(mockService.getDetail(1L)).thenReturn(new TaskDetailResponse(entity, apiConfig, Collections.emptyList()));

        ResultEntity<TaskDetailResponse> result = controller.getById(1L);
        assertTrue(result.isSuccess());
        assertEquals("测试任务", result.getData().getTask().getName());
        assertEquals("https://api.example.com/x", result.getData().getApiConfig().getUrl());
    }

    @Test
    void testListMapsStatusFilterToEntity() throws Exception {
        injectService();
        when(mockService.list(any())).thenReturn(Collections.emptyList());
        TaskListQuery query = new TaskListQuery();
        query.setStatus(1);
        query.setName("abc");
        query.setDatasourceId(20L);

        ResultEntity<PageResult<CollectTaskEntity>> result = controller.list(query);

        assertTrue(result.isSuccess());
        ArgumentCaptor<CollectTaskEntity> captor = ArgumentCaptor.forClass(CollectTaskEntity.class);
        verify(mockService).list(captor.capture());
        assertEquals(TaskStatus.RUNNING, captor.getValue().getStatus());
        assertEquals("abc", captor.getValue().getName());
        assertEquals(20L, captor.getValue().getDatasourceId());
    }

    @Test
    void testListRejectsInvalidStatus() throws Exception {
        injectService();
        TaskListQuery query = new TaskListQuery();
        query.setStatus(9);

        assertThrows(CommonException.class, () -> controller.list(query));
        verify(mockService, never()).list(any());
    }

    @Test
    void testCreate() throws Exception {
        injectService();
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput task = new TaskInput();
        task.setName("新任务");
        request.setTask(task);

        ResultEntity<Void> result = controller.create(request);
        assertTrue(result.isSuccess());
        verify(mockService).createFromRequest(request);
    }

    @Test
    void testUpdate() throws Exception {
        injectService();
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput task = new TaskInput();
        task.setName("更新任务");
        request.setTask(task);

        ResultEntity<Void> result = controller.update(1L, request);
        assertTrue(result.isSuccess());
        verify(mockService).updateFromRequest(1L, request);
    }

    @Test
    void testDelete() throws Exception {
        injectService();
        ResultEntity<Void> result = controller.delete(1L);
        assertTrue(result.isSuccess());
        verify(mockService).delete(1L);
    }

    @Test
    void testExecute() throws Exception {
        injectService();
        when(mockService.executeAsync(1L)).thenReturn(42L);

        ResultEntity<TaskExecuteResponse> result = controller.execute(1L);
        assertTrue(result.isSuccess());
        assertEquals(42L, result.getData().getExecutionId());
    }

    @Test
    void testPause() throws Exception {
        injectService();
        ResultEntity<Void> result = controller.pause(1L);
        assertTrue(result.isSuccess());
        verify(mockService).pause(1L);
    }

    @Test
    void testResume() throws Exception {
        injectService();
        ResultEntity<Void> result = controller.resume(1L);
        assertTrue(result.isSuccess());
        verify(mockService).resume(1L);
    }

    @Test
    void testPreview() throws Exception {
        injectService();
        List<Map<String, Object>> previewData = new ArrayList<>();
        Map<String, Object> row = new HashMap<>();
        row.put("id", 1);
        previewData.add(row);
        when(mockService.preview(1L)).thenReturn(previewData);

        ResultEntity<List<Map<String, Object>>> result = controller.preview(1L);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }
}
