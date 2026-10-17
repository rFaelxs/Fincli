import { Routes } from '@angular/router';
import { autenticadoGuard, publicoGuard, raizGuard } from './core/auth/guardas';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  // A raiz não tem tela: o guarda decide entre a Gestão e o login.
  { path: '', pathMatch: 'full', canActivate: [raizGuard], children: [] },
  { path: 'dashboard', redirectTo: '/gestao' },
  {
    path: 'login',
    canActivate: [publicoGuard],
    title: 'Entrar · FinCLI',
    loadComponent: () => import('./features/auth/pages/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'cadastro',
    canActivate: [publicoGuard],
    title: 'Criar conta · FinCLI',
    loadComponent: () =>
      import('./features/auth/pages/cadastro.page').then((m) => m.CadastroPage),
  },
  {
    path: '',
    component: ShellComponent,
    canActivate: [autenticadoGuard],
    children: [
      {
        path: 'gestao',
        title: 'Gestão · FinCLI',
        loadComponent: () => import('./features/gestao/pages/gestao.page').then((m) => m.GestaoPage),
      },
      {
        path: 'relatorio',
        title: 'Relatório · FinCLI',
        loadComponent: () =>
          import('./features/relatorio/pages/relatorio.page').then((m) => m.RelatorioPage),
      },
      { path: 'transacoes', loadChildren: () => import('./features/transacoes/transacoes.routes') },
      { path: 'reservas', loadChildren: () => import('./features/reservas/reservas.routes') },
      {
        path: 'extrato',
        title: 'Extrato · FinCLI',
        loadComponent: () => import('./features/extrato/pages/extrato.page').then((m) => m.ExtratoPage),
      },
    ],
  },
  { path: '**', redirectTo: '' },
];
