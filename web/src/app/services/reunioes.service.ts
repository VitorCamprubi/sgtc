import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type ReuniaoStatus =
  | 'AGUARDANDO_DATA_REUNIAO'
  | 'CONCLUIDA'
  | 'CANCELADA'
  | 'NAO_REALIZADA';

export type Reuniao = {
  id: number;
  dataHora: string;
  pauta: string;
  observacoes: string | null;
  status: ReuniaoStatus;
  relatorio: string | null;
  encerradaEm: string | null;
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

  concluir(reuniaoId: number, req: { relatorio: string }) {
    return this.http.post<Reuniao>(`/api/reunioes/${reuniaoId}/concluir`, req);
  }

  cancelar(reuniaoId: number) {
    return this.http.post<Reuniao>(`/api/reunioes/${reuniaoId}/cancelar`, {});
  }
}

