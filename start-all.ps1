<#
.SYNOPSIS
    Starts every DPDMS service in the correct order, each in its own window.

.DESCRIPTION
    Run from the project root:   .\start-all.ps1
    If the jars are missing it builds them first (skipping tests).
    Waits for discovery-service and the gateway to answer before moving on,
    so you never get the "503 from the gateway" confusion.

.PARAMETER Build
    Force a rebuild even if the jars already exist.

.PARAMETER SkipUi
    Start the backend only (useful when you want to run web-ui from the IDE).

.EXAMPLE
    .\start-all.ps1
    .\start-all.ps1 -Build
#>
param(
    [switch]$Build,
    [switch]$SkipUi
)

$ErrorActionPreference = 'Stop'
$root = $PSScriptRoot
Set-Location $root

# Module name, port, and how long to pause before starting the next one.
$services = @(
    @{ Name = 'discovery-service';        Port = 8761; Wait = 25 },
    @{ Name = 'gateway';                  Port = 8080; Wait = 15 },
    @{ Name = 'auth-service';             Port = 8081; Wait = 12 },
    @{ Name = 'flood-service';            Port = 8082; Wait = 6  },
    @{ Name = 'drought-service';          Port = 8083; Wait = 6  },
    @{ Name = 'fire-service';             Port = 8084; Wait = 6  },
    @{ Name = 'zoonotic-disease-service'; Port = 8085; Wait = 6  },
    @{ Name = 'mining-accident-service';  Port = 8086; Wait = 6  },
    @{ Name = 'alert-service';            Port = 8087; Wait = 6  },
    @{ Name = 'report-service';           Port = 8088; Wait = 6  },
    @{ Name = 'dashboard-service';        Port = 8089; Wait = 6  },
    @{ Name = 'web-ui';                   Port = 8090; Wait = 0  }
)
if ($SkipUi) { $services = $services | Where-Object { $_.Name -ne 'web-ui' } }

function Write-Step($message) { Write-Host "`n==> $message" -ForegroundColor Cyan }
function Write-Warn($message) { Write-Host "    $message" -ForegroundColor Yellow }

# ---------- pre-flight checks ----------
Write-Step 'Pre-flight checks'

if (-not (Test-Path (Join-Path $root '.env'))) {
    Write-Warn 'No .env file found. Copying .env.example -> .env'
    Copy-Item (Join-Path $root '.env.example') (Join-Path $root '.env')
    Write-Host ''
    Write-Host '    Open .env now and set JWT_SECRET, INTERNAL_API_KEY and DB_PASSWORD,' -ForegroundColor Red
    Write-Host '    then run this script again.' -ForegroundColor Red
    exit 1
}

$javaVersion = (cmd /c "java -version 2>&1" | Select-Object -First 1)
Write-Host "    Java: $javaVersion"

# ---------- build if needed ----------
$jarFor = {
    param($module)
    Get-ChildItem -Path (Join-Path $root "$module\target") -Filter '*.jar' -ErrorAction SilentlyContinue |
        Where-Object { $_.Name -notlike '*-sources.jar' -and $_.Name -notlike '*.original' } |
        Select-Object -First 1
}

$missing = $services | Where-Object { -not (& $jarFor $_.Name) }
if ($Build -or $missing) {
    if ($missing) { Write-Warn ("Missing jars for: " + ($missing.Name -join ', ')) }
    Write-Step 'Building (mvn clean install -DskipTests)'
    & mvn clean install -DskipTests
    if ($LASTEXITCODE -ne 0) { Write-Host 'Build failed. Fix the errors above and retry.' -ForegroundColor Red; exit 1 }
}

# ---------- start ----------
foreach ($service in $services) {
    $jar = & $jarFor $service.Name
    if (-not $jar) { Write-Host "No jar for $($service.Name); skipping." -ForegroundColor Red; continue }

    Write-Step "Starting $($service.Name) on port $($service.Port)"

    Start-Process -FilePath 'cmd.exe' `
        -ArgumentList '/c', "title DPDMS $($service.Name) && java -jar `"$($jar.FullName)`"" `
        -WorkingDirectory $root

    if ($service.Wait -gt 0) {
        Write-Host "    waiting $($service.Wait)s..." -NoNewline
        Start-Sleep -Seconds $service.Wait
        Write-Host ' done'
    }
}

Write-Step 'All services launched'
Write-Host ''
Write-Host '    Front end      http://localhost:8090'
Write-Host '    API gateway    http://localhost:8080'
Write-Host '    Swagger UI     http://localhost:8080/swagger-ui.html'
Write-Host '    Eureka         http://localhost:8761'
Write-Host '    RabbitMQ       http://localhost:15672  (guest/guest)'
Write-Host '    Mailpit inbox  http://localhost:8025'
Write-Host ''
Write-Warn 'Give Eureka ~30 more seconds before the gateway stops returning 503.'
Write-Warn 'If localhost does not resolve, use 127.0.0.1 instead.'
Write-Host ''
Write-Host '    Stop everything with:  .\stop-all.ps1' -ForegroundColor Cyan
