import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type Reuniao = {
  id: number;
  dataHora: string;
  pauta: string;
  observacoes: string | null;
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
    req: { dataHora: string; pauta: string; observacoes: string }
  ) {
    return this.http.post<Reuniao>(`/api/grupos/${grupoId}/reunioes`, req);
  }
}

