# ApiGrab 基于 docker-compose 的一键部署

**要求**：准备一台能够安装 docker 的 Linux 操作系统。

## 一、安装 docker 及 docker-compose

```
[root@localhost install]# ls -l
总用量 12
-rw-r--r-- 1 root root 625 2月  11 22:24 docker-compose.yml
-rw-r--r-- 1 root root 378 2月  11 22:38 README.md
```

## 二、一键部署 ApiGrab

```
[root@localhost install]# docker-compose up -d
```

启动后访问 `http://<主机IP>:5880`，默认管理员账号 `admin / 123456`。

## 三、常用操作命令

- 1. 一键创建并启动容器服务：
```
docker-compose up -d
```

- 2. 销毁容器服务：
```
docker-compose down
```

- 3. 停止容器：
```
docker-compose stop
```

- 4. 重启容器：
```
docker-compose restart
```

- 5. 启动容器：
```
docker-compose start
```

## 四、配置项

应用容器通过环境变量注入元数据库连接（见 `docker-compose.yml`）：

