package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.AuthConfigMapper;
import com.gitee.apigrab.persistence.entity.AuthConfigEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class AuthConfigDao {

    @Resource
    private AuthConfigMapper authConfigMapper;

    public AuthConfigEntity getById(Long id) {
        return authConfigMapper.selectById(id);
    }

    public AuthConfigEntity getByName(String name) {
        return authConfigMapper.selectByName(name);
    }

    public List<AuthConfigEntity> list(AuthConfigEntity param) {
        return authConfigMapper.selectList(param);
    }

    public int insert(AuthConfigEntity entity) {
        return authConfigMapper.insert(entity);
    }

    public int updateById(AuthConfigEntity entity) {
        return authConfigMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return authConfigMapper.deleteById(id);
    }

    public long countReferences(Long id) {
        return authConfigMapper.countReferences(id);
    }
}
