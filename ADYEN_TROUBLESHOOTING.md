# Adyen API Key Troubleshooting Guide

## Current Error

**Status**: 401 Unauthorized  
**Error Code**: 000  
**Error Type**: security  
**Message**: HTTP Status Response - Unauthorized

## Root Cause Analysis

The Adyen API is rejecting your API key, which means authentication is failing. This indicates one of the following issues:

### 1. API Key Format Issues ❌

**Problem**: The API key might be corrupted or incorrectly formatted.

**How to Verify**:

- API keys should look like: `AQE...long_string...==` (Base64-like format)
- The key should have NO line breaks or spaces
- Length is typically 100-200 characters

**Solution**:

1. Log into [Adyen Test Customer Area](https://ca-test.adyen.com/)
2. Navigate to: **Developers** → **API credentials**
3. Select your API credential
4. Click **"Generate new API key"** button
5. Copy the ENTIRE key (select all, Ctrl+C)
6. Paste directly into `application-local.properties` on ONE line

### 2. API Key Status ❌

**Problem**: The API key might be inactive, expired, or revoked.

**How to Verify**:

- In Adyen Customer Area → API credentials
- Check if the credential status shows "Active" (green)
- Verify the creation date (recently created keys might take a minute to activate)

**Solution**:

- If inactive, activate it in Adyen Customer Area
- If old, generate a new API key

### 3. API Key Permissions ❌

**Problem**: The API key doesn't have required permissions for Checkout API.

**How to Verify** (In Adyen Customer Area → API credentials):

1. Click on your API credential
2. Scroll down to **"Permissions"** section
3. Check if these are enabled:
   - ✅ **Checkout encrypted** - REQUIRED
   - ✅ **Merchant Checkout** - REQUIRED
   - ✅ **Management API - Accounts read** (optional but recommended)

**Solution**:

1. Click "Edit" on the API credential
2. Enable the required permissions (checkboxes)
3. Click "Save changes"
4. Wait 1-2 minutes for changes to propagate

### 4. Merchant Account Configuration ❌

**Problem**: The merchant account name in config doesn't match your actual account.

**How to Verify**:

1. In Adyen Customer Area → **Developers** → **API credentials**
2. Click on your API credential
3. Look for **"Merchant accounts"** section
4. Note the EXACT merchant account name (case-sensitive)

**Current Config**: `EyepaxITConsultingPvtLtdECOM`

**Solution**:

- If the name is different, update `application-local.properties`:
  ```properties
  adyen.merchant-account=YourActualMerchantAccountName
  ```

### 5. Wrong API Key Type ❌

**Problem**: Using Client Key instead of API Key, or vice versa.

**Difference**:

- **API Key** (Server-side): Long string starting with `AQE...` - Use this for backend API calls
- **Client Key** (Client-side): Shorter string like `test_ABC123...` - Use for frontend Drop-in

**How to Verify**:

- Your current API key: `AQEzhmfuXNWTK0Qc+iSVi2Eo...` ✅ (Correct format)
- Your client key: `test_LNC4YR25OFEFHB6UMF5XVEFB3YTLYUZF` ✅ (Correct format)

**Status**: Format looks correct ✅

### 6. Environment Mismatch ❌

**Problem**: Using TEST credentials with LIVE environment or vice versa.

**How to Verify**:

- Current config: `adyen.environment=test` ✅
- API key location: Should be from TEST Customer Area (ca-test.adyen.com) ✅

**Status**: Configuration matches ✅

---

## Step-by-Step Fix Procedure

### Step 1: Get Fresh API Credentials

1. Go to: https://ca-test.adyen.com/
2. Log in with your Adyen test account
3. Click **"Developers"** in the left menu
4. Click **"API credentials"**
5. You should see your API credential listed (e.g., "EyepaxITConsultingPvtLtdECOM")

### Step 2: Verify API Credential Settings

Click on your API credential and verify:

**a) Status**: Should show "Active" (green indicator)

