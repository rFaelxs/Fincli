import { Routes } from '@angular/router';

const movimentar = () => import('./pages/movimentar.page').then((m) => m.MovimentarPage);

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
  {
    path: ':id/alocar',
    title: 'Movimentar reserva · FinCLI',
    data: { acao: 'alocar' },
    loadComponent: movimentar,
  },
  {
    path: ':id/sacar',
    title: 'Movimentar reserva · FinCLI',
    data: { acao: 'sacar' },
    loadComponent: movimentar,
  },
] satisfies Routes;
