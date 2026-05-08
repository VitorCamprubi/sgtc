import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterLink } from '@angular/router';
import { HttpErrorResponse } from '@angular/common/http';
import { UsuariosService } from '../services/usuarios.service';

@Component({
  standalone: true,
  selector: 'app-redefinir-senha',
  templateUrl: './redefinir-senha.component.html',
  styleUrls: ['./login.component.scss'],
  imports: [CommonModule, FormsModule, RouterLink],
})
export class RedefinirSenhaComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private usuariosApi = inject(UsuariosService);

  token = '';
  novaSenha = '';
  confirmarSenha = '';
  loading = false;
  done = false;
  error: string | null = null;

  ngOnInit(): void {
    this.token = this.route.snapshot.queryParamMap.get('token') ?? '';
    if (!this.token) {
      this.error = 'Link inválido. Solicite uma nova recuperação.';
    }
  }

  submit(): void {
    this.error = null;
    if (!this.token) {
      this.error = 'Link inválido. Solicite uma nova recuperação.';
      return;
    }
    const senha = this.novaSenha;
    if (!senha || senha.length < 8) {
      this.error = 'A senha deve ter pelo menos 8 caracteres.';
      return;
    }
    if (!/[A-Za-z]/.test(senha) || !/[0-9]/.test(senha)) {
      this.error = 'A senha deve conter letras e números.';
      return;
    }
    if (senha !== this.confirmarSenha) {
      this.error = 'As duas senhas não conferem.';
      return;
    }

    this.loading = true;
    this.usuariosApi.redefinirSenha(this.token, senha).subscribe({
      next: () => {
        this.loading = false;
        this.done = true;
        setTimeout(() => this.router.navigateByUrl('/login'), 4000);
      },
      error: (err: HttpErrorResponse) => {
        this.loading = false;
        if (err.status === 410) {
          this.error = 'Link expirado. Solicite uma nova recuperação.';
        } else if (err.status === 404) {
          this.error = 'Link inválido. Solicite uma nova recuperação.';
        } else if (err.status === 400) {
          const msg = (err.error?.message as string | undefined) ?? null;
          this.error = msg ?? 'Não foi possível redefinir a senha. Verifique a senha informada.';
        } else {
          this.error = 'Falha ao redefinir a senha. Tente novamente mais tarde.';
        }
      },
    });
  }
}
