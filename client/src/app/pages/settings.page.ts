import { Component, inject } from '@angular/core';
import { FormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { AuthService } from '../core/auth.service';
import { UserApi } from '../core/api.services';

@Component({
  standalone: true,
  imports: [ReactiveFormsModule],
  template: `
    <div class="page"><header><p class="eyebrow">Settings</p><h1>Profile settings</h1></header>
      @if (auth.user()?.role === 'CUSTOMER') { <form class="form-grid" [formGroup]="vehicleForm" (ngSubmit)="saveVehicle()"><input placeholder="Brand" formControlName="brand" /><input placeholder="Model" formControlName="model" /><input placeholder="Plate number" formControlName="plateNumber" /><input type="number" placeholder="Year" formControlName="year" /><input placeholder="Color" formControlName="color" /><input placeholder="Fuel type" formControlName="fuelType" /><button class="primary">Add vehicle</button></form> }
      @if (auth.user()?.role === 'MECHANIC' || auth.user()?.role === 'GARAGE_OWNER') { <form class="form-grid" [formGroup]="providerForm" (ngSubmit)="saveProvider()"><textarea placeholder="Bio" formControlName="bio"></textarea><input placeholder="Services comma separated" formControlName="services" /><input placeholder="Working hours" formControlName="workingHours" /><input type="number" placeholder="Radius km" formControlName="serviceRadiusKm" /><input type="number" placeholder="Latitude" formControlName="currentLat" /><input type="number" placeholder="Longitude" formControlName="currentLng" /><input type="number" placeholder="Base rate" formControlName="baseRate" /><button class="primary">Save provider profile</button><button class="secondary" type="button" (click)="setOnline(true)">Go online</button><button class="ghost" type="button" (click)="setOnline(false)">Go offline</button></form> }
      @if (message) { <p class="success">{{ message }}</p> }
    </div>
  `
})
export class SettingsPage {
  readonly auth = inject(AuthService);
  private readonly api = inject(UserApi);
  private readonly fb = inject(FormBuilder);
  message = '';
  vehicleForm = this.fb.nonNullable.group({ brand: ['', Validators.minLength(2)], model: ['', Validators.required], plateNumber: ['', Validators.minLength(3)], year: [2024, Validators.min(1990)], color: [''], fuelType: ['PETROL'] });
  providerForm = this.fb.nonNullable.group({ bio: [''], services: ['MECHANICAL'], workingHours: ['09:00-18:00', Validators.minLength(3)], serviceRadiusKm: [10, Validators.min(1)], currentLat: [12.9716], currentLng: [77.5946], phoneNumber: [''], businessName: [''], address: [''], baseRate: [500, Validators.min(0)], yearsExperience: [1] });
  saveVehicle() { this.api.addVehicle(this.vehicleForm.getRawValue()).subscribe(() => this.message = 'Vehicle saved.'); }
  saveProvider() { const v = this.providerForm.getRawValue(); this.api.saveProviderProfile({ ...v, services: v.services.split(',').map((s) => s.trim()).filter(Boolean) }).subscribe(() => this.message = 'Provider profile saved.'); }
  setOnline(isOnline: boolean) { this.api.setAvailability(isOnline).subscribe(() => this.message = isOnline ? 'You are online.' : 'You are offline.'); }
}
