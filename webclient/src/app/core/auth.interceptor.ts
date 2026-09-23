import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { catchError, throwError } from 'rxjs';
import { AuthService } from './auth.service';
import { MessagesService } from './messages.service';

/** Cookies and Angular's built-in XSRF interceptor authenticate same-origin requests. */
export const authInterceptor: HttpInterceptorFn = (req, next) => {
  const auth = inject(AuthService);
  const router = inject(Router);
  const messages = inject(MessagesService);
  const isApi = /^\/(?:api|restful|graphql)(?:\/|$|\?)/.test(req.url);
  return next(req).pipe(
    catchError((error: unknown) => {
      if (isApi && error instanceof HttpErrorResponse) {
        if (error.status === 401 && req.url !== '/api/auth/login') {
          auth.clearIdentity();
          void router.navigate(['/login']);
        } else if (error.status === 403) {
          messages.error(error.error?.error === 'csrf'
            ? 'The security token is no longer valid. Reload the page and try again.'
            : 'You do not have permission to perform this action.');
        }
      }
      return throwError(() => error);
    }),
  );
};
