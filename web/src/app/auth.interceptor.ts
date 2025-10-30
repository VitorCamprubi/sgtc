import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { AuthService } from './auth.service';

export const authInterceptor: HttpInterceptorFn = (req, next) => {
  if (req.url.startsWith('/api')) {
    const auth = inject(AuthService).getAuthHeader();
    if (auth) req = req.clone({ setHeaders: { Authorization: auth } });
  }
  return next(req);
};
