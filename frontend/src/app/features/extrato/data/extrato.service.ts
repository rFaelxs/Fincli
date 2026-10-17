import { HttpClient } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import { firstValueFrom } from 'rxjs';
import { ItemExtrato } from '../../../core/api/modelos';

/** `GET /api/extrato`: o histórico inteiro, já ordenado pelo servidor. */
@Injectable({ providedIn: 'root' })
export class ExtratoService {
  private readonly http = inject(HttpClient);

  listar(): Promise<ItemExtrato[]> {
    return firstValueFrom(this.http.get<ItemExtrato[]>('/api/extrato'));
  }
}
