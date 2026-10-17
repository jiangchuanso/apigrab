package com.gitee.apigrab.admin.service;

import com.gitee.apigrab.admin.dto.TaskDetailResponse;
import com.gitee.apigrab.admin.dto.TaskSaveRequest;
import com.gitee.apigrab.admin.dto.request.FieldMappingInput;
import com.gitee.apigrab.admin.dto.request.TaskInput;
import com.gitee.apigrab.admin.scheduler.TaskSchedulerManager;
import com.gitee.apigrab.persistence.entity.ApiConfigEntity;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.common.exception.CommonException;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectTaskServiceTest {

    @Mock private CollectTaskDao collectTaskDao;
    @Mock private ApiConfigDao apiConfigDao;
    @Mock private DatasourceConfigDao datasourceConfigDao;
    @Mock private FieldMappingDao fieldMappingDao;
    @Mock private ExecutionLogDao executionLogDao;
    @Mock private ExecutionStageLogDao executionStageLogDao;
    @Mock private CollectorEngine collectorEngine;
    @Mock private TaskSchedulerManager taskSchedulerManager;
    @Mock private HttpFetcher httpFetcher;
    @Mock private HttpResponseParser responseParser;
    @Mock private FieldMapper fieldMapper;

    @InjectMocks private CollectTaskService service;

    private CollectTaskEntity task;

    @BeforeEach
    void setUp() {
        task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("test task");
        task.setApiConfigId(10L);
        task.setDatasourceId(20L);
        task.setTargetTable("test_table");
    }

    @Test
    void testGetById() {
        when(collectTaskDao.getById(1L)).thenReturn(task);
        CollectTaskEntity result = service.getById(1L);
        assertEquals("test task", result.getName());
    }

    @Test
    void testGetByIdNotFound() {
        when(collectTaskDao.getById(999L)).thenReturn(null);
        assertThrows(CommonException.class, () -> service.getById(999L));
    }

    @Test
    void testCreateManualTask() {
        task.setScheduleType(ScheduleType.MANUAL);
        service.create(task);
        assertEquals(TaskStatus.NOT_STARTED, task.getStatus());
        assertEquals(500, task.getBatchSize().intValue());
        assertEquals(WriteMode.INSERT, task.getWriteMode());
        verify(collectTaskDao, times(1)).insert(task);
        verify(taskSchedulerManager, never()).registerTask(any());
    }

    @Test
    void testCreateCronTask() {
        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 0 2 * * ?");
        service.create(task);
        verify(taskSchedulerManager, times(1)).registerTask(task);
    }

    @Test
    void testUpdateReplacesScheduler() {
        CollectTaskEntity oldTask = new CollectTaskEntity();
        oldTask.setId(1L);
        oldTask.setScheduleType(ScheduleType.CRON);
        oldTask.setCronExpression("0 0 * * * ?");
        when(collectTaskDao.getById(1L)).thenReturn(oldTask);

        task.setScheduleType(ScheduleType.CRON);
        task.setCronExpression("0 30 * * * ?");
        service.update(task);

        verify(taskSchedulerManager, times(1)).unregisterTask(1L);
        verify(taskSchedulerManager, times(1)).registerTask(task);
    }

    @Test
    void testUpdateManualTask() {
        CollectTaskEntity oldTask = new CollectTaskEntity();
        oldTask.setId(1L);
        oldTask.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(oldTask);

        task.setScheduleType(ScheduleType.MANUAL);
        service.update(task);

        verify(taskSchedulerManager, never()).unregisterTask(any());
        verify(taskSchedulerManager, never()).registerTask(any());
    }

    @Test
    void testDeleteCronTask() {
        task.setScheduleType(ScheduleType.CRON);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        service.delete(1L);
        verify(taskSchedulerManager, times(1)).unregisterTask(1L);
        verify(collectTaskDao, times(1)).deleteById(1L);
    }

    @Test
    void testDeleteManualTask() {
        task.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        service.delete(1L);
        verify(taskSchedulerManager, never()).unregisterTask(any());
        verify(collectTaskDao, times(1)).deleteById(1L);
    }

    @Test
    void testExecuteAsync() {
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(executionLogDao.countRunningByTaskId(1L)).thenReturn(0L);
        when(collectorEngine.executeAsync(task)).thenReturn(88L);

        Long executionId = service.executeAsync(1L);

        assertEquals(88L, executionId);
    }

    @Test
    void testExecuteAsyncRejectedWhenRunning() {
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(executionLogDao.countRunningByTaskId(1L)).thenReturn(1L);

        assertThrows(CommonException.class, () -> service.executeAsync(1L));
        verify(collectorEngine, never()).executeAsync(any());
    }

    @Test
    void testPause() {
        task.setScheduleType(ScheduleType.CRON);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        service.pause(1L);
        assertEquals(TaskStatus.PAUSED, task.getStatus());
        verify(taskSchedulerManager, times(1)).pauseTask(1L);
    }

    @Test
    void testPauseManual() {
        task.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        service.pause(1L);
        assertEquals(TaskStatus.PAUSED, task.getStatus());
        verify(taskSchedulerManager, never()).pauseTask(any());
    }

    @Test
    void testResume() {
        task.setScheduleType(ScheduleType.CRON);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        service.resume(1L);
        assertEquals(TaskStatus.RUNNING, task.getStatus());
        verify(taskSchedulerManager, times(1)).resumeTask(1L);
    }

    @Test
    void testCreateFromRequestRejectsMissingApiConfig() {
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput t = new TaskInput();
        t.setApiConfigId(99L);
        // 需要一个合法名称才能走到「接口配置校验」这一步（名称唯一性校验在其之前）
        t.setName("missing-api-config-task");
        request.setTask(t);
        when(apiConfigDao.getById(99L)).thenReturn(null);

        assertThrows(CommonException.class, () -> service.createFromRequest(request));
        verify(collectTaskDao, never()).insert(any());
    }

    @Test
    void testCreateFromRequestRejectsMissingDatasource() {
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput t = new TaskInput();
        t.setApiConfigId(10L);
        t.setDatasourceId(99L);
        t.setName("missing-datasource-task");
        request.setTask(t);
        when(apiConfigDao.getById(10L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(99L)).thenReturn(null);

        CommonException ex = assertThrows(CommonException.class,
            () -> service.createFromRequest(request));

        assertTrue(ex.getMessage().contains("数据源"), "文案应可读: " + ex.getMessage());
        verify(collectTaskDao, never()).insert(any());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateFromRequestSavesMappingsByTaskId() {
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput t = new TaskInput();
        t.setApiConfigId(10L);
        t.setDatasourceId(20L);
        t.setName("t");
        request.setTask(t);
        FieldMappingInput mapping = new FieldMappingInput();
        mapping.setSourceField("$.a");
        request.setMappings(Collections.singletonList(mapping));
        stubExistingReferences();
        doAnswer(inv -> {
            ((CollectTaskEntity) inv.getArgument(0)).setId(77L);
            return 1;
        }).when(collectTaskDao).insert(any());

        service.createFromRequest(request);

        ArgumentCaptor<List<FieldMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldMappingDao).deleteByTaskId(77L);
        verify(fieldMappingDao).batchInsert(captor.capture());
        assertEquals(77L, captor.getValue().get(0).getTaskId());
        assertEquals("$.a", captor.getValue().get(0).getSourceField());
    }

    // ---------- 名称唯一性（design C1/C3/C4/C5/C6）----------
    // 注意：控制器调用的是 createFromRequest / updateFromRequest（不是 create/update），
    // 因此用例必须直接打这两个入口，否则不构成对实时路径的守护。

    private TaskSaveRequest requestWithName(String name) {
        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput t = new TaskInput();
        t.setApiConfigId(10L);
        t.setDatasourceId(20L);
        t.setName(name);
        request.setTask(t);
        return request;
    }

    /** 成功路径需要接口配置与数据源都存在（两个引用都会校验）。 */
    private void stubExistingReferences() {
        when(apiConfigDao.getById(10L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(20L)).thenReturn(new DatasourceConfigEntity());
    }

    @Test
    void testCreateFromRequestRejectsDuplicateName() {
        CollectTaskEntity existing = new CollectTaskEntity();
        existing.setId(99L);
        existing.setName("订单任务");
        when(collectTaskDao.getByName("订单任务")).thenReturn(existing);

        CommonException ex = assertThrows(CommonException.class,
            () -> service.createFromRequest(requestWithName("订单任务")));

        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(collectTaskDao, never()).insert(any());
    }

    @Test
    void testCreateFromRequestRejectsBlankName() {
        CommonException ex = assertThrows(CommonException.class,
            () -> service.createFromRequest(requestWithName("   ")));

        assertTrue(ex.getMessage().contains("不能为空"), "文案应可读: " + ex.getMessage());
        verify(collectTaskDao, never()).insert(any());
    }

    /**
     * 判重依赖 SQL 等值查询的结果（C4）：getByName 返回“仅大小写不同”的记录时，
     * 服务层应照单判重，而不是自己比较大小写后放行。
     */
    @Test
    void testCreateFromRequestRejectsNameMatchedByDatabaseEquality() {
        CollectTaskEntity existing = new CollectTaskEntity();
        existing.setId(99L);
        existing.setName("OrderTask");
        when(collectTaskDao.getByName("ORDERTASK")).thenReturn(existing);

        assertThrows(CommonException.class,
            () -> service.createFromRequest(requestWithName("ORDERTASK")));
        verify(collectTaskDao, never()).insert(any());
    }

    @Test
    void testCreateFromRequestTrimsName() {
        when(collectTaskDao.getByName("订单任务")).thenReturn(null);
        stubExistingReferences();

        service.createFromRequest(requestWithName("  订单任务  "));

        ArgumentCaptor<CollectTaskEntity> captor = ArgumentCaptor.forClass(CollectTaskEntity.class);
        verify(collectTaskDao).insert(captor.capture());
        assertEquals("订单任务", captor.getValue().getName(), "保存前应去除首尾空白");
    }

    @Test
    void testUpdateFromRequestRejectsNameUsedByAnother() {
        CollectTaskEntity old = new CollectTaskEntity();
        old.setId(1L);
        when(collectTaskDao.getById(1L)).thenReturn(old);

        CollectTaskEntity other = new CollectTaskEntity();
        other.setId(2L);
        other.setName("订单任务");
        when(collectTaskDao.getByName("订单任务")).thenReturn(other);

        CommonException ex = assertThrows(CommonException.class,
            () -> service.updateFromRequest(1L, requestWithName("订单任务")));

        assertTrue(ex.getMessage().contains("已存在"), "文案应可读: " + ex.getMessage());
        verify(collectTaskDao, never()).updateById(any());
    }

    @Test
    void testUpdateFromRequestAllowsSameNameOnItself() {
        CollectTaskEntity old = new CollectTaskEntity();
        old.setId(1L);
        when(collectTaskDao.getById(1L)).thenReturn(old);

        CollectTaskEntity self = new CollectTaskEntity();
        self.setId(1L);
        self.setName("订单任务");
        when(collectTaskDao.getByName("订单任务")).thenReturn(self);
        stubExistingReferences();

        service.updateFromRequest(1L, requestWithName("订单任务"));

        verify(collectTaskDao).updateById(any());
    }

    // ---------- 名称长度上限（design C10）----------

    @Test
    void testCreateFromRequestRejectsOverlongName() {
        CommonException ex = assertThrows(CommonException.class,
            () -> service.createFromRequest(requestWithName(repeat('a', 101))));

        assertTrue(ex.getMessage().contains("长度"), "文案应可读: " + ex.getMessage());
        verify(collectTaskDao, never()).insert(any());
    }

    @Test
    void testCreateFromRequestAcceptsNameAtExactLimit() {
        when(collectTaskDao.getByName(repeat('a', 100))).thenReturn(null);
        stubExistingReferences();

        service.createFromRequest(requestWithName(repeat('a', 100)));

        verify(collectTaskDao).insert(any());
    }

    @Test
    void testUpdateFromRequestRejectsOverlongName() {
        CollectTaskEntity old = new CollectTaskEntity();
        old.setId(1L);
        when(collectTaskDao.getById(1L)).thenReturn(old);

        assertThrows(CommonException.class,
            () -> service.updateFromRequest(1L, requestWithName(repeat('a', 101))));
        verify(collectTaskDao, never()).updateById(any());
    }

    private static String repeat(char c, int times) {
        StringBuilder sb = new StringBuilder(times);
        for (int i = 0; i < times; i++) {
            sb.append(c);
        }
        return sb.toString();
    }

    @Test
    @SuppressWarnings("unchecked")
    void testUpdateFromRequestSavesMappingsByTaskId() {
        CollectTaskEntity old = new CollectTaskEntity();
        old.setId(1L);
        old.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(old);

        TaskSaveRequest request = new TaskSaveRequest();
        TaskInput t = new TaskInput();
        t.setApiConfigId(10L);
        t.setDatasourceId(20L);
        // 名称唯一性校验先于接口配置校验执行，须给出合法名称
        t.setName("mapping-save-task");
        request.setTask(t);
        FieldMappingInput mapping = new FieldMappingInput();
        request.setMappings(Collections.singletonList(mapping));
        stubExistingReferences();

        service.updateFromRequest(1L, request);

        ArgumentCaptor<List<FieldMappingEntity>> captor = ArgumentCaptor.forClass(List.class);
        verify(fieldMappingDao).deleteByTaskId(1L);
        verify(fieldMappingDao).batchInsert(captor.capture());
        assertEquals(1L, captor.getValue().get(0).getTaskId());
        verify(collectTaskDao).updateById(any());
    }

    @Test
    void testDeleteRemovesMappingsAndExecutionLogsButKeepsApiConfig() {
        task.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(task);

        service.delete(1L);

        // 阶段明细依赖执行记录，删除顺序必须保持「阶段明细 → 执行记录 → 字段映射 → 任务」
        InOrder inOrder = inOrder(executionStageLogDao, executionLogDao, fieldMappingDao, collectTaskDao);
        inOrder.verify(executionStageLogDao).deleteByTaskId(1L);
        inOrder.verify(executionLogDao).deleteByTaskId(1L);
        inOrder.verify(fieldMappingDao).deleteByTaskId(1L);
        inOrder.verify(collectTaskDao).deleteById(1L);
        verify(apiConfigDao, never()).deleteById(any());
    }

    @Test
    void testDeleteSkipsExecutionLogsWhenTaskMissing() {
        when(collectTaskDao.getById(999L)).thenReturn(null);

        service.delete(999L);

        verify(executionStageLogDao, never()).deleteByTaskId(any());
        verify(executionLogDao, never()).deleteByTaskId(any());
        verify(collectTaskDao, never()).deleteById(any());
    }

    @Test
    void testDeleteRejectedWhenTaskRunning() {
        task.setScheduleType(ScheduleType.MANUAL);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(executionLogDao.countRunningByTaskId(1L)).thenReturn(1L);

        assertThrows(CommonException.class, () -> service.delete(1L));

        // 运行中的任务必须整体拒绝，不能先删日志再报错
        verify(executionStageLogDao, never()).deleteByTaskId(any());
        verify(executionLogDao, never()).deleteByTaskId(any());
        verify(fieldMappingDao, never()).deleteByTaskId(any());
        verify(collectTaskDao, never()).deleteById(any());
    }

    @Test
    void testGetDetailReturnsTaskApiConfigAndMappings() {
        when(collectTaskDao.getById(1L)).thenReturn(task);
        ApiConfigEntity cfg = new ApiConfigEntity();
        cfg.setId(10L);
        when(apiConfigDao.getById(10L)).thenReturn(cfg);
        when(fieldMappingDao.getByTaskId(1L)).thenReturn(Collections.emptyList());

        TaskDetailResponse detail = service.getDetail(1L);

        assertEquals(1L, detail.getTask().getId());
        assertEquals(10L, detail.getApiConfig().getId());
        assertTrue(detail.getMappings().isEmpty());
    }
}
