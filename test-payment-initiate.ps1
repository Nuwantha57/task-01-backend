# Test script for Adyen Payment Initiation API
# This script tests the /api/payments/adyen/initiate endpoint

$baseUrl = "http://localhost:8080"
$endpoint = "$baseUrl/api/payments/adyen/initiate"

# Test payment request payload
$payload = @{
    amount = @{
        currency = "USD"
        value = 1000  # 10.00 USD (in minor units)
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
} | ConvertTo-Json -Depth 10

Write-Host "Testing Adyen Payment Initiation API" -ForegroundColor Cyan
Write-Host "Endpoint: $endpoint" -ForegroundColor Yellow
Write-Host ""
Write-Host "Request Payload:" -ForegroundColor Green
Write-Host $payload
Write-Host ""

try {
    $response = Invoke-RestMethod -Uri $endpoint -Method Post -Body $payload -ContentType "application/json" -ErrorAction Stop
    
    Write-Host "✓ Success!" -ForegroundColor Green
    Write-Host ""
    Write-Host "Response:" -ForegroundColor Green
    $response | ConvertTo-Json -Depth 10 | Write-Host
    
} catch {
    Write-Host "✗ Error!" -ForegroundColor Red
    Write-Host ""
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Status Description: $($_.Exception.Response.StatusDescription)" -ForegroundColor Red
    Write-Host ""
    
    if ($_.ErrorDetails.Message) {
        Write-Host "Error Details:" -ForegroundColor Red
        Write-Host $_.ErrorDetails.Message
    }
    
    Write-Host ""
    Write-Host "Full Exception:" -ForegroundColor Yellow
    Write-Host $_.Exception.Message
}
