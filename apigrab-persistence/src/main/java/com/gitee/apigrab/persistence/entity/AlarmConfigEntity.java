package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.EnableStatus;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class AlarmConfigEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private EnableStatus status;
    private String endpoint;
    private String contentType;
    private String inputTemplate;
    private Date createTime;
    private Date updateTime;
}
