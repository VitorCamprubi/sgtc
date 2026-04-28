import { Component, OnInit, computed, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, RouterLink } from '@angular/router';
import { FontAwesomeModule } from '@fortawesome/angular-fontawesome';
import { faComments, faDownload, faTrashCan } from '@fortawesome/free-solid-svg-icons';
import { Comentario, ComentariosService } from '../../services/comentarios.service';
import { DocumentoVersao, DocumentosService } from '../../services/documentos.service';
import { GrupoMembroDTO, GrupoService } from '../../services/grupo.service';
import {
  Reuniao,
  ReuniaoDesempenhoGrupo,
  ReuniaoStatus,
  ReunioesService,
} from '../../services/reunioes.service';
import { GrupoResumoDTO } from '../../model/grupo-resumo.dto';
import { Usuario, UsuariosService } from '../../services/usuarios.service';

@Component({
  selector: 'app-grupo-detalhe',
  standalone: true,
  templateUrl: './grupo-detalhe.component.html',
  styleUrls: ['./grupo-detalhe.component.scss'],
  imports: [CommonModule, FormsModule, FontAwesomeModule, RouterLink],
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
  readonly maxAtividadesRealizadas = 340;

  grupoId = 0;
  grupoTitulo = signal<string>('');
  grupoStatus = signal<GrupoResumoDTO['status'] | null>(null);
  abaAtiva = signal<'documentos' | 'reunioes' | 'membros'>('documentos');
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
  concluindoReuniaoId = signal<number | null>(null);
  execucaoDataAtividades = '';
  execucaoAtividadesRealizadas = '';
  execucaoDesempenho: ReuniaoDesempenhoGrupo | '' = '';
  execucaoProfessorDisciplina = '';
  gerandoPdf = signal<boolean>(false);

  isAdmin = signal<boolean>(false);
  alunos = signal<Usuario[]>([]);
  membrosGrupo = signal<GrupoMembroDTO[] | null>(null);
  membrosSelecionados = signal<number[]>([]);
  removendoMembroId = signal<number | null>(null);
  buscaAluno = signal<string>('');

  alunosDisponiveis = computed(() => {
    const idsMembros = new Set((this.membrosGrupo() ?? []).map((m) => m.id));
    return this.alunos().filter((a) => !idsMembros.has(a.id));
  });

  alunosDisponiveisFiltrados = computed(() => {
    const termo = this.normalizarBuscaAluno(this.buscaAluno());
    if (!termo) return this.alunosDisponiveis();

    return this.alunosDisponiveis().filter((a) =>
      this.normalizarBuscaAluno(`${a.nome} ${a.email}`).includes(termo)
    );
  });

  grupoArquivado = computed(() => {
    const status = this.grupoStatus();
    return status !== null && status !== 'EM_CURSO';
  });

  canComment = computed(() => {
    const role = this.currentUser()?.role;
    return !this.grupoArquivado() && (role === 'ADMIN' || role === 'PROFESSOR');
  });

  canUploadDocumento = computed(() => this.currentUser() !== null && !this.grupoArquivado());

  canCreateReuniao = computed(() => this.currentUser() !== null && !this.grupoArquivado());

  canManageReunioes = computed(() => {
    const role = this.currentUser()?.role;
    return !this.grupoArquivado() && (role === 'ADMIN' || role === 'PROFESSOR');
  });

  canManageMembros = computed(() => this.isAdmin() && !this.grupoArquivado());

  reunioesAbertas = computed(() =>
    (this.listaReunioes() ?? []).filter((r) => this.isReuniaoAberta(r))
  );

  historicoReunioes = computed(() =>
    (this.listaReunioes() ?? []).filter((r) => !this.isReuniaoAberta(r))
  );

  ngOnInit() {
    this.route.paramMap.subscribe((params) => {
      const groupId = Number(params.get('id'));
      if (!Number.isFinite(groupId) || groupId <= 0) {
        this.error.set('Grupo invalido.');
        return;
      }

      this.abaAtiva.set(this.normalizarSecao(params.get('secao')));

      const grupoMudou = this.grupoId !== groupId;
      this.grupoId = groupId;
      this.carregarResumoGrupo();

      if (grupoMudou) {
        this.recarregar();
        this.recarregarReunioes();
        this.recarregarMembros();
      }
    });

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

  private carregarResumoGrupo() {
    this.grupos.obter(this.grupoId).subscribe({
      next: (g: GrupoResumoDTO) => {
        this.grupoTitulo.set(g.titulo);
        this.grupoStatus.set(g.status);
      },
      error: (e) => {
        this.grupoTitulo.set('');
        this.grupoStatus.set(null);
        this.error.set(`${e.status} ${e.statusText}`);
      },
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
    if (!this.canUploadDocumento()) {
      this.error.set('Grupo arquivado. Nao e possivel enviar novos documentos.');
      return;
    }

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
    if (this.grupoArquivado()) return false;
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
    if (this.grupoArquivado()) return false;
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
    this.cancelarEdicaoReuniao();
    this.cancelarConclusaoReuniao();

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
    if (this.grupoArquivado()) {
      this.error.set('Grupo arquivado. Nao e possivel agendar reunioes.');
      return;
    }
    if (!this.canCreateReuniao()) return;

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
    if (!this.canManageReunioes() || !this.isReuniaoAberta(r)) return;
    this.editandoReuniaoId.set(r.id);
    this.editarDataHora = this.toDateTimeLocalValue(r.dataHora);
    this.editarPauta = r.pauta;
    this.editarObservacoes = r.observacoes ?? '';
    this.cancelarConclusaoReuniao();
  }

  cancelarEdicaoReuniao() {
    this.editandoReuniaoId.set(null);
    this.editarDataHora = '';
    this.editarPauta = '';
    this.editarObservacoes = '';
  }

  salvarEdicaoReuniao() {
    const reuniaoId = this.editandoReuniaoId();
    if (!this.canManageReunioes() || !reuniaoId) return;

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

  iniciarConclusaoReuniao(r: Reuniao) {
    if (!this.canManageReunioes() || !this.isReuniaoAberta(r)) return;
    this.concluindoReuniaoId.set(r.id);
    this.execucaoDataAtividades = this.hojeComoDataInput();
    this.execucaoAtividadesRealizadas = '';
    this.execucaoDesempenho = '';
    this.execucaoProfessorDisciplina = this.currentUser()?.nome ?? '';
    this.cancelarEdicaoReuniao();
  }

  cancelarConclusaoReuniao() {
    this.concluindoReuniaoId.set(null);
    this.execucaoDataAtividades = '';
    this.execucaoAtividadesRealizadas = '';
    this.execucaoDesempenho = '';
    this.execucaoProfessorDisciplina = '';
  }

  concluirReuniao(r: Reuniao) {
    if (!this.canManageReunioes() || !this.isReuniaoAberta(r)) return;

    const dataAtividadesRealizadas = this.execucaoDataAtividades;
    const atividadesRealizadas = this.execucaoAtividadesRealizadas.trim();
    const desempenhoGrupo = this.execucaoDesempenho;
    const professorDisciplina = this.execucaoProfessorDisciplina.trim();

    if (
      !dataAtividadesRealizadas ||
      !atividadesRealizadas ||
      !desempenhoGrupo ||
      !professorDisciplina
    ) {
      this.error.set('Preencha todos os campos da execucao da reuniao.');
      return;
    }
    if (atividadesRealizadas.length > this.maxAtividadesRealizadas) {
      this.error.set(
        `Atividades realizadas ultrapassam o limite de ${this.maxAtividadesRealizadas} caracteres.`
      );
      return;
    }

    this.reunioes
      .concluir(r.id, {
        dataAtividadesRealizadas,
        atividadesRealizadas,
        desempenhoGrupo,
        professorDisciplina,
      })
      .subscribe({
      next: () => {
        this.cancelarConclusaoReuniao();
        this.recarregarReunioes();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  cancelarReuniao(r: Reuniao) {
    if (!this.canManageReunioes() || !this.isReuniaoAberta(r)) return;
    if (!confirm(`Cancelar reuniao "${r.pauta}"?`)) return;

    this.reunioes.cancelar(r.id).subscribe({
      next: () => {
        if (this.editandoReuniaoId() === r.id) this.cancelarEdicaoReuniao();
        if (this.concluindoReuniaoId() === r.id) this.cancelarConclusaoReuniao();
        this.recarregarReunioes();
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
    });
  }

  adicionarMembros() {
    if (!this.canManageMembros()) return;

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

    const idsVisiveis = Array.from(select.options)
      .map((o) => Number(o.value))
      .filter((id) => Number.isInteger(id) && id > 0);
    const idsSelecionadosVisiveis = Array.from(select.selectedOptions)
      .map((o) => Number(o.value))
      .filter((id) => Number.isInteger(id) && id > 0);
    const idsSelecionadosOcultos = this.membrosSelecionados().filter(
      (id) => !idsVisiveis.includes(id)
    );

    this.membrosSelecionados.set([
      ...new Set([...idsSelecionadosOcultos, ...idsSelecionadosVisiveis]),
    ]);
  }

  removerMembro(membro: GrupoMembroDTO) {
    if (!this.canManageMembros()) return;
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

  gerarPdfReunioes() {
    if (this.gerandoPdf()) return;

    this.gerandoPdf.set(true);
    this.reunioes.gerarPdfDoGrupo(this.grupoId).subscribe({
      next: (response) => {
        const blob = response.body;
        if (!blob) {
          this.error.set('Nao foi possivel gerar o PDF das reunioes.');
          return;
        }

        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        const fallbackNome = `reunioes-${this.normalizarNomeArquivo(
          this.grupoTitulo() || `grupo-${this.grupoId}`
        )}.pdf`;
        a.download = this.filenameFromContentDisposition(
          response.headers.get('content-disposition'),
          fallbackNome
        );
        a.click();
        window.URL.revokeObjectURL(url);
      },
      error: (e) => this.error.set(`${e.status} ${e.statusText}`),
      complete: () => this.gerandoPdf.set(false),
    });
  }

  isReuniaoAberta(r: Reuniao): boolean {
    return r.status === 'AGUARDANDO_DATA_REUNIAO';
  }

  statusReuniaoLabel(status: ReuniaoStatus): string {
    switch (status) {
      case 'AGUARDANDO_DATA_REUNIAO':
        return 'Aguardando data da reuniao';
      case 'CONCLUIDA':
        return 'Executada';
      case 'CANCELADA':
        return 'Cancelada';
      case 'NAO_REALIZADA':
        return 'Nao realizada';
      default:
        return status;
    }
  }

  statusReuniaoClass(status: ReuniaoStatus): string {
    switch (status) {
      case 'AGUARDANDO_DATA_REUNIAO':
        return 'badge-status aberto';
      case 'CONCLUIDA':
        return 'badge-status concluida';
      case 'CANCELADA':
        return 'badge-status cancelada';
      case 'NAO_REALIZADA':
        return 'badge-status nao-realizada';
      default:
        return 'badge-status';
    }
  }

  desempenhoLabel(desempenho: ReuniaoDesempenhoGrupo | null): string {
    switch (desempenho) {
      case 'RUIM':
        return 'Ruim';
      case 'REGULAR':
        return 'Regular';
      case 'BOM':
        return 'Bom';
      case 'OTIMO':
        return 'Otimo';
      default:
        return '-';
    }
  }

  private toDateTimeLocalValue(dataHora: string): string {
    return dataHora.length >= 16 ? dataHora.slice(0, 16) : dataHora;
  }

  private hojeComoDataInput(): string {
    const hoje = new Date();
    hoje.setMinutes(hoje.getMinutes() - hoje.getTimezoneOffset());
    return hoje.toISOString().slice(0, 10);
  }

  private filenameFromContentDisposition(
    contentDisposition: string | null,
    fallback: string
  ): string {
    if (!contentDisposition) return fallback;
    const match = /filename=\"?([^\";]+)\"?/i.exec(contentDisposition);
    return match?.[1] ?? fallback;
  }

  private normalizarNomeArquivo(valor: string): string {
    return valor
      .trim()
      .toLowerCase()
      .replace(/[^a-z0-9]+/g, '-')
      .replace(/(^-|-$)/g, '');
  }

  private normalizarSecao(
    secao: string | null
  ): 'documentos' | 'reunioes' | 'membros' {
    if (secao === 'reunioes') return 'reunioes';
    if (secao === 'membros') return 'membros';
    return 'documentos';
  }

  private normalizarBuscaAluno(valor: string): string {
    return (valor ?? '')
      .normalize('NFD')
      .replace(/[\u0300-\u036f]/g, '')
      .toLowerCase()
      .trim();
  }
}
