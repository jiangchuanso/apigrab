package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.service.DashboardService;
import com.gitee.apigrab.common.model.ResultEntity;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardControllerTest {

    private DashboardService mockService = mock(DashboardService.class);
    private DashboardController controller = new DashboardController();

    private void injectService() throws Exception {
        java.lang.reflect.Field field = DashboardController.class.getDeclaredField("dashboardService");
        field.setAccessible(true);
        field.set(controller, mockService);
    }

    @Test
    void testStatistics() throws Exception {
        injectService();
        Map<String, Object> stats = new HashMap<>();
        stats.put("apiConfigCount", 10);
        stats.put("datasourceCount", 5);
        stats.put("taskCount", 20);
        stats.put("successRate", 95);
        when(mockService.getStatistics()).thenReturn(stats);

        ResultEntity<Map<String, Object>> result = controller.statistics();
        assertTrue(result.isSuccess());
        assertEquals(10, result.getData().get("apiConfigCount"));
        assertEquals(5, result.getData().get("datasourceCount"));
        assertEquals(20, result.getData().get("taskCount"));
        assertEquals(95, result.getData().get("successRate"));
    }
}
