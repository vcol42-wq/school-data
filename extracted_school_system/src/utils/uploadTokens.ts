/**
 * uploadTokens.ts
 * دوال توليد الرموز الآمنة ورموز جلسات رفع الدرجات
 */

export function generateUploadToken(): string {
  const arr = new Uint8Array(32);
  if (typeof (globalThis as any).crypto !== 'undefined' && typeof (globalThis as any).crypto.getRandomValues === 'function') {
    (globalThis as any).crypto.getRandomValues(arr);
  } else {
    try {
      const nodeCrypto = require('crypto');
      const buf: Buffer = nodeCrypto.randomBytes(32);
      for (let i = 0; i < buf.length; i++) arr[i] = buf[i];
    } catch {
      for (let i = 0; i < 32; i++) arr[i] = Math.floor(Math.random() * 256);
    }
  }
  
  let binary = '';
  for (let i = 0; i < arr.length; i++) {
    binary += String.fromCharCode(arr[i]);
  }
  let str = btoa(binary);
  return str.replace(/\+/g, '-').replace(/\//g, '_').replace(/=+$/, '');
}

/**
 * توليد رمز PIN رقمي آمن (6 أرقام) لجلسة المعلم
 */
export function generateSecureOtp(): string {
  // توليد رقم عشوائي مكون من 6 أرقام (100000 - 999999)
  const random = Math.floor(100000 + Math.random() * 900000);
  return random.toString();
}
