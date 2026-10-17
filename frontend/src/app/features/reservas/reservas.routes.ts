import { Routes } from '@angular/router';

const movimentar = () => import('./pages/movimentar.page').then((m) => m.MovimentarPage);

/** Rotas de Reservas, carregadas sob demanda a partir de `/reservas`. */
export default [
  {
    path: '',
    title: 'Reservas · FinCLI',
    loadComponent: () => import('./pages/lista-reservas.page').then((m) => m.ListaReservasPage),
  },
  {
    path: 'nova',
    title: 'Nova reserva · FinCLI',
    loadComponent: () => import('./pages/nova-reserva.page').then((m) => m.NovaReservaPage),
  },
  {
    path: ':id/meta',
    title: 'Editar meta · FinCLI',
    loadComponent: () => import('./pages/editar-meta.page').then((m) => m.EditarMetaPage),
  },
  // A tela de movimentar é uma só; o `data` diz qual operação ela faz.
  {
    path: ':id/alocar',
    title: 'Alocar · FinCLI',
    data: { operacao: 'alocar' },
    loadComponent: movimentar,
  },
  {
    path: ':id/sacar',
    title: 'Sacar · FinCLI',
    data: { operacao: 'sacar' },
    loadComponent: movimentar,
  },
  // Sem id não há reserva para mexer (spec 07, item 28).
  { path: 'meta', redirectTo: '/reservas' },
  { path: 'alocar', redirectTo: '/reservas' },
  { path: 'sacar', redirectTo: '/reservas' },
] satisfies Routes;
