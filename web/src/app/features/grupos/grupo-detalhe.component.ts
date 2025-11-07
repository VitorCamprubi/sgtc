import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { DocumentosService, DocumentoVersao } from '../../services/documentos.service';
import { ComentariosService, Comentario } from '../../services/comentarios.service';
import { ReunioesService, Reuniao } from '../../services/reunioes.service';
import { UsuariosService, Usuario } from '../../services/usuarios.service';
import { GrupoService } from '../../services/grupo.service';

@Component({
  selector: 'app-grupo-detalhe',
  standalone: true,
  templateUrl: './grupo-detalhe.component.html',
  imports: [CommonModule, FormsModule],
})
export class GrupoDetalheComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private docs  = inject(DocumentosService);
  private comentarios = inject(ComentariosService);
  private reunioes = inject(ReunioesService);
  private usuarios = inject(UsuariosService);
  private grupos = inject(GrupoService);

  grupoId = 0;
  lista = signal<DocumentoVersao[] | null>(null);
  error = signal<string | null>(null);
  baixando = signal<number | null>(null);
  tituloDocumento = '';
  arquivoSelecionado: File | null = null;

  // Usuário atual / papéis
  currentUser = signal<Usuario | null>(null);

  // Comentários
  docSelecionado = signal<DocumentoVersao | null>(null);
  comentariosDoc = signal<Comentario[] | null>(null);
  novoComentario = '';

  // Reuniões
  listaReunioes = signal<Reuniao[] | null>(null);
  novaDataHora = '';
  novaPauta = '';
  novasObs = '';

  // Admin - adicionar membros
  isAdmin = signal<boolean>(false);
  alunos = signal<Usuario[]>([]);
  membrosSelecionados = signal<number[]>([]);

  ngOnInit() {
    this.grupoId = Number(this.route.snapshot.paramMap.get('id'));
    this.recarregar();
    this.recarregarReunioes();
    // Carrega contexto de usuário e alunos
    this.usuarios.getUsuarioAtualViaDebug().subscribe((u) => {
      this.isAdmin.set(u?.role === 'ADMIN');
      this.currentUser.set(u);
    });
    this.usuarios.listarPublic().subscribe((list) => {
      this.alunos.set(list.filter((u) => u.role === 'ALUNO'));
    });
  }

  recarregar() {
    this.error.set(null);
    this.docs.lista(this.grupoId).subscribe({
      next: (d) => this.lista.set(d),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  onFileSelected(ev: Event) {
    const input = ev.target as HTMLInputElement | null;
    this.arquivoSelecionado = input?.files?.[0] ?? null;
  }

  uploadFromState(fileInput: HTMLInputElement) {
    const file = this.arquivoSelecionado;
    const titulo = this.tituloDocumento.trim();
    if (!file || !titulo) {
      this.error.set('Informe título e selecione um arquivo.');
      return;
    }
    this.docs.upload(this.grupoId, titulo, file).subscribe({
      next: () => {
        this.recarregar();
        this.tituloDocumento = '';
        this.arquivoSelecionado = null;
        if (fileInput) fileInput.value = '';
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  canDelete(doc: DocumentoVersao): boolean {
    const u = this.currentUser();
    if (!u) return false;
    if (u.role === 'ADMIN' || u.role === 'ORIENTADOR' || u.role === 'COORIENTADOR') return true;
    return u.nome === doc.enviadoPor; // fallback por nome
  }

  excluir(doc: DocumentoVersao) {
    if (!this.canDelete(doc)) return;
    if (!confirm(`Excluir documento "${doc.titulo}" v${doc.versao}?`)) return;
    this.docs.delete(doc.id).subscribe({
      next: () => this.recarregar(),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  onDownload(doc: DocumentoVersao) {
    this.baixando.set(doc.id);
    this.docs.download(doc.id).subscribe({
      next: (res) => {
        const blob = res.body as Blob;
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = `${doc.titulo}_v${doc.versao}`;
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
      complete: () => this.baixando.set(null),
    });
  }

  selecionarDoc(doc: DocumentoVersao) {
    this.docSelecionado.set(doc);
    this.novoComentario = '';
    this.comentariosDoc.set(null);
    this.comentarios.listar(doc.id).subscribe({
      next: (list) => this.comentariosDoc.set(list),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  enviarComentario() {
    const doc = this.docSelecionado();
    const texto = this.novoComentario.trim();
    if (!doc || !texto) return;
    this.comentarios.comentar(doc.id, texto).subscribe({
      next: () => {
        this.novoComentario = '';
        this.selecionarDoc(doc);
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  recarregarReunioes() {
    this.listaReunioes.set(null);
    this.reunioes.listar(this.grupoId).subscribe({
      next: (r) => this.listaReunioes.set(r),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  agendarReuniao() {
    const dataHora = this.novaDataHora;
    const pauta = this.novaPauta.trim();
    const observacoes = this.novasObs.trim();
    if (!dataHora || !pauta) return;
    this.reunioes
      .agendar(this.grupoId, { dataHora, pauta, observacoes })
      .subscribe({
        next: () => {
          this.novaDataHora = '';
          this.novaPauta = '';
          this.novasObs = '';
          this.recarregarReunioes();
        },
        error: (e) => this.error.set(`${e.status} ${e.statusText}`),
      });
  }

  adicionarMembros() {
    const ids = this.membrosSelecionados();
    if (!ids.length) return;
    this.grupos.adicionarMembros(this.grupoId, ids).subscribe({
      next: () => this.membrosSelecionados.set([]),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  onMembrosChange(ev: Event) {
    const select = ev.target as HTMLSelectElement | null;
    if (!select) return;
    const ids = Array.from(select.selectedOptions).map((o) => Number(o.value));
    this.membrosSelecionados.set(ids);
  }
}
