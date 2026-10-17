package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ExecutionStageLogMapper;
import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class ExecutionStageLogDao {

    @Resource
    private ExecutionStageLogMapper executionStageLogMapper;

    public int insert(ExecutionStageLogEntity entity) {
        return executionStageLogMapper.insert(entity);
    }

    public List<ExecutionStageLogEntity> getByExecutionId(Long executionId) {
        return executionStageLogMapper.selectByExecutionId(executionId);
    }

    public int deleteByTaskId(Long taskId) {
        return executionStageLogMapper.deleteByTaskId(taskId);
    }
}
