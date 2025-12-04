import { Routes } from '@angular/router';
import { authGuard } from './core/auth.guard';
import { adminGuard } from './core/admin.guard';
import { LoginComponent } from './login/login.component';
import { GruposComponent } from './features/grupos/grupos.component';
import { GrupoDetalheComponent } from './features/grupos/grupo-detalhe.component';
import { AdminUsuariosComponent } from './features/admin/admin-usuarios.component';

export const routes: Routes = [
  {
    path: 'login',
    component: LoginComponent,
  },
  {
    path: 'admin/usuarios',
    canActivate: [authGuard, adminGuard],
    component: AdminUsuariosComponent,
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
