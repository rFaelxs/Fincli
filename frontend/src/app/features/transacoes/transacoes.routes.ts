import { Routes } from '@angular/router';

export default [
  {
    path: '',
    title: 'Transações · FinCLI',
    loadComponent: () => import('./pages/lista-transacoes.page').then((m) => m.ListaTransacoesPage),
  },
  {
    path: 'nova',
    title: 'Nova transação · FinCLI',
    loadComponent: () => import('./pages/nova-transacao.page').then((m) => m.NovaTransacaoPage),
  },
  {
    path: ':id/editar',
    title: 'Editar transação · FinCLI',
    loadComponent: () => import('./pages/editar-transacao.page').then((m) => m.EditarTransacaoPage),
  },
] satisfies Routes;
