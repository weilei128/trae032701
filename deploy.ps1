# 自动化部署脚本 - PowerShell 5兼容版本
# 配置项
$SERVER = "49.235.161.106"
$PORT = 22
$USER = "root"
$APP_DIR = "/opt/apps/account-book"
$JAR_NAME = "personal-account-book-1.0.0.jar"
$CONTAINER_NAME = "account-book-app"
$IMAGE_NAME = "account-book:latest"
$HOST_PORT = 10011
$CONTAINER_PORT = 8080

Write-Host "=== 开始自动化部署 ===" -ForegroundColor Cyan

# 步骤1: 打包项目
Write-Host "`n[1/6] 开始打包项目..." -ForegroundColor Yellow
mvn clean package -DskipTests
if ($LASTEXITCODE -ne 0) {
    Write-Host "打包失败！" -ForegroundColor Red
    exit 1
}
Write-Host "打包成功！" -ForegroundColor Green

# 步骤2: 创建Dockerfile
Write-Host "`n[2/6] 创建Dockerfile..." -ForegroundColor Yellow
$dockerfileContent = @"
FROM openjdk:8-jdk-alpine
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/\$TZ /etc/localtime && echo \$TZ > /etc/timezone
WORKDIR /app
COPY $JAR_NAME app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java \$JAVA_OPTS -jar app.jar"]
EXPOSE $CONTAINER_PORT
"@
$dockerfileContent | Out-File -FilePath "target\Dockerfile" -Encoding utf8
Write-Host "Dockerfile创建成功！" -ForegroundColor Green

# 步骤3: 服务器前置检查
Write-Host "`n[3/6] 服务器前置检查..." -ForegroundColor Yellow

# 检查Docker状态
Write-Host "检查Docker状态..."
$dockerStatus = ssh -p $PORT $USER@$SERVER "systemctl is-active docker"
if ($dockerStatus -ne "active") {
    Write-Host "Docker未运行，尝试启动..."
    ssh -p $PORT $USER@$SERVER "systemctl start docker"
    if ($LASTEXITCODE -ne 0) {
        Write-Host "Docker启动失败！" -ForegroundColor Red
        exit 1
    }
}
Write-Host "Docker运行正常！" -ForegroundColor Green

# 检查并创建目录
Write-Host "检查目录权限..."
ssh -p $PORT $USER@$SERVER "mkdir -p $APP_DIR; chmod 755 $APP_DIR"
Write-Host "目录权限检查完成！" -ForegroundColor Green

# 检查端口占用
Write-Host "检查端口 $HOST_PORT 占用..."
$portCheck = ssh -p $PORT $USER@$SERVER "lsof -i :$HOST_PORT 2>/dev/null | grep -v PID | wc -l"
if ($portCheck -gt 0) {
    $containerCheck = ssh -p $PORT $USER@$SERVER "docker ps --filter 'publish=$HOST_PORT' --format '{{.Names}}' 2>/dev/null | wc -l"
    if ($containerCheck -gt 0) {
        Write-Host "端口被容器占用，停止并删除相关容器..."
        ssh -p $PORT $USER@$SERVER "docker ps --filter 'publish=$HOST_PORT' --format '{{.Names}}' | xargs -r docker stop; docker ps -a --filter 'publish=$HOST_PORT' --format '{{.Names}}' | xargs -r docker rm"
    } else {
        Write-Host "端口被非容器进程占用，部署失败！" -ForegroundColor Red
        exit 1
    }
}
Write-Host "端口检查完成！" -ForegroundColor Green

# 步骤4: 清理旧资源并上传文件
Write-Host "`n[4/6] 清理旧资源并上传文件..." -ForegroundColor Yellow

# 清理旧文件
Write-Host "清理服务器旧文件..."
ssh -p $PORT $USER@$SERVER "rm -rf $APP_DIR/*"

# 清理旧容器和镜像
Write-Host "清理旧容器和镜像..."
ssh -p $PORT $USER@$SERVER "docker ps -a --filter 'name=$CONTAINER_NAME' --format '{{.Names}}' | xargs -r docker stop 2>/dev/null; docker ps -a --filter 'name=$CONTAINER_NAME' --format '{{.Names}}' | xargs -r docker rm 2>/dev/null; docker images --filter 'reference=$IMAGE_NAME' --format '{{.Repository}}:{{.Tag}}' | xargs -r docker rmi 2>/dev/null"

# 上传文件
Write-Host "上传文件到服务器..."
scp -P $PORT target\$JAR_NAME target\Dockerfile $USER@$SERVER`:$APP_DIR/
if ($LASTEXITCODE -ne 0) {
    Write-Host "文件上传失败！" -ForegroundColor Red
    exit 1
}
Write-Host "文件上传成功！" -ForegroundColor Green

# 步骤5: 构建镜像并启动容器
Write-Host "`n[5/6] 构建镜像并启动容器..." -ForegroundColor Yellow

# 构建镜像
Write-Host "构建Docker镜像..."
ssh -p $PORT $USER@$SERVER "cd $APP_DIR; docker build -t $IMAGE_NAME ."
if ($LASTEXITCODE -ne 0) {
    Write-Host "镜像构建失败！" -ForegroundColor Red
    exit 1
}
Write-Host "镜像构建成功！" -ForegroundColor Green

# 启动容器
Write-Host "启动容器..."
ssh -p $PORT $USER@$SERVER "docker run -d --name $CONTAINER_NAME -p $HOST_PORT:$CONTAINER_PORT -v $APP_DIR/data:/app/data $IMAGE_NAME"
if ($LASTEXITCODE -ne 0) {
    Write-Host "容器启动失败！" -ForegroundColor Red
    exit 1
}
Write-Host "容器启动成功！" -ForegroundColor Green

# 步骤6: 服务校验
Write-Host "`n[6/6] 服务校验..." -ForegroundColor Yellow
Write-Host "等待15秒让服务启动..."
Start-Sleep -Seconds 15

# 检查容器状态
$containerStatus = ssh -p $PORT $USER@$SERVER "docker inspect --format '{{.State.Running}}' $CONTAINER_NAME"
if ($containerStatus -ne "true") {
    Write-Host "容器未正常运行！" -ForegroundColor Red
    ssh -p $PORT $USER@$SERVER "docker logs $CONTAINER_NAME"
    exit 1
}

# 测试接口连通性
Write-Host "测试接口连通性..."
$testResult = ssh -p $PORT $USER@$SERVER "curl -s -o /dev/null -w '%{http_code}' http://localhost:$HOST_PORT/ 2>/dev/null || echo '000'"
if ($testResult -eq "200" -or $testResult -eq "404") {
    Write-Host "服务接口连通性测试通过！" -ForegroundColor Green
    Write-Host "`n=== 部署成功！ ===" -ForegroundColor Green
    Write-Host "`n访问地址：http://$SERVER:$HOST_PORT" -ForegroundColor Cyan
    Write-Host "容器名称：$CONTAINER_NAME" -ForegroundColor Cyan
    Write-Host "镜像名称：$IMAGE_NAME" -ForegroundColor Cyan
} else {
    Write-Host "服务接口连通性测试失败，HTTP状态码: $testResult" -ForegroundColor Red
    Write-Host "容器日志："
    ssh -p $PORT $USER@$SERVER "docker logs $CONTAINER_NAME"
    exit 1
}

Write-Host "`n=== 部署完成 ===" -ForegroundColor Green
