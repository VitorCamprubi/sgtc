import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { GrupoResumoDTO } from '../model/grupo-resumo.dto';

@Injectable({ providedIn: 'root' })
export class GrupoService {
  private http = inject(HttpClient);

  listar(): Observable<GrupoResumoDTO[]> {
    return this.http.get<GrupoResumoDTO[]>('/api/grupos');
  }
}
