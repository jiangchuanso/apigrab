package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.core.alarm.UnifyAlarmOpsService;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlarmServiceTest {

    @Mock private UnifyAlarmOpsService unifyAlarmOpsService;
    @InjectMocks private AlarmService service;

    @Test
    void testGetConfig() {
        AlarmConfigEntity config = new AlarmConfigEntity();
        config.setStatus(EnableStatus.ENABLED);
        config.setEndpoint("https://hooks.example.com");
        when(unifyAlarmOpsService.getConfig()).thenReturn(config);

        AlarmConfigEntity result = service.getConfig();
        assertEquals(EnableStatus.ENABLED, result.getStatus());
        assertEquals("https://hooks.example.com", result.getEndpoint());
    }

    @Test
    void testGetConfigReturnsDefaultWhenNull() {
        when(unifyAlarmOpsService.getConfig()).thenReturn(null);
        AlarmConfigEntity result = service.getConfig();
        assertEquals(EnableStatus.DISABLED, result.getStatus());
        assertNull(result.getEndpoint());
    }

    @Test
    void testSaveConfig() {
        AlarmConfigEntity config = new AlarmConfigEntity();
        config.setStatus(EnableStatus.ENABLED);
        service.saveConfig(config);
        verify(unifyAlarmOpsService, times(1)).saveConfig(config);
    }

    @Test
    void testTestAlarm() {
        AlarmConfigEntity config = new AlarmConfigEntity();
        config.setStatus(EnableStatus.ENABLED);
        when(unifyAlarmOpsService.getConfig()).thenReturn(config);

        service.testAlarm(Collections.singletonMap("key", "value"));
        verify(unifyAlarmOpsService, times(1)).testAlarm(config, Collections.singletonMap("key", "value"));
    }

    @Test
    void testTestAlarmWhenConfigNull() {
        when(unifyAlarmOpsService.getConfig()).thenReturn(null);
        service.testAlarm(Collections.singletonMap("key", "value"));
        verify(unifyAlarmOpsService, never()).testAlarm(any(), any());
    }
}
