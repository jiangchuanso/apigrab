package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.*;
import com.gitee.apigrab.persistence.entity.auth.BearerAuthConfig;
import com.gitee.apigrab.persistence.entity.enums.HttpMethod;
import com.gitee.apigrab.persistence.entity.enums.TransformType;
import org.junit.jupiter.api.Test;

import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ApiConfigEntityTest {

    @Test
    void testGettersAndSetters() {
        ApiConfigEntity entity = new ApiConfigEntity();
        Date now = new Date();
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("Content-Type", "application/json");
        Map<String, String> query = new LinkedHashMap<>();
        query.put("page", "1");
        BearerAuthConfig auth = new BearerAuthConfig();
        auth.setToken("abc123");
        PageConfig page = new PageConfig();
        page.setMode(PaginationMode.PAGE_NUMBER);
        page.setPageSize(20);

        entity.setId(1L);
        entity.setName("test");
        entity.setUrl("https://api.example.com/data");
        entity.setMethod(HttpMethod.GET);
        entity.setHeaders(headers);
        entity.setQueryParams(query);
        entity.setBodyType(BodyType.JSON);
        entity.setBodyContent("{\"key\":\"value\"}");
        entity.setConnectTimeout(5);
        entity.setReadTimeout(120);
        entity.setAuthType(AuthType.BEARER);
        entity.setAuthConfig(auth);
        entity.setRespType(RespType.JSON);
        entity.setDataPath("$.data.list");
        entity.setPageEnabled(1);
        entity.setPageConfig(page);
        entity.setTransformType(TransformType.GROOVY);
        entity.setConvertScript("return records");
        entity.setStatus(EnableStatus.ENABLED);
        entity.setRemark("remark");
        entity.setCreateTime(now);
        entity.setUpdateTime(now);

        assertEquals(1L, entity.getId());
        assertEquals(HttpMethod.GET, entity.getMethod());
        assertEquals("application/json", entity.getHeaders().get("Content-Type"));
        assertEquals("1", entity.getQueryParams().get("page"));
        assertEquals(BodyType.JSON, entity.getBodyType());
        assertEquals(5, entity.getConnectTimeout());
        assertEquals(120, entity.getReadTimeout());
        assertEquals(AuthType.BEARER, entity.getAuthType());
        assertEquals("abc123", ((BearerAuthConfig) entity.getAuthConfig()).getToken());
        assertEquals(RespType.JSON, entity.getRespType());
        assertEquals("$.data.list", entity.getDataPath());
        assertEquals(PaginationMode.PAGE_NUMBER, entity.getPageConfig().getMode());
        assertEquals(TransformType.GROOVY, entity.getTransformType());
        assertEquals(EnableStatus.ENABLED, entity.getStatus());
    }

    @Test
    void testDefaultValues() {
        ApiConfigEntity entity = new ApiConfigEntity();
        assertNull(entity.getId());
        assertNull(entity.getName());
        assertNull(entity.getUrl());
        assertNull(entity.getMethod());
        assertNull(entity.getStatus());
    }
}
