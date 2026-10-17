import { Component, computed, input } from '@angular/core';
import { Relatorio } from '../../../core/api/modelos';
import { colunas, descricaoDaBarra, segmentos, totais } from '../relatorio';

/** Barra de fluxo e os cinco totais do mês (spec 05, itens 5 a 10). */
@Component({
  selector: 'app-barra-fluxo',
  template: `<section class="card fluxo-card" aria-label="Fluxo do mês">
    <div class="fluxo" role="img" [attr.aria-label]="descricao()" data-cy="relatorio-fluxo">
      @for (s of lista(); track s.classe) {
        <div class="faixa-seg" [class]="'faixa-seg ' + s.classe" [style.width.%]="s.largura"
          [attr.title]="s.titulo" aria-hidden="true" data-cy="relatorio-segmento">
          {{ s.rotuloVisivel ? s.nome : '' }}</div>
      } @empty {
        <div class="faixa-seg sobra" style="width: 100%" aria-hidden="true"
          data-cy="relatorio-fluxo-vazio">nada lançado neste mês</div>
      }
    </div>
    <div class="fluxo-totais">
      @for (t of linhasTotais(); track t.rotulo) {
        <div>
          <span class="rot">{{ t.rotulo }}</span>
          <span class="val" [class]="'val ' + t.classe" data-cy="relatorio-total">{{ t.valor }}</span>
        </div>
      }
    </div>
  </section>`,
})
export class BarraFluxoComponent {
  readonly relatorio = input.required<Relatorio>();

  protected readonly lista = computed(() => segmentos(this.relatorio()));
  protected readonly descricao = computed(() => descricaoDaBarra(this.lista()));
  protected readonly linhasTotais = computed(() => totais(this.relatorio()));
}

/** "Sobra dos últimos 6 meses" (spec 05, itens 11 a 15). */
@Component({
  selector: 'app-historico-sobra',
  template: `<section class="secao" aria-labelledby="titulo-historico">
    <h2 class="titulo-secao" id="titulo-historico">Sobra dos últimos 6 meses</h2>
    <div class="card historico-card">
      <div class="historico">
        @for (c of lista(); track c.mes) {
          <div class="col" data-cy="relatorio-coluna">
            <span class="rotulo" data-cy="relatorio-coluna-rotulo">{{ c.rotulo }}</span>
            <div class="barra" [class.atual]="c.atual" [class.negativa]="c.negativa"
              [style.height.%]="c.altura" data-cy="relatorio-coluna-barra"></div>
          </div>
        }
      </div>
      <div class="historico-meses" aria-hidden="true">
        @for (c of lista(); track c.mes) {
          <span>{{ c.curto }}</span>
        }
      </div>
    </div>
  </section>`,
})
export class HistoricoSobraComponent {
  readonly relatorio = input.required<Relatorio>();

  protected readonly lista = computed(() => colunas(this.relatorio()));
}
