import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GrupoResumoDTO } from '../model/grupo-resumo.dto';

export type GrupoCreateRequest = {
  titulo: string;
  orientadorId: number;
  coorientadorId?: number | null;
};

export type AddMembrosRequest = {
  alunosIds: number[];
};

@Injectable({ providedIn: 'root' })
export class GrupoService {
  private http = inject(HttpClient);

  listarMeus(): Observable<GrupoResumoDTO[]> {
    return this.http.get<GrupoResumoDTO[]>('/api/grupos/me');
  }

  criar(req: GrupoCreateRequest): Observable<GrupoResumoDTO> {
    return this.http.post<GrupoResumoDTO>('/api/grupos', req);
  }

  adicionarMembros(id: number, alunosIds: number[]): Observable<void> {
    const body: AddMembrosRequest = { alunosIds };
    return this.http.post<void>(`/api/grupos/${id}/membros`, body);
  }
}
