package com.gitee.apigrab.core.mapping.builtin;

import com.gitee.apigrab.core.mapping.ConvertFunction;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class BuiltinConvertFunctionsTest {

    @Test
    void testToString() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_string");
        assertNotNull(fn);
        assertEquals("123", fn.convert(123, null));
        assertEquals("hello", fn.convert("hello", null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testToInt() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_int");
        assertNotNull(fn);
        assertEquals(42, fn.convert(42, null));
        assertEquals(123, fn.convert("123", null));
        assertEquals(99, fn.convert(99.9, null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testToIntStringParse() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_int");
        assertEquals(-5, fn.convert("-5", null));
    }

    @Test
    void testToDecimal() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_decimal");
        assertNotNull(fn);
        assertEquals(new BigDecimal("3.14"), fn.convert("3.14", null));
        assertEquals(new BigDecimal("100"), fn.convert(100, null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testToIntNonNumericReturnsNullWithoutThrowing() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_int");
        assertNull(fn.convert("", null), "空串应取空值");
        assertNull(fn.convert("   ", null), "纯空白应取空值");
        assertNull(fn.convert("abc", null), "非数字字符串应取空值");
        assertNull(fn.convert("null", null), "字符串 null 应取空值");
        assertNull(fn.convert("12.5x", null), "残缺数字应取空值");
    }

    @Test
    void testToDecimalNonNumericReturnsNullWithoutThrowing() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("to_decimal");
        assertNull(fn.convert("", null), "空串应取空值");
        assertNull(fn.convert("abc", null), "非数字字符串应取空值");
        assertNull(fn.convert("null", null), "字符串 null 应取空值");
    }

    @Test
    void testToIntAndDecimalTolerateSurroundingWhitespace() {
        assertEquals(25, BuiltinConvertFunctions.getFunction("to_int").convert(" 25 ", null));
        assertEquals(new BigDecimal("3.14"),
            BuiltinConvertFunctions.getFunction("to_decimal").convert(" 3.14 ", null));
    }

    @Test
    void testDefaultValue() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("default_value");
        assertNotNull(fn);
        assertEquals("actual", fn.convert("actual", "default"));
        assertEquals("default", fn.convert(null, "default"));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testTrim() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("trim");
        assertNotNull(fn);
        assertEquals("hello", fn.convert("  hello  ", null));
        assertEquals("", fn.convert("", null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testUpper() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("upper");
        assertNotNull(fn);
        assertEquals("HELLO", fn.convert("Hello", null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testLower() {
        ConvertFunction fn = BuiltinConvertFunctions.getFunction("lower");
        assertNotNull(fn);
        assertEquals("hello", fn.convert("HELLO", null));
        assertNull(fn.convert(null, null));
    }

    @Test
    void testHasFunction() {
        assertTrue(BuiltinConvertFunctions.hasFunction("to_string"));
        assertTrue(BuiltinConvertFunctions.hasFunction("to_int"));
        assertTrue(BuiltinConvertFunctions.hasFunction("trim"));
        assertFalse(BuiltinConvertFunctions.hasFunction("non_existent"));
    }

    @Test
    void testGetNonExistentFunction() {
        assertNull(BuiltinConvertFunctions.getFunction("non_existent"));
    }
}
