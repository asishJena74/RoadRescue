import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-timeline',
  standalone: true,
  template: `
    <ol class="timeline">
      @for (item of items; track $index) {
        <li><strong>{{ item.status || item.type || 'Update' }}</strong><span>{{ item.note || item.message || item.createdAt || '' }}</span></li>
      } @empty {
        <li><strong>No updates yet</strong><span>Tracking updates will appear here.</span></li>
      }
    </ol>
  `
})
export class TimelineComponent {
  @Input() items: Array<Record<string, any>> = [];
}
