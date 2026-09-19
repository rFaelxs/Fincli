import { Routes } from '@angular/router';

export default [
  {
    path: '',
    title: 'Relatório · FinCLI',
    loadComponent: () => import('./pages/relatorio.page').then((m) => m.RelatorioPage),
  },
] satisfies Routes;
