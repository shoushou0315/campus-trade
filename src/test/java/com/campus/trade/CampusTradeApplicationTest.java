package com.campus.trade;

import com.campus.trade.common.result.Result;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class CampusTradeApplicationTest {

    @Test
    void testResultSuccess() {
        Result<String> result = Result.success("hello");
        assertEquals(200, result.getCode());
        assertEquals("success", result.getMessage());
        assertEquals("hello", result.getData());
    }

    @Test
    void testResultError() {
        Result<String> result = Result.error(400, "参数错误");
        assertEquals(400, result.getCode());
        assertEquals("参数错误", result.getMessage());
        assertNull(result.getData());
    }

    @Test
    void testResultSuccessNoData() {
        Result<Void> result = Result.success();
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }
}
