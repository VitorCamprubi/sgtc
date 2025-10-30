import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Router } from '@angular/router';

@Component({
  standalone: true,
  selector: 'app-login',
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule],
})
export class LoginComponent {
  private router = inject(Router);

  email = '';
  password = '';
  error: string | null = null;

  submit(): void {
    this.error = null;
    const email = this.email.trim();
    const password = this.password;
    if (!email || !password) {
      this.error = 'Informe e-mail e senha.';
      return;
    }

    try {
      const basic = 'Basic ' + btoa(`${email}:${password}`);

      // Persistimos para o interceptor e o guard
      sessionStorage.setItem('sgtc_auth', basic);
      localStorage.setItem('sgtc_auth', basic);
      const userJson = JSON.stringify({ email });
      sessionStorage.setItem('sgtc_user', userJson);
      localStorage.setItem('sgtc_user', userJson);

      this.router.navigateByUrl('/grupos');
    } catch {
      this.error = 'Falha ao codificar credenciais.';
    }
  }
}
