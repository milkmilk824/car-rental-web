# DrivePilot 项目架构图

生成日期：2026-06-03

## 1. 全栈架构总览

```mermaid
flowchart TB
    subgraph Browser["表现层 / 前端体验"]
        Home["企业首页\n服务价值、租赁流程、企业服务"]
        Customer["用户端\n选车、下单、支付、合同、评价"]
        Staff["门店端\n订单履约、取车、还车、维保"]
        Admin["管理端\n看板、车辆、门店、订单、用户、支付、合同、评价"]
    end

    subgraph Frontend["前端工程层"]
        Router["React Router\n登录后按 role 分流"]
        Query["TanStack Query\n缓存、请求状态"]
        ApiClient["API Client\n/api 统一封装\nAuthorization: Bearer token\nPageResult 兼容"]
        Motion["GSAP + ECharts\n动效与运营图表"]
    end

    subgraph Backend["后端服务层 / Spring Boot"]
        Security["横切能力\nAuthInterceptor / TokenService\nRefreshToken / Redis 黑名单\n@RequireRole / @PublicEndpoint"]
        Common["公共能力\nApiResponse / PageResult\nGlobalExceptionHandler / DtoMapper\nOpenAPI / Swagger UI"]

        subgraph Controllers["控制层 Controller"]
            UserController["UserController"]
            CarController["CarController"]
            StoreController["StoreController"]
            OrderController["OrderController"]
            PaymentController["PaymentController"]
            ContractController["ContractController"]
            CommentController["CommentController"]
            AdminController["AdminController"]
            UploadController["UploadController"]
            BootstrapController["BootstrapController"]
        end

        subgraph Services["业务层 Service"]
            UserService["UserService\n注册、登录、资料、驾照、用户 CRUD"]
            CarService["CarService\n车辆搜索、详情、分类、状态、可租校验"]
            StoreService["StoreService\n门店查询、创建、编辑、删除"]
            StaffService["StoreStaffService\n员工门店绑定、权限校验"]
            OrderService["OrderService\n下单、取消、续租、取车、还车"]
            PaymentService["PaymentService\n支付单、模拟支付、回调、退款"]
            ContractService["ContractService\n合同生成、查询、签署"]
            CommentService["CommentService\n评价、防重复、后台移除"]
            MaintenanceService["MaintenanceService\n维修保养、车辆状态联动"]
            StatisticsService["StatisticsService\n看板统计、收入趋势、热门车型"]
            UploadService["UploadService\n图片校验、保存、URL 返回"]
            AuditService["AuditService\n管理员/门店写操作留痕"]
        end

        subgraph Repositories["持久层 Repository"]
            UserRepo["UserRepository"]
            StoreRepo["StoreRepository / StoreStaffRepository"]
            CarRepo["CarRepository / CarCategoryRepository / CarImageRepository"]
            OrderRepo["RentalOrderRepository"]
            PaymentRepo["PaymentOrderRepository"]
            ContractRepo["ContractRepository"]
            CommentRepo["CommentRepository"]
            MaintenanceRepo["MaintenanceRecordRepository"]
            RefreshRepo["RefreshTokenRepository"]
            AuditRepo["OperationAuditLogRepository"]
            RedisSession["Redis Session Store\nTokenService / StringRedisTemplate\nAccessToken 黑名单"]
        end

        subgraph Domain["实体层 Domain"]
            UserEntity["User / Store / StoreStaff"]
            CarEntity["Car / CarCategory / CarImage"]
            OrderEntity["RentalOrder / PaymentOrder / Contract"]
            SocialEntity["Comment / MaintenanceRecord / RefreshToken\nOperationAuditLog / BaseEntity"]
            Enums["Enums\nRole / Status / PayType / MaintenanceType"]
        end
    end

    subgraph Data["数据层"]
        MySQL["MySQL 8\n驱动库 drivepilot_car_rental"]
        Redis["Redis\nToken 会话、访问令牌黑名单、短期状态缓存"]
        H2["H2\n测试环境"]
        Schema["schema.sql\n建表脚本"]
        Uploads["本地 uploads\n车辆图片"]
    end

    Home --> Router
    Customer --> Router
    Staff --> Router
    Admin --> Router
    Router --> Query
    Query --> ApiClient
    ApiClient --> Controllers
    Motion --> Home
    Motion --> Customer
    Motion --> Admin

    Security -.拦截与鉴权.-> Controllers
    Common -.统一响应与异常.-> Controllers
    Controllers --> Services
    Services --> Repositories
    Services --> Domain
    Repositories --> Domain
    Repositories --> MySQL
    RedisSession --> Redis
    Repositories --> H2
    UploadService --> Uploads
    Schema --> MySQL
```

