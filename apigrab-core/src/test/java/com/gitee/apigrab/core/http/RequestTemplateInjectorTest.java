package com.gitee.apigrab.core.http;

import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.enums.BodyType;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class RequestTemplateInjectorTest {

    private ApiConfigEntity configWithTemplate() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setUrl("https://api.example.com/user/${id}");
        Map<String, String> headers = new LinkedHashMap<>();
        headers.put("X-User", "${id}");
        headers.put("Accept", "application/json");
        config.setHeaders(headers);
        Map<String, String> query = new LinkedHashMap<>();
        query.put("uid", "${id}");
        config.setQueryParams(query);
        config.setBodyContent("{\"userId\":\"${id}\"}");
        return config;
    }

    @Test
    void replacesPlaceholderAcrossUrlHeadersQueryAndBody() {
        ApiConfigEntity config = configWithTemplate();

        boolean replaced = RequestTemplateInjector.replacePlaceholder(config, "id", "42");

        assertTrue(replaced);
        assertEquals("https://api.example.com/user/42", config.getUrl());
        assertEquals("42", config.getHeaders().get("X-User"));
        assertEquals("application/json", config.getHeaders().get("Accept"));
        assertEquals("42", config.getQueryParams().get("uid"));
        assertEquals("{\"userId\":\"42\"}", config.getBodyContent());
    }

    @Test
    void replaceRequestFieldsDoesNotTouchUrl() {
        ApiConfigEntity config = configWithTemplate();

        boolean replaced = RequestTemplateInjector.replaceRequestFields(config, "id", "7");

        assertTrue(replaced);
        assertEquals("https://api.example.com/user/${id}", config.getUrl());
        assertEquals("7", config.getHeaders().get("X-User"));
        assertEquals("7", config.getQueryParams().get("uid"));
        assertEquals("{\"userId\":\"7\"}", config.getBodyContent());
    }

    @Test
    void returnsFalseWhenNoPlaceholderPresent() {
        ApiConfigEntity config = configWithTemplate();

        boolean replaced = RequestTemplateInjector.replacePlaceholder(config, "missing", "x");

        assertFalse(replaced);
        assertEquals("https://api.example.com/user/${id}", config.getUrl());
    }

    @Test
    void blankNameReturnsFalse() {
        ApiConfigEntity config = configWithTemplate();

        assertFalse(RequestTemplateInjector.replacePlaceholder(config, "", "x"));
        assertFalse(RequestTemplateInjector.replacePlaceholder(config, null, "x"));
    }

    private ApiConfigEntity formConfig(String bodyContent) {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setBodyType(BodyType.FORM);
        config.setBodyContent(bodyContent);
        return config;
    }

    @Test
    void replacesRawPlaceholderInFormBody() {
        ApiConfigEntity config = formConfig("id=${id}&scope=full");

        assertTrue(RequestTemplateInjector.replacePlaceholder(config, "id", "42"));

        assertEquals("id=42&scope=full", config.getBodyContent());
    }

    @Test
    void replacesFormEncodedPlaceholder() {
        ApiConfigEntity config = formConfig("id=%24%7Bid%7D&scope=full");

        assertTrue(RequestTemplateInjector.replacePlaceholder(config, "id", "42"));

        assertEquals("id=42&scope=full", config.getBodyContent());
    }

    @Test
    void formBodyInjectionEncodesValueOnceAndKeepsOtherPairs() {
        ApiConfigEntity config = formConfig("q=a%26b&id=%24%7Bid%7D");

        assertTrue(RequestTemplateInjector.replacePlaceholder(config, "id", "a&b=c+d 5%"));

        assertEquals("q=a%26b&id=a%26b%3Dc%2Bd+5%25", config.getBodyContent());
    }

    @Test
    void formBodyWithoutPlaceholderIsNotTouched() {
        ApiConfigEntity config = formConfig("a=1&b=2");

        assertFalse(RequestTemplateInjector.replacePlaceholder(config, "id", "42"));

        assertEquals("a=1&b=2", config.getBodyContent());
    }

    @Test
    void jsonBodyKeepsLiteralPercentEncodedToken() {
        // 非 FORM 正文不做解码：用户若真的想传 %24%7Bid%7D 字面量，行为不变
        ApiConfigEntity config = new ApiConfigEntity();
        config.setBodyType(BodyType.JSON);
        config.setBodyContent("{\"id\":\"%24%7Bid%7D\"}");

        assertFalse(RequestTemplateInjector.replacePlaceholder(config, "id", "42"));

        assertEquals("{\"id\":\"%24%7Bid%7D\"}", config.getBodyContent());
    }

    @Test
    void nullBodyTypeFallsBackToLiteralReplacement() {
        ApiConfigEntity config = new ApiConfigEntity();
        config.setBodyContent("id=${id}");

        assertTrue(RequestTemplateInjector.replacePlaceholder(config, "id", "42"));

        assertEquals("id=42", config.getBodyContent());
    }
}

