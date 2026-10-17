package com.gitee.apigrab.admin.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class TaskSaveRequestDeserializeTest {

    private static final String PAYLOAD = "{"
        + "\"task\":{\"name\":\"aaa\",\"apiConfigId\":1,\"datasourceId\":1,\"targetSchema\":\"TEST\","
        + "\"targetTable\":\"T_RC_DATA\",\"writeMode\":\"INSERT\",\"mergeKeys\":\"\",\"autoCreateTable\":1,"
        + "\"batchSize\":500,\"retryTimes\":0,\"timeoutMs\":0,\"alarmEnabled\":0,\"scheduleType\":\"MANUAL\","
        + "\"cronExpression\":\"\",\"status\":0,\"remark\":\"\"},"
        + "\"mappings\":[{\"sourceField\":\"$.cityid\",\"targetField\":\"cityid\",\"targetType\":\"BIGINT\","
        + "\"targetLength\":255,\"nullable\":1,\"defaultValue\":\"\",\"convertExpr\":\"\",\"sortOrder\":0}]}";

    @Test
    void deserializesTaskSaveRequest() throws Exception {
        ObjectMapper mapper = new ObjectMapper();
        TaskSaveRequest request = mapper.readValue(PAYLOAD, TaskSaveRequest.class);

        assertNotNull(request.getTask());
        assertEquals("aaa", request.getTask().getName());
        assertEquals(WriteMode.INSERT, request.getTask().getWriteMode());
        assertEquals(ScheduleType.MANUAL, request.getTask().getScheduleType());
        assertEquals(TaskStatus.NOT_STARTED, request.getTask().getStatus());
        assertEquals(1, request.getMappings().size());
        assertEquals(1, request.getMappings().get(0).getNullable());
    }
}
