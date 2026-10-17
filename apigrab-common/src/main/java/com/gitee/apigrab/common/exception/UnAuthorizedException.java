package com.gitee.apigrab.common.exception;

public class UnAuthorizedException extends CommonException {

    public UnAuthorizedException() {
        super(ResponseErrorCode.UNAUTHORIZED);
    }

    public UnAuthorizedException(String message) {
        super(ResponseErrorCode.UNAUTHORIZED.getCode(), message);
    }
}
