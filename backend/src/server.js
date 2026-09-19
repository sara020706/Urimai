require('dotenv').config();
const express = require('express');
const cors = require('cors');

const authRoutes = require('./routes/auth');
const profileRoutes = require('./routes/profile');
const documentRoutes = require('./routes/documents');
const savedSchemesRoutes = require('./routes/savedSchemes');

const app = express();
app.use(cors());
app.use(express.json());

app.get('/health', (_req, res) => res.json({ status: 'ok' }));

app.use('/auth', authRoutes);
app.use('/profile', profileRoutes);
app.use('/documents', documentRoutes);
app.use('/saved-schemes', savedSchemesRoutes);

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Internal server error.' });
});

const PORT = process.env.PORT || 4000;
app.listen(PORT, () => {
  console.log(`Urimai backend listening on port ${PORT}`);
});
