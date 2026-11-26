# MILA Hospitality Consultant — Android & iOS Mobile Apps (React Native & Expo)

Cross-platform Android and iOS mobile application for **MILA Hospitality Consultant** (`Creating Excellence in Hospitality`), built with **React Native, Expo SDK 52, TypeScript, and React Navigation**, connected directly to the **existing MILA Supabase backend** (`../supabase/migrations/`). No separate or disconnected database is created.

---

## Brand & Assets Included

- **Brand Palette**: Deep Charcoal (`#191D1B`), Matte Surface (`#212623`), Champagne Gold (`#D6BD96`), Warm Ivory (`#F5F1E9`).
- **App Icons & Splash**:
  - `assets/icon.png` (`1024×1024` full-bleed iOS & store icon)
  - `assets/adaptive-icon.png` (`1024×1024` Android Adaptive Icon foreground on `#191D1B`)
  - `assets/notification-icon.png` (`96×96` Android notification icon)
  - `assets/splash.png` (`1284×2778` dark charcoal & champagne-gold art-deco splash screen)
- **Responsive Mobile & Tablet Layouts**: Safe-area handling (`react-native-safe-area-context`) and adaptive phone/tablet breakpoints (`useResponsiveLayout`).

---

## Connected Supabase Backend Tables & Security

This mobile app connects directly to your existing MILA Supabase schema (migrations `202611250001` through `202611250004`):

- **Client Flow**: `services`, `testimonials`, `user_profiles`, `client_profiles`, `service_enquiries` (strict RLS isolation per client owner).
- **Job Seeker Flow (MILA Talent)**: `candidate_profiles`, `candidate_preferences`, `job_vacancies`, `job_applications`, `saved_jobs`, `notifications`, `device_push_tokens`.
- **Private CV Vault**: Uploads PDF/DOC/DOCX (max 10 MB) directly to the private `candidate-cvs` bucket under `<auth.uid()>/...` with short-lived private signed URLs for owner preview (never public download links).
- **Zero Service-Role Keys**: Only `EXPO_PUBLIC_SUPABASE_URL` and `EXPO_PUBLIC_SUPABASE_ANON_KEY` are used in the mobile app. Auth tokens are stored in iOS Keychain and Android Keystore via `expo-secure-store`.

---

## 1. Prerequisites & Environment Setup

1. Install **Node.js 20 LTS** and **npm** (or **pnpm**).
2. Ensure all 4 SQL migrations in `../supabase/migrations/` are applied to your MILA Supabase project.
3. Configure your mobile environment variables:

```bash
cd mobile-expo
cp .env.example .env
```

Edit `mobile-expo/.env` with your real MILA Supabase project values:

```env
EXPO_PUBLIC_SUPABASE_URL=https://<your-project-ref>.supabase.co
EXPO_PUBLIC_SUPABASE_ANON_KEY=<your-supabase-anon-key>
EXPO_PUBLIC_EAS_PROJECT_ID=<optional-expo-eas-project-id>
```

4. Install dependencies:

```bash
npm install
npm run typecheck
```

---

## 2. Running & Testing Locally (Android & iOS)

### Quick Development Preview (Expo Go)

```bash
cd mobile-expo
npx expo start
```

- Press **`a`** to launch on an **Android Emulator** (Android Studio AVD).
- Press **`i`** to launch on an **iOS Simulator** (macOS + Xcode required).
- Scan the QR code with **Expo Go** on a physical Android or iOS phone.

> **Note on Live Push Notifications**: Expo Go does not support custom native FCM/APNs credentials. To test real background push notifications from MILA's `dispatch-job-notifications` Edge Function, run a **Development Build** (`npx expo run:android` / `npx expo run:ios` or `eas build --profile development`).

### Native Development Builds (Full FCM & APNs Push Support)

**Android (Windows / macOS / Linux with Android Studio):**

1. Download `google-services.json` from your Firebase project (for package `com.milahospitality.mobile`) and place it at `mobile-expo/google-services.json`, then add `"googleServicesFile": "./google-services.json"` under `expo.android` in `app.json`.
2. Run:

```bash
cd mobile-expo
npx expo prebuild --platform android
npx expo run:android
```

**iOS (macOS with Xcode & CocoaPods):**

1. Download `GoogleService-Info.plist` (if using Firebase iOS) and enable Push Notifications capability + APNs Key (`.p8`) in Apple Developer Portal / Firebase Cloud Messaging settings.
2. Run:

```bash
cd mobile-expo
npx expo prebuild --platform ios
npx expo run:ios
```

---

## 3. Building Signed Production Binaries with EAS (Android & iOS)

1. Install EAS CLI and sign in:

```bash
npm install -g eas-cli
eas login
cd mobile-expo
eas init
```

2. Add your Supabase environment variables to EAS Secrets (or EAS Environment Variables):

```bash
eas env:create --name EXPO_PUBLIC_SUPABASE_URL --value "https://<your-project-ref>.supabase.co" --visibility plaintext
eas env:create --name EXPO_PUBLIC_SUPABASE_ANON_KEY --value "<your-anon-key>" --visibility plaintext
```

3. Build **Android APK (Internal QA)** or **Android App Bundle (Google Play)**:

```bash
# Installable APK for testing on Android phones
npm run build:android:preview

# Signed AAB for Google Play Store submission
npm run build:android:prod
```

4. Build **iOS TestFlight / App Store IPA** (builds in the cloud even without a Mac):

```bash
# Internal iOS preview build
npm run build:ios:preview

# Production IPA for App Store Connect / TestFlight
npm run build:ios:prod
```

---

## 4. End-to-End Verification Checklist

- **Client Flow**: Register a new client (`Client Registration`) -> verify login returns directly to MILA Home/Services (no dashboard) -> open any of the 8 Service Detail pages -> submit an Enquiry -> confirm `MILA-XXXXXX` reference appears in `service_enquiries` and is invisible to other client accounts.
- **Job Seeker Flow**: Switch to **MILA Talent** -> register a candidate (15 fields) -> toggle **I AM LOOKING FOR A JOB** -> upload a PDF/DOC/DOCX CV in **Profile & CV** -> confirm file exists in private `candidate-cvs/<uid>/...` bucket -> search & filter jobs by department, location, and experience -> save a job -> apply -> track live status in **Status** tab.
- **Push & Alert Sync**: Open **Alerts** tab on a physical Android/iOS dev build -> grant notification permission -> toggle Push & Email preferences -> verify active token appears in `public.device_push_tokens` and receives targeted alerts when an admin publishes a matching vacancy.
