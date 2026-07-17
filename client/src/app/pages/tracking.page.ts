import { Component, inject, OnDestroy, OnInit } from '@angular/core';
import { ActivatedRoute } from '@angular/router';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Subscription, interval, switchMap } from 'rxjs';
import { AuthService } from '../core/auth.service';
import { RequestApi } from '../core/api.services';
import { REQUEST_STATUSES } from '../core/constants';
import { AssistanceRequest, RequestStatus } from '../core/types';
import { StatusBadgeComponent } from '../shared/status-badge.component';
import { TimelineComponent } from '../shared/timeline.component';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, StatusBadgeComponent, TimelineComponent],
  template: `
    <div class="page"><header><p class="eyebrow">Live tracking</p><h1>{{ request?.issueType || 'Request' }}</h1></header>
      @if (request) { <section class="detail-card"><app-status-badge [status]="request.status" /><p>{{ request.description }}</p><p>{{ request.manualLocationText }}</p><app-timeline [items]="request.updates || []" /></section> }
      <form class="form-grid compact" [formGroup]="statusForm" (ngSubmit)="updateStatus()"><select formControlName="status">@for (status of statuses; track status) { <option [value]="status">{{ status.replace('_', ' ') }}</option> }</select><input placeholder="Update note" formControlName="note" /><button class="primary" type="submit">Update status</button></form>
      @if (auth.user()?.role === 'CUSTOMER') { <form class="form-grid compact" [formGroup]="reviewForm" (ngSubmit)="review()"><input type="number" min="1" max="5" formControlName="rating" /><input placeholder="Review comment" formControlName="comment" /><button class="secondary" type="submit">Submit review</button></form> }
    </div>
  `
})
export class TrackingPage implements OnInit, OnDestroy {
  readonly auth = inject(AuthService);
  private readonly api = inject(RequestApi);
  private readonly route = inject(ActivatedRoute);
  private readonly fb = inject(FormBuilder);
  readonly statuses = REQUEST_STATUSES;
  request?: AssistanceRequest;
  sub?: Subscription;
  statusForm = this.fb.nonNullable.group({ status: ['ACCEPTED' as RequestStatus], note: ['Status updated', Validators.minLength(3)] });
  reviewForm = this.fb.nonNullable.group({ rating: [5, [Validators.min(1), Validators.max(5)]], comment: ['Great service', Validators.minLength(3)] });
  ngOnInit() { const id = this.route.snapshot.paramMap.get('id') || ''; this.sub = interval(5000).pipe(switchMap(() => this.api.get(id))).subscribe((request) => this.request = request); this.api.get(id).subscribe((request) => this.request = request); }
  ngOnDestroy() { this.sub?.unsubscribe(); }
  updateStatus() { if (!this.request) return; this.api.updateStatus(this.request.id, this.statusForm.getRawValue()).subscribe((request) => this.request = request); }
  review() { if (!this.request) return; this.api.review(this.request.id, this.reviewForm.getRawValue()).subscribe(); }
}
