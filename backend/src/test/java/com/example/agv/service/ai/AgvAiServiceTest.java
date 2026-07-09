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
class AgvAiServiceTest {

    @Mock
    private DifyService difyService;

    @Mock
    private AgvTaskMapper agvTaskMapper;

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @Mock
    private AgvSensorRecordMapper agvSensorRecordMapper;

    @Mock
    private AgvIotActionRecordMapper agvIotActionRecordMapper;

    @InjectMocks
    private AgvAiService agvAiService;

    @Test
    void chatShouldUseDefaultQuestionWhenRequestIsNull() {
        AiChatResponse expected = new AiChatResponse("处理建议", "", "msg-001");
        when(difyService.chat(anyString(), isNull())).thenReturn(expected);

        AiChatResponse result = agvAiService.chat(null);

        assertSame(expected, result);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(queryCaptor.capture(), isNull());
        assertTrue(queryCaptor.getValue().contains("请根据当前任务状态给出处理建议"));
        assertTrue(queryCaptor.getValue().contains("未指定或未查询到任务"));
    }

    @Test
    void taskReviewShouldBuildPromptWithTaskFlawSensorAndIotContext() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskCode("TASK-001");
        task.setTaskName("配电房巡检");
        task.setExecutor("张三");
        task.setTaskStatus("待上传");
        task.setUploaded(0);

        AgvFlaw flaw = new AgvFlaw();
        flaw.setId(10L);
        flaw.setTaskId(1L);
        flaw.setFlawName("轨道裂缝");
        flaw.setFlawType("裂缝");
        flaw.setConfirmed(0);
        flaw.setUploaded(0);

        AgvSensorRecord sensor = new AgvSensorRecord();
        sensor.setTaskId(1L);
        sensor.setSensorName("烟雾传感器");
        sensor.setSensorType("smoke");
        sensor.setSensorValue("90");
        sensor.setStatus("异常");
        sensor.setAction("声光报警");

        AgvIotActionRecord actionRecord = new AgvIotActionRecord();
        actionRecord.setTaskId(1L);
        actionRecord.setTriggerType("sensor");
        actionRecord.setDeviceName("声光报警器");
        actionRecord.setAction("开启");
        actionRecord.setResult("成功");

        AiChatRequest request = new AiChatRequest();
        request.setConversationId("conv-001");
        request.setContext("前端显示当前任务存在异常");

        AiChatResponse expected = new AiChatResponse("建议先复核再上传", "conv-001", "msg-001");
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of(sensor));
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of(actionRecord));
        when(difyService.chat(anyString(), eq("conv-001"))).thenReturn(expected);

        AiChatResponse result = agvAiService.taskReview(1L, request);

        assertSame(expected, result);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(queryCaptor.capture(), eq("conv-001"));
        String prompt = queryCaptor.getValue();
        assertTrue(prompt.contains("配电房巡检"));
        assertTrue(prompt.contains("轨道裂缝"));
        assertTrue(prompt.contains("待确认故障数：1"));
        assertTrue(prompt.contains("传感器异常记录数：1"));
        assertTrue(prompt.contains("声光报警器"));
        assertTrue(prompt.contains("前端显示当前任务存在异常"));
    }

    @Test
    void flawReviewShouldUseTaskIdFromFlawWhenRequestTaskIdIsNull() {
        AgvTask task = new AgvTask();
        task.setId(1L);
        task.setTaskName("隧道巡检");
        task.setUploaded(1);

        AgvFlaw flaw = new AgvFlaw();
        flaw.setId(10L);
        flaw.setTaskId(1L);
        flaw.setFlawName("疑似异物");
        flaw.setConfirmed(1);
        flaw.setUploaded(0);

        AiChatRequest request = new AiChatRequest();
        request.setConversationId("conv-002");

        AiChatResponse expected = new AiChatResponse("建议备注", "conv-002", "msg-002");
        when(agvFlawMapper.selectById(10L)).thenReturn(flaw);
        when(agvTaskMapper.selectById(1L)).thenReturn(task);
        when(agvFlawMapper.selectList(any())).thenReturn(List.of(flaw));
        when(agvSensorRecordMapper.selectList(any())).thenReturn(List.of());
        when(agvIotActionRecordMapper.selectList(any())).thenReturn(List.of());
        when(difyService.chat(anyString(), eq("conv-002"))).thenReturn(expected);

        AiChatResponse result = agvAiService.flawReview(10L, request);

        assertSame(expected, result);
        ArgumentCaptor<String> queryCaptor = ArgumentCaptor.forClass(String.class);
        verify(difyService).chat(queryCaptor.capture(), eq("conv-002"));
        String prompt = queryCaptor.getValue();
        assertTrue(prompt.contains("隧道巡检"));
        assertTrue(prompt.contains("疑似异物"));
        assertTrue(prompt.contains("故障判断"));
    }
}
