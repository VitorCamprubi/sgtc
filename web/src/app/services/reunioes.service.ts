import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type ReuniaoStatus =
  | 'AGUARDANDO_DATA_REUNIAO'
  | 'CONCLUIDA'
  | 'CANCELADA'
  | 'NAO_REALIZADA';

export type ReuniaoDesempenhoGrupo = 'RUIM' | 'REGULAR' | 'BOM' | 'OTIMO';

export type Reuniao = {
  id: number;
  dataHora: string;
  pauta: string;
  observacoes: string | null;
  status: ReuniaoStatus;
  relatorio: string | null;
  encerradaEm: string | null;
  numeroEncontro: number | null;
  dataAtividadesRealizadas: string | null;
  atividadesRealizadas: string | null;
  desempenhoGrupo: ReuniaoDesempenhoGrupo | null;
  professorDisciplina: string | null;
  orientadorAssinatura: string | null;
  coorientadorAssinatura: string | null;
  criadoPor: string;
};

@Injectable({ providedIn: 'root' })
export class ReunioesService {
  private http = inject(HttpClient);

  listar(grupoId: number) {
    return this.http.get<Reuniao[]>(`/api/grupos/${grupoId}/reunioes`);
  }

  agendar(
    grupoId: number,
    req: { dataHora: string; pauta: string; observacoes?: string | null }
  ) {
    return this.http.post<Reuniao>(`/api/grupos/${grupoId}/reunioes`, req);
  }

  atualizar(
    reuniaoId: number,
    req: { dataHora: string; pauta: string; observacoes?: string | null }
  ) {
    return this.http.put<Reuniao>(`/api/reunioes/${reuniaoId}`, req);
  }

  concluir(
    reuniaoId: number,
    req: {
      numeroEncontro: number;
      dataAtividadesRealizadas: string;
      atividadesRealizadas: string;
      desempenhoGrupo: ReuniaoDesempenhoGrupo;
      professorDisciplina: string;
      orientadorAssinatura: string;
      coorientadorAssinatura: string;
    }
  ) {
    return this.http.post<Reuniao>(`/api/reunioes/${reuniaoId}/concluir`, req);
  }

  cancelar(reuniaoId: number) {
    return this.http.post<Reuniao>(`/api/reunioes/${reuniaoId}/cancelar`, {});
  }

  gerarPdfDoGrupo(grupoId: number) {
    return this.http.get(`/api/grupos/${grupoId}/reunioes/pdf`, {
      observe: 'response',
      responseType: 'blob',
    });
  }
}

