import express from 'express';
import path from 'path';
import { createServer as createViteServer } from 'vite';

const app = express();
const PORT = 3000;

app.use(express.json({ limit: '50mb' }));

// ---------------------------------------------------------------------------
// Config (injected from env at container runtime; fall back to localhost for
// local development with the stack running outside Docker)
// ---------------------------------------------------------------------------
const ICM_BASE_URL = process.env.ICM_BASE_URL || 'http://localhost:8085/idm/api';
const KC_TOKEN_URL = process.env.KC_TOKEN_URL
  || 'http://localhost:8180/realms/intelligent-content-management/protocol/openid-connect/token';
const KC_CLIENT_ID = process.env.KC_CLIENT_ID || 'icm-bff';
const KC_CLIENT_SECRET = process.env.KC_CLIENT_SECRET || '';

// ---------------------------------------------------------------------------
// ServiceAccountTokenProvider — client-credentials grant, cached until expiry
// ---------------------------------------------------------------------------
let cachedToken: string | null = null;
let tokenExpiresAt = 0;

async function getServiceAccountToken(): Promise<string> {
  const now = Date.now();
  if (cachedToken && now < tokenExpiresAt - 30_000) {
    return cachedToken;
  }

  const body = new URLSearchParams({
    grant_type: 'client_credentials',
    client_id: KC_CLIENT_ID,
    client_secret: KC_CLIENT_SECRET,
  });

  const res = await fetch(KC_TOKEN_URL, {
    method: 'POST',
    headers: { 'Content-Type': 'application/x-www-form-urlencoded' },
    body: body.toString(),
  });

  if (!res.ok) {
    const text = await res.text();
    throw new Error(`Token acquisition failed (${res.status}): ${text}`);
  }

  const json = await res.json() as { access_token: string; expires_in: number };
  cachedToken = json.access_token;
  tokenExpiresAt = now + json.expires_in * 1000;
  return cachedToken;
}

// ---------------------------------------------------------------------------
// IcmClient — typed calls to ICM with bearer token attached
// ---------------------------------------------------------------------------
async function icmFetch(path: string, init: RequestInit = {}): Promise<Response> {
  const token = await getServiceAccountToken();
  return fetch(`${ICM_BASE_URL}${path}`, {
    ...init,
    headers: {
      ...(init.headers as Record<string, string> || {}),
      Authorization: `Bearer ${token}`,
      'Content-Type': 'application/json',
    },
  });
}

// Shape adapter: ICM DocumentDto → frontend Document shape
function icmDocToFrontend(doc: any) {
  const content = doc.content || '';
  const words = content.trim() ? content.trim().split(/\s+/).length : 0;
  return {
    id: String(doc.id),
    title: doc.documentName || doc.name || '',
    content,
    tags: doc.tags || [],
    category: doc.category || 'General',
    charCount: content.length,
    wordCount: words,
    sentiment: doc.sentiment || 'Neutral',
    createdAt: doc.creationDate || new Date().toISOString(),
    updatedAt: doc.modificationDate || new Date().toISOString(),
  };
}

// ---------------------------------------------------------------------------
// BFF routes — /api/* proxied to ICM
// ---------------------------------------------------------------------------

