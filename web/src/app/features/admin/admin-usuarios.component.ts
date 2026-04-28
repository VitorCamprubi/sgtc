import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Usuario, UsuarioAdminPayload, UsuariosService } from '../../services/usuarios.service';

@Component({
  selector: 'app-admin-usuarios',
  standalone: true,
  templateUrl: './admin-usuarios.component.html',
  styleUrls: ['./admin-usuarios.component.scss'],
  imports: [CommonModule, FormsModule],
})
export class AdminUsuariosComponent implements OnInit {
  private usuariosApi = inject(UsuariosService);

  usuariosAdmin = signal<Usuario[] | null>(null);
  adminError = signal<string | null>(null);
  adminLoading = signal<boolean>(false);
  adminRoleFiltro = signal<'ALUNO' | 'PROFESSOR'>('ALUNO');
  editandoId = signal<number | null>(null);

  formNome = '';
  formEmail = '';
  formSenha = '';
  formRa: string | null = null;

  ngOnInit(): void {
    this.carregarUsuariosAdmin(this.adminRoleFiltro());
  }

  carregarUsuariosAdmin(role: 'ALUNO' | 'PROFESSOR' = this.adminRoleFiltro()) {
    if (role !== this.adminRoleFiltro()) {
      this.resetFormUsuario();
    }
    this.adminRoleFiltro.set(role);
    this.adminError.set(null);
    this.adminLoading.set(true);
    this.usuariosAdmin.set(null);

    this.usuariosApi.listarAdmin(role).subscribe({
      next: (list) => {
        this.usuariosAdmin.set(list);
        this.adminLoading.set(false);
      },
      error: (e) => {
        this.adminLoading.set(false);
        this.adminError.set(`${e.status} ${e.statusText}`);
      },
    });
  }

  private payloadAtual(): UsuarioAdminPayload | null {
    const nome = this.formNome.trim();
    const email = this.formEmail.trim();
    const role = this.adminRoleFiltro();
    const senha = this.formSenha.trim();
    const ra = role === 'ALUNO' ? this.formRa?.trim() || null : null;

    if (!nome || !email) {
      this.adminError.set('Preencha nome e email.');
      return null;
    }
    if (role === 'ALUNO' && !ra) {
      this.adminError.set('RA e obrigatorio para cadastro de aluno.');
      return null;
    }

    const payload: UsuarioAdminPayload = { nome, email, role, ra };
    if (senha) payload.senha = senha;
    return payload;
  }

  salvarUsuario() {
    const payload = this.payloadAtual();
    if (!payload) return;

    const id = this.editandoId();
    this.adminError.set(null);

    const req = id
      ? this.usuariosApi.atualizarAdmin(id, payload)
      : this.usuariosApi.criarAdmin(payload);

    req.subscribe({
      next: () => {
        this.resetFormUsuario();
        this.carregarUsuariosAdmin(this.adminRoleFiltro());
      },
      error: (e) => this.adminError.set(`${e.status} ${e.statusText}`),
    });
  }

  editarUsuario(u: Usuario) {
    this.editandoId.set(u.id);
    this.formNome = u.nome;
    this.formEmail = u.email;
    this.formRa = u.role === 'ALUNO' ? u.ra ?? null : null;
    this.formSenha = '';
  }

  excluirUsuario(u: Usuario) {
    if (!confirm(`Excluir o usuario "${u.nome}"?`)) return;

    this.adminError.set(null);
    this.usuariosApi.excluirAdmin(u.id).subscribe({
      next: () => {
        if (this.editandoId() === u.id) this.resetFormUsuario();
        this.carregarUsuariosAdmin(this.adminRoleFiltro());
      },
      error: (e) => this.adminError.set(`${e.status} ${e.statusText}`),
    });
  }

  resetFormUsuario() {
    this.editandoId.set(null);
    this.formNome = '';
    this.formEmail = '';
    this.formSenha = '';
    this.formRa = null;
  }

  roleCadastroLabel(): string {
    return this.adminRoleFiltro() === 'ALUNO' ? 'Aluno' : 'Professor';
  }
}
