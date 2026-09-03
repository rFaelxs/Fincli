/* ═══ Transações · editar ═══ */
'use strict';

const id = App.qs('id');

(async () => {
  const eu = await App.guardar();
  App.montarShell(eu, 'transacoes');

  if (!id) { location.replace('/transacoes/'); return; }

  try {
    const t = await App.api('/transacoes/' + id);
    document.getElementById('descricao').value = t.descricao || '';
    document.getElementById('categoria').value = t.categoria;
    document.getElementById('valor').value = t.valor;
    document.getElementById('data').value = t.data;
    document.getElementById('tipo').value = t.tipo;
    document.getElementById('essencial').checked = t.essencial;
    document.getElementById('subtitulo').textContent = 'Altere os campos e salve.';
    document.getElementById('form').hidden = false;
  } catch (e) {
    document.getElementById('subtitulo').textContent = e.message;
  }
})().catch(e => App.toast(e.message));

document.getElementById('form').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro');
  try {
    await App.api('/transacoes/' + id, { method: 'PUT', body: {
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
