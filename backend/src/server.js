require('dotenv').config();
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/auth');
const profileRoutes = require('./routes/profile');
const documentRoutes = require('./routes/documents');
const savedSchemesRoutes = require('./routes/savedSchemes');
const schemeRoutes = require('./routes/schemes');
const lawyerRoutes = require('./routes/lawyer');
const adminRoutes = require('./routes/admin');
const notificationRoutes = require('./routes/notifications');
const questionRoutes = require('./routes/questions');
const lawyerQuestionRoutes = require('./routes/lawyerQuestions');
const reportRoutes = require('./routes/reports');
const directoryRoutes = require('./routes/directory');
const aiRoutes = require('./routes/ai');

const app = express();

// Behind Render/Railway/Fly the client IP arrives in X-Forwarded-For. Without
// this, every request appears to come from the proxy and rate limiting would
// apply one shared bucket to all users.
app.set('trust proxy', 1);

// CORS_ORIGINS is a comma-separated allowlist. Unset keeps the permissive
// behaviour the mobile-only prototype relied on; set it before exposing the
// admin routes to a browser.
const allowedOrigins = String(process.env.CORS_ORIGINS || '')
  .split(',')
  .map((o) => o.trim())
  .filter(Boolean);

app.use(
  cors(
    allowedOrigins.length > 0
      ? {
          origin(origin, callback) {
            // No Origin header: native mobile clients and curl. Allowed.
            if (!origin || allowedOrigins.includes(origin)) return callback(null, true);
            return callback(new Error('Origin not allowed by CORS policy.'));
          }
        }
      : undefined
  )
);

app.use(express.json({ limit: '1mb' }));

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.use('/auth', authRoutes);
app.use('/profile', profileRoutes);
app.use('/documents', documentRoutes);
app.use('/saved-schemes', savedSchemesRoutes);
app.use('/schemes', schemeRoutes);
app.use('/lawyer', lawyerRoutes);
app.use('/lawyer', lawyerQuestionRoutes);
app.use('/admin', adminRoutes);
app.use('/notifications', notificationRoutes);
app.use('/questions', questionRoutes);
app.use('/reports', reportRoutes);
app.use('/lawyers', directoryRoutes);
app.use('/ai', aiRoutes);

// Unmatched route. Without this an unknown path falls through to the error
// handler below and reports as a 500.
app.use((_req, res) => {
  res.status(404).json({ error: 'Not found.', code: 'NOT_FOUND' });
});

app.use((err, _req, res, _next) => {
  console.error(err);

  // Multer surfaces oversized uploads as an error rather than a route result.
  if (err && err.code === 'LIMIT_FILE_SIZE') {
    return res.status(413).json({ error: 'File is too large.', code: 'FILE_TOO_LARGE' });
  }
  if (err && err.code === 'ENTITY_TOO_LARGE') {
    return res.status(413).json({ error: 'Request body is too large.', code: 'BODY_TOO_LARGE' });
  }
  // Raised by multer fileFilter for a MIME type outside the allowlist.
  if (err && err.code === 'UNSUPPORTED_FILE_TYPE') {
    return res.status(415).json({
      error: 'Unsupported file type. Upload a JPEG, PNG, WebP or PDF.',
      code: 'UNSUPPORTED_FILE_TYPE'
    });
  }

  res.status(500).json({ error: 'Internal server error.' });
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`Urimai backend listening on port ${PORT}`);
});
