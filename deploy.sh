#!/bin/bash
# Personal Account Book Deployment Script

SERVER_IP="49.235.161.106"
SERVER_PORT=22
USERNAME="root"
REMOTE_DIR="/opt/apps/account-book"
HOST_PORT=10013
CONTAINER_PORT=8080
CONTAINER_NAME="account-book"
IMAGE_NAME="account-book:latest"

echo "========== Starting Deployment =========="
echo "Server: ${SERVER_IP}:${SERVER_PORT}"
echo "Remote Directory: $REMOTE_DIR"
echo "Host Port: $HOST_PORT"

# Phase 1: Check Docker on server
echo "========== Phase 1: Server Pre-check =========="
echo "Checking Docker status..."
DOCKER_VERSION=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no -o ConnectTimeout=10 "$USERNAME@$SERVER_IP" "docker version --format '{{.Server.Version}}' 2>/dev/null")
if [ $? -eq 0 ]; then
    echo "[SUCCESS] Docker is running, version: $DOCKER_VERSION"
else
    echo "[ERROR] Docker check failed"
    exit 1
fi

# Check and create remote directory
echo "Checking remote directory $REMOTE_DIR..."
ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "mkdir -p $REMOTE_DIR && chmod 755 $REMOTE_DIR"
echo "[SUCCESS] Directory ready"

