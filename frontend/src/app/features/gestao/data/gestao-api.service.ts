import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Hoje, ResultadoComando } from '../../../core/api/modelos';

/** Corpo do `POST /api/previstas`, usado só pelo "desfazer" de uma remoção. */
export interface NovaContaPrevista {
  nome: string;
  valor: number;
  diaVencimento: number;
  /** `yyyy-MM` em conta avulsa; nulo em recorrente. */
  mesReferencia: string | null;
}

/** As chamadas da Gestão: a tela, a barra de comando e cada "desfazer". */
@Injectable({ providedIn: 'root' })
export class GestaoApiService {
  private readonly http = inject(HttpClient);

  hoje(): Promise<Hoje> {
    return firstValueFrom(this.http.get<Hoje>('/api/hoje'));
  }

  /** Só o texto trafega: tipo, valor e categoria são decididos no servidor. */
  comando(texto: string): Promise<ResultadoComando> {
    return firstValueFrom(this.http.post<ResultadoComando>('/api/comando', { texto }));
  }

  removerPrevista(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/previstas/${id}`));
  }

  recriarPrevista(conta: NovaContaPrevista): Promise<void> {
    return firstValueFrom(this.http.post<void>('/api/previstas', conta));
  }

  excluirTransacao(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/transacoes/${id}`));
  }

  sacar(reservaId: string, valor: number): Promise<void> {
    return firstValueFrom(this.http.post<void>(`/api/reservas/${reservaId}/sacar`, { valor }));
  }
}
