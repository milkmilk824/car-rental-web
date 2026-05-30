import { useMemo, useState } from "react";
import { clearSession, getStoredUser, saveSession } from "../api/client";
import type { User } from "../types";
import { AuthContext, type AuthContextValue } from "./auth-context";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStoredUser());

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loginWithResponse(response) {
        saveSession(response.token, response.user);
        setUser(response.user);
      },
      logout() {
        clearSession();
        setUser(null);
      },
    }),
    [user],
  );

  return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}
