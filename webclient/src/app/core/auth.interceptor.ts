import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';

import { AuthService } from './auth.service';

/**
 * Attaches the stored Basic credentials to every /restful request and
 * bounces to the login page when the server answers 401.
 */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);

  let request = req;
  if (req.url.startsWith('/restful') && !req.headers.has('Authorization') && auth.credentials) {
    request = req.clone({
      setHeaders: { Authorization: `Basic ${auth.credentials}` },
    });
  }

  return next(request).pipe(
    catchError((error: unknown) => {
      if (
        error instanceof HttpErrorResponse &&
        error.status === 401 &&
        !req.headers.has('Authorization') // not the login probe itself
      ) {
        auth.logout();
        router.navigate(['/login']);
      }
      return throwError(() => error);
    }),
  );
};
