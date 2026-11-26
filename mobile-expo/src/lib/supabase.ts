import 'react-native-url-polyfill/auto';
import { createClient, SupabaseClient } from '@supabase/supabase-js';
import * as SecureStore from 'expo-secure-store';
import { Platform } from 'react-native';

/**
 * MILA Supabase Cross-Platform Client (Android, iOS & Web)
 * - Connects directly to the existing MILA Supabase backend (no separate database).
 * - Uses ONLY public anon credentials via EXPO_PUBLIC_* env vars.
 * - NEVER embeds SUPABASE_SERVICE_ROLE_KEY in frontend code.
 * - Persists auth sessions in iOS Keychain / Android Keystore via chunked SecureStore,
 *   and uses a clean in-memory session store on Web (no localStorage token exposure).
 */

const CHUNK_SIZE = 1800;
const webMemoryStore = new Map<string, string>();

const SecureStoreChunkAdapter = {
  async getItem(key: string): Promise<string | null> {
    if (Platform.OS === 'web') {
      return webMemoryStore.get(key) ?? null;
    }
    const countStr = await SecureStore.getItemAsync(`${key}__chunks`);
    if (!countStr) {
      return SecureStore.getItemAsync(key);
    }
    const count = Number.parseInt(countStr, 10);
    if (Number.isNaN(count) || count <= 0) {
      return null;
    }
    const parts: string[] = [];
    for (let i = 0; i < count; i += 1) {
      const chunk = await SecureStore.getItemAsync(`${key}__chunk_${i}`);
      if (chunk === null) {
        return null;
      }
      parts.push(chunk);
    }
    return parts.join('');
  },

  async setItem(key: string, value: string): Promise<void> {
    if (Platform.OS === 'web') {
      webMemoryStore.set(key, value);
      return;
    }
    if (value.length <= CHUNK_SIZE) {
      await SecureStore.deleteItemAsync(`${key}__chunks`);
      await SecureStore.setItemAsync(key, value);
      return;
    }
    const totalChunks = Math.ceil(value.length / CHUNK_SIZE);
    for (let i = 0; i < totalChunks; i += 1) {
      const slice = value.slice(i * CHUNK_SIZE, (i + 1) * CHUNK_SIZE);
      await SecureStore.setItemAsync(`${key}__chunk_${i}`, slice);
    }
    await SecureStore.setItemAsync(`${key}__chunks`, String(totalChunks));
    await SecureStore.deleteItemAsync(key);
  },

  async removeItem(key: string): Promise<void> {
    if (Platform.OS === 'web') {
      webMemoryStore.delete(key);
      return;
    }
    const countStr = await SecureStore.getItemAsync(`${key}__chunks`);
    if (countStr) {
      const count = Number.parseInt(countStr, 10);
      if (!Number.isNaN(count) && count > 0) {
        for (let i = 0; i < count; i += 1) {
          await SecureStore.deleteItemAsync(`${key}__chunk_${i}`);
        }
      }
      await SecureStore.deleteItemAsync(`${key}__chunks`);
    }
    await SecureStore.deleteItemAsync(key);
  },
};

const rawUrl = (process.env.EXPO_PUBLIC_SUPABASE_URL ?? '').trim();
const rawAnonKey = (process.env.EXPO_PUBLIC_SUPABASE_ANON_KEY ?? '').trim();

export const isSupabaseConfigured: boolean =
  rawUrl.startsWith('https://') &&
  !rawUrl.includes('YOUR_SUPABASE_URL') &&
  rawAnonKey.length > 20 &&
  !rawAnonKey.includes('YOUR_SUPABASE_ANON_KEY');

const safeUrl = isSupabaseConfigured ? rawUrl : 'https://placeholder.supabase.co';
const safeAnonKey = isSupabaseConfigured ? rawAnonKey : 'placeholder-anon-key';

export const supabase: SupabaseClient = createClient(safeUrl, safeAnonKey, {
  auth: {
    storage: SecureStoreChunkAdapter,
    autoRefreshToken: isSupabaseConfigured,
    persistSession: true,
    detectSessionInUrl: false,
  },
});

export function assertSupabaseReady(): void {
  if (!isSupabaseConfigured) {
    throw new Error(
      'Supabase credentials are not configured. Copy mobile-expo/.env.example to mobile-expo/.env and set EXPO_PUBLIC_SUPABASE_URL and EXPO_PUBLIC_SUPABASE_ANON_KEY from your MILA Supabase project.'
    );
  }
}
