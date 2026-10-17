package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface AuthConfigMapper {
    AuthConfigEntity selectById(@Param("id") Long id);
    AuthConfigEntity selectByName(@Param("name") String name);
    List<AuthConfigEntity> selectList(AuthConfigEntity param);
    int insert(AuthConfigEntity entity);
    int updateById(AuthConfigEntity entity);
    int deleteById(@Param("id") Long id);
    long countReferences(@Param("id") Long id);
}
