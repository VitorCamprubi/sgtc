import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { GrupoResumoDTO } from '../../model/grupo-resumo.dto';
import { GrupoCreateRequest, GrupoService } from '../../services/grupo.service';
import { Usuario, UsuariosService } from '../../services/usuarios.service';

@Component({
  selector: 'app-grupos',
  standalone: true,
  templateUrl: './grupos.component.html',
  styleUrls: ['./grupos.component.scss'],
  imports: [CommonModule, RouterLink, FormsModule],
})
export class GruposComponent implements OnInit {
  private gruposApi = inject(GrupoService);
  private usuariosApi = inject(UsuariosService);

  grupos = signal<GrupoResumoDTO[] | null>(null);
  error = signal<string | null>(null);
  isAdmin = signal<boolean>(false);
  professores = signal<Usuario[]>([]);
  deletando = signal<number | null>(null);
  editandoGrupoId = signal<number | null>(null);

  novoTitulo = '';
  novaMateria: 'TG' | 'PTG' | null = null;
  novoOrientadorId: number | null = null;
  novoCoorientadorId: number | null = null;

  editarTitulo = '';
  editarMateria: 'TG' | 'PTG' | null = null;
  editarOrientadorId: number | null = null;
  editarCoorientadorId: number | null = null;

  ngOnInit(): void {
    this.carregarMeus();

    this.usuariosApi.getUsuarioAtual().subscribe((u) => {
      const admin = u?.role === 'ADMIN';
      this.isAdmin.set(admin);
      if (admin) {
        this.carregarProfessores();
      }
    });
  }

  carregarMeus(): void {
    this.error.set(null);
    this.grupos.set(null);
    this.gruposApi.listarMeus().subscribe({
      next: (data) => this.grupos.set(data),
      error: (err) => this.error.set(`${err.status ?? ''} ${err.statusText ?? 'Erro'}`.trim()),
    });
  }

  excluirGrupo(id: number, titulo: string) {
    if (!this.isAdmin()) return;
    if (!confirm(`Excluir o grupo "${titulo}"? Esta acao eh definitiva.`)) return;

    this.error.set(null);
    this.deletando.set(id);
    this.gruposApi.deletar(id).subscribe({
      next: () => {
        this.deletando.set(null);
        this.carregarMeus();
      },
      error: (e) => {
        this.deletando.set(null);
        this.error.set(`${e.status} ${e.statusText}`);
      },
    });
  }

  criarGrupo(): void {
    this.error.set(null);

    const payload: GrupoCreateRequest = {
      titulo: this.novoTitulo.trim(),
      materia: this.novaMateria!,
      orientadorId: this.novoOrientadorId!,
      coorientadorId: this.novoCoorientadorId || null,
    };

    if (!payload.titulo || !payload.orientadorId || !payload.materia) {
      this.error.set('Preencha titulo, materia e orientador.');
      return;
    }

    if (payload.orientadorId === payload.coorientadorId) {
      this.error.set('O mesmo professor nao pode ser orientador e coorientador no mesmo grupo.');
      return;
    }

    this.gruposApi.criar(payload).subscribe({
      next: () => {
        this.novoTitulo = '';
        this.novaMateria = null;
        this.novoOrientadorId = null;
        this.novoCoorientadorId = null;
        this.carregarMeus();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  iniciarEdicaoGrupo(g: GrupoResumoDTO): void {
    if (!this.isAdmin()) return;
    this.editandoGrupoId.set(g.id);
    this.editarTitulo = g.titulo;
    this.editarMateria = g.materia;
    this.editarOrientadorId = g.orientadorId;
    this.editarCoorientadorId = g.coorientadorId;
  }

  cancelarEdicaoGrupo(): void {
    this.editandoGrupoId.set(null);
    this.editarTitulo = '';
    this.editarMateria = null;
    this.editarOrientadorId = null;
    this.editarCoorientadorId = null;
  }

  salvarEdicaoGrupo(): void {
    const grupoId = this.editandoGrupoId();
    if (!this.isAdmin() || !grupoId) return;

    const payload: GrupoCreateRequest = {
      titulo: this.editarTitulo.trim(),
      materia: this.editarMateria!,
      orientadorId: this.editarOrientadorId!,
      coorientadorId: this.editarCoorientadorId || null,
    };

    if (!payload.titulo || !payload.orientadorId || !payload.materia) {
      this.error.set('Preencha titulo, materia e orientador para editar o grupo.');
      return;
    }

    if (payload.orientadorId === payload.coorientadorId) {
      this.error.set('O mesmo professor nao pode ser orientador e coorientador no mesmo grupo.');
      return;
    }

    this.error.set(null);
    this.gruposApi.atualizar(grupoId, payload).subscribe({
      next: () => {
        this.cancelarEdicaoGrupo();
        this.carregarMeus();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  private carregarProfessores(): void {
    this.usuariosApi.listarAdmin('PROFESSOR').subscribe({
      next: (professores) => this.professores.set(professores),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }
}
