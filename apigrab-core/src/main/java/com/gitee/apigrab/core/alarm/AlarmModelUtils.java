package com.gitee.apigrab.core.alarm;

import cn.hutool.core.date.DateUtil;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;

import java.util.HashMap;
import java.util.Map;

public class AlarmModelUtils {

    public static Map<String, String> buildAlarmModel(
            CollectTaskEntity task, Exception exception) {
        Map<String, String> model = new HashMap<>();
        model.put("taskName", task.getName());
        model.put("taskId", task.getId().toString());
        model.put("apiConfigId", task.getApiConfigId().toString());
        model.put("datasourceId", task.getDatasourceId().toString());
        model.put("exception", exception.getMessage());
        model.put("exceptionType", exception.getClass().getName());
        model.put("accessTime", DateUtil.now());
        return model;
    }

    public static Map<String, String> getExampleModel() {
        Map<String, String> model = new HashMap<>();
        model.put("taskName", "订单数据采集");
        model.put("exception", "Connection timeout to api.example.com");
        model.put("accessTime", "2025-01-01 12:00:00");
        return model;
    }
}
