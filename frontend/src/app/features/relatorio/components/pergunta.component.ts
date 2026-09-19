import { Component, input, output } from '@angular/core';
import { Pergunta } from '../relatorio';

/** Um acordeão de "Perguntas do mês". */
@Component({
  selector: 'app-pergunta',
  template: `
    <div class="pergunta">
      <button
        type="button"
        [attr.aria-expanded]="aberta()"
        [attr.aria-controls]="'resp-' + pergunta().chave"
        (click)="alternar.emit()"
      >
        <span>{{ pergunta().titulo }}</span>
        <span class="seta" aria-hidden="true">{{ aberta() ? '▴' : '▾' }}</span>
      </button>
      @if (aberta()) {
        <div class="resposta" [id]="'resp-' + pergunta().chave">
          <span class="destaque">{{ pergunta().destaque }}</span>
          <p>{{ pergunta().texto }}</p>
          @for (l of pergunta().linhas; track $index) {
            <div class="linha-barra">
              <span class="rot">{{ l.rotulo }}</span>
              <div class="trilho"><i [style.width.%]="l.largura.toFixed(1)"></i></div>
              <span class="val">{{ l.valor }}</span>
            </div>
          }
        </div>
      }
    </div>
  `,
  styles: `
    .pergunta {
      border: 1px solid var(--fin-border);
      border-radius: var(--fin-r-xl);
      background: var(--fin-surface);
      overflow: hidden;
    }
    .pergunta > button {
      width: 100%;
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: var(--fin-s-4);
      padding: 18px 22px;
      text-align: left;
      font-weight: 500;
      font-size: 17px;
      color: var(--fin-ink);
      transition: background 0.12s;
    }
    .pergunta > button:hover {
      background: var(--fin-hover);
    }
    .pergunta .seta {
      font-family: var(--fin-font-mono);
      font-size: 14px;
      color: var(--fin-faint);
    }

    .resposta {
      padding: 0 22px 22px;
      display: flex;
      flex-direction: column;
      gap: var(--fin-s-3);
      animation: finRiseIn 0.25s ease both;
    }
    .resposta .destaque {
      font-family: var(--fin-font-mono);
      font-weight: 700;
      font-size: 26px;
      line-height: 1;
      letter-spacing: -0.02em;
      color: var(--fin-brand-deep);
      font-variant-numeric: tabular-nums;
    }
    .resposta p {
      max-width: 620px;
      font-size: 16px;
      line-height: 1.5;
      color: var(--fin-text);
      text-wrap: pretty;
    }

    .linha-barra {
      display: flex;
      align-items: center;
      gap: var(--fin-s-3);
    }
    .linha-barra .rot {
      width: 132px;
      flex: none;
      font-size: 14px;
      color: var(--fin-text);
    }
    .linha-barra .trilho {
      flex: 1;
      height: 10px;
      border-radius: 5px;
      background: var(--fin-hairline);
      overflow: hidden;
    }
    .linha-barra .trilho > i {
      display: block;
      height: 100%;
      border-radius: 5px;
      background: var(--fin-brand);
    }
    .linha-barra .val {
      width: 104px;
      flex: none;
      text-align: right;
      font-family: var(--fin-font-mono);
      font-weight: 500;
      font-size: 14px;
      font-variant-numeric: tabular-nums;
      white-space: nowrap;
    }

    @media (max-width: 560px) {
      .linha-barra {
        flex-wrap: wrap;
      }
      .linha-barra .rot {
        width: auto;
      }
      .linha-barra .val {
        width: auto;
        margin-left: auto;
      }
      .linha-barra .trilho {
        flex-basis: 100%;
        order: 3;
      }
    }
  `,
})
export class PerguntaComponent {
  readonly pergunta = input.required<Pergunta>();
  readonly aberta = input.required<boolean>();
  readonly alternar = output<void>();
}
