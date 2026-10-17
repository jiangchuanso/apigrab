package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;

@Getter
@Setter
public class ExecutionLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long taskId;
    private String taskName;
    private ExecutionStatus status;
    private Date startTime;
    private Date endTime;
    private Integer totalRecords;
    private Integer successCount;
    private Integer failCount;
    private String errorMsg;
    private Long costMs;
    private Integer pageCount;
    private Stage currentStage;
    private Integer processedCount;
    private Date updatedAt;
    private Date createTime;
}
