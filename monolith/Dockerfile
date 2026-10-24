# ============================================================
# CampusTrade 单体应用 —— 多阶段构建
# 构建阶段使用 Maven 镜像完成编译打包，运行阶段仅包含 JRE，
# 最终镜像不需要本机安装 Maven 或 Java。
# 在仓库根目录执行：docker build -t campustrade:week05 ./monolith
# ============================================================

# ---------- 阶段一：构建 ----------
FROM maven:3.9-eclipse-temurin-25 AS build
WORKDIR /app

# 先只拷贝 pom 与镜像仓库配置，单独解析依赖以利用镜像层缓存
COPY pom.xml settings.xml ./
RUN mvn -s ./settings.xml -q -DskipTests dependency:go-offline

# 再拷贝源码并打包（构建镜像时跳过测试，测试在 CI/本地已验证）
COPY src ./src
RUN mvn -s ./settings.xml -q -DskipTests package

# ---------- 阶段二：运行 ----------
FROM eclipse-temurin:25-jre
WORKDIR /app

# 只从构建阶段拷贝可运行 JAR，源码与 Maven 缓存不进入最终镜像
COPY --from=build /app/target/campustrade-monolith-0.0.1-SNAPSHOT.jar app.jar

EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
