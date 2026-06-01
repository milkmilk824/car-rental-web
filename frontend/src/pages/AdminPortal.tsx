import {
  BellOutlined,
  CheckCircleOutlined,
  CloseOutlined,
  DownOutlined,
  EditOutlined,
  ExportOutlined,
  LogoutOutlined,
  MenuOutlined,
  PlusOutlined,
  SearchOutlined,
  UploadOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, Empty, Form, Input, InputNumber, Modal, Select, Space, Table, Tag, Upload } from "antd";
import type { ColumnsType } from "antd/es/table";
import type { UploadProps } from "antd";
import * as echarts from "echarts";
import gsap from "gsap";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import {
  adminMetrics,
  adminNavItems,
  hotVehicleStats,
  revenueTrend,
  statusTone,
  type AdminRecord,
  type AdminSection,
} from "../data/opsMock";
import { useAuth } from "../state/useAuth";
import type {
  Car,
  CarStatus,
  Comment,
  CommentStatus,
  Contract,
  ContractStatus,
  MaintenanceRecord,
  OrderStatus,
  PayStatus,
  PaymentOrder,
  RentalOrder,
  Store,
  StoreStaffBinding,
  StoreStatus,
  User,
  UserRole,
  UserStatus,
} from "../types";

type AdminEntity = Car | Store | RentalOrder | User | PaymentOrder | Contract | Comment | MaintenanceRecord;
type ContactInfo = {
  title: string;
  name: string;
  phone?: string;
  email?: string;
  note?: string;
};

interface AdminLiveRecord extends AdminRecord {
  entityId: number;
  section: AdminSection;
  raw: AdminEntity;
}

const fallbackImage = "/images/customer-hero-road.png";
const emptyCars: Car[] = [];
const emptyStores: Store[] = [];
const emptyOrders: RentalOrder[] = [];
const emptyUsers: User[] = [];
const emptyPayments: PaymentOrder[] = [];
const emptyComments: Comment[] = [];
const emptyContracts: Contract[] = [];
const emptyMaintenance: MaintenanceRecord[] = [];
const emptyStoreStaffBindings: StoreStaffBinding[] = [];
const editableSections: AdminSection[] = ["cars", "stores", "users", "maintenance"];
const creatableSections: AdminSection[] = ["cars", "stores", "users", "maintenance"];

const orderStatusLabel: Record<OrderStatus, string> = {
  PENDING_PAYMENT: "待支付",
  PENDING_PICKUP: "待取车",
  RENTING: "进行中",
  PENDING_RETURN: "待还车",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
  EXCEPTION: "异常",
};

const carStatusLabel: Record<CarStatus, string> = {
  AVAILABLE: "可租",
  RESERVED: "已预订",
  RENTING: "出租中",
  REPAIRING: "维修中",
  MAINTAINING: "保养中",
  OFFLINE: "下架",
};

const userStatusLabel: Record<UserStatus, string> = {
  ACTIVE: "正常",
  DISABLED: "禁用",
};

const storeStatusLabel: Record<StoreStatus, string> = {
  OPEN: "营业中",
  CLOSED: "暂停营业",
};

const payStatusLabel: Record<PayStatus, string> = {
  WAITING: "待支付",
  SUCCESS: "成功",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
  CLOSED: "已关闭",
};

const contractStatusLabel: Record<ContractStatus, string> = {
  UNSIGNED: "待签署",
  SIGNED: "已签署",
  ARCHIVED: "已归档",
};

const commentStatusLabel: Record<CommentStatus, string> = {
  PENDING: "待审核",
  APPROVED: "已展示",
  REMOVED: "已移除",
};

const roleLabel: Record<UserRole, string> = {
  USER: "个人用户",
  STORE_STAFF: "门店员工",
  ADMIN: "管理员",
};

function moduleTitle(section: AdminSection) {
  return adminNavItems.find((item) => item.key === section)?.label || "数据看板";
}

function tableSection(section: AdminSection): AdminSection {
  return section === "dashboard" ? "orders" : section;
}

function formatMoney(value: number | undefined) {
  return `￥${Number(value || 0).toLocaleString("zh-CN", { maximumFractionDigits: 2 })}`;
}

function formatDateTime(value?: string) {
  if (!value) return "暂无";
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString("zh-CN", {
    month: "2-digit",
    day: "2-digit",
    hour: "2-digit",
    minute: "2-digit",
  });
}

function imageOf(car?: Car) {
  return car?.imageUrls?.[0] || fallbackImage;
}

function CarImageUploader({
  imageUrls,
  onChange,
  message,
}: {
  imageUrls: string[];
  onChange: (urls: string[]) => void;
  message: ReturnType<typeof App.useApp>["message"];
}) {
  const uploadProps: UploadProps = {
    accept: "image/*",
    showUploadList: false,
    customRequest: async (options) => {
      try {
        const result = await api.uploadCarImage(options.file as File);
        onChange([...imageUrls, result.url]);
        options.onSuccess?.(result);
        message.success("车辆图片已上传");
      } catch (error) {
        const uploadError = error instanceof Error ? error : new Error("图片上传失败");
        options.onError?.(uploadError);
        message.error(uploadError.message);
      }
    },
  };

  return (
    <div className="car-image-uploader">
      <Upload {...uploadProps}>
        <Button icon={<UploadOutlined />}>上传车辆图片</Button>
      </Upload>
      {imageUrls.length ? (
        <div className="uploaded-image-list">
          {imageUrls.map((url) => (
            <div key={url}>
              <img src={url} alt="车辆图片" />
              <Button type="link" danger size="small" onClick={() => onChange(imageUrls.filter((item) => item !== url))}>
                移除
              </Button>
            </div>
          ))}
        </div>
      ) : (
        <span>支持 jpg、png、webp、gif、svg，保存车辆后会用于前台展示。</span>
      )}
    </div>
  );
}

function userName(user: User) {
  return user.realName || user.username || `用户 ${user.id}`;
}

function carRecord(car: Car): AdminLiveRecord {
  return {
    id: car.plateNumber || `CAR${car.id}`,
    entityId: car.id,
    section: "cars",
    raw: car,
    primary: car.carName,
    secondary: `${car.brand} ${car.model}`,
    category: car.category?.categoryName || "未分类",
    store: car.store?.storeName || "未分配门店",
    amount: `${formatMoney(car.pricePerDay)}/天`,
    status: carStatusLabel[car.status],
    payStatus: `押金 ${formatMoney(car.deposit)}`,
    time: `${Number(car.mileage || 0).toLocaleString("zh-CN")} km`,
    image: imageOf(car),
    extra: car.description || `车辆 ID ${car.id}`,
  };
}

function storeRecord(store: Store): AdminLiveRecord {
  return {
    id: `ST${store.id.toString().padStart(4, "0")}`,
    entityId: store.id,
    section: "stores",
    raw: store,
    primary: store.storeName,
    secondary: store.address,
    category: store.city,
    store: store.city,
    amount: store.phone || "暂无电话",
    status: storeStatusLabel[store.status],
    payStatus: store.businessHours || "营业时间未设置",
    time: `门店 ID ${store.id}`,
    image: "/images/home-hero-road.png",
    extra: `${store.city} · ${store.address}`,
  };
}

