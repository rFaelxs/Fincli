/* ═══ Reservas · alocar / sacar ═══ */
'use strict';

const id = App.qs('id');
const acao = App.qs('acao') === 'sacar' ? 'sacar' : 'alocar';

const textos = {
  alocar: { titulo: 'Alocar na reserva', sub: 'Move dinheiro do saldo disponível para a reserva.', ok: 'Alocar' },
  sacar:  { titulo: 'Sacar da reserva',  sub: 'Devolve dinheiro da reserva ao saldo disponível.',  ok: 'Sacar' }
};

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'reservas');

  if (!id) { location.replace('/reservas/'); return; }

  document.getElementById('titulo').textContent = textos[acao].titulo;
  document.getElementById('btn-ok').textContent = textos[acao].ok;

  try {
    const r = await App.api('/reservas/' + id);
    document.getElementById('r-nome').textContent = r.nome;
    document.getElementById('r-saldo').textContent = App.moeda(r.saldoAtual);

    if (acao === 'alocar') {
      // O teto de quem aloca é o saldo disponível — mostrado para orientar antes do erro.
      const d = await App.api('/dashboard');
      document.getElementById('r-disponivel').textContent = App.moeda(d.saldoDisponivel);
      document.getElementById('linha-disponivel').hidden = false;
    }

    document.getElementById('subtitulo').textContent = textos[acao].sub;
    document.getElementById('form').hidden = false;
  } catch (e) {
    document.getElementById('subtitulo').textContent = e.message;
  }
})().catch(e => App.toast(e.message));

document.getElementById('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro');
  try {
    await App.api('/reservas/' + id + '/' + acao, { method: 'POST', body: {
      valor: Number(document.getElementById('valor').value)
    }});
    location.replace('/reservas/');
  } catch (e) { App.erroForm('#erro', e.message); }
});
