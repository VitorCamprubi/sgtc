import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { NavigationEnd, Router, RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { filter } from 'rxjs/operators';
import { Usuario, UsuariosService } from './services/usuarios.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [CommonModule, RouterOutlet, RouterLink, RouterLinkActive],
  templateUrl: './app.html',
  styleUrl: './app.scss',
})
export class AppComponent implements OnInit {
  private usuariosApi = inject(UsuariosService);
  private router = inject(Router);
  userRole: Usuario['role'] | null = null;
  currentUrl = '';

  private readonly AUTH_KEY = 'sgtc_auth';
  private readonly USER_KEY = 'sgtc_user';

  private get storedUser(): Partial<Usuario> | null {
    try {
      const raw = sessionStorage.getItem(this.USER_KEY) || localStorage.getItem(this.USER_KEY);
      return raw ? (JSON.parse(raw) as Partial<Usuario>) : null;
    } catch {
      return null;
    }
  }

  get userEmail(): string | null {
    return this.storedUser?.email ?? null;
  }

  get isLoggedIn(): boolean {
    return !!(
      sessionStorage.getItem(this.AUTH_KEY) ||
      localStorage.getItem(this.AUTH_KEY)
    );
  }

  get isAdmin(): boolean {
    return this.storedUser?.role === 'ADMIN';
  }

  get isAdminLinkVisible(): boolean {
    return this.isLoggedIn && this.isAdmin;
  }

  get isAuthRoute(): boolean {
    const path = this.currentUrl.split('?')[0];
    return path === '/login' || path === '/esqueci-senha' || path === '/redefinir-senha';
  }

  ngOnInit(): void {
    this.currentUrl = this.router.url;
    this.userRole = this.storedUser?.role ?? null;

    this.usuariosApi.getUsuarioAtual().subscribe((u) => {
      this.userRole = u?.role ?? null;
      if (u) {
        const userJson = JSON.stringify({ id: u.id, nome: u.nome, email: u.email, role: u.role, ra: u.ra ?? null });
        sessionStorage.setItem(this.USER_KEY, userJson);
        localStorage.setItem(this.USER_KEY, userJson);
      }
    });

    this.router.events.pipe(
      filter((event): event is NavigationEnd => event instanceof NavigationEnd)
    ).subscribe((event) => {
      this.currentUrl = event.urlAfterRedirects;
      this.userRole = this.storedUser?.role ?? null;
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
