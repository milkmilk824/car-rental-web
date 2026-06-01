import type {
  AdminCreateUserRequest,
  AdminUpdateUserRequest,
  ApiResponse,
  Car,
  CarRequest,
  CarSearchParams,
  CarStatus,
  CarAvailability,
  Category,
  CategoryRequest,
  Comment,
  Contract,
  DashboardStats,
  LoginResponse,
  LicenseRequest,
  MaintenanceRecord,
  MaintenanceRequest,
  PageResult,
  PaymentOrder,
  PayType,
  RentalOrder,
  RefundRequest,
  RevenueTrendPoint,
  Store,
  StoreRequest,
  StoreStaffBinding,
  UpdateProfileRequest,
  UploadResponse,
  User,
  UserStatus,
} from "../types";

const TOKEN_KEY = "drivepilot_token";
const USER_KEY = "drivepilot_user";

export function getStoredToken() {
  return localStorage.getItem(TOKEN_KEY);
}

export function saveSession(token: string, user: User) {
  localStorage.setItem(TOKEN_KEY, token);
  localStorage.setItem(USER_KEY, JSON.stringify(user));
}

export function clearSession() {
  localStorage.removeItem(TOKEN_KEY);
  localStorage.removeItem(USER_KEY);
}

export function getStoredUser(): User | null {
  const raw = localStorage.getItem(USER_KEY);
  if (!raw) return null;
  try {
    return JSON.parse(raw) as User;
  } catch {
    clearSession();
    return null;
  }
}

type RequestOptions = RequestInit & { auth?: boolean };

async function request<T>(path: string, options: RequestOptions = {}) {
  const headers = new Headers(options.headers);
  if (!headers.has("Content-Type") && options.body && !(options.body instanceof FormData)) {
    headers.set("Content-Type", "application/json");
  }
  const token = getStoredToken();
  if (options.auth !== false && token) {
    headers.set("Authorization", `Bearer ${token}`);
  }

  const response = await fetch(path, { ...options, headers });
  const payload = (await response.json()) as ApiResponse<T>;
  if (!response.ok || payload.code !== 0) {
    throw new Error(payload.message || "请求失败");
  }
  return payload.data;
}

function jsonBody(value: unknown) {
  return JSON.stringify(value);
}

function queryString(params: object) {
  const search = new URLSearchParams();
  Object.entries(params).forEach(([key, value]) => {
    if (value !== undefined && value !== "") search.set(key, String(value));
  });
  return search.toString();
}

