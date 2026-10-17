package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import com.gitee.apigrab.persistence.entity.enums.UserRole;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface SystemUserMapper {
    SystemUserEntity selectById(@Param("id") Long id);
    SystemUserEntity selectByUsername(@Param("username") String username);
    List<SystemUserEntity> selectList(SystemUserEntity param);
    int insert(SystemUserEntity entity);
    int updateById(SystemUserEntity entity);
    int updatePassword(@Param("id") Long id, @Param("password") String password);
    int deleteById(@Param("id") Long id);
    int countEnabledByRole(@Param("role") UserRole role);
}
