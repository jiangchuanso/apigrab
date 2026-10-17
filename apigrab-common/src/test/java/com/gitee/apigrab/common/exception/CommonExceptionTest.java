package com.gitee.apigrab.common.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CommonExceptionTest {

    @Test
    void testWithErrorCode() {
        CommonException e = new CommonException(ResponseErrorCode.BAD_REQUEST);
        assertEquals(400, e.getCode());
        assertEquals("请求参数错误", e.getMessage());
    }

    @Test
    void testWithErrorCodeAndMessage() {
        CommonException e = new CommonException(ResponseErrorCode.NOT_FOUND, "用户ID不存在");
        assertEquals(404, e.getCode());
        assertEquals("用户ID不存在", e.getMessage());
    }

    @Test
    void testWithErrorCodeAndCause() {
        RuntimeException cause = new RuntimeException("connection lost");
        CommonException e = new CommonException(ResponseErrorCode.INTERNAL_ERROR, cause);
        assertEquals(9999, e.getCode());
        assertTrue(e.getMessage().contains("服务器内部错误"));
        assertSame(cause, e.getCause());
    }

    @Test
    void testWithCodeAndMessage() {
        CommonException e = new CommonException(1234, "自定义错误");
        assertEquals(1234, e.getCode());
        assertEquals("自定义错误", e.getMessage());
    }

    @Test
    void testUnAuthorizedExceptionNoArg() {
        UnAuthorizedException e = new UnAuthorizedException();
        assertEquals(401, e.getCode());
    }

    @Test
    void testUnAuthorizedExceptionWithMessage() {
        UnAuthorizedException e = new UnAuthorizedException("Token无效");
        assertEquals(401, e.getCode());
        assertEquals("Token无效", e.getMessage());
    }

    @Test
    void testResponseErrorCodeValues() {
        assertEquals(200, ResponseErrorCode.SUCCESS.getCode());
        assertEquals(400, ResponseErrorCode.BAD_REQUEST.getCode());
        assertEquals(401, ResponseErrorCode.UNAUTHORIZED.getCode());
        assertEquals(404, ResponseErrorCode.NOT_FOUND.getCode());
        assertEquals(9999, ResponseErrorCode.INTERNAL_ERROR.getCode());
        assertEquals(1001, ResponseErrorCode.UNSUPPORTED_AUTH_TYPE.getCode());
        assertEquals(2001, ResponseErrorCode.DATASOURCE_CONNECTION_FAILED.getCode());
        assertEquals(3001, ResponseErrorCode.HTTP_REQUEST_FAILED.getCode());
        assertEquals(5001, ResponseErrorCode.TASK_EXECUTION_ERROR.getCode());
    }

    @Test
    void testResponseErrorCodeMessages() {
        assertEquals("成功", ResponseErrorCode.SUCCESS.getMessage());
        assertEquals("请求参数错误", ResponseErrorCode.BAD_REQUEST.getMessage());
        assertEquals("不支持的认证类型", ResponseErrorCode.UNSUPPORTED_AUTH_TYPE.getMessage());
        assertEquals("数据源连接失败", ResponseErrorCode.DATASOURCE_CONNECTION_FAILED.getMessage());
        assertEquals("服务器内部错误", ResponseErrorCode.INTERNAL_ERROR.getMessage());
    }
}
