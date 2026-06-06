#!/bin/bash
# MC Archive Hub 部署脚本 for Ubuntu

APP_DIR="/home/ubuntu/mc-archive-hub"
JAR_NAME="mc-archive-hub-1.0.0.jar"
DATA_DIR="/home/ubuntu/mc-archive-data"
UPLOAD_DIR="/home/ubuntu/mc-archive-uploads"
LOG_FILE="/home/ubuntu/mc-archive.log"

# 颜色输出
red() { echo -e "\033[31m$*\033[0m"; }
green() { echo -e "\033[32m$*\033[0m"; }
yellow() { echo -e "\033[33m$*\033[0m"; }

echo "=== MC Archive Hub 部署 ==="

# 1. 检查目录
echo "[1/6] 检查目录..."
mkdir -p "$APP_DIR" "$DATA_DIR" "$UPLOAD_DIR"

# 2. 停止旧进程
echo "[2/6] 停止旧进程..."
PID=$(pgrep -f "$JAR_NAME" || true)
if [ -n "$PID" ]; then
    kill "$PID" 2>/dev/null || true
    sleep 2
    kill -9 "$PID" 2>/dev/null || true
    green "已停止旧进程 PID=$PID"
else
    yellow "无运行中的旧进程"
fi

# 3. 备份数据库（如果存在）
if [ -f "$DATA_DIR/mcarchive.db" ]; then
    echo "[3/6] 备份数据库..."
    cp "$DATA_DIR/mcarchive.db" "$DATA_DIR/mcarchive.db.bak.$(date +%Y%m%d_%H%M%S)"
    green "数据库已备份"
fi

# 4. 从 GitHub 拉取最新代码并构建
echo "[4/6] 拉取最新代码..."
cd "$APP_DIR" || exit 1
if [ -d ".git" ]; then
    git pull origin main
else
    git clone https://github.com/3024884218/mc-archive-hub.git .
fi

# 5. 构建 JAR
echo "[5/6] 构建 JAR..."
./gradlew bootJar --no-daemon -q
cp "build/libs/$JAR_NAME" .
green "构建完成"

# 6. 启动应用
echo "[6/6] 启动应用..."
nohup java -jar "$JAR_NAME" \
    --server.port=8080 \
    --spring.datasource.url="jdbc:sqlite:$DATA_DIR/mcarchive.db" \
    --app.upload.dir="$UPLOAD_DIR" \
    --app.base-url="http://43.128.141.158:8080" \
    >> "$LOG_FILE" 2>&1 &

sleep 3
NEW_PID=$(pgrep -f "$JAR_NAME" || true)
if [ -n "$NEW_PID" ]; then
    green "✅ 部署成功！PID=$NEW_PID"
    echo ""
    echo "访问地址: http://43.128.141.158:8080"
    echo "日志查看: tail -f $LOG_FILE"
else
    red "❌ 启动失败，查看日志:"
    tail -20 "$LOG_FILE"
    exit 1
fi
