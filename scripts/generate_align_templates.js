const fs = require('fs');

const srcPath = 'trusted-taxpayers-rs/src/main/resources/db/migration/V25__refresh_templates_rc_iitp_v7_6.sql';
const text = fs.readFileSync(srcPath, 'utf8');
const tplRegex = /VALUES \('([^']+)',\s*N'([^']+)',\s*'(P\d{2})',\s*'([^']*)',\s*'([^']*)',\s*'Y',\s*1\);/g;
const templates = {};
let match;
while ((match = tplRegex.exec(text))) {
  const [_, code, name, typeCode, requiredStr, rulesStr] = match;
  const required = requiredStr.split(',').map((s) => s.trim()).filter(Boolean);
  let patternNumber = null;
  try {
    patternNumber = JSON.parse(rulesStr).pattern;
  } catch (err) {
    patternNumber = null;
  }
  templates[code] = { code, name, typeCode, patternNumber, required };
}

const commonAdds = ['ins', 'tprdis', 'tadis'];
const ensureCommon = ['taxId', 'indatim', 'inno', 'inty', 'inp', 'tins', 'tbill'];
const orderPreference = ['taxId', 'indatim', 'inno', 'inty', 'ins', 'inp', 'tins', 'tinb', 'tob', 'bid', 'billid', 'ft', 'tocv', 'lt', 'setm', 'crn', 'tprdis', 'tadis', 'tbill'];

for (const tpl of Object.values(templates)) {
  const requiredSet = new Set(tpl.required);
  ensureCommon.forEach((f) => requiredSet.add(f));
  commonAdds.forEach((f) => requiredSet.add(f));
  tpl.required = requiredSet;
}

function orderedFields(tpl) {
  const result = [];
  const pending = new Set(tpl.required);
  for (const key of orderPreference) {
    if (pending.has(key)) {
      result.push(key);
      pending.delete(key);
    }
  }
  for (const key of tpl.required) {
    if (!result.includes(key)) {
      result.push(key);
    }
  }
  return result;
}

const templateRules = {};
const templateRuleKeys = {};

function addRule(code, field, type, params, explanation) {
  if (!templateRules[code]) templateRules[code] = [];
  if (!templateRuleKeys[code]) templateRuleKeys[code] = new Set();
  const key = `${code}|${field}|${type}|${params ?? ''}`;
  if (templateRuleKeys[code].has(key)) return;
  templateRuleKeys[code].add(key);
  templateRules[code].push({ field, type, params, explanation });
}

const messages = {
  taxId: { required: 'Ø´Ù…Ø§Ø±Ù‡ Ù…Ø§Ù„ÛŒØ§ØªÛŒ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù…Ø§Ø±Ù‡ Ù…Ø§Ù„ÛŒØ§ØªÛŒ Ø¨Ø§ÛŒØ¯ ØµØ±ÙØ§Ù‹ Ø´Ø§Ù…Ù„ Ø­Ø±ÙˆÙ Ùˆ Ø§Ø±Ù‚Ø§Ù… Ø¨Ø§Ø´Ø¯' },
  indatim: { required: 'ØªØ§Ø±ÛŒØ® Ùˆ Ø²Ù…Ø§Ù† ØµØ¯ÙˆØ± ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' },
  inno: { required: 'Ø´Ù…Ø§Ø±Ù‡ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù…Ø§Ø±Ù‡ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø¨Ø§ÛŒØ¯ Ø­Ø¯Ø§Ú©Ø«Ø± Û±Û° Ú©Ø§Ø±Ø§Ú©ØªØ± Ùˆ Ø´Ø§Ù…Ù„ Ø­Ø±ÙˆÙ ÛŒØ§ Ø§Ø±Ù‚Ø§Ù… Ø¨Ø§Ø´Ø¯' },
  inty: { required: 'Ù†ÙˆØ¹ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', enum: 'Ù†ÙˆØ¹ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  ins: { required: 'Ù…ÙˆØ¶ÙˆØ¹ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', enum: 'Ù…ÙˆØ¶ÙˆØ¹ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  inp: { required: 'Ø´Ù…Ø§Ø±Ù‡ Ø§Ù„Ú¯ÙˆÛŒ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', enum: 'Ø´Ù…Ø§Ø±Ù‡ Ø§Ù„Ú¯Ùˆ Ø¨Ø§ÛŒØ¯ Ø¨Ø§ Ø§Ù„Ú¯ÙˆÛŒ Ø§Ù†ØªØ®Ø§Ø¨â€ŒØ´Ø¯Ù‡ Ù‡Ù…Ø®ÙˆØ§Ù†ÛŒ Ø¯Ø§Ø´ØªÙ‡ Ø¨Ø§Ø´Ø¯' },
  tins: { required: 'Ø´Ù†Ø§Ø³Ù‡ ÙØ±ÙˆØ´Ù†Ø¯Ù‡ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù†Ø§Ø³Ù‡ ÙØ±ÙˆØ´Ù†Ø¯Ù‡ Ø¨Ø§ÛŒØ¯ Û±Û° ØªØ§ Û±Û´ Ø±Ù‚Ù… Ø¨Ø§Ø´Ø¯' },
  tinb: { required: 'Ø´Ù†Ø§Ø³Ù‡ Ø®Ø±ÛŒØ¯Ø§Ø± Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù†Ø§Ø³Ù‡ Ø®Ø±ÛŒØ¯Ø§Ø± Ø¨Ø§ÛŒØ¯ Û±Û° ØªØ§ Û±Û´ Ø±Ù‚Ù… Ø¨Ø§Ø´Ø¯' },
  tob: { required: 'Ù†ÙˆØ¹ Ø®Ø±ÛŒØ¯Ø§Ø± Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', enum: 'Ù†ÙˆØ¹ Ø®Ø±ÛŒØ¯Ø§Ø± Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  bid: { required: 'Ø´Ù†Ø§Ø³Ù‡ Ø®Ø±ÛŒØ¯Ø§Ø± Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù†Ø§Ø³Ù‡ Ø®Ø±ÛŒØ¯Ø§Ø± Ø¨Ø§ÛŒØ¯ Û±Û° ØªØ§ Û±Û² Ø±Ù‚Ù… Ø¨Ø§Ø´Ø¯' },
  setm: { required: 'Ù†ÙˆØ¹ ØªØ³ÙˆÛŒÙ‡ Ø¨Ø§ÛŒØ¯ Ù…Ø´Ø®Øµ Ø´ÙˆØ¯', enum: 'Ù†ÙˆØ¹ ØªØ³ÙˆÛŒÙ‡ Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  billid: { required: 'Ø´Ù†Ø§Ø³Ù‡ Ù‚Ø¨Ø¶ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù†Ø§Ø³Ù‡ Ù‚Ø¨Ø¶ Ø¨Ø§ÛŒØ¯ Û¶ ØªØ§ Û±Û¹ Ø±Ù‚Ù… Ø¨Ø§Ø´Ø¯' },
  ft: { required: 'Ù†ÙˆØ¹ Ù¾Ø±ÙˆØ§Ø² Ø¨Ø§ÛŒØ¯ Ù…Ø´Ø®Øµ Ø´ÙˆØ¯', enum: 'Ù†ÙˆØ¹ Ù¾Ø±ÙˆØ§Ø² Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  tocv: { required: 'Ø§Ø±Ø²Ø´ Ø§Ø±Ø²ÛŒ ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', range: 'Ø§Ø±Ø²Ø´ Ø§Ø±Ø²ÛŒ Ø¨Ø§ÛŒØ¯ Ù…Ù‚Ø¯Ø§Ø± ØºÛŒØ± Ù…Ù†ÙÛŒ Ø¨Ø§Ø´Ø¯' },
  lt: { required: 'Ù†ÙˆØ¹ Ø­Ù…Ù„ Ø¨Ø§ÛŒØ¯ Ù…Ø´Ø®Øµ Ø´ÙˆØ¯', enum: 'Ù†ÙˆØ¹ Ø­Ù…Ù„ Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯' },
  crn: { required: 'Ø´Ù…Ø§Ø±Ù‡ Ù‚Ø±Ø§Ø±Ø¯Ø§Ø¯ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ø´Ù…Ø§Ø±Ù‡ Ù‚Ø±Ø§Ø±Ø¯Ø§Ø¯ Ø¨Ø§ÛŒØ¯ Ø­Ø¯Ø§Ú©Ø«Ø± Û±Û² Ú©Ø§Ø±Ø§Ú©ØªØ± Ù…Ø¹ØªØ¨Ø± Ø¨Ø§Ø´Ø¯' },
  tprdis: { required: 'Ø¬Ù…Ø¹ Ù…Ø¨Ù„Øº Ù‚Ø¨Ù„ Ø§Ø² ØªØ®ÙÛŒÙ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', range: 'Ø¬Ù…Ø¹ Ù…Ø¨Ù„Øº Ù‚Ø¨Ù„ Ø§Ø² ØªØ®ÙÛŒÙ Ø¨Ø§ÛŒØ¯ ØºÛŒØ± Ù…Ù†ÙÛŒ Ø¨Ø§Ø´Ø¯' },
  tadis: { required: 'Ø¬Ù…Ø¹ Ù…Ø¨Ù„Øº Ø¨Ø¹Ø¯ Ø§Ø² ØªØ®ÙÛŒÙ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', range: 'Ø¬Ù…Ø¹ Ù…Ø¨Ù„Øº Ø¨Ø¹Ø¯ Ø§Ø² ØªØ®ÙÛŒÙ Ø¨Ø§ÛŒØ¯ ØºÛŒØ± Ù…Ù†ÙÛŒ Ø¨Ø§Ø´Ø¯' },
  tbill: { required: 'Ù…Ø¨Ù„Øº ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', range: 'Ù…Ø¨Ù„Øº ØµÙˆØ±ØªØ­Ø³Ø§Ø¨ Ø¨Ø§ÛŒØ¯ ØºÛŒØ± Ù…Ù†ÙÛŒ Ø¨Ø§Ø´Ø¯' },
  scc: { regex: 'Ú©Ø¯ Ú¯Ù…Ø±Ú©ÛŒ Ø¨Ø§ÛŒØ¯ Ûµ Ø±Ù‚Ù… Ø¨Ø§Ø´Ø¯' },
  scln: { regex: 'Ø´Ù…Ø§Ø±Ù‡ Ù…Ø¬ÙˆØ² Ú¯Ù…Ø±Ú©ÛŒ Ø¨Ø§ÛŒØ¯ Ûµ ØªØ§ Û±Û´ Ú©Ø§Ø±Ø§Ú©ØªØ± Ø¨Ø§Ø´Ø¯' },
  cut: { required: 'Ù†ÙˆØ¹ Ø§Ø±Ø² Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª', regex: 'Ù†ÙˆØ¹ Ø§Ø±Ø² Ø¨Ø§ÛŒØ¯ Ù…Ø·Ø§Ø¨Ù‚ Ø§Ø³ØªØ§Ù†Ø¯Ø§Ø±Ø¯ ISO 4217 Ø¨Ø§Ø´Ø¯' },
  cfee: { required: 'ÙÛŒ ÙˆØ§Ø­Ø¯ Ø§Ø±Ø²ÛŒ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' },
  exr: { required: 'Ù†Ø±Ø® ØªØ¨Ø¯ÛŒÙ„ Ø§Ø±Ø² Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' },
  cui: { required: 'Ø¹ÛŒØ§Ø± Ø·Ù„Ø§ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' },
  nw: { required: 'ÙˆØ²Ù† Ø®Ø§Ù„Øµ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' },
  ssrv: { required: 'Ø§Ø±Ø²Ø´ Ø±ÛŒØ§Ù„ÛŒ Ú©Ø§Ù„Ø§ Ø§Ù„Ø²Ø§Ù…ÛŒ Ø§Ø³Øª' }
};

const regexByField = {
  taxId: '^[A-Za-z0-9]{22}$',
  inno: '^[A-Za-z0-9-]{1,10}$',
  tins: '^[0-9]{10,14}$',
  tinb: '^[0-9]{10,14}$',
  bid: '^[0-9]{10,12}$',
  billid: '^[0-9]{6,19}$',
  crn: '^[A-Za-z0-9-]{1,12}$',
  scc: '^[0-9]{5}$',
  scln: '^[A-Za-z0-9]{5,14}$',
  cut: '^[A-Z]{3}$'
};

const enumByField = {
  inty: '1,2,3',
  ins: '1,2,3,4',
  tob: '1,2,3,4',
  setm: '1,2,3',
  ft: '1,2',
  lt: '1,2,3,4'
};

const rangeByField = {
  tprdis: '0:',
  tadis: '0:',
  tbill: '0:',
  tocv: '0:'
};

const templateSpecificRequirements = {
  T01_RETAIL: ['tob', 'bid'],
  T01_WHOLE: ['tob', 'tinb', 'setm'],
  T02_FX: ['tob', 'tocv'],
  T03_GOLD: ['tob'],
  T03_JEWEL: ['tob'],
  T04_CONTR: ['tob', 'tinb', 'crn'],
  T05_UTIL: ['tob', 'billid'],
  T06_AIR: ['tob', 'ft'],
  T07_EXPORT: ['tob', 'tocv'],
  T08_WAY: ['lt'],
  T09_PETROL: ['tob'],
  T11_STOCK: ['tob', 'tinb'],
  T13_INS: ['tob', 'bid']
};

for (const tpl of Object.values(templates)) {
  const merged = new Set(tpl.required);
  const extras = templateSpecificRequirements[tpl.code] || [];
  extras.forEach((f) => merged.add(f));
  tpl.required = merged;
  tpl.requiredOrdered = orderedFields(tpl);
}

function ensureRequired(code, field) {
  const msg = messages[field];
  if (msg && msg.required) {
    addRule(code, field, 'REQUIRED', null, msg.required);
  }
}

function ensureRegex(code, field) {
  const pattern = regexByField[field];
  if (pattern) {
    const msg = messages[field];
    const explanation = (msg && msg.regex) ? msg.regex : 'ÙØ±Ù…Øª ÙÛŒÙ„Ø¯ Ù…Ø¹ØªØ¨Ø± Ù†ÛŒØ³Øª';
    addRule(code, field, 'REGEX', pattern, explanation);
  }
}

function ensureEnum(code, field, values) {
  const msg = messages[field];
  const params = values || enumByField[field];
  if (params) {
    const explanation = (msg && msg.enum) ? msg.enum : 'Ù…Ù‚Ø¯Ø§Ø± ÙÛŒÙ„Ø¯ Ø¨Ø§ÛŒØ¯ ÛŒÚ©ÛŒ Ø§Ø² Ù…Ù‚Ø§Ø¯ÛŒØ± Ù…Ø¬Ø§Ø² Ø¨Ø§Ø´Ø¯';
    addRule(code, field, 'ENUM', params, explanation);
  }
}

function ensureRange(code, field) {
  const range = rangeByField[field];
  if (range) {
    const msg = messages[field];
    const explanation = (msg && msg.range) ? msg.range : 'Ù…Ù‚Ø¯Ø§Ø± Ø¨Ø§ÛŒØ¯ ØºÛŒØ± Ù…Ù†ÙÛŒ Ø¨Ø§Ø´Ø¯';
    addRule(code, field, 'RANGE', range, explanation);
  }
}

for (const tpl of Object.values(templates)) {
  const code = tpl.code;
  const pattern = tpl.patternNumber;

  ['taxId', 'indatim', 'inno', 'inty', 'ins', 'inp', 'tins', 'tprdis', 'tadis', 'tbill'].forEach((f) => ensureRequired(code, f));
  ['taxId', 'inno', 'tins'].forEach((f) => ensureRegex(code, f));
  ensureEnum(code, 'inty');
  ensureEnum(code, 'ins');
  if (pattern != null) {
    ensureEnum(code, 'inp', String(pattern));
  }
  ['tprdis', 'tadis', 'tbill'].forEach((f) => ensureRange(code, f));

  const specifics = templateSpecificRequirements[code] || [];
  specifics.forEach((field) => {
    ensureRequired(code, field);
    if (enumByField[field]) ensureEnum(code, field);
    if (regexByField[field]) ensureRegex(code, field);
    if (rangeByField[field]) ensureRange(code, field);
  });

  if (code === 'T02_FX') {
    ['cut', 'cfee', 'exr'].forEach((field) => {
      ensureRequired(code, field);
      if (field === 'cut') ensureRegex(code, field);
    });
    ['scc', 'scln'].forEach((field) => ensureRegex(code, field));
    ensureRange(code, 'tocv');
  }

  if (code.startsWith('T03_')) {
    ['cui', 'nw', 'ssrv'].forEach((field) => ensureRequired(code, field));
  }

  if (code === 'T05_UTIL') {
    ensureRegex(code, 'billid');
  }

  if (code === 'T06_AIR') {
    ensureEnum(code, 'ft');
  }

  if (code === 'T07_EXPORT') {
    ensureRange(code, 'tocv');
  }

  if (code === 'T08_WAY') {
    ensureEnum(code, 'lt');
  }

  if (code === 'T11_STOCK') {
    ensureRegex(code, 'tinb');
  }

  if (code === 'T13_INS') {
    ensureRegex(code, 'bid');
  }
}

const lines = [];
lines.push('-- ============================================================');
lines.push('-- V32: Align template definitions with RC_IITP_IS_V7_6');
lines.push('-- Auto-generated by script');
lines.push('-- ============================================================');
lines.push('BEGIN');
const codes = Object.keys(templates).map((c) => `'${c}'`).join(', ');
lines.push(`  DELETE FROM TEMPLATE_RULE WHERE TEMPLATE_CODE IN (${codes});`);
lines.push(`  DELETE FROM TEMPLATE WHERE CODE IN (${codes});`);
lines.push('EXCEPTION WHEN OTHERS THEN NULL;');
lines.push('END;');
lines.push('/');
lines.push('');

for (const tpl of Object.values(templates)) {
  const requiredStr = tpl.requiredOrdered.join(',');
  const rulesJson = JSON.stringify({ pattern: tpl.patternNumber, standard: 'RC_IITP_IS_V7_6' }).replace(/'/g, "''");
  const name = tpl.name.replace(/'/g, "''");
  lines.push('INSERT INTO TEMPLATE (CODE, NAME, TEMPLATE_TYPE_CODE, REQUIRED_FIELDS, RULES, ACTIVE, IS_ACTIVE)');
  lines.push(`VALUES ('${tpl.code}', N'${name}', '${tpl.typeCode}', '${requiredStr}', '${rulesJson}', 'Y', 1);`);
  lines.push('');
}

for (const [code, rules] of Object.entries(templateRules)) {
  for (const rule of rules) {
    const params = rule.params == null ? 'NULL' : `'${rule.params.replace(/'/g, "''")}'`;
    const explanation = rule.explanation ? `N'${rule.explanation.replace(/'/g, "''")}'` : 'NULL';
    lines.push('INSERT INTO TEMPLATE_RULE (TEMPLATE_CODE, FIELD_NAME, RULE_TYPE, RULE_PARAMS, EXPLANATION, ACTIVE)');
    lines.push(`VALUES ('${code}', '${rule.field}', '${rule.type}', ${params}, ${explanation}, 'Y');`);
  }
  lines.push('');
}

const outPath = 'trusted-taxpayers-rs/src/main/resources/db/migration/V32__align_templates_rc_iitp_v7_6.sql';
fs.writeFileSync(outPath, lines.join('\n'), 'utf8');
console.log('Generated', outPath);
