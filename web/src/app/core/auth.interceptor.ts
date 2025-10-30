import { inject } from '@angular/core';
import {
  HttpErrorResponse,
  HttpHandlerFn,
  HttpInterceptorFn,
  HttpRequest,
} from '@angular/common/http';
import { Router } from '@angular/router';
import { catchError } from 'rxjs/operators';
import { throwError } from 'rxjs';

export const authInterceptor: HttpInterceptorFn = (
  req: HttpRequest<unknown>,
  next: HttpHandlerFn
) => {
  const router = inject(Router);

  // usa o header salvo no login (ex.: "Basic abc123..." ou "Bearer xyz")
  const auth =
    sessionStorage.getItem('sgtc_auth') || localStorage.getItem('sgtc_auth');

  let authReq = req;
  if (auth && req.url.startsWith('/api/')) {
    authReq = req.clone({ setHeaders: { Authorization: auth } });
  }

  return next(authReq).pipe(
    catchError((err: HttpErrorResponse) => {
      if (err.status === 401) {
        // limpa e manda pro login
        sessionStorage.removeItem('sgtc_auth');
        localStorage.removeItem('sgtc_auth');
        router.navigateByUrl('/login');
      }
      return throwError(() => err);
    })
  );
};
