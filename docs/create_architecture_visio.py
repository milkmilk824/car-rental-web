from __future__ import annotations

from pathlib import Path

import win32com.client


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "docs" / "DrivePilot-architecture.vsdx"


def rgb(red: int, green: int, blue: int) -> str:
    return f"RGB({red},{green},{blue})"


def style_shape(shape, fill: str, line: str = "RGB(31,64,104)", text: str = "RGB(15,23,42)", font_size: int = 9):
    shape.CellsU("FillForegnd").FormulaU = fill
    shape.CellsU("LineColor").FormulaU = line
    shape.CellsU("LineWeight").FormulaU = "1.2 pt"
    shape.CellsU("Char.Size").FormulaU = f"{font_size} pt"
    shape.CellsU("Char.Color").FormulaU = text
    shape.CellsU("Para.HorzAlign").FormulaU = "1"
    shape.CellsU("VerticalAlign").FormulaU = "1"
    return shape


def box(page, x: float, y: float, w: float, h: float, text: str, fill: str, line: str = "RGB(31,64,104)", font_size: int = 9):
    shape = page.DrawRectangle(x, y, x + w, y + h)
    shape.Text = text
    return style_shape(shape, fill, line, font_size=font_size)


def title(page, text: str, x: float, y: float, w: float = 8.0, h: float = 0.45):
    shape = page.DrawRectangle(x, y, x + w, y + h)
    shape.Text = text
    style_shape(shape, "RGB(255,255,255)", "RGB(255,255,255)", "RGB(15,23,42)", 18)
    shape.CellsU("LinePattern").FormulaU = "0"
    return shape


def line(page, x1: float, y1: float, x2: float, y2: float, color: str = "RGB(37,99,235)"):
    shape = page.DrawLine(x1, y1, x2, y2)
    shape.CellsU("LineColor").FormulaU = color
    shape.CellsU("LineWeight").FormulaU = "1.4 pt"
    shape.CellsU("EndArrow").FormulaU = "13"
    return shape


def connect_vertical(page, from_shape, to_shape, color: str = "RGB(37,99,235)"):
    x1 = from_shape.CellsU("PinX").ResultIU
    y1 = from_shape.CellsU("PinY").ResultIU - from_shape.CellsU("Height").ResultIU / 2
    x2 = to_shape.CellsU("PinX").ResultIU
    y2 = to_shape.CellsU("PinY").ResultIU + to_shape.CellsU("Height").ResultIU / 2
    return line(page, x1, y1, x2, y2, color)


def prepare_page(app, doc, name: str, width: float = 16.0, height: float = 10.0):
    page = app.ActivePage if doc.Pages.Count == 1 and doc.Pages.Item(1).Shapes.Count == 0 else doc.Pages.Add()
    page.Name = name
    page.PageSheet.CellsU("PageWidth").FormulaU = f"{width} in"
    page.PageSheet.CellsU("PageHeight").FormulaU = f"{height} in"
    return page


