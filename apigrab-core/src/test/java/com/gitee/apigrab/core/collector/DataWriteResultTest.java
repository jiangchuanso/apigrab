package com.gitee.apigrab.core.collector;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class DataWriteResultTest {

    @Test
    void testDefaultConstructor() {
        DataWriteResult r = new DataWriteResult();
        assertEquals(0, r.getSuccessCount());
        assertEquals(0, r.getFailCount());
    }

    @Test
    void testFullConstructor() {
        DataWriteResult r = new DataWriteResult(50, 2);
        assertEquals(50, r.getSuccessCount());
        assertEquals(2, r.getFailCount());
    }

    @Test
    void testSetters() {
        DataWriteResult r = new DataWriteResult();
        r.setSuccessCount(100);
        r.setFailCount(0);
        assertEquals(100, r.getSuccessCount());
        assertEquals(0, r.getFailCount());
    }
}
