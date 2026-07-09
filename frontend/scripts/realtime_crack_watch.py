import os
import re
import cv2
import time
import math
import shutil
import requests
from pathlib import Path
from ultralytics import YOLO


# =========================
# 1. 基础配置
# =========================

BASE_DIR = Path.home() / "Desktop" / "软件系统开发实训"

MODEL_PATH = BASE_DIR / "runs" / "segment" / "crackseg_cpu_demo" / "weights" / "best.pt"

# OpenCV 读取 RTSP 时强制走 TCP，稳定一些
os.environ["OPENCV_FFMPEG_CAPTURE_OPTIONS"] = "rtsp_transport;tcp|stimeout;5000000|max_delay;500000"

# 四路摄像头，按你 easy-api 查到的真实 IP 配置
CAMERA_SOURCES = [
    {
        "key": "cam1",
        "name": "摄像头1",
        "rtsp": "rtsp://admin:Admin123@192.168.2.14:554/Streaming/Channels/101"
    },
    {
        "key": "cam2",
        "name": "摄像头2",
        "rtsp": "rtsp://admin:Admin123@192.168.2.13:554/Streaming/Channels/101"
    },
    {
        "key": "cam3",
        "name": "摄像头3",
        "rtsp": "rtsp://admin:Admin123@192.168.2.12:554/Streaming/Channels/101"
    },
    {
        "key": "cam4",
        "name": "摄像头4",
        "rtsp": "rtsp://admin:Admin123@192.168.2.11:554/Streaming/Channels/101"
    }
]

BACKEND_BASE_URL = "http://localhost:8088"
UPLOAD_URL = f"{BACKEND_BASE_URL}/agv/analysis/result"

ROUND = 1
CONF_THRESHOLD = 0.25

# 每轮识别间隔
INTERVAL_SECONDS = 3

# 同一路摄像头识别到裂缝后，至少间隔多少秒再上传，避免重复刷屏
UPLOAD_COOLDOWN_SECONDS = 10

# 识别图片保存到当前前端静态资源目录
STATIC_OUTPUT_DIR = (
    BASE_DIR
    / "agv-inspection-frontend-runs-ready-logicfix"
    / "public"
    / "images"
    / "crack"
)
STATIC_OUTPUT_DIR.mkdir(parents=True, exist_ok=True)


# =========================
# 2. 通用工具函数
# =========================

def extract_rows(data):
    """兼容不同后端分页返回格式。"""
    if isinstance(data, list):
        return data

    if not isinstance(data, dict):
        return []

    if isinstance(data.get("rows"), list):
        return data["rows"]

    inner = data.get("data")

    if isinstance(inner, list):
        return inner

    if isinstance(inner, dict):
        for key in ["rows", "list", "records", "items"]:
            if isinstance(inner.get(key), list):
                return inner[key]

    for key in ["list", "records", "items"]:
        if isinstance(data.get(key), list):
            return data[key]

    return []


def get_task_status(task):
    return str(
        task.get("status")
        or task.get("taskStatus")
        or task.get("state")
        or ""
    ).strip()


def get_current_running_task():
    """
    获取当前正在巡视的任务。
    如果多个任务都是“巡视中”，默认选 id 最大的最新任务，并打印警告。
    """
    url = f"{BACKEND_BASE_URL}/agv/task/list"

    try:
        # 先按状态查
        resp = requests.get(
            url,
            params={
                "taskStatus": "巡视中",
                "pageNum": 1,
                "pageSize": 100
            },
            timeout=5
        )
        resp.raise_for_status()
        rows = extract_rows(resp.json())

        running_tasks = [
            t for t in rows
            if get_task_status(t) == "巡视中"
        ]

        # 如果接口没有按 taskStatus 正确过滤，再查全部任务自己筛
        if not running_tasks:
            resp = requests.get(
                url,
                params={
                    "pageNum": 1,
                    "pageSize": 100
                },
                timeout=5
            )
            resp.raise_for_status()
            rows = extract_rows(resp.json())

            running_tasks = [
                t for t in rows
                if get_task_status(t) == "巡视中"
            ]

        if not running_tasks:
            print("当前没有状态为“巡视中”的任务，请先在前端启动任务。")
            return None

        if len(running_tasks) > 1:
            print("警告：当前存在多个“巡视中”任务，脚本将默认选择 id 最大的最新任务。")
            for t in running_tasks:
                print(
                    f" - id={t.get('id')} 编号={t.get('taskCode')} "
                    f"名称={t.get('taskName')} 状态={get_task_status(t)}"
                )

        running_tasks = sorted(
            running_tasks,
            key=lambda x: int(x.get("id", 0)),
            reverse=True
        )

        task = running_tasks[0]

        print(
            f"当前巡视任务：id={task.get('id')}，"
            f"编号={task.get('taskCode')}，"
            f"名称={task.get('taskName')}"
        )

        return task

    except Exception as e:
        print("获取当前巡视任务失败：", e)
        return None


