import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { forkJoin } from 'rxjs';
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
  arquivosAprovados = signal<GrupoResumoDTO[] | null>(null);
  arquivosReprovados = signal<GrupoResumoDTO[] | null>(null);
  error = signal<string | null>(null);
  isAdmin = signal<boolean>(false);
  podeVerArquivos = signal<boolean>(false);
  abaAtiva = signal<'grupos' | 'arquivos'>('grupos');
  professores = signal<Usuario[]>([]);
  deletando = signal<number | null>(null);
  definindoNota = signal<number | null>(null);
  editandoGrupoId = signal<number | null>(null);
  buscaArquivos = signal<string>('');

  novoTitulo = '';
  novaMateria: 'TG' | 'PTG' | null = null;
  novoOrientadorId: number | null = null;
  novoCoorientadorId: number | null = null;

  editarTitulo = '';
  editarMateria: 'TG' | 'PTG' | null = null;
  editarOrientadorId: number | null = null;
  editarCoorientadorId: number | null = null;

  ngOnInit(): void {
    this.usuariosApi.getUsuarioAtual().subscribe((u) => {
      const admin = u?.role === 'ADMIN';
      this.isAdmin.set(admin);
      this.podeVerArquivos.set(u?.role === 'ADMIN' || u?.role === 'PROFESSOR');
      if (admin) {
        this.carregarProfessores();
      }
    });

    this.carregarMeus();
  }

  carregarMeus(): void {
    this.abaAtiva.set('grupos');
    this.error.set(null);
    this.grupos.set(null);
    this.arquivosAprovados.set(null);
    this.arquivosReprovados.set(null);
    this.gruposApi.listarMeus().subscribe({
      next: (data) => this.grupos.set(data),
      error: (err) => {
        this.grupos.set([]);
        this.error.set(`${err.status ?? ''} ${err.statusText ?? 'Erro'}`.trim());
      },
    });
  }

  carregarArquivos(): void {
    if (!this.podeVerArquivos()) return;
    this.abaAtiva.set('arquivos');
    this.cancelarEdicaoGrupo();
    this.error.set(null);
    this.grupos.set(null);
    this.arquivosAprovados.set(null);
    this.arquivosReprovados.set(null);

    const busca = this.buscaArquivos().trim();
    forkJoin({
      aprovados: this.gruposApi.listarArquivosAprovados(busca),
      reprovados: this.gruposApi.listarArquivosReprovados(busca),
    }).subscribe({
      next: ({ aprovados, reprovados }) => {
        this.arquivosAprovados.set(aprovados);
        this.arquivosReprovados.set(reprovados);
      },
      error: (err) => {
        this.arquivosAprovados.set([]);
        this.arquivosReprovados.set([]);
        this.error.set(`${err.status ?? ''} ${err.statusText ?? 'Erro'}`.trim());
      },
    });
  }

  carregarAbaAtual(): void {
    if (this.abaAtiva() === 'arquivos') {
      this.carregarArquivos();
      return;
    }
    this.carregarMeus();
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

  definirNotaFinal(g: GrupoResumoDTO) {
    if (!this.isAdmin() || this.abaAtiva() !== 'grupos') return;

    const entrada = prompt(`Defina a nota final do grupo "${g.titulo}" (0 a 10):`, '6');
    if (entrada === null) return;

    const nota = this.parseNota(entrada);
    if (nota === null || nota < 0 || nota > 10) {
      this.error.set('Informe uma nota valida entre 0 e 10.');
      return;
    }

    this.error.set(null);
    this.definindoNota.set(g.id);
    this.gruposApi.definirNotaFinal(g.id, nota).subscribe({
      next: () => {
        this.definindoNota.set(null);
        this.carregarAbaAtual();
      },
      error: (e) => {
        this.definindoNota.set(null);
        this.error.set(`${e.status} ${e.statusText}`);
      },
    });
  }

  buscarNosArquivos(): void {
    if (!this.podeVerArquivos()) return;
    if (this.abaAtiva() !== 'arquivos') {
      this.abaAtiva.set('arquivos');
    }
    this.carregarArquivos();
  }

  limparBuscaArquivos(): void {
    this.buscaArquivos.set('');
    if (this.abaAtiva() === 'arquivos') {
      this.carregarArquivos();
    }
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
    if (!this.isAdmin() || !grupoId || this.abaAtiva() !== 'grupos') return;

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
        this.carregarAbaAtual();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  statusGrupoLabel(status: GrupoResumoDTO['status']): string {
    switch (status) {
      case 'EM_CURSO':
        return 'Em curso';
      case 'APROVADO':
        return 'Aprovado';
      case 'REPROVADO':
        return 'Reprovado';
      default:
        return status;
    }
  }

  statusGrupoClass(status: GrupoResumoDTO['status']): string {
    switch (status) {
      case 'EM_CURSO':
        return 'badge-status em-curso';
      case 'APROVADO':
        return 'badge-status aprovado';
      case 'REPROVADO':
        return 'badge-status reprovado';
      default:
        return 'badge-status';
    }
  }

  totalArquivos(): number {
    const aprovados = this.arquivosAprovados()?.length ?? 0;
    const reprovados = this.arquivosReprovados()?.length ?? 0;
    return aprovados + reprovados;
  }

  private parseNota(valor: string): number | null {
    const normalizado = valor.replace(',', '.').trim();
    if (!normalizado) return null;

    const numero = Number(normalizado);
    if (!Number.isFinite(numero)) return null;

    return Math.round(numero * 100) / 100;
  }

  private carregarProfessores(): void {
    this.usuariosApi.listarAdmin('PROFESSOR').subscribe({
      next: (professores) => this.professores.set(professores),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }
}
