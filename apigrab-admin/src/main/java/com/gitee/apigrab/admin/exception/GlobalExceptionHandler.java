package com.gitee.apigrab.admin.exception;

import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.model.ResultEntity;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Maps {@link CommonException} to an HTTP status derived from its business code
 * when that code is a standard HTTP status (e.g. 400/404); otherwise 500.
 *
 * <p>Also maps database unique-constraint violations ({@link DuplicateKeyException}) to a readable
 * 400 instead of an opaque 500. This is a <b>fallback</b>: the services already check for duplicate
 * names up front, so this path is only reachable through a concurrent race (two requests passing the
 * check simultaneously) or a write that bypasses the service layer.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CommonException.class)
    public ResponseEntity<ResultEntity<Void>> handleCommonException(CommonException e) {
        HttpStatus status = HttpStatus.resolve(e.getCode());
        if (status == null) {
            status = HttpStatus.INTERNAL_SERVER_ERROR;
        }
        return ResponseEntity.status(status).body(ResultEntity.error(e.getCode(), e.getMessage()));
    }

    /**
     * 数据库唯一约束冲突的兜底映射。
     *
     * <p>{@link DuplicateKeyException} 不携带业务语义，无法判断命中的是哪个唯一约束
     * （name / username / token 等），因此这里只给通用表述，**不臆造具体字段名**。
     * 正常使用下几乎不可达——服务层已前置判重，此处覆盖并发竞态与绕过服务层的写入。
     */
    @ExceptionHandler(DuplicateKeyException.class)
    public ResponseEntity<ResultEntity<Void>> handleDuplicateKeyException(DuplicateKeyException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
            .body(ResultEntity.error(HttpStatus.BAD_REQUEST.value(), "已存在相同名称的记录，请修改后重试"));
    }
}
