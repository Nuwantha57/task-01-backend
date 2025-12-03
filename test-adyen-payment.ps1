# Test script for Adyen Payment API
$body = @{
    amount = @{
        value = 1000
        currency = "EUR"
    }
    reference = "ORDER-TEST-123"
    returnUrl = "https://your-company.com/payment/result"
    paymentMethod = @{
        type = "scheme"
        number = "4111111111111111"
        expiryMonth = "03"
        expiryYear = "2030"
        holderName = "Test User"
        cvc = "737"
    }
    shopperEmail = "test@example.com"
} | ConvertTo-Json -Depth 10

Write-Host "Testing Adyen Payment Initiation Endpoint..." -ForegroundColor Cyan
Write-Host "URL: http://localhost:8080/api/payments/adyen/initiate" -ForegroundColor Yellow
Write-Host ""

try {
    $response = Invoke-WebRequest -Uri "http://localhost:8080/api/payments/adyen/initiate" `
        -Method POST `
        -ContentType "application/json" `
        -Body $body `
        -UseBasicParsing

    Write-Host "Response Status: $($response.StatusCode)" -ForegroundColor Green
    Write-Host "Response Body:" -ForegroundColor Green
    $response.Content | ConvertFrom-Json | ConvertTo-Json -Depth 10
} catch {
    Write-Host "Error occurred:" -ForegroundColor Red
    Write-Host "Status Code: $($_.Exception.Response.StatusCode.value__)" -ForegroundColor Red
    Write-Host "Error Message:" -ForegroundColor Red
    $_.Exception.Message
    if ($_.Exception.Response) {
        $reader = New-Object System.IO.StreamReader($_.Exception.Response.GetResponseStream())
        $reader.BaseStream.Position = 0
        $reader.DiscardBufferedData()
        $responseBody = $reader.ReadToEnd()
        Write-Host "Response Body:" -ForegroundColor Red
        $responseBody
    }
}
