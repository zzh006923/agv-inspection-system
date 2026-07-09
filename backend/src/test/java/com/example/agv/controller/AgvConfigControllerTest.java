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
class AgvConfigControllerTest {

    @Mock
    private AgvConfigMapper agvConfigMapper;

    @InjectMocks
    private AgvConfigController agvConfigController;

    @Test
    void getConfigShouldCreateDefaultConfigWhenMissing() {
        when(agvConfigMapper.selectOne(any())).thenReturn(null);

        AjaxResult result = agvConfigController.getConfig();

        assertEquals(200, result.getCode());
        AgvConfig config = (AgvConfig) result.getData();
        assertEquals("192.168.2.2", config.getHost());
        assertEquals(9001, config.getDrivePort());
        assertEquals(0, config.getDeleteFlag());
        assertNotNull(config.getUpdateTime());
        verify(agvConfigMapper).insert(config);
    }

    @Test
    void updateConfigShouldUpdateByIdWhenIdExists() {
        AgvConfig config = new AgvConfig();
        config.setId(7L);
        config.setHost("192.168.2.57");
        when(agvConfigMapper.selectById(7L)).thenReturn(config);

        AjaxResult result = agvConfigController.updateConfig(config);

        assertEquals(200, result.getCode());
        assertEquals(0, config.getDeleteFlag());
        assertNotNull(config.getUpdateTime());
        verify(agvConfigMapper).updateById(config);
        assertSame(config, result.getData());
    }

    @Test
    void updateConfigShouldUpdateActiveConfigWhenRequestHasNoId() {
        AgvConfig active = new AgvConfig();
        active.setId(8L);
        AgvConfig config = new AgvConfig();
        config.setHost("192.168.2.58");
        when(agvConfigMapper.selectOne(any())).thenReturn(active);
        when(agvConfigMapper.selectById(8L)).thenReturn(config);

        AjaxResult result = agvConfigController.updateConfig(config);

        assertEquals(200, result.getCode());
        assertEquals(8L, config.getId());
        verify(agvConfigMapper).updateById(config);
        assertSame(config, result.getData());
    }
}
