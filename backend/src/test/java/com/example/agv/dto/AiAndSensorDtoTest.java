package com.example.agv.dto;

import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class AiAndSensorDtoTest {

    @Test
    void aiChatRequestShouldStoreFields() {
        AiChatRequest request = new AiChatRequest();
        request.setTaskId(1L);
        request.setFlawId(2L);
        request.setQuestion("任务能否上传");
        request.setContext("存在待确认故障");
        request.setConversationId("conv-001");

        assertEquals(1L, request.getTaskId());
        assertEquals(2L, request.getFlawId());
        assertEquals("任务能否上传", request.getQuestion());
        assertEquals("存在待确认故障", request.getContext());
        assertEquals("conv-001", request.getConversationId());
    }

    @Test
    void aiChatResponseConstructorAndSettersShouldWork() {
        AiChatResponse response = new AiChatResponse("建议先复核", "conv-001", "msg-001");
        assertEquals("建议先复核", response.getAnswer());
        assertEquals("conv-001", response.getConversationId());
        assertEquals("msg-001", response.getMessageId());

        response.setAnswer("已更新");
        response.setConversationId("conv-002");
        response.setMessageId("msg-002");
        assertEquals("已更新", response.getAnswer());
        assertEquals("conv-002", response.getConversationId());
        assertEquals("msg-002", response.getMessageId());
    }

    @Test
    void sensorReportDtoShouldStoreFields() {
        SensorReportDTO dto = new SensorReportDTO();
        LocalDateTime now = LocalDateTime.now();
        dto.setDeviceId("sensor-001");
        dto.setTaskId(1L);
        dto.setSensorType("temperature_humidity");
        dto.setTemperature(new BigDecimal("28.5"));
        dto.setHumidity(new BigDecimal("60"));
        dto.setPersonDetected(false);
        dto.setLightValue(40.0);
        dto.setSmokeDetected(true);
        dto.setSmokeValue(55.0);
        dto.setDistance(3.2);
        dto.setReportTime(now);

        assertEquals("sensor-001", dto.getDeviceId());
        assertEquals(1L, dto.getTaskId());
        assertEquals("temperature_humidity", dto.getSensorType());
        assertEquals(new BigDecimal("28.5"), dto.getTemperature());
        assertEquals(new BigDecimal("60"), dto.getHumidity());
        assertFalse(dto.getPersonDetected());
        assertEquals(40.0, dto.getLightValue());
        assertTrue(dto.getSmokeDetected());
        assertEquals(55.0, dto.getSmokeValue());
        assertEquals(3.2, dto.getDistance());
        assertEquals(now, dto.getReportTime());
    }
}