def build_full_stack(page):
    title(page, "DrivePilot 全栈架构总览", 0.45, 9.35, 7.4, 0.5)

    frontend_fill = rgb(239, 246, 255)
    backend_fill = rgb(240, 253, 244)
    data_fill = rgb(255, 247, 237)
    cross_fill = rgb(245, 245, 255)
    layer_fill = rgb(248, 250, 252)

    l_front = box(page, 0.45, 7.35, 15.1, 1.7, "表现层 / 前端体验\n企业首页、用户端、门店端、管理端", layer_fill, rgb(148, 163, 184), 12)
    boxes_front = [
        box(page, 0.8, 7.65, 2.6, 0.72, "企业首页\n服务价值 / 企业服务", frontend_fill),
        box(page, 3.8, 7.65, 2.6, 0.72, "用户端\n选车 / 下单 / 支付", frontend_fill),
        box(page, 6.8, 7.65, 2.6, 0.72, "门店端\n取车 / 还车 / 维保", frontend_fill),
        box(page, 9.8, 7.65, 2.6, 0.72, "管理端\n看板 / CRUD / 审核", frontend_fill),
        box(page, 12.8, 7.65, 2.2, 0.72, "前端工程\nRouter / Query / GSAP", frontend_fill),
    ]

    l_api = box(page, 0.45, 5.35, 15.1, 1.55, "后端服务层 / Spring Boot API", layer_fill, rgb(148, 163, 184), 12)
    controllers = box(page, 0.8, 5.75, 5.2, 0.82, "控制层 Controller\nUser / Car / Store / Order / Payment\nContract / Comment / Admin / Upload", backend_fill, rgb(22, 163, 74), 8)
    services = box(page, 6.3, 5.75, 5.2, 0.82, "业务层 Service\n租赁规则 / 支付状态 / 合同评价\n维保联动 / 看板统计", backend_fill, rgb(22, 163, 74), 8)
    cross = box(page, 11.9, 5.75, 3.0, 0.82, "横切能力\n鉴权 / 刷新令牌 / 审计\nOpenAPI / 异常", cross_fill, rgb(99, 102, 241), 8)

    l_persist = box(page, 0.45, 3.55, 15.1, 1.32, "持久层 + 实体层", layer_fill, rgb(148, 163, 184), 12)
    repos = box(page, 0.8, 3.88, 5.2, 0.68, "Repository + Redis\nJPA / 分页 / RefreshToken\nAuditLog / 黑名单", backend_fill, rgb(22, 163, 74), 8)
    domains = box(page, 6.3, 3.88, 5.2, 0.68, "Domain\nUser / Store / Car / RentalOrder\nPaymentOrder / Contract / Comment", backend_fill, rgb(22, 163, 74), 8)
    enums = box(page, 11.9, 3.88, 3.0, 0.68, "Enums\n角色 / 状态 / 支付 / 维保", backend_fill, rgb(22, 163, 74), 8)

    l_data = box(page, 0.45, 1.55, 15.1, 1.42, "数据层", layer_fill, rgb(148, 163, 184), 12)
    mysql = box(page, 0.8, 1.9, 3.0, 0.75, "MySQL 8\n驱动库 drivepilot_car_rental", data_fill, rgb(234, 88, 12), 8)
    redis = box(page, 4.3, 1.9, 2.7, 0.75, "Redis\nToken 会话 / 黑名单 / TTL", data_fill, rgb(220, 38, 38), 8)
    h2 = box(page, 7.5, 1.9, 2.4, 0.75, "H2\n测试环境", data_fill, rgb(234, 88, 12), 8)
    schema = box(page, 10.4, 1.9, 2.4, 0.75, "schema.sql\n建表脚本", data_fill, rgb(234, 88, 12), 8)
    uploads = box(page, 13.2, 1.9, 2.2, 0.75, "uploads\n车辆图片", data_fill, rgb(234, 88, 12), 8)

    for item in boxes_front:
        line(page, item.CellsU("PinX").ResultIU, 7.35, item.CellsU("PinX").ResultIU, 6.9)
    connect_vertical(page, l_front, l_api)
    line(page, controllers.CellsU("PinX").ResultIU + 2.65, controllers.CellsU("PinY").ResultIU, services.CellsU("PinX").ResultIU - 2.65, services.CellsU("PinY").ResultIU)
    line(page, services.CellsU("PinX").ResultIU + 2.65, services.CellsU("PinY").ResultIU, cross.CellsU("PinX").ResultIU - 1.55, cross.CellsU("PinY").ResultIU, rgb(99, 102, 241))
    connect_vertical(page, l_api, l_persist)
    line(page, repos.CellsU("PinX").ResultIU + 2.65, repos.CellsU("PinY").ResultIU, domains.CellsU("PinX").ResultIU - 2.65, domains.CellsU("PinY").ResultIU)
    line(page, domains.CellsU("PinX").ResultIU + 2.65, domains.CellsU("PinY").ResultIU, enums.CellsU("PinX").ResultIU - 1.55, enums.CellsU("PinY").ResultIU)
    connect_vertical(page, l_persist, l_data)
    for target in (mysql, redis, h2, schema, uploads):
        line(page, repos.CellsU("PinX").ResultIU, 3.55, target.CellsU("PinX").ResultIU, 2.98, rgb(234, 88, 12))


