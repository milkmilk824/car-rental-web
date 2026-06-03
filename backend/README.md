# 企业级汽车租赁后端

这是按项目文档先完成的 Spring Boot 单体后端首版，保留了后续拆分 Spring Cloud 微服务的模块边界。

## 技术栈

- Java 17
- Spring Boot 3.3.5
- Spring Web / Validation / Data JPA
- Spring Data Redis
- Springdoc OpenAPI / Swagger UI
- **MySQL 8** 持久化数据库（默认 dev 环境）
- **Redis** 会话持久化（Token session、访问令牌黑名单，可配置为 Redis 必须或自动兜底）
- H2 内存数据库（test 环境，`mvn test` 无需 MySQL）
- BCrypt 密码加密
- 自定义 Token 鉴权、刷新令牌、退出登录与角色控制

## 数据库

| 环境 | 数据库 | Profile | 说明 |
|------|--------|---------|------|
| 开发 | MySQL 8 | `dev`（默认） | `ddl-auto: update` 自动建表，数据持久化 |
| 预发 | MySQL 8 | `mysql` | `ddl-auto: validate` 需手动执行 SQL |
| 测试 | H2 内存库 | `test` | `mvn test` 自动使用，不依赖 MySQL |

数据库名：`drivepilot_car_rental`
详细说明参见 **[docs/DATABASE.md](docs/DATABASE.md)**

## 本地启动

### 前置条件

1. MySQL 8 已安装并运行
2. 创建数据库（也可以让 dev profile 通过 JDBC 参数自动创建）：
   ```sql
   CREATE DATABASE IF NOT EXISTS drivepilot_car_rental DEFAULT CHARACTER SET utf8mb4;
   ```
3. 如需手动建表，执行 `src/main/resources/db/mysql/schema.sql`

### 连接配置

默认连接本机 `localhost:3306`，数据库用户默认 `root`，密码通过环境变量传入，避免把本机密码提交到仓库：

| 环境变量 | 默认值 | 说明 |
|----------|--------|------|
| `MYSQL_URL` | `jdbc:mysql://localhost:3306/drivepilot_car_rental?...` | JDBC 连接串 |
| `MYSQL_USER` | `root` | 数据库用户 |
| `MYSQL_PASSWORD` | 空 | 数据库密码；本机可设置为 `123456` |
| `REDIS_HOST` | `localhost` | Redis 主机 |
| `REDIS_PORT` | `6379` | Redis 端口 |
| `REDIS_PASSWORD` | 空 | Redis 密码 |
| `REDIS_DATABASE` | `0` | Redis database |
| `APP_SESSION_STORE` | `auto` | `local` 仅内存，`redis` 强制 Redis，`auto` Redis 优先、本地兜底 |
| `APP_SESSION_TOKEN_TTL` | `PT12H` | Token 在 Redis 中的有效期 |
| `APP_SESSION_REFRESH_TOKEN_TTL` | `P14D` | 刷新令牌有效期，持久化在 MySQL |
| `APP_CACHE_REDIS_PREFIX` | `drivepilot:cache:` | 热点查询缓存前缀，覆盖车辆分类、门店、车辆搜索、看板 |
| `APP_RATE_LIMIT_ENABLED` | `true` | 是否启用高风险接口限流 |
| `APP_RATE_LIMIT_LOGIN_LIMIT` | `20` | 每分钟登录限流阈值 |
| `APP_RATE_LIMIT_ORDER_LIMIT` | `30` | 每分钟下单限流阈值 |
| `APP_RATE_LIMIT_CALLBACK_LIMIT` | `120` | 每分钟支付回调限流阈值 |
| `APP_RATE_LIMIT_UPLOAD_LIMIT` | `20` | 每分钟上传限流阈值 |
| `APP_REDIS_CIRCUIT_FAILURE_THRESHOLD` | `3` | Redis 连续失败多少次后熔断 |
| `APP_REDIS_CIRCUIT_OPEN_DURATION` | `PT30S` | Redis 熔断打开时长 |
| `DB_POOL_MAX_SIZE` | `20` | Hikari 最大连接池大小 |
| `PAYMENT_CALLBACK_SECRET` | `change-me-local-secret` | 支付回调签名密钥；生产环境必须覆盖 |

### 启动命令

```powershell
cd E:\workspace\car-rental-web\backend
$env:MYSQL_PASSWORD="123456"
$env:APP_SESSION_STORE="auto"
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run
```

启动后访问：

- API 地址：http://localhost:8080
- Swagger UI：http://localhost:8080/swagger-ui.html
- OpenAPI JSON：http://localhost:8080/v3/api-docs
- H2 控制台（仅 H2/test 环境）：http://localhost:8080/h2-console

### 初始化数据

开发环境默认 **不会自动写入演示数据**，避免污染你本机 MySQL。`DataInitializer` 已保留为测试/临时联调用途，只有显式设置 `app.data-init.enabled=true` 时才会在空库写入样例数据。

真实账号请通过业务接口注册普通用户，管理员和门店员工建议由数据库管理员在 `users` 表中创建并分配 `ADMIN` / `STORE_STAFF` 角色。

