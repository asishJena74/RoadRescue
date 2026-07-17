import { Component, Input } from '@angular/core';
import { ProviderCandidate } from '../core/types';

@Component({
  selector: 'app-providers-map',
  standalone: true,
  template: `
    <section class="map-panel">
      <div class="map-grid"></div>
      <div class="map-list">
        @for (provider of providers; track provider.id) {
          <article>
            <strong>{{ provider.name }}</strong>
            <span>{{ provider.kind || 'Provider' }} · {{ provider.distanceKm || 0 }} km · Rating {{ provider.rating || 'New' }}</span>
          </article>
        } @empty {
          <p>No nearby providers yet.</p>
        }
      </div>
    </section>
  `
})
export class ProvidersMapComponent {
  @Input() providers: ProviderCandidate[] = [];
}
