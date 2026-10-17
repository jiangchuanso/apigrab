package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.DatasourceConfigMapper;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;
import java.util.List;

@Repository
public class DatasourceConfigDao {

    @Resource
    private DatasourceConfigMapper datasourceConfigMapper;

    public DatasourceConfigEntity getById(Long id) {
        return datasourceConfigMapper.selectById(id);
    }

    public DatasourceConfigEntity getByName(String name) {
        return datasourceConfigMapper.selectByName(name);
    }

    public List<DatasourceConfigEntity> list(DatasourceConfigEntity param) {
        return datasourceConfigMapper.selectList(param);
    }

    public int insert(DatasourceConfigEntity entity) {
        return datasourceConfigMapper.insert(entity);
    }

    public int updateById(DatasourceConfigEntity entity) {
        return datasourceConfigMapper.updateById(entity);
    }

    public int deleteById(Long id) {
        return datasourceConfigMapper.deleteById(id);
    }

    public List<DatasourceConfigEntity> selectEnabled() {
        return datasourceConfigMapper.selectEnabled();
    }

    public long countReferences(Long id) {
        return datasourceConfigMapper.countReferences(id);
    }
}
