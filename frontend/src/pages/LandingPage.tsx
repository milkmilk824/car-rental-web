import {
  ArrowRightOutlined,
  BankOutlined,
  CalendarOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  ShopOutlined,
  StarFilled,
} from "@ant-design/icons";
import { Button } from "antd";
import gsap from "gsap";
import { ScrollTrigger } from "gsap/ScrollTrigger";
import { useEffect, useRef } from "react";
import { Link } from "react-router-dom";

gsap.registerPlugin(ScrollTrigger);

const servicePromises = [
  { title: "精选车源", text: "严选优质品牌与车况，定期检测与保养，安全可靠。", asset: "/images/home-hero-road.png", tone: "car" },
  { title: "透明费用", text: "费用清晰透明，无隐形收费，价格公开，安心可控。", asset: "/svg_icons/shield_yen.svg", tone: "fee" },
  { title: "门店履约", text: "全国门店覆盖，就近取还，专业服务，高效交付。", asset: "/svg_icons/building.svg", tone: "store" },
  { title: "合同保障", text: "电子合同具备法律效力，支付安全有保障，权益无忧。", asset: "/svg_icons/document_shield.svg", tone: "contract" },
];

const workflow = [
  { title: "搜车", text: "多维筛选，找到心仪车辆", icon: SearchOutlined },
  { title: "预约", text: "选择时间与门店，确认预约", icon: CalendarOutlined },
  { title: "支付", text: "在线支付，安全便捷", icon: CreditCardOutlined },
  { title: "到店取车", text: "门店验车，快速办理取车", icon: ShopOutlined },
  { title: "还车验收", text: "门店验收，结算费用", icon: CarOutlined },
  { title: "评价归档", text: "服务评价，行程归档", icon: StarFilled },
];

const enterprisePartners = ["京东物流", "中国移动", "滴滴出行", "携程旅行", "SF Express", "中信证券"];

const enterpriseHighlights = [
  { label: "统一用车预算", value: "部门、门店、项目维度独立核算" },
  { label: "跨城履约网络", value: "支持异地取还、合同归档与发票追踪" },
  { label: "运营可视化", value: "订单、车辆、门店和支付流水集中管理" },
];

const metrics = [
  { value: "200+", label: "城市门店", icon: BankOutlined },
  { value: "80,000+", label: "可租车辆", icon: CarOutlined },
  { value: "29 分钟", label: "平均取车耗时", icon: ClockCircleOutlined },
  { value: "98.6%", label: "订单完成率", icon: CheckCircleOutlined },
];

