import { Component, computed, effect, inject, signal } from '@angular/core';
import { Relatorio } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { mesPorExtenso, ultimosMeses } from '../../../shared/format/formatos';
import { ToastService } from '../../../shared/ui/toast.component';
import { BarraFluxoComponent } from '../components/barra-fluxo.component';
import { HistoricoSobraComponent } from '../components/historico-sobra.component';
import { PerguntaComponent } from '../components/pergunta.component';
import { SeletorMesComponent } from '../components/seletor-mes.component';
import { RelatorioService } from '../data/relatorio.service';
import { Pergunta, colunasDoHistorico, perguntasDoMes } from '../relatorio';

/**
 * Quais perguntas estão abertas. Vive fora da página, como na tela antiga: trocar de mês, ou
 * sair e voltar, mantém o acordeão como o usuário deixou. A primeira nasce aberta.
 */
const abertas: Record<Pergunta['chave'], boolean> = { q1: true, q2: false, q3: false };

@Component({
  selector: 'app-relatorio-page',
  imports: [SeletorMesComponent, BarraFluxoComponent, HistoricoSobraComponent, PerguntaComponent],
  host: { class: 'pagina' },
  template: `
    <section class="cabecalho">
      <div>
        <span class="eyebrow">{{ eyebrow() }}</span>
        <h1>Para onde foi o dinheiro</h1>
      </div>
      <app-seletor-mes [(mes)]="mes" />
    </section>

    <app-barra-fluxo [dados]="dados()" />

    <section class="secao">
      <h2 class="titulo-secao">Sobra dos últimos 6 meses</h2>
      <app-historico-sobra [colunas]="colunas()" />
    </section>

    <section class="secao">
      <h2 class="titulo-secao">Perguntas do mês</h2>
      <div class="perguntas">
        @for (q of perguntas(); track q.chave) {
          <app-pergunta
            [pergunta]="q"
            [aberta]="aberta()[q.chave]"
            (alternar)="alternar(q.chave)"
          />
        }
      </div>
    </section>
  `,
  styles: `
    .cabecalho {
      display: flex;
      align-items: flex-end;
      justify-content: space-between;
      gap: var(--fin-s-4);
      flex-wrap: wrap;
    }
    .cabecalho > div {
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-2);
    }

    .secao {
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-4);
    }

    .perguntas {
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-3);
    }
  `,
})
export class RelatorioPage {
  private readonly servico = inject(RelatorioService);
  private readonly toast = inject(ToastService);

  protected readonly mes = signal(ultimosMeses(1)[0].valor);
  protected readonly dados = signal<Relatorio | null>(null);
  protected readonly aberta = signal({ ...abertas });

  protected readonly eyebrow = computed(() => {
    const d = this.dados();
    return d ? 'RELATÓRIO · ' + mesPorExtenso(d.mes).toUpperCase() : 'RELATÓRIO';
  });
  protected readonly colunas = computed(() => {
    const d = this.dados();
    return d ? colunasDoHistorico(d.historico, d.mes) : [];
  });
  protected readonly perguntas = computed(() => {
    const d = this.dados();
    return d ? perguntasDoMes(d) : [];
  });

  constructor() {
    // Carrega ao abrir e a cada troca de mês.
    effect(() => void this.carregar(this.mes()));
  }

  protected alternar(chave: Pergunta['chave']): void {
    abertas[chave] = !abertas[chave];
    this.aberta.set({ ...abertas });
  }

  /** Um erro não limpa a tela: o mês anterior continua visível e o motivo vai no toast. */
  private async carregar(mes: string): Promise<void> {
    try {
      this.dados.set(await this.servico.doMes(mes));
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }
}
