const jwt = require('jsonwebtoken');
const pool = require('./../db/pool');
const { wrap } = require('../lib/async');

function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) {
    return res.status(401).json({ error: 'Missing authorization token.' });
  }
  try {
    const payload = jwt.verify(token, process.env.JWT_SECRET);
    req.userId = payload.userId;
    req.tokenIssuedAt = payload.iat;
    next();
  } catch (_err) {
    return res.status(401).json({ error: 'Invalid or expired token.' });
  }
}

/**
 * Role and account status are read from the database on every request rather
 * than carried in the JWT.
 *
 * Tokens live 30 days. Putting role/status in the token would mean a blocked
 * user or suspended lawyer keeps full access until their token expires — up to
 * a month after the decision. That is not enforcement. One indexed primary-key
 * lookup is cheap next to the Neon round-trip every endpoint already pays.
 *
 * `token_valid_from` additionally gives real revocation, which a stateless JWT
 * cannot do at all: bumping it invalidates every token issued before that
 * moment.
 */
async function loadUser(req, res, next) {
  const { rows } = await pool.query(
    `SELECT u.id, u.username, u.display_name, u.role, u.account_status,
            u.status_reason, u.token_valid_from,
            lp.verification_status AS lawyer_status
       FROM users u
       LEFT JOIN lawyer_profiles lp ON lp.user_id = u.id
      WHERE u.id = $1`,
    [req.userId]
  );

  const user = rows[0];
  if (!user) {
    return res.status(401).json({ error: 'Account no longer exists.', code: 'ACCOUNT_MISSING' });
  }

  // JWT `iat` is in seconds; token_valid_from is a timestamp.
  if (req.tokenIssuedAt != null) {
    const validFromSeconds = Math.floor(new Date(user.token_valid_from).getTime() / 1000);
    if (req.tokenIssuedAt < validFromSeconds) {
      return res.status(401).json({ error: 'Session has been revoked. Please sign in again.', code: 'TOKEN_REVOKED' });
    }
  }

  req.user = user;
  next();
}

function requireActiveAccount(req, res, next) {
  const status = req.user.account_status;
  if (status === 'ACTIVE') return next();

  if (status === 'BLOCKED') {
    return res.status(403).json({
      error: req.user.status_reason || 'This account has been blocked.',
      code: 'ACCOUNT_BLOCKED'
    });
  }
  return res.status(403).json({
    error: req.user.status_reason || 'This account is currently suspended.',
    code: 'ACCOUNT_SUSPENDED'
  });
}

function requireRole(...roles) {
  return function roleGuard(req, res, next) {
    if (roles.includes(req.user.role)) return next();
    return res.status(403).json({ error: 'You do not have access to this resource.', code: 'FORBIDDEN_ROLE' });
  };
}

/**
 * Lawyer capability gate.
 *
 * Always use this, never requireRole('LAWYER'). Signup lets a client
 * self-assert role LAWYER, which is only safe because the role grants nothing
 * until an admin verifies it. Gating a capability on the role alone would
 * collapse that model.
 */
function requireVerifiedLawyer(req, res, next) {
  if (req.user.role !== 'LAWYER') {
    return res.status(403).json({ error: 'This resource is for legal specialists.', code: 'FORBIDDEN_ROLE' });
  }
  if (req.user.lawyer_status !== 'VERIFIED') {
    return res.status(403).json({
      error: 'Your professional verification is not yet approved.',
      code: 'LAWYER_NOT_VERIFIED',
      verificationStatus: req.user.lawyer_status || 'PENDING'
    });
  }
  next();
}

// Composed chains. Routers mount these so a route cannot accidentally omit a
// layer — in particular requireActiveAccount, whose absence would let a blocked
// user keep operating.
const authed = [requireAuth, wrap(loadUser), requireActiveAccount];
const lawyerOnly = [...authed, requireVerifiedLawyer];
const adminOnly = [...authed, requireRole('ADMIN')];

module.exports = {
  requireAuth,
  loadUser,
  requireActiveAccount,
  requireRole,
  requireVerifiedLawyer,
  authed,
  lawyerOnly,
  adminOnly
};
