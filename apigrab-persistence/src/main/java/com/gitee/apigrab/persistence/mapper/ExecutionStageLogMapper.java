package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExecutionStageLogMapper {
    int insert(ExecutionStageLogEntity entity);
    List<ExecutionStageLogEntity> selectByExecutionId(@Param("executionId") Long executionId);
    int deleteByTaskId(@Param("taskId") Long taskId);
}
