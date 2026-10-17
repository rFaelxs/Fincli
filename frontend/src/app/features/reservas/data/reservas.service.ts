import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { Reserva } from '../../../core/api/modelos';

/** `/api/reservas`. Alocar, sacar e meta mandam um valor só (spec 07). */
@Injectable({ providedIn: 'root' })
export class ReservasService {
  private readonly http = inject(HttpClient);

  /** A Reserva de Emergência vem primeiro e as demais por nome, já ordenadas pelo servidor. */
  listar(): Promise<Reserva[]> {
    return firstValueFrom(this.http.get<Reserva[]>('/api/reservas'));
  }

  buscar(id: string): Promise<Reserva> {
    return firstValueFrom(this.http.get<Reserva>(`/api/reservas/${id}`));
  }

  criar(nome: string, metaValor: number): Promise<Reserva> {
    return firstValueFrom(this.http.post<Reserva>('/api/reservas', { nome, metaValor }));
  }

  alocar(id: string, valor: number): Promise<Reserva> {
    return firstValueFrom(this.http.post<Reserva>(`/api/reservas/${id}/alocar`, { valor }));
  }

  sacar(id: string, valor: number): Promise<Reserva> {
    return firstValueFrom(this.http.post<Reserva>(`/api/reservas/${id}/sacar`, { valor }));
  }

  alterarMeta(id: string, valor: number): Promise<Reserva> {
    return firstValueFrom(this.http.put<Reserva>(`/api/reservas/${id}/meta`, { valor }));
  }

  excluir(id: string): Promise<void> {
    return firstValueFrom(this.http.delete<void>(`/api/reservas/${id}`));
  }
}
