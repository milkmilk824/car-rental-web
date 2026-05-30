import type { ReactNode } from "react";
import {
  BarChartOutlined,
  CarOutlined,
  CreditCardOutlined,
  FileDoneOutlined,
  HomeOutlined,
  OrderedListOutlined,
  ShopOutlined,
  StarOutlined,
  ToolOutlined,
  UserOutlined,
} from "@ant-design/icons";

export type StaffStatus = "PENDING_PICKUP" | "RENTING" | "PENDING_RETURN" | "EXCEPTION" | "COMPLETED";

export interface StatusMeta {
  label: string;
  color: string;
  tone: string;
}

export const staffStatusMeta: Record<StaffStatus, StatusMeta> = {
  PENDING_PICKUP: { label: "待取车", color: "blue", tone: "blue" },
  RENTING: { label: "租赁中", color: "green", tone: "green" },
  PENDING_RETURN: { label: "待还车", color: "orange", tone: "orange" },
  EXCEPTION: { label: "异常订单", color: "red", tone: "red" },
  COMPLETED: { label: "已完成", color: "default", tone: "gray" },
};

export interface StaffOrder {
  id: number;
  orderNo: string;
  status: StaffStatus;
  customerName: string;
  customerPhone: string;
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

export interface StaffMetric {
  label: string;
  value: number;
  trend: string;
  icon: ReactNode;
  tone: string;
}

export const opsStores = [
  "上海浦东国际机场店",
  "上海虹桥火车站店",
  "杭州东站旗舰店",
  "苏州园区交付中心",
];

export const staffOrdersSeed: StaffOrder[] = [
  {
    id: 1,
    orderNo: "DP202506020001",
    status: "PENDING_PICKUP",
    customerName: "王先生",
    customerPhone: "138****8888",
    carName: "大众 朗逸 2024款",
    carModel: "1.5L 自动 舒适版",
    plateNumber: "沪A · B88888",
    vin: "LSVCH6BR4RN123456",
    image: "/images/home-hero-road.png",
    pickupStore: "上海浦东国际机场店",
    returnStore: "上海浦东国际机场店",
    pickupTime: "06-02 周一 10:00",
    returnTime: "06-05 周四 10:00",
    amount: 328,
    mileage: 12560,
    assignee: "张小北",
    note: "客户已上传驾照，需复核原件并确认油量。",
    checklist: [
      { label: "驾照与身份证核验", ok: true },
      { label: "随车物品检查", ok: true },
      { label: "油量不低于 1/2", ok: true },
      { label: "车身划痕拍照", ok: false },
    ],
  },
  {
    id: 2,
    orderNo: "DP202506020002",
    status: "PENDING_PICKUP",
    customerName: "李女士",
    customerPhone: "139****2211",
    carName: "宝马 3系 2024款",
    carModel: "2.0T 运动套装",
    plateNumber: "沪A · C12345",
    vin: "LBV8W3104RM330021",
    image: "/images/customer-hero-road.png",
    pickupStore: "上海浦东国际机场店",
    returnStore: "上海虹桥火车站店",
    pickupTime: "06-02 周一 11:30",
    returnTime: "06-05 周四 11:30",
    amount: 598,
    mileage: 8420,
    assignee: "张小北",
    note: "企业客户，保留纸质交接单。",
    checklist: [
      { label: "合同确认", ok: true },
      { label: "押金免押额度确认", ok: true },
      { label: "车辆清洁", ok: true },
      { label: "儿童座椅", ok: false },
    ],
  },
  {
    id: 3,
    orderNo: "DP202505300045",
    status: "RENTING",
    customerName: "陈先生",
    customerPhone: "136****8899",
    carName: "丰田 RAV4 2024款",
    carModel: "2.0L 四驱风尚",
    plateNumber: "沪A · D66666",
    vin: "LFMJ34AF9R0024088",
    image: "/images/home-hero-road.png",
    pickupStore: "上海浦东国际机场店",
    returnStore: "上海浦东国际机场店",
    pickupTime: "05-30 周五 09:00",
    returnTime: "06-05 周四 18:00",
    amount: 1198,
    mileage: 22480,
    assignee: "周婷",
    note: "客户申请延还 2 小时，门店已备注。",
    checklist: [
      { label: "取车影像已归档", ok: true },
      { label: "电子合同已签署", ok: true },
      { label: "租赁中巡检提醒", ok: true },
      { label: "续租费用确认", ok: false },
    ],
  },
  {
    id: 4,
    orderNo: "DP202505310012",
    status: "PENDING_RETURN",
    customerName: "周先生",
    customerPhone: "138****1122",
    carName: "宝马 5系 2024款",
    carModel: "豪华套装",
    plateNumber: "沪A · E55555",
    vin: "LBV61FC05RSE88210",
    image: "/images/customer-hero-road.png",
    pickupStore: "上海浦东国际机场店",
    returnStore: "上海浦东国际机场店",
    pickupTime: "05-31 周六 09:00",
    returnTime: "06-03 周二 18:00",
    amount: 958,
    mileage: 18420,
    assignee: "张小北",
    note: "逾期 2 小时，请在还车时核算超时费用。",
    checklist: [
      { label: "外观复检", ok: false },
      { label: "里程核对", ok: false },
      { label: "违章预授权", ok: true },
      { label: "押金解冻确认", ok: false },
    ],
  },
  {
    id: 5,
    orderNo: "DP202505290033",
    status: "EXCEPTION",
    customerName: "赵女士",
    customerPhone: "137****6677",
    carName: "别克 GL8 ES 陆尊",
    carModel: "商务豪华型",
    plateNumber: "沪A · F77777",
    vin: "LSGUA83L5RE030091",
    image: "/images/home-hero-road.png",
    pickupStore: "上海虹桥火车站店",
    returnStore: "上海浦东国际机场店",
    pickupTime: "05-29 周四 14:00",
    returnTime: "06-05 周四 20:00",
    amount: 1480,
    mileage: 31670,
    assignee: "陈诚",
    note: "客户反馈右后轮胎压异常，已通知维保。",
    checklist: [
      { label: "客户沟通记录", ok: true },
      { label: "维保工单创建", ok: true },
      { label: "备用车方案", ok: false },
      { label: "费用责任确认", ok: false },
    ],
  },
];

export const staffMetrics: StaffMetric[] = [
  { label: "今日取车", value: 16, trend: "较昨日 +3", icon: <CarOutlined />, tone: "blue" },
  { label: "今日还车", value: 12, trend: "较昨日 -2", icon: <ShopOutlined />, tone: "green" },
  { label: "租赁中订单", value: 24, trend: "实时履约", icon: <OrderedListOutlined />, tone: "cyan" },
  { label: "异常订单", value: 3, trend: "需 30 分钟内处理", icon: <ToolOutlined />, tone: "red" },
];

export type AdminSection =
  | "dashboard"
  | "cars"
  | "stores"
  | "orders"
  | "users"
  | "payments"
  | "contracts"
  | "comments"
  | "maintenance";

export interface AdminNavItem {
  key: AdminSection;
  label: string;
  icon: ReactNode;
}

export const adminNavItems: AdminNavItem[] = [
  { key: "dashboard", label: "数据看板", icon: <BarChartOutlined /> },
  { key: "cars", label: "车辆管理", icon: <CarOutlined /> },
  { key: "stores", label: "门店管理", icon: <HomeOutlined /> },
  { key: "orders", label: "订单管理", icon: <OrderedListOutlined /> },
  { key: "users", label: "用户管理", icon: <UserOutlined /> },
  { key: "payments", label: "支付流水", icon: <CreditCardOutlined /> },
  { key: "contracts", label: "合同管理", icon: <FileDoneOutlined /> },
  { key: "comments", label: "评价管理", icon: <StarOutlined /> },
  { key: "maintenance", label: "维保记录", icon: <ToolOutlined /> },
];

export interface AdminMetric {
  label: string;
  value: string;
  sub: string;
  tone: string;
  icon: ReactNode;
}

export const adminMetrics: AdminMetric[] = [
  { label: "今日订单", value: "128", sub: "日环比 +12.5%", tone: "blue", icon: <OrderedListOutlined /> },
  { label: "月收入（含税）", value: "￥328,560", sub: "月同比 +18.6%", tone: "green", icon: <CreditCardOutlined /> },
  { label: "可租车辆", value: "328", sub: "出租中 158 辆", tone: "cyan", icon: <CarOutlined /> },
  { label: "活跃用户", value: "2,845", sub: "月同比 +9.8%", tone: "purple", icon: <UserOutlined /> },
];

export const revenueTrend = [
  { date: "05-07", value: 26800 },
  { date: "05-11", value: 21400 },
  { date: "05-15", value: 43800 },
  { date: "05-19", value: 32600 },
  { date: "05-23", value: 56320 },
  { date: "05-27", value: 46200 },
  { date: "06-01", value: 61200 },
  { date: "06-05", value: 55800 },
];

export const hotVehicleStats = [
  { name: "宝马 3系 2024款", value: 328 },
  { name: "丰田 RAV4 2024款", value: 274 },
  { name: "别克 GL8 ES 陆尊", value: 246 },
  { name: "大众 朗逸 2024款", value: 198 },
  { name: "特斯拉 Model 3", value: 156 },
];

export interface AdminRecord {
  id: string;
  primary: string;
  secondary: string;
  category: string;
  store: string;
  amount: string;
  status: string;
  payStatus: string;
  time: string;
  image: string;
  extra: string;
}

export const adminRecords: Record<AdminSection, AdminRecord[]> = {
  dashboard: [],
  orders: [
    {
      id: "DP202506050001",
      primary: "李想",
      secondary: "138****8888",
      category: "宝马 3系 2024款",
      store: "上海浦东国际机场店",
      amount: "￥328.00",
      status: "进行中",
      payStatus: "已支付",
      time: "06-05 09:42",
      image: "/images/customer-hero-road.png",
      extra: "取车 06-05 10:00，还车 06-06 10:00",
    },
    {
      id: "DP202506040032",
      primary: "王先生",
      secondary: "186****6666",
      category: "丰田 RAV4 2024款",
      store: "上海浦东国际机场店",
      amount: "￥298.00",
      status: "已完成",
      payStatus: "已支付",
      time: "06-04 13:28",
      image: "/images/home-hero-road.png",
      extra: "企业月结客户",
    },
    {
      id: "DP202506030045",
      primary: "陈先生",
      secondary: "177****9999",
      category: "大众 朗逸 2024款",
      store: "上海浦东国际机场店",
      amount: "￥168.00",
      status: "已取消",
      payStatus: "已退款",
      time: "06-03 15:36",
      image: "/images/home-hero-road.png",
      extra: "客户临时取消",
    },
  ],
  cars: [
    {
      id: "沪A · B88888",
      primary: "宝马 3系 2024款",
      secondary: "2.0T · 5座 · 自动挡",
      category: "豪华型",
      store: "上海浦东国际机场店",
      amount: "￥328/天",
      status: "可租",
      payStatus: "保险到期 2025-12-31",
      time: "12,560 km",
      image: "/images/customer-hero-road.png",
      extra: "车架号 LVG****X123456",
    },
    {
      id: "沪A · C12345",
      primary: "丰田 RAV4 2024款",
      secondary: "2.0L · SUV · 自动挡",
      category: "SUV",
      store: "上海虹桥火车站店",
      amount: "￥298/天",
      status: "出租中",
      payStatus: "保养剩余 1,800 km",
      time: "22,480 km",
      image: "/images/home-hero-road.png",
      extra: "本月 27 单",
    },
    {
      id: "沪A · D66666",
      primary: "别克 GL8 ES 陆尊",
      secondary: "商务 · 7座 · 自动挡",
      category: "商务型",
      store: "上海浦东国际机场店",
      amount: "￥598/天",
      status: "维修中",
      payStatus: "轮胎检查",
      time: "31,670 km",
      image: "/images/home-hero-road.png",
      extra: "预计 06-06 回库",
    },
  ],
  stores: [
    {
      id: "ST-SHA-PVG",
      primary: "上海浦东国际机场店",
      secondary: "航站楼 P1 停车场出口旁",
      category: "机场门店",
      store: "上海",
      amount: "42 辆",
      status: "营业中",
      payStatus: "09:00 - 22:00",
      time: "今日 36 单",
      image: "/images/home-hero-road.png",
      extra: "门店电话 021-6000-8888",
    },
    {
      id: "ST-SHA-HQ",
      primary: "上海虹桥火车站店",
      secondary: "虹桥综合交通枢纽 B1",
      category: "高铁门店",
      store: "上海",
      amount: "31 辆",
      status: "营业中",
      payStatus: "08:30 - 21:30",
      time: "今日 24 单",
      image: "/images/customer-hero-road.png",
      extra: "支持异店还车",
    },
  ],
  users: [
    {
      id: "U10086",
      primary: "张小北",
      secondary: "zhangsan · 138****8888",
      category: "黄金会员",
      store: "个人用户",
      amount: "￥8,000 免押",
      status: "正常",
      payStatus: "信用良好",
      time: "最近登录 06-05",
      image: "/images/customer-hero-road.png",
      extra: "累计 12 单",
    },
    {
      id: "U10087",
      primary: "上海晴山科技",
      secondary: "企业账号 · 21 位成员",
      category: "企业客户",
      store: "上海",
      amount: "￥50,000 账期",
      status: "正常",
      payStatus: "月结",
      time: "最近下单 06-04",
      image: "/images/home-hero-road.png",
      extra: "专属客户经理 周婷",
    },
  ],
  payments: [
    {
      id: "PAY202506050001",
      primary: "DP202506050001",
      secondary: "微信支付 · 420000****8888",
      category: "租车费用",
      store: "上海浦东国际机场店",
      amount: "￥328.00",
      status: "成功",
      payStatus: "已入账",
      time: "06-05 09:42",
      image: "/images/customer-hero-road.png",
      extra: "优惠金额 ￥30.00",
    },
    {
      id: "PAY202506030045",
      primary: "DP202506030045",
      secondary: "支付宝 · 202506****0045",
      category: "订单退款",
      store: "上海浦东国际机场店",
      amount: "￥168.00",
      status: "已退款",
      payStatus: "原路退回",
      time: "06-03 16:10",
      image: "/images/home-hero-road.png",
      extra: "取消订单退款",
    },
  ],
  contracts: [
    {
      id: "CT202506050001",
      primary: "DP202506050001 租赁合同",
      secondary: "李想 · 宝马 3系",
      category: "电子合同",
      store: "上海浦东国际机场店",
      amount: "￥328.00",
      status: "已签署",
      payStatus: "归档完成",
      time: "06-05 09:50",
      image: "/images/customer-hero-road.png",
      extra: "合同有效期至 06-06",
    },
    {
      id: "CT202506040032",
      primary: "DP202506040032 租赁合同",
      secondary: "王先生 · 丰田 RAV4",
      category: "电子合同",
      store: "上海浦东国际机场店",
      amount: "￥298.00",
      status: "待签署",
      payStatus: "短信已发送",
      time: "06-04 13:31",
      image: "/images/home-hero-road.png",
      extra: "需提醒客户补签",
    },
  ],
  comments: [
    {
      id: "CM202506050012",
      primary: "李想",
      secondary: "宝马 3系 · 5 星",
      category: "服务评价",
      store: "上海浦东国际机场店",
      amount: "5.0",
      status: "已展示",
      payStatus: "精选评价",
      time: "06-05 18:22",
      image: "/images/customer-hero-road.png",
      extra: "取车很快，车辆干净，合同在线签很方便。",
    },
    {
      id: "CM202506040006",
      primary: "陈先生",
      secondary: "大众 朗逸 · 4 星",
      category: "车辆评价",
      store: "上海虹桥火车站店",
      amount: "4.0",
      status: "待审核",
      payStatus: "含图片",
      time: "06-04 20:18",
      image: "/images/home-hero-road.png",
      extra: "门店指引可以再清楚一点。",
    },
  ],
  maintenance: [
    {
      id: "MT202506050001",
      primary: "宝马 3系 2024款",
      secondary: "REPAIR · 右后轮胎压异常",
      category: "维修",
      store: "上海浦东国际机场店",
      amount: "￥260.00",
      status: "进行中",
      payStatus: "维修记录",
      time: "06-05 16:30",
      image: "/images/customer-hero-road.png",
      extra: "预计 2 小时内完成复检。",
    },
  ],
};

export function formatCurrency(value: number) {
  return `￥${value.toLocaleString("zh-CN", { maximumFractionDigits: 0 })}`;
}

export function statusTone(status: string) {
  if (["成功", "已支付", "已入账", "已签署", "已展示", "正常", "营业中", "可租", "已完成"].includes(status)) {
    return "green";
  }
  if (["待签署", "待审核", "待取车", "出租中", "进行中"].includes(status)) return "blue";
  if (["待还车", "维修中", "已退款", "已取消"].includes(status)) return "orange";
  if (["异常订单", "异常", "失败"].includes(status)) return "red";
  return "default";
}
