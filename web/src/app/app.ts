import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterOutlet, RouterLink, RouterLinkActive } from '@angular/router';
import { UsuariosService, Usuario } from './services/usuarios.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class AppComponent implements OnInit {
  private usuariosApi = inject(UsuariosService);
  userRole: Usuario['role'] | null = null;

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

  get isAdmin(): boolean {
    return this.userRole === 'ADMIN';
  }

  get isAdminLinkVisible(): boolean {
    // mostra se já sabemos que é admin ou se o email armazenado é o do admin seed
    const email = this.userEmail;
    return this.isAdmin || email === 'admin@sgtc.local';
  }

  ngOnInit(): void {
    this.usuariosApi.getUsuarioAtualViaDebug().subscribe((u) => {
      this.userRole = u?.role ?? null;
    });
  }

  logout() {
    [sessionStorage, localStorage].forEach((storage) => {
      storage.removeItem(this.AUTH_KEY);
      storage.removeItem(this.USER_KEY);
    });
    location.href = '/login';
  }
}