**b) Merchant accounts**:

- Should list your merchant account
- Note the EXACT name (case-sensitive)

**c) Roles & Permissions**:

- ✅ Checkout encrypted
- ✅ Merchant Checkout
- ✅ Management API - Accounts read (optional)

**d) Authentication**:

- Should show "API key" section

### Step 3: Generate New API Key

1. In the API credential page, scroll to **"API Key"** section
2. Click **"Generate new API key"** button
3. **IMPORTANT**: Copy the entire key immediately (it won't be shown again)
4. The key should be 100-200 characters long
5. Store it temporarily in Notepad to verify no line breaks

### Step 4: Update Configuration

1. Open: `src/main/resources/application-local.properties`
2. Replace the entire `adyen.api-key` line:
   ```properties
   adyen.api-key=PASTE_YOUR_NEW_KEY_HERE
   ```
3. Verify the merchant account name matches exactly
4. Save the file

### Step 5: Restart and Test

```powershell
# Stop the running application (Ctrl+C if needed)

# Rebuild to ensure config is loaded
mvn clean package -DskipTests

# Start with local profile
mvn spring-boot:run "-Dspring-boot.run.arguments=--spring.profiles.active=local"

# In another terminal, test
.\test-adyen-payment.ps1
```

---

## Quick Verification Checklist

Before testing again, verify:

- [ ] API key has NO line breaks or spaces
- [ ] API key starts with `AQE` (server-side key)
- [ ] API credential status is "Active" in Adyen Customer Area
- [ ] Permissions include "Checkout encrypted" and "Merchant Checkout"
- [ ] Merchant account name matches exactly (case-sensitive)
- [ ] Using TEST environment (`adyen.environment=test`)
- [ ] API key was generated from TEST Customer Area (ca-test.adyen.com)

---

## Alternative: Test with Curl Directly to Adyen

To verify your API key works directly with Adyen (bypassing your app):

```powershell
# Test API key directly with Adyen's API
$headers = @{
    "x-API-key" = "YOUR_API_KEY_HERE"
    "Content-Type" = "application/json"
}

$body = @{
    merchantAccount = "EyepaxITConsultingPvtLtdECOM"
    amount = @{
        value = 1000
        currency = "EUR"
    }
    reference = "TEST-" + (Get-Date -Format "yyyyMMddHHmmss")
    paymentMethod = @{
        type = "scheme"
        number = "4111111111111111"
        expiryMonth = "03"
        expiryYear = "2030"
        holderName = "Test User"
        cvc = "737"
    }
    returnUrl = "https://your-company.com/checkout"
    channel = "Android"
} | ConvertTo-Json -Depth 10

Invoke-WebRequest -Uri "https://checkout-test.adyen.com/v70/payments" `
    -Method POST `
    -Headers $headers `
    -Body $body
```

**Expected Result**:

- ✅ Success: You'll get a response with `resultCode` (e.g., "Authorised")
- ❌ 401 Error: API key is definitely invalid
- ❌ 403 Error: API key valid but lacks permissions
- ❌ 422 Error: API key valid but request data is incorrect

---

## Still Not Working?

If you've tried everything above and still get 401:

1. **Create a NEW API credential**:

   - In Adyen Customer Area → Developers → API credentials
   - Click "Create new credential"
   - Choose "Web service user"
   - Assign your merchant account
   - Enable "Checkout encrypted" permission
   - Generate API key
   - Update your config

2. **Contact Adyen Support**:

   - The API credential might be locked or have restrictions
   - There might be IP whitelisting configured
   - Account-level issues might exist

3. **Check for IP Restrictions**:
   - In API credential settings, check if there are IP address restrictions
   - If yes, add your current IP or remove restrictions for testing

---

**Next Steps**: Please follow Step 1-5 above to get a fresh API key and verify all settings match.
