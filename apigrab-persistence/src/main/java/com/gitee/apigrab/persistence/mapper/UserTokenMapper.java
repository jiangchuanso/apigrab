package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.UserTokenEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.Date;

@Mapper
public interface UserTokenMapper {
    UserTokenEntity selectByToken(@Param("token") String token);
    int insert(UserTokenEntity entity);
    int deleteByToken(@Param("token") String token);
    int deleteByUserId(@Param("userId") Long userId);
    int updateLastAccess(@Param("token") String token, @Param("lastAccessTime") Date lastAccessTime,
                         @Param("expireTime") Date expireTime);
    int deleteExpired();
}
