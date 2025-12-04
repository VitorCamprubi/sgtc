import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';
import { UsuariosService } from '../services/usuarios.service';
import { map } from 'rxjs/operators';

export const adminGuard: CanActivateFn = () => {
  const router = inject(Router);
  const usuarios = inject(UsuariosService);

  return usuarios.getUsuarioAtualViaDebug().pipe(
    map((u) => {
      if (u?.role === 'ADMIN') return true;
      return router.createUrlTree(['/grupos']);
    })
  );
};
