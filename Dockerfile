FROM openjdk:8-jre-slim

# 设置时区为 Asia/Shanghai
ENV TZ=Asia/Shanghai
RUN ln -snf /usr/share/zoneinfo/$TZ /etc/localtime && echo $TZ > /etc/timezone

# 设置工作目录
WORKDIR /app

# 复制 JAR 文件
COPY personal-account-book-1.0.0.jar app.jar

# 创建数据目录
RUN mkdir -p /app/data

# 暴露端口
EXPOSE 8080

# JVM 参数: -Xms256m -Xmx512m
ENV JAVA_OPTS="-Xms256m -Xmx512m"

# 使用 ENTRYPOINT 启动
ENTRYPOINT ["sh", "-c", "java $JAVA_OPTS -jar app.jar"]