def parse_float(value):
    try:
        if value is None:
            return None
        return float(value)
    except Exception:
        return None


def extract_number_from_text(text, key):
    try:
        pattern = rf"{key}\s*=\s*([-+]?\d+(\.\d+)?)"
        match = re.search(pattern, text, re.IGNORECASE)
        if match:
            return float(match.group(1))
    except Exception:
        pass
    return None


def get_vehicle_status():
    """
    读取小车运行状态。
    返回：
    {
        "moving": True/False,
        "direction": "forward/backward/stop/unknown",
        "position": 当前距离 or None
    }
    """
    default_status = {
        "moving": False,
        "direction": "unknown",
        "position": None
    }

    try:
        resp = requests.get(f"{BACKEND_BASE_URL}/agv/movement/heartbeat", timeout=5)
        resp.raise_for_status()
        data = resp.json()

        vehicle_data = data.get("data", {})

        # 后端 data 是 dict
        if isinstance(vehicle_data, dict):
            is_running = vehicle_data.get("isRunning")
            direction = str(
                vehicle_data.get("direction")
                or vehicle_data.get("moveDirection")
                or vehicle_data.get("drivingDirection")
                or ""
            ).lower()

            position = (
                parse_float(vehicle_data.get("currentPosition"))
                or parse_float(vehicle_data.get("position"))
                or parse_float(vehicle_data.get("distance"))
            )

            moving = False

            if is_running is True:
                moving = True

            if direction in ["forward", "backward", "前进", "后退"]:
                moving = True

            if direction in ["stop", "stopped", "停止", ""]:
                if is_running is not True:
                    moving = False

            return {
                "moving": moving,
                "direction": direction or "unknown",
                "position": position
            }

        # 后端 data 是字符串
        text = str(vehicle_data).lower()

        moving = False
        if "isrunning=true" in text:
            moving = True
        if "direction=forward" in text or "direction=backward" in text:
            moving = True
        if "direction=stop" in text and "isrunning=true" not in text:
            moving = False

        position = extract_number_from_text(text, "currentPosition")
        if position is None:
            position = extract_number_from_text(text, "position")
        if position is None:
            position = extract_number_from_text(text, "distance")

        direction = "unknown"
        if "direction=forward" in text:
            direction = "forward"
        elif "direction=backward" in text:
            direction = "backward"
        elif "direction=stop" in text:
            direction = "stop"

        return {
            "moving": moving,
            "direction": direction,
            "position": position
        }

    except Exception as e:
        print("读取小车运行状态失败，暂停识别：", e)
        return default_status


def calc_level(confidence, crack_area):
    if confidence >= 0.80 or crack_area >= 5000:
        return "高"
    elif confidence >= 0.50 or crack_area >= 1000:
        return "中"
    else:
        return "低"


def read_camera_frame(rtsp_url, camera_name):
    """
    读取单路摄像头一帧。
    失败不退出脚本，只跳过该摄像头。
    """
    cap = None

    try:
        cap = cv2.VideoCapture(rtsp_url, cv2.CAP_FFMPEG)

        if not cap.isOpened():
            print(f"{camera_name} 连接失败，跳过。")
            return None

        # 有些 RTSP 第一帧不稳定，多读几次
        frame = None
        for _ in range(5):
            ret, current_frame = cap.read()
            if ret and current_frame is not None:
                frame = current_frame
                break
            time.sleep(0.2)

        if frame is None:
            print(f"{camera_name} 读取画面失败，跳过。")
            return None

        return frame

    except Exception as e:
        print(f"{camera_name} 读取异常：{e}")
        return None

    finally:
        if cap is not None:
            cap.release()


def upload_result(task_id, camera, image_url, confidence, crack_area, crack_length, level, distance):
    """
    上传裂缝识别结果到 Spring Boot 后端。
    """
    payload = {
        "taskId": task_id,
        "round": ROUND,
        "distance": round(distance, 1),
        "imageUrl": image_url,
        "rtspUrl": camera["rtsp"],
        "crackLength": crack_length,
        "crackArea": round(crack_area, 2),
        "confidence": round(confidence, 4),
        "level": level,
        "description": f"{camera['name']}实时画面经裂缝分割模型检测到疑似隧道裂缝"
    }

    print("准备上传识别结果：")
    print(payload)

    try:
        response = requests.post(UPLOAD_URL, json=payload, timeout=10)
        print("后端状态码：", response.status_code)
        print("后端响应：", response.text)

        return response.status_code == 200

    except Exception as e:
        print("上传失败：", e)
        return False


# =========================
# 3. 主流程
# =========================

