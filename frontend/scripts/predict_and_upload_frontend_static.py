from ultralytics import YOLO
from pathlib import Path
import requests
import math
import shutil

# =========================
# 1. 基础配置
# =========================

MODEL_PATH = r"C:\Users\zzh\Desktop\软件系统开发实训\runs\segment\crackseg_cpu_demo\weights\best.pt"
IMAGE_DIR = r"C:\Users\zzh\datasets\crack-seg\images\val"
BACKEND_URL = "http://localhost:8088/agv/analysis/result"
TASK_ID = 3
ROUND = 1
MAX_UPLOAD = 30
CONF_THRESHOLD = 0.25

# 关键修改：把预测后的图片复制到前端 public 静态资源目录
STATIC_OUTPUT_DIR = str(Path.home() / "Desktop" / "软件系统开发实训" / "agv-inspection-frontend-runs-ready-logicfix" / "public" / "images" / "crack")

print("正在加载模型...")
model = YOLO(MODEL_PATH)

image_paths = []
for suffix in ["*.jpg", "*.jpeg", "*.png"]:
    image_paths.extend(Path(IMAGE_DIR).glob(suffix))

print(f"共找到 {len(image_paths)} 张图片")
print(f"预测图将复制到: {STATIC_OUTPUT_DIR}")

upload_count = 0
for image_path in image_paths:
    if upload_count >= MAX_UPLOAD:
        break

    print("\n----------------------------------------")
    print(f"正在预测图片: {image_path}")

    results = model.predict(source=str(image_path), save=True, conf=CONF_THRESHOLD, device="cpu")
    result = results[0]

    if result.boxes is None or len(result.boxes) == 0:
        print("未检测到裂缝，跳过")
        continue

    confidence = float(result.boxes.conf.max().item())

    crack_area = 0.0
    if result.masks is not None and result.masks.data is not None:
        for mask in result.masks.data:
            crack_area += float(mask.sum().item())

    crack_length = round(math.sqrt(crack_area), 2) if crack_area > 0 else 0.0

    if confidence >= 0.80 or crack_area >= 5000:
        level = "高"
    elif confidence >= 0.50 or crack_area >= 1000:
        level = "中"
    else:
        level = "低"

    save_dir = Path(result.save_dir)
    saved_image_path = save_dir / image_path.name

    output_filename = f"{image_path.stem}_predict{image_path.suffix}"
    image_url = f"/images/crack/{output_filename}"

    static_dir = Path(STATIC_OUTPUT_DIR)
    static_dir.mkdir(parents=True, exist_ok=True)
    target_path = static_dir / output_filename

    if saved_image_path.exists():
        shutil.copy(saved_image_path, target_path)
        # 同时保留原名，前端兜底兼容
        shutil.copy(saved_image_path, static_dir / image_path.name)
        print(f"预测图已复制到: {target_path}")
    else:
        print("未找到保存后的预测图，跳过复制")

    payload = {
        "taskId": TASK_ID,
        "round": ROUND,
        "distance": round(100.0 + upload_count * 15.0, 1),
        "imageUrl": image_url,
        "rtspUrl": "rtsp://192.168.2.100/live/cam1",
        "crackLength": crack_length,
        "crackArea": round(crack_area, 2),
        "confidence": round(confidence, 4),
        "level": level,
        "description": "裂缝分割模型检测到隧道壁面存在疑似裂缝"
    }

    print("准备上传到后端:")
    print(payload)

    try:
        response = requests.post(BACKEND_URL, json=payload, timeout=10)
        print("后端响应状态码:", response.status_code)
        print("后端响应内容:", response.text)
        if response.status_code == 200:
            upload_count += 1
            print(f"成功上传第 {upload_count} 条裂缝识别结果")
        else:
            print("上传失败，请检查后端接口")
    except Exception as e:
        print("请求后端失败:", e)
        print("请确认 Spring Boot 后端正在运行，且接口地址正确")

print("\n========================================")
print(f"批量预测与上传完成，共上传 {upload_count} 条记录")
print("========================================")
