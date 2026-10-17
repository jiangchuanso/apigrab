package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ApiConfigMapper;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class ApiConfigDao {

    @Resource
    private ApiConfigMapper apiConfigMapper;

    public ApiConfigEntity getById(Long id) {
        return apiConfigMapper.selectById(id);
    }

    public ApiConfigEntity getByName(String name) {
        return apiConfigMapper.selectByName(name);
    }

    public List<ApiConfigEntity> list(ApiConfigEntity param) {
        return apiConfigMapper.selectList(param);
    }

    public int insert(ApiConfigEntity entity) {
        return apiConfigMapper.insert(entity);
    }

    public int updateById(ApiConfigEntity entity) {
        return apiConfigMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return apiConfigMapper.deleteById(id);
    }

    public List<ApiConfigEntity> selectEnabled() {
        return apiConfigMapper.selectEnabled();
    }
}
