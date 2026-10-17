import { Component, ViewEncapsulation, computed, effect, inject, signal, untracked } from '@angular/core';
import { Relatorio } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { ToastService } from '../../../shared/ui/toast.component';
import { ultimosMeses } from '../../transacoes/transacoes';
import {
  BarraFluxoComponent,
  HistoricoSobraComponent,
} from '../components/fluxo-e-historico.component';
import { PerguntaComponent } from '../components/pergunta.component';
import { PerguntasAbertas, RelatorioService } from '../data/relatorio.service';
import { eyebrow, perguntas } from '../relatorio';

/**
 * `/relatorio` (spec 05). O CSS da tela é o do front antigo, sem encapsulamento, para valer nos
 * componentes filhos; os nomes das classes são próprios desta tela.
 */
@Component({
  selector: 'app-relatorio',
  imports: [BarraFluxoComponent, HistoricoSobraComponent, PerguntaComponent],
  styleUrl: '../relatorio.css',
  encapsulation: ViewEncapsulation.None,
  template: `<div class="pagina">
    <section class="cabecalho">
      <div>
        <span class="eyebrow" data-cy="relatorio-eyebrow">{{ rotulo() }}</span>
        <h1>Para onde foi o dinheiro</h1>
      </div>
      <select aria-label="Mês do relatório" (change)="escolher($event)" data-cy="relatorio-mes">
        @for (m of meses; track m.valor) {
          <option [value]="m.valor" [selected]="m.valor === mes()">{{ m.rotulo }}</option>
        }
      </select>
    </section>
    @if (dados(); as r) {
      <app-barra-fluxo [relatorio]="r" />
      <app-historico-sobra [relatorio]="r" />
      <section class="secao" aria-labelledby="titulo-perguntas">
        <h2 class="titulo-secao" id="titulo-perguntas">Perguntas do mês</h2>
        <div class="perguntas">
          @for (p of listaPerguntas(); track $index) {
            <app-pergunta [pergunta]="p" [indice]="$index" [aberta]="abertas.abertas()[$index]"
              (alternar)="abertas.alternar($index)" />
          }
        </div>
      </section>
    }
  </div>`,
})
export class RelatorioPage {
  private readonly servico = inject(RelatorioService);
  private readonly toast = inject(ToastService);
  protected readonly abertas = inject(PerguntasAbertas);

  /** Os 12 últimos meses, do mais recente para o mais antigo (item 2). */
  protected readonly meses = ultimosMeses(12);
  protected readonly mes = signal(this.meses[0].valor);
  /** O conteúdo anterior fica na tela enquanto o novo mês carrega (item 31). */
  protected readonly dados = signal<Relatorio | null>(null);

  protected readonly rotulo = computed(() => eyebrow(this.mes()));
  protected readonly listaPerguntas = computed(() => {
    const r = this.dados();
    return r ? perguntas(r) : [];
  });

  constructor() {
    // Trocar o mês refaz a chamada e repinta a tela inteira (item 3).
    effect(() => {
      const mes = this.mes();
      untracked(() => void this.carregar(mes));
    });
  }

  protected escolher(evento: Event): void {
    this.mes.set((evento.target as HTMLSelectElement).value);
  }

  private async carregar(mes: string): Promise<void> {
    try {
      this.dados.set(await this.servico.doMes(mes));
    } catch (e) {
      // Erro não limpa a tela: o toast avisa e o mês anterior continua (item 31).
      this.toast.mostrar(mensagemDeErro(e));
    }
  }
}