function orderRecord(order: RentalOrder): AdminLiveRecord {
  const amount = Number(order.totalAmount || 0) + Number(order.depositAmount || 0);
  return {
    id: order.orderNo,
    entityId: order.id,
    section: "orders",
    raw: order,
    primary: userName(order.user),
    secondary: order.user.phone || order.user.username,
    category: order.car.carName,
    store: order.pickupStore.storeName,
    amount: formatMoney(amount),
    status: orderStatusLabel[order.status],
    payStatus: `租期 ${order.rentalDays} 天`,
    time: formatDateTime(order.startTime),
    image: imageOf(order.car),
    extra: `取车 ${formatDateTime(order.startTime)}，还车 ${formatDateTime(order.endTime)}`,
  };
}

function userRecord(user: User): AdminLiveRecord {
  return {
    id: `U${user.id.toString().padStart(5, "0")}`,
    entityId: user.id,
    section: "users",
    raw: user,
    primary: userName(user),
    secondary: `${user.username} · ${user.phone || "暂无手机号"}`,
    category: roleLabel[user.role],
    store: user.email || roleLabel[user.role],
    amount: user.driverLicenseNo ? "驾照已认证" : "待认证",
    status: userStatusLabel[user.status],
    payStatus: user.idCard ? "实名信息已留存" : "未实名",
    time: `用户 ID ${user.id}`,
    image: fallbackImage,
    extra: user.email || "暂无邮箱",
  };
}

function paymentRecord(payment: PaymentOrder): AdminLiveRecord {
  return {
    id: payment.paymentNo,
    entityId: payment.id,
    section: "payments",
    raw: payment,
    primary: `订单 ${payment.orderId}`,
    secondary: `${payment.payType} · 用户 ${payment.userId}`,
    category: "租车费用",
    store: `用户 ${payment.userId}`,
    amount: formatMoney(payment.payAmount),
    status: payStatusLabel[payment.payStatus],
    payStatus: payment.transactionNo || "暂无交易号",
    time: formatDateTime(payment.payTime),
    image: fallbackImage,
    extra: `支付单 ID ${payment.id}`,
  };
}

function contractRecord(contract: Contract): AdminLiveRecord {
  return {
    id: contract.contractNo,
    entityId: contract.id,
    section: "contracts",
    raw: contract,
    primary: `订单 ${contract.orderId} 租赁合同`,
    secondary: `用户 ${contract.userId}`,
    category: "电子合同",
    store: contract.contractUrl,
    amount: "-",
    status: contractStatusLabel[contract.signStatus],
    payStatus: contract.signStatus,
    time: `合同 ID ${contract.id}`,
    image: "/images/home-hero-road.png",
    extra: contract.contractUrl,
  };
}

function commentRecord(comment: Comment): AdminLiveRecord {
  return {
    id: `CM${comment.id.toString().padStart(6, "0")}`,
    entityId: comment.id,
    section: "comments",
    raw: comment,
    primary: comment.username,
    secondary: `车辆 ${comment.carId} · ${comment.score} 星`,
    category: "服务评价",
    store: `订单 ${comment.orderId}`,
    amount: comment.score.toFixed(1),
    status: commentStatusLabel[comment.status],
    payStatus: comment.content ? "含文字评价" : "仅评分",
    time: formatDateTime(comment.createTime),
    image: fallbackImage,
    extra: comment.content || "用户未填写评价内容。",
  };
}

function maintenanceRecord(record: MaintenanceRecord): AdminLiveRecord {
  return {
    id: `MT${record.id.toString().padStart(6, "0")}`,
    entityId: record.id,
    section: "maintenance",
    raw: record,
    primary: `车辆 ${record.carId}`,
    secondary: record.description || (record.type === "REPAIR" ? "维修记录" : "保养记录"),
    category: record.type === "REPAIR" ? "维修" : "保养",
    store: `车辆 ID ${record.carId}`,
    amount: formatMoney(record.cost),
    status: "进行中",
    payStatus: "维保记录",
    time: formatDateTime(record.recordTime),
    image: "/images/home-hero-road.png",
    extra: record.description || "暂无说明",
  };
}

function paymentTrend(payments: PaymentOrder[]) {
  const successful = payments.filter((item) => item.payStatus === "SUCCESS" && item.payTime);
  if (!successful.length) return revenueTrend;
  const grouped = successful.reduce<Record<string, number>>((acc, item) => {
    const date = item.payTime?.slice(5, 10) || "未知";
    acc[date] = (acc[date] || 0) + Number(item.payAmount || 0);
    return acc;
  }, {});
  return Object.entries(grouped)
    .sort(([a], [b]) => a.localeCompare(b))
    .slice(-8)
    .map(([date, value]) => ({ date, value }));
}

function actionLabel(section: AdminSection, record?: AdminLiveRecord) {
  if (!record) return "暂无操作";
  if (section === "cars") return "切换上下架";
  if (section === "stores") return "切换营业";
  if (section === "orders") {
    const order = record.raw as RentalOrder;
    if (order.status === "PENDING_PAYMENT") return "取消订单";
    if (order.status === "PENDING_PICKUP") return "确认取车";
    if (order.status === "RENTING" || order.status === "PENDING_RETURN") return "确认还车";
    if (order.status === "COMPLETED") return "生成合同";
    return "查看订单";
  }
  if (section === "users") return "切换状态";
  if (section === "payments") return "发起退款";
  if (section === "contracts") return "签署合同";
  if (section === "comments") return "移除评价";
  return "查看记录";
}

function canEditSection(section: AdminSection) {
  return editableSections.includes(section);
}

function canCreateSection(section: AdminSection) {
  return creatableSections.includes(section);
}

function csvCell(value: unknown) {
  const raw = String(value ?? "");
  return `"${raw.replaceAll('"', '""')}"`;
}

function buildContactInfo(record: AdminLiveRecord, users: User[]): ContactInfo | null {
  if (record.section === "orders") {
    const order = record.raw as RentalOrder;
    return {
      title: "订单用户",
      name: userName(order.user),
      phone: order.user.phone,
      email: order.user.email,
      note: `${order.orderNo} · ${order.car.carName}`,
    };
  }
  if (record.section === "users") {
    const target = record.raw as User;
    return {
      title: roleLabel[target.role],
      name: userName(target),
      phone: target.phone,
      email: target.email,
      note: target.driverLicenseNo ? "驾照已认证" : "驾照待认证",
    };
  }
  if (record.section === "payments") {
    const payment = record.raw as PaymentOrder;
    const target = users.find((item) => item.id === payment.userId);
    return {
      title: "支付用户",
      name: target ? userName(target) : `用户 ${payment.userId}`,
      phone: target?.phone,
      email: target?.email,
      note: `${payment.paymentNo} · ${payStatusLabel[payment.payStatus]}`,
    };
  }
  if (record.section === "contracts") {
    const contract = record.raw as Contract;
    const target = users.find((item) => item.id === contract.userId);
    return {
      title: "合同用户",
      name: target ? userName(target) : `用户 ${contract.userId}`,
      phone: target?.phone,
      email: target?.email,
      note: `${contract.contractNo} · ${contractStatusLabel[contract.signStatus]}`,
    };
  }
  if (record.section === "comments") {
    const comment = record.raw as Comment;
    const target = users.find((item) => item.id === comment.userId);
    return {
      title: "评价用户",
      name: target ? userName(target) : comment.username,
      phone: target?.phone,
      email: target?.email,
      note: `订单 ${comment.orderId} · ${comment.score} 星`,
    };
  }
  if (record.section === "stores") {
    const store = record.raw as Store;
    return {
      title: "门店联系方式",
      name: store.storeName,
      phone: store.phone,
      note: `${store.city} · ${store.address}`,
    };
  }
  if (record.section === "cars") {
    const car = record.raw as Car;
    return {
      title: "车辆所属门店",
      name: car.store.storeName,
      phone: car.store.phone,
      note: `${car.plateNumber} · ${car.carName}`,
    };
  }
  return null;
}

