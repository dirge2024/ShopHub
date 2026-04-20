@echo off
setlocal
chcp 65001 >nul
title ShopHub Start

echo [1/3] Starting ZooKeeper...
if not exist "D:\kafka\kafka_2.13-3.5.1\zookeeper-data" mkdir "D:\kafka\kafka_2.13-3.5.1\zookeeper-data"
start "ZooKeeper" powershell -NoExit -ExecutionPolicy Bypass -Command "Set-Location 'D:\kafka\kafka_2.13-3.5.1'; .\bin\windows\zookeeper-server-start.bat .\config\zookeeper.properties"
timeout /t 10 /nobreak >nul

echo [2/3] Starting Kafka...
start "Kafka" powershell -NoExit -ExecutionPolicy Bypass -Command "Set-Location 'D:\kafka\kafka_2.13-3.5.1'; .\bin\windows\kafka-server-start.bat .\config\server.properties"
timeout /t 12 /nobreak >nul

echo [3/3] Starting Nginx...
start "Nginx" cmd /c "cd /d D:\Java_project\market\nginx-1.18.0\nginx-1.18.0 && nginx.exe"

echo.
echo ZooKeeper, Kafka, and Nginx have been launched.
echo Frontend: http://localhost:8080
echo Start ShopHub and consultant in your IDE if needed.
echo.
pause
endlocal