def build_backend_layers(page):
    title(page, "后端分层结构：表现层 → 控制层 → 业务层 → 持久层 → 实体层 → 数据层", 0.45, 9.35, 14.5, 0.5)
    layers = [
        ("表现层", "React 企业首页 / 用户端 / 门店端 / 管理端\n外部支付回调", rgb(239, 246, 255)),
        ("控制层", "UserController / CarController / StoreController\nOrderController / PaymentController / ContractController\nCommentController / AdminController / UploadController", rgb(240, 253, 244)),
        ("业务层", "UserService / CarService / StoreService / StoreStaffService\nOrderService / PaymentService / ContractService\nCommentService / MaintenanceService / StatisticsService", rgb(236, 253, 245)),
        ("持久层", "Spring Data JPA Repository + Redis Session Store\n分页搜索 / 刷新令牌 / 操作审计 / Token 黑名单", rgb(254, 249, 195)),
        ("实体层", "User / Store / Car / RentalOrder / PaymentOrder\nContract / Comment / MaintenanceRecord / RefreshToken / OperationAuditLog", rgb(255, 247, 237)),
        ("数据层", "MySQL 8 / Redis / H2 Test / schema.sql / uploads", rgb(254, 242, 242)),
    ]
    y = 7.95
    shapes = []
    for idx, (name, desc, fill) in enumerate(layers, 1):
        badge = box(page, 0.75, y, 1.25, 0.9, f"{idx:02d}\n{name}", fill, rgb(37, 99, 235), 10)
        body = box(page, 2.25, y, 12.2, 0.9, desc, fill, rgb(148, 163, 184), 9)
        shapes.append((badge, body))
        y -= 1.28

    for idx in range(len(shapes) - 1):
        line(page, 1.38, shapes[idx][0].CellsU("PinY").ResultIU - 0.45, 1.38, shapes[idx + 1][0].CellsU("PinY").ResultIU + 0.45)
        line(page, 8.35, shapes[idx][1].CellsU("PinY").ResultIU - 0.45, 8.35, shapes[idx + 1][1].CellsU("PinY").ResultIU + 0.45, rgb(15, 118, 110))


def build_business_flow(page):
    title(page, "核心租赁业务闭环", 0.45, 9.35, 7.0, 0.5)
    steps = [
        ("1 登录/搜车", "POST /api/user/login\nGET /api/cars"),
        ("2 创建订单", "POST /api/orders\n车辆 RESERVED"),
        ("3 支付成功", "POST /api/payments/create\nsimulate-success"),
        ("4 门店取车", "PUT /api/store/orders/{id}/pickup\n订单 RENTING"),
        ("5 门店还车", "PUT /api/store/orders/{id}/return\n订单 COMPLETED"),
        ("6 合同/评价", "GET /api/contracts/order/{orderId}\nPOST /api/comments"),
    ]
    shapes = []
    x = 0.75
    for name, desc in steps:
        s = box(page, x, 6.6, 2.25, 1.15, f"{name}\n{desc}", rgb(239, 246, 255), rgb(37, 99, 235), 8)
        shapes.append(s)
        x += 2.45
    for first, second in zip(shapes, shapes[1:]):
        line(page, first.CellsU("PinX").ResultIU + 1.15, first.CellsU("PinY").ResultIU, second.CellsU("PinX").ResultIU - 1.15, second.CellsU("PinY").ResultIU)

    box(page, 1.1, 4.35, 4.0, 1.15, "用户端\n选车、下单、支付、合同、评价", rgb(240, 253, 244), rgb(22, 163, 74), 9)
    box(page, 6.1, 4.35, 4.0, 1.15, "门店端\n订单履约、确认取还、维保登记", rgb(255, 247, 237), rgb(234, 88, 12), 9)
    box(page, 11.1, 4.35, 4.0, 1.15, "管理端\n运营看板、车辆门店订单管理", rgb(245, 245, 255), rgb(99, 102, 241), 9)
    box(page, 3.0, 2.35, 10.0, 1.1, "状态流转\nPENDING_PAYMENT → PENDING_PICKUP → RENTING → COMPLETED\n车辆状态 RESERVED → RENTING → AVAILABLE", rgb(248, 250, 252), rgb(148, 163, 184), 10)


def main():
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    app = win32com.client.Dispatch("Visio.Application")
    app.Visible = True
    doc = app.Documents.Add("")

    page1 = prepare_page(app, doc, "全栈架构")
    build_full_stack(page1)

    page2 = prepare_page(app, doc, "后端分层")
    build_backend_layers(page2)

    page3 = prepare_page(app, doc, "业务闭环")
    build_business_flow(page3)

    doc.SaveAs(str(OUTPUT))
    print(str(OUTPUT))


if __name__ == "__main__":
    main()
