package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.admin.dto.TaskDetailResponse;
import com.gitee.apigrab.admin.dto.TaskSaveRequest;
import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.dto.request.TaskInput;
import com.gitee.apigrab.admin.scheduler.TaskSchedulerManager;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.common.exception.CommonException;
import com.gitee.apigrab.common.exception.ResponseErrorCode;
import com.gitee.apigrab.core.collector.CollectorEngine;
import com.gitee.apigrab.core.http.HttpFetcher;
import com.gitee.apigrab.core.http.HttpResponseParser;
import com.gitee.apigrab.core.mapping.FieldMapper;
import com.gitee.apigrab.persistence.dao.ApiConfigDao;
import com.gitee.apigrab.persistence.dao.CollectTaskDao;
import com.gitee.apigrab.persistence.dao.DatasourceConfigDao;
import com.gitee.apigrab.persistence.dao.ExecutionLogDao;
import com.gitee.apigrab.persistence.dao.ExecutionStageLogDao;
import com.gitee.apigrab.persistence.dao.FieldMappingDao;
import com.gitee.apigrab.persistence.entity.enums.ScheduleType;
import com.gitee.apigrab.persistence.entity.enums.TaskStatus;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
public class CollectTaskService {

    /** 与列定义 `rc_collect_task.name VARCHAR(100)` 对应（design C10）。就近定义：各表宽度可独立变化。 */
    private static final int NAME_MAX_LENGTH = 100;

    /** 手动触发中的任务 ID（仅本节点生效），用于闭合「查运行中 → 触发」的并发窗口。 */
    private final Set<Long> triggeringTaskIds = ConcurrentHashMap.newKeySet();

    @Resource
    private CollectTaskDao collectTaskDao;

    @Resource
    private ApiConfigDao apiConfigDao;

    @Resource
    private DatasourceConfigDao datasourceConfigDao;

    @Resource
    private FieldMappingDao fieldMappingDao;

    @Resource
    private ExecutionLogDao executionLogDao;

    @Resource
    private ExecutionStageLogDao executionStageLogDao;

    @Resource
    private CollectorEngine collectorEngine;

    @Resource
    private TaskSchedulerManager taskSchedulerManager;

    @Resource
    private HttpFetcher httpFetcher;

    @Resource
    private HttpResponseParser responseParser;

    @Resource
    private FieldMapper fieldMapper;