# Check port 10013
echo "Checking port $HOST_PORT usage..."
PORT_CHECK=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "
non_container_pid=\$(netstat -tlnp 2>/dev/null | grep ':$HOST_PORT ' | grep -v 'docker-proxy' | awk '{print \$7}' | cut -d'/' -f1 | head -1)
if [ -n \"\$non_container_pid\" ] && [ \"\$non_container_pid\" != \"-\" ]; then
    echo \"NON_CONTAINER:\$(cat /proc/\$non_container_pid/comm 2>/dev/null || echo 'unknown')\"
    exit 0
fi
container_id=\$(docker ps -q --filter \"publish=$HOST_PORT\" 2>/dev/null)
if [ -n \"\$container_id\" ]; then
    container_name=\$(docker inspect --format='{{.Name}}' \$container_id | sed 's/\///')
    echo \"CONTAINER:\$container_name:\$container_id\"
else
    echo \"FREE\"
fi
")
echo "Port check result: $PORT_CHECK"

if [[ "$PORT_CHECK" == NON_CONTAINER:* ]]; then
    PROCESS_NAME=$(echo "$PORT_CHECK" | cut -d':' -f2)
    echo "[ERROR] Port $HOST_PORT is occupied by non-container process: $PROCESS_NAME. Deployment failed!"
    exit 1
fi

if [[ "$PORT_CHECK" == CONTAINER:* ]]; then
    EXISTING_CONTAINER=$(echo "$PORT_CHECK" | cut -d':' -f2)
    EXISTING_CONTAINER_ID=$(echo "$PORT_CHECK" | cut -d':' -f3)
    echo "[WARN] Port $HOST_PORT is occupied by container $EXISTING_CONTAINER, stopping and removing..."
    ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker stop $EXISTING_CONTAINER_ID && docker rm $EXISTING_CONTAINER_ID"
    echo "[SUCCESS] Container $EXISTING_CONTAINER stopped and removed"
fi

# Check and remove existing container with same name
echo "Checking for existing container $CONTAINER_NAME..."
EXISTING_CONTAINER_ID=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker ps -aq --filter name=^/$CONTAINER_NAME\$ 2>/dev/null")
if [ -n "$EXISTING_CONTAINER_ID" ]; then
    echo "[WARN] Found existing container, removing..."
    ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker rm -f $CONTAINER_NAME 2>/dev/null"
    echo "[SUCCESS] Existing container removed"
fi

# Phase 2: Clean old files and images
echo "========== Phase 2: Clean old files and images =========="
echo "Cleaning old files..."
ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "rm -f $REMOTE_DIR/*.jar $REMOTE_DIR/Dockerfile 2>/dev/null; echo 'Cleaned'"

echo "Checking old images..."
OLD_IMAGE_ID=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker images --filter reference=$IMAGE_NAME --format '{{.ID}}' 2>/dev/null")
if [ -n "$OLD_IMAGE_ID" ]; then
    echo "[WARN] Found old image, removing..."
    ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker rmi -f $IMAGE_NAME 2>/dev/null; echo 'Image removed'"
    echo "[SUCCESS] Old image removed"
fi

# Phase 3: Upload files
echo "========== Phase 3: Upload files =========="
echo "Uploading JAR file..."
scp -P $SERVER_PORT -o StrictHostKeyChecking=no "target/personal-account-book-1.0.0.jar" "$USERNAME@$SERVER_IP:$REMOTE_DIR/"
echo "[SUCCESS] JAR file uploaded"

echo "Uploading Dockerfile..."
scp -P $SERVER_PORT -o StrictHostKeyChecking=no "Dockerfile" "$USERNAME@$SERVER_IP:$REMOTE_DIR/"
echo "[SUCCESS] Dockerfile uploaded"

# Phase 4: Build image and start container
echo "========== Phase 4: Build image and start container =========="
echo "Building Docker image..."
BUILD_RESULT=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "cd $REMOTE_DIR && docker build -t $IMAGE_NAME . 2>&1")
if [ $? -ne 0 ]; then
    echo "[ERROR] Build failed: $BUILD_RESULT"
    exit 1
fi
echo "[SUCCESS] Image built successfully"

echo "Starting container..."
RUN_RESULT=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker run -d --name $CONTAINER_NAME -p $HOST_PORT:$CONTAINER_PORT -v $REMOTE_DIR/data:/app/data --restart unless-stopped $IMAGE_NAME 2>&1")
if [ $? -ne 0 ]; then
    echo "[ERROR] Container start failed: $RUN_RESULT"
    exit 1
fi

sleep 3
CONTAINER_STATUS=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker ps --filter name=$CONTAINER_NAME --format '{{.Status}}'")
if [ -n "$CONTAINER_STATUS" ]; then
    echo "[SUCCESS] Container started, status: $CONTAINER_STATUS"
else
    echo "[ERROR] Container failed to start"
    LOGS=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker logs $CONTAINER_NAME 2>&1")
    echo "Container logs: $LOGS"
    exit 1
fi

# Phase 5: Health check
echo "========== Phase 5: Health Check =========="
echo "Waiting 15 seconds for application to start..."
sleep 15

HEALTH_URL="http://${SERVER_IP}:${HOST_PORT}/api/users"
echo "Testing API: $HEALTH_URL"

HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "$HEALTH_URL" 2>/dev/null)
if [ "$HTTP_CODE" == "200" ]; then
    echo "[SUCCESS] API returned status: $HTTP_CODE"
    echo ""
    echo "========== Deployment Successful! =========="
    echo "[SUCCESS] API Endpoint: http://${SERVER_IP}:${HOST_PORT}"
    echo "[SUCCESS] Frontend Page: http://${SERVER_IP}:${HOST_PORT}/index.html"
    echo ""
    echo "========== Test Cases =========="
    echo "1. User Registration:"
    echo "   curl -X POST http://${SERVER_IP}:${HOST_PORT}/api/users/register -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"123456\"}'"
    echo ""
    echo "2. User Login:"
    echo "   curl -X POST http://${SERVER_IP}:${HOST_PORT}/api/users/login -H 'Content-Type: application/json' -d '{\"username\":\"testuser\",\"password\":\"123456\"}'"
    echo ""
    echo "3. Get User List:"
    echo "   curl http://${SERVER_IP}:${HOST_PORT}/api/users"
    echo ""
    echo "4. Access Frontend:"
    echo "   http://${SERVER_IP}:${HOST_PORT}/index.html"
    echo ""
    echo "5. View Container Logs:"
    echo "   ssh -p $SERVER_PORT $USERNAME@$SERVER_IP 'docker logs $CONTAINER_NAME -f'"
else
    echo "[ERROR] Health check failed, HTTP code: $HTTP_CODE"
    LOGS=$(ssh -p $SERVER_PORT -o StrictHostKeyChecking=no "$USERNAME@$SERVER_IP" "docker logs $CONTAINER_NAME --tail 50 2>&1")
    echo "Container logs: $LOGS"
fi
