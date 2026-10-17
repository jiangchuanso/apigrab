package com.gitee.apigrab.common.model;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.*;

class PageResultTest {

    @Test
    void testDefaultConstructor() {
        PageResult<String> pr = new PageResult<>();
        assertEquals(0, pr.getPageNum());
        assertEquals(0, pr.getPageSize());
        assertEquals(0, pr.getTotal());
        assertEquals(0, pr.getPages());
        assertNull(pr.getList());
    }

    @Test
    void testFullConstructor() {
        PageResult<String> pr = new PageResult<>(1, 20, 55, Arrays.asList("a", "b"));
        assertEquals(1, pr.getPageNum());
        assertEquals(20, pr.getPageSize());
        assertEquals(55, pr.getTotal());
        assertEquals(3, pr.getPages()); // 55/20 = 2.75 -> 3
        assertEquals(2, pr.getList().size());
    }

    @Test
    void testPagesCalculationExactDivisible() {
        PageResult<String> pr = new PageResult<>(1, 10, 30, Arrays.asList("a", "b", "c"));
        assertEquals(3, pr.getPages());
    }

    @Test
    void testPagesCalculationSinglePage() {
        PageResult<String> pr = new PageResult<>(1, 50, 10, Collections.singletonList("a"));
        assertEquals(1, pr.getPages());
    }

    @Test
    void testPagesCalculationZeroTotal() {
        PageResult<String> pr = new PageResult<>(1, 20, 0, Collections.emptyList());
        assertEquals(0, pr.getPages());
    }

    @Test
    void testSetters() {
        PageResult<String> pr = new PageResult<>();
        pr.setPageNum(2);
        pr.setPageSize(10);
        pr.setTotal(100L);
        pr.setPages(10);
        pr.setList(Arrays.asList("x", "y"));
        assertEquals(2, pr.getPageNum());
        assertEquals(10, pr.getPageSize());
        assertEquals(100, pr.getTotal());
        assertEquals(10, pr.getPages());
        assertEquals(2, pr.getList().size());
    }
}
