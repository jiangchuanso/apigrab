package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface ApiConfigMapper {
    ApiConfigEntity selectById(@Param("id") Long id);
    ApiConfigEntity selectByName(@Param("name") String name);
    List<ApiConfigEntity> selectList(ApiConfigEntity param);
    int insert(ApiConfigEntity entity);
    int updateById(ApiConfigEntity entity);
    int deleteById(@Param("id") Long id);
    List<ApiConfigEntity> selectEnabled();
}
