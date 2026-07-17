import { Component, inject, OnInit } from '@angular/core';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../core/auth.service';
import { RequestApi } from '../core/api.services';
import { AssistanceRequest } from '../core/types';
import { StatusBadgeComponent } from '../shared/status-badge.component';

@Component({
  standalone: true,
  imports: [RouterLink, StatusBadgeComponent],
  template: `
    <div class="page"><header><p class="eyebrow">{{ auth.user()?.role?.replace('_', ' ') }}</p><h1>Dashboard</h1></header>
      <section class="toolbar">@if (auth.user()?.role === 'CUSTOMER') { <a class="primary" routerLink="/request">New assistance request</a> } <a class="secondary" routerLink="/history">View history</a><a class="secondary" routerLink="/settings">Settings</a></section>
      <section class="cards">
        @for (request of requests; track request.id) {
          <article class="card" (click)="open(request)"><div><strong>{{ request.issueType }}</strong><span>{{ request.description }}</span></div><app-status-badge [status]="request.status" /></article>
        } @empty { <article class="card"><strong>No requests yet</strong><span>Your active work will appear here.</span></article> }
      </section>
    </div>
  `
})
export class DashboardPage implements OnInit {
  readonly auth = inject(AuthService);
  private readonly api = inject(RequestApi);
  private readonly router = inject(Router);
  requests: AssistanceRequest[] = [];
  ngOnInit() { if (this.auth.user()?.role === 'ADMIN') this.router.navigate(['/admin']); this.api.list().subscribe((items) => this.requests = items); }
  open(request: AssistanceRequest) { this.router.navigate(['/tracking', request.id]); }
}
