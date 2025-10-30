import { Injectable, signal } from '@angular/core';

type Creds = { email: string; password: string };

@Injectable({ providedIn: 'root' })
export class AuthService {
  private creds = signal<Creds | null>(null);

  constructor() {
    const raw = localStorage.getItem('sgtc.credentials');
    if (raw) this.creds.set(JSON.parse(raw));
  }

  login(email: string, password: string) {
    const c = { email, password };
    this.creds.set(c);
    localStorage.setItem('sgtc.credentials', JSON.stringify(c));
  }

  logout() {
    this.creds.set(null);
    localStorage.removeItem('sgtc.credentials');
  }

  isAuthenticated() {
    return this.creds() !== null;
  }

  getAuthHeader(): string | null {
    const c = this.creds();
    return c ? 'Basic ' + btoa(`${c.email}:${c.password}`) : null;
  }

  currentEmail(): string | null {
    return this.creds()?.email ?? null;
  }
}
