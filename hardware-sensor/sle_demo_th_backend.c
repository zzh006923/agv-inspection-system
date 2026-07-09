#include <stdio.h>
#include <string.h>
#include <stdbool.h>
#include <stdint.h>
#include "lwip/ip_addr.h"
#include "lwip/api.h"

#include "sle_demo_th_backend.h"
#include "sle_demo_th_port.h"

/* WS63 SDK WiFi STA API */
#include "wifi_device.h"
#include "wifi_device_config.h"


extern errcode_t connect_to_hotspot(char *ssid, char *pswd);
extern errcode_t wifi_sta_wait_ready_for_tcp(uint32_t timeout_ms);


/* WiFi config: Hub 必须连接电脑所在的同一个 WiFi */
#define TH_WIFI_SSID "inhand"
#define TH_WIFI_PSWD "12345678"
/* Backend config: 电脑无线局域网 IPv4 */
#define TH_BACKEND_HOST "192.168.2.76"
#define TH_BACKEND_PORT 8088
#define TH_BACKEND_PATH "/agv/sensor/report"
#define HUB_BUILD_MARK "HUB_AUTO_TASK_192168276_V2"
/* 0 表示不固定任务：后端自动挂到最新“巡视中”任务，避免前端打开任务10/11时却写入任务9 */
#define TH_TASK_ID 0

#define TH_DEVICE_ID "server_th_001"

static bool g_wifi_ready = false;

/*
 * 使用 SDK 的 wifi_sta_enable() + wifi_sta_connect() 连接 WiFi。
 * SSID 和密码在 TH_WIFI_SSID / TH_WIFI_PSWD 配置。
 * 连接后启动 DHCP 获取 IP。
 */
static bool th_wifi_connect(void)
{
    errcode_t ret;

    printf("========== HUB_WIFI_USE_PROJECT_CONNECT ==========\r\n");

    /*
     * 复用工程已有 WiFi 连接流程：
     * connect_to_hotspot 内部会处理：
     * wifi_is_sta_enabled / wifi_sta_enable / scan / match ap /
     * wifi_sta_connect / DHCP / wait ip。
     */
    ret = connect_to_hotspot(TH_WIFI_SSID, TH_WIFI_PSWD);
    printf("========== connect_to_hotspot ret=%d ==========\r\n", ret);

    if (ret != ERRCODE_SUCC) {
        printf("========== CONNECT_TO_HOTSPOT_FAILED ==========\r\n");
        return false;
    }

    /*
     * 等待 TCP 可用：确认 DHCP/IP 基本就绪。
     */
    ret = wifi_sta_wait_ready_for_tcp(15000);
    printf("========== wifi_sta_wait_ready_for_tcp ret=%d ==========\r\n", ret);

    if (ret != ERRCODE_SUCC) {
        printf("========== WIFI_TCP_NOT_READY ==========\r\n");
        return false;
    }

    printf("========== WIFI_READY_FOR_HTTP ==========\r\n");
    return true;
}
static int th_abs_int(int value)
{
    return value < 0 ? -value : value;
}

/*
 * 生成温湿度 JSON。
 * 注意：WS63/嵌入式环境下 snprintf 的 %.2f 可能不稳定，
 * 这里改成整数拆小数，避免生成非法 JSON。
 */
static int th_backend_make_json(char *buf, int buf_size, float temperature, float humidity)
{
    if (buf == NULL || buf_size <= 0) {
        return -1;
    }

    int temp100 = (int)(temperature * 100.0f);
    int humi100 = (int)(humidity * 100.0f);

    return snprintf(buf, buf_size,
        "{\"deviceId\":\"%s\",\"taskId\":%d,\"sensorType\":\"th\","
        "\"temperature\":%d.%02d,\"humidity\":%d.%02d,\"distance\":1.5}",
        TH_DEVICE_ID,
        TH_TASK_ID,
        temp100 / 100,
        th_abs_int(temp100 % 100),
        humi100 / 100,
        th_abs_int(humi100 % 100)
    );
}

