package com.example.agv.service;

import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvTaskServiceBranchCoverageTest {

    @Mock
    private AgvTaskMapper agvTaskMapper;

    @InjectMocks
    private AgvTaskService service;

    @Test
    void listTaskShouldHandleBlankFiltersAndNonBlankFilters() {
        when(agvTaskMapper.selectList(any())).thenReturn(List.of());

        assertTrue(service.listTask(null, " ", "", null).isEmpty());
        assertTrue(service.listTask("T001", "张三", "李四", "待巡视").isEmpty());

        verify(agvTaskMapper, times(2)).selectList(any());
    }

    @Test
    void getTaskShouldReturnMapperResult() {
        AgvTask task = new AgvTask();
        task.setId(7L);
        when(agvTaskMapper.selectById(7L)).thenReturn(task);

        assertSame(task, service.getTask(7L));
    }

    @Test
    void endTaskShouldReturnNullWhenTaskNotFound() {
        when(agvTaskMapper.selectById(404L)).thenReturn(null);

        assertNull(service.endTask(404L, true));
        verify(agvTaskMapper, never()).updateById(any());
    }

    @Test
    void uploadTaskShouldReturnNullWhenTaskNotFound() {
        when(agvTaskMapper.selectById(404L)).thenReturn(null);

        assertNull(service.uploadTask(404L));
        verify(agvTaskMapper, never()).updateById(any());
    }
}
