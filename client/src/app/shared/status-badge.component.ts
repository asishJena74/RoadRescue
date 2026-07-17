import { Component, Input } from '@angular/core';

@Component({
  selector: 'app-status-badge',
  standalone: true,
  template: '<span class="status" [attr.data-status]="status">{{ label }}</span>'
})
export class StatusBadgeComponent {
  @Input() status = 'PENDING';
  get label() { return this.status.replace(/_/g, ' ').toLowerCase().replace(/\b\w/g, (char) => char.toUpperCase()); }
}
