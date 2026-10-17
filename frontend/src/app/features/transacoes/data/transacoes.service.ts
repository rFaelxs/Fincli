import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { DadosTransacao, Transacao } from '../../../core/api/modelos';

/** `/api/transacoes`. A lista vem inteira: os filtros rodam no front (spec 06). */
@Injectable({ providedIn: 'root' })
export class TransacoesService {
  private readonly http = inject(HttpClient);

  listar(): Promise<Transacao[]> {
    return firstValueFrom(this.http.get<Transacao[]>('/api/transacoes'));
  }

  buscar(id: string): Promise<Transacao> {
    return firstValueFrom(this.http.get<Transacao>(`/api/transacoes/${id}`));
  }

  criar(dados: DadosTransacao): Promise<Transacao> {
    return firstValueFrom(this.http.post<Transacao>('/api/transacoes', dados));
  }

  editar(id: string, dados: DadosTransacao): Promise<Transacao> {
    return firstValueFrom(this.http.put<Transacao>(`/api/transacoes/${id}`, dados));
  }

  excluir(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/transacoes/${id}`));
  }
}
