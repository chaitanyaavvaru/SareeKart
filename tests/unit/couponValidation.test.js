/**
 * Unit tests for coupon management UI polish – bulk, expiry, limits, CSV, search, validation
 * Mirrors logic in ManageCoupons.jsx
 */

function validateCoupon({ code, discountValue, discountType, minimumOrderAmount, maximumDiscountAmount, validFrom, validUntil, totalUsageLimit, perUserUsageLimit }) {
  const errs = {};
  const c = (code || '').trim().toUpperCase();
  if (!c) errs.code = 'Code is required';
  else if (!/^[A-Z0-9_-]{1,50}$/.test(c)) errs.code = 'A-Z, 0-9, hyphen, underscore only';
  if (!discountValue || Number(discountValue) <= 0) errs.value = 'Discount value must be > 0';
  if (discountType === 'PERCENTAGE' && Number(discountValue) > 100) errs.value = 'Percentage cannot exceed 100';
  if (minimumOrderAmount !== '' && minimumOrderAmount != null && Number(minimumOrderAmount) < 0) errs.minOrder = 'Cannot be negative';
  if (maximumDiscountAmount !== '' && maximumDiscountAmount != null && Number(maximumDiscountAmount) <= 0) errs.maxDiscount = 'Must be > 0 if set';
  if (validFrom && validUntil && new Date(validUntil) <= new Date(validFrom)) errs.validUntil = 'Must be after Valid From';
  if (totalUsageLimit !== '' && totalUsageLimit != null && (!Number.isInteger(Number(totalUsageLimit)) || Number(totalUsageLimit) <= 0)) errs.totalLimit = 'Must be a positive integer';
  if (perUserUsageLimit !== '' && perUserUsageLimit != null && (!Number.isInteger(Number(perUserUsageLimit)) || Number(perUserUsageLimit) <= 0)) errs.perUserLimit = 'Must be a positive integer';
  return errs;
}

function filterCoupons(coupons, q) {
  if (!q.trim()) return coupons;
  const lower = q.trim().toLowerCase();
  return coupons.filter(c => c.code.toLowerCase().includes(lower) || (c.description || '').toLowerCase().includes(lower));
}

function parseCsv(text) {
  const lines = text.split(/\r?\n/).filter(l => l.trim() !== '');
  if (lines.length < 2) throw new Error('CSV must have header and at least one data row');
  const headers = lines[0].split(',').map(h => h.trim().toLowerCase());
  if (!headers.includes('code')) throw new Error('Missing required header: code');
  return lines.slice(1).map(line => {
    const cols = line.split(',').map(c => c.trim());
    const obj = {};
    headers.forEach((h, i) => obj[h] = cols[i] ?? '');
    return obj;
  });
}

describe('Coupon validation', () => {
  test('valid code passes', () => {
    expect(validateCoupon({ code: 'WELCOME20', discountValue: '20', discountType: 'PERCENTAGE' })).toEqual({});
  });
  test('invalid code format', () => {
    expect(validateCoupon({ code: 'bad code!', discountValue: '10', discountType: 'PERCENTAGE' }).code).toBeDefined();
  });
  test('percentage cannot exceed 100', () => {
    expect(validateCoupon({ code: 'TEST', discountValue: '150', discountType: 'PERCENTAGE' }).value).toMatch(/100/);
  });
  test('expiry date ordering', () => {
    expect(validateCoupon({ code: 'TEST', discountValue: '10', discountType: 'FIXED_AMOUNT', validFrom: '2026-12-31T00:00', validUntil: '2026-01-01T00:00' }).validUntil).toBeDefined();
  });
  test('usage limits must be positive integers', () => {
    expect(validateCoupon({ code: 'TEST', discountValue: '10', discountType: 'FIXED_AMOUNT', totalUsageLimit: '0' }).totalLimit).toBeDefined();
    expect(validateCoupon({ code: 'TEST', discountValue: '10', discountType: 'FIXED_AMOUNT', perUserUsageLimit: '1.5' }).perUserLimit).toBeDefined();
  });
});

describe('Coupon search', () => {
  const data = [
    { code: 'WELCOME20', description: '20% off for new users' },
    { code: 'FLAT500', description: 'Flat 500 off' },
  ];
  test('filters by code', () => {
    expect(filterCoupons(data, 'welcome')).toHaveLength(1);
  });
  test('filters by description', () => {
    expect(filterCoupons(data, 'flat')).toHaveLength(1);
  });
  test('empty query returns all', () => {
    expect(filterCoupons(data, '')).toHaveLength(2);
  });
});

describe('CSV import', () => {
  test('parses valid csv', () => {
    const csv = 'code,description,discountType,discountValue\nWELCOME20,Test,PERCENTAGE,20\nFLAT500,,FIXED_AMOUNT,500';
    const rows = parseCsv(csv);
    expect(rows).toHaveLength(2);
    expect(rows[0].code).toBe('WELCOME20');
  });
  test('throws on missing code header', () => {
    expect(() => parseCsv('description,discountType\nTest,PERCENTAGE')).toThrow(/code/);
  });
  test('bulk selection logic', () => {
    const ids = [1,2,3];
    const selected = new Set([1,2]);
    const allSelected = ids.every(id => selected.has(id));
    expect(allSelected).toBe(false);
    selected.add(3);
    expect(ids.every(id => selected.has(id))).toBe(true);
  });
});
