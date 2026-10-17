package com.gitee.apigrab.common.model;

import com.gitee.apigrab.common.exception.ResponseErrorCode;

import java.io.Serializable;

public class ResultEntity<T> implements Serializable {

    private static final long serialVersionUID = 1L;

    private int code;
    private String message;
    private T data;

    private ResultEntity() {
    }

    private ResultEntity(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
    }

    public static <T> ResultEntity<T> success() {
        return new ResultEntity<>(ResponseErrorCode.SUCCESS.getCode(), ResponseErrorCode.SUCCESS.getMessage(), null);
    }

    public static <T> ResultEntity<T> success(T data) {
        return new ResultEntity<>(ResponseErrorCode.SUCCESS.getCode(), ResponseErrorCode.SUCCESS.getMessage(), data);
    }

    public static <T> ResultEntity<T> success(String message, T data) {
        return new ResultEntity<>(ResponseErrorCode.SUCCESS.getCode(), message, data);
    }

    public static <T> ResultEntity<T> error(int code, String message) {
        return new ResultEntity<>(code, message, null);
    }

    public static <T> ResultEntity<T> error(ResponseErrorCode errorCode) {
        return new ResultEntity<>(errorCode.getCode(), errorCode.getMessage(), null);
    }

    public static <T> ResultEntity<T> error(ResponseErrorCode errorCode, String message) {
        return new ResultEntity<>(errorCode.getCode(), message, null);
    }

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public T getData() {
        return data;
    }

    public void setData(T data) {
        this.data = data;
    }

    public boolean isSuccess() {
        return code == ResponseErrorCode.SUCCESS.getCode();
    }
}
