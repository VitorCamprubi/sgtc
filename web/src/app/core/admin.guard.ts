import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { map } from 'rxjs/operators';
import { UsuariosService } from '../services/usuarios.service';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const usuarios = inject(UsuariosService);

  return usuarios.getUsuarioAtual().pipe(
    map((u) => {
      if (u?.role === 'ADMIN') return true;
      return router.createUrlTree(['/grupos']);
    })
  );
};
