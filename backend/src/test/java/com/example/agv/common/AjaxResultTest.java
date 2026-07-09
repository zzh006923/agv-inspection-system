package com.example.agv.common;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AjaxResultTest {

    @Test
    void successShouldReturnCode200AndData() {
        Map<String, Object> data = Map.of("taskId", 1L, "status", "待巡视");

        AjaxResult result = AjaxResult.success(data);

        assertEquals(200, result.getCode());
        assertEquals("操作成功", result.getMsg());
        assertSame(data, result.getData());
    }

    @Test
    void errorShouldReturnCode500AndNullData() {
        AjaxResult result = AjaxResult.error("系统异常");

        assertEquals(500, result.getCode());
        assertEquals("系统异常", result.getMsg());
        assertNull(result.getData());
    }
}
