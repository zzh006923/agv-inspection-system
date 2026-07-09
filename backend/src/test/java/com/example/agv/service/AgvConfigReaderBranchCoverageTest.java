package com.example.agv.service;

import com.example.agv.entity.AgvConfig;
import com.example.agv.mapper.AgvConfigMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AgvConfigReaderBranchCoverageTest {

    @Mock
    private AgvConfigMapper agvConfigMapper;

    @InjectMocks
    private AgvConfigReader reader;

    @Test
    void allScalarGettersShouldReturnNullWhenConfigIsMissing() {
        when(agvConfigMapper.selectOne(any())).thenReturn(null);

        assertNull(reader.getHost());
        assertNull(reader.getDrivePort());
        assertNull(reader.getAnalysisPort());
        assertNull(reader.getCloudUrl());
        assertNull(reader.getCloudApiKey());
    }

    @Test
    void getConfigShouldReturnMapperResult() {
        AgvConfig config = new AgvConfig();
        config.setHost("192.168.2.57");
        when(agvConfigMapper.selectOne(any())).thenReturn(config);

        AgvConfig result = reader.getConfig();

        assertSame(config, result);
    }
}
