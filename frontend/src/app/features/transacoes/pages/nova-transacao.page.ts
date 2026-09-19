import { Component, inject, signal } from '@angular/core';
import { Router } from '@angular/router';
import { TransacaoRequest } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { FormularioTransacaoComponent } from '../components/formulario-transacao.component';
import { TransacoesService } from '../data/transacoes.service';

@Component({
  selector: 'app-nova-transacao-page',
  imports: [FormularioTransacaoComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Nova transação</h1>
        <span class="sub">Registre uma entrada ou saída.</span>
      </div>
    </div>

    <app-formulario-transacao
      [comExemplos]="true"
      rotuloEnviar="Salvar"
      [erro]="erro()"
      (enviar)="salvar($event)"
    />
  `,
})
export class NovaTransacaoPage {
  private readonly transacoes = inject(TransacoesService);
  private readonly router = inject(Router);

  protected readonly erro = signal('');

  protected async salvar(req: TransacaoRequest): Promise<void> {
    this.erro.set('');
    try {
      await this.transacoes.criar(req);
      await this.router.navigateByUrl('/transacoes', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
