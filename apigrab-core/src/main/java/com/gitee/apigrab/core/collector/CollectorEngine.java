package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.alarm.AlarmModelUtils;
import com.gitee.apigrab.core.alarm.UnifyAlarmOpsService;
import com.gitee.apigrab.core.mapping.FieldMapper;
import com.gitee.apigrab.core.transform.DataTransformChain;
import com.gitee.apigrab.persistence.dao.*;
import com.gitee.apigrab.persistence.entity.*;
import lombok.extern.slf4j.Slf4j;
import com.gitee.apigrab.persistence.entity.enums.ExecutionStatus;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
@Slf4j
public class CollectorEngine {

    @Resource
    private ApiConfigDao apiConfigDao;

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    @Resource
    private CollectTaskDao collectTaskDao;

    @Resource
    private ExecutionLogDao executionLogDao;

    @Resource
    private ExecutionStageLogDao executionStageLogDao;

    @Resource
    private FieldMappingDao fieldMappingDao;

    @Resource
    private PageCollector pageCollector;

    @Resource
    private DetailEnricher detailEnricher;

    @Resource
    private DataTransformChain dataTransformChain;

    @Resource
    private FieldMapper fieldMapper;

    @Resource
    private DataPersister dataPersister;

    @Resource
    private UnifyAlarmOpsService unifyAlarmOpsService;

    @Value("${apigrab.execution.failure-sample-limit:10}")
    private int failureSampleLimit = 10;

    /** 手动"立即执行"的后台执行线程池（守护线程，避免阻塞请求线程）。 */
    private final ExecutorService asyncExecutor = Executors.newFixedThreadPool(4, r -> {
        Thread thread = new Thread(r, "collect-async");
        thread.setDaemon(true);
        return thread;
    });

    public CollectResult executeByTaskId(Long taskId) {
        CollectTaskEntity task = collectTaskDao.getById(taskId);
        if (task == null) {
            throw new RuntimeException("Task not found: " + taskId);
        }
        return executeWithRetry(task);
    }

    /**
     * Execute a task with task-level retry, timeout and failure alarm.
     */
    public CollectResult executeWithRetry(CollectTaskEntity task) {
        int retryTimes = Optional.ofNullable(task.getRetryTimes()).map(v -> Math.max(0, v)).orElse(0);
        CollectResult lastResult = null;
        Exception lastError = null;
        for (int attempt = 0; attempt <= retryTimes; attempt++) {
            try {
                return executeWithTimeout(task);
            } catch (Exception e) {
                lastError = e;
                log.warn("Task {} attempt {}/{} failed: {}", task.getName(), attempt + 1, retryTimes + 1, e.getMessage());
            }
        }
        if (task.getAlarmEnabled() != null && task.getAlarmEnabled() == 1) {
            triggerAlarm(task, lastError);
        }
        return Optional.ofNullable(lastResult).orElseGet(() -> new CollectResult(0, 0, 0, 0));
    }

    /**
     * 手动"立即执行"：后台异步执行，尽快返回首个尝试的执行记录标识。
     * 重试/超时/告警语义与同步执行一致（每次尝试生成独立执行记录）。
     */
    public Long executeAsync(CollectTaskEntity task) {
        CompletableFuture<Long> idFuture = new CompletableFuture<>();
        asyncExecutor.submit(() -> runAsync(task, idFuture));
        try {
            return idFuture.get(5, TimeUnit.SECONDS);
        } catch (Exception e) {
            throw new RuntimeException("Task trigger failed: " + e.getMessage(), e);
        }
    }

    private void runAsync(CollectTaskEntity task, CompletableFuture<Long> idFuture) {
        int retryTimes = Optional.ofNullable(task.getRetryTimes()).map(v -> Math.max(0, v)).orElse(0);
        Exception lastError = null;
        for (int attempt = 0; attempt <= retryTimes; attempt++) {
            ExecutionLogEntity logEntity;
            try {
                logEntity = createExecutionLog(task);
            } catch (Exception e) {
                lastError = e;
                if (!idFuture.isDone()) {
                    idFuture.completeExceptionally(e);
                }
                break;
            }
            if (!idFuture.isDone()) {
                idFuture.complete(logEntity.getId());
            }
            try {
                executeWithTimeoutOnLog(task, logEntity);
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("Task {} async attempt {}/{} failed: {}", task.getName(), attempt + 1, retryTimes + 1, e.getMessage());
            }
        }
        if (!idFuture.isDone()) {
            idFuture.completeExceptionally(Optional.ofNullable(lastError).orElseGet(() -> new RuntimeException("Task failed")));
        }
        if (lastError != null && task.getAlarmEnabled() != null && task.getAlarmEnabled() == 1) {
            triggerAlarm(task, lastError);
        }
    }

