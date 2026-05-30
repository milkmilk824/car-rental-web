# DrivePilot 企业级汽车租赁平台

DrivePilot 是一个企业级汽车租赁 WebApp，包含 React 前端和 Spring Boot 后端，默认接入本机 MySQL，不向真实库自动写入演示数据。

## 项目结构

- `frontend/`：React + Vite + TypeScript + Ant Design + GSAP + ECharts
- `backend/`：Spring Boot 3 + Spring Data JPA + MySQL 8

## 本地启动

### 后端

```powershell
cd backend
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="123456"
mvn spring-boot:run
```

后端默认端口：`http://localhost:8080`

### 前端

```powershell
cd frontend
npm install
npm run dev
```

前端默认端口：`http://127.0.0.1:5173`

## 数据库

开发环境使用 MySQL 数据库 `drivepilot_car_rental`。应用会通过 JPA 更新表结构，但不会默认插入演示账号或演示车辆。

首次创建管理员账号的推荐方式：

1. 在前端或 `POST /api/user/register` 注册普通用户。
2. 在 MySQL 中执行：

```sql
USE drivepilot_car_rental;

UPDATE users
SET role = 'ADMIN', status = 'ACTIVE', update_time = NOW()
WHERE username = '你的用户名';
```

更多接口和数据库说明见 `backend/README.md` 与 `backend/docs/DATABASE.md`。

## 验证命令

```powershell
cd backend
mvn test

cd ../frontend
npm run lint
npm run build
```
