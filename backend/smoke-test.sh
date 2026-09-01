#!/usr/bin/env bash
# Teste de fumaça da API do FinCLI.
# Idempotente: gera um CPF valido aleatorio por execucao, entao cada rodada usa
# um usuario novo e as assercoes de valores absolutos permanecem exatas.
API="http://localhost:8080/api"
A=/tmp/fincli_a.jar   # cookie jar do usuario A
B=/tmp/fincli_b.jar   # cookie jar do usuario B
rm -f "$A" "$B"

# Gera um CPF valido: 9 digitos aleatorios + 2 digitos verificadores (modulo 11).
gerar_cpf() {
  local d=() soma resto dv1 dv2 i
  for i in $(seq 0 8); do d[i]=$((RANDOM % 10)); done
  soma=0; for i in $(seq 0 8); do soma=$((soma + d[i] * (10 - i))); done
  resto=$((soma % 11)); dv1=$(( resto < 2 ? 0 : 11 - resto ))
  soma=0; for i in $(seq 0 8); do soma=$((soma + d[i] * (11 - i))); done
  soma=$((soma + dv1 * 2))
  resto=$((soma % 11)); dv2=$(( resto < 2 ? 0 : 11 - resto ))
  printf "%s%s%s%s%s%s%s%s%s%s%s" "${d[@]}" "$dv1" "$dv2"
}

CPF_A=$(gerar_cpf)
CPF_B=$(gerar_cpf)
[ "$CPF_A" = "$CPF_B" ] && CPF_B=$(gerar_cpf)
echo "usuario A: CPF $CPF_A · usuario B: CPF $CPF_B"

csrf() { grep XSRF-TOKEN "$1" | awk '{print $7}' | tail -1; }

req() { # req <jar> <metodo> <caminho> [json]
  local jar=$1 metodo=$2 caminho=$3 corpo=${4:-}
  curl -s -o /tmp/body.txt -w "%{http_code}" -b "$jar" -c "$jar" \
    -X "$metodo" "$API$caminho" \
    -H "Content-Type: application/json" \
    -H "X-XSRF-TOKEN: $(csrf "$jar")" \
    ${corpo:+-d "$corpo"}
}

ok() { printf "  \033[32mOK\033[0m   %s\n" "$1"; }
bad() { printf "  \033[31mFALHOU\033[0m %s (esperado %s, veio %s)\n" "$1" "$2" "$3"; FALHAS=$((FALHAS+1)); }
check() { [ "$1" = "$2" ] && ok "$3" || bad "$3" "$2" "$1"; }
FALHAS=0

echo "== sessao =="
curl -s -c "$A" "$API/csrf" > /dev/null; ok "cookie XSRF-TOKEN emitido"

code=$(req "$A" POST /cadastro '{"nome":"Rafael Teste","cpf":"'$CPF_A'","senha":"senhaforte1"}')
check "$code" 201 "cadastro cria usuario"

code=$(req "$A" POST /cadastro '{"nome":"Outro","cpf":"'$CPF_A'","senha":"senhaforte1"}')
check "$code" 409 "CPF duplicado e recusado"

code=$(req "$A" POST /cadastro '{"nome":"X","cpf":"123.456.789-00","senha":"senhaforte1"}')
check "$code" 400 "CPF com digito verificador invalido e recusado"

code=$(req "$A" GET /me)
check "$code" 401 "sem login, /me nega"

code=$(req "$A" POST /login '{"cpf":"'$CPF_A'","senha":"errada"}')
check "$code" 401 "senha errada nega"

code=$(req "$A" POST /login '{"cpf":"'$CPF_A'","senha":"senhaforte1"}')
check "$code" 200 "login autentica"

code=$(req "$A" GET /me); check "$code" 200 "/me devolve usuario da sessao"
echo "       corpo: $(cat /tmp/body.txt)"

echo
echo "== cenario que quebrava no CLI: alocar e depois lancar transacao =="
req "$A" POST /transacoes '{"valor":1000.00,"categoria":"Salario","descricao":"Salario","data":"2026-08-01","tipo":"ENTRADA","essencial":false}' > /dev/null
ok "entrada de R$ 1000 registrada"

