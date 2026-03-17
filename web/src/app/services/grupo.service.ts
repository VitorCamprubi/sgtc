import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GrupoResumoDTO } from '../model/grupo-resumo.dto';

export type GrupoCreateRequest = {
  titulo: string;
  materia: 'TG' | 'PTG';
  orientadorId: number;
  coorientadorId?: number | null;
};

export type AddMembrosRequest = {
  alunosIds: number[];
};

export type UpdateMembrosRequest = {
  alunosIds: number[];
};

export type GrupoMembroDTO = {
  id: number;
  nome: string;
  email: string;
  role: 'ADMIN' | 'PROFESSOR' | 'ALUNO';
  ra?: string | null;
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

  atualizar(id: number, req: GrupoCreateRequest): Observable<GrupoResumoDTO> {
    return this.http.put<GrupoResumoDTO>(`/api/grupos/${id}`, req);
  }

  adicionarMembros(id: number, alunosIds: number[]): Observable<void> {
    const body: AddMembrosRequest = { alunosIds };
    return this.http.post<void>(`/api/grupos/${id}/membros`, body);
  }

  atualizarMembros(id: number, alunosIds: number[]): Observable<void> {
    const body: UpdateMembrosRequest = { alunosIds };
    return this.http.put<void>(`/api/grupos/${id}/membros`, body);
  }

  listarMembros(id: number): Observable<GrupoMembroDTO[]> {
    return this.http.get<GrupoMembroDTO[]>(`/api/grupos/${id}/membros`);
  }

  removerMembro(id: number, alunoId: number): Observable<void> {
    return this.http.delete<void>(`/api/grupos/${id}/membros/${alunoId}`);
  }

  deletar(id: number): Observable<void> {
    return this.http.delete<void>(`/api/grupos/${id}`);
  }
}
