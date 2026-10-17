package com.gitee.apigrab.persistence.mapper;

import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface AlarmConfigMapper {
    AlarmConfigEntity selectById(@Param("id") Long id);
    int replace(AlarmConfigEntity entity);
    int updateById(AlarmConfigEntity entity);
}
