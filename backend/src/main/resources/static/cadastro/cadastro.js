/* ═══ Cadastro ═══ */
'use strict';

App.initPublico();

document.getElementById('form-cadastro').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro-cadastro');
  const cpf = document.getElementById('cpf').value;
  const senha = document.getElementById('senha').value;
  try {
    await App.api('/cadastro', {
      method: 'POST', silencioso: true,
      body: { nome: document.getElementById('nome').value, cpf, senha }
    });
    await App.api('/login', { method: 'POST', silencioso: true, body: { cpf, senha } });
    location.replace('/dashboard/');
  } catch (e) {
    App.erroForm('#erro-cadastro', e.message);
  }
});
