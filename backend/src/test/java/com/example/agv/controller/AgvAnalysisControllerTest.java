package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvFlaw;
import com.example.agv.mapper.AgvFlawMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvAnalysisControllerTest {

    @Mock
    private AgvFlawMapper agvFlawMapper;

    @InjectMocks
    private AgvAnalysisController agvAnalysisController;

    @Test
    void receiveAnalysisResultShouldRejectMissingTaskId() {
        AgvAnalysisController.AnalysisResultRequest request = new AgvAnalysisController.AnalysisResultRequest();

        AjaxResult result = agvAnalysisController.receiveAnalysisResult(request);

        assertEquals(500, result.getCode());
        assertEquals("任务ID不能为空", result.getMsg());
        verify(agvFlawMapper, never()).insert(any());
    }

    @Test
    void receiveAnalysisResultShouldConvertAlgorithmResultToFlaw() {
        AgvAnalysisController.AnalysisResultRequest request = new AgvAnalysisController.AnalysisResultRequest();
        request.setTaskId(1L);
        request.setDistance(12.5);
        request.setImageUrl("http://img/crack.jpg");
        request.setRtspUrl("rtsp://cam1");
        request.setCrackLength(2.2);
        request.setCrackArea(0.8);
        request.setLevel("高");
        request.setDescription("识别到裂缝");

        AjaxResult result = agvAnalysisController.receiveAnalysisResult(request);

        assertEquals(200, result.getCode());
        AgvFlaw flaw = (AgvFlaw) result.getData();
        assertEquals(1L, flaw.getTaskId());
        assertEquals("结构缺陷", flaw.getFlawType());
        assertEquals("隧道裂缝", flaw.getFlawName());
        assertEquals("识别到裂缝", flaw.getFlawDesc());
        assertEquals("裂缝分割模型", flaw.getSource());
        assertEquals(0, flaw.getConfirmed());
        assertEquals(0, flaw.getUploaded());
        ArgumentCaptor<AgvFlaw> captor = ArgumentCaptor.forClass(AgvFlaw.class);
        verify(agvFlawMapper).insert(captor.capture());
        assertSame(flaw, captor.getValue());
    }
}
