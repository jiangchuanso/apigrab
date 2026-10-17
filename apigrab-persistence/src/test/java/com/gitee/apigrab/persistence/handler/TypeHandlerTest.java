package com.gitee.apigrab.persistence.handler;

import com.gitee.apigrab.persistence.entity.DetailConfig;
import com.gitee.apigrab.persistence.entity.enums.*;
import com.gitee.apigrab.persistence.entity.AuthConfig;
import com.gitee.apigrab.persistence.entity.PageConfig;
import com.gitee.apigrab.persistence.entity.auth.OAuth2AuthConfig;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class TypeHandlerTest {

    @Test
    void stringMapRoundTrip() throws Exception {
        StringMapTypeHandler handler = new StringMapTypeHandler();
        Map<String, String> map = new LinkedHashMap<>();
        map.put("Content-Type", "application/json");

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, map, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        Map<String, String> out = handler.getNullableResult(rs, 1);
        assertEquals("application/json", out.get("Content-Type"));
    }

    @Test
    void objectMapRoundTrip() throws Exception {
        ObjectMapTypeHandler handler = new ObjectMapTypeHandler();
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("k", 1);
        map.put("nested", Collections.singletonMap("x", true));

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, map, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        Map<String, Object> out = handler.getNullableResult(rs, 1);
        assertEquals(1, out.get("k"));
        assertTrue(((Map<?, ?>) out.get("nested")).get("x") instanceof Boolean);
    }

    @Test
    void failureSamplesRoundTrip() throws Exception {
        FailureSamplesTypeHandler handler = new FailureSamplesTypeHandler();
        List<Map<String, Object>> samples = new ArrayList<>();
        samples.add(Collections.singletonMap("reason", "boom"));

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, samples, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        List<Map<String, Object>> out = handler.getNullableResult(rs, 1);
        assertEquals(1, out.size());
        assertEquals("boom", out.get(0).get("reason"));
    }

    @Test
    void pageConfigRoundTrip() throws Exception {
        PageConfigTypeHandler handler = new PageConfigTypeHandler();
        PageConfig cfg = new PageConfig();
        cfg.setMode(PaginationMode.PAGE_NUMBER);
        cfg.setParamLocation(ParamLocation.BODY);
        cfg.setPageSize(100);
        cfg.setStopByBatchSize(true);

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, cfg, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        PageConfig out = handler.getNullableResult(rs, 1);
        assertEquals(PaginationMode.PAGE_NUMBER, out.getMode());
        assertEquals(ParamLocation.BODY, out.getParamLocation());
        assertEquals(100, out.getPageSize());
        assertTrue(out.getStopByBatchSize());
    }

    @Test
    void detailConfigRoundTrip() throws Exception {
        DetailConfigTypeHandler handler = new DetailConfigTypeHandler();
        DetailConfig cfg = new DetailConfig();
        cfg.setEnabled(1);
        cfg.setKeyField("id");
        cfg.setKeyPlaceholder("id");
        cfg.setUrl("https://api.example.com/detail/${id}");
        cfg.setMethod(HttpMethod.GET);
        cfg.setRespType(RespType.JSON);
        cfg.setItemPath("$.data");
        cfg.setConcurrency(4);

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, cfg, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        DetailConfig out = handler.getNullableResult(rs, 1);
        assertEquals(1, out.getEnabled());
        assertEquals("id", out.getKeyField());
        assertEquals("https://api.example.com/detail/${id}", out.getUrl());
        assertEquals(HttpMethod.GET, out.getMethod());
        assertEquals("$.data", out.getItemPath());
        assertEquals(4, out.getConcurrency());
    }

    @Test
    void authConfigRoundTrip() throws Exception {
        AuthConfigTypeHandler handler = new AuthConfigTypeHandler();
        OAuth2AuthConfig cfg = new OAuth2AuthConfig();
        cfg.setTokenUrl("https://x/token");
        cfg.setClientId("id");

        PreparedStatement ps = mock(PreparedStatement.class);
        ArgumentCaptor<String> captor = ArgumentCaptor.forClass(String.class);
        handler.setNonNullParameter(ps, 1, cfg, null);
        verify(ps).setString(eq(1), captor.capture());

        ResultSet rs = mock(ResultSet.class);
        when(rs.getString(1)).thenReturn(captor.getValue());
        AuthConfig out = handler.getNullableResult(rs, 1);
        // 多态：按 authType 还原为具体子类
        assertTrue(out instanceof OAuth2AuthConfig);
        assertEquals("https://x/token", ((OAuth2AuthConfig) out).getTokenUrl());
        assertEquals("id", ((OAuth2AuthConfig) out).getClientId());
    }

    @Test
    void codedEnumRoundTrip() throws Exception {
        CodedEnumTypeHandler<TaskStatus> handler = new CodedEnumTypeHandler<>(TaskStatus.class);

        PreparedStatement ps = mock(PreparedStatement.class);
        handler.setNonNullParameter(ps, 1, TaskStatus.PAUSED, null);
        verify(ps).setInt(1, 2);

        ResultSet rs = mock(ResultSet.class);
        when(rs.getInt(1)).thenReturn(2);
        assertEquals(TaskStatus.PAUSED, handler.getNullableResult(rs, 1));
    }
}
