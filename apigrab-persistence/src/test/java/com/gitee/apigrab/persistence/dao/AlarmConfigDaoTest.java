package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.AlarmConfigMapper;
import com.gitee.apigrab.persistence.entity.AlarmConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class AlarmConfigDaoTest {

    private AlarmConfigDao dao;
    private AlarmConfigMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new AlarmConfigDao();
        mapper = Mockito.mock(AlarmConfigMapper.class);
        java.lang.reflect.Field field = AlarmConfigDao.class.getDeclaredField("alarmConfigMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetById() {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        entity.setId(1L);
        entity.setEndpoint("https://webhook.example.com");
        when(mapper.selectById(1L)).thenReturn(entity);

        AlarmConfigEntity result = dao.getById(1L);
        assertNotNull(result);
        assertEquals("https://webhook.example.com", result.getEndpoint());
    }

    @Test
    void testReplace() {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        entity.setEndpoint("https://new-webhook.example.com");
        when(mapper.replace(entity)).thenReturn(1);
        assertEquals(1, dao.replace(entity));
    }

    @Test
    void testUpdateById() {
        AlarmConfigEntity entity = new AlarmConfigEntity();
        entity.setId(1L);
        when(mapper.updateById(entity)).thenReturn(1);
        assertEquals(1, dao.updateById(entity));
    }
}
