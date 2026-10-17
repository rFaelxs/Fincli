import { Injectable, inject, signal } from '@angular/core';
import { ContaPrevista, Hoje, TipoComando } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { GuardadoService } from '../../../layout/shell/data/guardado.service';
import { ToastService } from '../../../shared/ui/toast.component';
import { GestaoApiService } from './gestao-api.service';

/** A faixa acima da lista: confirmação com "desfazer" ou erro com "tentar de novo". */
export interface Faixa {
  texto: string;
  acao: 'desfazer' | 'tentar de novo';
  erro: boolean;
}

/** O que o "desfazer" da faixa sabe desfazer. Só o último lançamento, e só em memória. */
type Desfazivel =
  | { tipo: TipoComando; id: string | null; reservaId: string | null; valor: number }
  | { tipo: 'PREVISTA_REMOVIDA'; conta: ContaPrevista; mes: string };

export const APORTE_DESFEITO = 'Aporte desfeito — o saque ficou registrado no extrato.';

/**
 * Estado da tela Gestão, compartilhado pelos componentes dela. Fornecido pela página: nasce e
 * morre com a tela. Depois de cada operação a tela inteira é relida (spec 03, item 20).
 */
@Injectable()
export class GestaoStore {
  private readonly api = inject(GestaoApiService);
  private readonly guardado = inject(GuardadoService);
  private readonly toast = inject(ToastService);
  private desfazivel: Desfazivel | null = null;

  /** Nulo até a primeira carga, que a tela mostra com esqueleto. */
  readonly dados = signal<Hoje | null>(null);
  readonly faixa = signal<Faixa | null>(null);

  async carregar(): Promise<void> {
    try {
      this.dados.set(await this.api.hoje());
    } catch (e) {
      this.falhou(e);
    }
  }

  /**
   * Envia o texto da barra. A mensagem da faixa vem pronta do servidor.
   *
   * @returns se o lançamento foi gravado
   */
  async lancar(texto: string): Promise<boolean> {
    try {
      const r = await this.api.comando(texto);
      this.desfazivel = { tipo: r.tipo, id: r.id, reservaId: r.reservaId, valor: r.valor };
      this.faixa.set({ texto: r.mensagem, acao: 'desfazer', erro: false });
      await this.depoisDeMudar();
      return true;
    } catch (e) {
      this.falhou(e);
      return false;
    }
  }

  /** Remove a conta e oferece o "desfazer", que a recria com os mesmos campos (itens 20 e 21). */
  async removerConta(conta: ContaPrevista): Promise<void> {
    const mes = this.dados()?.mes ?? '';
    try {
      await this.api.removerPrevista(conta.id);
      this.desfazivel = { tipo: 'PREVISTA_REMOVIDA', conta, mes };
      this.faixa.set({ texto: `Removido ${conta.nome}`, acao: 'desfazer', erro: false });
      await this.depoisDeMudar();
    } catch (e) {
      this.falhou(e);
    }
  }

  /** O botão da faixa: desfaz o último lançamento ou tenta carregar de novo (item 22). */
  async agir(): Promise<void> {
    const faixa = this.faixa();
    this.faixa.set(null);
    if (faixa?.acao === 'desfazer') {
      await this.desfazer();
    } else {
      await this.carregar();
    }
  }

  /** Spec 04, itens 29 e 30. */
  private async desfazer(): Promise<void> {
    const alvo = this.desfazivel;
    this.desfazivel = null;
    if (!alvo) {
      return;
    }
    try {
      if (alvo.tipo === 'PREVISTA_REMOVIDA') {
        const { nome, valor, diaVencimento, recorrente } = alvo.conta;
        await this.api.recriarPrevista({
          nome, valor, diaVencimento, mesReferencia: recorrente ? null : alvo.mes,
        });
      } else if (alvo.tipo === 'APORTE') {
        // Não apaga a alocação: saca o mesmo valor, e o extrato mostra os dois movimentos.
        await this.api.sacar(alvo.reservaId!, alvo.valor);
        this.toast.mostrar(APORTE_DESFEITO);
      } else if (alvo.tipo === 'PREVISTA') {
        await this.api.removerPrevista(alvo.id!);
      } else {
        await this.api.excluirTransacao(alvo.id!);
      }
      await this.depoisDeMudar();
    } catch (e) {
      this.falhou(e);
    }
  }

  /** Relê a tela e o card "GUARDADO" do shell, que não recarrega sem navegação. */
  private async depoisDeMudar(): Promise<void> {
    void this.guardado.carregar();
    await this.carregar();
  }

  private falhou(erro: unknown): void {
    this.faixa.set({ texto: mensagemDeErro(erro), acao: 'tentar de novo', erro: true });
  }
}
