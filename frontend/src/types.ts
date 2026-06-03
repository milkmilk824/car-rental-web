export type UserRole = "USER" | "STORE_STAFF" | "ADMIN";
export type UserStatus = "ACTIVE" | "DISABLED";
export type StoreStatus = "OPEN" | "CLOSED";
export type CarStatus = "AVAILABLE" | "RESERVED" | "RENTING" | "REPAIRING" | "MAINTAINING" | "OFFLINE";
export type OrderStatus =
  | "PENDING_PAYMENT"
  | "PENDING_PICKUP"
  | "RENTING"
  | "PENDING_RETURN"
  | "COMPLETED"
  | "CANCELLED"
  | "REFUNDING"
  | "REFUNDED"
  | "EXCEPTION";
export type PayType = "ALIPAY" | "WECHAT" | "BANK_CARD" | "CASH" | "MOCK";
export type PayStatus = "WAITING" | "SUCCESS" | "REFUNDING" | "REFUNDED" | "CLOSED";
export type ContractStatus = "UNSIGNED" | "SIGNED" | "ARCHIVED";
export type CommentStatus = "PENDING" | "APPROVED" | "REMOVED";
export type MaintenanceType = "REPAIR" | "MAINTENANCE";

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
  timestamp: string;
}

export interface PageResult<T> {
  items: T[];
  total: number;
  page: number;
  size: number;
  totalPages: number;
}

export interface User {
  id: number;
  username: string;
  phone?: string;
  email?: string;
  realName?: string;
  idCard?: string;
  driverLicenseNo?: string;
  status: UserStatus;
  role: UserRole;
}

export interface LoginResponse {
  token: string;
  refreshToken: string;
  user: User;
}

export interface Category {
  id: number;
  categoryName: string;
  description?: string;
}

export interface Store {
  id: number;
  storeName: string;
  city: string;
  address: string;
  phone?: string;
  businessHours?: string;
  status: StoreStatus;
}

export interface StoreStaffBinding {
  id: number;
  store: Store;
  user: User;
}

export interface Car {
  id: number;
  carName: string;
  brand: string;
  model: string;
  category?: Category;
  plateNumber: string;
  store: Store;
  pricePerDay: number;
  deposit: number;
  status: CarStatus;
  mileage: number;
  description?: string;
  imageUrls: string[];
}

export interface RentalOrder {
  id: number;
  orderNo: string;
  user: User;
  car: Car;
  pickupStore: Store;
  returnStore: Store;
  startTime: string;
  endTime: string;
  rentalDays: number;
  totalAmount: number;
  depositAmount: number;
  status: OrderStatus;
}

export interface PaymentOrder {
  id: number;
  paymentNo: string;
  orderId: number;
  userId: number;
  payAmount: number;
  payType: PayType;
  payStatus: PayStatus;
  transactionNo?: string;
  payTime?: string;
}

export interface Contract {
  id: number;
  contractNo: string;
  orderId: number;
  userId: number;
  contractUrl: string;
  signStatus: ContractStatus;
}

export interface Comment {
  id: number;
  userId: number;
  username: string;
  carId: number;
  orderId: number;
  score: number;
  content?: string;
  status: CommentStatus;
  createTime: string;
}

export interface MaintenanceRecord {
  id: number;
  carId: number;
  type: MaintenanceType;
  description?: string;
  cost: number;
  recordTime: string;
}

export interface DashboardStats {
  todayOrders: number;
  monthRevenue: number;
  rentalRate: number;
  activeUsers: number;
  availableCars: number;
  rentingOrders: number;
  hotCars: Array<{ carId: number; carName: string; orderCount: number }>;
  storePerformance: Array<{ storeId: number; storeName: string; orderCount: number }>;
}

export interface RevenueTrendPoint {
  date: string;
  revenue: number;
}

export interface CarAvailability {
  carId: number;
  available: boolean;
  reason: string;
}

export interface UploadResponse {
  url: string;
  filename: string;
  size: number;
  contentType: string;
}

export interface UpdateProfileRequest {
  phone?: string;
  email?: string;
  realName?: string;
}

export interface LicenseRequest {
  realName: string;
  idCard: string;
  driverLicenseNo: string;
}

export interface CarSearchParams {
  keyword?: string;
  brand?: string;
  categoryId?: number;
  storeId?: number;
  city?: string;
  status?: CarStatus;
  minPrice?: number;
  maxPrice?: number;
  page?: number;
  size?: number;
}

export interface CategoryRequest {
  categoryName: string;
  description?: string;
}

export interface CarRequest {
  carName: string;
  brand: string;
  model: string;
  categoryId: number;
  plateNumber: string;
  storeId: number;
  pricePerDay: number;
  deposit: number;
  status: CarStatus;
  mileage?: number;
  description?: string;
  imageUrls?: string[];
}

export interface StoreRequest {
  storeName: string;
  city: string;
  address: string;
  phone?: string;
  businessHours?: string;
  status: StoreStatus;
}

export interface MaintenanceRequest {
  carId: number;
  type: MaintenanceType;
  description?: string;
  cost?: number;
  recordTime?: string;
}

export interface RefundRequest {
  paymentNo: string;
  reason?: string;
}

export interface AdminCreateUserRequest {
  username: string;
  password: string;
  phone?: string;
  email?: string;
  role?: UserRole;
  status?: UserStatus;
}

export interface AdminUpdateUserRequest {
  phone?: string;
  email?: string;
  realName?: string;
  role?: UserRole;
  status?: UserStatus;
}
