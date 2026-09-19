import { Component, inject } from '@angular/core';
import { Router, RouterLink, RouterLinkActive } from '@angular/router';
import { SessaoService } from '../../../core/auth/sessao.service';
import { CardGuardadoComponent } from './card-guardado.component';
import { RodapeUsuarioComponent } from './rodape-usuario.component';

/**
 * O design do redesign tem só "Gestão" e "Relatório". Transações, Reservas e Extrato continuam na
 * lista porque continuam existindo: tirá-las deixaria telas inalcançáveis.
 */
const ITENS = [
  { rota: '/gestao', rotulo: 'Gestão' },
  { rota: '/relatorio', rotulo: 'Relatório' },
  { rota: '/transacoes', rotulo: 'Transações' },
  { rota: '/reservas', rotulo: 'Reservas' },
  { rota: '/extrato', rotulo: 'Extrato' },
];

@Component({
  selector: 'app-barra-lateral',
  imports: [RouterLink, RouterLinkActive, CardGuardadoComponent, RodapeUsuarioComponent],
  template: `
    <a class="marca" routerLink="/gestao" aria-label="FinCLI, ir para Gestão">
      <img src="/assets/img/fincli-logo.png" alt="FinCLI" />
    </a>

    <nav aria-label="Seções">
      @for (item of itens; track item.rota) {
        <a [routerLink]="item.rota" routerLinkActive="" ariaCurrentWhenActive="page">
          <span class="ponto" aria-hidden="true"></span>{{ item.rotulo }}
        </a>
      }
    </nav>

    <app-card-guardado />

    <app-rodape-usuario [nome]="usuario()?.nome ?? ''" (sair)="sair()" />
  `,
  host: { style: 'display: contents' },
})
export class BarraLateralComponent {
  private readonly sessao = inject(SessaoService);
  private readonly router = inject(Router);

  protected readonly itens = ITENS;
  protected readonly usuario = this.sessao.usuario;

  protected async sair(): Promise<void> {
    await this.sessao.sair();
    await this.router.navigateByUrl('/login', { replaceUrl: true });
  }
}
