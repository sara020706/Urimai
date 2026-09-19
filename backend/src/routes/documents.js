const express = require('express');
const multer = require('multer');
const pool = require('../db/pool');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
const upload = multer({ storage: multer.memoryStorage(), limits: { fileSize: 10 * 1024 * 1024 } });

function toApiDocument(row) {
  return {
    id: row.id,
    documentName: row.document_name,
    fileName: row.file_name,
    mimeType: row.mime_type,
    uploadedAt: new Date(row.uploaded_at).getTime()
  };
}

router.get('/', requireAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT id, document_name, file_name, mime_type, uploaded_at
     FROM uploaded_documents WHERE user_id = $1 ORDER BY uploaded_at DESC`,
    [req.userId]
  );
  res.json(result.rows.map(toApiDocument));
});

router.post('/', requireAuth, upload.single('file'), async (req, res) => {
  const { documentName } = req.body || {};
  if (!documentName || !req.file) {
    return res.status(400).json({ error: 'documentName and file are required.' });
  }

  const result = await pool.query(
    `INSERT INTO uploaded_documents (user_id, document_name, file_name, mime_type, file_data)
     VALUES ($1, $2, $3, $4, $5)
     RETURNING id, document_name, file_name, mime_type, uploaded_at`,
    [req.userId, documentName, req.file.originalname, req.file.mimetype, req.file.buffer]
  );
  res.status(201).json(toApiDocument(result.rows[0]));
});

router.get('/:id/file', requireAuth, async (req, res) => {
  const result = await pool.query(
    `SELECT file_name, mime_type, file_data FROM uploaded_documents WHERE id = $1 AND user_id = $2`,
    [req.params.id, req.userId]
  );
  const doc = result.rows[0];
  if (!doc) {
    return res.status(404).json({ error: 'Document not found.' });
  }
  res.setHeader('Content-Type', doc.mime_type || 'application/octet-stream');
  res.setHeader('Content-Disposition', `inline; filename="${doc.file_name}"`);
  res.send(doc.file_data);
});

router.delete('/:id', requireAuth, async (req, res) => {
  const result = await pool.query(
    'DELETE FROM uploaded_documents WHERE id = $1 AND user_id = $2 RETURNING id',
    [req.params.id, req.userId]
  );
  if (result.rowCount === 0) {
    return res.status(404).json({ error: 'Document not found.' });
  }
  res.status(204).send();
});

module.exports = router;
