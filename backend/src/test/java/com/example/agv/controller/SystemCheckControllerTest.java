package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class SystemCheckControllerTest {

    private SystemCheckController controller;

    @BeforeEach
    void setUp() {
        controller = new SystemCheckController();
        ReflectionTestUtils.setField(controller, "mockMode", true);
    }

    @Test
    void getAndSetMockModeShouldWork() {
        AjaxResult initial = controller.getMockMode();
        assertEquals(200, initial.getCode());
        assertEquals(true, initial.getData());

        AjaxResult changed = controller.setMockMode(false);
        assertEquals(200, changed.getCode());
        assertEquals(false, changed.getData());
    }

    @Test
    void checkItemsShouldPassInMockMode() {
        assertEquals(200, controller.checkFs().getCode());
        assertEquals(200, controller.checkDb().getCode());
        assertEquals(200, controller.checkAgv().getCode());
        assertEquals(200, controller.checkCam().getCode());
    }

    @Test
    void checkAllShouldAggregateMockModeResults() {
        AjaxResult result = controller.checkAll();

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(true, data.get("passed"));
        assertEquals(true, data.get("mockMode"));
        assertEquals("系统自检全部通过", data.get("message"));
    }
}
