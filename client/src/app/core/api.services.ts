import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable } from '@angular/core';
import { environment } from '../../environments/environment';
import { AssistanceRequest, IssueType, PaymentMethod, ProviderCandidate, RequestStatus } from './types';

@Injectable({ providedIn: 'root' })
export class RequestApi {
  constructor(private readonly http: HttpClient) {}

  list() { return this.http.get<AssistanceRequest[]>(`${environment.apiUrl}/requests`); }
  get(id: string) { return this.http.get<AssistanceRequest>(`${environment.apiUrl}/requests/${id}`); }
  nearbyProviders(issueType: IssueType, lat: number, lng: number) {
    const params = new HttpParams().set('issueType', issueType).set('lat', lat).set('lng', lng);
    return this.http.get<ProviderCandidate[]>(`${environment.apiUrl}/requests/nearby-providers`, { params });
  }
  create(payload: { vehicleId?: string; issueType: IssueType; description: string; pickupLat: number; pickupLng: number; manualLocationText?: string; mediaUrls: string[]; paymentMethod: PaymentMethod }) {
    return this.http.post<AssistanceRequest>(`${environment.apiUrl}/requests`, payload);
  }
  updateStatus(id: string, payload: { status: RequestStatus; note?: string; latitude?: number; longitude?: number }) {
    return this.http.patch<AssistanceRequest>(`${environment.apiUrl}/requests/${id}/status`, payload);
  }
  review(id: string, payload: { rating: number; comment: string }) {
    return this.http.post<Record<string, unknown>>(`${environment.apiUrl}/requests/${id}/review`, payload);
  }
}

@Injectable({ providedIn: 'root' })
export class UserApi {
  constructor(private readonly http: HttpClient) {}
  addVehicle(payload: Record<string, unknown>) { return this.http.post<Record<string, unknown>>(`${environment.apiUrl}/users/vehicles`, payload); }
  saveProviderProfile(payload: Record<string, unknown>) { return this.http.put<Record<string, unknown>>(`${environment.apiUrl}/users/provider-profile`, payload); }
  setAvailability(isOnline: boolean) { return this.http.patch<Record<string, unknown>>(`${environment.apiUrl}/users/availability`, { isOnline }); }
}

@Injectable({ providedIn: 'root' })
export class AdminApi {
  constructor(private readonly http: HttpClient) {}
  analytics() { return this.http.get<Record<string, unknown>>(`${environment.apiUrl}/admin/analytics`); }
  users() { return this.http.get<Array<Record<string, unknown>>>(`${environment.apiUrl}/admin/users`); }
  moderate(userId: string, payload: { isBlocked?: boolean; isVerified?: boolean }) {
    return this.http.patch<Record<string, unknown>>(`${environment.apiUrl}/admin/users/${userId}`, payload);
  }
}
