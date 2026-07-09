package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.common.TableDataInfo;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.mapper.AgvFlawMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvFlawControllerTest {

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @InjectMocks
    private AgvFlawController agvFlawController;

    @Test
    void listFlawShouldPaginateRows() {
        AgvFlaw flaw1 = new AgvFlaw();
        flaw1.setId(1L);
        AgvFlaw flaw2 = new AgvFlaw();
        flaw2.setId(2L);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw1, flaw2));

        TableDataInfo result = agvFlawController.listFlaw(1L, null, null, null, null, null, null, 2, 1);

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getTotal());
        List<?> rows = (List<?>) result.getRows();
        assertEquals(1, rows.size());
        assertSame(flaw2, rows.get(0));
    }

    @Test
    void addFlawShouldRejectMissingTaskId() {
        AgvFlaw flaw = new AgvFlaw();

        AjaxResult result = agvFlawController.addFlaw(flaw);

        assertEquals(500, result.getCode());
        assertEquals("所属任务ID不能为空", result.getMsg());
        verify(agvFlawMapper, never()).insert(any());
    }

    @Test
    void addFlawShouldSetDefaultValuesAndInsert() {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(1L);
        flaw.setFlawName("裂缝");

        AjaxResult result = agvFlawController.addFlaw(flaw);

        assertEquals(200, result.getCode());
        assertEquals(1, flaw.getRound());
        assertEquals(0, flaw.getShown());
        assertEquals(0, flaw.getConfirmed());
        assertEquals(0, flaw.getUploaded());
        assertEquals(1, flaw.getCountNum());
        assertEquals(0, flaw.getDeleteFlag());
        assertNotNull(flaw.getCreateTime());
        verify(agvFlawMapper).insert(flaw);
    }

    @Test
    void updateFlawShouldRejectMissingId() {
        AjaxResult result = agvFlawController.updateFlaw(new AgvFlaw());

        assertEquals(500, result.getCode());
        assertEquals("故障ID不能为空", result.getMsg());
        verify(agvFlawMapper, never()).updateById(any());
    }

    @Test
    void liveInfoShouldReturnUnshownFlawsAndMarkShown() {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setId(10L);
        flaw.setShown(0);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));

        AjaxResult result = agvFlawController.liveInfo(1L);

        assertEquals(200, result.getCode());
        assertSame(flaw, ((List<?>) result.getData()).get(0));
        assertEquals(1, flaw.getShown());
        verify(agvFlawMapper).updateById(flaw);
    }

    @Test
    void checkAllConfirmedShouldReturnSummary() {
        when(agvFlawMapper.selectCount(any())).thenReturn(3L, 0L);

        AjaxResult result = agvFlawController.checkAllConfirmed(1L);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(1L, data.get("taskId"));
        assertEquals(3L, data.get("total"));
        assertEquals(0L, data.get("unconfirmed"));
        assertEquals(true, data.get("allConfirmed"));
    }
}
