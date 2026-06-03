import {
  ArrowRightOutlined,
  AuditOutlined,
  BankOutlined,
  CalendarOutlined,
  CarOutlined,
  CheckCircleOutlined,
  ClockCircleOutlined,
  CreditCardOutlined,
  CustomerServiceOutlined,
  EnvironmentOutlined,
  FileDoneOutlined,
  PhoneOutlined,
  QuestionCircleOutlined,
  SafetyCertificateOutlined,
  SearchOutlined,
  ShopOutlined,
  ToolOutlined,
} from "@ant-design/icons";
import { Button, Collapse, Input, Segmented, Tag } from "antd";
import type { CollapseProps } from "antd";
import gsap from "gsap";
import { useEffect, useMemo, useRef, useState } from "react";
import { Link } from "react-router-dom";

const helpCategories = ["全部", "租车流程", "费用支付", "取还车", "合同发票", "售后保障", "企业服务"];

const helpFaqs = [
  {
    category: "租车流程",
    title: "第一次使用 DrivePilot 怎么完成租车？",
    answer:
      "登录后进入选车页，选择取还车城市、门店和时间，确认车辆可租后提交订单。系统会创建支付单，支付成功后自动生成电子合同，按预约时间到门店取车即可。",
  },
  {
    category: "租车流程",
    title: "可以提前多久预约车辆？",
    answer:
      "平台支持提前预约可租车辆。建议至少提前 2 小时下单，节假日、商务车型和热门门店建议提前 1 到 3 天确认，以便门店完成车辆整备。",
  },
  {
    category: "费用支付",
    title: "费用包含哪些项目？",
    answer:
      "订单金额由日租价、租期天数和可能产生的附加费用组成。车辆卡片会展示日租价格和押金，支付前的确认页会展示本次订单的费用明细。",
  },
  {
    category: "费用支付",
    title: "支付成功后可以取消或退款吗？",
    answer:
      "待取车订单可发起取消，系统会进入退款处理流程。若订单已经开始租赁，需要由门店完成还车验收后再进行费用结算。",
  },
  {
    category: "取还车",
    title: "到店取车需要携带什么？",
    answer:
      "请携带本人身份证、有效驾驶证，并确保预留手机号可接收门店通知。门店会完成车辆外观、油量或电量、里程等验车确认。",
  },
  {
    category: "取还车",
    title: "能否异地还车？",
    answer:
      "支持同平台门店间的异地还车能力，具体以订单可选门店和车辆规则为准。部分车型或城市可能需要额外确认履约能力。",
  },
  {
    category: "合同发票",
    title: "电子合同在哪里查看？",
    answer:
      "支付成功后系统会为订单生成电子合同。你可以在我的行程中查看已完成或进行中的订单，并从订单操作入口打开合同详情。",
  },
  {
    category: "合同发票",
    title: "企业用户如何对账和开票？",
    answer:
      "企业用车订单会归档订单、支付流水和合同信息。后续可按企业、部门、门店或项目维度导出对账数据，并由专属客服协助开票。",
  },
  {
    category: "售后保障",
    title: "行程中车辆出现故障怎么办？",
    answer:
      "请第一时间联系 24/7 客服或取车门店。门店会根据车辆状态安排道路救援、替换车辆或维修保养登记，并同步更新订单状态。",
  },
  {
    category: "售后保障",
    title: "还车验收后发现费用异常怎么办？",
    answer:
      "可以通过客服提交费用复核。平台会结合门店验车记录、支付流水、合同条款和车辆状态进行核查，并保留完整处理记录。",
  },
  {
    category: "企业服务",
    title: "企业长租和个人短租有什么区别？",
    answer:
      "企业长租更关注预算管控、多人用车、合同归档和跨门店履约；个人短租更强调快速选车、在线支付和自助查看行程。两类用户都使用同一套订单履约链路。",
  },
  {
    category: "企业服务",
    title: "如何开通企业用车方案？",
    answer:
      "可以在帮助中心提交企业合作咨询，也可以从首页企业方案入口进入。客户经理会根据城市、用车规模、车型偏好和结算方式配置方案。",
  },
];

