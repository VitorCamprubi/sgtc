import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { GrupoService, GrupoCreateRequest } from '../../services/grupo.service';
import { UsuariosService, Usuario } from '../../services/usuarios.service';
import { FormsModule } from '@angular/forms';
import { GrupoResumoDTO } from '../../model/grupo-resumo.dto';

@Component({
  selector: 'app-grupos',
  standalone: true,
  templateUrl: './grupos.component.html',
  styleUrls: ['./grupos.component.scss'],
  imports: [CommonModule, RouterLink, FormsModule],
})
export class GruposComponent implements OnInit {
  private router = inject(Router);
  private gruposApi = inject(GrupoService);
  private usuariosApi = inject(UsuariosService);

  grupos = signal<GrupoResumoDTO[] | null>(null);
  error = signal<string | null>(null);
  isAdmin = signal<boolean>(false);
  usuarios = signal<Usuario[]>([]);
  orientadores = signal<Usuario[]>([]);

  ngOnInit(): void {
    this.carregarMeus();
    // carrega info de usuários para Ações de Admin
    this.usuariosApi.listarPublic().subscribe({
      next: (us) => {
        this.usuarios.set(us);
        this.orientadores.set(us.filter((u) => u.role === 'ORIENTADOR'));
      },
    });
    this.usuariosApi.getUsuarioAtualViaDebug().subscribe((u) => {
      this.isAdmin.set(u?.role === 'ADMIN');
    });
  }

  carregarMeus(): void {
    this.error.set(null);
    this.grupos.set(null);
    this.gruposApi.listarMeus().subscribe({
      next: (data) => this.grupos.set(data),
      error: (err) =>
        this.error.set(`${err.status ?? ''} ${err.statusText ?? 'Erro'}`.trim()),
    });
  }

  logout(ev?: Event) {
    ev?.preventDefault();
    sessionStorage.removeItem('sgtc_auth');
    localStorage.removeItem('sgtc_auth');
    sessionStorage.removeItem('sgtc_user');
    localStorage.removeItem('sgtc_user');
    this.router.navigateByUrl('/login');
  }

  get userEmail(): string | null {
    const raw =
      sessionStorage.getItem('sgtc_user') || localStorage.getItem('sgtc_user');
    try {
      return raw ? JSON.parse(raw).email ?? null : null;
    } catch {
      return null;
    }
  }

  // Admin: criar grupo
  novoTitulo = '';
  novoOrientadorId: number | null = null;
  novoCoorientadorId: number | null = null;

  criarGrupo() {
    this.error.set(null);
    const payload: GrupoCreateRequest = {
      titulo: this.novoTitulo.trim(),
      orientadorId: this.novoOrientadorId!,
      coorientadorId: this.novoCoorientadorId || null,
    };
    if (!payload.titulo || !payload.orientadorId) {
      this.error.set('Preencha título e orientador.');
      return;
    }
    this.gruposApi.criar(payload).subscribe({
      next: () => {
        this.novoTitulo = '';
        this.novoOrientadorId = null;
        this.novoCoorientadorId = null;
        this.carregarMeus();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }
}