/* ==================== Edit Form Components ==================== */

function EditCarForm({ record, onDone, onCancel }: { record: AdminLiveRecord; onDone: () => void; onCancel: () => void }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const car = record.raw as Car;
  const [imageUrls, setImageUrls] = useState<string[]>(car.imageUrls || []);
  const editMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.updateCar(car.id, {
        carName: String(values.carName || ""),
        brand: String(values.brand || ""),
        model: String(values.model || ""),
        categoryId: Number(values.categoryId || 1),
        plateNumber: String(values.plateNumber || ""),
        storeId: Number(values.storeId || 1),
        pricePerDay: Number(values.pricePerDay || 0),
        deposit: Number(values.deposit || 0),
        status: (values.status as CarStatus) || "AVAILABLE",
        mileage: Number(values.mileage || 0),
        description: values.description ? String(values.description) : undefined,
        imageUrls,
      }),
    onSuccess: () => { message.success("车辆已更新"); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{
      carName: car.carName, brand: car.brand, model: car.model,
      categoryId: car.category?.id, plateNumber: car.plateNumber,
      storeId: car.store?.id, pricePerDay: car.pricePerDay,
      deposit: car.deposit, status: car.status, mileage: car.mileage,
      description: car.description,
    }} onFinish={(v) => { setLoading(true); editMutation.mutate(v); }}>
      <Form.Item name="carName" label="车辆名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="brand" label="品牌" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="model" label="型号" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="plateNumber" label="车牌号" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="categoryId" label="分类ID"><InputNumber style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="storeId" label="门店ID"><InputNumber style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="pricePerDay" label="日租金" rules={[{ required: true }]}><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="deposit" label="押金"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="mileage" label="里程(km)"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(carStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
      <Form.Item label="车辆图片">
        <CarImageUploader imageUrls={imageUrls} onChange={setImageUrls} message={message} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>保存</Button>
      </Space>
    </Form>
  );
}

function EditStoreForm({ record, onDone, onCancel }: { record: AdminLiveRecord; onDone: () => void; onCancel: () => void }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const store = record.raw as Store;
  const editMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.updateStore(store.id, {
        storeName: String(values.storeName || ""),
        city: String(values.city || ""),
        address: String(values.address || ""),
        phone: values.phone ? String(values.phone) : undefined,
        businessHours: values.businessHours ? String(values.businessHours) : undefined,
        status: (values.status as StoreStatus) || "OPEN",
      }),
    onSuccess: () => { message.success("门店已更新"); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{
      storeName: store.storeName, city: store.city, address: store.address,
      phone: store.phone, businessHours: store.businessHours, status: store.status,
    }} onFinish={(v) => { setLoading(true); editMutation.mutate(v); }}>
      <Form.Item name="storeName" label="门店名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="city" label="城市" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="address" label="地址" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="phone" label="电话"><Input /></Form.Item>
      <Form.Item name="businessHours" label="营业时间"><Input placeholder="如 09:00-21:00" /></Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(storeStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>保存</Button>
      </Space>
    </Form>
  );
}

function EditUserForm({ record, onDone, onCancel }: { record: AdminLiveRecord; onDone: () => void; onCancel: () => void }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const user = record.raw as User;
  const editMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.updateUser(user.id, {
        phone: values.phone ? String(values.phone) : undefined,
        email: values.email ? String(values.email) : undefined,
        realName: values.realName ? String(values.realName) : undefined,
        role: (values.role as UserRole) || user.role,
        status: (values.status as UserStatus) || user.status,
      }),
    onSuccess: () => { message.success("用户已更新"); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{
      phone: user.phone || "", email: user.email || "",
      realName: user.realName || "", role: user.role, status: user.status,
    }} onFinish={(v) => { setLoading(true); editMutation.mutate(v); }}>
      <Form.Item name="realName" label="真实姓名"><Input /></Form.Item>
      <Form.Item name="phone" label="手机号"><Input /></Form.Item>
      <Form.Item name="email" label="邮箱"><Input /></Form.Item>
      <Form.Item name="role" label="角色">
        <Select options={Object.entries(roleLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(userStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>保存</Button>
      </Space>
    </Form>
  );
}

function EditMaintenanceForm({ record, onDone, onCancel }: { record: AdminLiveRecord; onDone: () => void; onCancel: () => void }) {
  const { message } = App.useApp();
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const mt = record.raw as MaintenanceRecord;
  const editMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.updateMaintenance(mt.id, {
        carId: mt.carId,
        type: (values.type as "REPAIR" | "MAINTENANCE") || mt.type,
        description: values.description ? String(values.description) : undefined,
        cost: Number(values.cost || 0),
        recordTime: values.recordTime ? String(values.recordTime) : mt.recordTime,
      }),
    onSuccess: () => { message.success("维保记录已更新"); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{
      type: mt.type, description: mt.description || "",
      cost: mt.cost, recordTime: undefined,
    }} onFinish={(v) => { setLoading(true); editMutation.mutate(v); }}>
      <Form.Item name="type" label="类型" rules={[{ required: true }]}>
        <Select options={[{ label: "维修", value: "REPAIR" }, { label: "保养", value: "MAINTENANCE" }]} />
      </Form.Item>
      <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
      <Form.Item name="cost" label="费用"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="recordTime" label="记录时间"><Input placeholder="保持原有时间" /></Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>保存</Button>
      </Space>
    </Form>
  );
}

/* ==================== Create Form Selector ==================== */

function CreateFormSelector({ section, stores, onDone, onCancel }: { section: AdminSection; stores: Store[]; onDone: () => void; onCancel: () => void }) {
  const { message } = App.useApp();
  if (section === "cars") return <CreateCarForm stores={stores} message={message} onDone={onDone} onCancel={onCancel} />;
  if (section === "stores") return <CreateStoreForm message={message} onDone={onDone} onCancel={onCancel} />;
  if (section === "users") return <CreateUserForm message={message} onDone={onDone} onCancel={onCancel} />;
  if (section === "orders" || section === "contracts" || section === "payments" || section === "comments") {
    return (
      <div style={{ padding: 24, textAlign: "center", color: "#718096" }}>
        <p>{moduleTitle(section)}不支持从管理后台直接创建，请通过业务流程自动生成。</p>
        <Button onClick={onCancel}>关闭</Button>
      </div>
    );
  }
  if (section === "maintenance") return <CreateMaintenanceForm message={message} onDone={onDone} onCancel={onCancel} />;
  return (
    <div style={{ padding: 24, textAlign: "center", color: "#718096" }}>
      <p>该模块由订单、支付或履约流程自动生成。</p>
      <Button onClick={onCancel}>关闭</Button>
    </div>
  );
}

function CreateCarForm({ stores, message, onDone, onCancel }: { stores: Store[]; message: ReturnType<typeof App.useApp>["message"]; onDone: () => void; onCancel: () => void }) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const [imageUrls, setImageUrls] = useState<string[]>([]);
  const queryClient = useQueryClient();
  const catsQuery = useQuery({ queryKey: ["categories"], queryFn: api.categories });
  const createMutation = useMutation({
    mutationFn: async (values: Record<string, unknown>) => {
      let categoryId = Number(values.categoryId || 0);
      const categoryName = String(values.categoryName || "").trim();
      if (categoryName) {
        const category = await api.createCategory({
          categoryName,
          description: values.categoryDescription ? String(values.categoryDescription) : undefined,
        });
        categoryId = category.id;
      }
      if (!categoryId) {
        throw new Error("请先选择或创建车辆分类");
      }
      const storeId = Number(values.storeId || 0);
      if (!storeId) {
        throw new Error("请先创建并选择门店");
      }
      return api.createCar({
        carName: String(values.carName || ""), brand: String(values.brand || ""),
        model: String(values.model || ""), categoryId,
        plateNumber: String(values.plateNumber || ""), storeId,
        pricePerDay: Number(values.pricePerDay || 0), deposit: Number(values.deposit || 0),
        status: (values.status as CarStatus) || "AVAILABLE", mileage: Number(values.mileage || 0),
        description: values.description ? String(values.description) : undefined,
        imageUrls: imageUrls.length ? imageUrls : undefined,
      });
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["categories"] });
      message.success("车辆已创建");
      form.resetFields();
      setImageUrls([]);
      onDone();
    },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{ status: "AVAILABLE", mileage: 0, pricePerDay: 0, deposit: 0 }}
      onFinish={(v) => { setLoading(true); createMutation.mutate(v); }}>
      <Form.Item name="carName" label="车辆名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="brand" label="品牌" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="model" label="型号" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="plateNumber" label="车牌号" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="categoryId" label="分类">
        <Select
          allowClear
          loading={catsQuery.isLoading}
          placeholder="选择已有分类"
          options={(catsQuery.data || []).map((c) => ({ label: c.categoryName, value: c.id }))}
        />
      </Form.Item>
      <Form.Item name="categoryName" label="新分类名称">
        <Input placeholder="空库首次建车时可在这里创建分类，填写后优先使用新分类" />
      </Form.Item>
      <Form.Item name="categoryDescription" label="新分类说明">
        <Input />
      </Form.Item>
      <Form.Item name="storeId" label="门店" rules={[{ required: true }]}>
        <Select
          disabled={!stores.length}
          placeholder={stores.length ? "选择门店" : "请先在门店管理创建门店"}
          options={stores.map((s) => ({ label: s.storeName, value: s.id }))}
        />
      </Form.Item>
      <Form.Item name="pricePerDay" label="日租金" rules={[{ required: true }]}><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="deposit" label="押金"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="mileage" label="里程(km)"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(carStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Form.Item name="description" label="描述"><Input.TextArea rows={2} /></Form.Item>
      <Form.Item label="车辆图片">
        <CarImageUploader imageUrls={imageUrls} onChange={setImageUrls} message={message} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>创建</Button>
      </Space>
    </Form>
  );
}

