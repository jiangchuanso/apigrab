package com.gitee.apigrab.common.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

import static org.junit.jupiter.api.Assertions.*;

class JacksonConfigTest {

    @Test
    void testObjectMapperCreation() {
        JacksonConfig config = new JacksonConfig();
        ObjectMapper mapper = config.objectMapper(Jackson2ObjectMapperBuilder.json());
        assertNotNull(mapper);
        // Verify the mapper has modules (JavaTimeModule was registered)
        assertTrue(mapper.getRegisteredModuleIds().size() > 0);
        // Verify serialization features were configured correctly
        assertFalse(mapper.isEnabled(com.fasterxml.jackson.databind.SerializationFeature.WRITE_DATES_AS_TIMESTAMPS));
    }
}
