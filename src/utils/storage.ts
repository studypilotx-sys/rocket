import { StudyMaterialItem } from '../types';

/**
 * Storage Utility for StudyPilot
 * Handles IndexedDB for high-capacity local persistence (avoiding browser 5MB localStorage QuotaExceededError)
 * and provides safe wrappers around localStorage with quota recovery.
 */

const DB_NAME = 'StudyPilotDB';
const DB_VERSION = 1;
const STORE_MATERIALS = 'study_materials';
const STORE_APP_DATA = 'app_data';

// Helper to open or initialize IndexedDB
function openDB(): Promise<IDBDatabase | null> {
  return new Promise((resolve) => {
    if (typeof window === 'undefined' || !window.indexedDB) {
      resolve(null);
      return;
    }

    try {
      const request = window.indexedDB.open(DB_NAME, DB_VERSION);

      request.onupgradeneeded = (event) => {
        const db = (event.target as IDBOpenDBRequest).result;
        if (!db.objectStoreNames.contains(STORE_MATERIALS)) {
          db.createObjectStore(STORE_MATERIALS, { keyPath: 'id' });
        }
        if (!db.objectStoreNames.contains(STORE_APP_DATA)) {
          db.createObjectStore(STORE_APP_DATA);
        }
      };

      request.onsuccess = () => {
        resolve(request.result);
      };

      request.onerror = () => {
        console.warn('[StudyPilot DB] Failed to open IndexedDB, using fallback storage');
        resolve(null);
      };
    } catch (e) {
      console.warn('[StudyPilot DB] IndexedDB exception:', e);
      resolve(null);
    }
  });
}

/**
 * Persist full materials collection (with data URLs/files) into IndexedDB
 */
export async function saveMaterialsToDB(materials: StudyMaterialItem[]): Promise<boolean> {
  const db = await openDB();
  if (!db) return false;

  return new Promise((resolve) => {
    try {
      const tx = db.transaction([STORE_MATERIALS], 'readwrite');
      const store = tx.objectStore(STORE_MATERIALS);

      // Clear previous and re-insert to keep in sync
      const clearReq = store.clear();
      clearReq.onsuccess = () => {
        for (const item of materials) {
          store.put(item);
        }
      };

      tx.oncomplete = () => {
        db.close();
        resolve(true);
      };

      tx.onerror = (err) => {
        console.warn('[StudyPilot DB] Error saving materials to IndexedDB:', err);
        db.close();
        resolve(false);
      };
    } catch (err) {
      console.warn('[StudyPilot DB] Exception in saveMaterialsToDB:', err);
      db.close();
      resolve(false);
    }
  });
}

/**
 * Load full materials collection from IndexedDB
 */
export async function loadMaterialsFromDB(): Promise<StudyMaterialItem[] | null> {
  const db = await openDB();
  if (!db) return null;

  return new Promise((resolve) => {
    try {
      const tx = db.transaction([STORE_MATERIALS], 'readonly');
      const store = tx.objectStore(STORE_MATERIALS);
      const request = store.getAll();

      request.onsuccess = () => {
        db.close();
        const res = request.result as StudyMaterialItem[];
        resolve(res && res.length > 0 ? res : null);
      };

      request.onerror = () => {
        db.close();
        resolve(null);
      };
    } catch (err) {
      console.warn('[StudyPilot DB] Exception in loadMaterialsFromDB:', err);
      db.close();
      resolve(null);
    }
  });
}

/**
 * Permanently delete a material from IndexedDB by ID
 */
export async function deleteMaterialFromDB(id: string): Promise<boolean> {
  const db = await openDB();
  if (!db) return false;

  return new Promise((resolve) => {
    try {
      const tx = db.transaction([STORE_MATERIALS], 'readwrite');
      const store = tx.objectStore(STORE_MATERIALS);
      const req = store.delete(id);

      req.onsuccess = () => {
        // success
      };

      tx.oncomplete = () => {
        db.close();
        resolve(true);
      };

      tx.onerror = (err) => {
        console.warn('[StudyPilot DB] Error deleting material from IndexedDB:', err);
        db.close();
        resolve(false);
      };
    } catch (err) {
      console.warn('[StudyPilot DB] Exception in deleteMaterialFromDB:', err);
      db.close();
      resolve(false);
    }
  });
}

/**
 * Clean compact representation of materials for localStorage fallback
 * Strips huge base64 strings so localStorage stays under 100KB rather than hitting 5MB quota
 */
