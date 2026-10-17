# Sobe o backend do FinCLI em modo desenvolvimento.
#
# Uso:  .\run-dev.ps1        (na pasta backend\)
# Para: Ctrl+C
#
# As credenciais ficam em backend\.env.local (fora do git). Se o arquivo nao
# existir, o script mostra o modelo e para.

$ErrorActionPreference = 'Stop'
$raiz = Split-Path -Parent $MyInvocation.MyCommand.Path
$porta = 8080

# ── 1. Credenciais ───────────────────────────────────────────────────────────

$envFile = Join-Path $raiz '.env.local'
if (-not (Test-Path $envFile)) {
  Write-Host 'Arquivo backend\.env.local nao encontrado. Crie-o com este conteudo:' -ForegroundColor Yellow
  Write-Host ''
  Write-Host '  FINCLI_DB_USER=fincli_app'
  Write-Host '  FINCLI_DB_PASSWORD=<senha do login fincli_app>'
  Write-Host '  FINCLI_COOKIE_SECURE=false'
  Write-Host ''
  exit 1
}

Get-Content $envFile | ForEach-Object {
  if ($_ -match '^\s*([^#=]+)=(.*)$') {
    [System.Environment]::SetEnvironmentVariable($Matches[1].Trim(), $Matches[2].Trim())
  }
}

# ── 2. Instancia anterior ────────────────────────────────────────────────────
# Precisa morrer ANTES do build: no Windows o JAR em uso nao pode ser renomeado,
# e o repackage do Spring Boot falha no meio, deixando um JAR sem Main-Class.

$emUso = Get-NetTCPConnection -LocalPort $porta -State Listen -ErrorAction SilentlyContinue
if ($emUso) {
  $pidAntigo = ($emUso | Select-Object -First 1).OwningProcess
  $proc = Get-Process -Id $pidAntigo -ErrorAction SilentlyContinue
  Write-Host "Porta $porta ocupada pelo PID $pidAntigo ($($proc.ProcessName)). Encerrando..." -ForegroundColor Yellow
  Stop-Process -Id $pidAntigo -Force
  Start-Sleep -Milliseconds 1200
}

# ── 3. Build ─────────────────────────────────────────────────────────────────

Push-Location $raiz
try {
  Write-Host 'Compilando...' -ForegroundColor Cyan
  mvn -q package -DskipTests
  if ($LASTEXITCODE -ne 0) { throw "mvn package falhou (codigo $LASTEXITCODE)." }
} finally {
  Pop-Location
}

$jar = Join-Path $raiz 'target\fincli-backend-1.0.0.jar'
if (-not (Test-Path $jar)) { throw "JAR nao encontrado em $jar." }

# Um repackage interrompido deixa um JAR comum, sem loader — que falha com
# "no main manifest attribute" so na hora de rodar. Melhor detectar aqui.
Add-Type -AssemblyName System.IO.Compression.FileSystem
$zip = [System.IO.Compression.ZipFile]::OpenRead($jar)
try {
  $temLoader = $null -ne ($zip.Entries | Where-Object { $_.FullName -like 'BOOT-INF/*' } | Select-Object -First 1)
} finally {
  $zip.Dispose()
}
if (-not $temLoader) {
  throw "JAR sem BOOT-INF (repackage incompleto). Rode: mvn clean package -DskipTests"
}

# ── 4. Sobe ──────────────────────────────────────────────────────────────────

Write-Host ''
Write-Host "FinCLI no ar: http://localhost:$porta" -ForegroundColor Green
Write-Host 'Ctrl+C para parar.'
Write-Host ''

# java -jar, e nao mvn spring-boot:run: o acento no caminho do repo corrompe o
# classpath do processo filho que o plugin cria (ver README).
java -jar $jar
