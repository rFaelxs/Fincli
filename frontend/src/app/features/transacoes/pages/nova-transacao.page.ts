import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DadosTransacao } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { FormularioTransacaoComponent } from '../components/formulario-transacao.component';
import { TransacoesService } from '../data/transacoes.service';

/** `/transacoes/nova` (spec 06, itens 19 a 27). */
@Component({
  selector: 'app-nova-transacao',
  imports: [FormularioTransacaoComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Nova transação</h1>
        <p class="sub">Registre uma entrada ou saída.</p>
      </div>
    </header>
    <app-formulario-transacao rotuloEnvio="Salvar" [erro]="erro()" (enviar)="salvar($event)" />
  </div>`,
})
export class NovaTransacaoPage {
  private readonly transacoes = inject(TransacoesService);
  private readonly router = inject(Router);

  protected readonly erro = signal('');

  /** Sucesso troca a rota sem deixar o formulário no histórico; erro mantém o que foi digitado. */
  protected async salvar(dados: DadosTransacao): Promise<void> {
    this.erro.set('');
    try {
      await this.transacoes.criar(dados);
      await this.router.navigateByUrl('/transacoes', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