def main():
    if not MODEL_PATH.exists():
        print(f"模型不存在：{MODEL_PATH}")
        return

    print("正在加载裂缝识别模型...")
    model = YOLO(str(MODEL_PATH))

    print("已配置四路摄像头：")
    for camera in CAMERA_SOURCES:
        print(f"{camera['name']}：{camera['rtsp']}")

    print("脚本开始监听：")
    print("只有任务处于“巡视中”，且小车正在前进/后退时，才会轮询四路摄像头识别裂缝。")
    print("按 Ctrl+C 停止脚本。")

    frame_index = 0

    # 每路摄像头单独记录上次上传时间，避免某一路连续重复上传
    last_upload_time_by_camera = {
        camera["key"]: 0
        for camera in CAMERA_SOURCES
    }

    try:
        while True:
            # 1. 检查是否有巡视中任务
            current_task = get_current_running_task()

            if current_task is None:
                print("当前没有巡视中任务，暂停识别。")
                time.sleep(2)
                continue

            current_task_id = current_task.get("id")

            # 2. 检查小车是否真的在运行
            vehicle_status = get_vehicle_status()

            if not vehicle_status["moving"]:
                print(
                    f"当前任务存在，但小车未运行，暂停识别。"
                    f"方向={vehicle_status['direction']}，位置={vehicle_status['position']}"
                )
                time.sleep(5)
                continue

            print(
                f"\n小车正在运行，开始轮询四路摄像头。"
                f"方向={vehicle_status['direction']}，位置={vehicle_status['position']}"
            )

            # 3. 轮询四路摄像头
            for camera in CAMERA_SOURCES:
                camera_key = camera["key"]
                camera_name = camera["name"]
                rtsp_url = camera["rtsp"]

                print("\n----------------------------------------")
                print(f"正在读取 {camera_name}：{rtsp_url}")

                frame = read_camera_frame(rtsp_url, camera_name)

                if frame is None:
                    continue

                frame_index += 1
                timestamp = time.strftime("%Y%m%d_%H%M%S")

                raw_filename = f"realtime_{camera_key}_{timestamp}_{frame_index}.jpg"
                raw_path = STATIC_OUTPUT_DIR / raw_filename

                cv2.imwrite(str(raw_path), frame)

                print(f"正在识别 {camera_name} 实时帧：{raw_filename}")

                # 4. 模型识别
                results = model.predict(
                    source=str(raw_path),
                    save=False,
                    conf=CONF_THRESHOLD,
                    device="cpu",
                    verbose=False
                )

                result = results[0]

                # 5. 未检测到裂缝
                if result.boxes is None or len(result.boxes) == 0:
                    print(f"{camera_name} 未检测到裂缝")
                    try:
                        raw_path.unlink()
                    except Exception:
                        pass
                    continue

                # 6. 冷却时间，避免重复上传
                now = time.time()
                last_upload_time = last_upload_time_by_camera.get(camera_key, 0)

                if now - last_upload_time < UPLOAD_COOLDOWN_SECONDS:
                    print(f"{camera_name} 检测到裂缝，但距离上次上传太近，本次跳过。")
                    try:
                        raw_path.unlink()
                    except Exception:
                        pass
                    continue

                # 7. 计算裂缝信息
                confidence = float(result.boxes.conf.max().item())

                crack_area = 0.0
                if result.masks is not None and result.masks.data is not None:
                    for mask in result.masks.data:
                        crack_area += float(mask.sum().item())

                crack_length = round(math.sqrt(crack_area), 2) if crack_area > 0 else 0.0
                level = calc_level(confidence, crack_area)

                output_filename = f"realtime_{camera_key}_{timestamp}_{frame_index}_predict.jpg"
                output_path = STATIC_OUTPUT_DIR / output_filename

                try:
                    annotated = result.plot()
                    cv2.imwrite(str(output_path), annotated)
                except Exception as e:
                    print("保存预测图失败，使用原图兜底：", e)
                    shutil.copy(raw_path, output_path)

                image_url = f"/images/crack/{output_filename}"

                # 优先使用小车真实当前位置，没有则用帧序号兜底
                if vehicle_status["position"] is not None:
                    distance = vehicle_status["position"]
                else:
                    distance = 100 + frame_index * 2

                # 8. 上传后端
                ok = upload_result(
                    task_id=current_task_id,
                    camera=camera,
                    image_url=image_url,
                    confidence=confidence,
                    crack_area=crack_area,
                    crack_length=crack_length,
                    level=level,
                    distance=distance
                )

                if ok:
                    last_upload_time_by_camera[camera_key] = now
                    print(f"{camera_name} 实时裂缝识别结果已上传")
                else:
                    print(f"{camera_name} 识别结果上传失败")

                # 删除原始帧，只保留预测图，避免文件夹越来越大
                try:
                    raw_path.unlink()
                except Exception:
                    pass

            time.sleep(INTERVAL_SECONDS)

    except KeyboardInterrupt:
        print("\n已停止实时识别脚本。")


if __name__ == "__main__":
    main()