static bool th_backend_http_post(const char *json)
{
	
    if (json == NULL) {
        th_log_error("[TH-backend] json is null");
        return false;
    }
    printf("========== HTTP_POST_BEGIN ==========\r\n");
    printf("BACKEND=%s:%d%s\r\n", TH_BACKEND_HOST, TH_BACKEND_PORT, TH_BACKEND_PATH);
    printf("JSON=%s\r\n", json);

    ip_addr_t ip;
    if (ipaddr_aton(TH_BACKEND_HOST, &ip) == 0) {
        th_log_error("[TH-backend] invalid backend ip: %s", TH_BACKEND_HOST);
        return false;
    }

    struct netconn *conn = netconn_new(NETCONN_TCP);
    if (conn == NULL) {
        th_log_error("[TH-backend] netconn_new failed");
        return false;
    }

    err_t err = netconn_connect(conn, &ip, TH_BACKEND_PORT);
    printf("netconn_connect err=%d\r\n", err);

    if (err != ERR_OK) {
        printf("========== NETCONN_CONNECT_FAILED ==========\r\n");
        th_log_error("[TH-backend] netconn_connect failed, err=%d", err);
        netconn_delete(conn);
        return false;
    }

    char req[1024];
    int json_len = strlen(json);

    int req_len = snprintf(req, sizeof(req),
        "POST %s HTTP/1.1\r\n"
        "Host: %s:%d\r\n"
        "Content-Type: application/json\r\n"
        "Content-Length: %d\r\n"
        "Connection: close\r\n"
        "\r\n"
        "%s",
        TH_BACKEND_PATH,
        TH_BACKEND_HOST,
        TH_BACKEND_PORT,
        json_len,
        json
    );

    if (req_len <= 0 || req_len >= (int)sizeof(req)) {
        th_log_error("[TH-backend] http request too long");
        netconn_close(conn);
        netconn_delete(conn);
        return false;
    }

    err = netconn_write(conn, req, req_len, NETCONN_COPY);
    printf("netconn_write err=%d\r\n", err);

    if (err != ERR_OK) {
        printf("========== NETCONN_WRITE_FAILED ==========\r\n");
        th_log_error("[TH-backend] netconn_write failed, err=%d", err);
        netconn_close(conn);
        netconn_delete(conn);
        return false;
    }

    th_log_info("[TH-backend] HTTP POST sent");

    /*
     * 读取后端 HTTP 响应，便于串口判断是否真的被后端处理成功。
     * 看到 HTTP/1.1 200 或 HTTP/1.0 200，才说明后端接口返回成功。
     */
    bool ok = false;
    struct netbuf *resp_buf = NULL;
    err = netconn_recv(conn, &resp_buf);
    printf("netconn_recv err=%d\r\n", err);

    if (err == ERR_OK && resp_buf != NULL) {
        void *data = NULL;
        u16_t len = 0;
        if (netbuf_data(resp_buf, &data, &len) == ERR_OK && data != NULL && len > 0) {
            char resp[128];
            int copy_len = len < (sizeof(resp) - 1) ? len : (sizeof(resp) - 1);
            memcpy(resp, data, copy_len);
            resp[copy_len] = '\0';

            printf("HTTP response: %s\r\n", resp);
            th_log_info("[TH-backend] response: %s", resp);

            if (strstr(resp, "HTTP/1.1 200") != NULL || strstr(resp, "HTTP/1.0 200") != NULL) {
                printf("========== HTTP_STATUS_200 ==========\r\n");
                th_log_info("[TH-backend] HTTP POST success, status code: 200");
                ok = true;
            } else {
                printf("========== HTTP_RESPONSE_NOT_200 ==========\r\n");
                th_log_error("[TH-backend] HTTP response is not 200");
            }
        }
        netbuf_delete(resp_buf);
    } else {
        th_log_error("[TH-backend] read HTTP response failed, err=%d", err);
    }

    netconn_close(conn);
    netconn_delete(conn);

    return ok;
}

