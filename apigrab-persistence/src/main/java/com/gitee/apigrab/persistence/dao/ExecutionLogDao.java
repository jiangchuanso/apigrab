package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ExecutionLogMapper;
import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class ExecutionLogDao {

    @Resource
    private ExecutionLogMapper executionLogMapper;

    public ExecutionLogEntity getById(Long id) {
        return executionLogMapper.selectById(id);
    }

    public List<ExecutionLogEntity> list(ExecutionLogEntity param) {
        return executionLogMapper.selectList(param);
    }

    public int insert(ExecutionLogEntity entity) {
        return executionLogMapper.insert(entity);
    }

    public int updateById(ExecutionLogEntity entity) {
        return executionLogMapper.updateById(entity);
    }

    public int deleteByTaskId(Long taskId) {
        return executionLogMapper.deleteByTaskId(taskId);
    }

    public long countRunningByTaskId(Long taskId) {
        return executionLogMapper.countRunningByTaskId(taskId);
    }
}
