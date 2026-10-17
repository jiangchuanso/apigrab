package com.gitee.apigrab.common.model;

import com.gitee.apigrab.common.exception.ResponseErrorCode;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ResultEntityTest {

    @Test
    void testSuccessNoData() {
        ResultEntity<String> result = ResultEntity.success();
        assertTrue(result.isSuccess());
        assertEquals(ResponseErrorCode.SUCCESS.getCode(), result.getCode());
        assertEquals(ResponseErrorCode.SUCCESS.getMessage(), result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testSuccessWithData() {
        ResultEntity<String> result = ResultEntity.success("hello");
        assertTrue(result.isSuccess());
        assertEquals("hello", result.getData());
    }

    @Test
    void testSuccessWithMessageAndData() {
        ResultEntity<Integer> result = ResultEntity.success("自定义消息", 42);
        assertTrue(result.isSuccess());
        assertEquals("自定义消息", result.getMessage());
        assertEquals(42, (int) result.getData());
    }

    @Test
    void testErrorWithCodeAndMessage() {
        ResultEntity<Void> result = ResultEntity.error(500, "服务器内部错误");
        assertFalse(result.isSuccess());
        assertEquals(500, result.getCode());
        assertEquals("服务器内部错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testErrorWithErrorCode() {
        ResultEntity<Void> result = ResultEntity.error(ResponseErrorCode.BAD_REQUEST);
        assertFalse(result.isSuccess());
        assertEquals(400, result.getCode());
    }

    @Test
    void testErrorWithErrorCodeAndMessage() {
        ResultEntity<Void> result = ResultEntity.error(ResponseErrorCode.NOT_FOUND, "用户不存在");
        assertFalse(result.isSuccess());
        assertEquals(404, result.getCode());
        assertEquals("用户不存在", result.getMessage());
    }

    @Test
    void testGenericData() {
        ResultEntity<PageResult<String>> result = ResultEntity.success(new PageResult<String>());
        assertTrue(result.isSuccess());
        assertNotNull(result.getData());
        assertTrue(result.getData() instanceof PageResult);
    }

    @Test
    void testIsSuccessWhenCodeNotSuccess() {
        ResultEntity<Void> result = ResultEntity.error(ResponseErrorCode.INTERNAL_ERROR);
        assertFalse(result.isSuccess());
    }
}
