import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';

export type DocumentoVersao = {
  id: number;
  titulo: string;
  versao: number;
  enviadoPor: string;
  createdAt: string;
  tamanho: number;
};

@Injectable({ providedIn: 'root' })
export class DocumentosService {
  private http = inject(HttpClient);

  lista(grupoId: number) {
    return this.http.get<DocumentoVersao[]>(`/api/grupos/${grupoId}/documentos`);
  }

  upload(grupoId: number, titulo: string, file: File) {
    const fd = new FormData();
    fd.append('titulo', titulo);
    fd.append('file', file);
    return this.http.post<DocumentoVersao>(`/api/grupos/${grupoId}/documentos`, fd);
  }
}
