package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.persistence.dao.FieldMappingDao;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.List;

@Service
public class FieldMappingService {

    @Resource
    private FieldMappingDao fieldMappingDao;

    public List<FieldMappingEntity> listByTaskId(Long taskId) {
        return fieldMappingDao.getByTaskId(taskId);
    }

    /**
     * Batch replace all field mappings of a task: delete existing rows then insert the given list.
     * Empty or null input only clears existing mappings.
     *
     * <p>删除与插入必须同事务：插入失败时若删除已提交，任务的字段映射会被整体清空且无法恢复。
     */
    @Transactional
    public void saveMappings(Long taskId, List<FieldMappingEntity> mappings) {
        fieldMappingDao.deleteByTaskId(taskId);
        if (mappings == null || mappings.isEmpty()) {
            return;
        }
        for (FieldMappingEntity mapping : mappings) {
            mapping.setTaskId(taskId);
        }
        fieldMappingDao.batchInsert(mappings);
    }
}
