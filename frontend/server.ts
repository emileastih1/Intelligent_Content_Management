import express from 'express';
import path from 'path';
import fs from 'fs';
import { createServer as createViteServer } from 'vite';
import { GoogleGenAI } from '@google/genai';

const app = express();
const PORT = 3000;

app.use(express.json());

// Path for persistence
const DATA_DIR = path.join(process.cwd(), 'data');
const DB_PATH = path.join(DATA_DIR, 'db.json');

// Ensure data directory exists
if (!fs.existsSync(DATA_DIR)) {
  fs.mkdirSync(DATA_DIR, { recursive: true });
}

// Interfaces
interface DBStructure {
  documents: Array<{
    id: string;
    title: string;
    content: string;
    tags: string[];
    category: string;
    charCount: number;
    wordCount: number;
    sentiment: 'Positive' | 'Neutral' | 'Critical' | 'Analytical' | 'Informative';
    createdAt: string;
    updatedAt: string;
  }>;
}

// Helper to seed dynamic initial content if empty
const SEED_DOCUMENTS = [
  {
    id: 'doc-1',
    title: 'Q1 Product Performance Report',
    content: `Overview:
In Q1, our core digital platform showed outstanding performance numbers. We saw user engagement spike by 42% following the release of the dynamic document analysis tools.

Key Metric Breakdown:
- January: Active Users - 12000, CPU load - 35%
- February: Active Users - 14500, CPU load - 41%
- March: Active Users - 18200, CPU load - 52%
- April: Active Users - 22100, CPU load - 64%
- May: Active Users - 28000, CPU load - 78%

System Stability:
Database write cycles remained under 12ms. Load testing successfully passed 5000 concurrent updates per second. Our custom area visualization tools successfully assisted the marketing and server telemetry teams. Some minor critical bugs on mobile web browsers were logged in February, which are now solved.

Conclusion & Action Items:
Increase server storage reserves. Optimize large document uploads of size > 40MB. Expand tag categorization.`,
    tags: ['performance', 'analytics', 'statistics', 'Q1-report'],
    category: 'Report',
    charCount: 887,
    wordCount: 145,
    sentiment: 'Analytical' as const,
    createdAt: new Date(Date.now() - 30 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 28 * 24 * 60 * 60 * 1000).toISOString()
  },
  {
    id: 'doc-2',
    title: 'Project Phoenix Roadmap',
    content: `Architecture Specification - Project Phoenix

Objective:
Migrate our legacy documents archive to a server-side indexed cloud environment with real-time AI context indexing.

Timeline & Phase Milestones:
- Phase 1 (Jan - Feb): Set up robust Express gateway structures and initial schema bindings. (Confidence: 95%)
- Phase 2 (March): Deploy SQLite/JSON persistent state loops and integrate model endpoints. (Confidence: 88%)
- Phase 3 (April): Finalise Framer Motion client layers and responsiveness grid maps. (Confidence: 92%)
- Phase 4 (May - June): Implement OAuth and deep multi-user sessions. (Confidence: 85%)

Technical Stack:
- Frontend: React 19 + Tailwind CSS + Lucide Icons + Framer Motion.
- Backend: Modular Node.js / Express custom server.
- AI Proxy: @google/genai module interfacing with gemini-3.5-flash.`,
    tags: ['phoenix', 'roadmap', 'technical-spec', 'ai-integration'],
    category: 'Architecture',
    charCount: 881,
    wordCount: 130,
    sentiment: 'Informative' as const,
    createdAt: new Date(Date.now() - 15 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 12 * 24 * 60 * 60 * 1000).toISOString()
  },
  {
    id: 'doc-3',
    title: 'GreenEnergy Performance Q4',
    content: `Corporate Sustainability Performance Index

Our wind and solar fields showed remarkable efficiency gains throughout the fourth quarter of the fiscal year:

Quarterly Generation Metric (Megawatt-hours):
- Oct: Solar - 450, Wind - 800
- Nov: Solar - 380, Wind - 920
- Dec: Solar - 320, Wind - 1100
- Jan: Solar - 400, Wind - 1250
- Feb: Solar - 490, Wind - 1050

Environmental Impact Analysis:
The offsets recorded directly equal absolute reductions of 1,200 tons of carbon equivalent emissions. However, wind generator maintenance in December caused an unexpected 3% power transmission loss. Action items include scheduling visual drone inspections for panel decay and upgrading wind gearboxes.`,
    tags: ['sustainability', 'green-energy', 'performance', 'Q4'],
    category: 'Sustainability',
    charCount: 771,
    wordCount: 110,
    sentiment: 'Positive' as const,
    createdAt: new Date(Date.now() - 50 * 24 * 60 * 60 * 1000).toISOString(),
    updatedAt: new Date(Date.now() - 48 * 24 * 60 * 60 * 1000).toISOString()
  }
];