export function createCompactMaterials(materials: StudyMaterialItem[]): StudyMaterialItem[] {
  return materials.map(mat => {
    // If uriOrPath is a massive data URL (e.g. > 1KB base64), replace with a lightweight placeholder
    if (mat.uriOrPath && mat.uriOrPath.startsWith('data:') && mat.uriOrPath.length > 1024) {
      return {
        ...mat,
        uriOrPath: '[STORED_IN_INDEXEDDB]'
      };
    }
    return mat;
  });
}

/**
 * Emergency storage optimizer when QuotaExceededError is caught
 */
function cleanupLocalStorageQuota() {
  try {
    // 1. Strip materials in localStorage if they exist
    const rawMaterials = localStorage.getItem('studypilot_materials_v1');
    if (rawMaterials && rawMaterials.length > 50000) {
      try {
        const parsed = JSON.parse(rawMaterials) as StudyMaterialItem[];
        const compact = createCompactMaterials(parsed);
        localStorage.setItem('studypilot_materials_v1', JSON.stringify(compact));
      } catch {
        localStorage.removeItem('studypilot_materials_v1');
      }
    }

    // 2. Trim study sessions if excessive
    const rawSessions = localStorage.getItem('studypilot_sessions_v1');
    if (rawSessions && rawSessions.length > 100000) {
      try {
        const parsedSessions = JSON.parse(rawSessions);
        if (Array.isArray(parsedSessions) && parsedSessions.length > 30) {
          const trimmed = parsedSessions.slice(-30);
          localStorage.setItem('studypilot_sessions_v1', JSON.stringify(trimmed));
        }
      } catch {
        localStorage.removeItem('studypilot_sessions_v1');
      }
    }
  } catch (e) {
    console.warn('[StudyPilot Storage] Error during storage quota cleanup:', e);
  }
}

/**
 * Safe wrapper around window.localStorage
 * Completely prevents uncaught QuotaExceededError from crashing the app
 */
export const safeLocalStorage = {
  getItem(key: string): string | null {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return null;
      return window.localStorage.getItem(key);
    } catch (e) {
      console.warn(`[StudyPilot Storage] Failed to read ${key} from localStorage:`, e);
      return null;
    }
  },

  setItem(key: string, value: string): boolean {
    try {
      if (typeof window === 'undefined' || !window.localStorage) return false;
      window.localStorage.setItem(key, value);
      return true;
    } catch (err: any) {
      // Check for QuotaExceededError across browser engines
      const isQuota =
        err?.name === 'QuotaExceededError' ||
        err?.name === 'NS_ERROR_DOM_QUOTA_REACHED' ||
        err?.code === 22 ||
        err?.code === 1014 ||
        err?.number === -2147024882 ||
        (typeof err?.message === 'string' && err.message.toLowerCase().includes('quota'));

      if (isQuota) {
        console.warn(`[StudyPilot Storage] QuotaExceededError while saving '${key}'. Running storage quota recovery...`);
        cleanupLocalStorageQuota();

        // Retry saving once after quota cleanup
        try {
          window.localStorage.setItem(key, value);
          return true;
        } catch (retryErr) {
          console.warn(`[StudyPilot Storage] Quota still exceeded for '${key}'. Safely skipped to prevent crash.`);
          return false;
        }
      }

      console.warn(`[StudyPilot Storage] Unexpected error saving '${key}':`, err);
      return false;
    }
  },

  removeItem(key: string): void {
    try {
      if (typeof window !== 'undefined' && window.localStorage) {
        window.localStorage.removeItem(key);
      }
    } catch (e) {
      console.warn(`[StudyPilot Storage] Failed to remove ${key}:`, e);
    }
  }
};

/**
 * Safe JSON parser with default value fallback
 */
export function safeGetJSON<T>(key: string, defaultValue: T): T {
  try {
    const raw = safeLocalStorage.getItem(key);
    if (!raw) return defaultValue;
    return JSON.parse(raw) as T;
  } catch (err) {
    console.warn(`[StudyPilot Storage] JSON parse error for '${key}', returning fallback:`, err);
    return defaultValue;
  }
}

/**
 * Safe JSON serializer with quota protection
 */
export function safeSetJSON<T>(key: string, value: T): boolean {
  try {
    const str = JSON.stringify(value);
    return safeLocalStorage.setItem(key, str);
  } catch (err) {
    console.warn(`[StudyPilot Storage] Failed to serialize '${key}':`, err);
    return false;
  }
}
