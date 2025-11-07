import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';

export const routes: Routes = [
  { path: 'login', loadComponent: () => import('./login/login.component').then(m => m.LoginComponent) },
  {
    path: 'grupos',
    canActivate: [authGuard],
    loadComponent: () => import('./features/grupos/grupos.component').then(m => m.GruposComponent),
  },
  {
    path: 'grupos/:id',
    canActivate: [authGuard],
    loadComponent: () => import('./features/grupos/grupo-detalhe.component').then(m => m.GrupoDetalheComponent),
  },
  { path: '', pathMatch: 'full', redirectTo: 'grupos' },
  { path: '**', redirectTo: 'grupos' },
];
