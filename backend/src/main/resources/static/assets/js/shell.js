/* ═══ FinCLI · sidebar compartilhada das páginas autenticadas ═══ */
'use strict';

/**
 * Monta a sidebar no elemento #sidebar.
 *
 * O design do redesign tem só "Hoje" e "Relatório". Transações, Reservas e Extrato continuam
 * na lista porque continuam existindo: tirá-las deixaria páginas inalcançáveis.
 *
 * @param eu    usuário da sessão ({ nome })
 * @param ativa página ativa: hoje | relatorio | transacoes | reservas | extrato
 */
App.montarShell = function (eu, ativa) {
  const itens = [
    { id: 'hoje',       rotulo: 'Hoje' },
    { id: 'relatorio',  rotulo: 'Relatório' },
    { id: 'transacoes', rotulo: 'Transações' },
    { id: 'reservas',   rotulo: 'Reservas' },
    { id: 'extrato',    rotulo: 'Extrato' }
  ];

  const iniciais = eu.nome.trim().split(/\s+/).slice(0, 2).map(p => p[0]).join('').toUpperCase();
  const mesAtual = new Date().toLocaleDateString('pt-BR', { month: 'long', year: 'numeric' });

  document.getElementById('sidebar').innerHTML = `
    <a class="marca" href="/hoje/" aria-label="FinCLI, ir para Hoje">
      <img src="/assets/img/fincli-logo.png" alt="FinCLI">
    </a>

    <nav aria-label="Seções">
      ${itens.map(i => `
        <a href="/${i.id}/" ${i.id === ativa ? 'aria-current="page"' : ''}>
          <span class="ponto" aria-hidden="true"></span>${i.rotulo}
        </a>`).join('')}
    </nav>

    <div class="resumo">
      <span class="rot">GUARDADO</span>
      <span class="val esqueleto" id="side-guardado">R$ 0,00</span>
      <span class="leg" id="side-guardado-leg">—</span>
    </div>

    <div class="side-foot">
      <div class="user">
        <div class="avatar" aria-hidden="true">${App.esc(iniciais)}</div>
        <div style="min-width:0">
          <div class="who">${App.esc(eu.nome)}</div>
          <div class="quando">${App.esc(mesAtual)}</div>
        </div>
      </div>
      <button class="btn ghost" id="btn-sair">Sair</button>
    </div>`;

  document.getElementById('btn-sair').addEventListener('click', async () => {
    try { await App.api('/logout', { method: 'POST' }); } catch (e) { /* sessão já encerrada */ }
    location.replace('/login/');
  });

  App.atalhosDeNavegacao();
  App.carregarGuardado();
};

/**
 * Preenche o card "GUARDADO" da sidebar.
 *
 * Falha em silêncio: é informação de apoio, e derrubar a tela inteira porque um número lateral
 * não carregou seria pior do que deixá-lo vazio.
 */
App.carregarGuardado = async function () {
  const val = document.getElementById('side-guardado');
  const leg = document.getElementById('side-guardado-leg');
  if (!val) return;

  try {
    const reservas = await App.api('/reservas');
    const total = reservas.reduce((s, r) => s + Number(r.saldoAtual), 0);
    const emergencia = reservas.find(r => r.emergencia);

    val.textContent = App.moeda(total);
    val.classList.remove('esqueleto');

    // O percentual da emergência só aparece se houver meta: "emergência 0,0%" numa reserva
    // sem meta parece fracasso, quando na verdade não há nada a medir.
    const temMeta = emergencia && Number(emergencia.metaValor) > 0;
    leg.textContent = reservas.length
      ? `em ${reservas.length} ${reservas.length === 1 ? 'reserva' : 'reservas'}`
        + (temMeta ? ` · emergência ${App.pct(emergencia.progresso)}` : '')
      : 'nenhuma reserva ainda';
  } catch (e) {
    val.textContent = '—';
    val.classList.remove('esqueleto');
    leg.textContent = 'não consegui carregar';
  }
};

/**
 * Atalhos de teclado: H para Hoje, R para Relatório.
 *
 * Ignorados enquanto o foco está num campo de texto — senão digitar "hoje" na barra de comando
 * navegaria para outra tela.
 */
App.atalhosDeNavegacao = function () {
  document.addEventListener('keydown', e => {
    if (e.ctrlKey || e.metaKey || e.altKey || App.digitando(e.target)) return;
    const destino = { h: '/hoje/', r: '/relatorio/' }[e.key.toLowerCase()];
    if (destino && !location.pathname.startsWith(destino)) {
      location.assign(destino);
    }
  });
};

/** @return true se o alvo do evento aceita texto */
App.digitando = function (alvo) {
  if (!alvo) return false;
  const tag = alvo.tagName;
  return tag === 'INPUT' || tag === 'TEXTAREA' || tag === 'SELECT' || alvo.isContentEditable;
};
