import { useMemo, useState } from "react";
import { clearSession, getStoredToken, getStoredUser, saveSession } from "../api/client";
import type { User } from "../types";
import { AuthContext, type AuthContextValue } from "./auth-context";

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [user, setUser] = useState<User | null>(() => getStoredUser());

  const value = useMemo<AuthContextValue>(
    () => ({
      user,
      loginWithResponse(response) {
        saveSession(response.token, response.user, response.refreshToken);
        setUser(response.user);
      },
      updateUser(nextUser) {
        const token = getStoredToken();
        if (token) saveSession(token, nextUser);
        setUser(nextUser);
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
