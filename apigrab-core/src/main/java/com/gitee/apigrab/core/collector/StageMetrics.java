package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.persistence.entity.ExecutionStageLogEntity;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import lombok.Getter;
import lombok.Setter;

/**
 * Carries per-stage observability metrics for one pipeline stage.
 */
@Getter
@Setter
public class StageMetrics {

    private final Stage stage;
    private final FailureSampleCollector failureCollector;
    private int inputCount;
    private int outputCount;
    private int successCount;
    private int failCount;
    private long costMs;

    public StageMetrics(Stage stage, int failureSampleLimit) {
        this.stage = stage;
        this.failureCollector = new FailureSampleCollector(failureSampleLimit);
    }

    public ExecutionStageLogEntity toEntity(Long executionId) {
        ExecutionStageLogEntity entity = new ExecutionStageLogEntity();
        entity.setExecutionId(executionId);
        entity.setStage(stage);
        entity.setInputCount(inputCount);
        entity.setOutputCount(outputCount);
        entity.setSuccessCount(successCount);
        entity.setFailCount(failCount);
        entity.setCostMs(costMs);
        entity.setFailureSamples(failureCollector.getSamples());
        return entity;
    }
}
