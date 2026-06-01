import {
  BellOutlined,
  CalendarOutlined,
  CarOutlined,
  CheckCircleOutlined,
  CrownOutlined,
  CustomerServiceOutlined,
  EnvironmentOutlined,
  FileDoneOutlined,
  GiftOutlined,
  LogoutOutlined,
  MessageOutlined,
  SearchOutlined,
  SafetyCertificateOutlined,
} from "@ant-design/icons";
import { useMutation, useQuery, useQueryClient } from "@tanstack/react-query";
import { App, Button, DatePicker, Drawer, Empty, Form, Input, InputNumber, Modal, Select, Space, Spin, Tag, Timeline } from "antd";
import type { RangePickerProps } from "antd/es/date-picker";
import dayjs, { type Dayjs } from "dayjs";
import gsap from "gsap";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../state/useAuth";
import type { Car, Comment, Contract, RentalOrder, Store } from "../types";

const { RangePicker } = DatePicker;

const fallbackImages = [
  "/images/home-hero-road.png",
  "/images/customer-hero-road.png",
  "/images/home-hero-road.png",
];

const orderStatusText: Record<RentalOrder["status"], string> = {
  PENDING_PAYMENT: "待支付",
  PENDING_PICKUP: "待取车",
  RENTING: "租赁中",
  PENDING_RETURN: "待还车",
  COMPLETED: "已完成",
  CANCELLED: "已取消",
  REFUNDING: "退款中",
  REFUNDED: "已退款",
  EXCEPTION: "异常",
};

const orderSteps = ["待支付", "待取车", "租赁中", "已完成"];

function carImage(car: Car, index = 0) {
  const source = car.imageUrls?.[0] || fallbackImages[index % fallbackImages.length];
  if (source.startsWith("/")) return source;
  return `${source}?auto=format&fit=crop&w=1200&q=88`;
}

function formatMoney(value: number) {
  return `￥${Number(value).toLocaleString("zh-CN", { maximumFractionDigits: 0 })}`;
}

function statusColor(status: RentalOrder["status"]) {
  if (status === "COMPLETED") return "green";
  if (status === "RENTING") return "blue";
  if (status === "PENDING_PAYMENT") return "orange";
  if (status === "CANCELLED" || status === "EXCEPTION") return "red";
  return "cyan";
}

function formatDateTime(value?: string) {
  if (!value) return "暂无";
  const date = dayjs(value);
  return date.isValid() ? date.format("YYYY-MM-DD HH:mm") : value;
}

function visibleComments(comments?: Comment[]) {
  return (comments || []).filter((comment) => comment.status !== "REMOVED");
}

function cleanProfileField(value?: string) {
  const next = value?.trim();
  if (!next || next.includes("*")) return undefined;
  return next;
}

