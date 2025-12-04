import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { LoginComponent } from './login/login.component';
import { GruposComponent } from './features/grupos/grupos.component';
import { GrupoDetalheComponent } from './features/grupos/grupo-detalhe.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'grupos',
    canActivate: [authGuard],
    component: GruposComponent,
  },
  {
    path: 'grupos/:id',
    canActivate: [authGuard],
    component: GrupoDetalheComponent,
  },
  { path: '', pathMatch: 'full', redirectTo: 'grupos' },
  { path: '**', redirectTo: 'grupos' },
];
