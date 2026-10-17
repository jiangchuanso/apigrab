package com.gitee.apigrab.core.collector;

import com.gitee.apigrab.core.datasource.DynamicDataSourceManager;
import com.gitee.apigrab.persistence.entity.CollectTaskEntity;
import com.gitee.apigrab.persistence.entity.DatasourceConfigEntity;
import com.gitee.apigrab.persistence.entity.FieldMappingEntity;
import com.gitee.apigrab.persistence.entity.enums.DbType;
import com.gitee.apigrab.persistence.entity.enums.WriteMode;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DataPersisterTest {

    private DataPersister persister;
    private DynamicDataSourceManager dataSourceManager;
    private Connection conn;
    private PreparedStatement ps;
    private Statement stmt;
    private DatasourceConfigEntity dsConfig;
    private List<FieldMappingEntity> mappings;

    @BeforeEach
    void setUp() throws Exception {
        persister = new DataPersister();
        dataSourceManager = Mockito.mock(DynamicDataSourceManager.class);
        conn = Mockito.mock(Connection.class);
        ps = Mockito.mock(PreparedStatement.class);
        stmt = Mockito.mock(Statement.class);

        java.lang.reflect.Field f = DataPersister.class.getDeclaredField("dataSourceManager");
        f.setAccessible(true);
        f.set(persister, dataSourceManager);

        dsConfig = new DatasourceConfigEntity();
        dsConfig.setDbType(DbType.MYSQL);

        FieldMappingEntity mapping = new FieldMappingEntity();
        mapping.setTargetField("c1");
        mappings = Collections.singletonList(mapping);

        when(dataSourceManager.getConnection(10L)).thenReturn(conn);
        when(conn.prepareStatement(anyString())).thenReturn(ps);
        when(conn.createStatement()).thenReturn(stmt);
    }

    private CollectTaskEntity task(WriteMode mode, int batchSize, int autoCreate) {
        CollectTaskEntity task = new CollectTaskEntity();
        task.setId(1L);
        task.setDatasourceId(10L);
        task.setTargetTable("t_target");
        task.setWriteMode(mode);
        task.setBatchSize(batchSize);
        task.setAutoCreateTable(autoCreate);
        return task;
    }

    private List<Map<String, Object>> records(int count) {
        Map<String, Object>[] arr = new Map[count];
        for (int i = 0; i < count; i++) {
            Map<String, Object> r = new LinkedHashMap<>();
            r.put("c1", "v" + i);
            arr[i] = r;
        }
        return Arrays.asList(arr);
    }

    @Test
    void testPersistBatchesAndCounts() throws Exception {
        CollectTaskEntity task = task(WriteMode.INSERT, 2, 0);
        when(ps.executeUpdate()).thenReturn(2, 1);

        DataWriteResult result = persister.persist(records(3), task, dsConfig, mappings, null);

        assertEquals(3, result.getSuccessCount());
        assertEquals(0, result.getFailCount());
        verify(conn, times(2)).prepareStatement(anyString());
        verify(conn).close();
    }

    @Test
    void testPrepareTruncateRunsOnceAcrossMultipleWrites() throws Exception {
        CollectTaskEntity task = task(WriteMode.TRUNCATE_INSERT, 500, 0);
        when(ps.executeUpdate()).thenReturn(1);

        DataPersister.WriteSession session = persister.openSession(task, dsConfig, mappings);
        session.prepare();
        session.write(records(1), null);
        session.write(records(1), null);
        session.close();

        verify(conn, times(1)).createStatement();
        verify(stmt, times(1)).execute(anyString());
    }

    @Test
    void testEmptyWriteDoesNotPrepare() throws Exception {
        CollectTaskEntity task = task(WriteMode.TRUNCATE_INSERT, 500, 0);

        DataPersister.WriteSession session = persister.openSession(task, dsConfig, mappings);
        DataWriteResult result = session.write(Collections.emptyList(), null);
        session.close();

        assertEquals(0, result.getSuccessCount());
        verify(conn, never()).createStatement();
        verify(conn, never()).prepareStatement(anyString());
    }

    @Test
    void testBatchFailureCountsAndCollectsSample() throws Exception {
        CollectTaskEntity task = task(WriteMode.INSERT, 10, 0);
        when(ps.executeUpdate()).thenThrow(new SQLException("duplicate key"));

        FailureSampleCollector failureCollector = new FailureSampleCollector(10);
        DataPersister.WriteSession session = persister.openSession(task, dsConfig, mappings);
        DataWriteResult result = session.write(records(2), failureCollector);
        session.close();

        assertEquals(0, result.getSuccessCount());
        assertEquals(2, result.getFailCount());
        assertFalse(failureCollector.isEmpty());
    }
}