export function CustomerApp() {
  const { message } = App.useApp();
  const { user, logout, updateUser } = useAuth();
  const queryClient = useQueryClient();
  const pageRef = useRef<HTMLDivElement>(null);
  const [keyword, setKeyword] = useState("");
  const [city, setCity] = useState<string>();
  const [selectedCar, setSelectedCar] = useState<Car | null>(null);
  const [bookingCar, setBookingCar] = useState<Car | null>(null);
  const [range, setRange] = useState<[Dayjs, Dayjs] | null>([dayjs().add(1, "day"), dayjs().add(4, "day")]);
  const [pickupStoreId, setPickupStoreId] = useState<number>();
  const [returnStoreId, setReturnStoreId] = useState<number>();
  const [contract, setContract] = useState<Contract | null>(null);
  const [contractLoading, setContractLoading] = useState(false);
  const [commentOrder, setCommentOrder] = useState<RentalOrder | null>(null);
  const [profileOpen, setProfileOpen] = useState(false);
  const [renewOrder, setRenewOrder] = useState<RentalOrder | null>(null);

  const storesQuery = useQuery({ queryKey: ["stores"], queryFn: api.stores });
  const carsQuery = useQuery({
    queryKey: ["cars", keyword, city],
    queryFn: () => api.cars({ status: "AVAILABLE", keyword, city, size: 12 }),
  });
  const ordersQuery = useQuery({ queryKey: ["my-orders"], queryFn: api.myOrders });
  const profileQuery = useQuery({ queryKey: ["profile"], queryFn: api.profile });

  const stores = useMemo(() => storesQuery.data || [], [storesQuery.data]);
  const cars = useMemo(() => carsQuery.data?.items || [], [carsQuery.data]);
  const orders = useMemo(() => ordersQuery.data || [], [ordersQuery.data]);
  const profile = profileQuery.data || user;
  const latestOrder = orders[0];
  const activeOrder = latestOrder;
  const commentsQuery = useQuery({
    queryKey: ["car-comments", activeOrder?.car.id],
    queryFn: () => api.carComments(activeOrder!.car.id),
    enabled: Boolean(activeOrder?.id && activeOrder.status === "COMPLETED"),
  });
  const hasReviewedActiveOrder = useMemo(
    () =>
      Boolean(
        activeOrder &&
          commentsQuery.data?.some(
            (comment) =>
              comment.orderId === activeOrder.id &&
              comment.userId === user?.id &&
              comment.status !== "REMOVED",
          ),
      ),
    [activeOrder, commentsQuery.data, user?.id],
  );
  const selectedCarCommentsQuery = useQuery({
    queryKey: ["car-comments", selectedCar?.id],
    queryFn: () => api.carComments(selectedCar!.id),
    enabled: Boolean(selectedCar?.id),
  });
  const selectedCarComments = visibleComments(selectedCarCommentsQuery.data);
  const totalAmount = orders.reduce((sum, order) => sum + Number(order.totalAmount || 0), 0);

  const cityOptions = useMemo(() => Array.from(new Set(stores.map((store) => store.city))), [stores]);

  const openBooking = (car: Car) => {
    setBookingCar(car);
    setPickupStoreId(car.store.id);
    setReturnStoreId(car.store.id);
  };

  useEffect(() => {
    const context = gsap.context(() => {
      gsap.from(".rental-nav", { y: -24, autoAlpha: 0, duration: 0.65, ease: "power3.out" });
      gsap.from(".rental-hero-copy > *, .booking-panel", {
        y: 26,
        autoAlpha: 0,
        duration: 0.7,
        stagger: 0.08,
        ease: "power3.out",
      });
      gsap.from(".member-card", { x: 28, autoAlpha: 0, duration: 0.65, delay: 0.16, ease: "power2.out" });
      gsap.from(".trip-panel", { x: 34, autoAlpha: 0, duration: 0.7, delay: 0.2, ease: "power3.out" });
    }, pageRef);
    return () => context.revert();
  }, []);

  useEffect(() => {
    if (!cars.length) return;
    const context = gsap.context(() => {
      gsap.from(".rental-car-card", {
        y: 38,
        autoAlpha: 0,
        duration: 0.65,
        stagger: 0.08,
        ease: "power2.out",
      });
    }, pageRef);
    return () => context.revert();
  }, [cars.length]);

  const bookingMutation = useMutation({
    mutationFn: async () => {
      if (!bookingCar || !range || !pickupStoreId || !returnStoreId) {
        throw new Error("请选择车辆、时间和门店");
      }
      const startTime = range[0].second(0).millisecond(0).format("YYYY-MM-DDTHH:mm:ss");
      const endTime = range[1].second(0).millisecond(0).format("YYYY-MM-DDTHH:mm:ss");
      const availability = await api.carAvailability(bookingCar.id, startTime, endTime);
      if (!availability.available) {
        throw new Error(availability.reason || "该车辆在所选时段不可租");
      }
      const order = await api.createOrder({
        carId: bookingCar.id,
        pickupStoreId,
        returnStoreId,
        startTime,
        endTime,
      });
      const payment = await api.createPayment(order.id, "MOCK");
      await api.simulatePayment(payment.paymentNo);
      const generatedContract = await api.contractByOrder(order.id);
      return { order, generatedContract };
    },
    onSuccess: ({ generatedContract }) => {
      setContract(generatedContract);
      setBookingCar(null);
      queryClient.invalidateQueries({ queryKey: ["cars"] });
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      message.success("订单已支付，合同已生成");
      gsap.fromTo(".trip-panel", { scale: 0.98, autoAlpha: 0.72 }, { scale: 1, autoAlpha: 1, duration: 0.35 });
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "下单失败"),
  });

  const updateProfileMutation = useMutation({
    mutationFn: (values: { phone?: string; email?: string; realName?: string }) =>
      api.updateProfile({
        phone: cleanProfileField(values.phone),
        email: cleanProfileField(values.email),
        realName: cleanProfileField(values.realName),
      }),
    onSuccess: (nextUser) => {
      updateUser(nextUser);
      queryClient.setQueryData(["profile"], nextUser);
      message.success("会员资料已更新");
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "资料更新失败"),
  });

  const updateLicenseMutation = useMutation({
    mutationFn: (values: { realName: string; idCard: string; driverLicenseNo: string }) =>
      api.updateLicense({
        realName: values.realName.trim(),
        idCard: values.idCard.trim(),
        driverLicenseNo: values.driverLicenseNo.trim(),
      }),
    onSuccess: (nextUser) => {
      updateUser(nextUser);
      queryClient.setQueryData(["profile"], nextUser);
      message.success("驾照认证信息已保存");
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "驾照信息保存失败"),
  });

  const cancelOrderMutation = useMutation({
    mutationFn: (order: RentalOrder) => api.cancelOrder(order.id),
    onSuccess: () => {
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      queryClient.invalidateQueries({ queryKey: ["cars"] });
      message.success("订单已取消");
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "订单取消失败"),
  });

  const renewOrderMutation = useMutation({
    mutationFn: (values: { extraDays: number }) => api.renewOrder(renewOrder!.id, values.extraDays),
    onSuccess: (order) => {
      setRenewOrder(null);
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      message.success(`已续租至 ${formatDateTime(order.endTime)}`);
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "续租失败"),
  });

  const confirmCancelOrder = (order: RentalOrder) => {
    Modal.confirm({
      title: "取消当前订单",
      content:
        order.status === "PENDING_PICKUP"
          ? "订单已完成支付，取消后将进入退款处理流程。"
          : "订单取消后车辆会释放为可租状态。",
      okText: "确认取消",
      cancelText: "再想想",
      okButtonProps: { danger: true },
      onOk: () => cancelOrderMutation.mutateAsync(order),
    });
  };

  const handleTripPrimaryAction = async (order: RentalOrder) => {
    if (order.status === "COMPLETED") {
      setContractLoading(true);
      try {
        setContract(await api.contractByOrder(order.id));
      } catch (error) {
        message.error(error instanceof Error ? error.message : "合同查询失败");
      } finally {
        setContractLoading(false);
      }
      return;
    }
    if (order.status === "PENDING_PICKUP") {
      message.info("订单已支付，请按预约时间到门店取车");
      return;
    }
    if (order.status === "RENTING" || order.status === "PENDING_RETURN") {
      message.info("请到还车门店办理还车复检");
      return;
    }
    message.info("当前订单暂无可执行操作");
  };

  const commentMutation = useMutation({
    mutationFn: (values: { score: number; content: string }) =>
      api.createComment(commentOrder!.id, values.score, values.content),
    onSuccess: () => {
      if (commentOrder) {
        queryClient.invalidateQueries({ queryKey: ["car-comments", commentOrder.car.id] });
      }
      setCommentOrder(null);
      queryClient.invalidateQueries({ queryKey: ["my-orders"] });
      message.success("评价已归档");
    },
    onError: (error) => message.error(error instanceof Error ? error.message : "评价失败"),
  });

  const disabledDate: RangePickerProps["disabledDate"] = (current) => current && current < dayjs().startOf("day");

  return (
    <div className="customer-page rental-home" ref={pageRef}>
      <header className="rental-nav">
        <Link className="rental-brand" to="/">
          <span className="brand-symbol">D</span>
          <strong>DrivePilot</strong>
          <em>企业级汽车租赁平台</em>
        </Link>
        <button className="city-switch">
          <EnvironmentOutlined />
          上海
        </button>
        <nav>
          {["首页", "选车", "门店", "我的行程", "企业服务", "帮助中心"].map((item, index) => (
            <a className={index === 0 ? "active" : ""} href={index === 1 ? "#recommend" : "#"} key={item}>
              {item}
            </a>
          ))}
        </nav>
        <Input
          prefix={<SearchOutlined />}
          value={keyword}
          onChange={(event) => setKeyword(event.target.value)}
          placeholder="搜索车型 / 门店"
          className="rental-nav-search"
        />
        <button className="icon-tool">
          <MessageOutlined />
        </button>
        <button className="icon-tool badge">
          <BellOutlined />
        </button>
        <div className="nav-user">
          <span>{(user?.realName || user?.username || "U").slice(0, 1)}</span>
          <div>
            <strong>{profile?.realName || user?.username}</strong>
            <small>{profile?.driverLicenseNo ? "驾照已认证" : "待认证"}</small>
          </div>
          <Button type="link" size="small" onClick={() => setProfileOpen(true)}>
            资料
          </Button>
          <Button type="text" icon={<LogoutOutlined />} onClick={logout} />
        </div>
      </header>

      <section className="rental-hero">
        <div className="rental-hero-copy">
          <span className="hero-pill">企业级 · 灵活租赁 · 全国服务</span>
          <h1>
            探索理想座驾
            <br />
            开启每段精彩旅程
          </h1>
          <div className="rental-proof">
            {[
              ["车型丰富", CarOutlined],
              ["价格透明", CheckCircleOutlined],
              ["服务保障", SafetyCertificateOutlined],
              ["全国网点", EnvironmentOutlined],
            ].map(([label, Icon]) => {
              const ProofIcon = Icon as typeof CarOutlined;
              return (
                <span key={label as string}>
                  <ProofIcon />
                  {label as string}
                </span>
              );
            })}
          </div>
        </div>

        <div className="member-card">
          <span>
            <CrownOutlined />
            黄金会员
          </span>
          <p>有效期至 2026-06-01</p>
          <div>
            <strong>￥{Number(totalAmount || 8000).toLocaleString("zh-CN")}</strong>
            <small>免押额度</small>
          </div>
          <div>
            <strong>2,480</strong>
            <small>可用积分</small>
          </div>
          <Button block onClick={() => setProfileOpen(true)}>
            会员中心
          </Button>
        </div>

        <div className="booking-panel">
          <div className="booking-tabs">
            <button className="active">短租自驾</button>
            <button>长期租赁</button>
          </div>
          <div className="booking-fields">
            <label>
              <span>取车城市</span>
              <Select
                value={city || "上海"}
                onChange={setCity}
                options={["上海", ...cityOptions.filter((value) => value !== "上海")].map((value) => ({
                  label: value,
                  value,
                }))}
              />
            </label>
            <label>
              <span>取车门店</span>
              <Select
                value={pickupStoreId}
                onChange={setPickupStoreId}
                placeholder="上海浦东国际机场店"
                options={stores.map((store: Store) => ({ label: store.storeName, value: store.id }))}
              />
            </label>
            <label className="date-field">
              <span>取还时间</span>
              <RangePicker
                showTime
                value={range}
                disabledDate={disabledDate}
                onChange={(value) => setRange(value as [Dayjs, Dayjs] | null)}
              />
            </label>
            <Button type="primary" size="large" onClick={() => document.querySelector("#recommend")?.scrollIntoView()}>
              开始选车
            </Button>
          </div>
          <p>
            <CheckCircleOutlined />
            支持异店还车
          </p>
        </div>
      </section>

      <main className="rental-shell" id="recommend">
        <section className="recommend-panel">
          <div className="recommend-header">
            <h2>为你推荐</h2>
            <div className="recommend-tabs">
              {["精选推荐", "经济型", "SUV", "商务型", "豪华型", "新能源"].map((item, index) => (
                <button className={index === 0 ? "active" : ""} key={item}>
                  {item}
                </button>
              ))}
            </div>
            <Button type="text">查看全部</Button>
          </div>

          {carsQuery.isLoading ? (
            <div className="loading-panel">
              <Spin />
            </div>
          ) : cars.length ? (
            <div className="rental-card-grid">
              {cars.slice(0, 4).map((car, index) => (
                <article className={`rental-car-card ${index === 1 ? "featured" : ""}`} key={car.id}>
                  <button onClick={() => setSelectedCar(car)}>
                    <img src={carImage(car, index)} alt={car.carName} />
                    {index === 1 && <span>热门</span>}
                  </button>
                  <div>
                    <h3>{car.carName}</h3>
                    <p>
                      {car.brand} · {car.category?.categoryName || "标准车型"} · {car.store.city}
                    </p>
                    <div className="car-tags">
                      <Tag color="blue">性价比高</Tag>
                      <Tag color="cyan">空间宽敞</Tag>
                    </div>
                    <div className="price-row">
                      <strong>{formatMoney(car.pricePerDay)}</strong>
                      <span>/ 天起</span>
                      <small>押金 {formatMoney(car.deposit)}</small>
                    </div>
                    <footer>
                      <span>{car.store.storeName}</span>
                      <em>可租</em>
                    </footer>
                    <div className="car-card-actions">
                      <Button onClick={() => setSelectedCar(car)}>详情</Button>
                      <Button type="primary" onClick={() => openBooking(car)}>
                        立即预约
                      </Button>
                    </div>
                  </div>
                </article>
              ))}
            </div>
          ) : (
            <Empty description="暂无匹配车辆" />
          )}
        </section>

        <aside className="trip-panel">
          <div className="trip-header">
            <h2>我的行程</h2>
            <Button type="text">查看全部</Button>
          </div>
          <div className="trip-tabs">
            {["进行中", "待支付", "已完成", "已取消"].map((item, index) => (
              <span className={index === 0 ? "active" : ""} key={item}>
                {item}
              </span>
            ))}
          </div>
          {activeOrder ? (
            <div className="trip-card">
              <div className="trip-order-no">
                <span>订单号 {activeOrder.orderNo}</span>
                <Tag color={statusColor(activeOrder.status)}>{orderStatusText[activeOrder.status]}</Tag>
              </div>
              <div className="trip-car-summary">
                <img src={carImage(activeOrder.car)} alt={activeOrder.car.carName} />
                <div>
                  <strong>{activeOrder.car.carName}</strong>
                  <span>{activeOrder.car.plateNumber}</span>
                </div>
                <b>{formatMoney(activeOrder.car.pricePerDay)} /天</b>
              </div>
              <div className="trip-route">
                <p>
                  <span>取车</span>
                  <strong>{activeOrder.pickupStore.storeName}</strong>
                  <em>{dayjs(activeOrder.startTime).format("MM-DD ddd HH:mm")}</em>
                </p>
                <p>
                  <span>还车</span>
                  <strong>{activeOrder.returnStore.storeName}</strong>
                  <em>{dayjs(activeOrder.endTime).format("MM-DD ddd HH:mm")}</em>
                </p>
              </div>
              <Timeline
                items={orderSteps.map((title) => ({
                  color:
                    orderStatusText[activeOrder.status] === title || activeOrder.status === "COMPLETED"
                      ? "#2563eb"
                      : "#d1d8e4",
                  content: title,
                }))}
              />
              <div className="trip-actions">
                <Button
                  type="primary"
                  block
                  loading={contractLoading}
                  onClick={() => handleTripPrimaryAction(activeOrder)}
                >
                  {activeOrder.status === "COMPLETED" ? "查看合同" : "查看进度"}
                </Button>
                {(activeOrder.status === "PENDING_PAYMENT" || activeOrder.status === "PENDING_PICKUP") && (
                  <Button
                    block
                    danger
                    loading={cancelOrderMutation.isPending}
                    onClick={() => confirmCancelOrder(activeOrder)}
                  >
                    取消订单
                  </Button>
                )}
                {(activeOrder.status === "RENTING" || activeOrder.status === "PENDING_RETURN") && (
                  <Button block onClick={() => setRenewOrder(activeOrder)}>
                    申请续租
                  </Button>
                )}
                {activeOrder.status === "COMPLETED" &&
                  (hasReviewedActiveOrder ? (
                    <Tag color="green">已评价</Tag>
                  ) : (
                    <Button block onClick={() => setCommentOrder(activeOrder)}>
                      评价本次服务
                    </Button>
                  ))}
              </div>
            </div>
          ) : (
            <div className="trip-empty">
              <FileDoneOutlined />
              <strong>暂无行程</strong>
              <span>下单后可在这里查看取还车、支付和合同进度。</span>
            </div>
          )}
        </aside>
      </main>

      <section className="service-shortcuts">
        {[
          ["免押租车", "最高可享 8000 免押额度", GiftOutlined],
          ["企业专享", "用车管理 高效便捷", FileDoneOutlined],
          ["积分商城", "积分兑换 精选好礼", CrownOutlined],
          ["24/7 客服", "专业服务 随时在线", CustomerServiceOutlined],
        ].map(([title, text, Icon]) => {
          const ShortcutIcon = Icon as typeof GiftOutlined;
          return (
            <article key={title as string}>
              <ShortcutIcon />
              <div>
                <strong>{title as string}</strong>
                <span>{text as string}</span>
              </div>
            </article>
          );
        })}
      </section>

      <Drawer
        open={!!selectedCar}
        onClose={() => setSelectedCar(null)}
        title={selectedCar?.carName}
        size="large"
        className="detail-drawer"
      >
        {selectedCar && (
          <div className="drawer-content">
            <img src={carImage(selectedCar)} alt={selectedCar.carName} />
            <Space wrap>
              <Tag icon={<CarOutlined />}>{selectedCar.category?.categoryName || "标准车型"}</Tag>
              <Tag icon={<CalendarOutlined />}>{selectedCar.mileage.toLocaleString("zh-CN")} km</Tag>
              <Tag>{selectedCar.store.storeName}</Tag>
            </Space>
            <p>{selectedCar.description}</p>
            <div className="price-stack">
              <span>日租价格</span>
              <strong>{formatMoney(selectedCar.pricePerDay)}</strong>
              <small>押金 {formatMoney(selectedCar.deposit)}</small>
            </div>
            <div className="car-review-section">
              <div className="mini-section-title">
                <strong>真实评价</strong>
                <span>{selectedCarComments.length ? `${selectedCarComments.length} 条反馈` : "等待首条反馈"}</span>
              </div>
              {selectedCarCommentsQuery.isLoading ? (
                <Spin />
              ) : selectedCarComments.length ? (
                <div className="car-review-list">
                  {selectedCarComments.slice(0, 3).map((comment) => (
                    <article key={comment.id}>
                      <div>
                        <strong>{comment.username}</strong>
                        <Tag color="blue">{comment.score} 星</Tag>
                      </div>
                      <p>{comment.content || "用户未填写文字评价。"}</p>
                      <span>{formatDateTime(comment.createTime)}</span>
                    </article>
                  ))}
                </div>
              ) : (
                <Empty image={Empty.PRESENTED_IMAGE_SIMPLE} description="这辆车暂无评价" />
              )}
            </div>
            <Button type="primary" block size="large" onClick={() => openBooking(selectedCar)}>
              预约这辆车
            </Button>
          </div>
        )}
      </Drawer>

      <Modal
        open={!!bookingCar}
        title="确认租赁订单"
        onCancel={() => setBookingCar(null)}
        onOk={() => bookingMutation.mutate()}
        confirmLoading={bookingMutation.isPending}
        okText="创建订单并模拟支付"
      >
        {bookingCar && (
          <div className="booking-modal">
            <strong>{bookingCar.carName}</strong>
            <RangePicker
              showTime
              value={range}
              disabledDate={disabledDate}
              onChange={(value) => setRange(value as [Dayjs, Dayjs] | null)}
            />
            <Select
              value={pickupStoreId}
              onChange={setPickupStoreId}
              placeholder="取车门店"
              options={stores.map((store: Store) => ({ label: store.storeName, value: store.id }))}
            />
            <Select
              value={returnStoreId}
              onChange={setReturnStoreId}
              placeholder="还车门店"
              options={stores.map((store: Store) => ({ label: store.storeName, value: store.id }))}
            />
          </div>
        )}
      </Modal>

      <Modal open={!!contract} onCancel={() => setContract(null)} footer={null} title="合同已生成">
        {contract && (
          <div className="contract-card">
            <FileDoneOutlined />
            <h3>{contract.contractNo}</h3>
            <p>合同文件：{contract.contractUrl}</p>
            <Tag color="cyan">{contract.signStatus}</Tag>
          </div>
        )}
      </Modal>

      <Modal
        open={profileOpen}
        onCancel={() => setProfileOpen(false)}
        footer={null}
        title="会员资料与驾照认证"
        destroyOnHidden
      >
        <div className="profile-modal">
          <section className="profile-summary">
            <span>{(profile?.realName || profile?.username || "U").slice(0, 1)}</span>
            <div>
              <strong>{profile?.realName || profile?.username}</strong>
              <small>{profile?.driverLicenseNo ? `驾照 ${profile.driverLicenseNo}` : "请补全驾照信息后再取车"}</small>
            </div>
            <Tag color={profile?.driverLicenseNo ? "green" : "orange"}>
              {profile?.driverLicenseNo ? "已认证" : "待认证"}
            </Tag>
          </section>

          <Form
            layout="vertical"
            initialValues={{
              realName: profile?.realName,
              phone: cleanProfileField(profile?.phone),
              email: profile?.email,
            }}
            onFinish={(values) => updateProfileMutation.mutate(values)}
          >
            <div className="profile-form-grid">
              <Form.Item name="realName" label="姓名">
                <Input placeholder="真实姓名" />
              </Form.Item>
              <Form.Item name="phone" label="手机号">
                <Input placeholder={profile?.phone ? `当前 ${profile.phone}` : "用于门店联系"} />
              </Form.Item>
              <Form.Item name="email" label="邮箱">
                <Input placeholder="用于接收合同通知" />
              </Form.Item>
            </div>
            <Button type="primary" htmlType="submit" loading={updateProfileMutation.isPending}>
              保存会员资料
            </Button>
          </Form>

          <Form
            layout="vertical"
            initialValues={{
              realName: profile?.realName,
            }}
            onFinish={(values) => updateLicenseMutation.mutate(values)}
          >
            <div className="profile-form-grid">
              <Form.Item name="realName" label="证件姓名" rules={[{ required: true, message: "请输入真实姓名" }]}>
                <Input placeholder="与身份证一致" />
              </Form.Item>
              <Form.Item name="idCard" label="身份证号" rules={[{ required: true, message: "请输入身份证号" }]}>
                <Input placeholder="用于租赁合同" />
              </Form.Item>
              <Form.Item
                name="driverLicenseNo"
                label="驾驶证号"
                rules={[{ required: true, message: "请输入驾驶证号" }]}
              >
                <Input placeholder="门店取车时核验" />
              </Form.Item>
            </div>
            <Button htmlType="submit" loading={updateLicenseMutation.isPending}>
              保存驾照认证
            </Button>
          </Form>
        </div>
      </Modal>

      <Modal
        open={!!renewOrder}
        onCancel={() => setRenewOrder(null)}
        footer={null}
        title={`续租 ${renewOrder?.car.carName || ""}`}
        destroyOnHidden
      >
        {renewOrder && (
          <Form layout="vertical" initialValues={{ extraDays: 1 }} onFinish={(values) => renewOrderMutation.mutate(values)}>
            <div className="renew-summary">
              <span>当前还车时间</span>
              <strong>{formatDateTime(renewOrder.endTime)}</strong>
              <small>续租后金额会按当前日租价重新计算。</small>
            </div>
            <Form.Item
              name="extraDays"
              label="续租天数"
              rules={[{ required: true, message: "请输入续租天数" }]}
            >
              <InputNumber min={1} max={30} precision={0} style={{ width: "100%" }} />
            </Form.Item>
            <Button type="primary" htmlType="submit" loading={renewOrderMutation.isPending} block>
              确认续租
            </Button>
          </Form>
        )}
      </Modal>

      <Modal
        open={!!commentOrder}
        onCancel={() => setCommentOrder(null)}
        footer={null}
        title={`评价 ${commentOrder?.car.carName || ""}`}
      >
        <Form layout="vertical" onFinish={(values) => commentMutation.mutate(values)}>
          <Form.Item name="score" label="评分" initialValue={5}>
            <Select
              options={[1, 2, 3, 4, 5].map((score) => ({ label: `${score} 星`, value: score }))}
            />
          </Form.Item>
          <Form.Item name="content" label="评价内容" rules={[{ required: true, message: "请输入评价" }]}>
            <Input.TextArea rows={4} placeholder="车况、门店服务、取还车体验" />
          </Form.Item>
          <Button type="primary" htmlType="submit" loading={commentMutation.isPending} block>
            提交评价
          </Button>
        </Form>
      </Modal>
    </div>
  );
}
