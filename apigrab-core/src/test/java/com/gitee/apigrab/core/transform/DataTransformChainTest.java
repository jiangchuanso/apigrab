package com.gitee.apigrab.core.transform;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.TransformType;
import org.junit.jupiter.api.Test;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;

class DataTransformChainTest {

    @Test
    void testExecuteWithFieldMappingTransformer() {
        List<DataTransformer> transformers = Collections.singletonList(new BuiltinTransformer());
        DataTransformChain chain = new DataTransformChain(transformers);

        List<Map<String, Object>> data = new ArrayList<>();
        data.add(Collections.singletonMap("key", "value"));

        ApiConfigEntity config = new ApiConfigEntity();
        config.setTransformType(TransformType.FIELD_MAPPING);

        List<Map<String, Object>> result = chain.execute(data, config, Collections.emptyList());
        assertEquals(1, result.size());
        assertEquals("value", result.get(0).get("key"));
    }

    @Test
    void testExecuteWithNullTransformType() {
        List<DataTransformer> transformers = Collections.singletonList(new BuiltinTransformer());
        DataTransformChain chain = new DataTransformChain(transformers);

        List<Map<String, Object>> data = Collections.singletonList(Collections.singletonMap("a", 1));
        ApiConfigEntity config = new ApiConfigEntity();

        List<Map<String, Object>> result = chain.execute(data, config, Collections.emptyList());
        assertEquals(1, result.size());
        assertEquals(1, result.get(0).get("a"));
    }

    @Test
    void testExecuteWithNoMatchingTransformer() {
        // BuiltinTransformer only supports FIELD_MAPPING, null, or empty
        // So for GROOVY it would work if there's no GroovyTransformer registered
        List<DataTransformer> transformers = Collections.singletonList(new BuiltinTransformer());
        DataTransformChain chain = new DataTransformChain(transformers);

        List<Map<String, Object>> data = Collections.singletonList(Collections.singletonMap("a", 1));
        ApiConfigEntity config = new ApiConfigEntity();
        config.setTransformType(TransformType.GROOVY);

        List<Map<String, Object>> result = chain.execute(data, config, Collections.emptyList());
        assertSame(data, result); // No transformer supports GROOVY, passthrough
    }

    @Test
    void testBuiltinTransformerPriority() {
        DataTransformer nonBuiltin = new DataTransformer() {
            @Override public boolean supports(TransformType transformType) { return transformType == TransformType.GROOVY; }
            @Override public List<Map<String, Object>> transform(List<Map<String, Object>> records, String script) {
                records.forEach(r -> r.put("transformed", true));
                return records;
            }
        };
        List<DataTransformer> transformers = Arrays.asList(nonBuiltin, new BuiltinTransformer());
        DataTransformChain chain = new DataTransformChain(transformers);

        List<Map<String, Object>> data = new ArrayList<>();
        data.add(new LinkedHashMap<>());
        data.get(0).put("a", 1);

        ApiConfigEntity config = new ApiConfigEntity();
        config.setTransformType(TransformType.FIELD_MAPPING);

        List<Map<String, Object>> result = chain.execute(data, config, Collections.emptyList());
        assertEquals(1, result.size());
        assertFalse(result.get(0).containsKey("transformed")); // Builtin ran (no-op), not custom
    }

    @Test
    void testEmptyTransformers() {
        DataTransformChain chain = new DataTransformChain(Collections.emptyList());
        List<Map<String, Object>> data = Collections.singletonList(Collections.singletonMap("a", 1));
        List<Map<String, Object>> result = chain.execute(data, new ApiConfigEntity(), Collections.emptyList());
        assertSame(data, result);
    }
}
