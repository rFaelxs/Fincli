import { Component, computed, inject } from '@angular/core';
import { RouterLink } from '@angular/router';
import { TipoLancamento } from '../../../core/api/modelos';
import { diaMes, moedaAbs } from '../../../shared/format/formatos';
import { GestaoStore } from '../data/gestao.store';

// Movimentação de reserva não é entrada nem saída: fica sem sinal e ganha um selo com o verbo.
// Setas como ↳ faltam na JetBrains Mono e o navegador desenha um glifo qualquer no lugar.
const SINAL: Record<TipoLancamento, string> = { ENTRADA: '+', SAIDA: '−', APORTE: '', SAQUE: '' };
const CLASSE: Record<TipoLancamento, string> = {
  ENTRADA: 'entrada',
  SAIDA: 'saida',
  APORTE: 'aporte',
  SAQUE: 'aporte',
};
/** O que o leitor de tela lê antes do valor: o sinal em palavra. */
const LEITURA: Record<TipoLancamento, string> = {
  ENTRADA: 'entrada de',
  SAIDA: 'saída de',
  APORTE: 'guardado',
  SAQUE: 'sacado',
};
const SELO: Partial<Record<TipoLancamento, string>> = { APORTE: 'guardado', SAQUE: 'sacado' };

@Component({
  selector: 'app-lista-lancamentos',
  imports: [RouterLink],
  template: `
    <section class="secao">
      <div class="secao-head">
        <h2 class="titulo-secao">Últimos lançamentos</h2>
        <a class="link-secao" routerLink="/relatorio">ver o mês inteiro →</a>
      </div>
      <div class="lista">
        @if (linhas(); as lista) {
          @for (l of lista; track $index) {
            <div class="linha">
              <span class="dia">{{ l.dia }}</span>
              <span class="desc">{{ l.descricao }}</span>
              <span class="cat">{{ l.categoria }}</span>
              @if (l.essencial) {
                <span class="chip">essencial</span>
              }
              @if (l.selo) {
                <span class="chip">{{ l.selo }}</span>
              }
              <span class="valor" [class]="l.classe"
                ><span class="visualmente-oculto">{{ l.leitura + ' ' }}</span
                >{{ l.valor }}</span
              >
            </div>
          } @empty {
            <div class="vazio-lista">Nenhum lançamento neste mês ainda.</div>
          }
        }
      </div>
    </section>
  `,
  styleUrl: './lista-lancamentos.component.css',
})
export class ListaLancamentosComponent {
  private readonly dados = inject(GestaoStore).dados;

  protected readonly linhas = computed(() =>
    this.dados()?.ultimosLancamentos.map((l) => ({
      dia: diaMes(l.data),
      descricao: l.descricao,
      categoria: l.categoria,
      essencial: l.essencial,
      selo: SELO[l.tipo] ?? null,
      classe: CLASSE[l.tipo],
      leitura: LEITURA[l.tipo],
      valor: SINAL[l.tipo] + moedaAbs(l.valor),
    })),
  );
}
