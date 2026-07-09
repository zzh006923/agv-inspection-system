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

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvFlawControllerCoverageBoostTest {

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @InjectMocks
    private AgvFlawController controller;

    @Test
    void listFlawShouldReturnEmptyWhenPageOutOfRangeAndApplyAllFilters() {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setId(1L);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));

        TableDataInfo result = controller.listFlaw(
                1L, " 裂缝 ", " 横向 ", "高", "AI", 1, 0, 3, 10
        );

        assertEquals(200, result.getCode());
        assertEquals(1L, result.getTotal());
        assertTrue(((List<?>) result.getRows()).isEmpty());
    }

    @Test
    void getFlawShouldReturnSuccessOrError() {
        when(agvFlawMapper.selectById(1L)).thenReturn(null);
        assertEquals("故障/缺陷不存在", controller.getFlaw(1L).getMsg());

        AgvFlaw deleted = new AgvFlaw();
        deleted.setDeleteFlag(1);
        when(agvFlawMapper.selectById(2L)).thenReturn(deleted);
        assertEquals("故障/缺陷不存在", controller.getFlaw(2L).getMsg());

        AgvFlaw existing = new AgvFlaw();
        existing.setId(3L);
        existing.setDeleteFlag(0);
        when(agvFlawMapper.selectById(3L)).thenReturn(existing);
        AjaxResult result = controller.getFlaw(3L);
        assertEquals(200, result.getCode());
        assertSame(existing, result.getData());
    }

    @Test
    void addFlawShouldPreserveProvidedValues() {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(1L);
        flaw.setRound(2);
        flaw.setShown(1);
        flaw.setConfirmed(1);
        flaw.setUploaded(1);
        flaw.setCountNum(5);
        flaw.setDeleteFlag(0);
        LocalDateTime now = LocalDateTime.now().minusDays(1);
        flaw.setCreateTime(now);

        AjaxResult result = controller.addFlaw(flaw);

        assertEquals(200, result.getCode());
        assertEquals(2, flaw.getRound());
        assertEquals(1, flaw.getShown());
        assertEquals(1, flaw.getConfirmed());
        assertEquals(1, flaw.getUploaded());
        assertEquals(5, flaw.getCountNum());
        assertSame(now, flaw.getCreateTime());
        verify(agvFlawMapper).insert(flaw);
    }

    @Test
    void updateFlawShouldRejectMissingExistingAndUpdateOnSuccess() {
        AgvFlaw missing = new AgvFlaw();
        missing.setId(1L);
        when(agvFlawMapper.selectById(1L)).thenReturn(null);
        assertEquals("故障/缺陷不存在", controller.updateFlaw(missing).getMsg());

        AgvFlaw deleted = new AgvFlaw();
        deleted.setId(2L);
        deleted.setDeleteFlag(1);
        when(agvFlawMapper.selectById(2L)).thenReturn(deleted);
        AgvFlaw updateDeleted = new AgvFlaw();
        updateDeleted.setId(2L);
        assertEquals("故障/缺陷不存在", controller.updateFlaw(updateDeleted).getMsg());

        AgvFlaw existing = new AgvFlaw();
        existing.setId(3L);
        existing.setDeleteFlag(0);
        AgvFlaw updated = new AgvFlaw();
        updated.setId(3L);
        updated.setRemark("人工确认");
        when(agvFlawMapper.selectById(3L)).thenReturn(existing, updated);
        AjaxResult result = controller.updateFlaw(updated);
        assertEquals(200, result.getCode());
        assertSame(updated, result.getData());
        verify(agvFlawMapper).updateById(updated);
    }

    @Test
    void deleteFlawShouldReturnErrorWhenNoRowsAndSuccessWhenDeleted() {
        when(agvFlawMapper.deleteById(1L)).thenReturn(0);
        assertEquals("故障/缺陷不存在", controller.delFlaw(1L).getMsg());

        when(agvFlawMapper.deleteById(2L)).thenReturn(1);
        AjaxResult result = controller.delFlaw(2L);
        assertEquals(200, result.getCode());
        assertNull(result.getData());
    }

    @Test
    void liveInfoShouldReturnEmptyListWhenNoNewFlaw() {
        when(agvFlawMapper.selectList(any())).thenReturn(List.of());

        AjaxResult result = controller.liveInfo(1L);

        assertEquals(200, result.getCode());
        assertTrue(((List<?>) result.getData()).isEmpty());
        verify(agvFlawMapper, never()).updateById(any());
    }

    @Test
    void checkAllConfirmedShouldReturnFalseWhenUnconfirmedExists() {
        when(agvFlawMapper.selectCount(any())).thenReturn(4L, 2L);

        AjaxResult result = controller.checkAllConfirmed(1L);

        Map<?, ?> data = (Map<?, ?>) result.getData();
        assertEquals(4L, data.get("total"));
        assertEquals(2L, data.get("unconfirmed"));
        assertEquals(false, data.get("allConfirmed"));
    }
}
