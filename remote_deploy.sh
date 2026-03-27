#!/bin/bash
set -e

APP_DIR="/opt/apps/account-book"
CONTAINER_NAME="account-book-app"
IMAGE_NAME="account-book:latest"
HOST_PORT=10011
CONTAINER_PORT=8080

echo "=== 开始部署 ==="

# 清理旧容器
echo "清理旧容器..."
if docker ps -a --format '{{.Names}}' | grep -q "^${CONTAINER_NAME}$"; then
    docker stop ${CONTAINER_NAME} 2>/dev/null || true
    docker rm ${CONTAINER_NAME} 2>/dev/null || true
fi

# 清理占用端口的容器
echo "检查端口 ${HOST_PORT}..."
for c in $(docker ps --filter "publish=${HOST_PORT}" --format '{{.Names}}'); do
    echo "停止占用端口的容器: $c"
    docker stop $c 2>/dev/null || true
    docker rm $c 2>/dev/null || true
done

# 清理旧镜像
echo "清理旧镜像..."
if docker images --format '{{.Repository}}:{{.Tag}}' | grep -q "^${IMAGE_NAME}$"; then
    docker rmi ${IMAGE_NAME} 2>/dev/null || true
fi

# 构建镜像
echo "构建Docker镜像..."
cd ${APP_DIR}
docker build -t ${IMAGE_NAME} .

# 复制data目录（如果不存在）
mkdir -p ${APP_DIR}/data
cp -n /opt/apps/account-book/*.csv ${APP_DIR}/data/ 2>/dev/null || true

# 启动容器
echo "启动容器..."
docker run -d \
    --name ${CONTAINER_NAME} \
    -p ${HOST_PORT}:${CONTAINER_PORT} \
    -v ${APP_DIR}/data:/app/data \
    --restart=always \
    ${IMAGE_NAME}

echo "等待服务启动..."
sleep 15

# 检查容器状态
if [ "$(docker inspect --format '{{.State.Running}}' ${CONTAINER_NAME})" = "true" ]; then
    echo "容器运行正常！"
    # 测试接口
    HTTP_CODE=$(curl -s -o /dev/null -w '%{http_code}' http://localhost:${HOST_PORT}/ 2>/dev/null || echo "000")
    echo "HTTP状态码: ${HTTP_CODE}"
    if [ "${HTTP_CODE}" = "200" ] || [ "${HTTP_CODE}" = "404" ] || [ "${HTTP_CODE}" = "401" ]; then
        echo "=== 部署成功！ ==="
        echo "访问地址: http://49.235.161.106:${HOST_PORT}"
        echo "容器名称: ${CONTAINER_NAME}"
        echo "镜像名称: ${IMAGE_NAME}"
    else
        echo "服务测试失败，HTTP码: ${HTTP_CODE}"
        docker logs ${CONTAINER_NAME}
        exit 1
    fi
else
    echo "容器启动失败！"
    docker logs ${CONTAINER_NAME}
    exit 1
fi
