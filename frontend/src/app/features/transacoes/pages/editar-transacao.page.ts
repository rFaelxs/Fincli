import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { Router } from '@angular/router';
import { DadosTransacao } from '../../../core/api/modelos';
import { mensagemDeErro } from '../../../core/api/problema';
import { FormularioTransacaoComponent } from '../components/formulario-transacao.component';
import { TransacoesService } from '../data/transacoes.service';

type Carga =
  | { tipo: 'carregando' }
  | { tipo: 'falhou'; mensagem: string }
  | { tipo: 'pronta'; dados: DadosTransacao };

/** `/transacoes/:id/editar` (spec 06, itens 28 a 32). */
@Component({
  selector: 'app-editar-transacao',
  imports: [FormularioTransacaoComponent],
  template: `<div class="pagina">
    <header class="head">
      <div>
        <h1>Editar transação</h1>
        <p class="sub" data-cy="transacao-subtitulo">{{ subtitulo() }}</p>
      </div>
    </header>
    @if (carga(); as c) {
      @if (c.tipo === 'pronta') {
        <app-formulario-transacao rotuloEnvio="Salvar alterações" [inicial]="c.dados"
          [erro]="erro()" (enviar)="salvar($event)" />
      }
    }
  </div>`,
})
export class EditarTransacaoPage implements OnInit {
  private readonly transacoes = inject(TransacoesService);
  private readonly router = inject(Router);

  /** Vem do parâmetro da rota. */
  readonly id = input<string>();

  protected readonly carga = signal<Carga>({ tipo: 'carregando' });
  protected readonly erro = signal('');

  /** Falha na carga troca o subtítulo pela mensagem e deixa o formulário escondido (item 30). */
  protected readonly subtitulo = computed(() => {
    const c = this.carga();
    if (c.tipo === 'carregando') {
      return 'Carregando…';
    }
    return c.tipo === 'falhou' ? c.mensagem : 'Altere os campos e salve.';
  });

  async ngOnInit(): Promise<void> {
    const id = this.id();
    if (!id) {
      await this.router.navigateByUrl('/transacoes', { replaceUrl: true });
      return;
    }
    try {
      const { id: _, ...dados } = await this.transacoes.buscar(id);
      this.carga.set({ tipo: 'pronta', dados });
    } catch (e) {
      this.carga.set({ tipo: 'falhou', mensagem: mensagemDeErro(e) });
    }
  }

  protected async salvar(dados: DadosTransacao): Promise<void> {
    this.erro.set('');
    try {
      await this.transacoes.editar(this.id()!, dados);
      await this.router.navigateByUrl('/transacoes', { replaceUrl: true });
    } catch (e) {
      this.erro.set(mensagemDeErro(e));
    }
  }
}
