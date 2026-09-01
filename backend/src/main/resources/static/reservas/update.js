/* ═══ Reservas · editar meta ═══ */
'use strict';

const id = App.qs('id');

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'reservas');

  if (!id) { location.replace('/reservas/'); return; }

  try {
    const r = await App.api('/reservas/' + id);
    document.getElementById('r-nome').textContent = r.nome;
    document.getElementById('r-saldo').textContent = App.moeda(r.saldoAtual);
    document.getElementById('meta').value = r.metaValor;
    document.getElementById('subtitulo').textContent = 'Defina o novo valor-alvo.';
    document.getElementById('form').hidden = false;
  } catch (e) {
    document.getElementById('subtitulo').textContent = e.message;
  }
})().catch(e => App.toast(e.message));

document.getElementById('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro');
  try {
    await App.api('/reservas/' + id + '/meta', { method: 'PUT', body: {
      valor: Number(document.getElementById('meta').value)
    }});
    location.replace('/reservas/');
  } catch (e) { App.erroForm('#erro', e.message); }
});
