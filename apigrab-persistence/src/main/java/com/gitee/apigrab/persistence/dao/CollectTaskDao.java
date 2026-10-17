package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.CollectTaskMapper;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class CollectTaskDao {

    @Resource
    private CollectTaskMapper collectTaskMapper;

    public CollectTaskEntity getById(Long id) {
        return collectTaskMapper.selectById(id);
    }

    public CollectTaskEntity getByName(String name) {
        return collectTaskMapper.selectByName(name);
    }

    public List<CollectTaskEntity> list(CollectTaskEntity param) {
        return collectTaskMapper.selectList(param);
    }

    public int insert(CollectTaskEntity entity) {
        return collectTaskMapper.insert(entity);
    }

    public int updateById(CollectTaskEntity entity) {
        return collectTaskMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return collectTaskMapper.deleteById(id);
    }

    public List<CollectTaskEntity> selectEnabled() {
        return collectTaskMapper.selectEnabled();
    }
}