    public CollectTaskEntity getById(Long id) {
        CollectTaskEntity entity = collectTaskDao.getById(id);
        if (entity == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "任务不存在");
        }
        return entity;
    }

    public List<CollectTaskEntity> list(CollectTaskEntity param) {
        return collectTaskDao.list(param);
    }

    public void create(CollectTaskEntity entity) {
        if (entity.getBatchSize() == null) {
            entity.setBatchSize(500);
        }
        if (entity.getWriteMode() == null) {
            entity.setWriteMode(WriteMode.INSERT);
        }
        if (entity.getScheduleType() == null) {
            entity.setScheduleType(ScheduleType.MANUAL);
        }
        if (entity.getStatus() == null) {
            entity.setStatus(TaskStatus.NOT_STARTED);
        }

        collectTaskDao.insert(entity);

        // 如果是 CRON 类型，注册到 Quartz
        if (entity.getScheduleType() == ScheduleType.CRON && entity.getCronExpression() != null) {
            taskSchedulerManager.registerTask(entity);
        }
    }

    public void update(CollectTaskEntity entity) {
        CollectTaskEntity old = getById(entity.getId());

        // 取消旧的任务调度
        if (old.getScheduleType() == ScheduleType.CRON) {
            taskSchedulerManager.unregisterTask(old.getId());
        }

        collectTaskDao.updateById(entity);

        // 重新注册
        if (entity.getScheduleType() == ScheduleType.CRON && entity.getCronExpression() != null) {
            taskSchedulerManager.registerTask(entity);
        }
    }

    @Transactional
    public void delete(Long id) {
        CollectTaskEntity entity = collectTaskDao.getById(id);
        if (entity == null) {
            return;
        }
        // 运行中的任务不能删除：引擎持有任务快照，删除后仍会写入执行记录，产生无法再级联清理的孤儿数据
        if (hasRunningExecution(id)) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "任务正在执行中，无法删除");
        }
        // 执行阶段明细以执行记录为父，必须先于执行记录删除（子查询依赖其存在）
        executionStageLogDao.deleteByTaskId(id);
        executionLogDao.deleteByTaskId(id);
        // 仅删除任务与其字段映射；被关联的接口配置（可能被其它任务共享）保留
        fieldMappingDao.deleteByTaskId(id);
        collectTaskDao.deleteById(id);
        // 注销调度不参与数据库事务，提交后再执行；否则删除回滚后任务仍在但调度已消失
        if (entity.getScheduleType() == ScheduleType.CRON) {
            runAfterCommit(() -> taskSchedulerManager.unregisterTask(id));
        }
    }

    @Transactional
    public void createFromRequest(TaskSaveRequest request) {
        CollectTaskEntity task = toTaskEntity(request.getTask());
        task.setName(requireUniqueTaskName(task.getName(), null));
        validateApiConfig(task.getApiConfigId());
        validateDatasource(task.getDatasourceId());
        applyTaskDefaults(task);
        collectTaskDao.insert(task);
        saveMappings(task.getId(), request.getMappings());
        if (task.getScheduleType() == ScheduleType.CRON && task.getCronExpression() != null) {
            // 注册调度放到事务提交后：注册失败不应回滚已保存的任务，回滚也不应留下调度
            runAfterCommit(() -> taskSchedulerManager.registerTask(task));
        }
    }

    @Transactional
    public void updateFromRequest(Long id, TaskSaveRequest request) {
        CollectTaskEntity old = getById(id);
        CollectTaskEntity task = toTaskEntity(request.getTask());
        task.setId(id);
        task.setName(requireUniqueTaskName(task.getName(), id));
        validateApiConfig(task.getApiConfigId());
        validateDatasource(task.getDatasourceId());
        applyTaskDefaults(task);

        boolean wasScheduled = old.getScheduleType() == ScheduleType.CRON;
        boolean needsSchedule = task.getScheduleType() == ScheduleType.CRON && task.getCronExpression() != null;

        saveMappings(id, request.getMappings());
        collectTaskDao.updateById(task);
        if (wasScheduled || needsSchedule) {
            // 注销旧调度 + 注册新调度必须在提交后按序执行，避免回滚后调度与任务数据不一致
            runAfterCommit(() -> {
                if (wasScheduled) {
                    taskSchedulerManager.unregisterTask(id);
                }
                if (needsSchedule) {
                    taskSchedulerManager.registerTask(task);
                }
            });
        }
    }

    public TaskDetailResponse getDetail(Long id) {
        CollectTaskEntity task = getById(id);
        ApiConfigEntity apiConfig = Optional.ofNullable(task.getApiConfigId()).map(apiConfigDao::getById).orElse(null);
        List<FieldMappingEntity> mappings = fieldMappingDao.getByTaskId(id);
        return new TaskDetailResponse(task, apiConfig, mappings);
    }

    private void validateApiConfig(Long apiConfigId) {
        if (apiConfigId == null || apiConfigDao.getById(apiConfigId) == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "接口配置不存在");
        }
    }

    /**
     * 任务必须引用一个存在的数据源。{@code APIGRAB_COLLECT_TASK.datasource_id} 是 NOT NULL 且没有外键，
     * 不校验会让无效引用一路存进库，直到执行写库阶段才报错。
     */
    private void validateDatasource(Long datasourceId) {
        if (datasourceId == null || datasourceConfigDao.getById(datasourceId) == null) {
            throw new CommonException(ResponseErrorCode.NOT_FOUND, "数据源不存在");
        }
    }

    /**
     * 在事务提交后执行调度副作用（Quartz 使用独立事务，无法随数据库事务回滚）。
     * 无事务上下文时（例如单元测试直接调用）立即执行。
     * 提交后阶段已无法回滚数据，失败只能记录日志，避免"数据已写入却报错"的误导。
     */
    private void runAfterCommit(Runnable action) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            action.run();
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    action.run();
                } catch (RuntimeException e) {
                    log.error("Failed to sync scheduler after commit", e);
                }
            }
        });
    }

    /**
     * 名称归一与唯一性校验，返回 trim 后的名称（design C3/C4/C5/C6）。
     *
     * <p><b>必须由 {@code createFromRequest} / {@code updateFromRequest} 调用</b>——控制器走的
     * 是这两个入口，它们直接操作 DAO 而不经过 {@code create} / {@code update}；把校验加在后两者
     * 上不会生效。
     *
     * <p>判重依赖 {@code getByName} 的 SQL 等值查询，**不在 Java 层比较字符串**：表使用 MySQL
     * 默认（大小写不敏感）排序规则，SQL 等值天然忽略大小写，与数据库唯一索引判定一致。
     *
     * @param selfId 更新时传入自身 id 以排除自己；新增时传 {@code null}
     */
    private String requireUniqueTaskName(String rawName, Long selfId) {
        String name = rawName == null ? null : rawName.trim();
        if (name == null || name.isEmpty()) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "采集任务名称不能为空");
        }
        if (name.length() > NAME_MAX_LENGTH) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST,
                "采集任务名称长度不能超过 " + NAME_MAX_LENGTH + " 个字符");
        }
        CollectTaskEntity existing = collectTaskDao.getByName(name);
        if (existing != null && (selfId == null || !selfId.equals(existing.getId()))) {
            throw new CommonException(ResponseErrorCode.BAD_REQUEST, "采集任务名称已存在");
        }
        return name;
    }

    private void applyTaskDefaults(CollectTaskEntity task) {
        if (task.getBatchSize() == null) task.setBatchSize(500);
        if (task.getWriteMode() == null) task.setWriteMode(WriteMode.INSERT);
        if (task.getScheduleType() == null) task.setScheduleType(ScheduleType.MANUAL);
        if (task.getStatus() == null) task.setStatus(TaskStatus.NOT_STARTED);
        if (task.getRetryTimes() == null) task.setRetryTimes(0);
        if (task.getTimeoutMs() == null) task.setTimeoutMs(0L);
        if (task.getAlarmEnabled() == null) task.setAlarmEnabled(0);
    }

    private CollectTaskEntity toTaskEntity(TaskInput input) {
        CollectTaskEntity task = new CollectTaskEntity();
        if (input != null) {
            BeanUtils.copyProperties(input, task);
        }
        return task;
    }

    private void saveMappings(Long taskId, List<FieldMappingInput> mappings) {
        if (taskId == null) {
            return;
        }
        fieldMappingDao.deleteByTaskId(taskId);
        if (mappings != null && !mappings.isEmpty()) {
            List<FieldMappingEntity> entities = new ArrayList<>(mappings.size());
            for (FieldMappingInput mapping : mappings) {
                FieldMappingEntity entity = new FieldMappingEntity();
                BeanUtils.copyProperties(mapping, entity);
                entity.setTaskId(taskId);
                entities.add(entity);
            }
            fieldMappingDao.batchInsert(entities);
        }
    }

    /**
     * 手动"立即执行"：后台异步执行并返回本次执行记录标识，供前端跳转到执行详情。
     * 若该任务已有运行中的执行记录，则拒绝重复触发。
     *
     * <p>「查运行中 → 触发」之间存在并发窗口：执行记录由引擎在后台线程创建，两次几乎同时的
     * 请求可能都查不到 RUNNING。{@code triggeringTaskIds} 用于在本节点内串行化这个窗口；
     * 跨节点仍需数据库层约束，当前未覆盖。
     */
    public Long executeAsync(Long id) {
        CollectTaskEntity task = getById(id);
        if (!triggeringTaskIds.add(id)) {
            throw new CommonException(ResponseErrorCode.TASK_EXECUTION_ERROR, "任务正在执行中，请稍后再试");
        }
        try {
            if (hasRunningExecution(id)) {
                throw new CommonException(ResponseErrorCode.TASK_EXECUTION_ERROR, "任务正在执行中，请稍后再试");
            }
            return collectorEngine.executeAsync(task);
        } finally {
            triggeringTaskIds.remove(id);
        }
    }

    private boolean hasRunningExecution(Long taskId) {
        return executionLogDao.countRunningByTaskId(taskId) > 0;
    }

    public void pause(Long id) {
        CollectTaskEntity task = getById(id);
        task.setStatus(TaskStatus.PAUSED);
        collectTaskDao.updateById(task);
        if (task.getScheduleType() == ScheduleType.CRON) {
            taskSchedulerManager.pauseTask(id);
        }
    }

    public void resume(Long id) {
        CollectTaskEntity task = getById(id);
        task.setStatus(TaskStatus.RUNNING);
        collectTaskDao.updateById(task);
        if (task.getScheduleType() == ScheduleType.CRON) {
            taskSchedulerManager.resumeTask(id);
        }
    }

    public List<Map<String, Object>> preview(Long id) {
        CollectTaskEntity task = getById(id);
        ApiConfigEntity apiConfig = apiConfigDao.getById(task.getApiConfigId());
        List<FieldMappingEntity> mappings = fieldMappingDao.getByTaskId(id);

        try {
            String response = httpFetcher.fetch(apiConfig);
            List<Map<String, Object>> rawData = responseParser.parse(response, apiConfig);

            // 只返回前 10 条
            List<Map<String, Object>> previewData = rawData.size() > 10 ? rawData.subList(0, 10) : rawData;
            return fieldMapper.mapFields(previewData, mappings);
        } catch (Exception e) {
            throw new RuntimeException("Preview failed: " + e.getMessage(), e);
        }
    }
}
