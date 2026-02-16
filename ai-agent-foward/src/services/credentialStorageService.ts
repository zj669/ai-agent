import CryptoJS from 'crypto-js';
import type { User } from '../types/auth';

const CREDENTIAL_STORAGE_KEY = 'auth_encrypted_credential';
const CREDENTIAL_DEVICE_KEY = 'auth_encrypted_credential_device_id';
const LEGACY_STORAGE_KEYS = [
  'auth_token',
  'auth_refresh_token',
  'auth_device_id',
  'auth_user',
  'auth_expire_at'
];
const FIXED_SECRET = 'remember-me-fixed-secret-v1';

export interface CredentialPayload {
  token: string;
  refreshToken: string;
  deviceId: string;
  user: User;
  expireAt: number;
}

const buildDeviceEntropy = (deviceId: string): string => {
  const userAgent = typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown';
  const platform = typeof navigator !== 'undefined' ? navigator.platform : 'unknown';
  return `${deviceId}::${userAgent}::${platform}`;
};

const buildEncryptionKey = (deviceId: string): string => {
  const entropy = buildDeviceEntropy(deviceId);
  return CryptoJS.SHA256(`${FIXED_SECRET}::${entropy}`).toString();
};

const clearLegacyStorage = (): void => {
  LEGACY_STORAGE_KEYS.forEach(key => {
    localStorage.removeItem(key);
    sessionStorage.removeItem(key);
  });
};

export const clearCredential = (): void => {
  localStorage.removeItem(CREDENTIAL_STORAGE_KEY);
  localStorage.removeItem(CREDENTIAL_DEVICE_KEY);
  sessionStorage.removeItem(CREDENTIAL_STORAGE_KEY);
  sessionStorage.removeItem(CREDENTIAL_DEVICE_KEY);
  clearLegacyStorage();
};

export const saveCredential = (payload: CredentialPayload, rememberMe: boolean): void => {
  const key = buildEncryptionKey(payload.deviceId);
  const encrypted = CryptoJS.AES.encrypt(JSON.stringify(payload), key).toString();
  const targetStorage = rememberMe ? localStorage : sessionStorage;
  const otherStorage = rememberMe ? sessionStorage : localStorage;

  targetStorage.setItem(CREDENTIAL_STORAGE_KEY, encrypted);
  targetStorage.setItem(CREDENTIAL_DEVICE_KEY, payload.deviceId);
  otherStorage.removeItem(CREDENTIAL_STORAGE_KEY);
  otherStorage.removeItem(CREDENTIAL_DEVICE_KEY);
};

export const loadCredential = (): CredentialPayload | null => {
  const encrypted = localStorage.getItem(CREDENTIAL_STORAGE_KEY) || sessionStorage.getItem(CREDENTIAL_STORAGE_KEY);

  if (!encrypted) {
    return null;
  }

  const deviceId = localStorage.getItem(CREDENTIAL_DEVICE_KEY) || sessionStorage.getItem(CREDENTIAL_DEVICE_KEY);
  if (!deviceId) {
    clearCredential();
    return null;
  }

  try {
    const decrypted = CryptoJS.AES.decrypt(encrypted, buildEncryptionKey(deviceId)).toString(CryptoJS.enc.Utf8);
    if (!decrypted) {
      clearCredential();
      return null;
    }

    const payload = JSON.parse(decrypted) as CredentialPayload;
    if (!payload.token || !payload.refreshToken || !payload.deviceId || !payload.user || !payload.expireAt) {
      clearCredential();
      return null;
    }

    return payload;
  } catch {
    clearCredential();
    return null;
  }
};

export const hasStoredCredential = (): boolean => {
  return Boolean(
    (localStorage.getItem(CREDENTIAL_STORAGE_KEY) || sessionStorage.getItem(CREDENTIAL_STORAGE_KEY)) &&
      (localStorage.getItem(CREDENTIAL_DEVICE_KEY) || sessionStorage.getItem(CREDENTIAL_DEVICE_KEY))
  );
};