## 已实现模块

| 模块 | 功能 | CRUD 完备度 |
|------|------|-------------|
| 用户 | 注册、登录、个人信息、实名认证/驾驶证 | ✅ 完整 |
| 用户管理(Admin) | 列表、创建、编辑、状态切换、删除 | ✅ 完整 |
| 车辆分类 | 列表、创建、编辑、删除 | ✅ 完整 |
| 车辆 | 搜索、详情、创建、编辑、删除、上下架、图片、门店/分类关联 | ✅ 完整 |
| 门店 | 查询（按城市/营业状态）、创建、编辑、删除、营业切换 | ✅ 完整 |
| 租赁订单 | 创建、列表(用户/门店/全部)、详情、取消、续租、取车、还车 | ✅ 完整 |
| 支付 | 创建、状态查询、按订单查、回调、模拟成功、退款、流水列表 | ✅ 完整 |
| 合同 | 生成、列表、详情、按订单查、签署 | ✅ 完整 |
| 评价 | 创建、按车查询、列表、审核移除（软删除） | ✅ 完整 |
| 维修保养 | 创建、更新、删除、按车查询、列表 | ✅ 完整 |
| 数据看板 | 日订单、月收入、出租率、活跃用户、热门车型TOP5、门店业绩 | ✅ 完整 |
| 操作审计 | 管理员/门店关键写操作留痕、审计日志分页查询 | ✅ 完整 |
| 高并发基础 | 热点 Redis 缓存、限流、Redis 熔断降级、异步审计、复合索引、N+1 优化 | ✅ 已补强 |

> 数据接入说明：用户、车辆、分类、门店、维修保养等基础资料已接入真实 MySQL 增删改查。  
> 订单、支付、合同、评价属于流程型数据：通过下单、支付、签署、退款、评价移除等业务动作更新状态；评价模块使用软删除(`status=REMOVED`)；合同由支付成功自动生成，也可由管理员补生成。

## 创建首个管理员账号

数据库初始为空，需要创建管理员账号。推荐使用以下**安全可控**方式：

### 方式一：注册后提升角色（推荐）

先通过 `POST /api/user/register` 注册一个真实用户，密码由应用生成 BCrypt 哈希并写入数据库。然后在 MySQL 中将该用户提升为管理员：

```sql
USE drivepilot_car_rental;

UPDATE users
SET role = 'ADMIN', status = 'ACTIVE', update_time = NOW()
WHERE username = '你的用户名';
```

这样不会在文档里固化密码哈希，也能保证管理员账号来自真实注册流程。

### 方式二：启用临时 Bootstrap 接口（需代码开启）

如需通过 API 创建首个管理员，可临时开启受密钥保护的 bootstrap 端点。该端点默认关闭，且只允许在库中不存在 `ADMIN` 用户时创建第一个管理员：

```powershell
$env:APP_BOOTSTRAP_ENABLED="true"
$env:APP_BOOTSTRAP_SECRET="change-this-secret"
```

```http
POST /api/bootstrap/admin
Content-Type: application/json

{
  "username": "admin",
  "password": "your-password",
  "phone": "18800000000",
  "email": "admin@example.com",
  "secret": "change-this-secret"
}
```

创建完成后应关闭 `APP_BOOTSTRAP_ENABLED` 并重启服务。

## 全部接口清单

### 公开接口（无需登录）
- `POST /api/user/register` — 注册
- `POST /api/user/login` — 登录
- `POST /api/user/refresh` — 刷新访问令牌并轮换刷新令牌
- `GET /api/cars`, `GET /api/cars/search` — 车辆搜索
- `GET /api/cars/categories` — 分类列表
- `GET /api/cars/{id}` — 车辆详情
- `GET /api/cars/{id}/availability?startTime=&endTime=` — 按租期确认车辆是否可租
- `GET /api/stores` — 门店列表（支持 city, onlyOpen 筛选）
- `GET /api/stores/{id}` — 门店详情
- `GET /api/comments/car/{carId}` — 车辆评价
- `POST /api/payments/callback` — 支付回调（外部系统）

### 用户接口（需登录）
- `GET /api/user/profile` — 个人信息
- `POST /api/user/logout` — 退出登录，撤销访问令牌并使刷新令牌失效
- `PUT /api/user/profile` — 更新个人信息
- `POST /api/user/license` — 提交驾照信息
- `POST /api/orders` — 创建订单
- `GET /api/orders/my` — 我的订单
- `GET /api/orders/{id}` — 订单详情
- `PUT /api/orders/{id}/cancel` — 取消订单
- `PUT /api/orders/{id}/renew` — 续租
- `POST /api/payments/create` — 创建支付单
- `GET /api/payments/status/{paymentNo}` — 支付状态
- `GET /api/payments/order/{orderId}` — 按订单查支付
- `POST /api/payments/{paymentNo}/simulate-success` — 模拟支付成功
- `GET /api/contracts/order/{orderId}` — 按订单查看合同
- `POST /api/comments` — 发表评价

