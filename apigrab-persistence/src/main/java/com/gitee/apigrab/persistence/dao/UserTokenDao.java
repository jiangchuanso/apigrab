package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.UserTokenMapper;
import com.gitee.apigrab.persistence.entity.UserTokenEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.Date;

@Repository
public class UserTokenDao {

    @Resource
    private UserTokenMapper userTokenMapper;

    public UserTokenEntity getByToken(String token) {
        return userTokenMapper.selectByToken(token);
    }

    public int insert(UserTokenEntity entity) {
        return userTokenMapper.insert(entity);
    }

    public int deleteByToken(String token) {
        return userTokenMapper.deleteByToken(token);
    }

    public int deleteByUserId(Long userId) {
        return userTokenMapper.deleteByUserId(userId);
    }

    public int updateLastAccess(String token, Date lastAccessTime, Date expireTime) {
        return userTokenMapper.updateLastAccess(token, lastAccessTime, expireTime);
    }

    public int deleteExpired() {
        return userTokenMapper.deleteExpired();
    }
}
