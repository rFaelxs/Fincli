/* ═══ Login ═══ */
'use strict';

App.initPublico();

document.getElementById('form-login').addEventListener('submit', async ev => {
  ev.preventDefault();
  App.limparErro('#erro-login');
  try {
    await App.api('/login', {
      method: 'POST', silencioso: true,
      body: {
        cpf: document.getElementById('cpf').value,
        senha: document.getElementById('senha').value
      }
    });
    location.replace('/hoje/');
  } catch (e) {
    App.erroForm('#erro-login', e.status === 401 ? 'CPF ou senha inválidos.' : e.message);
  }
});
