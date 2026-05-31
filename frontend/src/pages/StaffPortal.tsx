import {
  BellOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  DownOutlined,
  FileDoneOutlined,
  LogoutOutlined,
  MessageOutlined,
  SearchOutlined,
  SettingOutlined,
  ShopOutlined,
  ToolOutlined,
  UserOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Checkbox, Empty, Form, Input, Modal, Select, Space, Table, Tag, Timeline } from "antd";
import type { ColumnsType } from "antd/es/table";
import gsap from "gsap";
import { useEffect, useMemo, useRef, useState, type ReactNode } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { formatCurrency, staffMetrics, staffStatusMeta, type StaffStatus } from "../data/opsMock";
import { useAuth } from "../state/useAuth";
import type { MaintenanceType, OrderStatus, RentalOrder, Store } from "../types";

interface StaffOrderView {
  id: number;
  orderNo: string;
  status: StaffStatus;
  rawStatus: OrderStatus;
  carStatus: string;
  customerName: string;
  customerPhone: string;
  carId: number;
  carName: string;
  carModel: string;
  plateNumber: string;
  vin: string;
  image: string;
  pickupStore: string;
  returnStore: string;
  pickupTime: string;
  returnTime: string;
  amount: number;
  mileage: number;
  assignee: string;
  note: string;
  checklist: Array<{ label: string; ok: boolean }>;
}

const laneKeys: StaffStatus[] = ["PENDING_PICKUP", "RENTING", "PENDING_RETURN", "EXCEPTION"];
const sidebarItems: Array<{ key: StaffStatus | "ALL" | "MAINTENANCE"; label: string; icon: ReactNode }> = [
  { key: "ALL", label: "履约看板", icon: <FileDoneOutlined /> },
  { key: "PENDING_PICKUP", label: "待取车", icon: <ShopOutlined /> },
  { key: "RENTING", label: "租赁中", icon: <CheckCircleOutlined /> },
  { key: "PENDING_RETURN", label: "待还车", icon: <DownOutlined /> },
  { key: "MAINTENANCE", label: "维修保养", icon: <ToolOutlined /> },
];

const fallbackImage = "/images/customer-hero-road.png";
const emptyStores: Store[] = [];
const emptyRentalOrders: RentalOrder[] = [];

function staffStatus(status: OrderStatus): StaffStatus {
  if (status === "PENDING_PICKUP") return "PENDING_PICKUP";
  if (status === "RENTING") return "RENTING";
  if (status === "PENDING_RETURN") return "PENDING_RETURN";
  if (status === "EXCEPTION") return "EXCEPTION";
  return "COMPLETED";
}

