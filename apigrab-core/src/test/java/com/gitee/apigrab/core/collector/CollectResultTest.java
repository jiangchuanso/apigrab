package com.gitee.apigrab.core.collector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CollectResultTest {

    @Test
    void testDefaultConstructor() {
        CollectResult r = new CollectResult();
        assertEquals(0, r.getTotalRecords());
        assertEquals(0, r.getSuccessCount());
        assertEquals(0, r.getFailCount());
        assertEquals(0, r.getPageCount());
        assertEquals(0, r.getCostMs());
    }

    @Test
    void testFullConstructor() {
        CollectResult r = new CollectResult(100, 95, 5, 3);
        assertEquals(100, r.getTotalRecords());
        assertEquals(95, r.getSuccessCount());
        assertEquals(5, r.getFailCount());
        assertEquals(3, r.getPageCount());
    }

    @Test
    void testSetters() {
        CollectResult r = new CollectResult();
        r.setTotalRecords(200);
        r.setSuccessCount(180);
        r.setFailCount(20);
        r.setPageCount(10);
        r.setCostMs(1500L);

        assertEquals(200, r.getTotalRecords());
        assertEquals(180, r.getSuccessCount());
        assertEquals(20, r.getFailCount());
        assertEquals(10, r.getPageCount());
        assertEquals(1500L, r.getCostMs());
    }

    @Test
    void testAllZero() {
        CollectResult r = new CollectResult(0, 0, 0, 0);
        assertEquals(0, r.getTotalRecords());
        assertEquals(0, r.getSuccessCount());
    }
}
