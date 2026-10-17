package com.gitee.apigrab.persistence.entity;

import com.gitee.apigrab.persistence.entity.enums.Stage;
import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.Map;

@Getter
@Setter
public class ExecutionStageLogEntity implements Serializable {
    private static final long serialVersionUID = 1L;
    private Long id;
    private Long executionId;
    private Stage stage;
    private Integer inputCount;
    private Integer outputCount;
    private Integer successCount;
    private Integer failCount;
    private Long costMs;
    private List<Map<String, Object>> failureSamples;
    private Date createTime;
}