req "$A" GET /reservas > /dev/null
RES_ID=$(grep -o '"id":"[^"]*"' /tmp/body.txt | head -1 | cut -d'"' -f4)
ok "reserva de emergencia criada no cadastro (id $RES_ID)"

code=$(req "$A" POST "/reservas/$RES_ID/alocar" '{"valor":500.00}')
check "$code" 200 "alocacao de R$ 500 aceita"

req "$A" POST /transacoes '{"valor":50.00,"categoria":"Mercado","descricao":"Compra","data":"2026-08-02","tipo":"SAIDA","essencial":true}' > /dev/null
ok "saida de R$ 50 registrada DEPOIS da alocacao"

req "$A" GET /dashboard > /dev/null
echo "       dashboard: $(cat /tmp/body.txt)"
SALDO=$(grep -o '"saldoDisponivel":[0-9.]*' /tmp/body.txt | cut -d: -f2)
SALDO_RES=$(grep -o '"saldoAtual":[0-9.]*' /tmp/body.txt | head -1 | cut -d: -f2)
check "$SALDO" "450.00" "saldo disponivel = 1000 - 50 - 500"
check "$SALDO_RES" "500.00" "alocacao SOBREVIVEU a transacao posterior"

echo
echo "== regras de negocio =="
code=$(req "$A" POST "/reservas/$RES_ID/alocar" '{"valor":99999.00}')
check "$code" 400 "alocacao acima do saldo e recusada"

code=$(req "$A" POST "/reservas/$RES_ID/sacar" '{"valor":99999.00}')
check "$code" 400 "saque acima do saldo da reserva e recusado"

code=$(req "$A" DELETE "/reservas/$RES_ID")
check "$code" 409 "reserva de emergencia nao pode ser excluida"

code=$(req "$A" POST /transacoes '{"valor":-5,"categoria":"X","data":"2026-08-01","tipo":"SAIDA","essencial":false}')
check "$code" 400 "valor negativo e recusado"

echo
echo "== isolamento entre usuarios (IDOR) =="
req "$A" GET /transacoes > /dev/null
TX_ID=$(grep -o '"id":"[^"]*"' /tmp/body.txt | head -1 | cut -d'"' -f4)

curl -s -c "$B" "$API/csrf" > /dev/null
req "$B" POST /cadastro '{"nome":"Invasor","cpf":"'$CPF_B'","senha":"senhaforte2"}' > /dev/null
req "$B" POST /login '{"cpf":"'$CPF_B'","senha":"senhaforte2"}' > /dev/null
ok "usuario B autenticado"

code=$(req "$B" DELETE "/transacoes/$TX_ID")
check "$code" 404 "B nao consegue apagar transacao de A"

code=$(req "$B" POST "/reservas/$RES_ID/alocar" '{"valor":1.00}')
check "$code" 404 "B nao consegue alocar na reserva de A"

req "$B" GET /dashboard > /dev/null
SALDO_B=$(grep -o '"saldoDisponivel":[0-9.]*' /tmp/body.txt | cut -d: -f2)
check "$SALDO_B" "0.00" "B ve apenas os proprios dados"

echo
echo "== extrato e selic =="
req "$A" GET /extrato > /dev/null
echo "       extrato: $(cat /tmp/body.txt)"

req "$A" GET /dashboard > /dev/null
SELIC=$(grep -o '"selicMetaAnual":[0-9.]*' /tmp/body.txt | cut -d: -f2)
[ -n "$SELIC" ] && ok "meta Selic do BCB: $SELIC% a.a." || bad "selic" "valor" "nulo"

code=$(req "$A" POST /logout)
check "$code" 204 "logout encerra a sessao"
code=$(req "$A" GET /me)
check "$code" 401 "sessao invalidada apos logout"

echo
[ "$FALHAS" -eq 0 ] && echo "TODOS OS CHECKS PASSARAM" || echo "$FALHAS CHECK(S) FALHARAM"
exit "$FALHAS"
