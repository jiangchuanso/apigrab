package com.gitee.apigrab.core.alarm;

import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class AlarmModelUtilsTest {

    @Test
    void testBuildAlarmModel() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("测试任务");
        task.setApiConfigId(10L);
        task.setDatasourceId(20L);

        Exception exception = new RuntimeException("Connection timeout");

        java.util.Map<String, String> model = AlarmModelUtils.buildAlarmModel(task, exception);
        assertEquals("测试任务", model.get("taskName"));
        assertEquals("1", model.get("taskId"));
        assertEquals("10", model.get("apiConfigId"));
        assertEquals("20", model.get("datasourceId"));
        assertEquals("Connection timeout", model.get("exception"));
        assertEquals("java.lang.RuntimeException", model.get("exceptionType"));
        assertNotNull(model.get("accessTime"));
    }

    @Test
    void testGetExampleModel() {
        java.util.Map<String, String> example = AlarmModelUtils.getExampleModel();
        assertEquals("订单数据采集", example.get("taskName"));
        assertNotNull(example.get("exception"));
        assertNotNull(example.get("accessTime"));
    }
}
