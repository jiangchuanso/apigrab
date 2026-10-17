package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.entity.enums.UserRole;
import com.gitee.apigrab.persistence.mapper.SystemUserMapper;
import com.gitee.apigrab.persistence.entity.SystemUserEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class SystemUserDao {

    @Resource
    private SystemUserMapper systemUserMapper;

    public SystemUserEntity getById(Long id) {
        return systemUserMapper.selectById(id);
    }

    public SystemUserEntity getByUsername(String username) {
        return systemUserMapper.selectByUsername(username);
    }

    public List<SystemUserEntity> list(SystemUserEntity param) {
        return systemUserMapper.selectList(param);
    }

    public int insert(SystemUserEntity entity) {
        return systemUserMapper.insert(entity);
    }

    public int updateById(SystemUserEntity entity) {
        return systemUserMapper.updateById(entity);
    }

    public int updatePassword(Long id, String password) {
        return systemUserMapper.updatePassword(id, password);
    }

    public int deleteById(Long id) {
        return systemUserMapper.deleteById(id);
    }

    public int countEnabledByRole(UserRole role) {
        return systemUserMapper.countEnabledByRole(role);
    }
}
