package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface FieldMappingMapper {
    List<FieldMappingEntity> selectByTaskId(@Param("taskId") Long taskId);
    int batchInsert(@Param("list") List<FieldMappingEntity> list);
    int deleteByTaskId(@Param("taskId") Long taskId);
}
