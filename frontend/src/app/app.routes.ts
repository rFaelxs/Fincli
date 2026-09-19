import { Routes } from '@angular/router';
import { autenticadoGuard, publicoGuard } from './core/auth/guardas';
import { ShellComponent } from './layout/shell/shell.component';

export const routes: Routes = [
  // A raiz decide entre Gestão e login: sem sessão, a guarda da Gestão manda para o login.
  { path: '', pathMatch: 'full', redirectTo: 'gestao' },
  // A tela Gestão substituiu o Dashboard; o redirecionamento não quebra favoritos antigos.
  { path: 'dashboard', redirectTo: 'gestao' },

  {
    path: 'login',
    title: 'Entrar · FinCLI',
    canActivate: [publicoGuard],
    loadComponent: () => import('./features/auth/pages/login.page').then((m) => m.LoginPage),
  },
  {
    path: 'cadastro',
    title: 'Criar conta · FinCLI',
    canActivate: [publicoGuard],
    loadComponent: () => import('./features/auth/pages/cadastro.page').then((m) => m.CadastroPage),
  },

  {
    path: '',
    component: ShellComponent,
    canActivate: [autenticadoGuard],
    children: [
      { path: 'gestao', loadChildren: () => import('./features/gestao/gestao.routes') },
      { path: 'relatorio', loadChildren: () => import('./features/relatorio/relatorio.routes') },
      {
        path: 'transacoes',
        loadChildren: () => import('./features/transacoes/transacoes.routes'),
      },
      { path: 'reservas', loadChildren: () => import('./features/reservas/reservas.routes') },
      { path: 'extrato', loadChildren: () => import('./features/extrato/extrato.routes') },
    ],
  },

  { path: '**', redirectTo: 'gestao' },
];
