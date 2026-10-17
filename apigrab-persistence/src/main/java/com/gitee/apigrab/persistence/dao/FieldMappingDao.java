package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.FieldMappingMapper;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class FieldMappingDao {

    @Resource
    private FieldMappingMapper fieldMappingMapper;

    public List<FieldMappingEntity> getByTaskId(Long taskId) {
        return fieldMappingMapper.selectByTaskId(taskId);
    }

    public int batchInsert(List<FieldMappingEntity> list) {
        return fieldMappingMapper.batchInsert(list);
    }

    public int deleteByTaskId(Long taskId) {
        return fieldMappingMapper.deleteByTaskId(taskId);
    }
}
