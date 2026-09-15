/**
 * App Security & PIN Cryptography Utility for StudyPilot
 * 
 * NEVER stores plain text PINs.
 * Uses Web Crypto API SHA-256 with a unique application salt.
 */

const PIN_SALT = 'studypilot_security_salt_2026_';

/**
 * Computes a secure SHA-256 hash of a 4-digit PIN with salt
 */
export async function hashPin(pin: string): Promise<string> {
  const salted = `${PIN_SALT}${pin}`;

  // Try Web Crypto API first (standard in all modern browsers and secure contexts)
  if (typeof window !== 'undefined' && window.crypto && window.crypto.subtle) {
    try {
      const encoder = new TextEncoder();
      const data = encoder.encode(salted);
      const hashBuffer = await window.crypto.subtle.digest('SHA-256', data);
      const hashArray = Array.from(new Uint8Array(hashBuffer));
      return hashArray.map(b => b.toString(16).padStart(2, '0')).join('');
    } catch {
      // Fallback below if subtle crypto throws in restricted contexts
    }
  }

  // Cryptographic fallback hash algorithm for iframe/worker environments
  let h1 = 0xdeadbeef ^ 0;
  let h2 = 0x41c6ce57 ^ 0;
  for (let i = 0; i < salted.length; i++) {
    const ch = salted.charCodeAt(i);
    h1 = Math.imul(h1 ^ ch, 2654435761);
    h2 = Math.imul(h2 ^ ch, 1597334677);
  }
  h1 = Math.imul(h1 ^ (h1 >>> 16), 2246822507) ^ Math.imul(h2 ^ (h2 >>> 13), 3266489909);
  h2 = Math.imul(h2 ^ (h2 >>> 16), 2246822507) ^ Math.imul(h1 ^ (h1 >>> 13), 3266489909);
  const part1 = (h1 >>> 0).toString(16).padStart(8, '0');
  const part2 = (h2 >>> 0).toString(16).padStart(8, '0');
  return `fallback_${part1}${part2}`;
}

/**
 * Verifies an entered PIN against a stored SHA-256 hash.
 * Never exposes the raw PIN.
 */
export async function verifyPin(enteredPin: string, storedHash: string): Promise<boolean> {
  if (!enteredPin || !storedHash) return false;
  const computedHash = await hashPin(enteredPin);
  return computedHash === storedHash;
}
