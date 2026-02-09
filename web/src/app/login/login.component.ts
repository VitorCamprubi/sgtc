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
    this.http
      .post<LoginResponse>('/api/auth/login', { email, senha: password })
      .subscribe({
        next: (res) => {
          const token = res.type ? `${res.type} ${res.token}` : `Bearer ${res.token}`;
          sessionStorage.setItem('sgtc_auth', token);
          localStorage.setItem('sgtc_auth', token);
          const userJson = JSON.stringify({ email });
          sessionStorage.setItem('sgtc_user', userJson);
          localStorage.setItem('sgtc_user', userJson);
          this.router.navigateByUrl('/grupos');
        },
        error: (err: HttpErrorResponse) => {
          this.error =
            err.status === 401 ? 'Credenciais inválidas.' : 'Falha ao autenticar.';
          sessionStorage.removeItem('sgtc_auth');
          localStorage.removeItem('sgtc_auth');
        },
        complete: () => (this.loading = false),
      });
  }
}

interface LoginResponse {
  token: string;
  type?: string;
}
