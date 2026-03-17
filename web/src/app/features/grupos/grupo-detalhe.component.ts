import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faComments, faDownload, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { Comentario, ComentariosService } from '../../services/comentarios.service';
import { DocumentoVersao, DocumentosService } from '../../services/documentos.service';
import { GrupoMembroDTO, GrupoService } from '../../services/grupo.service';
import { Reuniao, ReunioesService } from '../../services/reunioes.service';
import { Usuario, UsuariosService } from '../../services/usuarios.service';

@Component({
  selector: 'app-grupo-detalhe',
  standalone: true,
  templateUrl: './grupo-detalhe.component.html',
  styleUrls: ['./grupo-detalhe.component.scss'],
  imports: [CommonModule, FormsModule, FontAwesomeModule],
})
export class GrupoDetalheComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private docs = inject(DocumentosService);
  private comentarios = inject(ComentariosService);
  private reunioes = inject(ReunioesService);
  private usuarios = inject(UsuariosService);
  private grupos = inject(GrupoService);

  faDownload = faDownload;
  faComments = faComments;
  faTrashCan = faTrashCan;

  grupoId = 0;
  lista = signal<DocumentoVersao[] | null>(null);
  error = signal<string | null>(null);
  baixando = signal<number | null>(null);
  tituloDocumento = '';
  arquivoSelecionado: File | null = null;

  currentUser = signal<Usuario | null>(null);

  docSelecionado = signal<DocumentoVersao | null>(null);
  comentariosDoc = signal<Comentario[] | null>(null);
  novoComentario = '';
  editandoComentarioId = signal<number | null>(null);
  textoComentarioEditando = '';
  editandoDocumentoId = signal<number | null>(null);
  tituloDocumentoEditando = '';

  listaReunioes = signal<Reuniao[] | null>(null);
  novaDataHora = '';
  novaPauta = '';
  novasObs = '';
  editandoReuniaoId = signal<number | null>(null);
  editarDataHora = '';
  editarPauta = '';
  editarObservacoes = '';

  isAdmin = signal<boolean>(false);
  alunos = signal<Usuario[]>([]);
  membrosGrupo = signal<GrupoMembroDTO[] | null>(null);
  membrosSelecionados = signal<number[]>([]);
  removendoMembroId = signal<number | null>(null);

  alunosDisponiveis = computed(() => {
    const idsMembros = new Set((this.membrosGrupo() ?? []).map((m) => m.id));
    return this.alunos().filter((a) => !idsMembros.has(a.id));
  });

  canComment = computed(() => {
    const role = this.currentUser()?.role;
    return role === 'ADMIN' || role === 'PROFESSOR';
  });

  canSchedule = this.canComment;

  ngOnInit() {
    this.grupoId = Number(this.route.snapshot.paramMap.get('id'));
    if (!Number.isFinite(this.grupoId) || this.grupoId <= 0) {
      this.error.set('Grupo invalido.');
      return;
    }

    this.recarregar();
    this.recarregarReunioes();
    this.recarregarMembros();

    this.usuarios.getUsuarioAtual().subscribe((u) => {
      this.currentUser.set(u);
      const admin = u?.role === 'ADMIN';
      this.isAdmin.set(admin);
      if (admin) {
        this.usuarios.listarAdmin('ALUNO').subscribe({
          next: (list) => this.alunos.set(list),
          error: (e) => this.error.set(`${e.status} ${e.statusText}`),
        });
      }
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
      this.error.set('Informe titulo e selecione um arquivo.');
      return;
    }

    this.docs.upload(this.grupoId, titulo, file).subscribe({
      next: () => {
        this.recarregar();
        this.tituloDocumento = '';
        this.arquivoSelecionado = null;
        fileInput.value = '';
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  canDelete(doc: DocumentoVersao): boolean {
    const u = this.currentUser();
    if (!u) return false;
    if (u.role === 'ADMIN' || u.role === 'PROFESSOR') return true;
    return u.id === doc.enviadoPorId;
  }

  excluir(doc: DocumentoVersao) {
    if (!this.canDelete(doc)) return;
    if (!confirm(`Excluir documento "${doc.titulo}" v${doc.versao}?`)) return;

    this.docs.delete(doc.id).subscribe({
      next: () => this.recarregar(),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  iniciarEdicaoDocumento(doc: DocumentoVersao) {
    if (!this.canDelete(doc)) return;
    this.editandoDocumentoId.set(doc.id);
    this.tituloDocumentoEditando = doc.titulo;
  }

  cancelarEdicaoDocumento() {
    this.editandoDocumentoId.set(null);
    this.tituloDocumentoEditando = '';
  }

  salvarEdicaoDocumento(doc: DocumentoVersao) {
    if (!this.canDelete(doc)) return;

    const titulo = this.tituloDocumentoEditando.trim();
    if (!titulo) {
      this.error.set('Informe um titulo valido para o documento.');
      return;
    }

    this.docs.atualizarTitulo(doc.id, titulo).subscribe({
      next: (atualizado) => {
        this.cancelarEdicaoDocumento();
        this.recarregar();

        const selecionado = this.docSelecionado();
        if (selecionado?.id === atualizado.id) {
          this.docSelecionado.set({ ...selecionado, titulo: atualizado.titulo });
        }
      },
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
    if (!doc || !texto || !this.canComment()) return;

    this.comentarios.comentar(doc.id, texto).subscribe({
      next: () => {
        this.novoComentario = '';
        this.selecionarDoc(doc);
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  podeGerenciarComentario(c: Comentario): boolean {
    const u = this.currentUser();
    if (!u) return false;
    return u.role === 'ADMIN' || u.id === c.autorId;
  }

  iniciarEdicaoComentario(c: Comentario) {
    if (!this.podeGerenciarComentario(c)) return;
    this.editandoComentarioId.set(c.id);
    this.textoComentarioEditando = c.texto;
  }

  cancelarEdicaoComentario() {
    this.editandoComentarioId.set(null);
    this.textoComentarioEditando = '';
  }

  salvarEdicaoComentario(c: Comentario) {
    if (!this.podeGerenciarComentario(c)) return;

    const texto = this.textoComentarioEditando.trim();
    if (!texto) {
      this.error.set('Comentario nao pode ser vazio.');
      return;
    }

    const doc = this.docSelecionado();
    if (!doc) return;

    this.comentarios.atualizar(c.id, texto).subscribe({
      next: () => {
        this.cancelarEdicaoComentario();
        this.selecionarDoc(doc);
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  excluirComentario(c: Comentario) {
    if (!this.podeGerenciarComentario(c)) return;
    if (!confirm('Excluir este comentario?')) return;

    const doc = this.docSelecionado();
    if (!doc) return;

    this.comentarios.excluir(c.id).subscribe({
      next: () => this.selecionarDoc(doc),
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

  recarregarMembros() {
    this.membrosGrupo.set(null);
    this.grupos.listarMembros(this.grupoId).subscribe({
      next: (membros) => this.membrosGrupo.set(membros),
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  agendarReuniao() {
    if (!this.canSchedule()) return;

    const dataHora = this.novaDataHora;
    const pauta = this.novaPauta.trim();
    const observacoes = this.novasObs.trim();

    if (!dataHora || !pauta) {
      alert('E necessario informar data/hora e pauta.');
      return;
    }

    this.reunioes.agendar(this.grupoId, { dataHora, pauta, observacoes }).subscribe({
      next: () => {
        this.novaDataHora = '';
        this.novaPauta = '';
        this.novasObs = '';
        this.recarregarReunioes();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  iniciarEdicaoReuniao(r: Reuniao) {
    if (!this.canSchedule()) return;
    this.editandoReuniaoId.set(r.id);
    this.editarDataHora = this.toDateTimeLocalValue(r.dataHora);
    this.editarPauta = r.pauta;
    this.editarObservacoes = r.observacoes ?? '';
  }

  cancelarEdicaoReuniao() {
    this.editandoReuniaoId.set(null);
    this.editarDataHora = '';
    this.editarPauta = '';
    this.editarObservacoes = '';
  }

  salvarEdicaoReuniao() {
    const reuniaoId = this.editandoReuniaoId();
    if (!this.canSchedule() || !reuniaoId) return;

    const dataHora = this.editarDataHora;
    const pauta = this.editarPauta.trim();
    const observacoes = this.editarObservacoes.trim();

    if (!dataHora || !pauta) {
      this.error.set('E necessario informar data/hora e pauta.');
      return;
    }

    this.reunioes.atualizar(reuniaoId, { dataHora, pauta, observacoes }).subscribe({
      next: () => {
        this.cancelarEdicaoReuniao();
        this.recarregarReunioes();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  excluirReuniao(r: Reuniao) {
    if (!this.canSchedule()) return;
    if (!confirm(`Excluir reuniao "${r.pauta}"?`)) return;

    this.reunioes.excluir(r.id).subscribe({
      next: () => {
        if (this.editandoReuniaoId() === r.id) this.cancelarEdicaoReuniao();
        this.recarregarReunioes();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  adicionarMembros() {
    if (!this.isAdmin()) return;

    const ids = this.membrosSelecionados();
    if (!ids.length) return;

    this.grupos.adicionarMembros(this.grupoId, ids).subscribe({
      next: () => {
        this.membrosSelecionados.set([]);
        this.recarregarMembros();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  onMembrosChange(ev: Event) {
    const select = ev.target as HTMLSelectElement | null;
    if (!select) return;

    const ids = Array.from(select.selectedOptions).map((o) => Number(o.value));
    this.membrosSelecionados.set(ids);
  }

  removerMembro(membro: GrupoMembroDTO) {
    if (!this.isAdmin()) return;
    if (!confirm(`Remover "${membro.nome}" deste grupo?`)) return;

    this.removendoMembroId.set(membro.id);
    this.grupos.removerMembro(this.grupoId, membro.id).subscribe({
      next: () => {
        this.removendoMembroId.set(null);
        this.recarregarMembros();
      },
      error: (e) => {
        this.removendoMembroId.set(null);
        this.error.set(`${e.status} ${e.statusText}`);
      },
    });
  }

  atualizarPainel() {
    this.recarregar();
    this.recarregarReunioes();
    this.recarregarMembros();
  }

  private toDateTimeLocalValue(dataHora: string): string {
    return dataHora.length >= 16 ? dataHora.slice(0, 16) : dataHora;
  }
}
