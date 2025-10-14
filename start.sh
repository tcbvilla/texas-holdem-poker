#!/bin/bash

echo "🃏 启动德州扑克系统..."

# 检查MySQL是否运行
echo "检查MySQL服务..."
if ! pgrep -x "mysqld" > /dev/null; then
    echo "❌ MySQL服务未运行，请先启动MySQL"
    exit 1
fi

# 初始化数据库
echo "初始化数据库..."
mysql -u root -p < database/init.sql

# 启动后端
echo "启动后端服务..."
cd backend
mvn spring-boot:run &
BACKEND_PID=$!

# 等待后端启动
echo "等待后端启动..."
sleep 10

# 启动前端
echo "启动前端服务..."
cd ../frontend
npm install
npm start &
FRONTEND_PID=$!

echo "✅ 系统启动完成！"
echo "后端: http://localhost:8080"
echo "前端: http://localhost:3000"

# 等待用户中断
trap "echo '停止服务...'; kill $BACKEND_PID $FRONTEND_PID; exit" INT
wait
