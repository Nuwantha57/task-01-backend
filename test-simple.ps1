# Simple test for Adyen Payment API
Write-Host "Testing Adyen Payment Initiate API" -ForegroundColor Cyan
Write-Host ""

$body = @{
    amount = @{
        currency = "USD"
        value = 1000
    }
    reference = "TEST-ORDER-$(Get-Date -Format 'yyyyMMddHHmmss')"
    returnUrl = "http://localhost:3000/payment/result"
    paymentMethod = @{
        type = "scheme"
        encryptedCardNumber = "test_4111111111111111"
        encryptedExpiryMonth = "test_03"
        encryptedExpiryYear = "test_2030"
        encryptedSecurityCode = "test_737"
    }
    shopperReference = "test-user-123"
    shopperEmail = "test@example.com"
}

$json = $body | ConvertTo-Json -Depth 10
Write-Host "Request Body:" -ForegroundColor Yellow
Write-Host $json
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri "http://localhost:8080/api/payments/adyen/initiate" -Method Post -Body $json -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "SUCCESS!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host
    
} catch {
    Write-Host "ERROR!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Status: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Message: $($_.Exception.Message)" -ForegroundColor Red
    
    if ($_.ErrorDetails.Message) {
        Write-Host ""
        Write-Host "Error Details:" -ForegroundColor Yellow
        Write-Host $_.ErrorDetails.Message
    }
}
