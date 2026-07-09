package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvConfig;
import com.example.agv.mapper.AgvConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AgvConfigControllerCoverageBoostTest {

    @Mock
    private AgvConfigMapper agvConfigMapper;

    @InjectMocks
    private AgvConfigController controller;

    @Test
    void updateConfigShouldInsertDefaultWhenNoActiveConfigExists() {
        AgvConfig config = new AgvConfig();
        config.setHost("10.0.0.1");
        when(agvConfigMapper.selectOne(any())).thenReturn(null);

        AjaxResult result = controller.updateConfig(config);

        assertEquals(200, result.getCode());
        assertEquals(0, config.getDeleteFlag());
        assertNotNull(config.getUpdateTime());
        verify(agvConfigMapper).insert(config);
        verify(agvConfigMapper, never()).updateById(any());
    }

    @Test
    void updateConfigShouldInsertWhenIdDoesNotExist() {
        AgvConfig config = new AgvConfig();
        config.setId(8L);
        config.setHost("10.0.0.2");
        when(agvConfigMapper.selectById(8L)).thenReturn(null);

        AjaxResult result = controller.updateConfig(config);

        assertEquals(200, result.getCode());
        verify(agvConfigMapper).insert(config);
        verify(agvConfigMapper, never()).updateById(any());
    }

    @Test
    void getConfigShouldReturnExistingActiveConfig() {
        AgvConfig existing = new AgvConfig();
        existing.setId(1L);
        existing.setHost("192.168.1.10");
        when(agvConfigMapper.selectOne(any())).thenReturn(existing);

        AjaxResult result = controller.getConfig();

        assertEquals(200, result.getCode());
        assertSame(existing, result.getData());
        verify(agvConfigMapper, never()).insert(any());
    }
}
