const { rateLimit, ipKeyGenerator } = require('express-rate-limit');

const common = {
  standardHeaders: true,
  legacyHeaders: false,
  message: { error: 'Too many requests. Please try again shortly.', code: 'RATE_LIMITED' }
};

// Brute-force protection on credential endpoints. Keyed by IP: the account
// being targeted is attacker-controlled, so keying by username would let an
// attacker spread attempts across many usernames from one host.
//
// RATE_LIMIT_DISABLED exists so the integration suites, which create several
// accounts per run from one host, do not exhaust the window after a couple of
// runs. It is opt-in via env and never set in production; the limiter is
// otherwise always active, including in development.
const authLimiter = rateLimit({
  ...common,
  windowMs: 15 * 60 * 1000,
  limit: 20,
  skip: () => String(process.env.RATE_LIMIT_DISABLED || '') === 'true'
});

// Per-user limits for endpoints that cost money or create content.
function perUser(limit, windowMs) {
  return rateLimit({
    ...common,
    windowMs,
    limit,
    keyGenerator: (req, res) => (req.userId != null ? `u:${req.userId}` : ipKeyGenerator(req, res))
  });
}

const questionLimiter = perUser(10, 60 * 60 * 1000);
const ocrLimiter = perUser(10, 60 * 60 * 1000);
const contactLimiter = perUser(20, 24 * 60 * 60 * 1000);

module.exports = { authLimiter, questionLimiter, ocrLimiter, contactLimiter, perUser };
