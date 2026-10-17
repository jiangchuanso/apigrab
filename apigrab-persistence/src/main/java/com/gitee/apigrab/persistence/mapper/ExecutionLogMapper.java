package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.ExecutionLogEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ExecutionLogMapper {
    ExecutionLogEntity selectById(@Param("id") Long id);
    List<ExecutionLogEntity> selectList(ExecutionLogEntity param);
    int insert(ExecutionLogEntity entity);
    int updateById(ExecutionLogEntity entity);
    int deleteByTaskId(@Param("taskId") Long taskId);
    long countRunningByTaskId(@Param("taskId") Long taskId);
}
