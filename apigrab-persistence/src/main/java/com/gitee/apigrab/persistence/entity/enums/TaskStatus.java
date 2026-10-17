package com.gitee.apigrab.persistence.entity.enums;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Collect task status: 0 not started, 1 running, 2 paused.
 */
public enum TaskStatus implements CodedEnum {
    NOT_STARTED(0),
    RUNNING(1),
    PAUSED(2);

    private final int code;

    TaskStatus(int code) {
        this.code = code;
    }

    @Override
    @JsonValue
    public int getCode() {
        return code;
    }

    @JsonCreator
    public static TaskStatus fromCode(Integer code) {
        if (code == null) {
            return null;
        }
        for (TaskStatus status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        return null;
    }
}
