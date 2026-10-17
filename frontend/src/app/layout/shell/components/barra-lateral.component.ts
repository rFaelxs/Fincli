import { Component, computed, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessaoService } from '../../../core/auth/sessao.service';
import { CardGuardadoComponent } from './card-guardado.component';
import { RodapeUsuarioComponent } from './rodape-usuario.component';

/** As telas da navegação, nesta ordem (spec 01, item 4). */
export const ITENS_NAVEGACAO = [
  { rota: '/gestao', rotulo: 'Gestão' },
  { rota: '/relatorio', rotulo: 'Relatório' },
  { rota: '/transacoes', rotulo: 'Transações' },
  { rota: '/reservas', rotulo: 'Reservas' },
  { rota: '/extrato', rotulo: 'Extrato' },
] as const;

@Component({
  selector: 'app-barra-lateral',
  imports: [RouterLink, RouterLinkActive, CardGuardadoComponent, RodapeUsuarioComponent],
  template: `<aside class="side">
    <a class="marca" routerLink="/gestao" aria-label="FinCLI, ir para Gestão">
      <img src="assets/img/fincli-logo.png" alt="" width="168" height="64">
    </a>
    <nav aria-label="Principal">
      @for (item of itens; track item.rota) {
        <a [routerLink]="item.rota" routerLinkActive="" ariaCurrentWhenActive="page">
          <span class="ponto" aria-hidden="true"></span>{{ item.rotulo }}
        </a>
      }
    </nav>
    <app-card-guardado />
    <app-rodape-usuario [nome]="nome()" (sair)="sair()" />
  </aside>`,
  host: { style: 'display: contents' },
})
export class BarraLateralComponent {
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly itens = ITENS_NAVEGACAO;
  protected readonly nome = computed(() => this.sessao.usuario()?.nome ?? '');

  /** Sai e vai para o login mesmo que o servidor falhe (spec 01, item 12). */
  protected async sair(): Promise<void> {
    await this.sessao.sair();
    await this.router.navigateByUrl('/login');
  }
}
