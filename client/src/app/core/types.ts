export type Role = 'CUSTOMER' | 'MECHANIC' | 'GARAGE_OWNER' | 'ADMIN';
export type IssueType = 'FLAT_TIRE' | 'BATTERY' | 'ENGINE' | 'LOCKOUT' | 'TOWING' | 'FUEL' | 'OTHER';
export type RequestStatus = 'PENDING' | 'ACCEPTED' | 'EN_ROUTE' | 'IN_PROGRESS' | 'COMPLETED' | 'CANCELLED' | 'REJECTED';
export type PaymentMethod = 'CASH' | 'CARD' | 'UPI' | 'WALLET';

export interface AuthUser {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: Role;
  isVerified?: boolean;
  isBlocked?: boolean;
}

export interface AuthResponse {
  token: string;
  user: AuthUser;
}

export interface AssistanceRequest {
  id: string;
  requestId?: string;
  issueType: IssueType;
  description: string;
  status: RequestStatus;
  paymentMethod?: PaymentMethod;
  paymentStatus?: string;
  customerId?: string;
  providerId?: string;
  providerKind?: string;
  manualLocationText?: string;
  pickupLat?: number;
  pickupLng?: number;
  estimatedPrice?: number;
  finalPrice?: number;
  createdAt?: string;
  updatedAt?: string;
  updates?: Array<Record<string, unknown>>;
  review?: Record<string, unknown>;
  [key: string]: unknown;
}

export interface ProviderCandidate {
  id: string;
  name: string;
  kind?: string;
  distanceKm?: number;
  rating?: number;
  services?: string[];
  baseRate?: number;
  [key: string]: unknown;
}
