<#
.SYNOPSIS
    Stops every DPDMS service by freeing the ports it listens on.

.DESCRIPTION
    Run from the project root:   .\stop-all.ps1
    Only kills processes that own a DPDMS port (8080-8090, 8761), so it will
    not touch unrelated Java programs you have running.
#>

$ports = 8080, 8081, 8082, 8083, 8084, 8085, 8086, 8087, 8088, 8089, 8090, 8761
$stopped = 0

foreach ($port in $ports) {
    $connections = Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue
    foreach ($connection in $connections) {
        $process = Get-Process -Id $connection.OwningProcess -ErrorAction SilentlyContinue
        if ($process) {
            Write-Host "Stopping $($process.ProcessName) (PID $($process.Id)) on port $port" -ForegroundColor Yellow
            Stop-Process -Id $process.Id -Force -ErrorAction SilentlyContinue
            $stopped++
        }
    }
}

if ($stopped -eq 0) {
    Write-Host 'Nothing was listening on the DPDMS ports.' -ForegroundColor Green
} else {
    Write-Host "`nStopped $stopped process(es)." -ForegroundColor Green
}

Write-Host 'RabbitMQ and Mailpit are Docker containers - stop them with: docker compose down'
