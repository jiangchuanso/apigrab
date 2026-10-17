package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.service.FieldMappingService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.springframework.beans.BeanUtils;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/field-mappings")
public class FieldMappingController {

    @Resource
    private FieldMappingService fieldMappingService;

    @GetMapping("/task/{taskId}")
    public ResultEntity<List<FieldMappingEntity>> list(@PathVariable Long taskId) {
        return ResultEntity.success(fieldMappingService.listByTaskId(taskId));
    }

    @PostMapping("/task/{taskId}")
    public ResultEntity<Void> save(@PathVariable Long taskId, @RequestBody List<FieldMappingInput> mappings) {
        fieldMappingService.saveMappings(taskId, toEntities(mappings));
        return ResultEntity.success();
    }

    private List<FieldMappingEntity> toEntities(List<FieldMappingInput> mappings) {
        if (mappings == null) {
            return null;
        }
        List<FieldMappingEntity> entities = new ArrayList<>(mappings.size());
        for (FieldMappingInput mapping : mappings) {
            FieldMappingEntity entity = new FieldMappingEntity();
            BeanUtils.copyProperties(mapping, entity);
            entities.add(entity);
        }
        return entities;
    }
}
