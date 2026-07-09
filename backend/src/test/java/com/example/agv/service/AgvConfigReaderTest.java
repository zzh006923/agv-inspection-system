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
class AgvConfigReaderTest {

    @Mock
    private AgvConfigMapper agvConfigMapper;

    @InjectMocks
    private AgvConfigReader agvConfigReader;

    @Test
    void shouldReadBasicConfigFields() {
        AgvConfig config = new AgvConfig();
        config.setHost("192.168.2.57");
        config.setDrivePort(8081);
        config.setAnalysisPort(9000);
        config.setCloudUrl("http://cloud.example.com");
        config.setCloudApiKey("key-001");
        when(agvConfigMapper.selectOne(any())).thenReturn(config);

        assertEquals("192.168.2.57", agvConfigReader.getHost());
        assertEquals(8081, agvConfigReader.getDrivePort());
        assertEquals(9000, agvConfigReader.getAnalysisPort());
        assertEquals("http://cloud.example.com", agvConfigReader.getCloudUrl());
        assertEquals("key-001", agvConfigReader.getCloudApiKey());
    }

    @Test
    void getCameraUrlsShouldReturnFourCameraUrlsInOrder() {
        AgvConfig config = new AgvConfig();
        config.setCam1("rtsp://cam1");
        config.setCam2("rtsp://cam2");
        config.setCam3("rtsp://cam3");
        config.setCam4("rtsp://cam4");
        when(agvConfigMapper.selectOne(any())).thenReturn(config);

        String[] urls = agvConfigReader.getCameraUrls();

        assertArrayEquals(new String[]{"rtsp://cam1", "rtsp://cam2", "rtsp://cam3", "rtsp://cam4"}, urls);
    }

    @Test
    void getCameraUrlsShouldReturnEmptyArrayWhenConfigNotFound() {
        when(agvConfigMapper.selectOne(any())).thenReturn(null);

        String[] urls = agvConfigReader.getCameraUrls();

        assertNotNull(urls);
        assertEquals(0, urls.length);
    }
}