// GET /api/documents — list
app.get('/api/documents', async (req, res) => {
  try {
    const icmRes = await icmFetch('/v1/document');
    if (!icmRes.ok) {
      const body = await icmRes.text();
      return res.status(icmRes.status).json({ success: false, error: body });
    }
    const docs = await icmRes.json() as any[];
    res.json({ success: true, documents: docs.map(icmDocToFrontend) });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// POST /api/documents — create (authored, content-first)
app.post('/api/documents', async (req, res) => {
  try {
    const body = req.body;
    const docs = Array.isArray(body) ? body : [body];
    const results = [];

    for (const doc of docs) {
      const icmRes = await icmFetch('/v1/document', {
        method: 'POST',
        body: JSON.stringify({
          name: doc.title || 'Untitled',
          content: doc.content || '',
          tags: doc.tags || [],
          category: doc.category || 'General',
        }),
      });
      if (!icmRes.ok) {
        const errBody = await icmRes.text();
        return res.status(icmRes.status).json({ success: false, error: errBody });
      }
      const created = await icmRes.json();
      results.push(created);
    }

    // Re-fetch list so caller has the full populated shape
    const listRes = await icmFetch('/v1/document');
    const allDocs = listRes.ok ? ((await listRes.json()) as any[]).map(icmDocToFrontend) : [];
    const createdIds = results.map(r => String(r.id));
    const createdDocs = allDocs.filter(d => createdIds.includes(d.id));

    res.json({ success: true, documents: createdDocs.length ? createdDocs : results.map(icmDocToFrontend) });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// PUT /api/documents/:id — update
app.put('/api/documents/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const body = req.body;
    const icmRes = await icmFetch(`/v1/document/${id}`, {
      method: 'PUT',
      body: JSON.stringify({
        name: body.title,
        content: body.content,
        tags: body.tags,
        category: body.category,
      }),
    });
    if (!icmRes.ok) {
      const errBody = await icmRes.text();
      return res.status(icmRes.status).json({ success: false, error: errBody });
    }
    const updated = await icmRes.json();
    res.json({ success: true, document: icmDocToFrontend(updated) });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// DELETE /api/documents/:id
app.delete('/api/documents/:id', async (req, res) => {
  try {
    const { id } = req.params;
    const icmRes = await icmFetch(`/v1/document/${id}`, { method: 'DELETE' });
    if (!icmRes.ok) {
      const errBody = await icmRes.text();
      return res.status(icmRes.status).json({ success: false, error: errBody });
    }
    res.json({ success: true });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// POST /api/documents/batch-update
app.post('/api/documents/batch-update', async (req, res) => {
  try {
    const { documentIds, updatePayload } = req.body;
    const icmRes = await icmFetch('/v1/document/batch-update', {
      method: 'POST',
      body: JSON.stringify({ documentIds, updatePayload }),
    });
    if (!icmRes.ok) {
      const errBody = await icmRes.text();
      return res.status(icmRes.status).json({ success: false, error: errBody });
    }
    const result = await icmRes.json();
    res.json({
      success: true,
      documents: Array.isArray(result) ? result.map(icmDocToFrontend) : [],
    });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// GET /api/documents/search?q=
app.get('/api/documents/search', async (req, res) => {
  try {
    const q = req.query.q as string || '';
    const icmRes = await icmFetch(`/v1/document/search?q=${encodeURIComponent(q)}`);
    if (!icmRes.ok) {
      const errBody = await icmRes.text();
      return res.status(icmRes.status).json({ success: false, error: errBody });
    }
    const docs = await icmRes.json() as any[];
    res.json({ success: true, documents: docs.map(icmDocToFrontend) });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// POST /api/chat/stream — SSE relay from ICM /v1/document/ask (ADR-0003, ADR-0007)
app.post('/api/chat/stream', async (req, res) => {
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  try {
    const { message, documentIds } = req.body;

    const token = await getServiceAccountToken();
    const icmRes = await fetch(`${ICM_BASE_URL}/v1/document/ask?topK=2`, {
      method: 'POST',
      headers: {
        Authorization: `Bearer ${token}`,
        'Content-Type': 'application/json',
        Accept: 'text/event-stream',
      },
      body: JSON.stringify({
        question: message,
        documentIds: documentIds || [],
      }),
    });

    if (!icmRes.ok || !icmRes.body) {
      const errText = await icmRes.text().catch(() => '');
      res.write(`data: ${JSON.stringify({ error: `ICM ask failed (${icmRes.status}): ${errText}` })}\n\n`);
      res.end();
      return;
    }

    const reader = icmRes.body.getReader();
    const decoder = new TextDecoder();

    while (true) {
      const { done, value } = await reader.read();
      if (done) break;
      const chunk = decoder.decode(value, { stream: true });
      res.write(chunk);
    }

    res.end();
  } catch (err: any) {
    res.write(`data: ${JSON.stringify({ error: `Stream error: ${err.message}` })}\n\n`);
    res.end();
  }
});

// ---------------------------------------------------------------------------
// Vite middleware (dev) / static (prod)
// ---------------------------------------------------------------------------
async function startServer() {
  if (process.env.NODE_ENV !== 'production') {
    const vite = await createViteServer({
      server: { middlewareMode: true },
      appType: 'spa',
    });
    app.use(vite.middlewares);
  } else {
    const distPath = path.join(process.cwd(), 'dist');
    app.use(express.static(distPath));
    app.get('*', (req, res) => {
      res.sendFile(path.join(distPath, 'index.html'));
    });
  }

  app.listen(PORT, '0.0.0.0', () => {
    console.log(`BFF server running on http://0.0.0.0:${PORT}`);
    console.log(`ICM_BASE_URL: ${ICM_BASE_URL}`);
  });
}

startServer();
