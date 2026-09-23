import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterOutlet } from '@angular/router';

import { AuthService } from './core/auth.service';
import { MessagesService } from './core/messages.service';

@Component({
  selector: 'app-root',
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.css',
})
export class App {
  protected readonly auth = inject(AuthService);
  protected readonly messages = inject(MessagesService);
  private readonly router = inject(Router);

  async logout(): Promise<void> {
    try {
      await this.auth.logout();
      await this.router.navigate(['/login']);
    } catch {
      this.messages.error('Sign out could not be completed. Please try again.');
    }
  }
}
