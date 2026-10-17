package com.gitee.apigrab.common.exception;

public enum ResponseErrorCode {

    SUCCESS(200, "成功"),
    BAD_REQUEST(400, "请求参数错误"),
    UNAUTHORIZED(401, "未授权"),
    FORBIDDEN(403, "无权限访问"),
    NOT_FOUND(404, "资源不存在"),
    METHOD_NOT_ALLOWED(405, "请求方法不允许"),
    UNSUPPORTED_AUTH_TYPE(1001, "不支持的认证类型"),
    DATASOURCE_CONNECTION_FAILED(2001, "数据源连接失败"),
    DATASOURCE_NOT_FOUND(2002, "数据源不存在"),
    HTTP_REQUEST_FAILED(3001, "HTTP请求失败"),
    JSON_PARSE_ERROR(3002, "JSON解析错误"),
    SCRIPT_EXECUTION_ERROR(4001, "脚本执行错误"),
    TASK_EXECUTION_ERROR(5001, "任务执行失败"),
    ALARM_SEND_FAILED(6001, "告警发送失败"),
    FIELD_MAPPING_ERROR(7001, "字段映射错误"),
    UNSUPPORTED_DB_TYPE(8001, "不支持的数据库类型"),
    INTERNAL_ERROR(9999, "服务器内部错误");

    private final int code;
    private final String message;

    ResponseErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
