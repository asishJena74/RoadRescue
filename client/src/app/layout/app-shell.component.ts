import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';
import { AuthService } from '../core/auth.service';

@Component({
  selector: 'app-shell',
  standalone: true,
  imports: [RouterLink, RouterOutlet],
  template: `
    <div class="app-shell">
      <aside class="sidebar">
        <a class="brand-row" routerLink="/dashboard">
          <span class="brand-mark">R</span>
          <span><strong>RoadRescue</strong><small>{{ roleLabel() }}</small></span>
        </a>
        <nav>
          <a routerLink="/dashboard">Dashboard</a>
          @if (auth.user()?.role === 'CUSTOMER') { <a routerLink="/request">Request help</a> }
          <a routerLink="/history">History</a>
          <a routerLink="/settings">Settings</a>
          @if (auth.user()?.role === 'ADMIN') { <a routerLink="/admin">Admin</a> }
        </nav>
        <button class="ghost full" type="button" (click)="logout()">Sign out</button>
      </aside>
      <main class="main-panel"><router-outlet /></main>
    </div>
  `
})
export class AppShellComponent {
  readonly auth = inject(AuthService);
  private readonly router = inject(Router);
  readonly roleLabel = computed(() => (this.auth.user()?.role ?? 'Guest').replace(/_/g, ' '));

  logout() {
    this.auth.logout();
    this.router.navigate(['/']);
  }
}
