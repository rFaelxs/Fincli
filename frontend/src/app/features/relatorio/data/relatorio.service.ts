import { HttpClient } from '@angular/common/http';
import { Injectable, inject, signal } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Relatorio } from '../../../core/api/modelos';

/** `GET /api/relatorio?mes=yyyy-MM`. */
@Injectable({ providedIn: 'root' })
export class RelatorioService {
  private readonly http = inject(HttpClient);

  doMes(mes: string): Promise<Relatorio> {
    return firstValueFrom(this.http.get<Relatorio>('/api/relatorio', { params: { mes } }));
  }
}

/**
 * Quais perguntas estão abertas. Vive fora da tela, em memória: trocar de mês, ou sair e voltar,
 * mantém cada pergunta como o usuário deixou (spec 05, observação). A primeira nasce aberta.
 */
@Injectable({ providedIn: 'root' })
export class PerguntasAbertas {
  readonly abertas = signal([true, false, false]);

  alternar(indice: number): void {
    this.abertas.update((a) => a.map((aberta, i) => (i === indice ? !aberta : aberta)));
  }
}
