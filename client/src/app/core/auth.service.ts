import { HttpClient } from '@angular/common/http';
import { Injectable, signal } from '@angular/core';
import { tap } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthResponse, AuthUser, Role } from './types';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private readonly tokenKey = 'roadrescue_token';
  private readonly userKey = 'roadrescue_user';
  readonly token = signal<string | null>(localStorage.getItem(this.tokenKey));
  readonly user = signal<AuthUser | null>(this.readUser());

  constructor(private readonly http: HttpClient) {}

  login(email: string, password: string) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/login`, { email, password }).pipe(tap((response) => this.setSession(response)));
  }

  register(payload: { name: string; email: string; password: string; phone: string; role: Role }) {
    return this.http.post<AuthResponse>(`${environment.apiUrl}/auth/register`, payload).pipe(tap((response) => this.setSession(response)));
  }

  refreshMe() {
    return this.http.get<AuthUser>(`${environment.apiUrl}/auth/me`).pipe(tap((user) => this.setUser(user)));
  }

  logout() {
    localStorage.removeItem(this.tokenKey);
    localStorage.removeItem(this.userKey);
    this.token.set(null);
    this.user.set(null);
  }

  private setSession(response: AuthResponse) {
    localStorage.setItem(this.tokenKey, response.token);
    this.token.set(response.token);
    this.setUser(response.user);
  }

  private setUser(user: AuthUser) {
    localStorage.setItem(this.userKey, JSON.stringify(user));
    this.user.set(user);
  }

  private readUser(): AuthUser | null {
    const raw = localStorage.getItem(this.userKey);
    if (!raw) return null;
    try { return JSON.parse(raw) as AuthUser; } catch { return null; }
  }
}