export function LandingPage() {
  const pageRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    const header = pageRef.current?.querySelector(".public-header");
    const syncHeader = () => header?.classList.toggle("is-scrolled", window.scrollY > 72);
    syncHeader();
    window.addEventListener("scroll", syncHeader, { passive: true });

    const context = gsap.context(() => {
      gsap.set(".hero-line", { yPercent: 105, autoAlpha: 0 });
      gsap.set(".hero-car", { x: 80, scale: 0.96, autoAlpha: 0 });
      gsap.set(".metric-tile, .service-panel, .workflow-step, .enterprise-card, .partner-logo", { y: 36, autoAlpha: 0 });

      gsap
        .timeline({ defaults: { ease: "power3.out", duration: reduceMotion ? 0 : 0.9 } })
        .to(".hero-line", { yPercent: 0, autoAlpha: 1, stagger: 0.08 })
        .to(".hero-copy", { y: 0, autoAlpha: 1, duration: reduceMotion ? 0 : 0.7 }, "-=0.35")
        .to(".hero-car", { x: 0, scale: 1, autoAlpha: 1 }, "-=0.55")
        .to(".hero-actions", { y: 0, autoAlpha: 1, duration: reduceMotion ? 0 : 0.5 }, "-=0.45")
        .to(".metric-tile", { y: 0, autoAlpha: 1, stagger: 0.06, duration: reduceMotion ? 0 : 0.42 }, "-=0.75");

      if (!reduceMotion) {
        gsap.to(".hero-car", {
          y: -70,
          scrollTrigger: {
            trigger: ".landing-hero",
            start: "top top",
            end: "bottom top",
            scrub: true,
          },
        });
      }

      gsap.to(".service-panel", {
        y: 0,
        autoAlpha: 1,
        stagger: 0.12,
        duration: reduceMotion ? 0 : 0.7,
        ease: "power2.out",
        scrollTrigger: {
          trigger: ".service-section",
          start: "top 70%",
        },
      });

      gsap.to(".workflow-line-fill", {
        scaleX: 1,
        transformOrigin: "left center",
        ease: "none",
        scrollTrigger: {
          trigger: ".workflow-section",
          start: "top 80%",
          end: "bottom 62%",
          scrub: reduceMotion ? false : 0.6,
        },
      });

      gsap.to(".workflow-step", {
        y: 0,
        autoAlpha: 1,
        stagger: 0.08,
        duration: reduceMotion ? 0 : 0.55,
        scrollTrigger: {
          trigger: ".workflow-section",
          start: "top 70%",
        },
      });

      gsap.to(".enterprise-card, .partner-logo", {
        y: 0,
        autoAlpha: 1,
        stagger: 0.07,
        duration: reduceMotion ? 0 : 0.55,
        ease: "power2.out",
        scrollTrigger: {
          trigger: ".enterprise-section",
          start: "top 76%",
        },
      });
    }, pageRef);
    return () => {
      window.removeEventListener("scroll", syncHeader);
      context.revert();
    };
  }, []);

  return (
    <div className="landing-page" ref={pageRef}>
      <header className="public-header">
        <Link className="brand-mark" to="/">
          <span className="brand-symbol">D</span>
          <span>DrivePilot</span>
        </Link>
        <nav>
          <a href="#services">车辆服务</a>
          <a href="#enterprise">企业方案</a>
          <a href="#stores">门店网络</a>
          <a href="#workflow">租赁流程</a>
          <a href="#footer">帮助中心</a>
        </nav>
        <Link to="/login" className="header-login">
          登录
        </Link>
      </header>

      <main>
        <section className="landing-hero">
          <div className="hero-content">
            <div className="hero-kicker">
              <SafetyCertificateOutlined />
              <span>值得信赖的企业级出行伙伴</span>
            </div>
            <div className="headline-mask">
              <h1 className="hero-line">高品质汽车租赁，</h1>
            </div>
            <div className="headline-mask">
              <h1 className="hero-line">
                从选车到还车<span>全程在线</span>
              </h1>
            </div>
            <p className="hero-copy">
              面向企业与个人用户，整合精选车源、透明订单、在线支付、合同归档与门店履约，让每一次出行都更可控。
            </p>
            <div className="hero-actions">
              <Link to="/login">
                <Button type="primary" size="large" className="magnetic-btn">
                  立即租车 <ArrowRightOutlined />
                </Button>
              </Link>
              <a href="#enterprise">
                <Button size="large" className="ghost-btn">
                  企业合作
                </Button>
              </a>
            </div>
            <div className="hero-proof-row">
              {["精选车源", "透明费用", "合同保障", "全国交付"].map((item) => (
                <span key={item}>
                  <CheckCircleOutlined />
                  {item}
                </span>
              ))}
            </div>
          </div>

          <div className="hero-visual">
            <div className="light-sweep" />
            <img
              className="hero-car"
              src="/images/home-hero-road.png"
              alt="DrivePilot 高品质租赁车辆"
            />
            <div className="hero-floating-panel">
              <span>今日可租车辆</span>
              <strong>128</strong>
              <small>上海 / 北京 / 杭州</small>
            </div>
          </div>
        </section>

        <section className="metrics-band" id="stores">
          {metrics.map(({ value, label, icon: Icon }) => (
            <div className="metric-tile" key={label}>
              <Icon />
              <div>
                <strong>{value}</strong>
                <span>{label}</span>
              </div>
            </div>
          ))}
        </section>

        <section className="service-section" id="services">
          <div className="section-heading">
            <span>车辆服务</span>
            <h2>把线下租车的不确定，收束成可追踪的在线流程</h2>
          </div>
          <div className="service-grid">
            {servicePromises.map((item) => (
              <article className="service-panel" key={item.title}>
                <div className={`service-illustration ${item.tone}`}>
                  <img src={item.asset} alt="" aria-hidden="true" />
                </div>
                <div>
                  <h3>{item.title}</h3>
                  <p>{item.text}</p>
                </div>
                <span className="service-arrow">
                  <ArrowRightOutlined />
                </span>
              </article>
            ))}
          </div>
        </section>

        <section className="workflow-section" id="workflow">
          <div className="section-heading">
            <span>租赁流程</span>
            <h2>从搜车到评价，关键节点都在同一条服务线上</h2>
          </div>
          <div className="workflow-track">
            <div className="workflow-line">
              <div className="workflow-line-fill" />
            </div>
            {workflow.map(({ title, text, icon: Icon }, index) => (
              <div className="workflow-step" key={title}>
                <span className="workflow-icon">
                  <Icon />
                </span>
                <strong>{String(index + 1).padStart(2, "0")}</strong>
                <b>{title}</b>
                <span>{text}</span>
              </div>
            ))}
          </div>
        </section>

        <section className="enterprise-section" id="enterprise">
          <div className="enterprise-copy">
            <span>企业方案</span>
            <h2>众多企业与用户的共同选择</h2>
            <p>面向门店连锁、企业用车与平台化租赁运营，DrivePilot 把车辆、合同、支付与门店履约放进同一套在线工作流。</p>
          </div>
          <div className="partner-rail" aria-label="合作企业">
            {enterprisePartners.map((partner, index) => (
              <div className="partner-logo" key={partner}>
                <i>{String(index + 1).padStart(2, "0")}</i>
                <span>{partner}</span>
              </div>
            ))}
          </div>
          <div className="enterprise-card-grid">
            {enterpriseHighlights.map((item) => (
              <article className="enterprise-card" key={item.label}>
                <strong>{item.label}</strong>
                <p>{item.value}</p>
              </article>
            ))}
            <Link to="/login" className="enterprise-cta">
              进入平台 <ArrowRightOutlined />
            </Link>
          </div>
        </section>
      </main>
      <footer className="landing-footer" id="footer">
        <div>
          <Link className="brand-mark light" to="/">
            <span className="brand-symbol">D</span>
            <span>DrivePilot</span>
          </Link>
          <p>企业级汽车租赁平台，连接用户、门店与运营管理，让租赁流程更透明、更高效。</p>
        </div>
        <div>
          <strong>服务</strong>
          <a href="#services">车辆服务</a>
          <a href="#workflow">租赁流程</a>
          <a href="#stores">门店网络</a>
        </div>
        <div>
          <strong>企业</strong>
          <a href="#enterprise">企业方案</a>
          <Link to="/login">进入平台</Link>
          <span>400-888-8899</span>
        </div>
        <div>
          <strong>保障</strong>
          <span>透明费用</span>
          <span>电子合同</span>
          <span>全国交付</span>
        </div>
      </footer>
    </div>
  );
}
