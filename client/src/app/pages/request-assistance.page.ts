import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { RequestApi } from '../core/api.services';
import { ISSUE_TYPES, PAYMENT_METHODS } from '../core/constants';
import { IssueType, PaymentMethod, ProviderCandidate } from '../core/types';
import { ProvidersMapComponent } from '../shared/providers-map.component';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule, ProvidersMapComponent],
  template: `
    <div class="page"><header><p class="eyebrow">Customer</p><h1>Request assistance</h1></header>
      <form class="form-grid" [formGroup]="form" (ngSubmit)="submit()">
        <select formControlName="issueType">@for (type of issueTypes; track type) { <option [value]="type">{{ type.replace('_', ' ') }}</option> }</select>
        <select formControlName="paymentMethod">@for (method of paymentMethods; track method) { <option [value]="method">{{ method }}</option> }</select>
        <textarea placeholder="Describe the problem" formControlName="description"></textarea>
        <input type="number" step="0.000001" placeholder="Latitude" formControlName="pickupLat" />
        <input type="number" step="0.000001" placeholder="Longitude" formControlName="pickupLng" />
        <input placeholder="Manual location" formControlName="manualLocationText" />
        @if (message) { <p class="success">{{ message }}</p> }
        <button class="secondary" type="button" (click)="findProviders()">Find nearby providers</button><button class="primary" type="submit" [disabled]="form.invalid">Create request</button>
      </form>
      <app-providers-map [providers]="providers" />
    </div>
  `
})
export class RequestAssistancePage {
  private readonly fb = inject(FormBuilder);
  private readonly api = inject(RequestApi);
  private readonly router = inject(Router);
  readonly issueTypes = ISSUE_TYPES;
  readonly paymentMethods = PAYMENT_METHODS;
  providers: ProviderCandidate[] = [];
  message = '';
  form = this.fb.nonNullable.group({
    issueType: ['FLAT_TIRE' as IssueType, Validators.required], paymentMethod: ['CASH' as PaymentMethod, Validators.required],
    description: ['', [Validators.required, Validators.minLength(5)]], pickupLat: [12.9716, Validators.required], pickupLng: [77.5946, Validators.required], manualLocationText: ['']
  });
  findProviders() { const v = this.form.getRawValue(); this.api.nearbyProviders(v.issueType, Number(v.pickupLat), Number(v.pickupLng)).subscribe((providers) => this.providers = providers); }
  submit() { const v = this.form.getRawValue(); this.api.create({ ...v, pickupLat: Number(v.pickupLat), pickupLng: Number(v.pickupLng), mediaUrls: [] }).subscribe((request) => this.router.navigate(['/tracking', request.id])); }
}
