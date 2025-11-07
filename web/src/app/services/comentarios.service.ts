import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type Comentario = {
  id: number;
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
}

