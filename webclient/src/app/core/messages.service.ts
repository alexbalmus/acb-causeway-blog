import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  kind: 'info' | 'error';
  text: string;
}

/** App-wide toast messages, mirroring the Wicket viewer's feedback banners. */
@Injectable({ providedIn: 'root' })
export class MessagesService {
  private nextId = 1;
  readonly toasts = signal<Toast[]>([]);

  info(text: string): void {
    this.push('info', text, 4000);
  }

  error(text: string): void {
    this.push('error', text, 7000);
  }

  dismiss(id: number): void {
    this.toasts.update((list) => list.filter((t) => t.id !== id));
  }

  private push(kind: Toast['kind'], text: string, ttlMs: number): void {
    const toast: Toast = { id: this.nextId++, kind, text };
    this.toasts.update((list) => [...list, toast]);
    setTimeout(() => this.dismiss(toast.id), ttlMs);
  }
}