function CreateStoreForm({ message, onDone, onCancel }: { message: ReturnType<typeof App.useApp>["message"]; onDone: () => void; onCancel: () => void }) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const createMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.createStore({
        storeName: String(values.storeName || ""), city: String(values.city || ""),
        address: String(values.address || ""), phone: values.phone ? String(values.phone) : undefined,
        businessHours: values.businessHours ? String(values.businessHours) : undefined,
        status: (values.status as StoreStatus) || "OPEN",
      }),
    onSuccess: () => { message.success("门店已创建"); form.resetFields(); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{ status: "OPEN" }}
      onFinish={(v) => { setLoading(true); createMutation.mutate(v); }}>
      <Form.Item name="storeName" label="门店名称" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="city" label="城市" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="address" label="地址" rules={[{ required: true }]}><Input /></Form.Item>
      <Form.Item name="phone" label="电话"><Input /></Form.Item>
      <Form.Item name="businessHours" label="营业时间"><Input placeholder="如 09:00-21:00" /></Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(storeStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>创建</Button>
      </Space>
    </Form>
  );
}

function CreateUserForm({ message, onDone, onCancel }: { message: ReturnType<typeof App.useApp>["message"]; onDone: () => void; onCancel: () => void }) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const createMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.createUser({
        username: String(values.username || ""), password: String(values.password || ""),
        phone: values.phone ? String(values.phone) : undefined,
        email: values.email ? String(values.email) : undefined,
        role: (values.role as UserRole) || "USER",
        status: (values.status as UserStatus) || "ACTIVE",
      }),
    onSuccess: () => { message.success("用户已创建"); form.resetFields(); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{ role: "USER", status: "ACTIVE" }}
      onFinish={(v) => { setLoading(true); createMutation.mutate(v); }}>
      <Form.Item name="username" label="用户名" rules={[{ required: true, min: 3 }]}><Input /></Form.Item>
      <Form.Item name="password" label="密码" rules={[{ required: true, min: 6 }]}><Input.Password /></Form.Item>
      <Form.Item name="phone" label="手机号"><Input /></Form.Item>
      <Form.Item name="email" label="邮箱"><Input /></Form.Item>
      <Form.Item name="role" label="角色">
        <Select options={Object.entries(roleLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Form.Item name="status" label="状态">
        <Select options={Object.entries(userStatusLabel).map(([k, v]) => ({ label: v, value: k }))} />
      </Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>创建</Button>
      </Space>
    </Form>
  );
}

function CreateMaintenanceForm({ message, onDone, onCancel }: { message: ReturnType<typeof App.useApp>["message"]; onDone: () => void; onCancel: () => void }) {
  const [form] = Form.useForm();
  const [loading, setLoading] = useState(false);
  const createMutation = useMutation({
    mutationFn: (values: Record<string, unknown>) =>
      api.createMaintenance({
        carId: Number(values.carId || 0), type: (values.type as "REPAIR" | "MAINTENANCE") || "MAINTENANCE",
        description: values.description ? String(values.description) : undefined,
        cost: Number(values.cost || 0), recordTime: new Date().toISOString(),
      }),
    onSuccess: () => { message.success("维保记录已创建"); form.resetFields(); onDone(); },
    onError: (err: Error) => message.error(err.message),
    onSettled: () => setLoading(false),
  });
  return (
    <Form form={form} layout="vertical" initialValues={{ type: "MAINTENANCE", cost: 0 }}
      onFinish={(v) => { setLoading(true); createMutation.mutate(v); }}>
      <Form.Item name="carId" label="车辆ID" rules={[{ required: true }]}><InputNumber style={{ width: "100%" }} /></Form.Item>
      <Form.Item name="type" label="类型" rules={[{ required: true }]}>
        <Select options={[{ label: "维修", value: "REPAIR" }, { label: "保养", value: "MAINTENANCE" }]} />
      </Form.Item>
      <Form.Item name="description" label="描述"><Input.TextArea rows={3} /></Form.Item>
      <Form.Item name="cost" label="费用"><InputNumber style={{ width: "100%" }} min={0} /></Form.Item>
      <Space style={{ justifyContent: "flex-end", width: "100%" }}>
        <Button onClick={onCancel}>取消</Button>
        <Button type="primary" htmlType="submit" loading={loading}>创建</Button>
      </Space>
    </Form>
  );
}

/* ==================== AdminPortal ==================== */

