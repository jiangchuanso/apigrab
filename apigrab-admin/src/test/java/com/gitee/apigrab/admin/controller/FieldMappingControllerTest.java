package com.gitee.apigrab.admin.controller;

import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.service.FieldMappingService;
import com.gitee.apigrab.common.model.ResultEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

// Pure Mockito controller test (no Spring context), consistent with ApiConfigControllerTest.
class FieldMappingControllerTest {

    private FieldMappingController newController(FieldMappingService mockService) {
        FieldMappingController controller = new FieldMappingController();
        try {
            java.lang.reflect.Field field = FieldMappingController.class.getDeclaredField("fieldMappingService");
            field.setAccessible(true);
            field.set(controller, mockService);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return controller;
    }

    @Test
    void testControllerList() {
        FieldMappingService mockService = mock(FieldMappingService.class);
        FieldMappingController controller = newController(mockService);

        FieldMappingEntity mapping = new FieldMappingEntity();
        mapping.setTargetField("userName");
        when(mockService.listByTaskId(1L)).thenReturn(Collections.singletonList(mapping));

        ResultEntity<List<FieldMappingEntity>> result = controller.list(1L);

        assert result.isSuccess();
        assert result.getData().size() == 1;
        assert result.getData().get(0).getTargetField().equals("userName");
    }

    @Test
    void testControllerSave() {
        FieldMappingService mockService = mock(FieldMappingService.class);
        FieldMappingController controller = newController(mockService);

        FieldMappingInput mapping = new FieldMappingInput();
        mapping.setTargetField("userName");
        List<FieldMappingInput> mappings = Collections.singletonList(mapping);

        ResultEntity<Void> result = controller.save(5L, mappings);

        assert result.isSuccess();
        ArgumentCaptor<List<FieldMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(mockService, times(1)).saveMappings(eq(5L), captor.capture());
        assertEquals("userName", captor.getValue().get(0).getTargetField());
    }
}
