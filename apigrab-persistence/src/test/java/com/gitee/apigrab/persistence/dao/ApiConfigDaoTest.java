package com.gitee.apigrab.persistence.dao;

import com.gitee.apigrab.persistence.mapper.ApiConfigMapper;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ApiConfigDaoTest {

    private ApiConfigDao dao;
    private ApiConfigMapper mapper;

    @BeforeEach
    void setUp() throws Exception {
        dao = new ApiConfigDao();
        mapper = Mockito.mock(ApiConfigMapper.class);
        java.lang.reflect.Field field = ApiConfigDao.class.getDeclaredField("apiConfigMapper");
        field.setAccessible(true);
        field.set(dao, mapper);
    }

    @Test
    void testGetById() {
        ApiConfigEntity entity = new ApiConfigEntity();
        entity.setId(1L);
        entity.setName("测试接口");
        when(mapper.selectById(1L)).thenReturn(entity);

        ApiConfigEntity result = dao.getById(1L);
        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals("测试接口", result.getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(mapper.selectById(999L)).thenReturn(null);
        assertNull(dao.getById(999L));
    }

    @Test
    void testList() {
        ApiConfigEntity param = new ApiConfigEntity();
        List<ApiConfigEntity> list = Arrays.asList(new ApiConfigEntity(), new ApiConfigEntity());
        when(mapper.selectList(param)).thenReturn(list);

        List<ApiConfigEntity> result = dao.list(param);
        assertEquals(2, result.size());
    }

    @Test
    void testInsert() {
        ApiConfigEntity entity = new ApiConfigEntity();
        entity.setName("新接口");
        when(mapper.insert(entity)).thenReturn(1);

        int rows = dao.insert(entity);
        assertEquals(1, rows);
        verify(mapper).insert(entity);
    }

    @Test
    void testUpdateById() {
        ApiConfigEntity entity = new ApiConfigEntity();
        entity.setId(1L);
        when(mapper.updateById(entity)).thenReturn(1);

        int rows = dao.updateById(entity);
        assertEquals(1, rows);
    }

    @Test
    void testDeleteById() {
        when(mapper.deleteById(1L)).thenReturn(1);
        int rows = dao.deleteById(1L);
        assertEquals(1, rows);
    }

    @Test
    void testSelectEnabled() {
        when(mapper.selectEnabled()).thenReturn(Collections.singletonList(new ApiConfigEntity()));
        List<ApiConfigEntity> result = dao.selectEnabled();
        assertEquals(1, result.size());
    }
}
