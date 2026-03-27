#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
自动化部署脚本
功能：打包Java项目、创建Docker镜像、部署到远程服务器
"""
import subprocess
import sys
import time
import os

# 配置项
SERVER = "49.235.161.106"
PORT = 22
USER = "root"
APP_DIR = "/opt/apps/account-book"
JAR_NAME = "personal-account-book-1.0.0.jar"
CONTAINER_NAME = "account-book-app"
IMAGE_NAME = "account-book:latest"
HOST_PORT = 10011
CONTAINER_PORT = 8080

def run_command(cmd, shell=True, cwd=None):
    """执行命令并返回结果"""
    try:
        result = subprocess.run(cmd, shell=shell, cwd=cwd, capture_output=True, text=True)
        return result.returncode, result.stdout.strip(), result.stderr.strip()
    except Exception as e:
        return -1, "", str(e)

def print_step(step, message):
    """打印步骤信息"""
    print(f"\n\033[1;33m[{step}] {message}\033[0m")

def print_success(message):
    """打印成功信息"""
    print(f"\033[1;32m{message}\033[0m")

def print_error(message):
    """打印错误信息"""
    print(f"\033[1;31m{message}\033[0m")

def print_info(message):
    """打印普通信息"""
    print(f"\033[1;36m{message}\033[0m")

def ssh_command(cmd):
    """通过SSH执行远程命令"""
    ssh_cmd = f'ssh -p {PORT} {USER}@{SERVER} "{cmd}"'
    return run_command(ssh_cmd)

def main():
    print_info("=" * 50)
    print_info("开始自动化部署")
    print_info("=" * 50)

    # 步骤1: 打包项目
    print_step("1/6", "开始打包项目...")
    code, out, err = run_command("mvn clean package -DskipTests")
    if code != 0:
        print_error(f"打包失败！{err}")
        sys.exit(1)
    print_success("打包成功！")

    # 步骤2: 创建Dockerfile
    print_step("2/6", "创建Dockerfile...")
    dockerfile_content = f'''FROM openjdk:8-jdk-alpine
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone
WORKDIR /app
COPY {JAR_NAME} app.jar
ENV JAVA_OPTS="-Xms256m -Xmx512m"
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
EXPOSE {CONTAINER_PORT}
'''
    target_dir = "target"
    if not os.path.exists(target_dir):
        os.makedirs(target_dir)
    
    with open(os.path.join(target_dir, "Dockerfile"), "w", encoding="utf-8") as f:
        f.write(dockerfile_content)
    print_success("Dockerfile创建成功！")

    # 步骤3: 服务器前置检查
    print_step("3/6", "服务器前置检查...")
    
    # 检查Docker状态
    print("检查Docker状态...")
    code, out, err = ssh_command("systemctl is-active docker")
    if out.strip() != "active":
        print("Docker未运行，尝试启动...")
        code, out, err = ssh_command("systemctl start docker")
        if code != 0:
            print_error(f"Docker启动失败！{err}")
            sys.exit(1)
    print_success("Docker运行正常！")

    # 检查并创建目录
    print("检查目录权限...")
    code, out, err = ssh_command(f"mkdir -p {APP_DIR}; chmod 755 {APP_DIR}")
    if code != 0:
        print_error(f"目录创建失败！{err}")
    print_success("目录权限检查完成！")

    # 检查端口占用
    print(f"检查端口 {HOST_PORT} 占用...")
    code, out, err = ssh_command(f"lsof -i :{HOST_PORT} 2>/dev/null | grep -v PID | wc -l")
    try:
        port_count = int(out.strip())
    except:
        port_count = 0
    
    if port_count > 0:
        code, out, err = ssh_command(f"docker ps --filter 'publish={HOST_PORT}' --format '{{.Names}}' 2>/dev/null | wc -l")
        try:
            container_count = int(out.strip())
        except:
            container_count = 0
        
        if container_count > 0:
            print("端口被容器占用，停止并删除相关容器...")
            ssh_command(f"docker ps --filter 'publish={HOST_PORT}' --format '{{.Names}}' | xargs -r docker stop")
            ssh_command(f"docker ps -a --filter 'publish={HOST_PORT}' --format '{{.Names}}' | xargs -r docker rm")
        else:
            print_error("端口被非容器进程占用，部署失败！")
            sys.exit(1)
    print_success("端口检查完成！")

    # 步骤4: 清理旧资源并上传文件
    print_step("4/6", "清理旧资源并上传文件...")
    
    # 清理旧文件
    print("清理服务器旧文件...")
    ssh_command(f"rm -rf {APP_DIR}/*")

    # 清理旧容器和镜像
    print("清理旧容器和镜像...")
    ssh_command(f"docker ps -a --filter 'name={CONTAINER_NAME}' --format '{{.Names}}' | xargs -r docker stop 2>/dev/null")
    ssh_command(f"docker ps -a --filter 'name={CONTAINER_NAME}' --format '{{.Names}}' | xargs -r docker rm 2>/dev/null")
    ssh_command(f"docker images --filter 'reference={IMAGE_NAME}' --format '{{.Repository}}:{{.Tag}}' | xargs -r docker rmi 2>/dev/null")

    # 上传文件
    print("上传文件到服务器...")
    scp_cmd = f'scp -P {PORT} target/{JAR_NAME} target/Dockerfile {USER}@{SERVER}:{APP_DIR}/'
    code, out, err = run_command(scp_cmd)
    if code != 0:
        print_error(f"文件上传失败！{err}")
        sys.exit(1)
    print_success("文件上传成功！")

    # 步骤5: 构建镜像并启动容器
    print_step("5/6", "构建镜像并启动容器...")
    
    # 构建镜像
    print("构建Docker镜像...")
    code, out, err = ssh_command(f"cd {APP_DIR}; docker build -t {IMAGE_NAME} .")
    if code != 0:
        print_error(f"镜像构建失败！{err}")
        sys.exit(1)
    print_success("镜像构建成功！")

    # 启动容器
    print("启动容器...")
    code, out, err = ssh_command(f"docker run -d --name {CONTAINER_NAME} -p {HOST_PORT}:{CONTAINER_PORT} -v {APP_DIR}/data:/app/data {IMAGE_NAME}")
    if code != 0:
        print_error(f"容器启动失败！{err}")
        sys.exit(1)
    print_success("容器启动成功！")

    # 步骤6: 服务校验
    print_step("6/6", "服务校验...")
    print("等待15秒让服务启动...")
    time.sleep(15)

    # 检查容器状态
    code, out, err = ssh_command(f"docker inspect --format '{{{{.State.Running}}}}' {CONTAINER_NAME}")
    if out.strip().lower() != "true":
        print_error("容器未正常运行！")
        ssh_command(f"docker logs {CONTAINER_NAME}")
        sys.exit(1)

    # 测试接口连通性
    print("测试接口连通性...")
    code, out, err = ssh_command(f"curl -s -o /dev/null -w '%{{http_code}}' http://localhost:{HOST_PORT}/ 2>/dev/null")
    status_code = out.strip()
    if status_code in ["200", "404", "401"]:
        print_success("服务接口连通性测试通过！")
        print_info("\n" + "=" * 50)
        print_success("部署成功！")
        print_info("=" * 50)
        print_info(f"\n访问地址：http://{SERVER}:{HOST_PORT}")
        print_info(f"容器名称：{CONTAINER_NAME}")
        print_info(f"镜像名称：{IMAGE_NAME}")
    else:
        print_error(f"服务接口连通性测试失败，HTTP状态码: {status_code}")
        print("容器日志：")
        ssh_command(f"docker logs {CONTAINER_NAME}")
        sys.exit(1)

    print_info("\n" + "=" * 50)
    print_success("部署完成！")
    print_info("=" * 50 + "\n")

if __name__ == "__main__":
    main()
