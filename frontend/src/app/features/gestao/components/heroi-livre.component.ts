import { Component, computed, inject } from '@angular/core';
import { diaPorExtenso, moeda, moedaAbs, soMes } from '../../../shared/format/formatos';
import { GestaoStore } from '../data/gestao.store';

/** Quanto dá para gastar até o fim do mês — o número grande da tela. */
@Component({
  selector: 'app-heroi-livre',
  template: `
    <section class="heroi">
      <span class="eyebrow">{{ rotulo() }}</span>
      <div class="heroi-linha">
        <span class="heroi-valor num" [class.esqueleto]="!dados()" [class.negativo]="negativo()">{{
          valor()
        }}</span>
        <span class="pilula" [class.negativo]="negativo()">{{ pilula() }}</span>
      </div>
      <p class="heroi-texto">{{ texto() }}</p>
    </section>
  `,
  styleUrl: './heroi-livre.component.css',
})
export class HeroiLivreComponent {
  private readonly store = inject(GestaoStore);

  protected readonly dados = this.store.dados;

  // No vermelho o número muda de cor, mas quem carrega o significado é o rótulo e a pílula: cor
  // sozinha não comunica.
  protected readonly negativo = computed(() => Number(this.dados()?.livre ?? 0) < 0);

  protected readonly rotulo = computed(() => {
    const d = this.dados();
    if (!d) return 'DÁ PRA GASTAR';
    if (this.store.vazio()) return 'SEM LANÇAMENTOS AINDA';
    const ate = ' · ATÉ ' + diaPorExtenso(d.fimDoMes).toUpperCase();
    return (this.negativo() ? 'NO VERMELHO' : 'DÁ PRA GASTAR') + ate;
  });

  protected readonly valor = computed(() => {
    const d = this.dados();
    if (!d || this.store.vazio()) return moeda(0);
    return moedaAbs(d.livre);
  });

  protected readonly pilula = computed(() => {
    const d = this.dados();
    if (!d) return '';
    if (this.store.vazio()) return soMes(d.mes) + ' está em branco';
    if (this.negativo()) return moedaAbs(d.livre) + ' no vermelho';
    return (
      moeda(d.porDia) + ' por dia · ' + d.diasRestantes + (d.diasRestantes === 1 ? ' dia' : ' dias')
    );
  });

  protected readonly texto = computed(() => {
    const d = this.dados();
    if (!d) return '';
    if (this.store.vazio()) {
      return 'Lance o primeiro gasto na barra abaixo — é só escrever o que aconteceu.';
    }
    const mes = soMes(d.mes);
    return this.negativo()
      ? `As contas que ainda vencem e o que já saiu passam do que entrou em ${mes}. ` +
          'Cortar aqui é escolher o que não vai acontecer.'
      : 'Já descontei as contas que ainda vencem no mês e o aporte das reservas. ' +
          `É dinheiro que você pode gastar sem estragar ${mes}.`;
  });
}
