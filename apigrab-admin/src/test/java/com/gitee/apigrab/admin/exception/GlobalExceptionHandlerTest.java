package com.gitee.apigrab.admin.exception;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.common.model.ResultEntity;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 全局异常映射。
 *
 * <p>重点守护 {@link DuplicateKeyException} 的兜底：数据库唯一约束冲突此前未被映射，
 * 会落到 HTTP 500 与原始 SQL 报文。服务层已前置判重，该路径仅在并发竞态或绕过服务层时可达，
 * 但一旦发生，用户不应看到服务器内部错误。
 */
class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    void duplicateKeyMapsToReadableBadRequestNotServerError() {
        ResponseEntity<ResultEntity<Void>> response =
            handler.handleDuplicateKeyException(new DuplicateKeyException("Duplicate entry 'x' for key 'uk_name'"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode(), "唯一冲突不得返回 500");
        assertNotNull(response.getBody());
        assertEquals(400, response.getBody().getCode());
    }

    @Test
    void duplicateKeyMessageIsGenericAndDoesNotLeakSql() {
        String leaked = "Duplicate entry 'OrderApi' for key 'uk_name'";
        ResponseEntity<ResultEntity<Void>> response =
            handler.handleDuplicateKeyException(new DuplicateKeyException(leaked));

        String message = response.getBody() == null ? null : response.getBody().getMessage();
        assertNotNull(message);
        // 不得回显原始 SQL 报文
        assertTrue(message.indexOf("Duplicate entry") < 0, "不得暴露原始 SQL 报文: " + message);
        assertTrue(message.indexOf("uk_name") < 0, "不得暴露索引名: " + message);
        // DuplicateKeyException 不携带业务语义，无法判断是哪个唯一约束，故文案应为通用表述
        assertTrue(message.contains("已存在"), "文案应可读: " + message);
    }

    @Test
    void commonExceptionMappingIsUnchanged() {
        ResponseEntity<ResultEntity<Void>> response = handler.handleCommonException(
            new CommonException(ResponseErrorCode.BAD_REQUEST, "接口配置名称已存在"));

        assertEquals(HttpStatus.BAD_REQUEST, response.getStatusCode());
        assertEquals(400, response.getBody() == null ? null : response.getBody().getCode());
        assertEquals("接口配置名称已存在", response.getBody() == null ? null : response.getBody().getMessage());
    }

    @Test
    void nonStandardBusinessCodeFallsBackToServerError() {
        // 既有的兜底行为：业务码不是标准 HTTP 状态时返回 500
        ResponseEntity<ResultEntity<Void>> response =
            handler.handleCommonException(new CommonException(99999, "内部错误"));

        assertEquals(HttpStatus.INTERNAL_SERVER_ERROR, response.getStatusCode());
    }
}
