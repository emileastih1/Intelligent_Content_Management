import React, { useState, useEffect, useRef } from 'react';
import { Document, Message } from '../types';
import { motion, AnimatePresence } from 'motion/react';
import {
  Send, Sparkles, MessageSquare, AlertCircle, RefreshCw, FileText,
  Clock, ShieldAlert, Cpu, CheckCircle
} from 'lucide-react';

interface AIChatPanelProps {
  documents: Document[];
  selectedChatDocIds: string[];
  onSelectDocForChat: (ids: string[]) => void;
}

const PRESET_QUERIES = [
  {
    label: 'Summarize Performance Report',
    text: 'Draft a bulleted executive summary of Q1 Product Performance Report. Group it by User spike and Stability numbers.'
  },
  {
    label: 'Compare Timelines',
    text: 'Compare the timeline / roadmap phases described across all documents and assess the relative confidence margins.'
  },
  {
    label: 'Extract Energy Metrics',
    text: 'Formulate a neat markdown table listing Wind and Solar generation metrics by month from GreenEnergy Performance Q4.'
  },
  {
    label: 'Perform Risk Audits',
    text: 'Look across all documents and identify potential engineering risks, maintenance issues, or browser-specific bugs.'
  }
];

export default function AIChatPanel({
  documents,
  selectedChatDocIds,
  onSelectDocForChat
}: AIChatPanelProps) {
  const [messages, setMessages] = useState<Message[]>([
    {
      id: 'welcome-msg',
      role: 'model',
      content: `Hello! I am your advanced Document Intelligence AI Assistant. 

I can analyze reports, roads, specs, and metrics currently stored in the database. I will reference their contents to draft accurate summaries, extract key figures into tables, and perform critical audits.

To begin, select specific documents you want me to analyze using the **Chat Target** buttons or query across our entire archive.`,
      timestamp: new Date().toLocaleTimeString()
    }
  ]);
  const [userInput, setUserInput] = useState('');
  const [isLoading, setIsLoading] = useState(false);
  const [streamedText, setStreamedText] = useState('');
  const [isStreaming, setIsStreaming] = useState(false);
  const [activeDocCount, setActiveDocCount] = useState(0);

  const messagesEndRef = useRef<HTMLDivElement>(null);

  // Auto Scroll
  useEffect(() => {
    messagesEndRef.current?.scrollIntoView({ behavior: 'smooth' });
  }, [messages, streamedText]);

  useEffect(() => {
    // Keep tag counts
    setActiveDocCount(
      selectedChatDocIds.length > 0 
        ? selectedChatDocIds.length 
        : documents.length
    );
  }, [selectedChatDocIds, documents]);

  // Streaming & Chat dispatch
  const handleSendMessage = async (textToSend: string) => {
    if (!textToSend.trim() || isLoading) return;

    const userMsg: Message = {
      id: `user-${Date.now()}`,
      role: 'user',
      content: textToSend,
      timestamp: new Date().toLocaleTimeString(),
      references: selectedChatDocIds.length > 0 ? [...selectedChatDocIds] : documents.map(d => d.id)
    };

    setMessages(prev => [...prev, userMsg]);
    setUserInput('');
    setIsLoading(true);
    setStreamedText('');
    setIsStreaming(true);

    try {
      // Dispatch SSE Stream request
      const response = await fetch('/api/chat/stream', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          message: textToSend,
          messages: [...messages, userMsg],
          documentIds: selectedChatDocIds
        })
      });

      if (!response.ok) {
        throw new Error(`Server returned code ${response.status}`);
      }

      // Check if body stream is not null
      if (!response.body) {
        throw new Error('Readable stream not supported, or empty body received.');
      }

      const reader = response.body.getReader();
      const decoder = new TextDecoder('utf-8');
      let done = false;
      let accumulatedResponse = '';

      while (!done) {
        const { value, done: readerDone } = await reader.read();
        done = readerDone;
        if (value) {
          const chunkStr = decoder.decode(value, { stream: !done });
          
          // Server-Sent Events stream splits lines by "\n\n" or "\n"
          const lines = chunkStr.split('\n');
          for (const line of lines) {
            if (line.startsWith('data: ')) {
              const dataString = line.substring(6).trim();
              if (dataString === '[DONE]') {
                done = true;
                break;
              }
              try {
                const parsed = JSON.parse(dataString);
                if (parsed.error) {
                  accumulatedResponse += `\n\n[Error: ${parsed.error}]`;
                  setStreamedText(accumulatedResponse);
                } else if (parsed.chunk) {
                  accumulatedResponse += parsed.chunk;
                  setStreamedText(accumulatedResponse);
                }
              } catch (e) {
                // Ignore parsing errors for empty/partial chunks
              }
            }
          }
        }
      }

      // Save streamed result in formal messages state
      setMessages(prev => [
        ...prev,
        {
          id: `ai-${Date.now()}`,
          role: 'model',
          content: accumulatedResponse || "Analysis completed without content streams.",
          timestamp: new Date().toLocaleTimeString(),
          references: selectedChatDocIds.length > 0 ? [...selectedChatDocIds] : documents.map(d => d.id)
        }
      ]);
      setStreamedText('');
      setIsStreaming(false);

    } catch (streamError) {
      console.warn('Streaming error, falling back to complete API block:', streamError);
      // Fallback: Non-streaming complete POST API request
      try {
        const response = await fetch('/api/chat', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            message: textToSend,
            messages: [...messages, userMsg],
            documentIds: selectedChatDocIds
          })
        });

        const data = await response.json();
        if (data.success) {
          setMessages(prev => [
            ...prev,
            {
              id: `ai-${Date.now()}`,
              role: 'model',
              content: data.content,
              timestamp: new Date().toLocaleTimeString(),
              references: data.referencedIds
            }
          ]);
        } else {
          setMessages(prev => [
            ...prev,
            {
              id: `ai-err-${Date.now()}`,
              role: 'model',
              content: `⚠️ Failed to fetch AI answer: ${data.error}`,
              timestamp: new Date().toLocaleTimeString()
            }
          ]);
        }
      } catch (postError: any) {
        setMessages(prev => [
          ...prev,
          {
            id: `ai-err-${Date.now()}`,
            role: 'model',
            content: `❌ Connection error to backend API: ${postError.message || postError}`,
            timestamp: new Date().toLocaleTimeString()
          }
        ]);
      }
      setStreamedText('');
      setIsStreaming(false);
    } finally {
      setIsLoading(false);
    }
  };

  const handleClearHistory = () => {
    setMessages([
      {
        id: 'new-session-msg',
        role: 'model',
        content: "Chat session refreshed. Select documents and ask me anything!",
        timestamp: new Date().toLocaleTimeString()
      }
    ]);
  };

  // Safe manual markdown table formatting
  const formatMarkdownText = (rawText: string) => {
    // Extremely basic block parser to format paragraphs, code, and table elements safely
    const blocks = rawText.split('\n');
    let inTable = false;
    let tableHeaders: string[] = [];

    return blocks.map((line, idx) => {
      // Check for markdown horizontal divider
      if (line.trim() === '---') {
        return <hr key={idx} className="my-3 border-slate-200 dark:border-slate-800" />;
      }

      // Check for code blocks
      const codeMatch = line.match(/^```(\w*)/);
      if (codeMatch) {
        return null; // For simplicity in line-by-line render
      }

      // Bullet check
      if (line.trim().startsWith('- ') || line.trim().startsWith('* ')) {
        return (
          <li key={idx} className="ml-5 list-disc text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed mb-1">
            {line.trim().substring(2)}
          </li>
        );
      }

      // Headers check
      if (line.match(/^### /)) {
        return <h4 key={idx} className="text-xs sm:text-sm font-extrabold text-slate-800 dark:text-slate-200 mt-3 mb-1.5 uppercase tracking-wide">{line.substring(4)}</h4>;
      }
      if (line.match(/^## /)) {
        return <h3 key={idx} className="text-sm sm:text-base font-bold text-slate-900 dark:text-slate-100 mt-4 mb-2">{line.substring(3)}</h3>;
      }

      // Simple Table layout parsing
      if (line.trim().startsWith('|') && line.trim().endsWith('|')) {
        const columns = line.split('|').map(c => c.trim()).filter((_, i, arr) => i > 0 && i < arr.length - 1);
        if (line.includes('---')) {
          return null; // Divider inside tables
        }
        
        // Simple inline grid for table
        return (
          <div key={idx} className="grid grid-cols-4 gap-2 bg-slate-50 dark:bg-slate-950/40 p-2 text-xs border-b border-slate-150 dark:border-slate-800 font-sans">
            {columns.map((col, cidx) => (
              <span key={cidx} className="font-medium truncate text-slate-600 dark:text-slate-400">{col}</span>
            ))}
          </div>
        );
      }

      // Paragraph
      if (line.trim()) {
        return (
          <p key={idx} className="text-xs sm:text-sm text-slate-700 dark:text-slate-300 leading-relaxed mb-2 last:mb-0">
            {line}
          </p>
        );
      }

      return <div key={idx} className="h-2" />;
    });
  };

  return (
    <div className="flex flex-col h-[650px] glass-panel rounded-2xl overflow-hidden shadow-sm relative">
      {/* Session Header */}
      <div className="flex justify-between items-center px-4 py-3 border-b border-white/10 dark:border-slate-800/55 bg-white/5 dark:bg-slate-950/20 backdrop-blur-md">
        <div className="flex items-center gap-2">
          <div className="h-2.5 w-2.5 rounded-full bg-emerald-500 animate-pulse" />
          <div>
            <h3 className="font-bold text-xs text-slate-800 dark:text-slate-100 flex items-center gap-1.5 uppercase tracking-wider">
              Gemini Co-Pilot
            </h3>
            <p className="text-[10px] text-slate-400 dark:text-slate-500 flex items-center gap-1">
              <Cpu className="h-3 w-3" />
              Sourcing Context: <span className="font-mono text-indigo-500 dark:text-indigo-400 font-bold">{activeDocCount} active document(s)</span>
            </p>
          </div>
        </div>

        <div className="flex items-center gap-2">
          {selectedChatDocIds.length > 0 && (
            <button
              id="btn-chat-reset-scope"
              onClick={() => onSelectDocForChat([])}
              className="text-[10px] font-semibold text-slate-500 dark:text-slate-455 hover:text-indigo-500 flex items-center gap-1 bg-slate-100 dark:bg-slate-800 px-2 py-1 rounded-lg transition-all cursor-pointer"
            >
              Analyze All Documents
            </button>
          )}

          <button
            id="btn-chat-clear"
            onClick={handleClearHistory}
            className="p-1 px-2.5 hover:bg-slate-100 dark:hover:bg-slate-800 text-slate-400 dark:text-slate-500 hover:text-slate-700 dark:hover:text-slate-355 rounded-lg text-[10px] font-bold border border-slate-200 dark:border-slate-800 transition-all cursor-pointer"
          >
            Reset Session
          </button>
        </div>
      </div>

      {/* Preset prompt helper chips */}
      {messages.length <= 1 && (
        <div className="px-4 py-3 border-b border-white/5 dark:border-slate-805 bg-indigo-500/5 dark:bg-indigo-950/10">
          <span className="text-[10px] font-bold text-indigo-500 dark:text-indigo-400 uppercase tracking-widest flex items-center gap-1.5 mb-2">
            <Sparkles className="h-3.5 w-3.5 animate-pulse" />
            Quick Insights Prompts
          </span>
          <div className="flex flex-wrap gap-1.5">
            {PRESET_QUERIES.map((preset, idx) => (
              <button
                id={`preset-chip-${idx}`}
                key={idx}
                onClick={() => handleSendMessage(preset.text)}
                className="text-[10px] text-slate-650 dark:text-slate-300 hover:text-indigo-500 dark:hover:text-indigo-400 glass-card px-2.5 py-1.5 rounded-xl cursor-pointer hover:border-indigo-400/50 dark:hover:border-indigo-500/30 shadow-3xs transition-all hover:scale-[1.02]"
              >
                {preset.label}
              </button>
            ))}
          </div>
        </div>
      )}

      {/* Message scrolling stage */}
      <div className="flex-1 overflow-y-auto p-4 flex flex-col gap-4">
        {messages.map((msg, index) => {
          const isUser = msg.role === 'user';
          return (
            <div
              id={`chat-bubble-${msg.id}`}
              key={msg.id}
              className={`flex items-start gap-2.5 max-w-[85%] ${
                isUser ? 'self-end flex-row-reverse' : 'self-start'
              }`}
            >
              {/* Avatar indicator */}
              <div className={`p-2 rounded-xl shrink-0 ${
                isUser ? 'bg-indigo-650 text-white' : 'bg-white/10 dark:bg-slate-800/60 text-indigo-500 dark:text-indigo-400 border border-white/5'
              }`}>
                {isUser ? (
                  <MessageSquare className="h-3.5 w-3.5" />
                ) : (
                  <Cpu className="h-3.5 w-3.5" />
                )}
              </div>

              <div className="flex flex-col gap-1">
                <div className={`rounded-2xl p-3.5 text-xs sm:text-sm shadow-3xs ${
                  isUser 
                    ? 'bg-gradient-to-r from-indigo-600 to-indigo-505 text-white rounded-tr-none shadow-xs'
                    : 'glass-card text-slate-800 dark:text-slate-200 rounded-tl-none border border-white/10 dark:border-white/5'
                }`}>
                  {/* Referenced docs tags */}
                  {msg.references && msg.references.length > 0 && (
                    <div className="flex flex-wrap gap-1 mb-2 border-b border-indigo-200/40 dark:border-slate-700 pb-1.5">
                      <span className="text-[8.5px] font-bold text-indigo-400 font-mono uppercase tracking-widest flex items-center gap-1 mr-1">
                        Ref Document Contexts:
                      </span>
                      {msg.references.map((refId) => {
                        const originalDoc = documents.find(d => d.id === refId);
                        return originalDoc ? (
                          <span key={refId} className="text-[9px] font-semibold bg-indigo-500/10 text-indigo-300 dark:text-indigo-400 px-1.5 py-0.5 rounded border border-indigo-500/20">
                            {originalDoc.title.substring(0, 15)}...
                          </span>
                        ) : null;
                      })}
                    </div>
                  )}
                  {isUser ? <p className="leading-relaxed whitespace-pre-wrap">{msg.content}</p> : formatMarkdownText(msg.content)}
                </div>
                <span className={`text-[9px] text-slate-400 font-medium px-1.5 flex items-center gap-1 ${isUser ? 'self-end' : 'self-start'}`}>
                  <Clock className="h-2.5 w-2.5" />
                  {msg.timestamp}
                </span>
              </div>
            </div>
          );
        })}

        {/* Live streaming chunk overlay */}
        {isStreaming && streamedText && (
          <div className="flex items-start gap-2.5 max-w-[85%] self-start">
            <div className="p-2 rounded-xl bg-indigo-505/10 text-indigo-505 dark:text-indigo-400 shrink-0 border border-indigo-505/20">
              <Cpu className="h-3.5 w-3.5 animate-spin" />
            </div>
            <div className="flex flex-col gap-1">
              <div className="rounded-2xl p-3.5 text-xs sm:text-sm glass-panel border-indigo-400/20 bg-indigo-500/5 text-slate-800 dark:text-slate-200 rounded-tl-none shadow-3xs">
                <span className="text-[9px] font-bold bg-indigo-600 text-white px-2 py-0.5 rounded-full uppercase tracking-wider mb-2.5 inline-flex items-center gap-1 animate-pulse">
                  <RefreshCw className="h-2.5 w-2.5 animate-spin" /> Real-time Response Stream
                </span>
                {formatMarkdownText(streamedText)}
              </div>
            </div>
          </div>
        )}

        {/* Generic loader indicator */}
        {isLoading && !streamedText && (
          <div className="flex items-start gap-2.5 self-start">
            <div className="p-2 rounded-xl bg-slate-100 dark:bg-slate-800 text-indigo-500 shrink-0">
              <RefreshCw className="h-3.5 w-3.5 animate-spin" />
            </div>
            <div className="bg-slate-50 dark:bg-slate-800/40 border border-slate-100 dark:border-slate-800 py-3 px-4 rounded-2xl rounded-tl-none text-xs text-slate-500 dark:text-slate-400 italic">
              AI assistant analyzing document database context...
            </div>
          </div>
        )}

        <div ref={messagesEndRef} />
      </div>

      {/* Input panel container */}
      <form
        id="frm-chat-input-area"
        onSubmit={(e) => { e.preventDefault(); handleSendMessage(userInput); }}
        className="p-3 bg-white/5 dark:bg-slate-950/30 border-t border-white/10 dark:border-slate-800/50 flex gap-2 items-center backdrop-blur-md"
      >
        <div className="relative flex-1">
          <input
            id="chat-input-text-field"
            type="text"
            value={userInput}
            onChange={(e) => setUserInput(e.target.value)}
            placeholder={
              documents.length === 0 
                ? 'Store database entries to enable chat co-pilot...' 
                : `Query AI co-pilot indexing ${activeDocCount} document(s)...`
            }
            disabled={isLoading || documents.length === 0}
            className="w-full glass-input text-slate-800 dark:text-slate-100 placeholder-slate-400 dark:placeholder-slate-500 rounded-xl py-3 pl-4 pr-10 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all disabled:opacity-60"
          />
          <Sparkles className="absolute right-3.5 top-3.5 h-4 w-4 text-slate-400 cursor-help" />
        </div>

        <button
          id="btn-chat-send"
          type="submit"
          disabled={!userInput.trim() || isLoading || documents.length === 0}
          className="p-3 rounded-xl bg-indigo-650 hover:bg-indigo-600 dark:bg-indigo-500 dark:hover:bg-indigo-600 text-white disabled:bg-slate-200 dark:disabled:bg-slate-800 disabled:text-slate-400 dark:disabled:text-slate-600 cursor-pointer shadow-md transition-all flex items-center justify-center"
        >
          <Send className="h-4 w-4" />
        </button>
      </form>
    </div>
  );
}
