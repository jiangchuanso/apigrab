package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.alarm.UnifyAlarmOpsService;
import com.gitee.apigrab.core.mapping.FieldMapper;
import com.gitee.apigrab.core.transform.DataTransformChain;
import com.gitee.apigrab.persistence.dao.*;
import com.gitee.apigrab.persistence.entity.*;
import com.gitee.apigrab.persistence.entity.enums.Stage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class CollectorEngineTest {

    private CollectorEngine engine;
    private ApiConfigDao apiConfigDao;
    private DatasourceConfigDao datasourceConfigDao;
    private CollectTaskDao collectTaskDao;
    private ExecutionLogDao executionLogDao;
    private ExecutionStageLogDao executionStageLogDao;
    private FieldMappingDao fieldMappingDao;
    private PageCollector pageCollector;
    private DetailEnricher detailEnricher;
    private DataTransformChain dataTransformChain;
    private FieldMapper fieldMapper;
    private DataPersister dataPersister;
    private UnifyAlarmOpsService unifyAlarmOpsService;

    @BeforeEach
    void setUp() throws Exception {
        engine = new CollectorEngine();
        apiConfigDao = Mockito.mock(ApiConfigDao.class);
        datasourceConfigDao = Mockito.mock(DatasourceConfigDao.class);
        collectTaskDao = Mockito.mock(CollectTaskDao.class);
        executionLogDao = Mockito.mock(ExecutionLogDao.class);
        executionStageLogDao = Mockito.mock(ExecutionStageLogDao.class);
        fieldMappingDao = Mockito.mock(FieldMappingDao.class);
        pageCollector = Mockito.mock(PageCollector.class);
        detailEnricher = Mockito.mock(DetailEnricher.class);
        dataTransformChain = Mockito.mock(DataTransformChain.class);
        fieldMapper = Mockito.mock(FieldMapper.class);
        dataPersister = Mockito.mock(DataPersister.class);
        unifyAlarmOpsService = Mockito.mock(UnifyAlarmOpsService.class);

        injectField("apiConfigDao", apiConfigDao);
        injectField("datasourceConfigDao", datasourceConfigDao);
        injectField("collectTaskDao", collectTaskDao);
        injectField("executionLogDao", executionLogDao);
        injectField("executionStageLogDao", executionStageLogDao);
        injectField("fieldMappingDao", fieldMappingDao);
        injectField("pageCollector", pageCollector);
        injectField("detailEnricher", detailEnricher);
        injectField("dataTransformChain", dataTransformChain);
        injectField("fieldMapper", fieldMapper);
        injectField("dataPersister", dataPersister);
        injectField("unifyAlarmOpsService", unifyAlarmOpsService);
    }

    private void injectField(String name, Object value) throws Exception {
        java.lang.reflect.Field field = CollectorEngine.class.getDeclaredField(name);
        field.setAccessible(true);
        field.set(engine, value);
    }

    @Test
    void testExecuteByTaskIdNotFound() {
        when(collectTaskDao.getById(999L)).thenReturn(null);
        RuntimeException ex = assertThrows(RuntimeException.class, () -> engine.executeByTaskId(999L));
        assertTrue(ex.getMessage().contains("not found"));
    }

    @Test
    void testExecuteAsyncReturnsExecutionIdAndRuns() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("异步任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(new DatasourceConfigEntity());
        when(executionLogDao.insert(any())).thenAnswer(inv -> {
            ((ExecutionLogEntity) inv.getArgument(0)).setId(555L);
            return 1;
        });
        when(pageCollector.collectPages(any(), any())).thenReturn(0);

        Long executionId = engine.executeAsync(task);

        assertEquals(555L, executionId);
    }

    @Test
    void testExecuteApiConfigNotFound() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(null);

        CollectResult result = engine.executeByTaskId(1L);
        // 应记录失败日志，返回默认结果
        verify(executionLogDao, atLeastOnce()).updateById(any());
    }

    @Test
    void testExecuteDatasourceNotFound() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(null);

        CollectResult result = engine.executeByTaskId(1L);
        verify(executionLogDao, atLeastOnce()).updateById(any());
    }

    @Test
    void testExecuteSuccess() throws Exception {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("测试任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);

        ApiConfigEntity apiConfig = new ApiConfigEntity();
        DatasourceConfigEntity dsConfig = new DatasourceConfigEntity();

        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(apiConfig);
        when(datasourceConfigDao.getById(200L)).thenReturn(dsConfig);
        when(executionLogDao.insert(any())).thenReturn(1);
        when(executionLogDao.updateById(any())).thenReturn(1);

        // 模拟 PageCollector 逐页回调
        Map<String, Object> record = new HashMap<>();
        record.put("name", "test");
        List<Map<String, Object>> rawData = Collections.singletonList(record);
        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(rawData, 1);
            return 1;
        });

        // 模拟字段映射
        when(fieldMappingDao.getByTaskId(1L)).thenReturn(Collections.emptyList());
        when(fieldMapper.mapFields(eq(rawData), any())).thenReturn(rawData);
        when(dataTransformChain.execute(eq(rawData), eq(apiConfig), any())).thenReturn(rawData);

        // 模拟写库会话
        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(eq(task), eq(dsConfig), any())).thenReturn(session);
        DataWriteResult writeResult = new DataWriteResult();
        writeResult.setSuccessCount(1);
        writeResult.setFailCount(0);
        when(session.write(eq(rawData), any())).thenReturn(writeResult);

        CollectResult result = engine.executeByTaskId(1L);
        assertEquals(1, result.getTotalRecords());
        assertEquals(1, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(1, result.getPageCount()); // 关键验证：分页计数已回传
        // 三阶段明细应各写入一条
        verify(executionStageLogDao, times(3)).insert(any());
    }

    @Test
    void testExecutePersistsStageLogsAndProgress() throws Exception {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(2L);
        task.setName("阶段日志任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);

        when(collectTaskDao.getById(2L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(new DatasourceConfigEntity());
        when(executionLogDao.insert(any())).thenReturn(1);

        Map<String, Object> record = new HashMap<>();
        record.put("name", "test");
        List<Map<String, Object>> rawData = Collections.singletonList(record);
        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(rawData, 1);
            return 1;
        });
        when(fieldMappingDao.getByTaskId(2L)).thenReturn(Collections.emptyList());
        when(fieldMapper.mapFields(anyList(), any())).thenReturn(rawData);
        when(dataTransformChain.execute(anyList(), any(), any())).thenReturn(rawData);

        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(any(), any(), any())).thenReturn(session);
        DataWriteResult writeResult = new DataWriteResult();
        writeResult.setSuccessCount(1);
        when(session.write(anyList(), any())).thenReturn(writeResult);

        engine.executeByTaskId(2L);

        org.mockito.ArgumentCaptor<ExecutionStageLogEntity> captor =
            org.mockito.ArgumentCaptor.forClass(ExecutionStageLogEntity.class);
        verify(executionStageLogDao, times(3)).insert(captor.capture());
        java.util.List<ExecutionStageLogEntity> stages = captor.getAllValues();
        assertEquals(Stage.READ, stages.get(0).getStage());
        assertEquals(Stage.TRANSFORM, stages.get(1).getStage());
        assertEquals(Stage.WRITE, stages.get(2).getStage());
    }

    @Test
    void testExecuteEmptyData() throws Exception {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("空数据任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);

        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(new DatasourceConfigEntity());
        when(executionLogDao.insert(any())).thenReturn(1);
        when(executionLogDao.updateById(any())).thenReturn(1);

        when(pageCollector.collectPages(any(), any())).thenReturn(0);

        CollectResult result = engine.executeByTaskId(1L);
        assertEquals(0, result.getTotalRecords());
        assertEquals(0, result.getPageCount());
    }

    @Test
    void testExecuteWithRetryRetriesAndAlarmsOnFailure() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setName("重试任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        task.setRetryTimes(2);
        task.setAlarmEnabled(1);
        when(collectTaskDao.getById(1L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(null);
        when(executionLogDao.insert(any())).thenReturn(1);

        CollectResult result = engine.executeByTaskId(1L);

        assertEquals(0, result.getTotalRecords());
        verify(executionLogDao, times(3)).insert(any()); // 1 attempt + 2 retries
        verify(unifyAlarmOpsService, times(1)).triggerAlarm(any());
    }

    @Test
    void testExecuteWithTimeoutAborts() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(2L);
        task.setName("超时任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        task.setTimeoutMs(50L);
        when(collectTaskDao.getById(2L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(new DatasourceConfigEntity());
        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            try {
                Thread.sleep(400);
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
            return 0;
        });

        CollectResult result = engine.executeByTaskId(2L);

        assertEquals(0, result.getTotalRecords());
    }

    @Test
    void testAlarmNotTriggeredWhenDisabled() {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(3L);
        task.setName("无告警任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        task.setAlarmEnabled(0);
        when(collectTaskDao.getById(3L)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(null);
        when(executionLogDao.insert(any())).thenReturn(1);

        engine.executeByTaskId(3L);

        verify(unifyAlarmOpsService, never()).triggerAlarm(any());
    }

    // ---------- 多页流式：聚合口径、逐页处理、边界语义 ----------

    private CollectTaskEntity basicTask(long id) {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(id);
        task.setName("流式任务");
        task.setApiConfigId(100L);
        task.setDatasourceId(200L);
        when(collectTaskDao.getById(id)).thenReturn(task);
        when(apiConfigDao.getById(100L)).thenReturn(new ApiConfigEntity());
        when(datasourceConfigDao.getById(200L)).thenReturn(new DatasourceConfigEntity());
        when(executionLogDao.insert(any())).thenReturn(1);
        when(fieldMappingDao.getByTaskId(id)).thenReturn(Collections.emptyList());
        when(fieldMapper.mapFields(anyList(), any())).thenAnswer(inv -> inv.getArgument(0));
        when(dataTransformChain.execute(anyList(), any(), any())).thenAnswer(inv -> inv.getArgument(0));
        return task;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> page(int size) {
        List<Map<String, Object>> list = new ArrayList<>();
        for (int i = 0; i < size; i++) {
            Map<String, Object> r = new HashMap<>();
            r.put("id", i);
            list.add(r);
        }
        return list;
    }

    /** S1/S7/S10：多页时统计跨页聚合，且转换/写库按页执行（会话仅 prepare 一次）。 */
    @Test
    void testExecuteMultiPageAggregatesStatsAndStreamsPerPage() throws Exception {
        CollectTaskEntity task = basicTask(5L);
        List<Map<String, Object>> p1 = page(2);
        List<Map<String, Object>> p2 = page(1);

        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(p1, 1);
            handler.onPage(p2, 2);
            return 2;
        });

        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(eq(task), any(), any())).thenReturn(session);
        when(session.write(anyList(), any())).thenAnswer(inv -> {
            List<Map<String, Object>> l = inv.getArgument(0);
            return new DataWriteResult(l.size(), 0);
        });

        CollectResult result = engine.executeByTaskId(5L);

        assertEquals(3, result.getTotalRecords());
        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        assertEquals(2, result.getPageCount());

        // 转换/写库均为逐页调用（单页可见性），而非整批
        org.mockito.ArgumentCaptor<List<Map<String, Object>>> captor =
            org.mockito.ArgumentCaptor.forClass(List.class);
        verify(dataTransformChain, times(2)).execute(captor.capture(), any(), any());
        assertEquals(2, captor.getAllValues().get(0).size());
        assertEquals(1, captor.getAllValues().get(1).size());
        verify(session, times(2)).write(anyList(), any());
        verify(session, times(1)).prepare();
        verify(fieldMappingDao, times(1)).getByTaskId(5L);
        verify(executionStageLogDao, times(3)).insert(any());
    }

    /** S5/C5：后续页转换失败 → 任务失败，但此前已写入的页保留。 */
    @Test
    void testExecuteTransformFailureOnLaterPageKeepsEarlierWrites() throws Exception {
        CollectTaskEntity task = basicTask(6L);
        List<Map<String, Object>> p1 = page(1);
        List<Map<String, Object>> p2 = page(1);

        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(p1, 1);
            handler.onPage(p2, 2);
            return 2;
        });
        when(dataTransformChain.execute(anyList(), any(), any()))
            .thenAnswer(inv -> inv.getArgument(0))
            .thenThrow(new RuntimeException("transform boom"));

        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(any(), any(), any())).thenReturn(session);
        when(session.write(anyList(), any())).thenReturn(new DataWriteResult(1, 0));

        CollectResult result = engine.executeByTaskId(6L);

        // 任务失败 → 返回默认结果
        assertEquals(0, result.getTotalRecords());
        // 首页已写入，第二页转换失败后不再写
        verify(session, times(1)).write(anyList(), any());
        verify(session, times(1)).close();
        verify(executionLogDao, atLeastOnce()).updateById(any());
    }

    /** S6/C6：写库批失败计入失败条数并继续，不影响任务成功。 */
    @Test
    void testExecuteWriteFailuresAccumulateAndContinue() throws Exception {
        CollectTaskEntity task = basicTask(7L);
        List<Map<String, Object>> p1 = page(1);
        List<Map<String, Object>> p2 = page(1);

        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(p1, 1);
            handler.onPage(p2, 2);
            return 2;
        });

        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(any(), any(), any())).thenReturn(session);
        when(session.write(anyList(), any()))
            .thenReturn(new DataWriteResult(1, 1))
            .thenReturn(new DataWriteResult(1, 0));

        CollectResult result = engine.executeByTaskId(7L);

        assertEquals(2, result.getTotalRecords());
        assertEquals(2, result.getSuccessCount());
        assertEquals(1, result.getFailCount());
        assertEquals(2, result.getPageCount());
    }

    /** S9/C12：非分页空页 → 不打开写库会话，仅记录 READ 阶段。 */
    @Test
    void testExecuteEmptySinglePageSkipsWriteSession() throws Exception {
        CollectTaskEntity task = basicTask(8L);
        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(Collections.emptyList(), 1);
            return 1;
        });

        CollectResult result = engine.executeByTaskId(8L);

        assertEquals(0, result.getTotalRecords());
        assertEquals(1, result.getPageCount());
        verify(dataPersister, never()).openSession(any(), any(), any());
        verify(executionStageLogDao, times(1)).insert(any());
    }

    /** C5：启用详情补全时，阶段日志按 READ → DETAIL → TRANSFORM → WRITE 出现补全阶段。 */
    @Test
    void testExecuteWithDetailEnrichmentLogsDetailStage() throws Exception {
        CollectTaskEntity task = basicTask(9L);
        ApiConfigEntity apiConfig = new ApiConfigEntity();
        DetailConfig dc = new DetailConfig();
        dc.setEnabled(1);
        apiConfig.setDetailConfig(dc);
        when(apiConfigDao.getById(100L)).thenReturn(apiConfig);

        // 模拟补全阶段：如实回填计数（真实实现由 DetailEnricher 负责）
        doAnswer(inv -> {
            List<Map<String, Object>> recs = inv.getArgument(0);
            StageMetrics stage = inv.getArgument(2);
            stage.setInputCount(recs.size());
            stage.setSuccessCount(recs.size());
            stage.setOutputCount(recs.size());
            return recs;
        }).when(detailEnricher).enrich(anyList(), any(), any());

        List<Map<String, Object>> p1 = page(2);
        when(pageCollector.collectPages(any(), any())).thenAnswer(inv -> {
            PageCollector.PageHandler handler = inv.getArgument(1);
            handler.onPage(p1, 1);
            return 1;
        });
        DataPersister.WriteSession session = Mockito.mock(DataPersister.WriteSession.class);
        when(dataPersister.openSession(any(), any(), any())).thenReturn(session);
        when(session.write(anyList(), any())).thenAnswer(inv ->
            new DataWriteResult(((List<?>) inv.getArgument(0)).size(), 0));

        engine.executeByTaskId(9L);

        org.mockito.ArgumentCaptor<ExecutionStageLogEntity> captor =
            org.mockito.ArgumentCaptor.forClass(ExecutionStageLogEntity.class);
        verify(executionStageLogDao, times(4)).insert(captor.capture());
        java.util.List<ExecutionStageLogEntity> stages = captor.getAllValues();
        assertEquals(Stage.READ, stages.get(0).getStage());
        assertEquals(Stage.DETAIL, stages.get(1).getStage());
        assertEquals(2, stages.get(1).getInputCount());
        assertEquals(Stage.TRANSFORM, stages.get(2).getStage());
        assertEquals(Stage.WRITE, stages.get(3).getStage());
    }
}
