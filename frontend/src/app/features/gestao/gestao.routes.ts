import { Routes } from '@angular/router';

export default [
  {
    path: '',
    title: 'Gestão · FinCLI',
    loadComponent: () => import('./pages/gestao.page').then((m) => m.GestaoPage),
  },
] satisfies Routes;
