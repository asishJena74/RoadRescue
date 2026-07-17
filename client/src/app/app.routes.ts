import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { AdminDashboardPage } from './pages/admin-dashboard.page';
import { AuthPage } from './pages/auth.page';
import { DashboardPage } from './pages/dashboard.page';
import { HistoryPage } from './pages/history.page';
import { LandingPage } from './pages/landing.page';
import { RegisterRedirectPage } from './pages/register-redirect.page';
import { RequestAssistancePage } from './pages/request-assistance.page';
import { SettingsPage } from './pages/settings.page';
import { TrackingPage } from './pages/tracking.page';

export const routes: Routes = [
  { path: '', component: LandingPage },
  { path: 'login', component: AuthPage },
  { path: 'register', component: RegisterRedirectPage },
  { path: 'dashboard', component: DashboardPage, canActivate: [authGuard] },
  { path: 'request', component: RequestAssistancePage, canActivate: [authGuard] },
  { path: 'history', component: HistoryPage, canActivate: [authGuard] },
  { path: 'tracking/:id', component: TrackingPage, canActivate: [authGuard] },
  { path: 'settings', component: SettingsPage, canActivate: [authGuard] },
  { path: 'admin', component: AdminDashboardPage, canActivate: [authGuard] },
  { path: '**', redirectTo: '' }
];
