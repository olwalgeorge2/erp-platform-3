# Test database connection from host
$env:PGPASSWORD = "erp_pass"

Write-Host "Testing connection to erp-postgres on localhost:5432..." -ForegroundColor Yellow

try {
    # Use a disposable postgres container so we do not need local tools installed
    docker run --rm --network host --env PGPASSWORD=$env:PGPASSWORD postgres:16-alpine `
        psql -h localhost -p 5432 -U erp_user -d erp_identity `
        -c "SELECT 'Connection successful!' AS status;"

    if ($LASTEXITCODE -eq 0) {
        Write-Host "[OK] Database connection successful!" -ForegroundColor Green
    } else {
        Write-Host "[FAIL] Connection failed with exit code $LASTEXITCODE" -ForegroundColor Red
    }
} catch {
    Write-Host "[ERROR] $_" -ForegroundColor Red
}
