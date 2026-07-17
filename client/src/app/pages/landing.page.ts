import { Component } from '@angular/core';
import { RouterLink } from '@angular/router';

@Component({
  standalone: true,
  imports: [RouterLink],
  template: `
    <main class="landing">
      <section class="hero">
        <div>
          <p class="eyebrow">Mobile garage and roadside assistance</p>
          <h1>RoadRescue</h1>
          <p>RoadRescue connects stranded drivers to trusted mechanics, garages, towing teams, and emergency support near their live location.</p>
          <div class="actions"><a class="primary" routerLink="/login">Get help now</a><a class="secondary" routerLink="/register">Create account</a></div>
        </div>
      </section>
      <section class="feature-grid">
        <article><strong>Customers</strong><span>Book urgent assistance, track status, and review service.</span></article>
        <article><strong>Providers</strong><span>Manage availability, services, location, and status updates.</span></article>
        <article><strong>Admins</strong><span>Monitor users, requests, verification, and platform health.</span></article>
      </section>
    </main>
  `
})
export class LandingPage {}
