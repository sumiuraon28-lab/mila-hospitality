import * as Device from 'expo-device';
import * as Notifications from 'expo-notifications';
import { Platform } from 'react-native';
import { isSupabaseConfigured, supabase } from '../lib/supabase';
import { MilaColors } from '../theme/milaTheme';

/**
 * Configures foreground notification presentation for Android and iOS.
 */
if (Platform.OS !== 'web') {
  Notifications.setNotificationHandler({
    handleNotification: async () => ({
      shouldShowAlert: true,
      shouldPlaySound: true,
      shouldSetBadge: true,
    }),
  });
}

export interface PushRegistrationStatus {
  permissionStatus: 'granted' | 'denied' | 'undetermined';
  nativeToken: string | null;
  platform: 'ANDROID' | 'IOS' | 'WEB';
  syncedToSupabase: boolean;
  message: string;
}

/**
 * Creates Android notification channels aligned with MILA's FCM backend payload
 * (`mila_job_matches` and `mila_application_updates`).
 */
export async function ensureNotificationChannels(): Promise<void> {
  if (Platform.OS !== 'android') return;

  await Notifications.setNotificationChannelAsync('mila_job_matches', {
    name: 'MILA Matched Job Vacancies',
    description: 'Targeted hospitality career matches tailored to your profile and preferences.',
    importance: Notifications.AndroidImportance.HIGH,
    lightColor: MilaColors.champagneGold,
    vibrationPattern: [0, 220, 120, 220],
  });

  await Notifications.setNotificationChannelAsync('mila_application_updates', {
    name: 'MILA Application Updates',
    description: 'Status updates on your active MILA Talent hospitality applications.',
    importance: Notifications.AndroidImportance.HIGH,
    lightColor: MilaColors.champagneGold,
    vibrationPattern: [0, 180, 100, 180],
  });
}

/**
 * Requests Android 13+ / iOS APNs push permission, retrieves the native device push token
 * (FCM on Android, APNs on iOS), and registers it in MILA's `public.device_push_tokens` table.
 */
export async function registerDevicePushToken(
  userId: string,
  enabled: boolean
): Promise<PushRegistrationStatus> {
  if (Platform.OS === 'web') {
    return {
      permissionStatus: 'undetermined',
      nativeToken: null,
      platform: 'WEB',
      syncedToSupabase: false,
      message: 'In-app and email job alerts are active on Web. Install the Android/iOS app for native push notifications.',
    };
  }

  const platform: 'ANDROID' | 'IOS' = Platform.OS === 'ios' ? 'IOS' : 'ANDROID';
  await ensureNotificationChannels();

  if (!Device.isDevice) {
    return {
      permissionStatus: 'undetermined',
      nativeToken: null,
      platform,
      syncedToSupabase: false,
      message: 'Physical Android or iOS device required for live FCM/APNs push tokens.',
    };
  }

  const existing = await Notifications.getPermissionsAsync();
  let finalStatus = existing.status;

  if (finalStatus !== 'granted' && enabled) {
    const requested = await Notifications.requestPermissionsAsync({
      ios: {
        allowAlert: true,
        allowBadge: true,
        allowSound: true,
      },
    });
    finalStatus = requested.status;
  }

  if (finalStatus !== 'granted') {
    return {
      permissionStatus: finalStatus === 'denied' ? 'denied' : 'undetermined',
      nativeToken: null,
      platform,
      syncedToSupabase: false,
      message: 'Notification permission is currently disabled in device settings.',
    };
  }

  let nativeToken: string | null = null;
  try {
    const tokenResult = await Notifications.getDevicePushTokenAsync();
    nativeToken = typeof tokenResult.data === 'string' ? tokenResult.data : null;
  } catch {
    nativeToken = null;
  }

  if (!nativeToken || nativeToken.length < 20) {
    return {
      permissionStatus: 'granted',
      nativeToken: null,
      platform,
      syncedToSupabase: false,
      message:
        'Push permission granted. Run a native development or EAS build with Firebase/APNs configured to receive a live device token.',
    };
  }

  if (!isSupabaseConfigured) {
    return {
      permissionStatus: 'granted',
      nativeToken,
      platform,
      syncedToSupabase: false,
      message: 'Device push token obtained. Configure Supabase credentials to sync.',
    };
  }

  const deviceLabel = [Device.manufacturer, Device.modelName].filter(Boolean).join(' ') || 'Mobile Device';

  const { error } = await supabase.from('device_push_tokens').upsert(
    {
      user_id: userId,
      fcm_token: nativeToken,
      platform,
      apns_environment: platform === 'IOS' ? (__DEV__ ? 'SANDBOX' : 'PRODUCTION') : null,
      device_label: deviceLabel.slice(0, 120),
      is_active: enabled,
      last_seen_at: new Date().toISOString(),
    },
    { onConflict: 'user_id,fcm_token' }
  );

  if (error) {
    return {
      permissionStatus: 'granted',
      nativeToken,
      platform,
      syncedToSupabase: false,
      message: `Unable to register push token: ${error.message}`,
    };
  }

  return {
    permissionStatus: 'granted',
    nativeToken,
    platform,
    syncedToSupabase: true,
    message: enabled
      ? `${platform === 'IOS' ? 'iOS APNs' : 'Android FCM'} push alerts active and synced.`
      : 'Push alerts paused on this device.',
  };
}
