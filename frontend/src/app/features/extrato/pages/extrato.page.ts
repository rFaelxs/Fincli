import { Component, inject, signal } from '@angular/core';
import { mensagemDeErro } from '../../../core/api/problema';
import { ToastService } from '../../../shared/ui/toast.component';
import { ExtratoService } from '../data/extrato.service';
import { LinhaExtrato, linhasDoExtrato } from '../extrato';

/**
 * `/extrato` (spec 08). Os itens ficam direto no `.feed`, sem componente em volta de cada um: o
 * CSS depende de `.feed-day:first-child` e `.item:last-child` numa lista plana.
 */
@Component({
  selector: 'app-extrato',
  styleUrl: './extrato.page.css',
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Extrato</h1>
        <p class="sub">transações e movimentações de reserva · cronológico</p>
      </div>
    </header>
    @if (linhas(); as linhas) {
      <section class="card" aria-label="Lançamentos">
        @if (linhas.length > 0) {
          <div class="feed" data-cy="extrato-feed">
            @for (l of linhas; track $index) {
              @if (l.tipo === 'dia') {
                <h2 class="feed-day" data-cy="extrato-dia">{{ l.rotulo }}</h2>
              } @else {
                <div class="item" data-cy="extrato-item">
                  <span class="badge" [class.in]="l.selo === 'in'" [class.out]="l.selo === 'out'"
                    [class.mov]="l.selo === 'mov'" aria-hidden="true"
                    data-cy="extrato-simbolo">{{ l.simbolo }}</span>
                  <div class="body">
                    <div class="t" data-cy="extrato-titulo">{{ l.titulo }}</div>
                    <div class="s" data-cy="extrato-subtitulo">{{ l.subtitulo }}</div>
                  </div>
                  <span class="amt" [class.money-in]="l.selo === 'in'"
                    [class.money-out]="l.selo === 'out'" data-cy="extrato-valor">{{ l.valor }}</span>
                </div>
              }
            }
          </div>
        } @else {
          <p class="vazio" data-cy="extrato-vazio">Nada por aqui ainda.</p>
        }
      </section>
    }
  </div>`,
})
export class ExtratoPage {
  private readonly extrato = inject(ExtratoService);
  private readonly toast = inject(ToastService);

  /** Nulo enquanto carrega ou se a carga falhou: aí a tela não diz "Nada por aqui". */
  protected readonly linhas = signal<LinhaExtrato[] | null>(null);

  constructor() {
    void this.carregar();
  }

  private async carregar(): Promise<void> {
    try {
      this.linhas.set(linhasDoExtrato(await this.extrato.listar()));
    } catch (e) {
      this.toast.mostrar(mensagemDeErro(e));
    }
  }
}
