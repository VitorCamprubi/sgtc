import { Component, OnInit, inject, signal } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { HttpClient } from '@angular/common/http';

type GrupoResumo = {
  id: number;
  titulo: string;
  orientadorNome: string | null;
  coorientadorNome: string | null;
  totalMembros: number;
};

@Component({
  selector: 'app-grupos',
  standalone: true,
  templateUrl: './grupos.component.html',
  styleUrls: ['./grupos.component.scss'],
  imports: [CommonModule, RouterLink],
})
export class GruposComponent implements OnInit {
  private http = inject(HttpClient);
  private router = inject(Router);

  grupos = signal<GrupoResumo[] | null>(null);
  error = signal<string | null>(null);
  filtro = signal<'todos' | 'meus'>('todos');

  ngOnInit(): void {
    this.carregarTodos();
  }

  carregarTodos(): void {
    this.filtro.set('todos');
    this.buscar('/api/grupos');
  }

  carregarMeus(): void {
    this.filtro.set('meus');
    this.buscar('/api/grupos/me');
  }

  private buscar(url: string) {
    this.error.set(null);
    this.grupos.set(null);
    this.http.get<GrupoResumo[]>(url).subscribe({
      next: (data) => this.grupos.set(data),
      error: (err) =>
        this.error.set(`${err.status ?? ''} ${err.statusText ?? 'Erro'}`.trim()),
    });
  }

  logout(ev?: Event) {
    ev?.preventDefault();
    sessionStorage.removeItem('sgtc_auth');
    localStorage.removeItem('sgtc_auth');
    sessionStorage.removeItem('sgtc_user');
    localStorage.removeItem('sgtc_user');
    this.router.navigateByUrl('/login');
  }

  get userEmail(): string | null {
    const raw =
      sessionStorage.getItem('sgtc_user') || localStorage.getItem('sgtc_user');
    try {
      return raw ? JSON.parse(raw).email ?? null : null;
    } catch {
      return null;
    }
  }
}
