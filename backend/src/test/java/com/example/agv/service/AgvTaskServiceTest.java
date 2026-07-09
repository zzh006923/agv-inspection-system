package com.example.agv.service;

import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvTaskServiceTest {

    @Mock
    private AgvTaskMapper agvTaskMapper;

    @InjectMocks
    private AgvTaskService agvTaskService;

    @Test
    void addTaskShouldSetDefaultStatusAndInsert() {
        AgvTask task = new AgvTask();
        task.setTaskCode("T001");
        task.setTaskName("一号巡检任务");

        AgvTask result = agvTaskService.addTask(task);

        assertSame(task, result);
        assertEquals("待巡视", result.getTaskStatus());
        assertEquals(0, result.getUploaded());
        assertEquals(0, result.getDeleteFlag());
        assertNotNull(result.getCreateTime());
        verify(agvTaskMapper).insert(task);
    }

    @Test
    void startTaskShouldChangeWaitingTaskToRunning() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待巡视");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.startTask(1L);

        assertSame(task, result);
        assertEquals("巡视中", result.getTaskStatus());
        assertNotNull(result.getExecTime());
        verify(agvTaskMapper).updateById(task);
    }

    @Test
    void startTaskShouldReturnNullWhenTaskNotFound() {
        when(agvTaskMapper.selectById(99L)).thenReturn(null);

        AgvTask result = agvTaskService.startTask(99L);

        assertNull(result);
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void startTaskShouldReturnNullWhenStatusIsNotWaiting() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.startTask(1L);

        assertNull(result);
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void endTaskShouldChangeTaskToWaitingUpload() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.endTask(1L, false);

        assertSame(task, result);
        assertEquals("待上传", result.getTaskStatus());
        assertNotNull(result.getEndTime());
        assertNull(result.getRemark());
        verify(agvTaskMapper).updateById(task);
    }

    @Test
    void endTaskShouldSetAbortRemarkWhenTaskIsAborted() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.endTask(1L, true);

        assertEquals("待上传", result.getTaskStatus());
        assertEquals("任务被终止", result.getRemark());
        verify(agvTaskMapper).updateById(task);
    }

    @Test
    void uploadTaskShouldCompleteWaitingUploadTask() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("待上传");
        task.setUploaded(0);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.uploadTask(1L);

        assertSame(task, result);
        assertEquals("已完成", result.getTaskStatus());
        assertEquals(1, result.getUploaded());
        verify(agvTaskMapper).updateById(task);
    }

    @Test
    void uploadTaskShouldReturnNullWhenTaskIsNotWaitingUpload() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskStatus("巡视中");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);

        AgvTask result = agvTaskService.uploadTask(1L);

        assertNull(result);
        verify(agvTaskMapper, never()).updateById(any());
    }
}
