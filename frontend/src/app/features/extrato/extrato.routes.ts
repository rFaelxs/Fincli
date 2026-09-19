import { Routes } from '@angular/router';

export default [
  {
    path: '',
    title: 'Extrato · FinCLI',
    loadComponent: () => import('./pages/extrato.page').then((m) => m.ExtratoPage),
  },
] satisfies Routes;
