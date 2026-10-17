package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.AlarmConfigRequest;
import com.gitee.apigrab.admin.service.AlarmService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.core.alarm.AlarmModelUtils;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.Map;

@RestController
@RequestMapping("/api/alarm")
public class AlarmController {

    @Resource
    private AlarmService alarmService;

    @GetMapping("/config")
    public ResultEntity<AlarmConfigEntity> getConfig() {
        return ResultEntity.success(alarmService.getConfig());
    }

    @PostMapping("/config")
    public ResultEntity<Void> saveConfig(@RequestBody AlarmConfigRequest request) {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        BeanUtils.copyProperties(request, entity);
        alarmService.saveConfig(entity);
        return ResultEntity.success();
    }

    /**
     * The alarm data model is an inherently dynamic key-value map (alarm template
     * variables), so it intentionally remains a raw map rather than a fixed class.
     */
    @PostMapping("/test")
    public ResultEntity<Void> testAlarm(@RequestBody Map<String, String> dataModel) {
        alarmService.testAlarm(dataModel);
        return ResultEntity.success();
    }

    @GetMapping("/example")
    public ResultEntity<Map<String, String>> getExample() {
        return ResultEntity.success(AlarmModelUtils.getExampleModel());
    }
}
