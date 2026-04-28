import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule],
})
export class LoginComponent {
  private router = inject(Router);
  private http = inject(HttpClient);

  email = '';
  password = '';
  error: string | null = null;
  loading = false;

  submit(): void {
    this.error = null;
    const email = this.email.trim();
    const password = this.password;
    if (!email || !password) {
      this.error = 'Informe e-mail e senha.';
      return;
    }

    this.loading = true;
    this.http.post<LoginResponse>('/api/auth/login', { email, senha: password }).subscribe({
      next: (res) => {
        const token = res.type ? `${res.type} ${res.token}` : `Bearer ${res.token}`;
        sessionStorage.setItem('sgtc_auth', token);
        localStorage.setItem('sgtc_auth', token);

        const payload = this.readJwtPayload(res.token);
        const role = this.readRole(payload);
        const subjectEmail = this.readSubjectEmail(payload) ?? email;
        const userJson = JSON.stringify({ email: subjectEmail, role });
        sessionStorage.setItem('sgtc_user', userJson);
        localStorage.setItem('sgtc_user', userJson);

        this.loading = false;
        this.router.navigateByUrl('/grupos');
      },
      error: (err: HttpErrorResponse) => {
        this.error = err.status === 401 ? 'Credenciais invalidas.' : 'Falha ao autenticar.';
        sessionStorage.removeItem('sgtc_auth');
        localStorage.removeItem('sgtc_auth');
        sessionStorage.removeItem('sgtc_user');
        localStorage.removeItem('sgtc_user');
        this.loading = false;
      },
    });
  }

  private readJwtPayload(token: string): Record<string, unknown> | null {
    try {
      const parts = token.split('.');
      if (parts.length < 2) return null;

      const base64 = parts[1].replace(/-/g, '+').replace(/_/g, '/');
      const padded = base64.padEnd(Math.ceil(base64.length / 4) * 4, '=');
      return JSON.parse(atob(padded)) as Record<string, unknown>;
    } catch {
      return null;
    }
  }

  private readRole(payload: Record<string, unknown> | null): UserRole | null {
    const role = payload?.['role'];
    return role === 'ADMIN' || role === 'PROFESSOR' || role === 'ALUNO' ? role : null;
  }

  private readSubjectEmail(payload: Record<string, unknown> | null): string | null {
    const subject = payload?.['sub'];
    return typeof subject === 'string' ? subject : null;
  }
}

interface LoginResponse {
  token: string;
  type?: string;
}

type UserRole = 'ADMIN' | 'PROFESSOR' | 'ALUNO';
