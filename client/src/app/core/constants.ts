import { IssueType, PaymentMethod, RequestStatus, Role } from './types';

export const ROLES: Role[] = ['CUSTOMER', 'MECHANIC', 'GARAGE_OWNER', 'ADMIN'];
export const ISSUE_TYPES: IssueType[] = ['FLAT_TIRE', 'BATTERY', 'ENGINE', 'LOCKOUT', 'TOWING', 'FUEL', 'OTHER'];
export const PAYMENT_METHODS: PaymentMethod[] = ['CASH', 'CARD', 'UPI', 'WALLET'];
export const REQUEST_STATUSES: RequestStatus[] = ['PENDING', 'ACCEPTED', 'EN_ROUTE', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED', 'REJECTED'];

export function label(value: string | null | undefined): string {
  return (value ?? '').toString().replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase());
}
