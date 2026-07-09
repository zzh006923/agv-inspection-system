package com.example.agv.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.agv.entity.AgvConfig;
import com.example.agv.mapper.AgvConfigMapper;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;

/**
 * agv_config 表读取服务（只读）
 * <p>
 * 提供给系统自检等模块读取系统配置参数。
 * 写入操作由系统设置模块负责，本类不处理。
 */
@Service
public class AgvConfigReader {

    @Resource
    private AgvConfigMapper agvConfigMapper;

    /**
     * 获取有效（未删除）的系统配置
     *
     * @return AgvConfig 对象，若无配置则返回 null
     */
    public AgvConfig getConfig() {
        QueryWrapper<AgvConfig> wrapper = new QueryWrapper<>();
        wrapper.eq("delete_flag", 0);
        wrapper.last("LIMIT 1");
        return agvConfigMapper.selectOne(wrapper);
    }

    /**
     * 获取车辆IP地址
     */
    public String getHost() {
        AgvConfig config = getConfig();
        return config != null ? config.getHost() : null;
    }

    /**
     * 获取车辆控制端口
     */
    public Integer getDrivePort() {
        AgvConfig config = getConfig();
        return config != null ? config.getDrivePort() : null;
    }

    /**
     * 获取分析程序端口
     */
    public Integer getAnalysisPort() {
        AgvConfig config = getConfig();
        return config != null ? config.getAnalysisPort() : null;
    }

    /**
     * 获取云端上传地址
     */
    public String getCloudUrl() {
        AgvConfig config = getConfig();
        return config != null ? config.getCloudUrl() : null;
    }

    /**
     * 获取云端接口密钥
     */
    public String getCloudApiKey() {
        AgvConfig config = getConfig();
        return config != null ? config.getCloudApiKey() : null;
    }

    /**
     * 获取摄像头RTSP地址数组（按 cam1~cam4 顺序）
     */
    public String[] getCameraUrls() {
        AgvConfig config = getConfig();
        if (config == null) {
            return new String[0];
        }
        return new String[]{config.getCam1(), config.getCam2(), config.getCam3(), config.getCam4()};
    }
}
