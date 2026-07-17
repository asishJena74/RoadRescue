import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { ROLES } from '../core/constants';
import { Role } from '../core/types';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, RouterLink],
  template: `
    <main class="auth-page">
      <form class="auth-card" [formGroup]="form" (ngSubmit)="submit()">
        <a class="brand-row" routerLink="/"><span class="brand-mark">R</span><strong>RoadRescue</strong></a>
        <p class="eyebrow">RoadRescue access</p>
        <h1>{{ mode === 'login' ? 'Welcome back' : 'Create your account' }}</h1>
        @if (mode === 'register') {
          <input placeholder="Full name" formControlName="name" />
          <input placeholder="Phone" formControlName="phone" />
          <select formControlName="role">@for (role of roles; track role) { <option [value]="role">{{ role.replace('_', ' ') }}</option> }</select>
        }
        <input placeholder="Email" formControlName="email" />
        <input placeholder="Password" type="password" formControlName="password" />
        @if (error) { <p class="error">{{ error }}</p> }
        <button class="primary full" type="submit" [disabled]="form.invalid || loading">{{ loading ? 'Please wait...' : (mode === 'login' ? 'Sign in' : 'Register') }}</button>
        <button class="ghost full" type="button" (click)="toggle()">{{ mode === 'login' ? 'Need an account?' : 'Already registered?' }}</button>
      </form>
    </main>
  `
})
export class AuthPage {
  private readonly fb = inject(FormBuilder);
  private readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly roles = ROLES;
  mode: 'login' | 'register' = inject(ActivatedRoute).snapshot.queryParamMap.get('mode') === 'register' ? 'register' : 'login';
  loading = false;
  error = '';
  form = this.fb.nonNullable.group({
    name: [''], phone: [''], role: ['CUSTOMER' as Role],
    email: ['', [Validators.required, Validators.email]],
    password: ['', [Validators.required, Validators.minLength(8)]]
  });

  toggle() { this.mode = this.mode === 'login' ? 'register' : 'login'; this.error = ''; }
  submit() {
    this.error = ''; this.loading = true;
    const value = this.form.getRawValue();
    const request = this.mode === 'login'
      ? this.auth.login(value.email, value.password)
      : this.auth.register({ name: value.name, phone: value.phone, email: value.email, password: value.password, role: value.role });
    request.subscribe({ next: () => this.router.navigate(['/dashboard']), error: (err) => { this.error = err?.error?.message || 'Authentication failed.'; this.loading = false; } });
  }
}
