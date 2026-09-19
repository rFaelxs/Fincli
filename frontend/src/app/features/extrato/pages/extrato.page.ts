import { Component, OnInit, inject, signal } from '@angular/core';
import { lerErro } from '../../../core/api/problema';
import { ToastService } from '../../../shared/ui/toast.component';
import { ExtratoService } from '../data/extrato.service';
import { LinhaExtrato, linhasDoExtrato } from '../extrato';

/**
 * Os itens ficam direto no .feed, sem elemento em volta: o CSS depende de .feed-day:first-child e
 * .item:last-child numa lista plana.
 */
@Component({
  selector: 'app-extrato-page',
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Extrato</h1>
        <span class="asof">transações e movimentações de reserva · cronológico</span>
      </div>
    </div>

    <div class="card">
      <div class="feed">
        @if (linhas(); as lista) {
          @for (l of lista; track $index) {
            @if (l.dia) {
              <div class="feed-day">{{ l.dia }}</div>
            }
            <div class="item">
              <span class="badge" [class]="l.classe" aria-hidden="true">{{ l.glifo }}</span>
              <span class="body"
                ><span class="t">{{ l.titulo }}</span
                >&ngsp;<span class="s">{{ l.subtitulo }}</span></span
              >
              <span class="amt num" [class]="l.tom">{{ l.valor }}</span>
            </div>
          } @empty {
            <div class="vazio">Nada por aqui ainda.</div>
          }
        }
      </div>
    </div>
  `,
  styleUrl: './extrato.page.css',
})
export class ExtratoPage implements OnInit {
  private readonly extrato = inject(ExtratoService);
  private readonly toast = inject(ToastService);

  /** Nulo até a primeira resposta: a lista vazia só aparece depois de carregar. */
  protected readonly linhas = signal<LinhaExtrato[] | null>(null);

  async ngOnInit(): Promise<void> {
    try {
      this.linhas.set(linhasDoExtrato(await this.extrato.listar()));
    } catch (e) {
      this.toast.mostrar(lerErro(e));
    }
  }
}
