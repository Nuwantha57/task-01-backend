# Adyen Configuration Environment Variables
# Update these values with your actual Adyen credentials
# Then run this script in PowerShell: .\set-adyen-env.ps1

# Adyen API Key (Get from Adyen Customer Area > Developers > API credentials)
$env:ADYEN_API_KEY = "AQEzhmfuXNWTK0Qc+iSVi2EopvyyfKhCA4BfVHFfyH+biHJnisMK6UPdR9iOOvwG74f+M92SEMFdWw2+5HzctViMSCJMYAc=-YOm3CG45vOtw0bIkEH/GLurae26N/jydFqTRbfRYTm4=-i1i2w?F=azM6~f:deyQ"

# Adyen Client Key (Get from Adyen Customer Area > Developers > API credentials)
$env:ADYEN_CLIENT_KEY = "test_LNC4YR25OFEFHB6UMF5XVEFB3YTLYUZF"

# Adyen Merchant Account (Your merchant account name)
$env:ADYEN_MERCHANT_ACCOUNT = "EyepaxITConsultingPvtLtdECOM"

# Adyen Environment (test or live)
$env:ADYEN_ENVIRONMENT = "test"

Write-Host "Adyen environment variables have been set for this PowerShell session!" -ForegroundColor Green
Write-Host ""
Write-Host "Environment Variables:"
Write-Host "  ADYEN_API_KEY: $($env:ADYEN_API_KEY.Substring(0, [Math]::Min(10, $env:ADYEN_API_KEY.Length)))..." -ForegroundColor Cyan
Write-Host "  ADYEN_CLIENT_KEY: $($env:ADYEN_CLIENT_KEY.Substring(0, [Math]::Min(10, $env:ADYEN_CLIENT_KEY.Length)))..." -ForegroundColor Cyan
Write-Host "  ADYEN_MERCHANT_ACCOUNT: $env:ADYEN_MERCHANT_ACCOUNT" -ForegroundColor Cyan
Write-Host "  ADYEN_ENVIRONMENT: $env:ADYEN_ENVIRONMENT" -ForegroundColor Cyan
Write-Host ""
Write-Host "Note: These variables are only set for the current PowerShell session." -ForegroundColor Yellow
Write-Host "You need to run this script again if you open a new terminal." -ForegroundColor Yellow
Write-Host ""
Write-Host "Now you can run: mvn spring-boot:run" -ForegroundColor Green