// Read DB helper
const readDB = (): DBStructure => {
  if (!fs.existsSync(DB_PATH)) {
    const defaultData = { documents: SEED_DOCUMENTS };
    fs.writeFileSync(DB_PATH, JSON.stringify(defaultData, null, 2));
    return defaultData;
  }
  try {
    const dataStr = fs.readFileSync(DB_PATH, 'utf-8');
    return JSON.parse(dataStr);
  } catch (err) {
    console.error('Error reading DB, resetting to seed data', err);
    return { documents: SEED_DOCUMENTS };
  }
};

// Write DB helper
const writeDB = (data: DBStructure) => {
  fs.writeFileSync(DB_PATH, JSON.stringify(data, null, 2));
};

// Initialize DB
readDB();

// API: Get List of documents
app.get('/api/documents', (req, res) => {
  try {
    const db = readDB();
    res.json({ success: true, documents: db.documents });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// API: Create new documents (Single or Multi)
app.post('/api/documents', (req, res) => {
  try {
    const db = readDB();
    const body = req.body;
    
    let toAdd: any[] = [];
    if (Array.isArray(body)) {
      toAdd = body;
    } else if (body && typeof body === 'object') {
      toAdd = [body];
    } else {
      return res.status(400).json({ success: false, error: 'Invalid document payload' });
    }

    const addedDocs = toAdd.map((docData) => {
      const content = docData.content || '';
      const wordCount = content.trim() ? content.trim().split(/\s+/).length : 0;
      const charCount = content.length;
      
      // Determine a mock sentiment based on key terms
      let sentiment: 'Positive' | 'Neutral' | 'Critical' | 'Analytical' | 'Informative' = 'Neutral';
      const contentLower = content.toLowerCase();
      if (contentLower.includes('warning') || contentLower.includes('delay') || contentLower.includes('critical') || contentLower.includes('bug')) {
        sentiment = 'Critical';
      } else if (contentLower.includes('increase') || contentLower.includes('success') || contentLower.includes('win') || contentLower.includes('improve')) {
        sentiment = 'Positive';
      } else if (contentLower.includes('metric') || contentLower.includes('data') || contentLower.includes(' January') || contentLower.includes('percent')) {
        sentiment = 'Analytical';
      } else if (contentLower.includes('roadmap') || contentLower.includes('specification') || contentLower.includes('objective')) {
        sentiment = 'Informative';
      }

      return {
        id: docData.id || `doc-${Date.now()}-${Math.random().toString(36).substr(2, 5)}`,
        title: docData.title || 'Untitled Document',
        content: content,
        tags: Array.isArray(docData.tags) ? docData.tags : [],
        category: docData.category || 'General',
        charCount,
        wordCount,
        sentiment,
        createdAt: docData.createdAt || new Date().toISOString(),
        updatedAt: new Date().toISOString()
      };
    });

    db.documents.push(...addedDocs);
    writeDB(db);

    res.json({ success: true, documents: addedDocs });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// API: Update a single document
app.put('/api/documents/:id', (req, res) => {
  try {
    const db = readDB();
    const docId = req.params.id;
    const body = req.body;

    const index = db.documents.findIndex(d => d.id === docId);
    if (index === -1) {
      return res.status(404).json({ success: false, error: 'Document not found' });
    }

    const existing = db.documents[index];
    const updatedContent = body.content !== undefined ? body.content : existing.content;
    const wordCount = updatedContent.trim() ? updatedContent.trim().split(/\s+/).length : 0;
    const charCount = updatedContent.length;

    // Recalculate sentiment
    let sentiment = existing.sentiment;
    const contentLower = updatedContent.toLowerCase();
    if (contentLower.includes('warning') || contentLower.includes('delay') || contentLower.includes('critical') || contentLower.includes('bug')) {
      sentiment = 'Critical';
    } else if (contentLower.includes('increase') || contentLower.includes('success') || contentLower.includes('win') || contentLower.includes('improve')) {
      sentiment = 'Positive';
    } else if (contentLower.includes('metric') || contentLower.includes('data') || contentLower.includes('percent') || contentLower.includes('analysis')) {
      sentiment = 'Analytical';
    } else if (contentLower.includes('roadmap') || contentLower.includes('specification') || contentLower.includes('spec')) {
      sentiment = 'Informative';
    }

    db.documents[index] = {
      ...existing,
      title: body.title !== undefined ? body.title : existing.title,
      content: updatedContent,
      tags: Array.isArray(body.tags) ? body.tags : existing.tags,
      category: body.category !== undefined ? body.category : existing.category,
      wordCount,
      charCount,
      sentiment,
      updatedAt: new Date().toISOString()
    };

    writeDB(db);
    res.json({ success: true, document: db.documents[index] });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// API: Batch update multiple documents (e.g. bulk apply tags, or bulk categorise)
app.post('/api/documents/batch-update', (req, res) => {
  try {
    const db = readDB();
    const { documentIds, updatePayload } = req.body; // updatePayload: { tagsToAdd?: string[], category?: string }

    if (!Array.isArray(documentIds) || !updatePayload) {
      return res.status(400).json({ success: false, error: 'Missing documentIds or updatePayload' });
    }

    const updated: any[] = [];
    db.documents = db.documents.map(doc => {
      if (documentIds.includes(doc.id)) {
        let updatedTags = [...doc.tags];
        if (Array.isArray(updatePayload.tagsToAdd)) {
          // Unique additions
          updatePayload.tagsToAdd.forEach((t: string) => {
            if (!updatedTags.includes(t)) updatedTags.push(t);
          });
        }
        
        const docUpdated = {
          ...doc,
          category: updatePayload.category !== undefined ? updatePayload.category : doc.category,
          tags: updatedTags,
          updatedAt: new Date().toISOString()
        };
        updated.push(docUpdated);
        return docUpdated;
      }
      return doc;
    });

    writeDB(db);
    res.json({ success: true, documents: updated });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// API: Delete a document
app.delete('/api/documents/:id', (req, res) => {
  try {
    const db = readDB();
    const docId = req.params.id;

    const index = db.documents.findIndex(d => d.id === docId);
    if (index === -1) {
      return res.status(404).json({ success: false, error: 'Document not found' });
    }

    const deleted = db.documents.splice(index, 1);
    writeDB(db);
    res.json({ success: true, deleted: deleted[0] });
  } catch (err: any) {
    res.status(500).json({ success: false, error: err.message });
  }
});

// API: AI Chat with context
app.post('/api/chat', async (req, res) => {
  try {
    const { message, messages, documentIds } = req.body;

    // Load documents to build context
    const db = readDB();
    let docsForContext = db.documents;
    
    if (Array.isArray(documentIds) && documentIds.length > 0) {
      docsForContext = db.documents.filter(doc => documentIds.includes(doc.id));
    }

    // Compose custom prompt injecting document states
    let contextPrompt = "You are an advanced Document Intelligence AI Assistant. Analyze user's requests based on the documents provided below.\n\n";
    contextPrompt += `AVAILABLE DATABASE DOCUMENTS (${docsForContext.length} Total):\n`;
    
    docsForContext.forEach((doc, idx) => {
      contextPrompt += `--- Document #${idx + 1} ---\n`;
      contextPrompt += `ID: ${doc.id}\n`;
      contextPrompt += `Title: ${doc.title}\n`;
      contextPrompt += `Category: ${doc.category}\n`;
      contextPrompt += `Tags: ${doc.tags.join(', ')}\n`;
      contextPrompt += `Word Count: ${doc.wordCount}\n`;
      contextPrompt += `Content:\n${doc.content}\n\n`;
    });

    contextPrompt += `INSTRUCTION:\n`;
    contextPrompt += `1. Rely heavily on the injected documents to answer. If facts aren't in the documents, look logically across them.\n`;
    contextPrompt += `2. If the user asks for summaries or reports, use markdown formatting with tables, clean bullets, and clear headers.\n`;
    contextPrompt += `3. Keep your analysis objective, deeply factual, and highly professional.\n\n`;

    // Lazy load GoogleGenAI
    if (!process.env.GEMINI_API_KEY) {
      return res.status(500).json({
        success: false,
        error: 'GEMINI_API_KEY environment variable is not configured. Please add it in Settings > Secrets.'
      });
    }

    const ai = new GoogleGenAI({
      apiKey: process.env.GEMINI_API_KEY,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        }
      }
    });

    // Format chat history
    // Since Gemini SDK uses structured chats or just contents, let's assemble contents nicely
    const systemInstruction = contextPrompt;
    
    // Simple execution using ai.models.generateContent with loaded history
    const geminiHistory = [];
    if (Array.isArray(messages)) {
      messages.forEach((msg: any) => {
        // Exclude system contexts or other metadata
        if (msg.role === 'user' || msg.role === 'model') {
          geminiHistory.push({
            role: msg.role === 'user' ? 'user' : 'model',
            parts: [{ text: msg.content }]
          });
        }
      });
    }
    
    // Add current message if not yet in chat
    if (geminiHistory.length === 0 || geminiHistory[geminiHistory.length - 1].parts[0].text !== message) {
      geminiHistory.push({
        role: 'user',
        parts: [{ text: message }]
      });
    }

    const response = await ai.models.generateContent({
      model: 'gemini-3.5-flash',
      contents: geminiHistory,
      config: {
        systemInstruction,
        temperature: 0.2,
      }
    });

    res.json({
      success: true,
      content: response.text || "No response text was generated by the model.",
      referencedIds: docsForContext.map(d => d.id)
    });

  } catch (err: any) {
    console.error('Gemini API Error:', err);
    res.status(500).json({
      success: false,
      error: `Gemini API encountered an error: ${err.message || err}`
    });
  }
});

// SSE Streaming AI Endpoint
app.post('/api/chat/stream', async (req, res) => {
  // Set up Server-Sent Events headers
  res.setHeader('Content-Type', 'text/event-stream');
  res.setHeader('Cache-Control', 'no-cache');
  res.setHeader('Connection', 'keep-alive');

  try {
    const { message, messages, documentIds } = req.body;

    const db = readDB();
    let docsForContext = db.documents;
    
    if (Array.isArray(documentIds) && documentIds.length > 0) {
      docsForContext = db.documents.filter(doc => documentIds.includes(doc.id));
    }

    let contextPrompt = "You are an advanced Document Intelligence AI Assistant. Analyze user's requests based on the documents provided below.\n\n";
    contextPrompt += `AVAILABLE DATABASE DOCUMENTS (${docsForContext.length} Total):\n`;
    
    docsForContext.forEach((doc, idx) => {
      contextPrompt += `--- Document #${idx + 1} ---\n`;
      contextPrompt += `ID: ${doc.id}\n`;
      contextPrompt += `Title: ${doc.title}\n`;
      contextPrompt += `Category: ${doc.category}\n`;
      contextPrompt += `Tags: ${doc.tags.join(', ')}\n`;
      contextPrompt += `Word Count: ${doc.wordCount}\n`;
      contextPrompt += `Content:\n${doc.content}\n\n`;
    });

    contextPrompt += `INSTRUCTION:\n`;
    contextPrompt += `1. Rely heavily on the injected documents to answer. If facts aren't in the documents, look logically across them.\n`;
    contextPrompt += `2. If physical metrics or numbers are present, provide comparisons. Use tables and headers.\n`;
    contextPrompt += `3. Keep your analysis objective, factual, concise, and highly professional.\n\n`;

    if (!process.env.GEMINI_API_KEY) {
      res.write(`data: ${JSON.stringify({ error: 'GEMINI_API_KEY is missing. Please set it in Settings > Secrets.' })}\n\n`);
      res.end();
      return;
    }

    const ai = new GoogleGenAI({
      apiKey: process.env.GEMINI_API_KEY,
      httpOptions: {
        headers: {
          'User-Agent': 'aistudio-build',
        }
      }
    });

    const geminiHistory = [];
    if (Array.isArray(messages)) {
      messages.forEach((msg: any) => {
        if (msg.role === 'user' || msg.role === 'model') {
          geminiHistory.push({
            role: msg.role === 'user' ? 'user' : 'model',
            parts: [{ text: msg.content }]
          });
        }
      });
    }

    if (geminiHistory.length === 0 || geminiHistory[geminiHistory.length - 1].parts[0].text !== message) {
      geminiHistory.push({
        role: 'user',
        parts: [{ text: message }]
      });
    }

    const streamResponse = await ai.models.generateContentStream({
      model: 'gemini-3.5-flash',
      contents: geminiHistory,
      config: {
        systemInstruction: contextPrompt,
        temperature: 0.2,
      },
    });

    for await (const chunk of streamResponse) {
      const textChunk = chunk.text;
      if (textChunk) {
        res.write(`data: ${JSON.stringify({ chunk: textChunk })}\n\n`);
      }
    }

    res.write('data: [DONE]\n\n');
    res.end();

  } catch (err: any) {
    console.error('Gemini Stream Error:', err);
    res.write(`data: ${JSON.stringify({ error: `Stream error: ${err.message || err}` })}\n\n`);
    res.end();
  }
});

// Vite Middleware & static fallback routing
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
    console.log(`Server is running on http://0.0.0.0:${PORT}`);
  });
}

startServer();
