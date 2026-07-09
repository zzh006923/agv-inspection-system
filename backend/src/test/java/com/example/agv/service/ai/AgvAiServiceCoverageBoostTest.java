package com.example.agv.service.ai;

import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.entity.AgvIotActionRecord;
import com.example.agv.entity.AgvSensorRecord;
import com.example.agv.entity.AgvTask;
import com.example.agv.mapper.AgvFlawMapper;
import com.example.agv.mapper.AgvIotActionRecordMapper;
import com.example.agv.mapper.AgvSensorRecordMapper;
import com.example.agv.mapper.AgvTaskMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvAiServiceCoverageBoostTest {

    @Mock private DifyService difyService;
    @Mock private AgvTaskMapper agvTaskMapper;
    @Mock private AgvFlawMapper agvFlawMapper;
    @Mock private AgvSensorRecordMapper agvSensorRecordMapper;
    @Mock private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @InjectMocks
    private AgvAiService service;

    @Test
    void chatShouldUseProvidedQuestionTaskFlawAndContext() {
        AiChatRequest request = new AiChatRequest();
        request.setTaskId(1L);
        request.setFlawId(2L);
        request.setQuestion("帮我生成备注");
        request.setContext("前端补充信息");
        request.setConversationId("conv");

        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskName("隧道巡检");
        task.setUploaded(1);
        AgvFlaw selected = new AgvFlaw();
        selected.setId(2L);
        selected.setTaskId(1L);
        selected.setFlawName("裂缝");
        selected.setConfirmed(0);
        selected.setUploaded(0);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        when(agvFlawMapper.selectById(2L)).thenReturn(selected);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of());
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        AiChatResponse expected = new AiChatResponse("备注", "conv", "msg");
        when(difyService.chat(anyString(), eq("conv"))).thenReturn(expected);

        AiChatResponse response = service.chat(request);

        assertSame(expected, response);
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(prompt.capture(), eq("conv"));
        assertTrue(prompt.getValue().contains("帮我生成备注"));
        assertTrue(prompt.getValue().contains("隧道巡检"));
        assertTrue(prompt.getValue().contains("裂缝"));
        assertTrue(prompt.getValue().contains("前端补充信息"));
    }

    @Test
    void taskReviewShouldHandleNullRequestAndEmptyLists() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskName("空任务");
        task.setUploaded(0);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of());
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        when(difyService.chat(anyString(), isNull())).thenReturn(new AiChatResponse("复盘", "", "msg"));

        AiChatResponse response = service.taskReview(1L, null);

        assertEquals("复盘", response.getAnswer());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(prompt.capture(), isNull());
        assertTrue(prompt.getValue().contains("暂无故障记录"));
        assertTrue(prompt.getValue().contains("暂无传感器记录"));
        assertTrue(prompt.getValue().contains("暂无联动记录"));
    }

    @Test
    void flawReviewShouldUseRequestTaskIdWhenProvidedEvenIfFlawHasOtherTaskId() {
        AgvFlaw flaw = new AgvFlaw();
        flaw.setId(9L);
        flaw.setTaskId(99L);
        flaw.setFlawName("光照不足");
        when(agvFlawMapper.selectById(9L)).thenReturn(flaw, flaw);
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskName("请求任务");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(new AgvSensorRecord()));
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of(new AgvIotActionRecord()));
        when(difyService.chat(anyString(), eq("conv"))).thenReturn(new AiChatResponse("研判", "conv", "msg"));

        AiChatRequest request = new AiChatRequest();
        request.setTaskId(1L);
        request.setConversationId("conv");
        AiChatResponse response = service.flawReview(9L, request);

        assertEquals("研判", response.getAnswer());
        ArgumentCaptor<String> prompt = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(prompt.capture(), eq("conv"));
        assertTrue(prompt.getValue().contains("请求任务"));
        assertTrue(prompt.getValue().contains("光照不足"));
    }
}
