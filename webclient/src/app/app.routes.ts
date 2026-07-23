import { Routes } from '@angular/router';

import { authGuard } from './core/auth.guard';
import { BlogDetail } from './pages/blog-detail/blog-detail';
import { Home } from './pages/home/home';
import { Login } from './pages/login/login';
import { PostDetail } from './pages/post-detail/post-detail';

export const routes: Routes = [
  { path: 'login', component: Login },
  { path: '', component: Home, canActivate: [authGuard] },
  { path: 'blogs/:id', component: BlogDetail, canActivate: [authGuard] },
  { path: 'posts/:id', component: PostDetail, canActivate: [authGuard] },
  { path: '**', redirectTo: '' },
];