## 2. 后端分层结构

```mermaid
flowchart LR
    L1["表现层\nReact 企业首页 / 用户端 / 门店端 / 管理端\n外部支付回调"]
    L2["控制层\nUser / Car / Store / Order / Payment\nContract / Comment / Admin / Upload / Bootstrap"]
    L3["业务层\nUserService / CarService / StoreService\nOrderService / PaymentService / ContractService\nCommentService / MaintenanceService / StatisticsService / AuditService"]
    L4["持久层\nSpring Data JPA Repository + Redis Session Store\n分页搜索、悲观锁、租期冲突检测、刷新令牌、审计日志"]
    L5["实体层\nUser / Store / Car / RentalOrder\nPaymentOrder / Contract / Comment / MaintenanceRecord\nRefreshToken / OperationAuditLog"]
    L6["数据层\nMySQL 8 / Redis / H2 Test / schema.sql / uploads"]

    L1 --> L2 --> L3 --> L4 --> L5 --> L6
```

## 3. 核心业务闭环

```mermaid
sequenceDiagram
    actor U as 用户
    participant FE as 用户端 React
    participant API as Spring Boot API
    participant DB as MySQL
    actor S as 门店员工
    actor A as 管理员

    U->>FE: 登录 / 搜索车辆
    FE->>API: GET /api/cars
    API->>DB: 查询车辆、门店、分类
    DB-->>API: 可租车辆
    API-->>FE: ApiResponse<PageResult<Car>>

    U->>FE: 创建订单
    FE->>API: POST /api/orders
    API->>DB: 校验车辆状态与租期冲突
    API->>DB: 保存订单，车辆 RESERVED

    U->>FE: 支付
    FE->>API: POST /api/payments/create
    FE->>API: POST /api/payments/{paymentNo}/simulate-success
    API->>DB: 支付 SUCCESS，订单 PENDING_PICKUP

    S->>FE: 确认取车
    FE->>API: PUT /api/store/orders/{id}/pickup
    API->>DB: 订单 RENTING，车辆 RENTING

    S->>FE: 确认还车
    FE->>API: PUT /api/store/orders/{id}/return
    API->>DB: 订单 COMPLETED，车辆 AVAILABLE

    U->>FE: 查看合同并评价
    FE->>API: GET /api/contracts/order/{orderId}
    FE->>API: POST /api/comments
    API->>DB: 保存评价，防重复校验

    A->>FE: 查看运营后台
    FE->>API: GET /api/admin/dashboard
    API->>DB: 汇总收入、订单、车辆、门店数据
```

## 4. 关键接口模块

| 模块 | 主要接口 |
|---|---|
| 用户 | `POST /api/user/register`、`POST /api/user/login`、`GET/PUT /api/user/profile`、`POST /api/user/license` |
| 车辆 | `GET /api/cars`、`GET /api/cars/{id}`、`GET /api/cars/{id}/availability`、`/api/admin/cars/*` |
| 门店 | `GET /api/stores`、`GET /api/store/my-stores`、`/api/admin/stores/*` |
| 订单 | `POST /api/orders`、`GET /api/orders/my`、`PUT /api/orders/{id}/cancel`、`PUT /api/store/orders/{id}/pickup`、`PUT /api/store/orders/{id}/return` |
| 支付 | `POST /api/payments/create`、`POST /api/payments/{paymentNo}/simulate-success`、`POST /api/payments/callback`、`GET /api/admin/payments` |
| 合同 | `POST /api/contracts/generate`、`GET /api/contracts/order/{orderId}`、`PUT /api/contracts/{id}/sign`、`GET /api/admin/contracts` |
| 评价 | `POST /api/comments`、`GET /api/comments/car/{carId}`、`GET /api/admin/comments`、`DELETE /api/admin/comments/{id}` |
| 管理 | `GET /api/admin/dashboard`、`GET /api/admin/dashboard/revenue-trend`、`/api/admin/users/*` |