export const api = {
  login: (username: string, password: string) =>
    request<LoginResponse>("/api/user/login", {
      method: "POST",
      body: jsonBody({ username, password }),
      auth: false,
    }),
  register: (values: { username: string; password: string; phone?: string; email?: string }) =>
    request<LoginResponse>("/api/user/register", {
      method: "POST",
      body: jsonBody(values),
      auth: false,
    }),
  profile: () => request<User>("/api/user/profile"),
  updateProfile: (payload: UpdateProfileRequest) =>
    request<User>("/api/user/profile", {
      method: "PUT",
      body: jsonBody(payload),
    }),
  updateLicense: (payload: LicenseRequest) =>
    request<User>("/api/user/license", {
      method: "POST",
      body: jsonBody(payload),
    }),
  cars: (params: CarSearchParams = {}) => request<PageResult<Car>>(`/api/cars?${queryString(params)}`, { auth: false }),
  categories: () => request<Category[]>("/api/cars/categories", { auth: false }),
  carDetail: (id: number) => request<Car>(`/api/cars/${id}`, { auth: false }),
  carAvailability: (id: number, startTime: string, endTime: string) =>
    request<CarAvailability>(`/api/cars/${id}/availability?${queryString({ startTime, endTime })}`, { auth: false }),
  stores: () => request<Store[]>("/api/stores", { auth: false }),
  myStores: () => request<Store[]>("/api/store/my-stores"),
  createOrder: (payload: {
    carId: number;
    pickupStoreId: number;
    returnStoreId: number;
    startTime: string;
    endTime: string;
  }) =>
    request<RentalOrder>("/api/orders", {
      method: "POST",
      body: jsonBody(payload),
    }),
  myOrders: () => request<RentalOrder[]>("/api/orders/my"),
  renewOrder: (id: number, extraDays: number) =>
    request<RentalOrder>(`/api/orders/${id}/renew`, {
      method: "PUT",
      body: jsonBody({ extraDays }),
    }),
  createPayment: (orderId: number, payType: PayType = "MOCK") =>
    request<PaymentOrder>("/api/payments/create", {
      method: "POST",
      body: jsonBody({ orderId, payType }),
    }),
  paymentByOrder: (orderId: number) => request<PaymentOrder>(`/api/payments/order/${orderId}`),
  simulatePayment: (paymentNo: string) =>
    request<PaymentOrder>(`/api/payments/${paymentNo}/simulate-success`, { method: "POST" }),
  cancelOrder: (id: number) =>
    request<RentalOrder>(`/api/orders/${id}/cancel`, { method: "PUT" }),
  contractByOrder: (orderId: number) => request<Contract>(`/api/contracts/order/${orderId}`),
  carComments: (carId: number) => request<Comment[]>(`/api/comments/car/${carId}`, { auth: false }),
  createComment: (orderId: number, score: number, content: string) =>
    request<Comment>("/api/comments", {
      method: "POST",
      body: jsonBody({ orderId, score, content }),
    }),
  storeOrders: (storeId: number) => request<RentalOrder[]>(`/api/store/orders?storeId=${storeId}`),
  confirmPickup: (orderId: number) =>
    request<RentalOrder>(`/api/store/orders/${orderId}/pickup`, { method: "PUT" }),
  confirmReturn: (orderId: number) =>
    request<RentalOrder>(`/api/store/orders/${orderId}/return`, { method: "PUT" }),
  createMaintenance: (payload: MaintenanceRequest) =>
    request<MaintenanceRecord>("/api/admin/cars/maintenance", {
      method: "POST",
      body: jsonBody(payload),
    }),
  maintenanceByCar: (carId: number) => request<MaintenanceRecord[]>(`/api/admin/cars/${carId}/maintenance`),
  updateMaintenance: (id: number, payload: MaintenanceRequest) =>
    request<MaintenanceRecord>(`/api/admin/cars/maintenance/${id}`, {
      method: "PUT",
      body: jsonBody(payload),
    }),
  deleteMaintenance: (id: number) =>
    request<void>(`/api/admin/cars/maintenance/${id}`, {
      method: "DELETE",
    }),
  dashboard: () => request<DashboardStats>("/api/admin/dashboard"),
  revenueTrend: (days = 7) => request<RevenueTrendPoint[]>(`/api/admin/dashboard/revenue-trend?${queryString({ days })}`),
  adminCars: (params: CarSearchParams = {}) => request<PageResult<Car>>(`/api/cars?${queryString(params)}`),
  createCategory: (payload: CategoryRequest) =>
    request<Category>("/api/admin/cars/categories", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateCategory: (id: number, payload: CategoryRequest) =>
    request<Category>(`/api/admin/cars/categories/${id}`, {
      method: "PUT",
      body: jsonBody(payload),
    }),
  deleteCategory: (id: number) =>
    request<void>(`/api/admin/cars/categories/${id}`, {
      method: "DELETE",
    }),
  createCar: (payload: CarRequest) =>
    request<Car>("/api/admin/cars", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateCar: (id: number, payload: CarRequest) =>
    request<Car>(`/api/admin/cars/${id}`, {
      method: "PUT",
      body: jsonBody(payload),
    }),
  deleteCar: (id: number) =>
    request<void>(`/api/admin/cars/${id}`, {
      method: "DELETE",
    }),
  adminOrders: () => request<RentalOrder[]>("/api/admin/orders"),
  adminUsers: () => request<User[]>("/api/admin/users"),
  createUser: (payload: AdminCreateUserRequest) =>
    request<User>("/api/admin/users", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateUser: (id: number, payload: AdminUpdateUserRequest) =>
    request<User>(`/api/admin/users/${id}`, {
      method: "PUT",
      body: jsonBody(payload),
    }),
  updateUserStatus: (userId: number, status: UserStatus) =>
    request<User>(`/api/admin/users/${userId}/status?${queryString({ status })}`, {
      method: "PUT",
    }),
  deleteUser: (id: number) =>
    request<void>(`/api/admin/users/${id}`, {
      method: "DELETE",
    }),
  adminStores: () => request<Store[]>("/api/stores"),
  bindStoreStaff: (storeId: number, userId: number) =>
    request<StoreStaffBinding>(`/api/admin/stores/${storeId}/staff/${userId}`, { method: "POST" }),
  unbindStoreStaff: (storeId: number, userId: number) =>
    request<void>(`/api/admin/stores/${storeId}/staff/${userId}`, { method: "DELETE" }),
  storeStaff: (storeId: number) => request<StoreStaffBinding[]>(`/api/admin/stores/${storeId}/staff`),
  createStore: (payload: StoreRequest) =>
    request<Store>("/api/admin/stores", {
      method: "POST",
      body: jsonBody(payload),
    }),
  updateStore: (id: number, payload: StoreRequest) =>
    request<Store>(`/api/admin/stores/${id}`, {
      method: "PUT",
      body: jsonBody(payload),
    }),
  deleteStore: (id: number) =>
    request<void>(`/api/admin/stores/${id}`, {
      method: "DELETE",
    }),
  adminPayments: () => request<PaymentOrder[]>("/api/admin/payments"),
  refundPayment: (payload: RefundRequest) =>
    request<PaymentOrder>("/api/payments/refund", {
      method: "POST",
      body: jsonBody(payload),
    }),
  adminComments: () => request<Comment[]>("/api/admin/comments"),
  deleteComment: (id: number) =>
    request<void>(`/api/admin/comments/${id}`, {
      method: "DELETE",
    }),
  adminContracts: () => request<Contract[]>("/api/admin/contracts"),
  generateContract: (orderId: number) =>
    request<Contract>("/api/contracts/generate", {
      method: "POST",
      body: jsonBody({ orderId }),
    }),
  contractDetail: (id: number) => request<Contract>(`/api/contracts/${id}`),
  signContract: (id: number) =>
    request<Contract>(`/api/contracts/${id}/sign`, {
      method: "PUT",
    }),
  adminMaintenance: () => request<MaintenanceRecord[]>("/api/admin/cars/maintenance"),
  updateCarStatus: (carId: number, status: CarStatus) =>
    request<Car>(`/api/admin/cars/${carId}/status`, {
      method: "PUT",
      body: jsonBody({ status }),
    }),
  uploadCarImage: (file: File) => {
    const body = new FormData();
    body.append("file", file);
    return request<UploadResponse>("/api/admin/upload/car-image", {
      method: "POST",
      body,
    });
  },
};
