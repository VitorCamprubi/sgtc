import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, of } from 'rxjs';
import { catchError } from 'rxjs/operators';

export type Usuario = {
  id: number;
  nome: string;
  email: string;
  role: 'ADMIN' | 'PROFESSOR' | 'ALUNO';
  ra?: string | null;
};

export type UsuarioAdminPayload = {
  nome: string;
  email: string;
  senha?: string;
  role: 'ALUNO' | 'PROFESSOR';
  ra?: string | null;
};

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);

  getUsuarioAtual(): Observable<Usuario | null> {
    const hasAuth = !!(
      sessionStorage.getItem('sgtc_auth') || localStorage.getItem('sgtc_auth')
    );
    if (!hasAuth) return of(null);

    return this.http.get<Usuario>('/api/auth/me').pipe(catchError(() => of(null)));
  }

  listarAdmin(role?: 'ALUNO' | 'PROFESSOR') {
    const params = role ? { role } : undefined;
    return this.http.get<Usuario[]>('/api/admin/usuarios', { params });
  }

  criarAdmin(payload: UsuarioAdminPayload) {
    return this.http.post<Usuario>('/api/admin/usuarios', payload);
  }

  atualizarAdmin(id: number, payload: UsuarioAdminPayload) {
    return this.http.put<Usuario>(`/api/admin/usuarios/${id}`, payload);
  }

  excluirAdmin(id: number) {
    return this.http.delete<void>(`/api/admin/usuarios/${id}`);
  }
}
