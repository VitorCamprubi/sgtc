import { inject, Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map } from 'rxjs';

export type Usuario = {
  id: number;
  nome: string;
  email: string;
  role: 'ADMIN' | 'ORIENTADOR' | 'COORIENTADOR' | 'ALUNO';
  ra?: string | null;
};

@Injectable({ providedIn: 'root' })
export class UsuariosService {
  private http = inject(HttpClient);

  listarPublic(): Observable<Usuario[]> {
    return this.http.get<Usuario[]>('/public/debug/users');
  }

  getUsuarioAtualViaDebug(): Observable<Usuario | null> {
    const raw = sessionStorage.getItem('sgtc_user') || localStorage.getItem('sgtc_user');
    let email: string | null = null;
    try {
      email = raw ? (JSON.parse(raw).email as string) : null;
    } catch {
      email = null;
    }
    if (!email) return this.listarPublic().pipe(map(() => null));
    return this.listarPublic().pipe(
      map((list) => list.find((u) => u.email === email) ?? null)
    );
  }
}
