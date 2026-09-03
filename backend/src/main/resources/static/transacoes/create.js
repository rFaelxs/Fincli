/* ═══ Transações · criar ═══ */
'use strict';

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'transacoes');
  document.getElementById('data').valueAsDate = new Date();
})().catch(e => App.toast(e.message));

document.getElementById('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro');
  try {
    await App.api('/transacoes', { method: 'POST', body: {
      valor: Number(document.getElementById('valor').value),
      categoria: document.getElementById('categoria').value,
      descricao: document.getElementById('descricao').value || null,
      data: document.getElementById('data').value,
      tipo: document.getElementById('tipo').value,
      essencial: document.getElementById('essencial').checked
    }});
    location.replace('/transacoes/');
  } catch (e) { App.erroForm('#erro', e.message); }
});
