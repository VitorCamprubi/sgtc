import { CanActivateFn, Router } from '@angular/router';
import { inject } from '@angular/core';

export const authGuard: CanActivateFn = () => {
  const hasAuth = !!(
    sessionStorage.getItem('sgtc_auth') || localStorage.getItem('sgtc_auth')
  );
  if (!hasAuth) inject(Router).navigateByUrl('/login');
  return hasAuth;
};
