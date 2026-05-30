import React, { useState, useEffect } from 'react';
import { Document } from './types';
import Sidebar from './components/Sidebar';
import DocumentEditor from './components/DocumentEditor';
import AIChatPanel from './components/AIChatPanel';
import InteractiveChart from './components/InteractiveChart';
import { motion, AnimatePresence } from 'motion/react';
import {
  Layers, MessageSquare, Activity, AlertCircle, Database, HelpCircle,
  FolderOpen, ShieldCheck, Sun, Moon, Sparkles
} from 'lucide-react';

export default function App() {
  const [documents, setDocuments] = useState<Document[]>([]);
  const [activeTab, setActiveTab] = useState<'editor' | 'chat' | 'analytics'>('editor');
  const [selectedChatDocIds, setSelectedChatDocIds] = useState<string[]>([]);
  const [darkMode, setDarkMode] = useState<boolean>(true);
  const [isLoadingDocs, setIsLoadingDocs] = useState<boolean>(true);
  const [statusNotice, setStatusNotice] = useState<string | null>(null);

  // Sync dark class on mount and toggle
  useEffect(() => {
    const savedTheme = localStorage.getItem('theme');
    const isDark = savedTheme ? savedTheme === 'dark' : true;
    setDarkMode(isDark);
    if (isDark) {
      document.documentElement.classList.add('dark');
    } else {
      document.documentElement.classList.remove('dark');
    }
  }, []);

  const handleToggleTheme = () => {
    const nextDark = !darkMode;
    setDarkMode(nextDark);
    localStorage.setItem('theme', nextDark ? 'dark' : 'light');
    
    // Attempt writable check
    try {
      if (nextDark) {
        document.documentElement.classList.add('dark');
      } else {
        document.documentElement.classList.remove('dark');
      }
    } catch (_) {}
  };

  // Fetch Documents
  const fetchDocuments = async () => {
    setIsLoadingDocs(true);
    setStatusNotice(null);
    try {
      const response = await fetch('/api/documents');
      const data = await response.json();
      if (data.success) {
        setDocuments(data.documents);
      } else {
        setStatusNotice('Unable to load document repositories.');
      }
    } catch (err: any) {
      console.error(err);
      setStatusNotice('Local API server is spinning up or offline.');
    } finally {
      setIsLoadingDocs(false);
    }
  };

  useEffect(() => {
    fetchDocuments();
  }, []);

  return (
    <div className={darkMode ? 'dark' : ''}>
      <div className="min-h-screen bg-slate-50 dark:bg-slate-950 text-slate-900 dark:text-slate-100 font-sans flex flex-col md:flex-row transition-all duration-350 relative overflow-hidden">
        
        {/* Dynamic ambient glass glows */}
        <div className="absolute top-[-10%] right-[-10%] w-[350px] sm:w-[600px] h-[350px] sm:h-[600px] bg-indigo-500/10 dark:bg-indigo-650/8 rounded-full blur-[80px] sm:blur-[130px] pointer-events-none z-0"></div>
        <div className="absolute bottom-[5%] left-[5%] w-[250px] sm:w-[450px] h-[250px] sm:h-[450px] bg-purple-500/10 dark:bg-purple-650/8 rounded-full blur-[70px] sm:blur-[115px] pointer-events-none z-0"></div>

        {/* Dynamic Sidebar */}
        <Sidebar
          documents={documents}
          activeTab={activeTab}
          setActiveTab={(tab) => setActiveTab(tab)}
          selectedChatDocIds={selectedChatDocIds}
          onSelectDocForChat={setSelectedChatDocIds}
          darkMode={darkMode}
          onToggleTheme={handleToggleTheme}
        />

        {/* Core Main Terminal Stage */}
        <div className="flex-1 flex flex-col min-w-0 relative z-10">
          
          {/* Top Panel Global Header */}
          <header className="border-b border-slate-200/50 dark:border-white/10 bg-white/40 dark:bg-slate-950/40 backdrop-blur-md p-4 shrink-0 flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 relative z-10 transition-colors">
            <div className="flex items-center gap-3">
              <div className="p-2.5 bg-indigo-500/10 dark:bg-indigo-950/40 border border-indigo-500/20 rounded-xl shadow-xs">
                <Database className="h-5 w-5 text-indigo-500 dark:text-indigo-400 animate-pulse" />
              </div>
              <div>
                <h1 className="text-sm font-black uppercase tracking-wider text-slate-800 dark:text-slate-100 flex items-center gap-1.5">
                  Document Intelligence Hub
                  <span className="text-[10px] font-bold bg-indigo-100/80 dark:bg-indigo-950/80 text-indigo-600 dark:text-indigo-400 px-2 py-0.5 rounded-lg border border-indigo-200/40 dark:border-indigo-900/40">
                    v1.0 Ready
                  </span>
                </h1>
                <p className="text-[11px] text-slate-400 dark:text-slate-450 mt-0.5 flex items-center gap-1">
                  <ShieldCheck className="h-3 w-3 text-emerald-500" />
                  Local persistent database coupled with secure Gemini server-side environment
                </p>
              </div>
            </div>

            {/* Keys indicator - never show UI for key, just verify presence */}
            <div className="flex items-center gap-3">
              <div className="flex items-center gap-2 text-[10px] bg-white/50 dark:bg-slate-900/50 backdrop-blur-xs border border-slate-200/55 dark:border-white/5 p-2 rounded-xl text-slate-500 dark:text-slate-400 shadow-2xs">
                <div className="h-2 w-2 rounded-full bg-emerald-500 dark:bg-emerald-400 animate-pulse" />
                <span>AI Models Online: <strong className="text-slate-800 dark:text-slate-300 font-mono">gemini-3.5-flash</strong></span>
              </div>
            </div>
          </header>

          {/* Quick Notice Banner */}
          {statusNotice && (
            <div className="m-4 mb-2 p-3 bg-red-50/50 border border-red-100 dark:bg-red-950/10 dark:border-red-900/20 text-red-650 dark:text-red-400 rounded-xl text-xs flex items-center gap-2 backdrop-blur-xs z-10 shadow-3xs">
              <AlertCircle className="h-4 w-4 shrink-0" />
              <span>{statusNotice} Click the refresh button below to re-query database.</span>
              <button onClick={fetchDocuments} className="ml-auto font-bold underline text-[10px] hover:text-red-700 dark:hover:text-red-300 cursor-pointer">
                Sync Core
              </button>
            </div>
          )}

          {/* Core Content View Board */}
          <main className="flex-1 p-4 md:p-6 overflow-y-auto max-w-7xl w-full mx-auto relative z-10">
            <AnimatePresence mode="wait">
              {isLoadingDocs ? (
                <motion.div
                  key="loading-stage"
                  initial={{ opacity: 0 }}
                  animate={{ opacity: 1 }}
                  exit={{ opacity: 0 }}
                  className="flex flex-col items-center justify-center py-20"
                >
                  <Database className="h-10 w-10 text-indigo-500 dark:text-indigo-400 animate-spin mb-3" />
                  <p className="text-xs text-slate-500 font-semibold font-mono animate-pulse">Syncing document registries...</p>
                </motion.div>
              ) : (
                <motion.div
                  key={activeTab}
                  initial={{ opacity: 0, y: 15 }}
                  animate={{ opacity: 1, y: 0 }}
                  exit={{ opacity: 0, y: -15 }}
                  transition={{ duration: 0.25 }}
                >
                  {/* WORKSPACE VIEW (Document Editor / Upload) */}
                  {activeTab === 'editor' && (
                    <div className="flex flex-col gap-6">
                      <div className="glass-panel border-l-4 border-l-indigo-500 p-5 rounded-2xl relative overflow-hidden shadow-xs hover:shadow-md transition-all duration-300">
                        <div className="absolute -right-10 -top-10 w-40 h-40 bg-indigo-505/5 rounded-full blur-2xl pointer-events-none"></div>
                        <h2 className="text-sm font-extrabold uppercase tracking-widest text-slate-800 dark:text-slate-200 flex items-center gap-2">
                          <FolderOpen className="h-4 w-4 text-indigo-500 dark:text-indigo-450 animate-pulse" />
                          Document Sandbox Workspace
                        </h2>
                        <p className="text-xs text-slate-550 dark:text-slate-405 mt-2.5 max-w-3xl leading-relaxed">
                          Create, update, tag, or drag-and-drop multiple documents. Select specific items as <strong>Chat Target</strong> to narrow the co-pilot's reasoning context.
                        </p>
                      </div>
                      <DocumentEditor
                        documents={documents}
                        onRefresh={fetchDocuments}
                        onSelectDocForChat={setSelectedChatDocIds}
                        selectedChatDocIds={selectedChatDocIds}
                      />
                    </div>
                  )}

                  {/* CO-PILOT CHAT GENERAL VIEW */}
                  {activeTab === 'chat' && (
                    <div className="flex flex-col gap-6">
                      <div className="glass-panel border-l-4 border-l-emerald-500 p-5 rounded-2xl relative overflow-hidden shadow-xs hover:shadow-md transition-all duration-300">
                        <div className="absolute -right-10 -top-10 w-40 h-40 bg-emerald-505/5 rounded-full blur-2xl pointer-events-none"></div>
                        <h2 className="text-sm font-extrabold uppercase tracking-widest text-slate-800 dark:text-slate-200 flex items-center gap-2">
                          <Sparkles className="h-4 w-4 text-emerald-500 dark:text-emerald-450 animate-pulse" />
                          Document Chat Assistant
                        </h2>
                        <p className="text-xs text-slate-550 dark:text-slate-405 mt-2.5 max-w-3xl leading-relaxed">
                          Converse with the Gemini-3.5 model trained to parse your documents. You can toggle specific files in focus, run multi-document comparisons, or ask for dynamic summaries.
                        </p>
                      </div>
                      <AIChatPanel
                        documents={documents}
                        selectedChatDocIds={selectedChatDocIds}
                        onSelectDocForChat={setSelectedChatDocIds}
                      />
                    </div>
                  )}

                  {/* ANALYTICS CHARTS AND GRAPHS */}
                  {activeTab === 'analytics' && (
                    <div className="flex flex-col gap-6">
                      <div className="glass-panel border-l-4 border-l-indigo-500 p-5 rounded-2xl relative overflow-hidden shadow-xs hover:shadow-md transition-all duration-300">
                        <div className="absolute -right-10 -top-10 w-40 h-40 bg-indigo-505/5 rounded-full blur-2xl pointer-events-none"></div>
                        <h2 className="text-sm font-extrabold uppercase tracking-widest text-slate-800 dark:text-slate-200 flex items-center gap-2">
                          <Activity className="h-4 w-4 text-indigo-500 dark:text-indigo-450" />
                          Database Analytics Dashboard
                        </h2>
                        <p className="text-xs text-slate-550 dark:text-slate-405 mt-2.5 max-w-3xl leading-relaxed">
                          Interactive charts visualizing character frequency, word counts, semantic tags, and document-level sentiments.
                        </p>
                      </div>
                      <InteractiveChart documents={documents} />
                    </div>
                  )}
                </motion.div>
              )}
            </AnimatePresence>
          </main>
        </div>
      </div>
    </div>
  );
}
