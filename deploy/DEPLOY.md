# 阿里云 Ubuntu 部署指南

本文档按步骤将本项目部署到阿里云 ECS（Ubuntu 22.04/24.04）。  
架构：**Nginx（80）→ 前端静态文件 + 反向代理 `/api` → Spring Boot（127.0.0.1:8080）→ PostgreSQL**。

---

## 第 0 步：准备

- 阿里云 ECS：建议 **2核 4G**，系统盘 40G+
- 安全组放行：**80**（必选）、**443**（HTTPS 可选）、**22**（SSH）
- **不要**对公网开放 8080、5432

本地代码已包含：

| 文件 | 作用 |
|------|------|
| `application-prod.yml` | 生产配置，从环境变量读数据库密码 |
| `deploy/env.example` | 服务器环境变量模板 |
| `deploy/nginx-poker.conf` | Nginx 配置模板 |
| `deploy/poker-backend.service` | systemd 服务模板 |

---

## 第 1 步：购买并登录 ECS

```bash
ssh root@<你的公网IP>
```

建议创建专用用户（可选）：

```bash
adduser poker
usermod -aG sudo poker
```

---

## 第 2 步：安装依赖

```bash
sudo apt update
sudo apt install -y openjdk-17-jdk maven nginx postgresql postgresql-contrib git
java -version   # 应显示 17
```

---

## 第 3 步：配置 PostgreSQL

```bash
sudo -u postgres psql
```

在 psql 里执行：

```sql
CREATE USER poker WITH PASSWORD '你的强密码';
CREATE DATABASE poker_db OWNER poker;
GRANT ALL PRIVILEGES ON DATABASE poker_db TO poker;
\q
```

若使用 **阿里云 RDS PostgreSQL**，在 RDS 控制台建库建用户，记下内网地址，后面填到 `DB_URL`。

---

## 第 4 步：拉取代码并构建

```bash
sudo mkdir -p /opt/poker
sudo chown $USER:$USER /opt/poker
cd /opt/poker

git clone https://github.com/tcbvilla/texas-holdem-poker.git .
# 或 scp/rsync 上传

# 后端 jar
cd backend
mvn -DskipTests package
cp target/poker-system-0.0.1-SNAPSHOT.jar /opt/poker/poker-system.jar

# 前端静态资源
cd ../frontend
npm install
npm run build
# 输出在 frontend/build/
```

---

## 第 5 步：配置生产环境变量

```bash
cp /opt/poker/deploy/env.example /opt/poker/.env
chmod 600 /opt/poker/.env
nano /opt/poker/.env
```

至少修改：

```bash
DB_PASSWORD=你的强密码
# 若用 RDS：
# DB_URL=jdbc:postgresql://pg-xxx.pg.rds.aliyuncs.com:5432/poker_db
```

---

## 第 6 步：配置 systemd 后端服务

```bash
# 若用专用用户 poker，先授权目录
sudo chown -R poker:poker /opt/poker

sudo cp /opt/poker/deploy/poker-backend.service /etc/systemd/system/
sudo systemctl daemon-reload
sudo systemctl enable poker-backend
sudo systemctl start poker-backend
sudo systemctl status poker-backend
```

首次启动会自动跑 Liquibase 建表。查看日志：

```bash
journalctl -u poker-backend -f
```

---

## 第 7 步：配置 Nginx

```bash
sudo cp /opt/poker/deploy/nginx-poker.conf /etc/nginx/sites-available/poker
sudo nano /etc/nginx/sites-available/poker
# 将 YOUR_DOMAIN_OR_IP 改成公网 IP 或域名

sudo ln -sf /etc/nginx/sites-available/poker /etc/nginx/sites-enabled/
sudo rm -f /etc/nginx/sites-enabled/default   # 可选，避免冲突
sudo nginx -t
sudo systemctl reload nginx
```

浏览器访问：`http://<公网IP>/`

---

## 第 8 步：验证

1. 打开首页，注册/登录
2. 创建俱乐部、房间，双浏览器（或无痕）测试对战
3. 后端健康：`curl -s http://127.0.0.1:8080/api/public/clubs`（若有公开接口）

---

## 更新发布（以后改代码）

```bash
cd /opt/poker
git pull

cd backend && mvn -DskipTests package
cp target/poker-system-0.0.1-SNAPSHOT.jar /opt/poker/poker-system.jar
sudo systemctl restart poker-backend

cd ../frontend && npm install && npm run build
sudo systemctl reload nginx
```

---

## 注意事项

1. **牌局与 Token 在内存**：重启 `poker-backend` 会清空进行中牌局，用户需重新登录。
2. **单机部署**：不要对 8080 做多台负载均衡，除非以后做 Redis/持久化改造。
3. **HTTPS**：可在阿里云申请免费证书，或 `certbot` 配置到 Nginx 443。
4. **本地开发**不受影响：默认 `SPRING_PROFILES_ACTIVE=dev`，仍连本机 PostgreSQL。

---

## 故障排查

| 现象 | 检查 |
|------|------|
| 502 Bad Gateway | `systemctl status poker-backend`，`journalctl -u poker-backend` |
| 数据库连不上 | `.env` 中 `DB_URL`、密码；RDS 白名单是否含 ECS 内网 IP |
| 页面空白 | `ls /opt/poker/frontend/build`；Nginx `root` 路径是否正确 |
| API 403/401 | 浏览器 F12 看 Network；Token 是否带在 Authorization 头 |
