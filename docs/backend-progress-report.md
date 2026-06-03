# DrivePilot 后端项目完成度报告

报告日期：2026-06-03

## 1. 总体结论

当前后端已经完成核心业务闭环，属于可联调、可验收、可接真实 MySQL 数据库与 Redis 会话持久化的 Spring Boot 单体后端。

整体完成度评估：

| 维度 | 完成度 | 说明 |
|---|---:|---|
| 后端基础架构 | 96% | Spring Boot、JPA、MySQL、Redis、OpenAPI、统一响应、异常处理已完成 |
| 核心业务接口 | 92% | 用户、车辆、门店、订单、支付、合同、评价、维保、审计基本齐全 |
| 三端对接能力 | 92% | 用户端、门店端、管理端接口均已覆盖，后台大表接口已分页 |
| 生产级能力 | 91% | Redis 会话/热点缓存、刷新令牌、退出黑名单、异步审计、限流熔断、接口文档、复合索引已补齐 |

验证结果：

```text
mvn test
Tests run: 22
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

## 2. 后端层级结构

```text
表现层 -> 控制层 -> 业务层 -> 持久层 -> 实体层 -> 数据层
```

## 3. 表现层

表现层主要指前端页面或外部系统通过 HTTP 调用后端接口。

| 调用方 | 说明 | 状态 |
|---|---|---|
| 用户端 | 登录、选车、下单、支付、合同、评价 | 已接入 |
| 门店端 | 门店订单、确认取车、确认还车、维保登记 | 已接入 |
| 管理端 | 用户、车辆、门店、订单、支付、合同、评价、看板 | 已接入 |
| 外部支付系统 | 支付回调接口 | 有模拟回调，真实支付未接 |

对应前端入口：

```text
frontend/src/pages/CustomerApp.tsx
frontend/src/pages/StaffPortal.tsx
frontend/src/pages/AdminPortal.tsx
frontend/src/api/client.ts
```

## 4. 控制层 Controller

位置：

```text
backend/src/main/java/com/example/carrental/controller
```

| Controller | 负责模块 | 主要接口 | 状态 |
|---|---|---|---|
| UserController | 用户登录与个人信息 | 注册、登录、资料、驾照认证 | 完成 |
| CarController | 车辆、分类、维保 | 车辆搜索、详情、分类 CRUD、车辆 CRUD、维保 CRUD | 完成 |
| StoreController | 门店与员工绑定 | 门店查询、门店 CRUD、员工绑定、我的门店 | 完成 |
| OrderController | 租赁订单 | 下单、我的订单、取消、续租、取车、还车 | 完成 |
| PaymentController | 支付 | 创建支付单、模拟支付、回调、退款、流水 | 基本完成 |
| ContractController | 合同 | 生成、查询、签署、按订单查询 | 基本完成 |
| CommentController | 评价 | 创建评价、车辆评价、后台评价管理 | 完成 |
| AdminController | 管理后台 | 看板、用户管理 | 完成 |
| UploadController | 文件上传 | 车辆图片上传 | 完成 |
| BootstrapController | 首个管理员 | 受密钥保护创建首个管理员 | 完成 |

控制层的主要职责：

- 接收 HTTP 请求。
- 解析参数和请求体。
- 做基础校验。
- 根据角色注解控制访问权限。
- 调用业务层 Service。
- 返回统一 `ApiResponse<T>`。

## 5. 业务层 Service

位置：

```text
backend/src/main/java/com/example/carrental/service
```

| Service | 业务能力 | 完成情况 |
|---|---|---|
| UserService | 注册、登录、资料、驾照、管理员用户 CRUD、bootstrap 管理员 | 完成 |
| CarService | 车辆搜索、详情、可租校验、分类 CRUD、车辆 CRUD、车辆状态 | 完成 |
| StoreService | 门店列表、详情、创建、编辑、删除 | 完成 |
| StoreStaffService | 门店员工绑定、解绑、员工门店权限校验 | 完成 |
| OrderService | 下单、取消、续租、取车、还车、订单权限判断 | 完成 |
| PaymentService | 支付单、模拟支付成功、支付回调、退款、支付流水 | 基本完成 |
| ContractService | 合同生成、查询、签署 | 基本完成 |
| CommentService | 评价、防重复评价、后台移除 | 完成 |
| MaintenanceService | 维保创建、更新、删除、车辆状态联动 | 完成 |
| StatisticsService | 看板统计、收入趋势、热门车型、门店业绩 | 完成 |
| UploadService | 图片校验、保存、URL 返回 | 完成 |
| DtoMapper | 实体转 DTO | 完成 |
| HotCacheService | 车辆分类、门店列表、车辆搜索、看板等热点数据 Redis 缓存，本地兜底 | 完成 |
| RateLimitService | 登录、下单、支付回调、上传等高风险接口限流 | 完成 |
| RedisCircuitBreaker | Redis 异常熔断与本地降级，避免 Redis 故障拖慢接口 | 完成 |
| AuditService | 管理员/门店关键写操作异步审计入库 | 完成 |

业务层已完成的核心规则：

- 注册时校验用户名和手机号重复。
- 登录时校验密码、账号状态。
- 下单时校验车辆状态、租期、门店营业状态、订单时间冲突。
- 创建订单后车辆进入 `RESERVED`。
- 支付成功后订单进入 `PENDING_PICKUP`。
- 取车后订单进入 `RENTING`，车辆进入 `RENTING`。
- 还车后订单进入 `COMPLETED`，车辆回到 `AVAILABLE`。
- 取消订单会释放车辆。
- 续租只能对租赁中订单操作。
- 完成订单后才能评价，且同一订单不能重复评价。
- 创建维保记录会同步更新车辆状态为维修或保养。

## 6. 持久层 Repository

位置：

```text
backend/src/main/java/com/example/carrental/repository
```

| Repository | 对应数据 | 能力 |
|---|---|---|
| UserRepository | 用户 | 按用户名查询、手机号校验、角色存在校验 |
| CarRepository | 车辆 | 分页搜索、状态统计、悲观锁查车 |
| CarCategoryRepository | 车辆分类 | 按分类名查询 |
| CarImageRepository | 车辆图片 | 基础 CRUD |
| StoreRepository | 门店 | 按城市、状态查询 |
| StoreStaffRepository | 门店员工绑定 | 员工门店绑定关系查询 |
| RentalOrderRepository | 订单 | 用户订单、门店订单、状态统计、租期冲突检测 |
| PaymentOrderRepository | 支付 | 按支付号、订单、支付状态查询 |
| ContractRepository | 合同 | 按订单查合同、合同列表 |
| CommentRepository | 评价 | 按车辆查评价、防重复评价 |
| MaintenanceRecordRepository | 维保 | 按车查维保、全量维保列表 |
| RefreshTokenRepository | 刷新令牌 | MySQL 持久化 refresh token，支持过期和撤销 |
| OperationAuditLogRepository | 操作审计 | 管理员/门店关键写操作留痕、分页查询 |
| Redis Session Store | Token 会话 | 通过 `TokenService` + `StringRedisTemplate` 写入 Redis，支持 TTL、黑名单与本地兜底 |

持久层特点：

- 使用 Spring Data JPA。
- 车辆搜索使用 `JpaSpecificationExecutor`。
- 车辆下单查询使用悲观锁，降低并发重复预订风险。
- 订单仓库有租期重叠检测查询。
- 支付、合同、评价、维保均接入真实数据库。
- 支付创建按订单建立唯一约束，回调/退款支持 `Idempotency-Key` 幂等键与悲观锁并发状态保护。
- 订单、车辆、门店、支付、评价、维保已补复合索引，覆盖车辆时间窗、门店订单、支付状态时间、评价车辆状态、维保车辆时间等查询。
- 分页列表查询通过 `EntityGraph` 预取关键关联，配合 `hibernate.default_batch_fetch_size=50` 降低 DTO 映射 N+1 风险。
- Token 会话已加入 Redis 持久化能力，`APP_SESSION_STORE=redis` 可强制使用 Redis，`auto` 可在 Redis 不可用时回退到本地内存。
- 刷新令牌已通过 MySQL 表持久化，刷新时轮换旧 token，退出时撤销 refresh token 并将 access token 加入黑名单。

## 7. 实体层 Domain

位置：

```text
backend/src/main/java/com/example/carrental/domain
```

| 实体 | 表 | 说明 |
|---|---|---|
| User | users | 用户、角色、状态、驾照信息 |
| Store | store | 门店、城市、地址、营业状态 |
| StoreStaff | store_staff | 门店员工绑定 |
| CarCategory | car_category | 车辆分类 |
| Car | car | 车辆主体、价格、押金、状态、门店、分类 |
| CarImage | car_image | 车辆图片 |
| RentalOrder | rental_order | 租赁订单、取还车时间、金额、状态 |
| PaymentOrder | payment_order | 支付单、支付状态、交易号 |
| Contract | contract | 合同编号、合同 URL、签署状态 |
| Comment | comment_record | 评价、评分、内容、状态 |
| MaintenanceRecord | maintenance_record | 维修保养记录 |
| BaseEntity | 通用字段 | createTime、updateTime |

主要枚举：

| 枚举 | 值 |
|---|---|
| UserRole | USER、STORE_STAFF、ADMIN |
| UserStatus | ACTIVE、DISABLED |
| StoreStatus | OPEN、CLOSED |
| CarStatus | AVAILABLE、RESERVED、RENTING、REPAIRING、MAINTAINING、OFFLINE |
| OrderStatus | PENDING_PAYMENT、PENDING_PICKUP、RENTING、PENDING_RETURN、COMPLETED、CANCELLED、REFUNDING、REFUNDED、EXCEPTION |
| PayType | ALIPAY、WECHAT、BANK_CARD、CASH、MOCK |
| PayStatus | WAITING、SUCCESS、REFUNDING、REFUNDED、CLOSED |
| ContractStatus | UNSIGNED、SIGNED、ARCHIVED |
| CommentStatus | PENDING、APPROVED、REMOVED |
| MaintenanceType | REPAIR、MAINTENANCE |

## 8. 数据层

数据库配置：

```text
dev profile   -> MySQL 8，ddl-auto: update
mysql profile -> MySQL 8，ddl-auto: validate
test profile  -> H2 内存数据库
session store  -> Redis / local / auto
```

| 环境 | 数据库 | 状态 |
|---|---|---|
| 开发环境 | MySQL `drivepilot_car_rental` | 已接入 |
| 会话缓存 | Redis `drivepilot:session:*` | 已接入 |
| 热点缓存 | Redis `drivepilot:cache:*` | 已接入，Redis 不可用时本地兜底 |
| 限流计数 | Redis `drivepilot:rate-limit:*` | 已接入，Redis 异常时本地计数 |
| 测试环境 | H2 | 已接入 |
| 初始化数据 | DataInitializer | 默认关闭，避免污染真实 MySQL |
| 建表 SQL | `src/main/resources/db/mysql/schema.sql` | 已提供 |

配置文件：

```text
backend/src/main/resources/application.yml
backend/src/main/resources/application-dev.yml
backend/src/main/resources/application-mysql.yml
backend/src/test/resources/application-test.yml
backend/src/main/resources/db/mysql/schema.sql
```

## 9. 当前接口完成情况

### 9.1 公开接口

| 接口 | 功能 | 状态 |
|---|---|---|
| `POST /api/user/register` | 用户注册 | 完成 |
| `POST /api/user/login` | 用户登录 | 完成 |
| `GET /api/cars` | 车辆搜索 | 完成 |
| `GET /api/cars/search` | 车辆搜索别名 | 完成 |
| `GET /api/cars/categories` | 分类列表 | 完成 |
| `GET /api/cars/{id}` | 车辆详情 | 完成 |
| `GET /api/cars/{id}/availability` | 车辆可租校验 | 完成 |
| `GET /api/stores` | 门店列表 | 完成 |
| `GET /api/stores/{id}` | 门店详情 | 完成 |
| `GET /api/comments/car/{carId}` | 车辆评价 | 完成 |
| `POST /api/payments/callback` | 支付回调 | 模拟完成 |

### 9.2 用户接口

| 接口 | 功能 | 状态 |
|---|---|---|
| `GET /api/user/profile` | 查询个人资料 | 完成 |
| `PUT /api/user/profile` | 更新个人资料 | 完成 |
| `POST /api/user/license` | 驾照认证 | 完成 |
| `POST /api/orders` | 创建租赁订单 | 完成 |
| `GET /api/orders/my` | 我的订单 | 完成 |
| `GET /api/orders/{id}` | 订单详情 | 完成 |
| `PUT /api/orders/{id}/cancel` | 取消订单 | 完成 |
| `PUT /api/orders/{id}/renew` | 续租 | 完成 |
| `POST /api/payments/create` | 创建支付单 | 完成 |
| `GET /api/payments/status/{paymentNo}` | 支付状态 | 完成 |
| `GET /api/payments/order/{orderId}` | 按订单查支付 | 完成 |
| `POST /api/payments/{paymentNo}/simulate-success` | 模拟支付成功 | 完成 |
| `GET /api/contracts/order/{orderId}` | 按订单查合同 | 完成 |
| `POST /api/comments` | 创建评价 | 完成 |

### 9.3 门店端接口

| 接口 | 功能 | 状态 |
|---|---|---|
| `GET /api/store/my-stores` | 当前员工绑定门店 | 完成 |
| `GET /api/store/orders?storeId=` | 门店订单 | 完成 |
| `PUT /api/store/orders/{id}/pickup` | 确认取车 | 完成 |
| `PUT /api/store/orders/{id}/return` | 确认还车 | 完成 |
| `POST /api/admin/cars/maintenance` | 创建维保记录 | 完成 |
| `GET /api/admin/cars/maintenance` | 维保列表 | 完成 |
| `GET /api/admin/cars/{id}/maintenance` | 车辆维保记录 | 完成 |
| `PUT /api/admin/cars/maintenance/{id}` | 更新维保 | 完成 |
| `DELETE /api/admin/cars/maintenance/{id}` | 删除维保 | 完成 |

### 9.4 管理端接口

| 模块 | 接口 | 状态 |
|---|---|---|
| 看板 | `/api/admin/dashboard` | 完成 |
| 收入趋势 | `/api/admin/dashboard/revenue-trend` | 完成 |
| 用户管理 | `/api/admin/users/**` | 完成 |
| 车辆管理 | `/api/admin/cars/**` | 完成 |
| 分类管理 | `/api/admin/cars/categories/**` | 完成 |
| 门店管理 | `/api/admin/stores/**` | 完成 |
| 员工绑定 | `/api/admin/stores/{storeId}/staff/**` | 完成 |
| 订单管理 | `/api/admin/orders` | 完成 |
| 支付流水 | `/api/admin/payments` | 完成 |
| 退款 | `/api/payments/refund` | 完成 |
| 合同管理 | `/api/admin/contracts`、`/api/contracts/**` | 基本完成 |
| 评价管理 | `/api/admin/comments/**` | 完成 |
| 图片上传 | `/api/admin/upload/car-image` | 完成 |

## 10. 已完成的核心业务闭环

```text
注册/登录
  ↓
查询车辆
  ↓
校验可租时间
  ↓
创建订单
  ↓
创建支付单
  ↓
模拟支付成功
  ↓
自动生成合同
  ↓
门店确认取车
  ↓
用户续租/门店确认还车
  ↓
用户评价
  ↓
后台查看订单、支付、合同、评价、看板
```

该链路已被集成测试覆盖。

## 11. 测试验证

已执行：

```powershell
mvn test
```

结果：

```text
Tests run: 22
Failures: 0
Errors: 0
Skipped: 0
BUILD SUCCESS
```

测试覆盖：

| 测试类 | 覆盖内容 |
|---|---|
| ApiFlowIntegrationTest | 完整租车链路 |
| BootstrapAdminIntegrationTest | 首个管理员创建 |
| OperationalInterfacesIntegrationTest | 门店员工权限、车辆可租校验、上传、收入趋势 |
| ProductionHardeningIntegrationTest | 刷新/退出、OpenAPI、后台分页、操作审计 |
| CarRentalApplicationTests | Spring 上下文启动 |
| ConcurrencyHardeningIntegrationTest | 退款/回调幂等、登录限流 |
| TokenServiceRedisTest | Redis 会话持久化与本地兜底 |
| HotCacheServiceTest | Redis 热点缓存读写 |
| RedisCircuitBreakerTest | Redis 熔断打开与恢复 |
| TestControllerIntegrationTest | 测试控制器 dev/test 可用、prod 不注册 |

## 12. 剩余待完善项

| 优先级 | 待完善项 | 说明 |
|---|---|---|
| 高 | 真实支付 | 当前是 MOCK 支付和模拟回调 |
| 高 | 合同 PDF | 当前合同是 URL，不是真正生成 PDF 文件 |
| 中 | 数据库迁移 | 建议引入 Flyway 或 Liquibase |
| 中 | 门店员工维保权限 | 维保接口建议按绑定门店进一步限制 |
| 中 | 压测与容量规划 | 已具备缓存/限流/索引基础，但仍需 JMeter/k6 压测确认连接池、Redis、MySQL 参数 |

## 13. 最终评价

当前后端已经达到：

```text
可运行
可联调
可持久化
可完成核心业务流程
可支撑当前三端前端页面
```

但如果按企业级生产系统要求，还需要继续补：

```text
真实支付
合同文件生成
数据库迁移
更严格权限控制
压测容量报告
```

总体判断：项目后端核心功能已完成，剩余主要是生产化增强。
