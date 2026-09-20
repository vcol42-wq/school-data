import { getSupabase } from './supabaseClient';

export interface LicenseRecord {
  id?: string;
  license_key: string;
  school_name: string;
  contact_phone?: string;
  contact_email?: string;
  machine_fingerprint?: string;
  is_activated: boolean;
  activated_at?: string;
  notes?: string;
  created_at?: string;
}

const LOCAL_LICENSE_STORAGE_KEY = 'the_principal_v6_desktop_license';
const LOCAL_FINGERPRINT_STORAGE_KEY = 'the_principal_v6_machine_fingerprint';

/**
 * Generate or retrieve a persistent machine fingerprint for this computer.
 */
export function getMachineFingerprint(): string {
  let fp = localStorage.getItem(LOCAL_FINGERPRINT_STORAGE_KEY);
  if (!fp) {
    const raw = [
      navigator.userAgent,
      screen.width + 'x' + screen.height,
      screen.colorDepth,
      navigator.hardwareConcurrency || 'x',
      navigator.language || 'ar',
      new Date().getTimezoneOffset()
    ].join('###');

    // Simple robust hash
    let hash = 0;
    for (let i = 0; i < raw.length; i++) {
      const chr = raw.charCodeAt(i);
      hash = ((hash << 5) - hash) + chr;
      hash |= 0;
    }
    const randPart = Math.random().toString(36).substring(2, 6).toUpperCase();
    fp = `PC-${Math.abs(hash).toString(16).toUpperCase()}-${randPart}`;
    localStorage.setItem(LOCAL_FINGERPRINT_STORAGE_KEY, fp);
  }
  return fp;
}

/**
 * Get locally stored active license if present.
 */
export function getLocalLicense(): LicenseRecord | null {
  try {
    const saved = localStorage.getItem(LOCAL_LICENSE_STORAGE_KEY);
    if (!saved) return null;
    return JSON.parse(saved);
  } catch {
    return null;
  }
}

/**
 * Check if the current desktop installation is permanently activated.
 */
export function isDesktopActivated(): boolean {
  const local = getLocalLicense();
  return !!(local && local.is_activated && local.license_key);
}

/**
 * Compute 2-character hex checksum for a license key.
 */
export function computeLicenseChecksum(base: string): string {
  const clean = base.replace(/[^0-9A-Z]/g, '');
  let hash = 0x55AA;
  for (let i = 0; i < clean.length; i++) {
    hash = ((hash << 5) - hash) + clean.charCodeAt(i);
    hash = (hash & 0xFFFF);
  }
  const hex = Math.abs(hash).toString(16).toUpperCase().padStart(4, '0');
  return hex.substring(hex.length - 2);
}

/**
 * Check whether a key is valid according to the Unified KeyGen algorithm.
 */
export function isValidUnifiedLicenseKey(key: string): boolean {
  const clean = key.trim().toUpperCase();
  const parts = clean.split('-');
  if (parts.length < 3) return false;

  // If format ends with a 2-char checksum:
  const lastPart = parts[parts.length - 1];
  if (lastPart.length === 2) {
    const base = parts.slice(0, parts.length - 1).join('-');
    const expected = computeLicenseChecksum(base);
    if (lastPart === expected) return true;
  }

  // Fallback for standard BOSS keys (e.g. BOSS-XXXX-XXXX-XXXX)
  return clean.startsWith('BOSS-') && clean.length >= 14;
}

/**
 * Verify and activate a lifetime product license key in Supabase.
 */