function formatDateTime(value: string) {
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    weekday: "short",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function customerName(order: RentalOrder) {
  return order.user.realName || order.user.username || "未实名用户";
}

function actionLabel(order?: StaffOrderView) {
  if (!order) return "查看详情";
  if (order.rawStatus === "PENDING_PICKUP") return "确认取车";
  if (order.rawStatus === "RENTING" || order.rawStatus === "PENDING_RETURN") return "确认还车";
  if (order.status === "EXCEPTION") return "登记维保";
  return "查看详情";
}

function buildChecklist(order: RentalOrder): StaffOrderView["checklist"] {
  const pickedUp = ["RENTING", "PENDING_RETURN", "COMPLETED"].includes(order.status);
  const finished = order.status === "COMPLETED";
  return [
    { label: "驾照与身份证核验", ok: true },
    { label: "电子合同与押金确认", ok: order.status !== "PENDING_PAYMENT" },
    { label: "取车影像归档", ok: pickedUp },
    { label: "还车外观与里程复检", ok: finished },
  ];
}

function toStaffOrderView(order: RentalOrder, assignee: string): StaffOrderView {
  const total = Number(order.totalAmount || 0) + Number(order.depositAmount || 0);
  return {
    id: order.id,
    orderNo: order.orderNo,
    status: staffStatus(order.status),
    rawStatus: order.status,
    carStatus: order.car.status,
    customerName: customerName(order),
    customerPhone: order.user.phone || "暂无手机号",
    carId: order.car.id,
    carName: order.car.carName,
    carModel: `${order.car.brand} ${order.car.model}`,
    plateNumber: order.car.plateNumber,
    vin: `CAR-${order.car.id.toString().padStart(6, "0")}`,
    image: order.car.imageUrls?.[0] || fallbackImage,
    pickupStore: order.pickupStore.storeName,
    returnStore: order.returnStore.storeName,
    pickupTime: formatDateTime(order.startTime),
    returnTime: formatDateTime(order.endTime),
    amount: total,
    mileage: order.car.mileage || 0,
    assignee,
    note:
      order.status === "PENDING_PICKUP"
        ? "客户已完成支付，请核验驾照、合同、押金和车况影像。"
        : order.status === "RENTING"
          ? "车辆正在租赁中，可在客户到店后确认还车。"
          : order.status === "COMPLETED"
            ? "订单已完成，还车复检和费用结算已归档。"
            : "请跟进该订单的履约状态。",
    checklist: buildChecklist(order),
  };
}

function maintenanceFilter(order: StaffOrderView) {
  return order.status === "EXCEPTION" || order.carStatus === "REPAIRING" || order.carStatus === "MAINTAINING";
}

export function StaffPortal() {
  const { message } = App.useApp();
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const pageRef = useRef<HTMLDivElement>(null);
  const [maintenanceForm] = Form.useForm<{ type: MaintenanceType; description?: string; cost?: number }>();
  const [selectedStoreId, setSelectedStoreId] = useState<number>();
  const [activeStatus, setActiveStatus] = useState<StaffStatus | "ALL" | "MAINTENANCE">("ALL");
  const [selectedOrderId, setSelectedOrderId] = useState(0);
  const [confirmOrder, setConfirmOrder] = useState<StaffOrderView | null>(null);
  const [maintenanceOrder, setMaintenanceOrder] = useState<StaffOrderView | null>(null);
  const [keyword, setKeyword] = useState("");

  const storesQuery = useQuery({ queryKey: ["my-stores"], queryFn: api.myStores });
  const stores = storesQuery.data ?? emptyStores;
  const effectiveStoreId = selectedStoreId ?? stores[0]?.id;
  const selectedStore = stores.find((item) => item.id === effectiveStoreId) || stores[0];

  const ordersQuery = useQuery({
    queryKey: ["staff-orders", effectiveStoreId],
    queryFn: () => api.storeOrders(effectiveStoreId || 0),
    enabled: Boolean(effectiveStoreId),
  });

  const rentalOrders = ordersQuery.data ?? emptyRentalOrders;
  const orders = useMemo(
    () => rentalOrders.map((order) => toStaffOrderView(order, user?.realName || user?.username || "门店员工")),
    [rentalOrders, user?.realName, user?.username],
  );
  const selectedOrder = orders.find((order) => order.id === selectedOrderId) || orders[0];

  const filteredOrders = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return orders.filter((order) => {
      const statusMatched =
        activeStatus === "ALL" ||
        (activeStatus === "MAINTENANCE" ? maintenanceFilter(order) : order.status === activeStatus);
      const keywordMatched =
        !normalized ||
        [order.orderNo, order.customerName, order.customerPhone, order.carName, order.plateNumber]
          .join(" ")
          .toLowerCase()
          .includes(normalized);
      return statusMatched && keywordMatched;
    });
  }, [activeStatus, keyword, orders]);

  const realStaffMetrics = useMemo(() => {
    const pickup = orders.filter((order) => order.rawStatus === "PENDING_PICKUP").length;
    const returning = orders.filter((order) => order.rawStatus === "PENDING_RETURN").length;
    const renting = orders.filter((order) => order.rawStatus === "RENTING").length;
    const maintenance = orders.filter(maintenanceFilter).length;
    return [
      { ...staffMetrics[0], value: pickup, trend: `${selectedStore?.storeName || "当前门店"} 待交付` },
      { ...staffMetrics[1], value: returning, trend: "等待还车复检" },
      { ...staffMetrics[2], value: renting, trend: "实时履约" },
      { ...staffMetrics[3], value: maintenance, trend: "需优先处理" },
    ];
  }, [orders, selectedStore?.storeName]);

  const invalidateStaffData = () => {
    queryClient.invalidateQueries({ queryKey: ["staff-orders"] });
    queryClient.invalidateQueries({ queryKey: ["admin-orders"] });
    queryClient.invalidateQueries({ queryKey: ["admin-cars"] });
    queryClient.invalidateQueries({ queryKey: ["admin-maintenance"] });
  };

  const pickupMutation = useMutation({
    mutationFn: api.confirmPickup,
    onSuccess: () => {
      invalidateStaffData();
      message.success("取车已确认");
      setConfirmOrder(null);
    },
    onError: (error) => message.error(error.message),
  });

  const returnMutation = useMutation({
    mutationFn: api.confirmReturn,
    onSuccess: () => {
      invalidateStaffData();
      message.success("还车已确认");
      setConfirmOrder(null);
    },
    onError: (error) => message.error(error.message),
  });

  const maintenanceMutation = useMutation({
    mutationFn: api.createMaintenance,
    onSuccess: () => {
      invalidateStaffData();
      message.success("维保工单已登记");
      setMaintenanceOrder(null);
      maintenanceForm.resetFields();
    },
    onError: (error) => message.error(error.message),
  });

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) return;
    const context = gsap.context(() => {
      gsap.from(".ops-sidebar, .ops-topbar", { y: -18, autoAlpha: 0, duration: 0.5, ease: "power2.out" });
      gsap.from(".ops-page-title, .ops-kpi-card, .ops-panel, .ops-detail-drawer", {
        y: 24,
        autoAlpha: 0,
        duration: 0.55,
        stagger: 0.06,
        ease: "power2.out",
      });
      const orderCards = gsap.utils.toArray<HTMLElement>(".staff-order-card");
      if (orderCards.length) {
        gsap.from(orderCards, { y: 18, autoAlpha: 0, duration: 0.45, stagger: 0.04, delay: 0.25 });
      }
    }, pageRef);
    return () => context.revert();
  }, []);

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced || !selectedOrder) return;
    const targets = gsap.utils.toArray<HTMLElement>(".staff-detail .detail-motion");
    if (!targets.length) return;
    gsap.fromTo(
      targets,
      { x: 18, autoAlpha: 0 },
      { x: 0, autoAlpha: 1, duration: 0.32, stagger: 0.04, ease: "power2.out" },
    );
  }, [selectedOrderId, selectedOrder]);

  const handlePrimaryAction = (order?: StaffOrderView) => {
    if (!order) {
      message.warning("当前门店暂无可操作订单");
      return;
    }
    if (order.status === "EXCEPTION") {
      setMaintenanceOrder(order);
      maintenanceForm.setFieldsValue({ type: "REPAIR", description: order.note, cost: 0 });
      return;
    }
    if (order.rawStatus !== "PENDING_PICKUP" && order.rawStatus !== "RENTING" && order.rawStatus !== "PENDING_RETURN") {
      message.info("当前订单状态无需门店处理");
      return;
    }
    setConfirmOrder(order);
  };

  const completeOrderAction = () => {
    if (!confirmOrder) return;
    if (confirmOrder.rawStatus === "PENDING_PICKUP") {
      pickupMutation.mutate(confirmOrder.id);
      return;
    }
    returnMutation.mutate(confirmOrder.id);
  };

  const completeMaintenance = () => {
    if (!maintenanceOrder) return;
    const values = maintenanceForm.getFieldsValue();
    maintenanceMutation.mutate({
      carId: maintenanceOrder.carId,
      type: values.type || "REPAIR",
      description: values.description || maintenanceOrder.note,
      cost: Number(values.cost || 0),
    });
  };

  const columns: ColumnsType<StaffOrderView> = [
    {
      title: "订单号",
      dataIndex: "orderNo",
      width: 176,
      render: (value: string, record) => (
        <button className="text-link-button" onClick={() => setSelectedOrderId(record.id)}>
          {value}
        </button>
      ),
    },
    {
      title: "状态",
      width: 104,
      render: (_, record) => <Tag color={staffStatusMeta[record.status].color}>{staffStatusMeta[record.status].label}</Tag>,
    },
    {
      title: "客户",
      render: (_, record) => (
        <div className="table-stack">
          <strong>{record.customerName}</strong>
          <span>{record.customerPhone}</span>
        </div>
      ),
    },
    {
      title: "车辆",
      render: (_, record) => (
        <div className="ops-table-car">
          <img src={record.image} alt={record.carName} />
          <div>
            <strong>{record.carName}</strong>
            <span>{record.plateNumber}</span>
          </div>
        </div>
      ),
    },
    {
      title: "取还时间 / 地点",
      render: (_, record) => (
        <div className="table-stack wide">
          <span>
            取 {record.pickupTime} · {record.pickupStore}
          </span>
          <span>
            还 {record.returnTime} · {record.returnStore}
          </span>
        </div>
      ),
    },
    { title: "金额", width: 100, render: (_, record) => <strong className="amount-text">{formatCurrency(record.amount)}</strong> },
    {
      title: "操作",
      width: 138,
      render: (_, record) => (
        <Button size="small" type={record.status === "EXCEPTION" ? "default" : "link"} onClick={() => handlePrimaryAction(record)}>
          {actionLabel(record)}
        </Button>
      ),
    },
  ];

  return (
    <main className="ops-page staff-ops" ref={pageRef}>
      <header className="ops-topbar">
        <Link className="rental-brand ops-brand" to="/">
          <span className="brand-symbol">D</span>
          <strong>DrivePilot</strong>
          <em>门店端</em>
        </Link>
        <Select
          value={selectedStore?.id}
          loading={storesQuery.isLoading}
          onChange={setSelectedStoreId}
          className="ops-store-select"
          options={stores.map((item: Store) => ({ label: item.storeName, value: item.id }))}
          placeholder="选择门店"
        />
        <Input className="ops-search" prefix={<SearchOutlined />} value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索订单号 / 手机号 / 车牌号" />
        <button className="icon-tool">
          <MessageOutlined />
        </button>
        <button className="icon-tool badge">
          <BellOutlined />
        </button>
        <div className="ops-user">
          <span>{(user?.realName || user?.username || "S").slice(0, 1)}</span>
          <div>
            <strong>{user?.realName || user?.username || "门店员工"}</strong>
            <small>门店店员</small>
          </div>
          <Button type="text" icon={<LogoutOutlined />} onClick={logout} />
        </div>
      </header>

      <div className="ops-layout">
        <aside className="ops-sidebar">
          <nav>
            {sidebarItems.map((item) => (
              <button
                key={item.key}
                className={activeStatus === item.key ? "active" : ""}
                onClick={() => setActiveStatus(item.key)}
              >
                {item.icon}
                <span>{item.label}</span>
                {item.key !== "ALL" && item.key !== "MAINTENANCE" && (
                  <b>{orders.filter((order) => order.status === item.key).length}</b>
                )}
              </button>
            ))}
          </nav>
          <div className="ops-sidebar-footer">
            <SettingOutlined />
            <span>收起菜单</span>
            <CloseOutlined />
          </div>
        </aside>

        <section className="ops-workspace">
          <div className="ops-page-title">
            <div>
              <span>{selectedStore?.city || "门店"}区域 · 实时履约</span>
              <h1>门店履约指挥台</h1>
              <p>聚合取车、租赁中、待还车和异常处理，门店员工可以在同一屏完成每日交付动作。</p>
            </div>
            <Space>
              <Button icon={<ToolOutlined />} disabled={!selectedOrder} onClick={() => selectedOrder && setMaintenanceOrder(selectedOrder)}>
                登记维保
              </Button>
              <Button type="primary" icon={<CheckCircleOutlined />} disabled={!selectedOrder} onClick={() => handlePrimaryAction(selectedOrder)}>
                {actionLabel(selectedOrder)}
              </Button>
            </Space>
          </div>

          <div className="ops-kpi-grid">
            {realStaffMetrics.map((metric) => (
              <article className={`ops-kpi-card ${metric.tone}`} key={metric.label}>
                <div>
                  <span>{metric.label}</span>
                  <strong>{metric.value}</strong>
                  <small>{metric.trend}</small>
                </div>
                <i>{metric.icon}</i>
              </article>
            ))}
          </div>

          <section className="ops-panel staff-kanban">
            <div className="ops-section-heading">
              <div>
                <h2>履约队列</h2>
                <span>订单按状态自动进入对应处理队列</span>
              </div>
              <Button type="text" onClick={() => setActiveStatus("ALL")}>
                查看全部
              </Button>
            </div>
            <div className="staff-lane-grid">
              {laneKeys.map((lane) => {
                const laneOrders = orders.filter((order) => order.status === lane);
                return (
                  <div className={`staff-lane ${staffStatusMeta[lane].tone}`} key={lane}>
                    <div className="staff-lane-title">
                      <strong>{staffStatusMeta[lane].label}</strong>
                      <span>{laneOrders.length}</span>
                    </div>
                    {laneOrders.map((order) => (
                      <button className="staff-order-card" key={order.id} onClick={() => setSelectedOrderId(order.id)}>
                        <span>{order.orderNo}</span>
                        <strong>{order.carName}</strong>
                        <small>
                          {order.customerName} · {order.customerPhone}
                        </small>
                        <em>{order.pickupTime}</em>
                      </button>
                    ))}
                    {!laneOrders.length && <div className="staff-empty-lane">暂无订单</div>}
                  </div>
                );
              })}
            </div>
          </section>

          <section className="ops-panel staff-table-panel">
            <div className="ops-section-heading">
              <div>
                <h2>全部订单</h2>
                <span>{filteredOrders.length} 条符合当前筛选</span>
              </div>
              <Space wrap>
                <Select
                  value={activeStatus}
                  onChange={setActiveStatus}
                  options={[
                    { label: "全部订单", value: "ALL" },
                    ...laneKeys.map((key) => ({ label: staffStatusMeta[key].label, value: key })),
                    { label: "维修保养", value: "MAINTENANCE" },
                  ]}
                />
                <Button
                  onClick={() => {
                    setKeyword("");
                    setActiveStatus("ALL");
                  }}
                >
                  重置
                </Button>
                <Button type="primary" onClick={() => ordersQuery.refetch()}>
                  查询
                </Button>
              </Space>
            </div>
            <Table
              rowKey="id"
              columns={columns}
              dataSource={filteredOrders}
              loading={ordersQuery.isLoading || ordersQuery.isFetching}
              pagination={{ pageSize: 5 }}
              scroll={{ x: 920 }}
              locale={{ emptyText: <Empty description="当前门店暂无订单" /> }}
              rowClassName={(record) => (record.id === selectedOrder?.id ? "selected-row" : "")}
              onRow={(record) => ({ onClick: () => setSelectedOrderId(record.id) })}
            />
          </section>
        </section>

        <aside className="ops-detail-drawer staff-detail">
          {selectedOrder ? (
            <>
              <div className="detail-panel-header detail-motion">
                <div>
                  <span>订单详情</span>
                  <h2>{selectedOrder.orderNo}</h2>
                </div>
                <Button type="text" icon={<CloseOutlined />} />
              </div>
              <Space className="detail-motion" wrap>
                <Tag color={staffStatusMeta[selectedOrder.status].color}>{staffStatusMeta[selectedOrder.status].label}</Tag>
                <Tag color="blue">后端实时</Tag>
                <Tag>{selectedOrder.assignee} 跟进</Tag>
              </Space>
              <div className="ops-customer-card detail-motion">
                <UserOutlined />
                <div>
                  <strong>{selectedOrder.customerName}</strong>
                  <span>{selectedOrder.customerPhone}</span>
                </div>
                <Button type="link">联系</Button>
              </div>
              <div className="detail-car-card detail-motion">
                <img src={selectedOrder.image} alt={selectedOrder.carName} />
                <div>
                  <strong>{selectedOrder.carName}</strong>
                  <span>{selectedOrder.carModel}</span>
                  <small>
                    {selectedOrder.plateNumber} · {selectedOrder.mileage.toLocaleString("zh-CN")} km
                  </small>
                </div>
              </div>
              <div className="detail-motion">
                <h3 className="detail-subtitle">取还车信息</h3>
                <Timeline
                  items={[
                    {
                      color: "#2563eb",
                      content: (
                        <span>
                          取车 {selectedOrder.pickupTime}
                          <br />
                          {selectedOrder.pickupStore}
                        </span>
                      ),
                    },
                    {
                      color: selectedOrder.status === "COMPLETED" ? "#22c55e" : "#cbd5e1",
                      content: (
                        <span>
                          还车 {selectedOrder.returnTime}
                          <br />
                          {selectedOrder.returnStore}
                        </span>
                      ),
                    },
                  ]}
                />
              </div>
              <div className="service-checklist detail-motion">
                <h3 className="detail-subtitle">服务清单</h3>
                {selectedOrder.checklist.map((item) => (
                  <div className={item.ok ? "checked" : ""} key={item.label}>
                    <CheckCircleOutlined />
                    <span>{item.label}</span>
                    <strong>{item.ok ? "正常" : "待检查"}</strong>
                  </div>
                ))}
              </div>
              <div className="amount-box detail-motion">
                <span>应收金额</span>
                <strong>{formatCurrency(selectedOrder.amount)}</strong>
                <small>{selectedOrder.note}</small>
              </div>
              <div className="detail-action-grid detail-motion">
                <Button type="primary" size="large" onClick={() => handlePrimaryAction(selectedOrder)}>
                  {actionLabel(selectedOrder)}
                </Button>
                <Button
                  size="large"
                  onClick={() => {
                    setMaintenanceOrder(selectedOrder);
                    maintenanceForm.setFieldsValue({ type: "REPAIR", description: selectedOrder.note, cost: 0 });
                  }}
                >
                  登记维保
                </Button>
              </div>
            </>
          ) : (
            <Empty description={selectedStore ? "该门店暂无履约订单" : "请先选择门店"} />
          )}
        </aside>
      </div>

      <Modal
        open={!!confirmOrder}
        title={confirmOrder ? actionLabel(confirmOrder) : ""}
        onOk={completeOrderAction}
        confirmLoading={pickupMutation.isPending || returnMutation.isPending}
        onCancel={() => setConfirmOrder(null)}
        okText="确认提交"
        cancelText="取消"
      >
        {confirmOrder && (
          <div className="staff-modal-content">
            <strong>{confirmOrder.carName}</strong>
            <span>
              {confirmOrder.orderNo} · {confirmOrder.customerName}
            </span>
            {confirmOrder.checklist.map((item) => (
              <Checkbox checked={item.ok} key={item.label}>
                {item.label}
              </Checkbox>
            ))}
          </div>
        )}
      </Modal>

      <Modal
        open={!!maintenanceOrder}
        title="登记维修保养"
        onOk={completeMaintenance}
        confirmLoading={maintenanceMutation.isPending}
        onCancel={() => setMaintenanceOrder(null)}
        okText="创建工单"
        cancelText="取消"
      >
        {maintenanceOrder && (
          <Form form={maintenanceForm} layout="vertical" className="staff-modal-content" initialValues={{ type: "REPAIR", cost: 0 }}>
            <strong>{maintenanceOrder.carName}</strong>
            <span>
              {maintenanceOrder.plateNumber} · {maintenanceOrder.orderNo}
            </span>
            <Form.Item name="type" label="维保类型">
              <Select
                options={[
                  { label: "维修", value: "REPAIR" },
                  { label: "保养", value: "MAINTENANCE" },
                ]}
              />
            </Form.Item>
            <Form.Item name="cost" label="预估费用">
              <Input type="number" min={0} prefix="￥" />
            </Form.Item>
            <Form.Item name="description" label="问题说明">
              <Input.TextArea rows={4} />
            </Form.Item>
          </Form>
        )}
      </Modal>
    </main>
  );
}
