package com.gitee.apigrab.core.transform;

import com.gitee.apigrab.persistence.entity.enums.TransformType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinTransformerTest {

    private final BuiltinTransformer transformer = new BuiltinTransformer();

    @Test
    void testSupportsFieldMapping() {
        assertTrue(transformer.supports(TransformType.FIELD_MAPPING));
    }

    @Test
    void testSupportsNull() {
        assertTrue(transformer.supports(null));
    }

    @Test
    void testSupportsEmpty() {
        assertTrue(transformer.supports(null));
    }

    @Test
    void testDoesNotSupportGroovy() {
        assertFalse(transformer.supports(TransformType.GROOVY));
    }

    @Test
    void testTransformReturnsSameRecords() {
        List<Map<String, Object>> records = new ArrayList<>();
        records.add(Collections.singletonMap("key", "value"));
        List<Map<String, Object>> result = transformer.transform(records, null);
        assertSame(records, result);
        assertEquals(1, result.size());
        assertEquals("value", result.get(0).get("key"));
    }
}
