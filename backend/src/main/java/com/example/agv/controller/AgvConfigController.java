package com.example.agv.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.common.AjaxResult;
import com.example.agv.entity.AgvConfig;
import com.example.agv.mapper.AgvConfigMapper;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.time.LocalDateTime;

/**
 * 系统设置控制器
 *
 * 对应系统设置页：读取和保存 AGV、云端、摄像头配置。
 */
@RestController
@RequestMapping("/agv/config")
@CrossOrigin
public class AgvConfigController {

    @Resource
    private AgvConfigMapper agvConfigMapper;

    /**
     * 获取系统配置。若数据库暂无配置，则创建一条默认配置。
     */
    @GetMapping
    public AjaxResult getConfig() {
        AgvConfig config = getActiveConfig();
        if (config == null) {
            config = buildDefaultConfig();
            agvConfigMapper.insert(config);
        }
        return AjaxResult.success(config);
    }

    /**
     * 保存系统配置。
     * 有 id 时更新对应记录；无 id 时更新当前有效配置；仍无配置时新增。
     */
    @PutMapping
    public AjaxResult updateConfig(@RequestBody AgvConfig config) {
        config.setUpdateTime(LocalDateTime.now());
        if (config.getDeleteFlag() == null) {
            config.setDeleteFlag(0);
        }

        if (config.getId() != null && agvConfigMapper.selectById(config.getId()) != null) {
            agvConfigMapper.updateById(config);
            return AjaxResult.success(agvConfigMapper.selectById(config.getId()));
        }

        AgvConfig active = getActiveConfig();
        if (active != null) {
            config.setId(active.getId());
            agvConfigMapper.updateById(config);
            return AjaxResult.success(agvConfigMapper.selectById(active.getId()));
        }

        agvConfigMapper.insert(config);
        return AjaxResult.success(config);
    }

    private AgvConfig getActiveConfig() {
        QueryWrapper<AgvConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0).last("LIMIT 1");
        return agvConfigMapper.selectOne(wrapper);
    }

    private AgvConfig buildDefaultConfig() {
        AgvConfig config = new AgvConfig();
        config.setHost("192.168.2.2");
        config.setDrivePort(9001);
        config.setAnalysisPort(9002);
        config.setCloudUrl("http://192.168.2.57/prod-api");
        config.setCloudApiKey("");
        config.setDbHost("localhost");
        config.setDbPort(3306);
        config.setDbName("agv_inspection");
        config.setDbUsername("root");
        config.setDbPassword("1234");
        config.setControlProtocol("http");
        config.setCam1("rtsp://192.168.2.2/live/cam1");
        config.setUsername1("admin");
        config.setPassword1("123456");
        config.setCam2("rtsp://192.168.2.2/live/cam2");
        config.setUsername2("admin");
        config.setPassword2("123456");
        config.setCam3("rtsp://192.168.2.2/live/cam3");
        config.setUsername3("admin");
        config.setPassword3("123456");
        config.setCam4("rtsp://192.168.2.2/live/cam4");
        config.setUsername4("admin");
        config.setPassword4("123456");
        config.setDeleteFlag(0);
        config.setUpdateTime(LocalDateTime.now());
        return config;
    }
}
