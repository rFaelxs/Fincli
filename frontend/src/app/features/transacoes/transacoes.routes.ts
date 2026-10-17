import { Routes } from '@angular/router';

/** Rotas de Transações, carregadas sob demanda a partir de `/transacoes`. */
export default [
  {
    path: '',
    title: 'Transações · FinCLI',
    loadComponent: () =>
      import('./pages/lista-transacoes.page').then((m) => m.ListaTransacoesPage),
  },
  {
    path: 'nova',
    title: 'Nova transação · FinCLI',
    loadComponent: () => import('./pages/nova-transacao.page').then((m) => m.NovaTransacaoPage),
  },
  // Edição sem id não tem o que editar (spec 06, item 31).
  { path: 'editar', redirectTo: '/transacoes' },
  {
    path: ':id/editar',
    title: 'Editar transação · FinCLI',
    loadComponent: () =>
      import('./pages/editar-transacao.page').then((m) => m.EditarTransacaoPage),
  },
] satisfies Routes;
