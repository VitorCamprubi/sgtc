import { Component } from '@angular/core';
import { RouterOutlet, RouterLink } from '@angular/router';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet, RouterLink],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class AppComponent {
  get userEmail() {
    return sessionStorage.getItem('userEmail');
  }

  logout() {
    sessionStorage.removeItem('auth');
    sessionStorage.removeItem('userEmail');
    location.href = '/login';
  }
}
