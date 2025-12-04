import { Component } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class AppComponent {
  private readonly AUTH_KEY = 'sgtc_auth';
  private readonly USER_KEY = 'sgtc_user';

  get userEmail(): string | null {
    try {
      const raw =
        sessionStorage.getItem(this.USER_KEY) ||
        localStorage.getItem(this.USER_KEY);
      return raw ? JSON.parse(raw).email ?? null : null;
    } catch {
      return null;
    }
  }

  logout() {
    [sessionStorage, localStorage].forEach((storage) => {
      storage.removeItem(this.AUTH_KEY);
      storage.removeItem(this.USER_KEY);
    });
    location.href = '/login';
  }
}
