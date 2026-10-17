/* ═══ Reservas · criar ═══ */
'use strict';

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'reservas');
})().catch(e => App.toast(e.message));

document.getElementById('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro');
  try {
    await App.api('/reservas', { method: 'POST', body: {
      nome: document.getElementById('nome').value,
      metaValor: Number(document.getElementById('meta').value)
    }});
    location.replace('/reservas/');
  } catch (e) { App.erroForm('#erro', e.message); }
});
