package com.example.agv.entity;

import com.example.agv.dto.SensorReportDTO;
import com.example.agv.dto.ai.AiChatRequest;
import com.example.agv.dto.ai.AiChatResponse;
import org.junit.jupiter.api.Test;

import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.lang.reflect.Method;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class AllEntityAndDtoAccessorsTest {

    @Test
    void allSimpleEntitiesAndDtosShouldRoundTripProperties() throws Exception {
        assertRoundTrip(new AgvConfig());
        assertRoundTrip(new AgvFlaw());
        assertRoundTrip(new AgvIotActionRecord());
        assertRoundTrip(new AgvSensorRecord());
        assertRoundTrip(new AgvTask());
        assertRoundTrip(new AgvUploadRecord());
        assertRoundTrip(new SensorReportDTO());
        assertRoundTrip(new AiChatRequest());
        assertRoundTrip(new AiChatResponse());

        AiChatResponse response = new AiChatResponse("回答", "conv", "msg");
        assertEquals("回答", response.getAnswer());
        assertEquals("conv", response.getConversationId());
        assertEquals("msg", response.getMessageId());
    }

    private void assertRoundTrip(Object bean) throws Exception {
        for (PropertyDescriptor descriptor : Introspector.getBeanInfo(bean.getClass(), Object.class).getPropertyDescriptors()) {
            Method write = descriptor.getWriteMethod();
            Method read = descriptor.getReadMethod();
            if (write == null || read == null) {
                continue;
            }
            Object value = sampleValue(descriptor.getPropertyType(), descriptor.getName());
            write.invoke(bean, value);
            assertEquals(value, read.invoke(bean), bean.getClass().getSimpleName() + "." + descriptor.getName());
        }
    }

    private Object sampleValue(Class<?> type, String propertyName) {
        Map<Class<?>, Object> values = new HashMap<>();
        values.put(String.class, propertyName + "-value");
        values.put(Long.class, 123L);
        values.put(Integer.class, 7);
        values.put(Double.class, 3.14);
        values.put(Boolean.class, Boolean.TRUE);
        values.put(BigDecimal.class, new BigDecimal("12.34"));
        values.put(LocalDateTime.class, LocalDateTime.of(2026, 1, 2, 3, 4, 5));
        if (values.containsKey(type)) {
            return values.get(type);
        }
        throw new IllegalArgumentException("Unsupported bean property type: " + type);
    }
}
