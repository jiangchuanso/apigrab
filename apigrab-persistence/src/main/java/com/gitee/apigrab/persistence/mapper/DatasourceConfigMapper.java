package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

@Mapper
public interface DatasourceConfigMapper {
    DatasourceConfigEntity selectById(@Param("id") Long id);
    DatasourceConfigEntity selectByName(@Param("name") String name);
    List<DatasourceConfigEntity> selectList(DatasourceConfigEntity param);
    int insert(DatasourceConfigEntity entity);
    int updateById(DatasourceConfigEntity entity);
    int deleteById(@Param("id") Long id);
    List<DatasourceConfigEntity> selectEnabled();
    long countReferences(@Param("id") Long id);
}
