package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.service.DatasourceService;
import com.gitee.apigrab.admin.dto.request.DatasourceRequest;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DatasourceControllerTest {

    private DatasourceService mockService = mock(DatasourceService.class);
    private DatasourceController controller = new DatasourceController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = DatasourceController.class.getDeclaredField("datasourceService");
        field.setAccessible(true);
        field.set(controller, mockService);
    }

    @Test
    void testGetById() throws Exception {
        injectService();
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        entity.setId(1L);
        entity.setName("测试数据源");
        when(mockService.getById(1L)).thenReturn(entity);

        ResultEntity<DatasourceConfigEntity> result = controller.getById(1L);
        assertTrue(result.isSuccess());
        assertEquals("测试数据源", result.getData().getName());
    }

    @Test
    void testCreate() throws Exception {
        injectService();
        DatasourceRequest request = new DatasourceRequest();
        request.setName("新数据源");

        ResultEntity<Void> result = controller.create(request);
        assertTrue(result.isSuccess());
        ArgumentCaptor<DatasourceConfigEntity> captor = ArgumentCaptor.forClass(DatasourceConfigEntity.class);
        verify(mockService).create(captor.capture());
        assertEquals("新数据源", captor.getValue().getName());
    }

    @Test
    void testUpdate() throws Exception {
        injectService();
        DatasourceRequest request = new DatasourceRequest();
        request.setName("更新数据源");

        ResultEntity<Void> result = controller.update(1L, request);
        assertTrue(result.isSuccess());
        ArgumentCaptor<DatasourceConfigEntity> captor = ArgumentCaptor.forClass(DatasourceConfigEntity.class);
        verify(mockService).update(captor.capture());
        assertEquals("更新数据源", captor.getValue().getName());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    void testDelete() throws Exception {
        injectService();
        ResultEntity<Void> result = controller.delete(1L);
        assertTrue(result.isSuccess());
        verify(mockService).delete(1L);
    }

    @Test
    void testTestConnectionSuccess() throws Exception {
        injectService();
        when(mockService.testConnection(1L)).thenReturn(true);

        ResultEntity<String> result = controller.testConnection(1L);
        assertTrue(result.isSuccess());
        assertEquals("连接成功", result.getData());
    }

    @Test
    void testTestConnectionFailed() throws Exception {
        injectService();
        when(mockService.testConnection(1L)).thenReturn(false);

        ResultEntity<String> result = controller.testConnection(1L);
        assertFalse(result.isSuccess());
    }

    @Test
    void testTestConnectionTransientSuccess() throws Exception {
        injectService();
        DatasourceRequest request = new DatasourceRequest();
        request.setJdbcUrl("jdbc:mysql://localhost:3306/test");
        when(mockService.testConnection(any(DatasourceConfigEntity.class))).thenReturn(true);

        ResultEntity<String> result = controller.testConnectionTransient(request);
        assertTrue(result.isSuccess());
        assertEquals("连接成功", result.getData());
    }

    @Test
    void testTestConnectionTransientFailed() throws Exception {
        injectService();
        DatasourceRequest request = new DatasourceRequest();
        when(mockService.testConnection(any(DatasourceConfigEntity.class))).thenReturn(false);

        ResultEntity<String> result = controller.testConnectionTransient(request);
        assertFalse(result.isSuccess());
    }

    @Test
    void testGetSchemas() throws Exception {
        injectService();
        when(mockService.getSchemas(1L)).thenReturn(Arrays.asList("db1", "db2"));

        ResultEntity<List<String>> result = controller.getSchemas(1L);
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
    }

    @Test
    void testGetTablesWithSchema() throws Exception {
        injectService();
        when(mockService.getTables(1L, "db1")).thenReturn(Arrays.asList("table1", "table2"));

        ResultEntity<List<String>> result = controller.getTables(1L, "db1");
        assertTrue(result.isSuccess());
        assertEquals(2, result.getData().size());
        verify(mockService).getTables(1L, "db1");
    }

    @Test
    void testGetColumns() throws Exception {
        injectService();
        List<Map<String, String>> columns = new ArrayList<>();
        Map<String, String> col = new HashMap<>();
        col.put("name", "id");
        col.put("type", "BIGINT");
        columns.add(col);
        when(mockService.getColumns(1L, "db1", "t_data")).thenReturn(columns);

        ResultEntity<List<Map<String, String>>> result = controller.getColumns(1L, "db1", "t_data");
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }
}
