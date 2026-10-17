package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.CollectTaskMapper;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectTaskDaoTest {

    private CollectTaskDao dao;
    private CollectTaskMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new CollectTaskDao();
        mapper = Mockito.mock(CollectTaskMapper.class);
        java.lang.reflect.Field field = CollectTaskDao.class.getDeclaredField("collectTaskMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetById() {
        CollectTaskEntity entity = new CollectTaskEntity();
        entity.setId(1L);
        entity.setName("每日同步");
        when(mapper.selectById(1L)).thenReturn(entity);

        CollectTaskEntity result = dao.getById(1L);
        assertNotNull(result);
        assertEquals("每日同步", result.getName());
    }

    @Test
    void testList() {
        CollectTaskEntity param = new CollectTaskEntity();
        when(mapper.selectList(param)).thenReturn(Arrays.asList(new CollectTaskEntity()));
        assertEquals(1, dao.list(param).size());
    }

    @Test
    void testInsert() {
        CollectTaskEntity entity = new CollectTaskEntity();
        when(mapper.insert(entity)).thenReturn(1);
        assertEquals(1, dao.insert(entity));
    }

    @Test
    void testUpdateById() {
        CollectTaskEntity entity = new CollectTaskEntity();
        when(mapper.updateById(entity)).thenReturn(1);
        assertEquals(1, dao.updateById(entity));
    }

    @Test
    void testDeleteById() {
        when(mapper.deleteById(1L)).thenReturn(1);
        assertEquals(1, dao.deleteById(1L));
    }

    @Test
    void testSelectEnabled() {
        when(mapper.selectEnabled()).thenReturn(Arrays.asList(new CollectTaskEntity()));
        assertEquals(1, dao.selectEnabled().size());
    }
}
