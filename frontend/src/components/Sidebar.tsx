import React, { useState } from 'react';
import { Document } from '../types';
import {
  FileText, MessageSquare, Activity, Key, Moon, Sun, Layers,
  Database, Menu, X, ArrowLeftRight, Check, HelpCircle
} from 'lucide-react';

interface SidebarProps {
  documents: Document[];
  activeTab: 'editor' | 'chat' | 'analytics';
  setActiveTab: (tab: 'editor' | 'chat' | 'analytics') => void;
  selectedChatDocIds: string[];
  onSelectDocForChat: (ids: string[]) => void;
  darkMode: boolean;
  onToggleTheme: () => void;
}

export default function Sidebar({
  documents,
  activeTab,
  setActiveTab,
  selectedChatDocIds,
  onSelectDocForChat,
  darkMode,
  onToggleTheme
}: SidebarProps) {
  const [mobileOpen, setMobileOpen] = useState(false);

  const toggleMobileMenu = () => {
    setMobileOpen(!mobileOpen);
  };

  const navItems = [
    {
      id: 'editor' as const,
      label: 'Document Sandbox',
      desc: 'Create, edit & tag docs',
      icon: Layers,
      color: 'text-indigo-500'
    },
    {
      id: 'chat' as const,
      label: 'AI Chat Advisor',
      desc: 'SSE response streams',
      icon: MessageSquare,
      color: 'text-emerald-500'
    },
    {
      id: 'analytics' as const,
      label: 'Database Metrics',
      desc: 'Interactive SVG graphs',
      icon: Activity,
      color: 'text-purple-500'
    }
  ];

  const handleNavClick = (tabId: 'editor' | 'chat' | 'analytics') => {
    setActiveTab(tabId);
    setMobileOpen(false);
  };

  return (
    <>
      {/* Mobile Top Header (hidden on md screens) */}
      <div className="md:hidden flex items-center justify-between p-4 bg-white dark:bg-slate-950 border-b border-slate-200 dark:border-slate-900 sticky top-0 z-40">
        <div className="flex items-center gap-2">
          <div className="h-2.5 w-2.5 rounded-full bg-indigo-500 animate-pulse" />
          <h2 className="text-xs font-black uppercase tracking-wider text-slate-800 dark:text-slate-100">
            Document Intelligence
          </h2>
        </div>
        
        <div className="flex items-center gap-2">
          <button
            id="mobile-theme-toggle"
            onClick={onToggleTheme}
            className="p-2 text-slate-500 hover:text-slate-700 dark:text-slate-400 dark:hover:text-slate-200 rounded-xl bg-slate-50 dark:bg-slate-900 cursor-pointer"
          >
            {darkMode ? <Sun className="h-4 w-4" /> : <Moon className="h-4 w-4" />}
          </button>

          <button
            id="mobile-hamburger-toggle"
            onClick={toggleMobileMenu}
            className="p-2 text-slate-500 dark:text-slate-400 rounded-xl hover:bg-slate-100 dark:hover:bg-slate-900 cursor-pointer"
          >
            {mobileOpen ? <X className="h-5 w-5" /> : <Menu className="h-5 w-5" />}
          </button>
        </div>
      </div>

      {/* Persistent Sidebar (large screens) & Floating Drawer (mobile) */}
      <div
        className={`fixed md:sticky top-0 left-0 bottom-0 z-30 w-72 glass-sidebar flex flex-col justify-between transition-transform duration-350 md:translate-x-0 ${
          mobileOpen ? 'translate-x-0' : '-translate-x-full md:translate-x-0'
        }`}
      >
        <div className="flex flex-col flex-1 overflow-y-auto">
          {/* Brand/App Identity section */}
          <div className="p-6 border-b border-slate-150/50 dark:border-white/10 hidden md:flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-gradient-to-br from-indigo-505 to-purple-600 flex items-center justify-center shadow-md shadow-indigo-500/20 shrink-0">
              <Database className="w-5 h-5 text-white" />
            </div>
            <div>
              <h1 className="text-sm font-black text-slate-800 dark:text-white uppercase tracking-wider">
                DocuMind AI
              </h1>
              <p className="text-[10px] text-slate-400 dark:text-slate-500 font-semibold font-mono tracking-tight mt-0.5">
                Multi-Document Analysis
              </p>
            </div>
          </div>

          {/* Navigation Section */}
          <div className="px-4 py-6 flex flex-col gap-1.5 border-b border-slate-100/60 dark:border-white/5">
            <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest px-3 mb-1.5">
              Control Center
            </span>
            {navItems.map((item) => {
              const IconComp = item.icon;
              const isActive = activeTab === item.id;
              return (
                <button
                  id={`sidebar-nav-${item.id}`}
                  key={item.id}
                  onClick={() => handleNavClick(item.id)}
                  className={`w-full flex items-center gap-3 px-3 py-2.5 rounded-xl text-left cursor-pointer transition-all ${
                    isActive
                      ? 'bg-slate-200/50 dark:bg-slate-900/50 text-slate-900 dark:text-white font-semibold shadow-xs border border-slate-300/30 dark:border-white/5 backdrop-blur-xs'
                      : 'text-slate-500 dark:text-slate-400 hover:bg-slate-100/30 dark:hover:bg-slate-900/20 hover:text-slate-800 dark:hover:text-slate-200'
                  }`}
                >
                  <IconComp className={`h-4.5 w-4.5 shrink-0 ${item.color}`} />
                  <div className="min-w-0">
                    <div className="text-xs leading-none">{item.label}</div>
                    <div className="text-[9.5px] text-slate-400 dark:text-slate-500 font-medium truncate mt-1">{item.desc}</div>
                  </div>
                </button>
              );
            })}
          </div>

          {/* Active Warehouse Sourcing Context indicator */}
          <div className="px-4 py-6 border-b border-slate-100/60 dark:border-white/5">
            <div className="px-3 flex justify-between items-center mb-3">
              <span className="text-[10px] font-bold text-slate-400 dark:text-slate-500 uppercase tracking-widest">
                Chat Target Index
              </span>
              <Database className="h-3 w-3 text-slate-400" />
            </div>

            <div className="flex flex-col gap-1.5 max-h-[220px] overflow-y-auto pr-1">
              {documents.length === 0 ? (
                <p className="text-[10.5px] text-slate-405 pl-3 italic">
                  No documents synced with local storage.
                </p>
              ) : (
                documents.map((doc) => {
                  const inChatScope = selectedChatDocIds.includes(doc.id);
                  return (
                    <button
                      id={`sidebar-doc-toggle-${doc.id}`}
                      key={doc.id}
                      onClick={() => {
                        if (inChatScope) {
                          onSelectDocForChat(selectedChatDocIds.filter(id => id !== doc.id));
                        } else {
                          onSelectDocForChat([...selectedChatDocIds, doc.id]);
                        }
                      }}
                      className={`flex items-center gap-2 px-3 py-1.5 rounded-lg text-left text-[11px] w-full transition-all truncate border cursor-pointer ${
                        inChatScope
                          ? 'bg-indigo-50/60 dark:bg-indigo-950/20 text-indigo-750 dark:text-indigo-400 font-bold border-indigo-200/60 dark:border-indigo-900/50'
                          : 'text-slate-600 dark:text-slate-400 hover:bg-slate-100/30 dark:hover:bg-slate-900/20 border-transparent'
                      }`}
                    >
                      <div className={`h-1.5 w-1.5 rounded-full shrink-0 ${
                        inChatScope ? 'bg-indigo-500' : 'bg-slate-300 dark:bg-slate-700'
                      }`} />
                      <span className="truncate flex-1">{doc.title}</span>
                      {inChatScope && <Check className="h-3 w-3 text-indigo-500 shrink-0" />}
                    </button>
                  );
                })
              )}
            </div>

            {selectedChatDocIds.length > 0 && (
              <button
                id="btn-sidebar-reset-scope"
                onClick={() => onSelectDocForChat([])}
                className="w-full text-center mt-3 text-[10px] font-extrabold text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
              >
                Clear Sourcing Selection
              </button>
            )}
          </div>
        </div>

        {/* Bottom Drawer Control settings (e.g. Theme, credentials check) */}
        <div className="p-4 border-t border-slate-150/55 dark:border-white/5 flex justify-between items-center bg-white/20 dark:bg-slate-950/30 backdrop-blur-xs">
          <button
            id="sidebar-theme-toggle"
            onClick={onToggleTheme}
            className="flex items-center gap-2 py-1.5 px-3 bg-white/60 dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-700 dark:text-slate-300 hover:text-indigo-500 dark:hover:text-indigo-400 rounded-xl text-xs font-semibold shadow-2xs cursor-pointer transition-all"
          >
            {darkMode ? (
              <>
                <Sun className="h-3.5 w-3.5 text-amber-500" />
                <span>Light Mode</span>
              </>
            ) : (
              <>
                <Moon className="h-3.5 w-3.5 text-indigo-500" />
                <span>Dark Mode</span>
              </>
            )}
          </button>

          <span className="text-[10px] text-slate-400 dark:text-slate-500 font-mono font-medium">
            SSL Secure
          </span>
        </div>
      </div>

      {/* Floating Menu overlay (clicking closes menu) */}
      {mobileOpen && (
        <div
          id="sidebar-mobile-backdrop"
          onClick={toggleMobileMenu}
          className="fixed inset-0 bg-slate-900/40 backdrop-blur-xs z-20 md:hidden"
        />
      )}
    </>
  );
}
