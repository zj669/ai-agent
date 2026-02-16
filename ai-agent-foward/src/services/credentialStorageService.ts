import CryptoJS from 'crypto-js';

const STORAGE_KEY = 'remembered_credential';
const APP_SECRET = 'ai-agent-remember-me-v1';

export interface CredentialPayload {
  email: string;
  password: string;
}

interface StoredCredential {
  encrypted: string;
  timestamp: number;
}

const deriveKey = (): string => {
  const userAgent = typeof navigator !== 'undefined' ? navigator.userAgent : 'unknown';
  const language = typeof navigator !== 'undefined' ? navigator.language : 'unknown';
  const timeZone = Intl.DateTimeFormat().resolvedOptions().timeZone || 'unknown';
  const fingerprint = [userAgent, language, timeZone].join('|');

  return CryptoJS.SHA256(`${APP_SECRET}:${fingerprint}`).toString();
};

export const clearCredential = (): void => {
  localStorage.removeItem(STORAGE_KEY);
};

export const saveCredential = (payload: CredentialPayload, _rememberMe: boolean): void => {
  const key = deriveKey();
  const encrypted = CryptoJS.AES.encrypt(JSON.stringify(payload), key).toString();
  const data: StoredCredential = {
    encrypted,
    timestamp: Date.now()
  };

  localStorage.setItem(STORAGE_KEY, JSON.stringify(data));
};

export const loadCredential = (): CredentialPayload | null => {
  const raw = localStorage.getItem(STORAGE_KEY);

  if (!raw) {
    return null;
  }

  try {
    const data = JSON.parse(raw) as StoredCredential;
    if (!data?.encrypted || !data?.timestamp) {
      clearCredential();
      return null;
    }

    const key = deriveKey();
    const decrypted = CryptoJS.AES.decrypt(data.encrypted, key).toString(CryptoJS.enc.Utf8);
    if (!decrypted) {
      clearCredential();
      return null;
    }

    const payload = JSON.parse(decrypted) as CredentialPayload;
    if (!payload?.email || !payload?.password) {
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
  return Boolean(localStorage.getItem(STORAGE_KEY));
};
