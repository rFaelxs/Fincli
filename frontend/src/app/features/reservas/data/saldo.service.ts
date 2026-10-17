import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom, map } from 'rxjs';

/**
 * `GET /api/saldo`: o saldo disponível para alocar. É só orientação: quem decide é a releitura
 * que o servidor faz dentro da alocação (spec 07, item 25).
 */
@Injectable({ providedIn: 'root' })
export class SaldoService {
  private readonly http = inject(HttpClient);

  disponivel(): Promise<number> {
    return firstValueFrom(
      this.http
        .get<{ saldoDisponivel: number }>('/api/saldo')
        .pipe(map((s) => s.saldoDisponivel)),
    );
  }
}
