import { Component, inject, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AdminApi } from '../core/api.services';

@Component({
  standalone: true,
  template: `
    <div class="page"><header><p class="eyebrow">Admin</p><h1>Platform control</h1></header>
      <section class="metric-grid">@for (metric of metricEntries; track metric[0]) { <article><strong>{{ metric[1] }}</strong><span>{{ metric[0] }}</span></article> }</section>
      <section class="table-card"><h2>Users</h2>
        @for (user of users; track user['id']) {
          <div class="table-row"><span><strong>{{ user['name'] }}</strong><small>{{ user['email'] }} · {{ user['role'] }}</small></span><span><button class="ghost" (click)="moderate(user, true)">Verify</button><button class="ghost danger" (click)="moderate(user, false)">Block</button></span></div>
        }
      </section>
    </div>
  `
})
export class AdminDashboardPage implements OnInit {
  private readonly api = inject(AdminApi);
  private readonly router = inject(Router);
  metricEntries: [string, unknown][] = [];
  users: Array<Record<string, unknown>> = [];
  ngOnInit() { this.load(); }
  load() { this.api.analytics().subscribe((data) => this.metricEntries = Object.entries(data)); this.api.users().subscribe((users) => this.users = users); }
  moderate(user: Record<string, unknown>, verify: boolean) { this.api.moderate(String(user['id']), verify ? { isVerified: true } : { isBlocked: true }).subscribe(() => this.load()); }
}
