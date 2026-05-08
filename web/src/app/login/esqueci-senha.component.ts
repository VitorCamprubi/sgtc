import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { UsuariosService } from '../services/usuarios.service';

@Component({
  standalone: true,
  selector: 'app-esqueci-senha',
  templateUrl: './esqueci-senha.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule, RouterLink],
})
export class EsqueciSenhaComponent {
  private usuariosApi = inject(UsuariosService);

  email = '';
  loading = false;
  message: string | null = null;
  error: string | null = null;

  submit(): void {
    this.message = null;
    this.error = null;
    const email = this.email.trim();
    if (!email) {
      this.error = 'Informe seu e-mail.';
      return;
    }
    this.loading = true;
    this.usuariosApi.solicitarRecuperacao(email).subscribe({
      next: () => {
        this.loading = false;
        this.message =
          'Se o e-mail estiver cadastrado, enviamos um link para redefinir sua senha (válido por 1 hora). Verifique sua caixa de entrada.';
      },
      error: () => {
        // Por segurança, mesma mensagem em caso de erro genérico
        this.loading = false;
        this.message =
          'Se o e-mail estiver cadastrado, enviamos um link para redefinir sua senha (válido por 1 hora). Verifique sua caixa de entrada.';
      },
    });
  }
}