export function AdminPortal() {
  const { message } = App.useApp();
  const { user, logout } = useAuth();
  const queryClient = useQueryClient();
  const pageRef = useRef<HTMLDivElement>(null);
  const revenueChartRef = useRef<HTMLDivElement>(null);
  const hotChartRef = useRef<HTMLDivElement>(null);
  const [activeSection, setActiveSection] = useState<AdminSection>("dashboard");
  const [selectedId, setSelectedId] = useState("");
  const [keyword, setKeyword] = useState("");
  const [editOpen, setEditOpen] = useState(false);
  const [createOpen, setCreateOpen] = useState(false);
  const [contactOpen, setContactOpen] = useState(false);
  const [storeFilter, setStoreFilter] = useState("all");
  const [statusFilter, setStatusFilter] = useState("all");
  const [trendDays, setTrendDays] = useState(30);
  const [staffUserId, setStaffUserId] = useState<number>();

  const dashboardQuery = useQuery({ queryKey: ["admin-dashboard"], queryFn: api.dashboard });
  const revenueTrendQuery = useQuery({ queryKey: ["admin-revenue-trend", trendDays], queryFn: () => api.revenueTrend(trendDays) });
  const carsQuery = useQuery({ queryKey: ["admin-cars"], queryFn: () => api.adminCars({ size: 100 }) });
  const storesQuery = useQuery({ queryKey: ["admin-stores"], queryFn: api.adminStores });
  const ordersQuery = useQuery({ queryKey: ["admin-orders"], queryFn: api.adminOrders });
  const usersQuery = useQuery({ queryKey: ["admin-users"], queryFn: api.adminUsers });
  const paymentsQuery = useQuery({ queryKey: ["admin-payments"], queryFn: api.adminPayments });
  const commentsQuery = useQuery({ queryKey: ["admin-comments"], queryFn: api.adminComments });
  const contractsQuery = useQuery({ queryKey: ["admin-contracts"], queryFn: api.adminContracts, retry: false });
  const maintenanceQuery = useQuery({ queryKey: ["admin-maintenance"], queryFn: api.adminMaintenance, retry: false });

  const cars = carsQuery.data?.items ?? emptyCars;
  const stores = storesQuery.data ?? emptyStores;
  const orders = ordersQuery.data ?? emptyOrders;
  const users = usersQuery.data ?? emptyUsers;
  const payments = paymentsQuery.data ?? emptyPayments;
  const comments = commentsQuery.data ?? emptyComments;
  const contracts = contractsQuery.data ?? emptyContracts;
  const maintenance = maintenanceQuery.data ?? emptyMaintenance;

  const recordsBySection = useMemo<Record<AdminSection, AdminLiveRecord[]>>(
    () => ({
      dashboard: [],
      cars: cars.map(carRecord),
      stores: stores.map(storeRecord),
      orders: orders.map(orderRecord),
      users: users.map(userRecord),
      payments: payments.map(paymentRecord),
      contracts: contracts.map(contractRecord),
      comments: comments.map(commentRecord),
      maintenance: maintenance.map(maintenanceRecord),
    }),
    [cars, comments, contracts, maintenance, orders, payments, stores, users],
  );

  const currentSection = tableSection(activeSection);
  const currentRecords = recordsBySection[currentSection];
  const storeFilterOptions = useMemo(
    () => [
      { label: "全部归属", value: "all" },
      ...Array.from(new Set(currentRecords.map((record) => record.store).filter(Boolean))).map((value) => ({
        label: value,
        value,
      })),
    ],
    [currentRecords],
  );
  const statusFilterOptions = useMemo(
    () => [
      { label: "全部状态", value: "all" },
      ...Array.from(new Set(currentRecords.map((record) => record.status).filter(Boolean))).map((value) => ({
        label: value,
        value,
      })),
    ],
    [currentRecords],
  );
  const visibleRecords = useMemo(() => {
    const normalized = keyword.trim().toLowerCase();
    return currentRecords.filter((record) => {
      const storeMatched = storeFilter === "all" || record.store === storeFilter;
      const statusMatched = statusFilter === "all" || record.status === statusFilter;
      const keywordMatched =
        !normalized ||
        [record.id, record.primary, record.secondary, record.category, record.store, record.status]
        .join(" ")
        .toLowerCase()
        .includes(normalized);
      return storeMatched && statusMatched && keywordMatched;
    });
  }, [currentRecords, keyword, statusFilter, storeFilter]);
  const selectedRecord = visibleRecords.find((record) => record.id === selectedId) || visibleRecords[0] || currentRecords[0];
  const selectedContact = selectedRecord ? buildContactInfo(selectedRecord, users) : null;
  const selectedStoreForStaff =
    selectedRecord?.section === "stores" ? (selectedRecord.raw as Store) : undefined;

  const storeStaffQuery = useQuery({
    queryKey: ["admin-store-staff", selectedStoreForStaff?.id],
    queryFn: () => api.storeStaff(selectedStoreForStaff!.id),
    enabled: Boolean(selectedStoreForStaff?.id),
  });
  const storeStaffBindings = storeStaffQuery.data ?? emptyStoreStaffBindings;
  const staffOptions = useMemo(
    () =>
      users
        .filter((item) => item.role === "STORE_STAFF" && !storeStaffBindings.some((binding) => binding.user.id === item.id))
        .map((item) => ({
          label: `${item.realName || item.username} · ${item.phone || item.username}`,
          value: item.id,
        })),
    [storeStaffBindings, users],
  );

  const bindStoreStaffMutation = useMutation({
    mutationFn: (userId: number) => {
      if (!selectedStoreForStaff) throw new Error("请先选择门店");
      return api.bindStoreStaff(selectedStoreForStaff.id, userId);
    },
    onSuccess: (_, userId) => {
      queryClient.invalidateQueries({ queryKey: ["admin-store-staff", selectedStoreForStaff?.id] });
      queryClient.invalidateQueries({ queryKey: ["my-stores"] });
      queryClient.invalidateQueries({ queryKey: ["admin-users"] });
      setStaffUserId(undefined);
      const staff = users.find((item) => item.id === userId);
      message.success(`${staff?.realName || staff?.username || "员工"} 已绑定到门店`);
    },
    onError: (error) => message.error(error.message),
  });

  const unbindStoreStaffMutation = useMutation({
    mutationFn: (userId: number) => {
      if (!selectedStoreForStaff) throw new Error("请先选择门店");
      return api.unbindStoreStaff(selectedStoreForStaff.id, userId);
    },
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["admin-store-staff", selectedStoreForStaff?.id] });
      queryClient.invalidateQueries({ queryKey: ["my-stores"] });
      message.success("门店员工已解绑");
    },
    onError: (error) => message.error(error.message),
  });

  const selectSection = (section: AdminSection) => {
    const nextSection = tableSection(section);
    const nextRecord = recordsBySection[nextSection][0];
    setActiveSection(section);
    setSelectedId(nextRecord?.id || "");
    setStoreFilter("all");
    setStatusFilter("all");
  };

  const liveMetrics = useMemo(() => {
    const dashboard = dashboardQuery.data;
    const monthRevenue = Number(dashboard?.monthRevenue || payments.filter((item) => item.payStatus === "SUCCESS").reduce((sum, item) => sum + Number(item.payAmount || 0), 0));
    return [
      {
        ...adminMetrics[0],
        value: String(dashboard?.todayOrders ?? orders.length),
        sub: `${orders.filter((item) => item.status === "PENDING_PICKUP").length} 单待取车`,
      },
      {
        ...adminMetrics[1],
        value: formatMoney(monthRevenue),
        sub: `${payments.filter((item) => item.payStatus === "SUCCESS").length} 笔成功支付`,
      },
      {
        ...adminMetrics[2],
        value: String(dashboard?.availableCars ?? cars.filter((item) => item.status === "AVAILABLE").length),
        sub: `出租中 ${dashboard?.rentingOrders ?? orders.filter((item) => item.status === "RENTING").length}`,
      },
      {
        ...adminMetrics[3],
        value: String(dashboard?.activeUsers ?? users.filter((item) => item.status === "ACTIVE").length),
        sub: `${users.filter((item) => item.role === "STORE_STAFF").length} 位门店员工`,
      },
    ];
  }, [cars, dashboardQuery.data, orders, payments, users]);

  const trendData = useMemo(() => {
    const liveTrend = revenueTrendQuery.data || [];
    if (liveTrend.length) {
      return liveTrend.map((item) => ({ date: item.date.slice(5), value: Number(item.revenue || 0) }));
    }
    return paymentTrend(payments);
  }, [payments, revenueTrendQuery.data]);
  const hotStats = useMemo(() => {
    const hotCars = dashboardQuery.data?.hotCars || [];
    if (!hotCars.length) return hotVehicleStats;
    return hotCars.map((item) => ({ name: item.carName, value: item.orderCount }));
  }, [dashboardQuery.data?.hotCars]);

  const invalidateAdminData = () => {
    [
      ["admin-dashboard"],
      ["admin-revenue-trend"],
      ["admin-cars"],
      ["admin-stores"],
      ["admin-orders"],
      ["admin-users"],
      ["admin-payments"],
      ["admin-comments"],
      ["admin-contracts"],
      ["admin-maintenance"],
      ["staff-orders"],
      ["stores"],
    ].forEach((queryKey) => queryClient.invalidateQueries({ queryKey }));
  };

  const actionMutation = useMutation({
    mutationFn: async (record: AdminLiveRecord) => {
      if (record.section === "cars") {
        const car = record.raw as Car;
        return api.updateCarStatus(car.id, car.status === "AVAILABLE" ? "OFFLINE" : "AVAILABLE");
      }
      if (record.section === "stores") {
        const store = record.raw as Store;
        return api.updateStore(store.id, {
          storeName: store.storeName,
          city: store.city,
          address: store.address,
          phone: store.phone,
          businessHours: store.businessHours,
          status: store.status === "OPEN" ? "CLOSED" : "OPEN",
        });
      }
      if (record.section === "orders") {
        const order = record.raw as RentalOrder;
        if (order.status === "PENDING_PAYMENT") {
          return api.cancelOrder(order.id);
        }
        if (order.status === "PENDING_PICKUP") {
          return api.confirmPickup(order.id);
        }
        if (order.status === "RENTING" || order.status === "PENDING_RETURN") {
          return api.confirmReturn(order.id);
        }
        if (order.status === "COMPLETED") {
          return api.generateContract(order.id);
        }
        throw new Error("当前订单状态无需处理");
      }
      if (record.section === "users") {
        const target = record.raw as User;
        return api.updateUserStatus(target.id, target.status === "ACTIVE" ? "DISABLED" : "ACTIVE");
      }
      if (record.section === "payments") {
        const payment = record.raw as PaymentOrder;
        if (payment.payStatus !== "SUCCESS" && payment.payStatus !== "REFUNDING") {
          throw new Error("当前支付状态不能退款");
        }
        return api.refundPayment({ paymentNo: payment.paymentNo, reason: "管理员后台退款" });
      }
      if (record.section === "contracts") {
        const contract = record.raw as Contract;
        if (contract.signStatus !== "UNSIGNED") {
          throw new Error("该合同无需签署");
        }
        return api.signContract(contract.id);
      }
      if (record.section === "comments") {
        const comment = record.raw as Comment;
        if (comment.status === "REMOVED") {
          throw new Error("评价已移除");
        }
        return api.deleteComment(comment.id);
      }
      throw new Error("该模块无需后台动作");
    },
    onSuccess: (_, record) => {
      invalidateAdminData();
      message.success(`${moduleTitle(record.section)}已更新`);
    },
    onError: (error) => message.error(error.message),
  });

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced) return;
    const context = gsap.context(() => {
      gsap.from(".ops-sidebar, .ops-topbar", { y: -18, autoAlpha: 0, duration: 0.5, ease: "power2.out" });
      gsap.from(".ops-page-title, .admin-kpi-card, .admin-chart-panel, .admin-data-panel, .ops-detail-drawer", {
        y: 26,
        autoAlpha: 0,
        duration: 0.55,
        stagger: 0.06,
        ease: "power2.out",
      });
      gsap.from(".admin-nav button", { x: -18, autoAlpha: 0, duration: 0.45, stagger: 0.04, delay: 0.1 });
    }, pageRef);
    return () => context.revert();
  }, []);

  useEffect(() => {
    if (activeSection !== "dashboard") return;
    if (!revenueChartRef.current || !hotChartRef.current) return;
    const revenueChart = echarts.init(revenueChartRef.current);
    const hotChart = echarts.init(hotChartRef.current);

    revenueChart.setOption({
      color: ["#2563eb"],
      grid: { top: 28, left: 46, right: 20, bottom: 34 },
      tooltip: { trigger: "axis" },
      xAxis: {
        type: "category",
        boundaryGap: false,
        data: trendData.map((item) => item.date),
        axisLabel: { color: "#718096" },
        axisLine: { lineStyle: { color: "#dbe5f0" } },
      },
      yAxis: {
        type: "value",
        axisLabel: { color: "#718096" },
        splitLine: { lineStyle: { color: "#edf2f7" } },
      },
      series: [
        {
          name: "收入（元）",
          type: "line",
          smooth: true,
          symbolSize: 8,
          data: trendData.map((item) => item.value),
          areaStyle: { color: "rgba(37, 99, 235, 0.12)" },
          lineStyle: { width: 3 },
        },
      ],
    });

    hotChart.setOption({
      color: ["#2563eb"],
      grid: { top: 18, left: 120, right: 36, bottom: 24 },
      tooltip: {},
      xAxis: {
        type: "value",
        axisLabel: { color: "#718096" },
        splitLine: { lineStyle: { color: "#edf2f7" } },
      },
      yAxis: {
        type: "category",
        inverse: true,
        data: hotStats.map((item) => item.name),
        axisLabel: { color: "#2d3748" },
      },
      series: [
        {
          type: "bar",
          data: hotStats.map((item) => item.value),
          barWidth: 14,
          itemStyle: { borderRadius: [0, 7, 7, 0] },
          label: { show: true, position: "right", color: "#64748b" },
        },
      ],
    });

    const resize = () => {
      revenueChart.resize();
      hotChart.resize();
    };
    window.addEventListener("resize", resize);
    return () => {
      window.removeEventListener("resize", resize);
      revenueChart.dispose();
      hotChart.dispose();
    };
  }, [activeSection, hotStats, trendData]);

  useEffect(() => {
    const reduced = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduced || !selectedRecord) return;
    gsap.fromTo(
      ".admin-detail .detail-motion",
      { x: 18, autoAlpha: 0 },
      { x: 0, autoAlpha: 1, duration: 0.3, stagger: 0.04, ease: "power2.out" },
    );
  }, [selectedId, selectedRecord]);

  const runPrimaryAction = () => {
    if (!selectedRecord) {
      message.info("当前模块暂无可操作数据");
      return;
    }
    actionMutation.mutate(selectedRecord);
  };

  const openCreateModal = () => {
    if (!canCreateSection(currentSection)) {
      message.info(`${moduleTitle(currentSection)}由业务流程生成，请在对应流程中处理`);
      return;
    }
    setCreateOpen(true);
  };

  const exportVisibleRecords = () => {
    if (!visibleRecords.length) {
      message.warning("当前筛选下暂无可导出数据");
      return;
    }
    const headers = ["编号", "主体信息", "说明", "类型", "归属", "金额/指标", "状态", "支付/说明", "时间", "备注"];
    const rows = visibleRecords.map((record) => [
      record.id,
      record.primary,
      record.secondary,
      record.category,
      record.store,
      record.amount,
      record.status,
      record.payStatus,
      record.time,
      record.extra,
    ]);
    const csv = [headers, ...rows].map((row) => row.map(csvCell).join(",")).join("\n");
    const blob = new Blob([`\uFEFF${csv}`], { type: "text/csv;charset=utf-8" });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.href = url;
    link.download = `drivepilot-${currentSection}-${new Date().toISOString().slice(0, 10)}.csv`;
    link.click();
    URL.revokeObjectURL(url);
    message.success(`已导出 ${visibleRecords.length} 条${moduleTitle(currentSection)}`);
  };

  const columns = useMemo<ColumnsType<AdminLiveRecord>>(
    () => [
      {
        title: "编号",
        dataIndex: "id",
        width: 168,
        render: (value: string, record) => (
          <button className="text-link-button" onClick={() => setSelectedId(record.id)}>
            {value}
          </button>
        ),
      },
      {
        title: "主体信息",
        render: (_, record) => (
          <div className="ops-table-car admin-record-cell">
            <img src={record.image} alt={record.primary} />
            <div>
              <strong>{record.primary}</strong>
              <span>{record.secondary}</span>
            </div>
          </div>
        ),
      },
      { title: "类型", dataIndex: "category", width: 108 },
      { title: "门店 / 归属", dataIndex: "store", width: 158 },
      { title: "金额 / 指标", dataIndex: "amount", width: 120, render: (value: string) => <strong className="amount-text">{value}</strong> },
      {
        title: "状态",
        width: 108,
        render: (_, record) => <Tag color={statusTone(record.status)}>{record.status}</Tag>,
      },
      {
        title: "支付 / 说明",
        dataIndex: "payStatus",
        width: 150,
        render: (value: string) => <Tag color={statusTone(value)}>{value}</Tag>,
      },
      { title: "时间", dataIndex: "time", width: 132 },
      {
        title: "操作",
        width: 130,
        render: (_, record) => (
          <Space size={6}>
            <Button type="link" size="small" onClick={() => setSelectedId(record.id)}>
              查看
            </Button>
            <Button
              type="link"
              size="small"
              onClick={() => {
                setSelectedId(record.id);
                setEditOpen(true);
              }}
            >
              编辑
            </Button>
          </Space>
        ),
      },
    ],
    [],
  );

  return (
    <main className="ops-page admin-ops" ref={pageRef}>
      <header className="ops-topbar">
        <Link className="rental-brand ops-brand" to="/">
          <span className="brand-symbol">D</span>
          <strong>DrivePilot</strong>
          <em>管理端</em>
        </Link>
        <Button type="text" icon={<MenuOutlined />} />
        <Input className="ops-search" prefix={<SearchOutlined />} value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder="搜索订单、车辆、用户、门店等..." />
        <Button type="primary" icon={<PlusOutlined />} onClick={openCreateModal}>
          {canCreateSection(currentSection) ? "快速创建" : "业务生成"}
        </Button>
        <button className="icon-tool badge">
          <BellOutlined />
        </button>
        <div className="ops-user">
          <span>{(user?.realName || user?.username || "A").slice(0, 1)}</span>
          <div>
            <strong>{user?.realName || user?.username || "管理员"}</strong>
            <small>超级管理员</small>
          </div>
          <Button type="text" icon={<LogoutOutlined />} onClick={logout} />
        </div>
      </header>

      <div className="ops-layout">
        <aside className="ops-sidebar admin-nav">
          <nav>
            {adminNavItems.map((item) => (
              <button key={item.key} className={activeSection === item.key ? "active" : ""} onClick={() => selectSection(item.key)}>
                {item.icon}
                <span>{item.label}</span>
                {item.key !== "dashboard" && <b>{recordsBySection[item.key].length}</b>}
              </button>
            ))}
          </nav>
          <div className="ops-sidebar-footer">
            <span>系统设置</span>
            <DownOutlined />
          </div>
        </aside>

        <section className="ops-workspace">
          <div className="ops-page-title admin-title">
            <div>
              <span>首页 / {moduleTitle(activeSection)}</span>
              <h1>{activeSection === "dashboard" ? "平台运营数据看板" : moduleTitle(activeSection)}</h1>
              <p>集中管理车源、门店、订单、支付、合同和评价，让运营动作在同一个后台闭环。</p>
            </div>
            <Space>
              <Button icon={<ExportOutlined />} onClick={exportVisibleRecords}>导出</Button>
            </Space>
          </div>

          <div className="admin-kpi-grid">
            {liveMetrics.map((metric) => (
              <article className={`admin-kpi-card ${metric.tone}`} key={metric.label}>
                <div>
                  <span>{metric.label}</span>
                  <strong>{metric.value}</strong>
                  <small>{metric.sub}</small>
                </div>
                <i>{metric.icon}</i>
              </article>
            ))}
          </div>

          {activeSection === "dashboard" && (
            <div className="admin-chart-grid">
              <section className="admin-chart-panel">
                <div className="ops-section-heading">
                  <div>
                    <h2>收入趋势</h2>
                    <span>基于支付流水生成</span>
                  </div>
                  <Select
                    value={String(trendDays)}
                    onChange={(value) => setTrendDays(Number(value))}
                    options={[
                      { label: "近 30 日", value: "30" },
                      { label: "近 7 日", value: "7" },
                    ]}
                  />
                </div>
                <div ref={revenueChartRef} className="admin-echart" />
              </section>
              <section className="admin-chart-panel">
                <div className="ops-section-heading">
                  <div>
                    <h2>热门车型 TOP5</h2>
                    <span>来自后台看板接口</span>
                  </div>
                  <Button type="primary">按订单数</Button>
                </div>
                <div ref={hotChartRef} className="admin-echart" />
              </section>
            </div>
          )}

          <section className="admin-data-panel">
            <div className="admin-module-tabs">
              {adminNavItems
                .filter((item) => item.key !== "dashboard")
                .map((item) => (
                  <button key={item.key} className={currentSection === item.key ? "active" : ""} onClick={() => selectSection(item.key)}>
                    {item.label}
                  </button>
                ))}
            </div>
            <div className="admin-filter-row">
              <Select value={storeFilter} onChange={setStoreFilter} options={storeFilterOptions} />
              <Select value={statusFilter} onChange={setStatusFilter} options={statusFilterOptions} />
              <Input prefix={<SearchOutlined />} value={keyword} onChange={(event) => setKeyword(event.target.value)} placeholder={`搜索${moduleTitle(currentSection)}`} />
              <Button
                onClick={() => {
                  setKeyword("");
                  setStoreFilter("all");
                  setStatusFilter("all");
                }}
              >
                重置
              </Button>
              <Button type="primary" onClick={invalidateAdminData}>
                查询
              </Button>
            </div>
            <Table
              rowKey="id"
              columns={columns}
              dataSource={visibleRecords}
              loading={
                carsQuery.isFetching ||
                storesQuery.isFetching ||
                ordersQuery.isFetching ||
                usersQuery.isFetching ||
                paymentsQuery.isFetching ||
                commentsQuery.isFetching ||
                contractsQuery.isFetching ||
                maintenanceQuery.isFetching
              }
              pagination={{ pageSize: 5 }}
              scroll={{ x: 1120 }}
              locale={{ emptyText: <Empty description={`${moduleTitle(currentSection)}暂无真实数据`} /> }}
              rowClassName={(record) => (record.id === selectedRecord?.id ? "selected-row" : "")}
              onRow={(record) => ({ onClick: () => setSelectedId(record.id) })}
            />
          </section>
        </section>

        <aside className="ops-detail-drawer admin-detail">
          {selectedRecord ? (
            <>
              <div className="detail-panel-header detail-motion">
                <div>
                  <span>{moduleTitle(currentSection)}详情</span>
                  <h2>{selectedRecord.id}</h2>
                </div>
                <Button type="text" icon={<CloseOutlined />} />
              </div>
              <div className="admin-detail-visual detail-motion">
                <img src={selectedRecord.image} alt={selectedRecord.primary} />
                <Tag color={statusTone(selectedRecord.status)}>{selectedRecord.status}</Tag>
              </div>
              <div className="admin-detail-title detail-motion">
                <h3>{selectedRecord.primary}</h3>
                <p>{selectedRecord.secondary}</p>
                <Space wrap>
                  <Tag>{selectedRecord.category}</Tag>
                  <Tag color="blue">{selectedRecord.store}</Tag>
                  <Tag color={statusTone(selectedRecord.payStatus)}>{selectedRecord.payStatus}</Tag>
                </Space>
              </div>
              <div className="admin-detail-list detail-motion">
                {[
                  ["金额 / 指标", selectedRecord.amount],
                  ["时间", selectedRecord.time],
                  ["说明", selectedRecord.extra],
                  ["当前模块", moduleTitle(currentSection)],
                ].map(([label, value]) => (
                  <div key={label}>
                    <span>{label}</span>
                    <strong>{value}</strong>
                  </div>
                ))}
              </div>
              {currentSection === "stores" && selectedStoreForStaff && (
                <div className="store-staff-card detail-motion">
                  <div className="store-staff-card-head">
                    <span>门店员工</span>
                    <Tag color="blue">{storeStaffBindings.length} 人已绑定</Tag>
                  </div>
                  <Space.Compact block>
                    <Select
                      allowClear
                      value={staffUserId}
                      loading={usersQuery.isFetching || storeStaffQuery.isFetching}
                      onChange={setStaffUserId}
                      placeholder={staffOptions.length ? "选择门店员工账号" : "暂无可绑定门店员工"}
                      options={staffOptions}
                    />
                    <Button
                      type="primary"
                      loading={bindStoreStaffMutation.isPending}
                      disabled={!staffUserId}
                      onClick={() => staffUserId && bindStoreStaffMutation.mutate(staffUserId)}
                    >
                      绑定
                    </Button>
                  </Space.Compact>
                  <div className="store-staff-list">
                    {storeStaffBindings.length ? (
                      storeStaffBindings.map((binding) => (
                        <div key={binding.id}>
                          <strong>{binding.user.realName || binding.user.username}</strong>
                          <span>{binding.user.phone || binding.user.email || binding.user.username}</span>
                          <Button
                            type="link"
                            danger
                            size="small"
                            loading={unbindStoreStaffMutation.isPending}
                            onClick={() => unbindStoreStaffMutation.mutate(binding.user.id)}
                          >
                            解绑
                          </Button>
                        </div>
                      ))
                    ) : (
                      <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="该门店暂未绑定员工" />
                    )}
                  </div>
                </div>
              )}
              <div className="admin-fee-card detail-motion">
                <span>运营备注</span>
                <p>{selectedRecord.extra}</p>
                <strong>{selectedRecord.amount}</strong>
              </div>
              <div className="detail-action-grid detail-motion">
                {canEditSection(currentSection) ? (
                  <Button icon={<EditOutlined />} onClick={() => setEditOpen(true)}>
                    编辑
                  </Button>
                ) : (
                  <Button icon={<EditOutlined />} onClick={() => setSelectedId(selectedRecord.id)}>
                    查看详情
                  </Button>
                )}
                <Button type="primary" icon={<CheckCircleOutlined />} loading={actionMutation.isPending} onClick={runPrimaryAction}>
                  {actionLabel(currentSection, selectedRecord)}
                </Button>
              </div>
              <Button size="large" block disabled={!selectedContact} onClick={() => setContactOpen(true)}>
                {selectedContact?.title || "暂无联系方式"}
              </Button>
            </>
          ) : (
            <Empty description={`${moduleTitle(currentSection)}暂无真实数据`} />
          )}
        </aside>
      </div>

      {/* ---- Edit Modal ---- */}
      <Modal
        open={editOpen}
        title={`编辑 ${selectedRecord?.primary || moduleTitle(currentSection)}`}
        onCancel={() => setEditOpen(false)}
        okText="保存"
        cancelText="取消"
        footer={null}
        destroyOnHidden
      >
        {currentSection === "cars" && selectedRecord && (
          <EditCarForm
            record={selectedRecord}
            onDone={() => { setEditOpen(false); invalidateAdminData(); }}
            onCancel={() => setEditOpen(false)}
          />
        )}
        {currentSection === "stores" && selectedRecord && (
          <EditStoreForm
            record={selectedRecord}
            onDone={() => { setEditOpen(false); invalidateAdminData(); }}
            onCancel={() => setEditOpen(false)}
          />
        )}
        {currentSection === "users" && selectedRecord && (
          <EditUserForm
            record={selectedRecord}
            onDone={() => { setEditOpen(false); invalidateAdminData(); }}
            onCancel={() => setEditOpen(false)}
          />
        )}
        {currentSection === "maintenance" && selectedRecord && (
          <EditMaintenanceForm
            record={selectedRecord}
            onDone={() => { setEditOpen(false); invalidateAdminData(); }}
            onCancel={() => setEditOpen(false)}
          />
        )}
        {currentSection !== "cars" && currentSection !== "stores" && currentSection !== "users" && currentSection !== "maintenance" && (
          <div className="admin-action-panel">
            <Tag color={selectedRecord ? statusTone(selectedRecord.status) : "default"}>{selectedRecord?.status}</Tag>
            <h3>{selectedRecord?.primary}</h3>
            <p>{selectedRecord?.extra}</p>
            <Space style={{ justifyContent: "flex-end", width: "100%" }}>
              <Button onClick={() => setEditOpen(false)}>关闭</Button>
              <Button
                type="primary"
                loading={actionMutation.isPending}
                onClick={() => selectedRecord && actionMutation.mutate(selectedRecord)}
              >
                {actionLabel(currentSection, selectedRecord)}
              </Button>
            </Space>
          </div>
        )}
      </Modal>

      <Modal
        open={contactOpen}
        title={selectedContact?.title || "联系方式"}
        footer={null}
        onCancel={() => setContactOpen(false)}
        destroyOnHidden
      >
        {selectedContact ? (
          <div className="contact-modal">
            <strong>{selectedContact.name}</strong>
            <div>
              <span>电话</span>
              <b>{selectedContact.phone || "暂无电话"}</b>
            </div>
            <div>
              <span>邮箱</span>
              <b>{selectedContact.email || "暂无邮箱"}</b>
            </div>
            <p>{selectedContact.note}</p>
          </div>
        ) : (
          <Empty description="暂无联系方式" />
        )}
      </Modal>

      {/* ---- Create Modal ---- */}
      <Modal
        open={createOpen}
        title={`创建${moduleTitle(currentSection)}`}
        onCancel={() => setCreateOpen(false)}
        okText="创建"
        cancelText="取消"
        footer={null}
        destroyOnHidden
      >
        <CreateFormSelector
          section={currentSection}
          stores={stores}
          onDone={() => { setCreateOpen(false); invalidateAdminData(); }}
          onCancel={() => setCreateOpen(false)}
        />
      </Modal>
    </main>
  );
}
