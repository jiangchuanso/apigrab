package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface CollectTaskMapper {
    CollectTaskEntity selectById(@Param("id") Long id);
    CollectTaskEntity selectByName(@Param("name") String name);
    List<CollectTaskEntity> selectList(CollectTaskEntity param);
    int insert(CollectTaskEntity entity);
    int updateById(CollectTaskEntity entity);
    int deleteById(@Param("id") Long id);
    List<CollectTaskEntity> selectEnabled();
}
