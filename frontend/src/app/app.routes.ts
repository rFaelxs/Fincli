import { Routes } from '@angular/router';
import { autenticadoGuard, publicoGuard, raizGuard } from './core/auth/guardas';
import { EmConstrucaoPage } from './layout/em-construcao.page';
import { ShellComponent } from './layout/shell/shell.component';

/** As telas ainda não construídas ficam provisórias, para a navegação do shell funcionar. */
function provisoria(path: string, titulo: string) {
  return { path, component: EmConstrucaoPage, data: { titulo }, title: `${titulo} · FinCLI` };
}

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
      provisoria('gestao', 'Gestão'),
      provisoria('relatorio', 'Relatório'),
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
