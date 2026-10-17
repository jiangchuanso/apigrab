package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.AlarmConfigMapper;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.springframework.stereotype.Repository;

import javax.annotation.Resource;

@Repository
public class AlarmConfigDao {

    @Resource
    private AlarmConfigMapper alarmConfigMapper;

    public AlarmConfigEntity getById(Long id) {
        return alarmConfigMapper.selectById(id);
    }

    public int replace(AlarmConfigEntity entity) {
        return alarmConfigMapper.replace(entity);
    }

    public int updateById(AlarmConfigEntity entity) {
        return alarmConfigMapper.updateById(entity);
    }
}
