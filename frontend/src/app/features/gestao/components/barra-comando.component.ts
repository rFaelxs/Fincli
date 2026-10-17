import { Component, ElementRef, computed, inject, input, signal, viewChild } from '@angular/core';
import { FaixaAvisoComponent } from '../../../shared/ui/faixa-aviso.component';
import { interpretar } from '../comando/gramatica';
import { GestaoStore } from '../data/gestao.store';
import { partesDoPreview } from '../gestao';

/** Exemplos mostrados quando não há preview (spec 04, item 23). */
export const SUGESTOES = ['guardar 300 viagem', 'uber 32 hoje', 'freela 450'];

/**
 * A barra de comando: campo, preview com a mesma gramática do servidor, sugestões e a faixa de
 * confirmação ou erro (spec 04, itens 15 a 37). `Ctrl/⌘K` ficou fora desta entrega.
 */
@Component({
  selector: 'app-barra-comando',
  imports: [FaixaAvisoComponent],
  styleUrl: './barra-comando.component.css',
  template: `<section class="comando-secao" [class.destaque]="destaque()" aria-label="Barra de comando">
    <div class="comando">
      <span class="prompt" aria-hidden="true">›</span>
      <label class="visualmente-oculto" for="comando">Lançar por texto</label>
      <input #campo id="comando" data-cy="comando-campo" autocomplete="off"
        placeholder="mercado 120 ontem essencial" [value]="texto()" [disabled]="enviando()"
        (input)="digitou($event)" (keydown.enter)="enviar()" (keydown.escape)="limpar()">
    </div>
    <div class="preview" aria-live="polite" data-cy="comando-preview">
      @if (preview(); as p) {
        <span class="rotulo">{{ p.verbo }}</span>
        <span class="chip-forte">{{ p.valor }}</span>
        @for (extra of p.extras; track $index) {
          <span class="chip-fraco">{{ extra }}</span>
        }
        <span class="dica">enter para salvar</span>
      }
    </div>
    @if (!preview() && !store.faixa()) {
      <div class="sugestoes" data-cy="comando-sugestoes">
        <span class="rotulo">tente:</span>
        @for (s of sugestoes; track s) {
          <button type="button" (click)="usar(s)" data-cy="comando-sugestao">{{ s }}</button>
        }
      </div>
    }
    @if (store.faixa(); as f) {
      <app-faixa-aviso [texto]="f.texto" [acao]="f.acao" [erro]="f.erro" (agir)="store.agir()" />
    }
  </section>`,
})
export class BarraComandoComponent {
  protected readonly store = inject(GestaoStore);
  private readonly campo = viewChild.required<ElementRef<HTMLInputElement>>('campo');

  /** A barra ganha destaque quando o mês está em branco (spec 03, item 9). */
  readonly destaque = input(false);

  protected readonly sugestoes = SUGESTOES;
  protected readonly texto = signal('');
  protected readonly enviando = signal(false);
  protected readonly preview = computed(() => {
    const comando = interpretar(this.texto());
    return comando ? partesDoPreview(comando) : null;
  });

  protected digitou(evento: Event): void {
    this.texto.set((evento.target as HTMLInputElement).value);
  }

  /** O exemplo preenche o campo, devolve o foco e refaz o preview (item 24). */
  protected usar(sugestao: string): void {
    this.texto.set(sugestao);
    this.campo().nativeElement.focus();
  }

  protected limpar(): void {
    this.texto.set('');
  }

  /** Texto sem preview não é enviado; o campo fica desabilitado durante o envio (itens 25 e 26). */
  protected async enviar(): Promise<void> {
    if (!this.preview() || this.enviando()) {
      return;
    }
    this.enviando.set(true);
    const gravou = await this.store.lancar(this.texto());
    this.enviando.set(false);
    if (gravou) {
      this.texto.set('');
    }
    // O campo só aceita foco depois que o `disabled` sair do DOM.
    setTimeout(() => this.campo().nativeElement.focus());
  }
}