### 门店员工接口（STORE_STAFF / ADMIN）
- `GET /api/store/my-stores` — 当前员工绑定门店
- `GET /api/store/orders?storeId=` — 门店订单
- `PUT /api/store/orders/{id}/pickup` — 确认取车
- `PUT /api/store/orders/{id}/return` — 确认还车
- `POST /api/admin/cars/maintenance` — 创建维保记录
- `GET /api/admin/cars/maintenance` — 维保记录列表
- `GET /api/admin/cars/{id}/maintenance` — 车辆维保记录
- `PUT /api/admin/cars/maintenance/{id}` — 更新维保记录
- `DELETE /api/admin/cars/maintenance/{id}` — 删除维保记录

### 管理员接口（ADMIN）
**数据看板：**
- `GET /api/admin/dashboard`
- `GET /api/admin/dashboard/revenue-trend?days=7`

**车辆与分类：**
- `POST /api/admin/upload/car-image` — 上传车辆图片，返回 `/uploads/car-images/...`
- `POST /api/admin/cars` — 创建车辆
- `PUT /api/admin/cars/{id}` — 更新车辆
- `DELETE /api/admin/cars/{id}` — 删除车辆
- `PUT /api/admin/cars/{id}/status` — 更新车辆状态
- `POST /api/admin/cars/categories` — 创建分类
- `PUT /api/admin/cars/categories/{id}` — 更新分类
- `DELETE /api/admin/cars/categories/{id}` — 删除分类

**用户管理：**
- `GET /api/admin/users` — 用户列表
- `GET /api/admin/users/{id}` — 用户详情
- `POST /api/admin/users` — 创建用户（含角色/状态）
- `PUT /api/admin/users/{id}` — 更新用户（角色/状态/信息）
- `PUT /api/admin/users/{id}/status?status=` — 切换用户状态
- `DELETE /api/admin/users/{id}` — 删除用户

**门店管理：**
- `POST /api/admin/stores` — 创建门店
- `PUT /api/admin/stores/{id}` — 更新门店
- `DELETE /api/admin/stores/{id}` — 删除门店
- `POST /api/admin/stores/{storeId}/staff/{userId}` — 绑定门店员工
- `GET /api/admin/stores/{storeId}/staff` — 查看门店员工
- `DELETE /api/admin/stores/{storeId}/staff/{userId}` — 解绑门店员工

**订单管理：**
- `GET /api/admin/orders` — 全部订单
- `PUT /api/admin/orders/{id}/pickup` — 确认取车
- `PUT /api/admin/orders/{id}/return` — 确认还车

**支付管理：**
- `GET /api/admin/payments` — 支付流水
- `POST /api/payments/refund` — 退款

**合同管理：**
- `POST /api/contracts/generate` — 生成合同
- `GET /api/admin/contracts` — 合同列表
- `GET /api/contracts/{id}` — 合同详情
- `PUT /api/contracts/{id}/sign` — 签署合同

**评价管理：**
- `GET /api/admin/comments` — 评价列表
- `DELETE /api/admin/comments/{id}` — 移除评价

**操作审计：**
- `GET /api/admin/audit-logs` — 审计日志分页查询

> 后台大表接口已统一分页返回 `PageResult<T>`：`/api/admin/users`、`/api/admin/orders`、`/api/store/orders`、`/api/admin/payments`、`/api/admin/contracts`、`/api/admin/comments`、`/api/admin/cars/maintenance`、`/api/admin/cars/{id}/maintenance`、`/api/admin/audit-logs`。分页列表使用 `EntityGraph` 与 `hibernate.default_batch_fetch_size=50` 降低 N+1 风险。

## 前端对接状态

前端三个入口（CustomerApp、StaffPortal、AdminPortal）的所有 CRUD 操作均通过 `src/api/client.ts` 调用上述真实后端接口：

| 入口 | API 数量 | 状态 |
|------|---------|------|
| CustomerApp | 11 个 | ✅ 全部对接真实接口 |
| StaffPortal | 10 个 | ✅ 全部对接真实接口 |
| AdminPortal | 31 个 | ✅ 全部对接真实接口（编辑/创建弹窗均为真实表单提交） |

> 前端 AdminPortal 的「导出」按钮为占位（提示通过数据库导出），「联系用户」为占位。这些不影响数据持久化。

需要登录的接口使用请求头：

```text
Authorization: Bearer <token>
```

## Profile 切换

```powershell
# 使用 MySQL (validate 模式，需预先建表)
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=mysql"

# 使用 H2（仅本地调试兜底，正式开发请用 MySQL）
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" spring-boot:run "-Dspring-boot.run.profiles=default"
```

## 验证

```powershell
& "D:\idea2024\IntelliJ IDEA 2024.1\plugins\maven\lib\maven3\bin\mvn.cmd" test
```

`ApiFlowIntegrationTest` 会跑通完整租车链路：登录、车辆查询、下单、创建支付单、模拟支付成功、生成合同、门店取车、门店还车、用户评价。
测试使用 H2 内存数据库，不依赖本机 MySQL。