export async function activateDesktopLicense(
  keyInput: string,
  schoolNameInput?: string
): Promise<{ success: boolean; message: string; license?: LicenseRecord }> {
  const cleanKey = keyInput.trim().toUpperCase();
  if (!cleanKey) {
    return { success: false, message: 'يرجى كتابة كود التفعيل أولاً.' };
  }

  const supabase = getSupabase();
  const currentFp = getMachineFingerprint();

  try {
    // 1. Fetch license details from Supabase
    const { data, error } = await supabase
      .from('desktop_licenses')
      .select('*')
      .eq('license_key', cleanKey)
      .maybeSingle();

    if (error) {
      console.warn('License verification error:', error);
      // Fallback: If offline or table not yet migrated, verify format
      if (isValidUnifiedLicenseKey(cleanKey)) {
        const fallbackLic: LicenseRecord = {
          license_key: cleanKey,
          school_name: schoolNameInput || 'مدرستنا الكريمة',
          machine_fingerprint: currentFp,
          is_activated: true,
          activated_at: new Date().toISOString()
        };
        localStorage.setItem(LOCAL_LICENSE_STORAGE_KEY, JSON.stringify(fallbackLic));
        return {
          success: true,
          message: 'تم تفعيل المنظومة بنجاح (وضع الحفظ المحلي الموثق) ✓',
          license: fallbackLic
        };
      }
      return { success: false, message: 'تعذر الاتصال بالسحابة للتحقق من الكود.' };
    }

    if (!data) {
      // If not in Supabase yet (e.g. issued offline from phone), test unified algorithm
      if (isValidUnifiedLicenseKey(cleanKey)) {
        const fallbackLic: LicenseRecord = {
          license_key: cleanKey,
          school_name: schoolNameInput || 'مدرستنا الكريمة',
          machine_fingerprint: currentFp,
          is_activated: true,
          activated_at: new Date().toISOString()
        };
        localStorage.setItem(LOCAL_LICENSE_STORAGE_KEY, JSON.stringify(fallbackLic));
        return {
          success: true,
          message: 'تهانينا! تم التحقق من الكود المعتمد وتفعيل المنظومة بنجاح 💎✓',
          license: fallbackLic
        };
      }
      return { success: false, message: 'كود التفعيل المدخل غير صحيح، يرجى التأكد من الرمز.' };
    }

    // 2. Check if already activated on another machine
    if (data.is_activated && data.machine_fingerprint && data.machine_fingerprint !== currentFp) {
      return {
        success: false,
        message: 'عذراً، هذا الكود مستخدم ومربوط بحاسوب آخر بالفعل. تواصل مع الدعم لإعادة التعيين.'
      };
    }

    // 3. Mark as activated in Supabase and bind machine fingerprint
    const updatePayload = {
      is_activated: true,
      machine_fingerprint: currentFp,
      activated_at: data.activated_at || new Date().toISOString(),
      school_name: schoolNameInput?.trim() || data.school_name
    };

    const { error: updateErr } = await supabase
      .from('desktop_licenses')
      .update(updatePayload)
      .eq('license_key', cleanKey);

    if (updateErr) {
      console.warn('Failed to update remote activation:', updateErr);
    }

    const activatedRecord: LicenseRecord = {
      ...data,
      ...updatePayload
    };

    // Save locally for offline permanence
    localStorage.setItem(LOCAL_LICENSE_STORAGE_KEY, JSON.stringify(activatedRecord));

    return {
      success: true,
      message: 'تهانينا! تم تفعيل المنظومة لمرة واحدة مدى الحياة بنجاح 💎✓',
      license: activatedRecord
    };
  } catch (err: any) {
    return {
      success: false,
      message: err?.message || 'حدث خطأ غير متوقع أثناء التفعيل.'
    };
  }
}

/**
 * Generate a new license key (Used by the Owner to issue keys to schools).
 */
export async function createNewLicenseKey(
  schoolName: string,
  contactPhone?: string,
  notes?: string
): Promise<{ success: boolean; licenseKey?: string; message: string }> {
  if (!schoolName.trim()) {
    return { success: false, message: 'يرجى إدخال اسم المدرسة المستفيدة.' };
  }

  // Format: BOSS-XXXX-XXXX-XXXX
  const p1 = Math.floor(1000 + Math.random() * 9000);
  const p2 = Math.floor(1000 + Math.random() * 9000);
  const p3 = Math.floor(1000 + Math.random() * 9000);
  const newKey = `BOSS-${p1}-${p2}-${p3}`;

  const supabase = getSupabase();

  try {
    const { error } = await supabase.from('desktop_licenses').insert([
      {
        license_key: newKey,
        school_name: schoolName.trim(),
        contact_phone: contactPhone?.trim() || null,
        notes: notes?.trim() || 'مدفوع لمرة واحدة',
        is_activated: false
      }
    ]);

    if (error) {
      throw error;
    }

    return {
      success: true,
      licenseKey: newKey,
      message: 'تم إنشاء كود التفعيل وحفظه في السحابة بنجاح ✓'
    };
  } catch (e: any) {
    return {
      success: false,
      message: e?.message || 'تعذر حفظ كود التفعيل الجديد في السحابة.'
    };
  }
}
