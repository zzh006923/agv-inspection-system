package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.common.TableDataInfo;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import com.example.agv.mapper.AgvUploadRecordMapper;
import com.example.agv.service.CloudUploadService;
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
class AgvTaskControllerTest {

    @Mock
    private AgvTaskMapper agvTaskMapper;

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @Mock
    private AgvUploadRecordMapper agvUploadRecordMapper;

    @Mock
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Mock
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @Mock
    private CloudUploadService cloudUploadService;

    @InjectMocks
    private AgvTaskController agvTaskController;

    @Test
    void listTaskShouldPaginateRows() {
        AgvTask task1 = new AgvTask();
        task1.setId(1L);
        AgvTask task2 = new AgvTask();
        task2.setId(2L);
        when(agvTaskMapper.selectList(any())).thenReturn(List.of(task1, task2));

        TableDataInfo result = agvTaskController.listTask(null, null, null, null, 1, 1);

        assertEquals(200, result.getCode());
        assertEquals(2L, result.getTotal());
        List<?> rows = (List<?>) result.getRows();
        assertEquals(1, rows.size());
        assertSame(task1, rows.get(0));
    }

    @Test
    void getTaskShouldReturnErrorWhenTaskDeleted() {
        AgvTask deletedTask = new AgvTask();
        deletedTask.setId(1L);
        deletedTask.setDeleteFlag(1);
        when(agvTaskMapper.selectById(1L)).thenReturn(deletedTask);

        AjaxResult result = agvTaskController.getTask(1L);

        assertEquals(500, result.getCode());
        assertEquals("任务不存在", result.getMsg());
    }

    @Test
    void addTaskShouldGenerateDefaults() {
        AgvTask task = new AgvTask();
        task.setTaskName("一号巡检任务");
        when(agvTaskMapper.selectOne(any())).thenReturn(null);

        AjaxResult result = agvTaskController.addTask(task);

        assertEquals(200, result.getCode());
        assertSame(task, result.getData());
        assertTrue(task.getTaskCode().startsWith("TASK"));
        assertEquals("待巡视", task.getTaskStatus());
        assertEquals(0, task.getUploaded());
        assertEquals(0, task.getDeleteFlag());
        assertEquals(1, task.getRound());
        assertNotNull(task.getCreateTime());
        verify(agvTaskMapper).insert(task);
    }

    @Test
    void startTaskShouldRejectNonWaitingTask() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AjaxResult result = agvTaskController.startTask(1L);

        assertEquals(500, result.getCode());
        assertEquals("只有待巡视任务可以启动", result.getMsg());
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void endTaskShouldRejectTaskNotRunning() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AjaxResult result = agvTaskController.endTask(1L, false);

        assertEquals(500, result.getCode());
        assertEquals("只有巡视中的任务可以结束", result.getMsg());
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void preUploadTaskShouldReturnSummaryAndCanUploadFlag() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvFlaw confirmedFlaw = new AgvFlaw();
        confirmedFlaw.setConfirmed(1);
        confirmedFlaw.setUploaded(0);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(confirmedFlaw));
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of());

        AjaxResult result = agvTaskController.preUploadTask(1L);

        assertEquals(200, result.getCode());
        Map<?, ?> data = (Map<?, ?>) result.getData();
        Map<?, ?> summary = (Map<?, ?>) data.get("summary");
        assertEquals(1L, summary.get("taskId"));
        assertEquals(1, summary.get("flawCount"));
        assertEquals(0L, summary.get("unconfirmedCount"));
        assertEquals(1L, summary.get("notUploadedFlawCount"));
        assertEquals(true, summary.get("canUpload"));
    }

    @Test
    void uploadTaskShouldRejectWhenThereAreUnconfirmedFlaws() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvFlaw unconfirmed = new AgvFlaw();
        unconfirmed.setConfirmed(0);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(unconfirmed));

        AjaxResult result = agvTaskController.uploadTask(1L);

        assertEquals(500, result.getCode());
        assertEquals("存在未确认故障/异常，请先确认或标记误报后再上传", result.getMsg());
        verify(cloudUploadService, never()).uploadTaskData(any(), any(), any(), any(), any());
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void uploadTaskShouldCompleteTaskAndInsertUploadRecordsWhenNoExistingRecord() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskCode("TASK-001");
        task.setTaskStatus("待上传");
        task.setUploaded(0);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvFlaw flaw = new AgvFlaw();
        flaw.setTaskId(1L);
        flaw.setFlawName("裂缝异常");
        flaw.setConfirmed(1);
        flaw.setUploaded(0);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));
        when(agvUploadRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(new AgvSensorRecord()));
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of(new AgvIotActionRecord()));
        when(cloudUploadService.uploadTaskData(any(), any(), any(), any(), any()))
                .thenReturn(CloudUploadService.UploadResult.localOnly("本地闭环完成"));

        AjaxResult result = agvTaskController.uploadTask(1L);

        assertEquals(200, result.getCode());
        assertEquals("已完成", task.getTaskStatus());
        assertEquals(1, task.getUploaded());
        assertEquals(1, flaw.getUploaded());
        verify(agvTaskMapper).updateById(task);
        verify(agvFlawMapper).updateById(flaw);
        verify(agvUploadRecordMapper, times(2)).insert(any());
    }

    @Test
    void updateTaskShouldRejectWhenTaskIsNotWaiting() {
        AgvTask update = new AgvTask();
        update.setId(1L);
        AgvTask existing = new AgvTask();
        existing.setId(1L);
        existing.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(1L)).thenReturn(existing);

        AjaxResult result = agvTaskController.updateTask(update);

        assertEquals(500, result.getCode());
        assertEquals("只有待巡视任务可以修改", result.getMsg());
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void deleteTaskShouldRejectTaskNotWaiting() {
        AgvTask existing = new AgvTask();
        existing.setId(1L);
        existing.setTaskStatus("待上传");
        when(agvTaskMapper.selectById(1L)).thenReturn(existing);

        AjaxResult result = agvTaskController.delTask(1L);

        assertEquals(500, result.getCode());
        assertEquals("只有待巡视任务可以删除", result.getMsg());
        verify(agvTaskMapper, never()).deleteById(1L);
    }
}
