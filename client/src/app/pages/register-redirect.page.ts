import { Component } from '@angular/core';
import { Router } from '@angular/router';

@Component({ standalone: true, template: '' })
export class RegisterRedirectPage {
  constructor(router: Router) { router.navigate(['/login'], { queryParams: { mode: 'register' } }); }
}
