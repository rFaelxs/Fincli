import {
  Component,
  ElementRef,
  Injector,
  afterNextRender,
  computed,
  inject,
  viewChild,
} from '@angular/core';
import { diaMes, moeda } from '../../../shared/format/formatos';
import { FaixaAvisoComponent } from '../../../shared/ui/faixa-aviso.component';
import { GestaoStore } from '../data/gestao.store';

const SUGESTOES = ['guardar 300 viagem', 'uber 32 hoje', 'freela 450'];

const TITULO = {
  APORTE: 'vai guardar',
  ENTRADA: 'vai receber',
  PREVISTA: 'vai prever',
  SAIDA: 'vai lançar',
};
const ROTULO_VALOR = {
  APORTE: 'aporte ',
  ENTRADA: 'entrada ',
  PREVISTA: 'conta ',
  SAIDA: 'saída ',
};

/** Texto livre vira lançamento; o preview é do cliente, a decisão é do servidor. */
@Component({
  selector: 'app-barra-comando',
  imports: [FaixaAvisoComponent],
  template: `
    <section class="comando-secao" [class.destaque]="store.vazio()">
      <label class="visualmente-oculto" for="comando">Lançar por texto</label>
      <div class="comando">
        <span class="prompt" aria-hidden="true">&rsaquo;</span>
        <input
          #campo
          id="comando"
          type="text"
          autocomplete="off"
          spellcheck="false"
          placeholder="mercado 120 ontem essencial"
          aria-describedby="preview"
          [value]="store.texto()"
          [disabled]="store.enviando()"
          (input)="store.digitar(campo.value)"
          (keydown.enter)="$event.preventDefault(); lancar()"
          (keydown.escape)="store.limpar()"
        />
        <kbd class="tecla" aria-hidden="true">{{ tecla }}</kbd>
      </div>

      <div id="preview" class="preview" aria-live="polite">
        @if (preview(); as p) {
          <span class="rotulo">{{ p.titulo }}</span>
          <span class="chip-forte">{{ p.valor }}</span>
          @for (t of p.fracos; track $index) {
            <span class="chip-fraco">{{ t }}</span>
          }
          <span class="dica">enter para salvar</span>
        }
      </div>

      @if (store.sugestoesVisiveis()) {
        <div class="sugestoes">
          <span class="rotulo">tente:</span>
          @for (s of sugestoes; track s) {
            <button type="button" (click)="usar(s)">{{ s }}</button>
          }
        </div>
      }

      @if (store.faixa(); as f) {
        <app-faixa-aviso
          [texto]="f.texto"
          [acao]="f.acao"
          [erro]="f.erro"
          (acionar)="store.acionarFaixa()"
        />
      }
    </section>
  `,
  styleUrl: './barra-comando.component.css',
  host: { '(document:keydown)': 'atalho($event)' },
})
export class BarraComandoComponent {
  protected readonly store = inject(GestaoStore);
  private readonly injector = inject(Injector);
  private readonly campo = viewChild.required<ElementRef<HTMLInputElement>>('campo');

  protected readonly sugestoes = SUGESTOES;

  /** A tecla mostrada tem de ser a que funciona na máquina de quem olha. */
  protected readonly tecla = /Mac|iPhone|iPad/.test(navigator.platform || navigator.userAgent)
    ? '⌘K'
    : 'Ctrl K';

  protected readonly preview = computed(() => {
    const p = this.store.preview();
    if (!p) return null;
    const fracos =
      p.tipo === 'PREVISTA'
        ? [`vence dia ${p.diaVencimento}`, p.recorrente ? 'todo mês' : 'só este mês']
        : [
            p.categoria.toLowerCase(),
            diaMes(p.data),
            p.tipo === 'SAIDA' ? (p.essencial ? 'essencial' : 'supérflua') : null,
          ];
    return {
      titulo: TITULO[p.tipo],
      valor: ROTULO_VALOR[p.tipo] + moeda(p.valor),
      fracos: fracos.filter((t): t is string => !!t),
    };
  });

  focar(): void {
    this.campo().nativeElement.focus();
  }

  protected async lancar(): Promise<void> {
    await this.store.lancar();
    // O campo volta habilitado só depois da resposta; o foco vai junto.
    afterNextRender(() => this.focar(), { injector: this.injector });
  }

  protected usar(sugestao: string): void {
    this.store.usarSugestao(sugestao);
    this.focar();
  }

  /** ⌘K / Ctrl+K foca a barra de qualquer lugar da tela. */
  protected atalho(e: KeyboardEvent): void {
    if ((e.metaKey || e.ctrlKey) && e.key.toLowerCase() === 'k') {
      e.preventDefault();
      const campo = this.campo().nativeElement;
      campo.focus();
      campo.select();
    }
  }
}
