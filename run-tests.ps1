# Run Tests with Coverage Script
Write-Host "================================" -ForegroundColor Cyan
Write-Host "TGMS Auth Service - Test Runner" -ForegroundColor Cyan
Write-Host "================================`n" -ForegroundColor Cyan

# Setup test database
Write-Host "Step 1: Setting up test database..." -ForegroundColor Yellow
.\setup-test-db.ps1

Write-Host "`nStep 2: Running tests with code coverage..." -ForegroundColor Yellow

# Run tests
./mvnw clean test

if ($LASTEXITCODE -eq 0) {
    Write-Host "`n================================" -ForegroundColor Green
    Write-Host "✓ ALL TESTS PASSED" -ForegroundColor Green
    Write-Host "================================`n" -ForegroundColor Green
    
    # Generate coverage report
    Write-Host "Step 3: Generating coverage report..." -ForegroundColor Yellow
    ./mvnw jacoco:report
    
    # Check if coverage report exists
    $coverageReport = "target\site\jacoco\index.html"
    if (Test-Path $coverageReport) {
        Write-Host "`nCode coverage report generated!" -ForegroundColor Green
        Write-Host "Location: $coverageReport" -ForegroundColor Cyan
        
        # Open coverage report in browser
        $openReport = Read-Host "`nOpen coverage report in browser? (Y/N)"
        if ($openReport -eq 'Y' -or $openReport -eq 'y') {
            Start-Process $coverageReport
        }
    }
    
    # Display summary
    Write-Host "`n================================" -ForegroundColor Cyan
    Write-Host "Test Summary" -ForegroundColor Cyan
    Write-Host "================================" -ForegroundColor Cyan
    Write-Host "View detailed results in target/surefire-reports/" -ForegroundColor White
    Write-Host "View coverage report in target/site/jacoco/index.html" -ForegroundColor White
    
} else {
    Write-Host "`n================================" -ForegroundColor Red
    Write-Host "✗ TESTS FAILED" -ForegroundColor Red
    Write-Host "================================`n" -ForegroundColor Red
    Write-Host "Check the output above for details" -ForegroundColor Yellow
    Write-Host "Test reports: target/surefire-reports/" -ForegroundColor Yellow
    exit 1
}
