import { Component, inject, OnInit } from '@angular/core';
import { RouterLink } from '@angular/router';
import { RequestApi } from '../core/api.services';
import { AssistanceRequest } from '../core/types';
import { StatusBadgeComponent } from '../shared/status-badge.component';

@Component({
  standalone: true,
  imports: [RouterLink, StatusBadgeComponent],
  template: `
    <div class="page"><header><p class="eyebrow">History</p><h1>Service requests</h1></header>
      <section class="cards">@for (request of requests; track request.id) { <a class="card" [routerLink]="['/tracking', request.id]"><strong>{{ request.issueType }}</strong><span>{{ request.manualLocationText || request.description }}</span><app-status-badge [status]="request.status" /></a> } @empty { <article class="card">No request history.</article> }</section>
    </div>
  `
})
export class HistoryPage implements OnInit {
  private readonly api = inject(RequestApi);
  requests: AssistanceRequest[] = [];
  ngOnInit() { this.api.list().subscribe((items) => this.requests = items); }
}
