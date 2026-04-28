import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type Comentario = {
  id: number;
  autorId: number;
  autor: string;
  texto: string;
  createdAt: string;
};

@Injectable({ providedIn: 'root' })
export class ComentariosService {
  private http = inject(HttpClient);

  listar(docId: number) {
    return this.http.get<Comentario[]>(`/api/documentos/${docId}/comentarios`);
  }

  comentar(docId: number, texto: string) {
    return this.http.post<Comentario>(`/api/documentos/${docId}/comentarios`, {
      texto,
    });
  }

  atualizar(comentarioId: number, texto: string) {
    return this.http.put<Comentario>(`/api/comentarios/${comentarioId}`, { texto });
  }

  excluir(comentarioId: number) {
    return this.http.delete<void>(`/api/comentarios/${comentarioId}`);
  }
}

