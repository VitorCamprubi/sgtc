<<<<<<< HEAD
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
=======
import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
import { HttpClient, HttpErrorResponse } from '@angular/common/http';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule],
})
<<<<<<< HEAD
export class LoginComponent implements OnInit {
  private router = inject(Router);
  private route = inject(ActivatedRoute);
=======
export class LoginComponent {
  private router = inject(Router);
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
  private http = inject(HttpClient);

  email = '';
  password = '';
  error: string | null = null;
<<<<<<< HEAD
  info: string | null = null;
  loading = false;
  /** Mostra o botao "reenviar email de confirmacao" quando o usuario tenta logar sem ter verificado. */
  showResend = false;
  resendingMessage: string | null = null;
  resending = false;

  ngOnInit(): void {
    // Le o query param ?verificacao=ok|expirado|invalido|erro vindo do redirect
    // do endpoint GET /api/auth/verify-email
    const status = this.route.snapshot.queryParamMap.get('verificacao');
    if (status === 'ok') {
      this.info = 'E-mail confirmado com sucesso! Voce ja pode entrar.';
    } else if (status === 'expirado') {
      this.error = 'O link de confirmacao expirou. Informe seu e-mail abaixo e clique em "Reenviar confirmacao".';
      this.showResend = true;
    } else if (status === 'invalido') {
      this.error = 'Link de confirmacao invalido ou ja utilizado.';
    } else if (status === 'erro') {
      this.error = 'Nao foi possivel confirmar o e-mail. Tente novamente mais tarde.';
    }
  }

  submit(): void {
    this.error = null;
    this.info = null;
    this.showResend = false;
    this.resendingMessage = null;

=======
  loading = false;

  submit(): void {
    this.error = null;
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
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
<<<<<<< HEAD
        if (err.status === 401) {
          this.error = 'Credenciais invalidas.';
        } else if (err.status === 403) {
          // E-mail ainda nao confirmado: oferece o reenvio
          this.error =
            'E-mail ainda nao confirmado. Verifique sua caixa de entrada ou clique em "Reenviar confirmacao".';
          this.showResend = true;
        } else {
          this.error = 'Falha ao autenticar.';
        }
=======
        this.error = err.status === 401 ? 'Credenciais invalidas.' : 'Falha ao autenticar.';
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
        sessionStorage.removeItem('sgtc_auth');
        localStorage.removeItem('sgtc_auth');
        sessionStorage.removeItem('sgtc_user');
        localStorage.removeItem('sgtc_user');
        this.loading = false;
      },
    });
  }

<<<<<<< HEAD
  reenviarConfirmacao(): void {
    const email = this.email.trim();
    if (!email) {
      this.resendingMessage = 'Informe seu e-mail no campo acima antes de reenviar.';
      return;
    }
    this.resending = true;
    this.resendingMessage = null;
    this.http.post<void>('/api/auth/resend-verification', { email }).subscribe({
      next: () => {
        this.resending = false;
        this.resendingMessage =
          'Se o e-mail estiver cadastrado, um novo link de confirmacao foi enviado.';
      },
      error: () => {
        this.resending = false;
        // Por seguranca o backend nao revela se o email existe; reportamos o mesmo texto
        this.resendingMessage =
          'Se o e-mail estiver cadastrado, um novo link de confirmacao foi enviado.';
      },
    });
  }

=======
>>>>>>> 4907f041c88e3fc897e86cccf1262a32da26fe88
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
