package com.example.agv.controller;

import com.example.agv.common.AjaxResult;
import com.example.agv.service.AgvConfigReader;
import com.example.agv.service.AgvMovementStateService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import javax.sql.DataSource;
import java.io.File;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.URI;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * 系统自检控制器。
 *
 * 自检顺序：文件系统 → 数据库 → AGV车辆 → 摄像头。
 * mockMode 只用于没有真实设备的课堂演示；连接车载 WiFi 或真实设备时，
 * 可在 application.yml 中将 agv.mock-mode 改为 false，执行真实连接检查。
 */
@RestController
@RequestMapping("/system/check")
@CrossOrigin
public class SystemCheckController {

    /** true：课堂演示模式；false：真实设备检测模式 */
    @Value("${agv.mock-mode:true}")
    private boolean mockMode;

    @Resource
    private AgvConfigReader agvConfigReader;

    @Resource
    private DataSource dataSource;

    @Resource
    private AgvMovementStateService agvMovementStateService;

    /**
     * 获取当前模拟模式状态。
     */
    @GetMapping("/mock-mode")
    public AjaxResult getMockMode() {
        return AjaxResult.success(mockMode);
    }

    /**
     * 临时切换模拟模式，便于课堂演示和真实设备调试。
     */
    @PutMapping("/mock-mode")
    public AjaxResult setMockMode(@RequestParam boolean enabled) {
        this.mockMode = enabled;
        return AjaxResult.success(mockMode);
    }

    /**
     * 1. 检查文件系统可用性。
     */
    @GetMapping("/fs")
    public AjaxResult checkFs() {
        if (mockMode) {
            return AjaxResult.success(new CheckResult("文件系统完整性", true, "演示模式：文件系统检查通过"));
        }
        try {
            String basePath = System.getProperty("user.dir");
            String[] requiredPaths = {
                    basePath + File.separator + "logs",
                    basePath + File.separator + "config"
            };
            for (String path : requiredPaths) {
                File dir = new File(path);
                if (!dir.exists() && !dir.mkdirs()) {
                    return AjaxResult.error("文件系统异常：无法创建目录 " + path);
                }
                if (!dir.canRead() || !dir.canWrite()) {
                    return AjaxResult.error("文件系统异常：目录不可读写 " + path);
                }
            }
            return AjaxResult.success(new CheckResult("文件系统完整性", true, "系统目录存在且可读写"));
        } catch (Exception e) {
            return AjaxResult.error("文件系统检查失败：" + e.getMessage());
        }
    }

    /**
     * 2. 检查数据库连接。
     */
    @GetMapping("/db")
    public AjaxResult checkDb() {
        if (mockMode) {
            return AjaxResult.success(new CheckResult("数据库连接", true, "演示模式：数据库连接检查通过"));
        }
        try (Connection conn = dataSource.getConnection()) {
            if (conn.isValid(3)) {
                return AjaxResult.success(new CheckResult("数据库连接", true, "数据库连接正常"));
            }
            return AjaxResult.error("数据库连接测试未通过");
        } catch (SQLException e) {
            return AjaxResult.error("数据库连接失败：" + e.getMessage());
        }
    }

    /**
     * 3. 检查 AGV 车辆连接。
     */
    @GetMapping("/agv")
    public AjaxResult checkAgv() {
        if (mockMode) {
            return AjaxResult.success(new CheckResult("车辆控制通信", true, "演示模式：AGV 车辆连接检查通过"));
        }
        try {
            Map<String, Object> conn = agvMovementStateService.checkRealConnection();
            Map<String, Object> data = new HashMap<>();
            data.put("item", "车辆控制通信");
            data.put("passed", true);
            data.put("message", "AGV 真实控制接口连接正常");
            data.put("connection", conn);

            // 分析端口用于裂缝/图像识别程序。如果已配置，则一并检测端口可达性。
            String host = agvConfigReader.getHost();
            Integer analysisPort = agvConfigReader.getAnalysisPort();
            if (!isBlank(host) && analysisPort != null) {
                try (Socket analysisSocket = new Socket()) {
                    analysisSocket.connect(new InetSocketAddress(host, analysisPort), 2000);
                    data.put("analysisService", "图像分析端口连接正常：" + host + ":" + analysisPort);
                } catch (Exception e) {
                    data.put("analysisService", "图像分析端口暂未连接：" + host + ":" + analysisPort + " - " + e.getMessage());
                }
            }
            return AjaxResult.success(data);
        } catch (Exception e) {
            return AjaxResult.error(e.getMessage());
        }
    }

