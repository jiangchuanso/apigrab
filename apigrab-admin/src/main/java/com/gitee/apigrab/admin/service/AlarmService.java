package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.core.alarm.UnifyAlarmOpsService;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class AlarmService {

    @Resource
    private UnifyAlarmOpsService unifyAlarmOpsService;

    public AlarmConfigEntity getConfig() {
        AlarmConfigEntity config = unifyAlarmOpsService.getConfig();
        if (config == null) {
            config = new AlarmConfigEntity();
            config.setStatus(EnableStatus.DISABLED);
        }
        return config;
    }

    public void saveConfig(AlarmConfigEntity entity) {
        unifyAlarmOpsService.saveConfig(entity);
    }

    public void testAlarm(Map<String, String> dataModel) {
        AlarmConfigEntity config = unifyAlarmOpsService.getConfig();
        if (config != null) {
            unifyAlarmOpsService.testAlarm(config, dataModel);
        }
    }
}
