package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.ApiConfigListQuery;
import com.gitee.apigrab.admin.dto.request.ApiConfigRequest;
import com.gitee.apigrab.admin.dto.request.DebugScriptRequest;
import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.dto.request.PreviewMappingRequest;
import com.gitee.apigrab.admin.service.ApiConfigService;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

// Using pure Mockito for controller test (no Spring context) due to PageHelper dependency

class ApiConfigControllerTest {

    private ApiConfigController newController(ApiConfigService mockService) throws Exception {
        ApiConfigController controller = new ApiConfigController();
        java.lang.reflect.Field field = ApiConfigController.class.getDeclaredField("apiConfigService");
        field.setAccessible(true);
        field.set(controller, mockService);
        return controller;
    }

    @Test
    void testControllerGetById() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigEntity entity = new ApiConfigEntity();
        entity.setId(1L);
        entity.setName("测试接口");
        when(mockService.getById(1L)).thenReturn(entity);

        ResultEntity<ApiConfigEntity> result = controller.getById(1L);
        assertTrue(result.isSuccess());
        assertEquals("测试接口", result.getData().getName());
    }

    @Test
    void testControllerListConvertsStatus() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);
        when(mockService.list(any())).thenReturn(Collections.emptyList());

        ApiConfigListQuery query = new ApiConfigListQuery();
        query.setName("abc");
        query.setStatus(1);

        ResultEntity<?> result = controller.list(query);
        assertTrue(result.isSuccess());

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(mockService).list(captor.capture());
        assertEquals("abc", captor.getValue().getName());
        assertEquals(EnableStatus.ENABLED, captor.getValue().getStatus());
    }

    @Test
    void testControllerListRejectsInvalidStatus() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigListQuery query = new ApiConfigListQuery();
        query.setStatus(9);
        assertThrows(CommonException.class, () -> controller.list(query));
        verify(mockService, never()).list(any());
    }

    @Test
    void testControllerCreate() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigRequest request = new ApiConfigRequest();
        request.setName("新接口");

        ResultEntity<Void> result = controller.create(request);
        assertTrue(result.isSuccess());

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(mockService).create(captor.capture());
        assertEquals("新接口", captor.getValue().getName());
    }

    @Test
    void testControllerDelete() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ResultEntity<Void> result = controller.delete(1L);
        assertTrue(result.isSuccess());
        verify(mockService).delete(1L);
    }

    @Test
    void testControllerTestApi() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("success", true);
        testResult.put("rawResponse", "{}");
        when(mockService.testApi(1L)).thenReturn(testResult);

        ResultEntity<Map<String, Object>> result = controller.test(1L, null);
        assertTrue(result.isSuccess());
        assertTrue((Boolean) result.getData().get("success"));
    }

    @Test
    void testControllerTestWithBodyUsesBodyConfig() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigRequest request = new ApiConfigRequest();
        request.setUrl("https://api.example.com/live");

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("success", true);
        when(mockService.testConfig(any())).thenReturn(testResult);

        ResultEntity<Map<String, Object>> result = controller.test(1L, request);
        assertTrue(result.isSuccess());

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(mockService).testConfig(captor.capture());
        assertEquals("https://api.example.com/live", captor.getValue().getUrl());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    void testControllerTestTransient() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigRequest request = new ApiConfigRequest();
        request.setName("未保存接口");

        Map<String, Object> testResult = new HashMap<>();
        testResult.put("success", true);
        when(mockService.testConfig(any())).thenReturn(testResult);

        ResultEntity<Map<String, Object>> result = controller.testTransient(request);
        assertTrue(result.isSuccess());
        assertTrue((Boolean) result.getData().get("success"));
    }

    @Test
    void testControllerDebugScript() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        List<Map<String, Object>> sampleData = Collections.singletonList(Collections.singletonMap("a", 1));
        when(mockService.debugScript(anyLong(), anyString(), anyList())).thenReturn(sampleData);

        DebugScriptRequest request = new DebugScriptRequest();
        request.setScript("return records");
        request.setSampleData(sampleData);

        ResultEntity<List<Map<String, Object>>> result = controller.debugScript(1L, request);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
    }

    @Test
    void testControllerUpdate() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        ApiConfigRequest request = new ApiConfigRequest();
        request.setName("更新接口");

        ResultEntity<Void> result = controller.update(1L, request);
        assertTrue(result.isSuccess());

        ArgumentCaptor<ApiConfigEntity> captor = ArgumentCaptor.forClass(ApiConfigEntity.class);
        verify(mockService).update(captor.capture());
        assertEquals("更新接口", captor.getValue().getName());
        assertEquals(1L, captor.getValue().getId());
    }

    @Test
    void testControllerPreviewMapping() throws Exception {
        ApiConfigService mockService = mock(ApiConfigService.class);
        ApiConfigController controller = newController(mockService);

        FieldMappingInput mapping = new FieldMappingInput();
        mapping.setSourceField("$.name");
        mapping.setTargetField("user_name");
        List<FieldMappingInput> mappings = Collections.singletonList(mapping);
        List<Map<String, Object>> sample = Collections.singletonList(Collections.singletonMap("name", "Alice"));

        List<Map<String, Object>> preview = Collections.singletonList(Collections.singletonMap("user_name", "Alice"));
        when(mockService.previewMapping(eq(1L), any(), eq(sample))).thenReturn(preview);

        PreviewMappingRequest request = new PreviewMappingRequest();
        request.setMappings(mappings);
        request.setSampleData(sample);

        ResultEntity<List<Map<String, Object>>> result = controller.previewMapping(1L, request);
        assertTrue(result.isSuccess());
        assertEquals(1, result.getData().size());
        assertEquals("Alice", result.getData().get(0).get("user_name"));

        ArgumentCaptor<List<FieldMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockService).previewMapping(eq(1L), captor.capture(), eq(sample));
        assertEquals("user_name", captor.getValue().get(0).getTargetField());
    }
}
