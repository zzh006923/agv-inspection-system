package com.example.agv.common;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TableDataInfoTest {

    @Test
    void successShouldReturnRowsAndTotal() {
        List<String> rows = List.of("任务1", "任务2");

        TableDataInfo result = TableDataInfo.success(rows, 2L);

        assertEquals(200, result.getCode());
        assertEquals("查询成功", result.getMsg());
        assertSame(rows, result.getRows());
        assertEquals(2L, result.getTotal());
    }

    @Test
    void successShouldUseZeroWhenTotalIsNull() {
        TableDataInfo result = TableDataInfo.success(List.of(), null);

        assertEquals(0L, result.getTotal());
    }
}
