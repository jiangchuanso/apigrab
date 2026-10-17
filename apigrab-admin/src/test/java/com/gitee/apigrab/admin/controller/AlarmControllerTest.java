package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.AlarmConfigRequest;
import com.gitee.apigrab.admin.service.AlarmService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlarmControllerTest {

    private AlarmService mockService = mock(AlarmService.class);
    private AlarmController controller = new AlarmController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = AlarmController.class.getDeclaredField("alarmService");
        field.setAccessible(true);
        field.set(controller, mockService);
    }

    @Test
    void testGetConfig() throws Exception {
        injectService();
        AlarmConfigEntity config = new AlarmConfigEntity();
        config.setId(1L);
        config.setEndpoint("https://webhook.example.com");
        when(mockService.getConfig()).thenReturn(config);

        ResultEntity<AlarmConfigEntity> result = controller.getConfig();
        assertTrue(result.isSuccess());
        assertEquals("https://webhook.example.com", result.getData().getEndpoint());
    }

    @Test
    void testSaveConfig() throws Exception {
        injectService();
        AlarmConfigRequest request = new AlarmConfigRequest();
        request.setEndpoint("https://new-webhook.example.com");
        request.setInputTemplate("{\"text\":\"告警\"}");

        ResultEntity<Void> result = controller.saveConfig(request);
        assertTrue(result.isSuccess());

        ArgumentCaptor<AlarmConfigEntity> captor = ArgumentCaptor.forClass(AlarmConfigEntity.class);
        verify(mockService).saveConfig(captor.capture());
        assertEquals("https://new-webhook.example.com", captor.getValue().getEndpoint());
        assertEquals("{\"text\":\"告警\"}", captor.getValue().getInputTemplate());
    }

    @Test
    void testTestAlarm() throws Exception {
        injectService();
        Map<String, String> dataModel = new HashMap<>();
        dataModel.put("taskName", "测试任务");
        dataModel.put("status", "FAILED");

        ResultEntity<Void> result = controller.testAlarm(dataModel);
        assertTrue(result.isSuccess());
        verify(mockService).testAlarm(dataModel);
    }

    @Test
    void testGetExample() throws Exception {
        injectService();
        ResultEntity<Map<String, String>> result = controller.getExample();
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        // AlarmModelUtils.getExampleModel() 应返回非空 Map
        assertTrue(result.getData().size() > 0);
    }
}
