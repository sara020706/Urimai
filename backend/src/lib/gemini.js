/**
 * Server-side Gemini client.
 *
 * The API key lives ONLY here, in backend environment. It previously shipped
 * inside the APK via the Secrets Gradle plugin, which made it public to anyone
 * who decompiled a build — an unbounded billing liability, since a leaked key
 * is a key someone else can spend.
 *
 * Everything the app used to call directly now proxies through authenticated
 * endpoints, so the key is never transmitted to a client.
 */

const DEFAULT_MODEL = process.env.GEMINI_MODEL || 'gemini-3.5-flash';
const BASE_URL = 'https://generativelanguage.googleapis.com/v1beta/models';

// Gemini latency is unbounded. Without a ceiling a slow upstream call would
// hold a request (and its pg pool slot) open indefinitely.
const TIMEOUT_MS = Number(process.env.GEMINI_TIMEOUT_MS || 25000);

class GeminiNotConfigured extends Error {
  constructor() {
    super('GEMINI_API_KEY is not set.');
    this.code = 'GEMINI_NOT_CONFIGURED';
  }
}

class GeminiFailed extends Error {
  constructor(message, status) {
    super(message);
    this.code = 'GEMINI_FAILED';
    this.upstreamStatus = status;
  }
}

function isConfigured() {
  const key = String(process.env.GEMINI_API_KEY || '').trim();
  // The Secrets plugin used to inject this placeholder when no real .env
  // existed, so a blank check alone would let placeholder calls through and
  // fail confusingly at the network layer.
  return key.length > 0 && key.toUpperCase() !== 'MY_GEMINI_API_KEY';
}

/**
 * Call generateContent.
 *
 * `parts` is the raw parts array, so a caller can mix text with inline_data for
 * vision requests. Returns the first candidate's text.
 */
async function generateContent(parts, options = {}) {
  if (!isConfigured()) throw new GeminiNotConfigured();

  const body = {
    contents: [{ parts }],
    generationConfig: {
      temperature: options.temperature != null ? options.temperature : 0.4,
      maxOutputTokens: options.maxOutputTokens || 2048
    }
  };
  if (options.responseMimeType) {
    body.generationConfig.responseMimeType = options.responseMimeType;
  }
  if (options.systemInstruction) {
    body.systemInstruction = { parts: [{ text: options.systemInstruction }] };
  }

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);

  let response;
  try {
    response = await fetch(
      `${BASE_URL}/${options.model || DEFAULT_MODEL}:generateContent`,
      {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
          // Header rather than query string: a key in a URL ends up in access
          // logs and proxy traces.
          'x-goog-api-key': String(process.env.GEMINI_API_KEY).trim()
        },
        body: JSON.stringify(body),
        signal: controller.signal
      }
    );
  } catch (err) {
    if (err.name === 'AbortError') {
      throw new GeminiFailed('The AI service took too long to respond.', 504);
    }
    throw new GeminiFailed('Could not reach the AI service.', 502);
  } finally {
    clearTimeout(timer);
  }

  if (!response.ok) {
    // Logged server-side only. The response body still never reaches the
    // client (it can quote the request, and on an auth failure it may name the
    // key), but without this an operator sees only a generic 503 and cannot
    // tell a bad model name from a bad key from a quota block.
    let detail = '';
    try {
      const body = await response.json();
      detail = (body && body.error && body.error.message) || '';
    } catch (_) {
      detail = '';
    }
    console.error(
      '[gemini] upstream %d for model %s: %s',
      response.status,
      options.model || DEFAULT_MODEL,
      String(detail).slice(0, 300)
    );
    throw new GeminiFailed('The AI service rejected the request.', response.status);
  }

  const payload = await response.json();
  const text = payload &&
    payload.candidates &&
    payload.candidates[0] &&
    payload.candidates[0].content &&
    payload.candidates[0].content.parts &&
    payload.candidates[0].content.parts[0] &&
    payload.candidates[0].content.parts[0].text;

  if (!text) throw new GeminiFailed('The AI service returned no usable content.', 502);
  return text;
}

/**
 * Which models this API key can actually use.
 *
 * Exists because a wrong GEMINI_MODEL and a wrong GEMINI_API_KEY both surface
 * as the same generic 503, and there is otherwise no way to tell them apart
 * without the key in hand.
 */
async function listModels() {
  if (!isConfigured()) throw new GeminiNotConfigured();

  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), TIMEOUT_MS);
  try {
    const response = await fetch('https://generativelanguage.googleapis.com/v1beta/models', {
      headers: { 'x-goog-api-key': String(process.env.GEMINI_API_KEY).trim() },
      signal: controller.signal
    });
    if (!response.ok) {
      throw new GeminiFailed('Could not list models.', response.status);
    }
    const body = await response.json();
    return (body.models || [])
      // Only models that can actually serve generateContent.
      .filter((m) => (m.supportedGenerationMethods || []).includes('generateContent'))
      .map((m) => String(m.name || '').replace(/^models\//, ''));
  } finally {
    clearTimeout(timer);
  }
}

/** Strip markdown fences a model sometimes wraps JSON in, then parse. */
function parseJsonResponse(raw) {
  const clean = String(raw)
    .trim()
    .replace(/^```json/i, '')
    .replace(/^```/, '')
    .replace(/```$/, '')
    .trim();
  return JSON.parse(clean);
}

module.exports = {
  isConfigured,
  listModels,
  generateContent,
  parseJsonResponse,
  GeminiNotConfigured,
  GeminiFailed,
  DEFAULT_MODEL
};