    private CollectResult executeWithTimeoutOnLog(final CollectTaskEntity task, final ExecutionLogEntity logEntity) throws Exception {
        Long timeoutMs = task.getTimeoutMs();
        if (timeoutMs == null || timeoutMs <= 0) {
            return executeOnce(task, logEntity);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "collect-task-" + logEntity.getId());
            thread.setDaemon(true);
            return thread;
        });
        Future<CollectResult> future = null;
        try {
            future = executor.submit(() -> {
                try {
                    return executeOnce(task, logEntity);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            if (future != null) {
                future.cancel(true);
            }
            RuntimeException timeoutError = new RuntimeException("Task execution timed out after " + timeoutMs + " ms");
            failLog(logEntity, timeoutError, timeoutMs);
            throw timeoutError;
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw ee;
        } finally {
            executor.shutdownNow();
        }
    }

    private CollectResult executeWithTimeout(final CollectTaskEntity task) throws Exception {
        Long timeoutMs = task.getTimeoutMs();
        if (timeoutMs == null || timeoutMs <= 0) {
            return executeOnce(task);
        }
        ExecutorService executor = Executors.newSingleThreadExecutor(r -> {
            Thread thread = new Thread(r, "collect-task-" + task.getId());
            thread.setDaemon(true);
            return thread;
        });
        Future<CollectResult> future = null;
        try {
            future = executor.submit(() -> {
                try {
                    return executeOnce(task);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            });
            return future.get(timeoutMs, TimeUnit.MILLISECONDS);
        } catch (TimeoutException te) {
            if (future != null) {
                future.cancel(true);
            }
            throw new RuntimeException("Task execution timed out after " + timeoutMs + " ms");
        } catch (ExecutionException ee) {
            Throwable cause = ee.getCause();
            if (cause instanceof Exception) {
                throw (Exception) cause;
            }
            throw ee;
        } finally {
            executor.shutdownNow();
        }
    }

    private void triggerAlarm(CollectTaskEntity task, Exception error) {
        try {
            Exception cause = Optional.ofNullable(error).orElseGet(() -> new RuntimeException("Task failed"));
            unifyAlarmOpsService.triggerAlarm(AlarmModelUtils.buildAlarmModel(task, cause));
        } catch (Exception e) {
            log.warn("Failed to trigger alarm for task {}: {}", task.getId(), e.getMessage());
        }
    }

    /** Backward-compatible single attempt execution that never throws. */
    public CollectResult execute(CollectTaskEntity task) {
        try {
            return executeOnce(task);
        } catch (Exception e) {
            log.error("Task execution failed: {}", task.getName(), e);
            return new CollectResult(0, 0, 0, 0);
        }
    }

    private CollectResult executeOnce(CollectTaskEntity task) throws Exception {
        ExecutionLogEntity logEntity = createExecutionLog(task);
        return executeOnce(task, logEntity);
    }

    private CollectResult executeOnce(CollectTaskEntity task, ExecutionLogEntity logEntity) throws Exception {
        long startTime = System.currentTimeMillis();

        List<ExecutionStageLogEntity> stageEntities = new ArrayList<>();
        StageMetrics readStage = new StageMetrics(Stage.READ, failureSampleLimit);
        StageMetrics detailStage = new StageMetrics(Stage.DETAIL, failureSampleLimit);
        StageMetrics transformStage = new StageMetrics(Stage.TRANSFORM, failureSampleLimit);
        StageMetrics writeStage = new StageMetrics(Stage.WRITE, failureSampleLimit);
        StreamingState state = new StreamingState();

        try {
            ApiConfigEntity apiConfig = apiConfigDao.getById(task.getApiConfigId());
            if (apiConfig == null) {
                throw new RuntimeException("API config not found: " + task.getApiConfigId());
            }
            DatasourceConfigEntity dsConfig = datasourceConfigDao.getById(task.getDatasourceId());
            if (dsConfig == null) {
                throw new RuntimeException("Datasource not found: " + task.getDatasourceId());
            }

            log.info("Starting task: {} (apiConfigId={}, datasourceId={})", task.getName(), task.getApiConfigId(), task.getDatasourceId());
            updateProgress(logEntity, Stage.READ, 0);

            // 逐页流式：读取一页 → 映射/转换 → 写库 → 释放，再取下一页
            state.activeStage = readStage;
            long loopStart = System.currentTimeMillis();
            int pageCount = pageCollector.collectPages(apiConfig, (records, pageIndex) -> {
                processPage(task, logEntity, apiConfig, dsConfig, records, state,
                        readStage, detailStage, transformStage, writeStage);
                return true;
            });
            long loopElapsed = System.currentTimeMillis() - loopStart;
            state.pageCount = pageCount;

            readStage.setCostMs(Math.max(0, loopElapsed - state.enrichCost - state.transformCost - state.writeCost));
            detailStage.setCostMs(state.enrichCost);
            transformStage.setCostMs(state.transformCost);
            writeStage.setCostMs(state.writeCost);
            stageEntities.add(readStage.toEntity(logEntity.getId()));
            if (detailStage.getInputCount() > 0) {
                stageEntities.add(detailStage.toEntity(logEntity.getId()));
            }
            if (state.totalRecords > 0) {
                stageEntities.add(transformStage.toEntity(logEntity.getId()));
                stageEntities.add(writeStage.toEntity(logEntity.getId()));
            }

            long costMs = System.currentTimeMillis() - startTime;
            completeLog(logEntity, state.totalRecords, state.totalSuccess, state.totalFail, pageCount, costMs);

        } catch (Exception e) {
            log.error("Task execution failed: {}", task.getName(), e);
            recordFailure(stageEntities, logEntity, state, readStage, detailStage, transformStage, writeStage, e);
            failLog(logEntity, e, System.currentTimeMillis() - startTime);
            throw e;
        } finally {
            if (state.session != null) {
                state.session.close();
            }
            persistStageLogs(stageEntities);
        }

        CollectResult result = new CollectResult(state.totalRecords, state.totalSuccess, state.totalFail, state.pageCount);
        result.setCostMs(System.currentTimeMillis() - startTime);
        return result;
    }

    /**
     * 处理单页：累计 READ 计数 → （首页时）打开写库会话并加载字段映射 → 映射/转换当前页 →
     * 写库当前页 → 更新进度。转换/写库异常直接向上传播（由 executeOnce 记录失败）。
     */
    private void processPage(CollectTaskEntity task, ExecutionLogEntity logEntity,
                             ApiConfigEntity apiConfig, DatasourceConfigEntity dsConfig,
                             List<Map<String, Object>> records, StreamingState state,
                             StageMetrics readStage, StageMetrics detailStage,
                             StageMetrics transformStage, StageMetrics writeStage) {
        readStage.setInputCount(readStage.getInputCount() + records.size());
        readStage.setOutputCount(readStage.getOutputCount() + records.size());
        readStage.setSuccessCount(readStage.getSuccessCount() + records.size());
        state.totalRecords += records.size();
        state.activeStage = null;
        state.activeSourceRecord = null;

        if (records.isEmpty()) {
            return;
        }

        // 详情补全：在写库会话打开之前执行（网络 IO 不应长时间占用目标库连接，design R5）。
        // 未启用时 enrich 原样返回、不产生阶段计数（design C1）。
        state.activeStage = detailStage;
        state.activeSourceRecord = records.get(0);
        long enrichStart = System.currentTimeMillis();
        detailEnricher.enrich(records, apiConfig, detailStage);
        state.enrichCost += System.currentTimeMillis() - enrichStart;
        state.activeStage = null;
        if (detailStage.getInputCount() > 0) {
            updateProgress(logEntity, Stage.DETAIL, state.totalRecords);
        }

        if (state.session == null) {
            state.mappings = fieldMappingDao.getByTaskId(task.getId());
            state.session = dataPersister.openSession(task, dsConfig, state.mappings);
            state.session.prepare();
        }
        List<FieldMappingEntity> mappings = state.mappings;

        state.activeStage = transformStage;
        state.activeSourceRecord = records.get(0);
        long transformStart = System.currentTimeMillis();
        List<Map<String, Object>> mapped = fieldMapper.mapFields(records, mappings);
        List<Map<String, Object>> transformed = dataTransformChain.execute(mapped, apiConfig, mappings);
        state.transformCost += System.currentTimeMillis() - transformStart;
        transformStage.setInputCount(transformStage.getInputCount() + records.size());
        transformStage.setOutputCount(transformStage.getOutputCount() + transformed.size());
        transformStage.setSuccessCount(transformStage.getSuccessCount() + transformed.size());
        state.activeStage = null;

        state.activeStage = writeStage;
        state.activeSourceRecord = transformed.isEmpty() ? null : transformed.get(0);
        long writeStart = System.currentTimeMillis();
        DataWriteResult writeResult = state.session.write(transformed, writeStage.getFailureCollector());
        state.writeCost += System.currentTimeMillis() - writeStart;
        writeStage.setInputCount(writeStage.getInputCount() + transformed.size());
        writeStage.setOutputCount(writeStage.getOutputCount() + writeResult.getSuccessCount());
        writeStage.setSuccessCount(writeStage.getSuccessCount() + writeResult.getSuccessCount());
        writeStage.setFailCount(writeStage.getFailCount() + writeResult.getFailCount());
        state.totalSuccess += writeResult.getSuccessCount();
        state.totalFail += writeResult.getFailCount();
        state.activeStage = null;

        updateProgress(logEntity, Stage.WRITE, state.totalRecords);
    }

    /** 记录失败阶段与失败样本，并重建应持久化的阶段日志（仅包含已进入的阶段）。 */
    private void recordFailure(List<ExecutionStageLogEntity> stageEntities, ExecutionLogEntity logEntity,
                               StreamingState state, StageMetrics readStage, StageMetrics detailStage,
                               StageMetrics transformStage, StageMetrics writeStage, Exception e) {
        stageEntities.clear();
        if (state.activeStage != null) {
            state.activeStage.getFailureCollector().add(e.getMessage(), state.activeSourceRecord);
            state.activeStage.setFailCount(Math.max(state.activeStage.getFailCount(), 1));
        }
        if (state.activeStage != null || readStage.getInputCount() > 0) {
            stageEntities.add(readStage.toEntity(logEntity.getId()));
        }
        if (detailStage.getInputCount() > 0 || state.activeStage == detailStage) {
            stageEntities.add(detailStage.toEntity(logEntity.getId()));
        }
        if (transformStage.getInputCount() > 0 || state.activeStage == transformStage) {
            stageEntities.add(transformStage.toEntity(logEntity.getId()));
        }
        if (writeStage.getInputCount() > 0 || state.activeStage == writeStage) {
            stageEntities.add(writeStage.toEntity(logEntity.getId()));
        }
    }

    /** 单次执行内跨页共享的流式状态。 */
    private static class StreamingState {
        List<FieldMappingEntity> mappings;
        DataPersister.WriteSession session;
        StageMetrics activeStage;
        Object activeSourceRecord;
        int totalRecords;
        int totalSuccess;
        int totalFail;
        int pageCount;
        long enrichCost;
        long transformCost;
        long writeCost;
    }

    private void updateProgress(ExecutionLogEntity logEntity, Stage stage, int processed) {
        try {
            logEntity.setCurrentStage(stage);
            logEntity.setProcessedCount(processed);
            logEntity.setUpdatedAt(new Date());
            executionLogDao.updateById(logEntity);
        } catch (Exception e) {
            log.warn("Failed to update execution progress for log {}: {}", logEntity.getId(), e.getMessage());
        }
    }

    private void persistStageLogs(List<ExecutionStageLogEntity> stageEntities) {
        if (stageEntities.isEmpty()) {
            return;
        }
        for (ExecutionStageLogEntity entity : stageEntities) {
            try {
                executionStageLogDao.insert(entity);
            } catch (Exception e) {
                log.warn("Failed to persist stage log for execution {}: {}", entity.getExecutionId(), e.getMessage());
            }
        }
    }

    private ExecutionLogEntity createExecutionLog(CollectTaskEntity task) {
        ExecutionLogEntity logEntity = new ExecutionLogEntity();
        logEntity.setTaskId(task.getId());
        logEntity.setStatus(ExecutionStatus.RUNNING);
        logEntity.setStartTime(new Date());
        executionLogDao.insert(logEntity);
        return logEntity;
    }

    private void completeLog(ExecutionLogEntity logEntity, int totalRecords, int successCount,
                             int failCount, int pageCount, long costMs) {
        logEntity.setStatus(ExecutionStatus.SUCCESS);
        logEntity.setCurrentStage(Stage.DONE);
        logEntity.setProcessedCount(successCount);
        logEntity.setEndTime(new Date());
        logEntity.setTotalRecords(totalRecords);
        logEntity.setSuccessCount(successCount);
        logEntity.setFailCount(failCount);
        logEntity.setPageCount(pageCount);
        logEntity.setCostMs(costMs);
        logEntity.setUpdatedAt(new Date());
        executionLogDao.updateById(logEntity);
    }

    private void failLog(ExecutionLogEntity logEntity, Exception e, long costMs) {
        logEntity.setStatus(ExecutionStatus.FAILED);
        logEntity.setEndTime(new Date());
        logEntity.setErrorMsg(e.getMessage());
        logEntity.setCostMs(costMs);
        logEntity.setUpdatedAt(new Date());
        executionLogDao.updateById(logEntity);
    }
}