bool th_backend_init(void)
{
    printf("========== %s ==========\r\n", HUB_BUILD_MARK);
    th_log_info("========== %s ==========", HUB_BUILD_MARK);
    th_log_info("[TH-backend] init");

    if (!th_wifi_connect()) {
        th_log_error("[TH-backend] wifi connect failed");
        g_wifi_ready = false;
        return false;
    }

    g_wifi_ready = true;
    th_log_info("[TH-backend] wifi connected");

    /* 已关闭开机假上传：只有收到 Server Notify 后才上报真实传感器数据。 */
    printf("========== WIFI_CONNECTED_WAIT_SENSOR_NOTIFY ==========\r\n");
    return true;
}

void th_backend_upload_async(float temperature, float humidity)
{
	printf("========== ENTER th_backend_upload_async ==========\r\n");
    printf("T=%d H=%d\r\n", (int)(temperature * 100), (int)(humidity * 100));
    if (!g_wifi_ready) {
        th_log_error("[TH-backend] wifi not ready, skip upload");
        return;
    }

    char json[256];

    int len = th_backend_make_json(json, sizeof(json), temperature, humidity);
    if (len <= 0 || len >= (int)sizeof(json)) {
        th_log_error("[TH-backend] make json failed");
        return;
    }

    th_log_info("[TH-backend] upload json: %s", json);

    if (!th_backend_http_post(json)) {
        th_log_error("[TH-backend] HTTP POST failed");
        return;
    }

    th_log_info("[TH-backend] upload success");
}

void th_backend_upload_person(int detected)
{
	printf("========== ENTER th_backend_upload_person ==========\r\n");
    printf("person detected=%d\r\n", detected);
    char json[256];

    int len = snprintf(json, sizeof(json),
        "{\"deviceId\":\"server_person_001\",\"taskId\":%d,\"sensorType\":\"person\","
        "\"personDetected\":%s,\"distance\":1.2}",
        TH_TASK_ID,
        detected ? "true" : "false"
    );

    if (len <= 0 || len >= (int)sizeof(json)) {
        th_log_error("[backend] person json too long");
        return;
    }

    th_log_info("[backend] person json: %s", json);

    if (!g_wifi_ready) {
        th_log_error("[backend] wifi not ready, skip person upload");
        return;
    }

    if (!th_backend_http_post(json)) {
        th_log_error("[backend] person HTTP POST failed");
        return;
    }

    th_log_info("[backend] person upload success");
}

void th_backend_upload_smoke(int detected)
{
	printf("========== ENTER th_backend_upload_smoke ==========\r\n");
    printf("smoke detected=%d\r\n", detected);
    char json[256];
    int smoke_value = detected ? 80 : 0;

    int len = snprintf(json, sizeof(json),
        "{\"deviceId\":\"server_smoke_001\",\"taskId\":%d,\"sensorType\":\"smoke\","
        "\"smokeDetected\":%s,\"smokeValue\":%d}",
        TH_TASK_ID,
        detected ? "true" : "false",
        smoke_value
    );

    if (len <= 0 || len >= (int)sizeof(json)) {
        th_log_error("[backend] smoke json too long");
        return;
    }

    th_log_info("[backend] smoke json: %s", json);

    if (!g_wifi_ready) {
        th_log_error("[backend] wifi not ready, skip smoke upload");
        return;
    }

    if (!th_backend_http_post(json)) {
        th_log_error("[backend] smoke HTTP POST failed");
        return;
    }

    th_log_info("[backend] smoke upload success");
}

void th_backend_upload_light(int32_t lux)
{
	printf("========== ENTER th_backend_upload_light ==========\r\n");
    printf("lux=%d\r\n", lux);
    char json[256];

    int len = snprintf(json, sizeof(json),
        "{\"deviceId\":\"server_light_001\",\"taskId\":%d,\"sensorType\":\"light\",\"lightValue\":%d}",
        TH_TASK_ID,
        lux
    );

    if (len <= 0 || len >= (int)sizeof(json)) {
        th_log_error("[backend] light json too long");
        return;
    }

    th_log_info("[backend] light json: %s", json);

    if (!g_wifi_ready) {
        th_log_error("[backend] wifi not ready, skip light upload");
        return;
    }

    if (!th_backend_http_post(json)) {
        th_log_error("[backend] light HTTP POST failed");
        return;
    }

    th_log_info("[backend] light upload success");
}