    /**
     * 4. 检查摄像头通道状态。
     * 支持 rtsp://ip/live/cam1 这类未显式写端口的地址，默认按 RTSP 554 端口检查。
     */
    @GetMapping("/cam")
    public AjaxResult checkCam() {
        if (mockMode) {
            return AjaxResult.success(new CheckResult("摄像头通道", true, "演示模式：4 个摄像头通道检查通过"));
        }
        String[] cams = agvConfigReader.getCameraUrls();
        if (cams.length == 0) {
            return AjaxResult.error("系统配置不完整：未设置摄像头地址");
        }
        StringBuilder failed = new StringBuilder();
        int checkedCount = 0;
        for (int i = 0; i < cams.length; i++) {
            String url = cams[i];
            if (isBlank(url)) {
                continue;
            }
            checkedCount++;
            try {
                URI uri = URI.create(url.trim());
                String host = uri.getHost();
                int port = uri.getPort() > 0 ? uri.getPort() : 554;
                if (isBlank(host)) {
                    throw new IllegalArgumentException("无法解析摄像头地址");
                }
                try (Socket socket = new Socket()) {
                    socket.connect(new InetSocketAddress(host, port), 2000);
                }
            } catch (Exception e) {
                if (failed.length() > 0) failed.append("；");
                failed.append("摄像头").append(i + 1).append("连接失败：").append(e.getMessage());
            }
        }
        if (checkedCount == 0) {
            return AjaxResult.error("系统配置不完整：摄像头地址为空");
        }
        if (failed.length() > 0) {
            return AjaxResult.error("摄像头检测异常：" + failed);
        }
        return AjaxResult.success(new CheckResult("摄像头通道", true, checkedCount + " 个摄像头通道连接正常"));
    }

    /**
     * 一次性执行全部自检，前端可据此判断是否允许进入主页。
     */
    @GetMapping("/all")
    public AjaxResult checkAll() {
        Map<String, Object> data = new HashMap<>();

        AjaxResult fs = checkFs();
        AjaxResult db = checkDb();
        AjaxResult agv = checkAgv();
        AjaxResult cam = checkCam();

        data.put("fs", normalize(fs));
        data.put("db", normalize(db));
        data.put("agv", normalize(agv));
        data.put("cam", normalize(cam));

        boolean passed = fs.getCode() == 200
                && db.getCode() == 200
                && agv.getCode() == 200
                && cam.getCode() == 200;

        data.put("passed", passed);
        data.put("mockMode", mockMode);
        data.put("message", passed ? "系统自检全部通过" : "系统自检存在异常");

        return AjaxResult.success(data);
    }

    private Map<String, Object> normalize(AjaxResult result) {
        Map<String, Object> item = new HashMap<>();
        item.put("code", result.getCode());
        item.put("passed", result.getCode() == 200);
        item.put("msg", result.getMsg());
        item.put("data", result.getData());
        return item;
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
    }

    /**
     * 自检结果内部类。
     */
    static class CheckResult {
        private String item;
        private boolean passed;
        private String message;

        public CheckResult() {}

        public CheckResult(String item, boolean passed, String message) {
            this.item = item;
            this.passed = passed;
            this.message = message;
        }

        public String getItem() { return item; }
        public void setItem(String item) { this.item = item; }
        public boolean isPassed() { return passed; }
        public void setPassed(boolean passed) { this.passed = passed; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
}
