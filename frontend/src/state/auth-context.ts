import { createContext } from "react";
import type { LoginResponse, User } from "../types";

export interface AuthContextValue {
  user: User | null;
  loginWithResponse: (response: LoginResponse) => void;
  updateUser: (user: User) => void;
  logout: () => void;
}

export const AuthContext = createContext<AuthContextValue | null>(null);
