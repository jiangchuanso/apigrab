package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.persistence.dao.FieldMappingDao;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FieldMappingServiceTest {

    @Mock private FieldMappingDao fieldMappingDao;

    @InjectMocks private FieldMappingService service;

    @Test
    void testListByTaskId() {
        FieldMappingEntity mapping = new FieldMappingEntity();
        when(fieldMappingDao.getByTaskId(1L)).thenReturn(Collections.singletonList(mapping));

        List<FieldMappingEntity> result = service.listByTaskId(1L);

        assertEquals(1, result.size());
        verify(fieldMappingDao, times(1)).getByTaskId(1L);
    }

    @Test
    void testSaveMappingsDeletesThenInsertsAndInjectsTaskId() {
        FieldMappingEntity a = new FieldMappingEntity();
        FieldMappingEntity b = new FieldMappingEntity();
        List<FieldMappingEntity> mappings = Arrays.asList(a, b);

        service.saveMappings(7L, mappings);

        InOrder inOrder = inOrder(fieldMappingDao);
        inOrder.verify(fieldMappingDao).deleteByTaskId(7L);
        inOrder.verify(fieldMappingDao).batchInsert(mappings);
        assertEquals(7L, a.getTaskId());
        assertEquals(7L, b.getTaskId());
    }

    @Test
    void testSaveMappingsEmptyOnlyDeletes() {
        service.saveMappings(9L, new ArrayList<>());

        verify(fieldMappingDao, times(1)).deleteByTaskId(9L);
        verify(fieldMappingDao, never()).batchInsert(anyList());
    }

    @Test
    void testSaveMappingsNullOnlyDeletes() {
        service.saveMappings(9L, null);

        verify(fieldMappingDao, times(1)).deleteByTaskId(9L);
        verify(fieldMappingDao, never()).batchInsert(anyList());
    }
}
