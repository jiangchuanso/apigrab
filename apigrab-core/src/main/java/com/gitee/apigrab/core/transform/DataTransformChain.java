package com.gitee.apigrab.core.transform;

import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
public class DataTransformChain {
    private final List<DataTransformer> transformers;

    public DataTransformChain(List<DataTransformer> transformers) {
        this.transformers = transformers.stream()
            .sorted((a, b) -> {
                if (a instanceof BuiltinTransformer) return -1;
                if (b instanceof BuiltinTransformer) return 1;
                return 0;
            })
            .collect(Collectors.toList());
    }

    public List<Map<String, Object>> execute(
            List<Map<String, Object>> rawData,
            ApiConfigEntity config,
            List<FieldMappingEntity> mappings) {
        List<Map<String, Object>> result = rawData;
        for (DataTransformer transformer : transformers) {
            if (transformer.supports(config.getTransformType())) {
                long start = System.currentTimeMillis();
                result = transformer.transform(result, config.getConvertScript());
                long cost = System.currentTimeMillis() - start;
                log.debug("Transformer {} cost {}ms", transformer.getClass().getSimpleName(), cost);
            }
        }
        return result;
    }
}
