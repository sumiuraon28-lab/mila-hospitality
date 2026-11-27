# MILA Hospitality — iPhone App Store & TestFlight Setup Guide

You can run MILA Hospitality on iPhone in **two ways**:

---

## 1. Instant iPhone Web App (Works Today — $0)
1. Push this repo to GitHub.
2. Open Safari on iPhone and visit:
   `https://sumiuraon28-lab.github.io/mila-hospitality/ios-app/`
3. Tap **Share → Add to Home Screen** to install the MILA icon full-screen on iPhone.

---

## 2. Official Apple App Store & TestFlight `.ipa` (No Mac Required!)
Once you enroll in the **Apple Developer Program ($99/year)** at https://developer.apple.com:

1. **Create an App ID** in Apple Developer Portal:
   - Bundle ID: `com.milahospitality.ios`
2. **Create an App Record** in App Store Connect (https://appstoreconnect.apple.com) using bundle ID `com.milahospitality.ios`.
3. **Create an iOS Distribution Certificate (`.p12`)** and an **App Store Provisioning Profile (`.mobileprovision`)** for `com.milahospitality.ios`.
4. **Add these GitHub Repository Secrets** (GitHub Repo → **Settings → Secrets and variables → Actions**):
   - `SUPABASE_URL` & `SUPABASE_ANON_KEY` (already added for Android!)
   - `APPLE_TEAM_ID` — your 10-character Apple Team ID
   - `APPLE_CERT_P12_BASE64` — base64 of your `.p12` distribution certificate
   - `APPLE_CERT_PASSWORD` — password for the `.p12` file
   - `APPLE_PROVISIONING_PROFILE_BASE64` — base64 of your `.mobileprovision` file
   - *(Optional for automatic TestFlight upload)*:
     - `ASC_API_KEY_ID`
     - `ASC_ISSUER_ID`
     - `ASC_API_KEY_P8_BASE64`
5. Go to **GitHub → Actions → Build MILA Hospitality iOS App (.ipa) → Run workflow**:
   - Choose **`signed-ipa`** to generate the signed `Mila-Hospitality.ipa` (and upload straight to TestFlight if ASC API keys are set).
   - Or choose **`unsigned-simulator`** anytime to verify the Xcode build before adding Apple certificates.
