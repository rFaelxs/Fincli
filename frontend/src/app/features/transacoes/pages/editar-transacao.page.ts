import { Component, OnInit, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { Transacao, TransacaoRequest } from '../../../core/api/modelos';
import { lerErro } from '../../../core/api/problema';
import { FormularioTransacaoComponent } from '../components/formulario-transacao.component';
import { TransacoesService } from '../data/transacoes.service';

@Component({
  selector: 'app-editar-transacao-page',
  imports: [FormularioTransacaoComponent],
  host: { class: 'pagina' },
  template: `
    <div class="head">
      <div>
        <h1>Editar transação</h1>
        <span class="sub">{{ subtitulo() }}</span>
      </div>
    </div>

    @if (transacao(); as t) {
      <app-formulario-transacao
        [inicial]="t"
        rotuloEnviar="Salvar alterações"
        [erro]="erro()"
        (enviar)="salvar($event)"
      />
    }
  `,
})
export class EditarTransacaoPage implements OnInit {
  private readonly transacoes = inject(TransacoesService);
  private readonly router = inject(Router);

  /** Da rota `/transacoes/:id/editar`. */
  readonly id = input.required<string>();

  protected readonly transacao = signal<Transacao | null>(null);
  protected readonly subtitulo = signal('Carregando…');
  protected readonly erro = signal('');

  async ngOnInit(): Promise<void> {
    try {
      this.transacao.set(await this.transacoes.buscar(this.id()));
      this.subtitulo.set('Altere os campos e salve.');
    } catch (e) {
      // Sem a transação o formulário não aparece: a mensagem fica no lugar do subtítulo.
      this.subtitulo.set(lerErro(e));
    }
  }

  protected async salvar(req: TransacaoRequest): Promise<void> {
    this.erro.set('');
    try {
      await this.transacoes.editar(this.id(), req);
      await this.router.navigateByUrl('/transacoes', { replaceUrl: true });
    } catch (e) {
      this.erro.set(lerErro(e));
    }
  }
}
