import { ArrowLeftOutlined, LockOutlined, UserOutlined } from "@ant-design/icons";
import { App, Button, Form, Input, Segmented } from "antd";
import gsap from "gsap";
import { useEffect, useRef, useState } from "react";
import { Link, useNavigate } from "react-router-dom";
import { api } from "../api/client";
import { useAuth } from "../state/useAuth";
import type { LoginResponse, UserRole } from "../types";

type AuthMode = "login" | "register";

function roleHome(role: UserRole) {
  if (role === "ADMIN") return "/admin";
  if (role === "STORE_STAFF") return "/staff";
  return "/app";
}

export function LoginPage() {
  const [mode, setMode] = useState<AuthMode>("login");
  const [loading, setLoading] = useState(false);
  const { message } = App.useApp();
  const { loginWithResponse } = useAuth();
  const navigate = useNavigate();
  const pageRef = useRef<HTMLDivElement>(null);

  useEffect(() => {
    const context = gsap.context(() => {
      gsap.from(".login-panel", { y: 36, autoAlpha: 0, duration: 0.7, ease: "power3.out" });
      gsap.from(".login-side > *", {
        x: -28,
        autoAlpha: 0,
        duration: 0.7,
        stagger: 0.08,
        ease: "power2.out",
      });
    }, pageRef);
    return () => context.revert();
  }, []);

  const submit = async (values: {
    username: string;
    password: string;
    phone?: string;
    email?: string;
  }) => {
    setLoading(true);
    try {
      const response: LoginResponse =
        mode === "login" ? await api.login(values.username, values.password) : await api.register(values);
      loginWithResponse(response);
      message.success(`欢迎回来，${response.user.username}`);
      navigate(roleHome(response.user.role), { replace: true });
    } catch (error) {
      message.error(error instanceof Error ? error.message : "登录失败");
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="login-page" ref={pageRef}>
      <Link to="/" className="back-home">
        <ArrowLeftOutlined /> 返回首页
      </Link>
      <section className="login-side">
        <div className="brand-mark light">
          <span className="brand-symbol">D</span>
          <span>DrivePilot</span>
        </div>
        <h1>登录后按账号角色自动进入工作台</h1>
        <p>普通用户进入租车体验，门店人员进入履约工作台，管理员进入运营管理后台。</p>
        <div className="demo-accounts">
          <span>真实账号体系</span>
          <strong>先注册用户，再按需要分配角色</strong>
          <small>首个管理员可在 MySQL 中将用户 role 提升为 ADMIN</small>
        </div>
      </section>

      <section className="login-panel">
        <Segmented
          block
          value={mode}
          onChange={(value) => setMode(value as AuthMode)}
          options={[
            { label: "登录", value: "login" },
            { label: "注册", value: "register" },
          ]}
        />
        <Form layout="vertical" onFinish={submit} className="auth-form">
          <Form.Item name="username" label="账号" rules={[{ required: true, message: "请输入账号" }]}>
            <Input prefix={<UserOutlined />} placeholder="请输入账号" size="large" />
          </Form.Item>
          <Form.Item name="password" label="密码" rules={[{ required: true, message: "请输入密码" }]}>
            <Input.Password prefix={<LockOutlined />} placeholder="请输入密码" size="large" />
          </Form.Item>
          {mode === "register" && (
            <>
              <Form.Item name="phone" label="手机号">
                <Input placeholder="用于订单通知" size="large" />
              </Form.Item>
              <Form.Item name="email" label="邮箱">
                <Input placeholder="用于合同接收" size="large" />
              </Form.Item>
            </>
          )}
          <Button type="primary" htmlType="submit" size="large" block loading={loading}>
            {mode === "login" ? "登录并进入平台" : "创建账号"}
          </Button>
        </Form>
      </section>
    </main>
  );
}
