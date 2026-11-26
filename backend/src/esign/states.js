/** Signature request lifecycle shared by every provider and by the Android app (ESign.java). */
export const SENT = 'sent';
export const VIEWED = 'viewed';
export const COMPLETED = 'completed';
export const DECLINED = 'declined';
export const EXPIRED = 'expired';
export const VOIDED = 'voided';

export const PENDING = new Set([SENT, VIEWED]);
