package com.gitee.apigrab.core.collector;

import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class FailureSampleCollectorTest {

    @Test
    void testRespectsLimit() {
        FailureSampleCollector collector = new FailureSampleCollector(2);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("id", 1);

        collector.add("e1", record);
        collector.add("e2", record);
        collector.add("e3", record);

        assertEquals(2, collector.getSamples().size());
    }

    @Test
    void testTruncatesLongSourceRecord() {
        FailureSampleCollector collector = new FailureSampleCollector(10);
        StringBuilder big = new StringBuilder();
        for (int i = 0; i < 2000; i++) {
            big.append('x');
        }
        collector.add("boom", big.toString());

        String sourceRecord = (String) collector.getSamples().get(0).get("sourceRecord");
        assertNotNull(sourceRecord);
        assertTrue(sourceRecord.length() <= 500);
    }

    @Test
    void testEmptyReturnsNullJson() {
        FailureSampleCollector collector = new FailureSampleCollector(10);
        assertTrue(collector.isEmpty());
        assertNull(collector.toJson());
    }

    @Test
    void testSerializesMapRecord() {
        FailureSampleCollector collector = new FailureSampleCollector(10);
        Map<String, Object> record = new LinkedHashMap<>();
        record.put("name", "Alice");
        collector.add("constraint violation", record);

        String json = collector.toJson();
        assertNotNull(json);
        assertTrue(json.contains("constraint violation"));
        assertTrue(json.contains("Alice"));
    }
}
