package com.gitee.apigrab.common.exception;

public class CommonException extends RuntimeException {

    private final int code;

    public CommonException(ResponseErrorCode errorCode) {
        super(errorCode.getMessage());
        this.code = errorCode.getCode();
    }

    public CommonException(ResponseErrorCode errorCode, String message) {
        super(message);
        this.code = errorCode.getCode();
    }

    public CommonException(ResponseErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.code = errorCode.getCode();
    }

    public CommonException(int code, String message) {
        super(message);
        this.code = code;
    }

    public int getCode() {
        return code;
    }
}
