import { Navigate, Route, Routes } from "react-router-dom";
import { AdminPortal } from "./pages/AdminPortal";
import { CustomerApp } from "./pages/CustomerApp";
import { LandingPage } from "./pages/LandingPage";
import { LoginPage } from "./pages/LoginPage";
import { StaffPortal } from "./pages/StaffPortal";
import { useAuth } from "./state/useAuth";
import type { UserRole } from "./types";

function roleHome(role?: UserRole) {
  if (role === "ADMIN") return "/admin";
  if (role === "STORE_STAFF") return "/staff";
  return "/app";
}

function RequireAuth({ children, roles }: { children: React.ReactNode; roles?: UserRole[] }) {
  const { user } = useAuth();
  if (!user) return <Navigate to="/login" replace />;
  if (roles && !roles.includes(user.role)) return <Navigate to={roleHome(user.role)} replace />;
  return children;
}

function AuthRedirect() {
  const { user } = useAuth();
  return <Navigate to={roleHome(user?.role)} replace />;
}

export default function App() {
  return (
    <Routes>
      <Route path="/" element={<LandingPage />} />
      <Route path="/login" element={<LoginPage />} />
      <Route
        path="/app"
        element={
          <RequireAuth roles={["USER"]}>
            <CustomerApp />
          </RequireAuth>
        }
      />
      <Route
        path="/staff"
        element={
          <RequireAuth roles={["STORE_STAFF"]}>
            <StaffPortal />
          </RequireAuth>
        }
      />
      <Route
        path="/admin"
        element={
          <RequireAuth roles={["ADMIN"]}>
            <AdminPortal />
          </RequireAuth>
        }
      />
      <Route path="/home" element={<AuthRedirect />} />
      <Route path="*" element={<Navigate to="/" replace />} />
    </Routes>
  );
}
