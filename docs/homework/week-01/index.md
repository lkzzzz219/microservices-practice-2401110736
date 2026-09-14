```
## 环境检查
下面是各开发工具版本信息，通过终端命令查询：
​```
java -version
mvn --version
git --version
docker version
docker compose version
​```
### 命令输出结果
PS C:\Users\linkunze> java --version
openjdk 25.0.4.1 2026-08-18 LTS
OpenJDK Runtime Environment Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS)
OpenJDK 64-Bit Server VM Temurin-25.0.4.1+1 (build 25.0.4.1+1-LTS, mixed mode, sharing)
PS C:\Users\linkunze> mvn --version
Apache Maven 3.9.16 (2bdd9fddda4b155ebf8000e807eb73fd829a51d5)
Maven home: E:\e\apache-maven-3.9.16-bin\apache-maven-3.9.16
Java version: 25.0.4.1, vendor: Eclipse Adoptium, runtime: C:\Program Files\Eclipse Adoptium\jdk-25.0.4.101-hotspot
Default locale: zh_CN, platform encoding: UTF-8
OS name: "windows 11", version: "10.0", arch: "amd64", family: "windows"
PS C:\Users\linkunze> git --version
git version 2.55.0.windows.5
PS C:\Users\linkunze> docker version
Client:
 Version:           29.7.2
 API version:       1.55
 Go version:        go1.26.5
 Git commit:        a7dcaa6
 Built:             Wed Aug  5 18:31:33 2026
 OS/Arch:           windows/amd64
 Context:           desktop-linux

Server: Docker Desktop 4.90.0 (238679)
 Engine:
  Version:          29.7.2
  API version:      1.55 (minimum version 1.40)
  Go version:       go1.26.5
  Git commit:       6a43e3d
  Built:            Wed Aug  5 18:28:36 2026
  OS/Arch:          linux/amd64
  Experimental:     false
 containerd:
  Version:          v2.3.3
  GitCommit:        aad11006b869517fcd3009450b6f82da282e1a9b
 runc:
  Version:          1.4.3
  GitCommit:        v1.4.3-0-gbb14dabe
 docker-init:
  Version:          0.19.0
  GitCommit:        de40ad0
PS C:\Users\linkunze> docker compose version
Docker Compose version v5.5.1

## 概念回答
### 什么是微服务架构？
微服务架构将大型应用按照业务能力拆分成若干个小型独立服务。每个服务运行在独立进程，通过HTTP接口完成互相调用，支持独立部署、独立迭代，交由不同小团队分别维护。

### 微服务和单体架构的主要区别是什么？
单体架构把全部业务逻辑放在同一个项目包内，组件耦合度高，共用数据库，每次修改需要整体重新发布，适合小型项目。
微服务按业务拆分，分布式部署，各个服务可拥有独立数据库，允许使用不同技术栈，支持单独扩容发布；但会带来网络通信、分布式数据一致性等额外复杂度，适合大型多团队项目。

### 为什么本课程先实现单体系统，再逐步拆分为微服务？
如果直接开发微服务，会被大量分布式相关问题干扰，难以理解业务本身。先完成单体系统，可以跑通完整业务流程，切身感受到单体架构耦合严重、发布繁琐、扩展受限的痛点，才能真正理解微服务拆分的目的与价值。

### 为什么作业需要提供可重复运行的测试或验证脚本？
消除不同计算机之间的环境差异，保证实验结果能够复现；方便老师校验作业功能；后续修改代码时，也可以快速验证原有功能是否遭到破坏。


## 问题记录
1. 最初本机只有JRE8运行环境，缺少JDK开发包，`javac`命令无法识别；后续安装Eclipse Temurin JDK25，配置系统环境变量后Java开发环境恢复正常。
2. Maven解压完成后，一开始环境变量配置未生效，提示命令找不到；核对路径、重启终端后Maven正常可用。
3. Docker Desktop一开始提示`Virtualization support not detected`，wsl查看`docker‑desktop`状态为`Stopped`；开启BIOS虚拟化、调整Windows内核隔离设置之后Docker完整运行。
4. 课件中大量`sudo、apt、wget、nano、source`均为Ubuntu‑Linux命令，Windows系统不能直接复制执行，Windows需要图形界面配置系统环境变量。

下一步计划：后续继续熟悉Docker、Maven的常用命令，巩固区分Windows与Linux终端命令。

```