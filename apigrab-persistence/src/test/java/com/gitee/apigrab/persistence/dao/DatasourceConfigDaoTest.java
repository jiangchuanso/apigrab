package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.DatasourceConfigMapper;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DatasourceConfigDaoTest {

    private DatasourceConfigDao dao;
    private DatasourceConfigMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new DatasourceConfigDao();
        mapper = Mockito.mock(DatasourceConfigMapper.class);
        java.lang.reflect.Field field = DatasourceConfigDao.class.getDeclaredField("datasourceConfigMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetById() {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        entity.setId(1L);
        entity.setName("生产MySQL");
        when(mapper.selectById(1L)).thenReturn(entity);

        DatasourceConfigEntity result = dao.getById(1L);
        assertNotNull(result);
        assertEquals("生产MySQL", result.getName());
    }

    @Test
    void testList() {
        DatasourceConfigEntity param = new DatasourceConfigEntity();
        when(mapper.selectList(param)).thenReturn(Arrays.asList(new DatasourceConfigEntity()));
        List<DatasourceConfigEntity> result = dao.list(param);
        assertEquals(1, result.size());
    }

    @Test
    void testInsert() {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
        when(mapper.insert(entity)).thenReturn(1);
        assertEquals(1, dao.insert(entity));
    }

    @Test
    void testUpdateById() {
        DatasourceConfigEntity entity = new DatasourceConfigEntity();
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
        when(mapper.selectEnabled()).thenReturn(Arrays.asList(new DatasourceConfigEntity(), new DatasourceConfigEntity()));
        assertEquals(2, dao.selectEnabled().size());
    }
}