const serviceChannels = [
  { title: "24/7 在线客服", text: "支付、合同、行程异常即时响应", icon: CustomerServiceOutlined, action: "发起咨询" },
  { title: "门店履约协助", text: "取车、还车、验车和车辆整备支持", icon: ShopOutlined, action: "查找门店" },
  { title: "企业客户经理", text: "长租、月结、发票和预算方案", icon: BankOutlined, action: "预约沟通" },
  { title: "道路救援联动", text: "故障、事故、替换车辆处理指引", icon: ToolOutlined, action: "查看预案" },
];

const flowSteps = [
  { title: "搜车", text: "按城市、门店、车型筛选", icon: SearchOutlined },
  { title: "预约", text: "确认取还车时间", icon: CalendarOutlined },
  { title: "支付", text: "在线支付并锁定车辆", icon: CreditCardOutlined },
  { title: "取车", text: "门店验车交付", icon: CarOutlined },
  { title: "还车", text: "验收结算费用", icon: CheckCircleOutlined },
  { title: "归档", text: "合同、评价和流水留存", icon: FileDoneOutlined },
];

function normalize(value: string) {
  return value.trim().toLowerCase();
}

export function HelpCenterPage() {
  const pageRef = useRef<HTMLDivElement>(null);
  const [query, setQuery] = useState("");
  const [activeCategory, setActiveCategory] = useState("全部");

  const filteredFaqs = useMemo(() => {
    const keyword = normalize(query);
    return helpFaqs.filter((faq) => {
      const categoryMatched = activeCategory === "全部" || faq.category === activeCategory;
      const keywordMatched =
        !keyword || normalize(`${faq.title} ${faq.answer} ${faq.category}`).includes(keyword);
      return categoryMatched && keywordMatched;
    });
  }, [activeCategory, query]);

  const collapseItems: CollapseProps["items"] = filteredFaqs.map((faq, index) => ({
    key: `${faq.category}-${index}`,
    label: (
      <div className="help-faq-title">
        <span>{faq.title}</span>
        <Tag>{faq.category}</Tag>
      </div>
    ),
    children: <p>{faq.answer}</p>,
  }));

  useEffect(() => {
    const reduceMotion = window.matchMedia("(prefers-reduced-motion: reduce)").matches;
    if (reduceMotion) return;

    const context = gsap.context(() => {
      gsap.from(".help-reveal", {
        y: 28,
        autoAlpha: 0,
        duration: 0.68,
        stagger: 0.07,
        ease: "power3.out",
      });
      gsap.from(".help-flow-step", {
        y: 22,
        autoAlpha: 0,
        duration: 0.52,
        stagger: 0.06,
        delay: 0.18,
        ease: "power2.out",
      });
      gsap.from(".help-route-line span", {
        scaleX: 0,
        transformOrigin: "left center",
        duration: 1.1,
        delay: 0.28,
        ease: "power2.inOut",
      });
    }, pageRef);

    return () => context.revert();
  }, []);

  return (
    <div className="help-page" ref={pageRef}>
      <header className="help-header">
        <Link className="brand-mark" to="/">
          <span className="brand-symbol">D</span>
          <span>DrivePilot</span>
        </Link>
        <nav>
          <Link to="/">首页</Link>
          <Link to="/app">选车</Link>
          <a href="#service">服务通道</a>
          <a href="#faq">常见问题</a>
        </nav>
        <Link to="/login" className="header-login help-login">
          登录
        </Link>
      </header>

      <main>
        <section className="help-hero">
          <div className="help-hero-copy">
            <span className="help-kicker help-reveal">
              <QuestionCircleOutlined />
              DrivePilot 帮助中心
            </span>
            <h1 className="help-reveal">从下单到归档，每一步都有清晰答案</h1>
            <p className="help-reveal">
              覆盖租车流程、费用支付、取还车、合同发票、售后保障和企业服务。遇到问题时，你可以先检索答案，也可以直接进入服务通道。
            </p>
            <div className="help-search-wrap help-reveal">
              <Input
                size="large"
                prefix={<SearchOutlined />}
                value={query}
                onChange={(event) => setQuery(event.target.value)}
                placeholder="搜索：退款、取车、合同、企业用车..."
              />
              <Button type="primary" size="large">
                搜索答案
              </Button>
            </div>
            <div className="help-hero-tags help-reveal">
              <span>
                <ClockCircleOutlined />
                平均 2 分钟定位问题
              </span>
              <span>
                <SafetyCertificateOutlined />
                合同与支付可追踪
              </span>
              <span>
                <EnvironmentOutlined />
                门店履约协同
              </span>
            </div>
          </div>

          <div className="help-hero-visual help-reveal">
            <img src="/images/home-hero-road.png" alt="DrivePilot 租赁服务车辆" />
            <div className="help-status-panel">
              <span>当前服务状态</span>
              <strong>全部通道正常</strong>
              <small>支付 · 合同 · 门店履约 · 评价归档</small>
            </div>
          </div>
        </section>

        <section className="help-flow-section">
          <div className="help-section-heading help-reveal">
            <span>租赁流程</span>
            <h2>业务链路按节点推进，减少线下反复确认</h2>
          </div>
          <div className="help-flow-track">
            <div className="help-route-line">
              <span />
            </div>
            {flowSteps.map(({ title, text, icon: Icon }) => (
              <article className="help-flow-step" key={title}>
                <div>
                  <Icon />
                </div>
                <strong>{title}</strong>
                <p>{text}</p>
              </article>
            ))}
          </div>
        </section>

        <section className="help-content-grid" id="faq">
          <aside className="help-side-panel help-reveal">
            <span>问题分类</span>
            <Segmented
              vertical
              block
              value={activeCategory}
              options={helpCategories}
              onChange={(value) => setActiveCategory(String(value))}
            />
            <div className="help-side-note">
              <AuditOutlined />
              <p>建议先按分类筛选，再输入关键词，能更快定位订单、支付和合同相关问题。</p>
            </div>
          </aside>

          <section className="help-faq-panel help-reveal">
            <div className="help-faq-head">
              <div>
                <span>常见问题</span>
                <h2>{activeCategory === "全部" ? "全部帮助主题" : activeCategory}</h2>
              </div>
              <Tag color="blue">{filteredFaqs.length} 条结果</Tag>
            </div>
            {collapseItems.length ? (
              <Collapse
                items={collapseItems}
                defaultActiveKey={filteredFaqs.length ? `${filteredFaqs[0].category}-0` : undefined}
              />
            ) : (
              <div className="help-empty">
                <SearchOutlined />
                <strong>没有找到匹配答案</strong>
                <p>换一个关键词试试，或通过下方服务通道联系人工客服。</p>
              </div>
            )}
          </section>
        </section>

        <section className="help-service-section" id="service">
          <div className="help-section-heading help-reveal">
            <span>服务通道</span>
            <h2>自助查询不够时，直接找到对应的人和流程</h2>
          </div>
          <div className="help-service-grid">
            {serviceChannels.map(({ title, text, icon: Icon, action }) => (
              <article className="help-service-card help-reveal" key={title}>
                <Icon />
                <strong>{title}</strong>
                <p>{text}</p>
                <button>
                  {action}
                  <ArrowRightOutlined />
                </button>
              </article>
            ))}
          </div>
        </section>

        <section className="help-cta help-reveal">
          <div>
            <span>
              <PhoneOutlined />
              还没找到答案？
            </span>
            <h2>把问题交给 DrivePilot 服务团队</h2>
            <p>我们会结合订单、门店、车辆和支付记录帮你定位问题，企业客户可进入专属处理通道。</p>
          </div>
          <div>
            <Link to="/login">
              <Button type="primary" size="large">
                登录后联系客服
              </Button>
            </Link>
            <Link to="/app">
              <Button size="large">返回选车</Button>
            </Link>
          </div>
        </section>
      </main>
    </div>
  );
}
