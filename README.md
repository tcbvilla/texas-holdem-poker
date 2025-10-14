# 德州扑克系统 (Texas Hold'em Poker System)

一个基于Spring Boot + React.js的德州扑克游戏系统基础框架。

## 技术栈

### 后端
- Java 17
- Spring Boot 3.2.0
- Spring Data JPA
- MySQL 8.0
- Liquibase (数据库迁移)
- Maven

### 前端
- React 18
- CSS3

## 项目结构

```
poker project/
├── backend/                 # Java后端
│   ├── src/main/java/com/poker/
│   │   └── PokerSystemApplication.java
│   ├── src/main/resources/
│   │   ├── application.yml
│   │   └── db/changelog/
│   └── pom.xml
├── frontend/               # React前端
│   ├── public/
│   ├── src/
│   │   ├── App.js
│   │   └── index.js
│   └── package.json
├── database/              # 数据库脚本
│   └── init.sql
└── README.md
```

## 快速开始

### 1. 数据库设置

确保MySQL服务正在运行，然后执行：

```bash
mysql -u root -p < database/init.sql
```

### 2. 启动后端

```bash
cd backend
mvn spring-boot:run
```

后端将在 http://localhost:8080 启动

### 3. 启动前端

```bash
cd frontend
npm install
npm start
```

前端将在 http://localhost:3000 启动

## 开发说明

1. 后端使用Liquibase进行数据库版本管理
2. 数据库连接配置在 `application.yml` 中
3. 项目已初始化基本结构，等待功能开发
