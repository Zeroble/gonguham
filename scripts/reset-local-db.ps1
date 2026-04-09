param(
    [switch]$NoBackendRestart
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [System.Text.Encoding]::UTF8

$root = Split-Path -Path $PSScriptRoot -Parent
$composeFile = Join-Path $root 'docker-compose.local.yml'
$backendDir = Join-Path $root 'backend'
$stdoutLog = Join-Path $backendDir 'bootrun-local.out.log'
$stderrLog = Join-Path $backendDir 'bootrun-local.err.log'
$pidFile = Join-Path $backendDir 'bootrun-local.pid'

function Get-BackendProcesses {
    Get-CimInstance Win32_Process | Where-Object {
        $commandLine = $_.CommandLine
        $commandLine -and (
            $commandLine -like "*$backendDir*bootRun*" -or
            $commandLine -like '*com.gonguham.backend.BackendApplicationKt*' -or
            $commandLine -like '*gradle-wrapper.jar* bootRun*'
        )
    }
}

function Stop-BackendProcesses {
    $targets = @(Get-BackendProcesses)
    if (-not $targets.Count) {
        Write-Host '[1/5] 중지할 백엔드 프로세스가 없습니다.'
        return
    }

    Write-Host "[1/5] 백엔드 프로세스를 종료합니다: $($targets.ProcessId -join ', ')"
    foreach ($target in $targets) {
        Stop-Process -Id $target.ProcessId -Force -ErrorAction SilentlyContinue
    }

    Start-Sleep -Seconds 2
}

function Wait-ForPostgresHealthy {
    $deadline = (Get-Date).AddMinutes(2)

    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 3
        $raw = docker compose -f $composeFile ps --format json
        if (-not $raw) {
            continue
        }

        $rows = @($raw | ConvertFrom-Json)
        if ($rows.Count -and $rows[0].Health -eq 'healthy') {
            return
        }
    }

    throw 'PostgreSQL 컨테이너가 healthy 상태가 되지 않았습니다.'
}

function Start-Backend {
    Remove-Item $stdoutLog, $stderrLog, $pidFile -ErrorAction SilentlyContinue

    Write-Host '[4/5] 백엔드를 다시 실행합니다.'
    $process = Start-Process `
        -FilePath (Join-Path $backendDir 'gradlew.bat') `
        -ArgumentList 'bootRun' `
        -WorkingDirectory $backendDir `
        -RedirectStandardOutput $stdoutLog `
        -RedirectStandardError $stderrLog `
        -PassThru

    $process.Id | Set-Content -Path $pidFile

    $deadline = (Get-Date).AddMinutes(3)
    while ((Get-Date) -lt $deadline) {
        Start-Sleep -Seconds 5
        $statusCode = curl.exe -s -o NUL -w '%{http_code}' http://localhost:8080/api/v1/health
        if ($statusCode -eq '200') {
            return
        }
    }

    Write-Host '백엔드 부팅 로그:'
    if (Test-Path $stdoutLog) {
        Get-Content $stdoutLog -Tail 120
    }
    if (Test-Path $stderrLog) {
        Get-Content $stderrLog -Tail 120
    }

    throw '백엔드가 제한 시간 안에 기동되지 않았습니다.'
}

function Show-SeedSnapshot {
    Write-Host '[5/5] 초기화 결과를 확인합니다.'
    docker exec gonguham-postgres `
        env PGPASSWORD=gonguham PGCLIENTENCODING=UTF8 `
        psql -U gonguham -d gonguham `
        -c "select count(*) as users from users;" `
        -c "select count(*) as studies from studies;" `
        -c "select count(*) as posts from posts;" `
        -c "select id, nickname, email from users order by id limit 10;" `
        -c "select id, title from studies order by id limit 12;"
}

Write-Host '공구함 로컬 DB를 초기화합니다.'
Stop-BackendProcesses

Write-Host '[2/5] PostgreSQL 컨테이너와 볼륨을 제거합니다.'
docker compose -f $composeFile down -v | Out-Null

Write-Host '[3/5] PostgreSQL 컨테이너를 새 볼륨으로 다시 시작합니다.'
docker compose -f $composeFile up -d | Out-Null
Wait-ForPostgresHealthy

if (-not $NoBackendRestart) {
    Start-Backend
} else {
    Write-Host '[4/5] 요청에 따라 백엔드 재시작은 건너뜁니다.'
}

Show-SeedSnapshot
Write-Host '초기화가 완료되었습니다.'
if (-not $NoBackendRestart) {
    Write-Host '백엔드는 현재 http://localhost:8080 에서 실행 중입니다.'
}
