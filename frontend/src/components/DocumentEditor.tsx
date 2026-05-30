import React, { useState, useRef, useEffect } from 'react';
import { Document } from '../types';
import { motion, AnimatePresence } from 'motion/react';
import {
  FileText, Plus, Trash2, Edit, Save, Tag, Folder, Upload,
  Layers, CheckSquare, Square, X, AlertCircle, FileUp, Sparkles, Filter
} from 'lucide-react';

interface DocumentEditorProps {
  documents: Document[];
  onRefresh: () => void;
  onSelectDocForChat: (ids: string[]) => void;
  selectedChatDocIds: string[];
}

export default function DocumentEditor({
  documents,
  onRefresh,
  onSelectDocForChat,
  selectedChatDocIds
}: DocumentEditorProps) {
  // Editing state
  const [editingDoc, setEditingDoc] = useState<Partial<Document> | null>(null);
  const [searchQuery, setSearchQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<string>('All');
  
  // Selection states (for batch operations)
  const [selectedIds, setSelectedIds] = useState<string[]>([]);
  const [isBulkEditing, setIsBulkEditing] = useState(false);
  const [bulkTag, setBulkTag] = useState('');
  const [bulkCategory, setBulkCategory] = useState('');

  // Drag and drop states
  const [isDragging, setIsDragging] = useState(false);
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  // Form input states
  const [formTitle, setFormTitle] = useState('');
  const [formCategory, setFormCategory] = useState('');
  const [formContent, setFormContent] = useState('');
  const [formTags, setFormTags] = useState('');

  // Sync editing Doc when selected
  const handleStartEdit = (doc: Document) => {
    setEditingDoc(doc);
    setFormTitle(doc.title);
    setFormCategory(doc.category);
    setFormContent(doc.content);
    setFormTags(doc.tags.join(', '));
    setErrorMsg(null);
  };

  const handleStartNew = () => {
    setEditingDoc({
      title: '',
      category: 'General',
      content: '',
      tags: []
    });
    setFormTitle('');
    setFormCategory('General');
    setFormContent('');
    setFormTags('');
    setErrorMsg(null);
  };

  // Save changes to API
  const handleSave = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!formTitle.trim()) {
      setErrorMsg('Document title is required.');
      return;
    }
    if (!formContent.trim()) {
      setErrorMsg('Document content cannot be empty.');
      return;
    }

    const tagsArray = formTags
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    try {
      if (editingDoc?.id) {
        // Update existing document
        const response = await fetch(`/api/documents/${editingDoc.id}`, {
          method: 'PUT',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: formTitle,
            category: formCategory,
            content: formContent,
            tags: tagsArray
          })
        });
        const result = await response.json();
        if (result.success) {
          setEditingDoc(null);
          onRefresh();
        } else {
          setErrorMsg(result.error || 'Failed to update document.');
        }
      } else {
        // Create new document
        const response = await fetch('/api/documents', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            title: formTitle,
            category: formCategory,
            content: formContent,
            tags: tagsArray
          })
        });
        const result = await response.json();
        if (result.success) {
          setEditingDoc(null);
          onRefresh();
        } else {
          setErrorMsg(result.error || 'Failed to create document.');
        }
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Error saving document.');
    }
  };

  // Single Delete
  const handleDelete = async (id: string) => {
    if (!confirm('Are you sure you want to delete this document from the database?')) return;
    try {
      const response = await fetch(`/api/documents/${id}`, { method: 'DELETE' });
      const result = await response.json();
      if (result.success) {
        onRefresh();
        if (editingDoc?.id === id) {
          setEditingDoc(null);
        }
      }
    } catch (err: any) {
      alert(`Delete failed: ${err.message}`);
    }
  };

  // Batch update trigger
  const handleBatchUpdate = async (e: React.FormEvent) => {
    e.preventDefault();
    if (selectedIds.length === 0) return;

    const tagsToAdd = bulkTag
      .split(',')
      .map(t => t.trim())
      .filter(t => t.length > 0);

    try {
      const response = await fetch('/api/documents/batch-update', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          documentIds: selectedIds,
          updatePayload: {
            tagsToAdd: tagsToAdd.length > 0 ? tagsToAdd : undefined,
            category: bulkCategory ? bulkCategory : undefined
          }
        })
      });
      const result = await response.json();
      if (result.success) {
        setIsBulkEditing(false);
        setBulkTag('');
        setBulkCategory('');
        setSelectedIds([]);
        onRefresh();
      }
    } catch (err: any) {
      alert(`Batch update failed: ${err.message}`);
    }
  };

  // Batch delete trigger
  const handleBatchDelete = async () => {
    if (selectedIds.length === 0) return;
    if (!confirm(`Are you sure you want to delete ALL ${selectedIds.length} selected documents?`)) return;

    try {
      for (const id of selectedIds) {
        await fetch(`/api/documents/${id}`, { method: 'DELETE' });
      }
      setSelectedIds([]);
      onRefresh();
    } catch (err: any) {
      alert(`Error during batch delete: ${err.message}`);
    }
  };

  // Drag & drop file processing
  const handleDragOver = (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(true);
  };

  const handleDragLeave = () => {
    setIsDragging(false);
  };

  const handleDrop = async (e: React.DragEvent) => {
    e.preventDefault();
    setIsDragging(false);
    setErrorMsg(null);

    const files = Array.from(e.dataTransfer.files) as File[];
    if (files.length === 0) return;

    progressBarFiles(files);
  };

  const fileInputRef = useRef<HTMLInputElement>(null);

  const handleFileSelect = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (e.target.files && e.target.files.length > 0) {
      progressBarFiles(Array.from(e.target.files));
    }
  };

  const progressBarFiles = async (files: File[]) => {
    const validTextFiles = files.filter(f => {
      const extension = f.name.split('.').pop()?.toLowerCase();
      return ['txt', 'md', 'json', 'csv', 'xml', 'html', 'js', 'ts', 'css'].includes(extension || '') || f.type.startsWith('text/');
    });

    if (validTextFiles.length === 0) {
      setErrorMsg('No readable text-based documents detected. Please drop .txt or .md files.');
      return;
    }

    const uploadPromises = validTextFiles.map(file => {
      return new Promise<any>((resolve, reject) => {
        const reader = new FileReader();
        reader.onload = async (event) => {
          try {
            const content = event.target?.result as string;
            // Classify category on file extension
            const ext = file.name.split('.').pop()?.toUpperCase() || 'TXT';
            const category = ext === 'MD' ? 'Markdown Documentation' : ext === 'CSV' ? 'Data Table' : 'General Report';
            
            resolve({
              title: file.name.substring(0, file.name.lastIndexOf('.')) || file.name,
              category,
              content,
              tags: [ext.toLowerCase(), 'uploaded']
            });
          } catch (err) {
            reject(err);
          }
        };
        reader.onerror = () => reject(new Error('File reading error'));
        reader.readAsText(file);
      });
    });

    try {
      const documentsToCreate = await Promise.all(uploadPromises);
      const response = await fetch('/api/documents', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify(documentsToCreate)
      });
      const result = await response.json();
      if (result.success) {
        onRefresh();
      } else {
        setErrorMsg('Upload processing failed on server.');
      }
    } catch (err: any) {
      setErrorMsg(err.message || 'Error occurred while processing file drop.');
    }
  };

  // Checkbox interactions
  const toggleSelectAll = () => {
    if (selectedIds.length === filteredDocs.length) {
      setSelectedIds([]);
    } else {
      setSelectedIds(filteredDocs.map(d => d.id));
    }
  };

  const toggleSelectDoc = (id: string) => {
    if (selectedIds.includes(id)) {
      setSelectedIds(selectedIds.filter(item => item !== id));
    } else {
      setSelectedIds([...selectedIds, id]);
    }
  };

  // Filters logic
  const categoriesList = ['All', ...Array.from(new Set(documents.map(d => d.category)))];

  const filteredDocs = documents.filter(doc => {
    const matchesSearch = doc.title.toLowerCase().includes(searchQuery.toLowerCase()) || 
                          doc.content.toLowerCase().includes(searchQuery.toLowerCase()) ||
                          doc.tags.some(t => t.toLowerCase().includes(searchQuery.toLowerCase()));
    const matchesCategory = selectedCategory === 'All' || doc.category === selectedCategory;
    return matchesSearch && matchesCategory;
  });

  return (
    <div className="grid grid-cols-1 lg:grid-cols-12 gap-6 items-start">
      {/* List / Table grid layout */}
      <div className="lg:col-span-7 flex flex-col gap-4">
        {/* Search, Filter & Actions layout */}
        <div className="glass-panel rounded-2xl p-4 shadow-sm flex flex-col gap-3 relative overflow-hidden">
          <div className="absolute -right-6 -top-6 w-24 h-24 bg-indigo-505/5 rounded-full blur-xl pointer-events-none"></div>
          <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 relative z-10">
            <h3 className="font-bold text-slate-855 dark:text-slate-100 flex items-center gap-2">
              <Layers className="h-4.5 w-4.5 text-indigo-500" />
              Document Warehouse ({filteredDocs.length})
            </h3>
            <button
              id="btn-add-doc-new"
              onClick={handleStartNew}
              className="flex items-center gap-1.5 py-1.5 px-3.5 bg-indigo-600 hover:bg-indigo-500 dark:bg-indigo-555 dark:hover:bg-indigo-600/80 text-white text-xs font-semibold rounded-xl cursor-pointer shadow-sm transition-all hover:scale-[1.02]"
            >
              <Plus className="h-4 w-4" />
              New Document
            </button>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-12 gap-2 mt-1 relative z-10">
            <div className="relative sm:col-span-7">
              <input
                id="inp-archive-search"
                type="text"
                placeholder="Search by title, contents, or tags..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full glass-input text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 rounded-xl py-2 pl-3 pr-10 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all"
              />
              <Filter className="absolute right-3.5 top-2.5 h-3.5 w-3.5 text-slate-400" />
            </div>

            <div className="sm:col-span-5 relative">
              <select
                id="sel-archive-category"
                value={selectedCategory}
                onChange={(e) => setSelectedCategory(e.target.value)}
                className="w-full appearance-none glass-input text-slate-800 dark:text-slate-300 rounded-xl py-2 px-3.5 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 cursor-pointer transition-all"
              >
                {categoriesList.map((cat, idx) => (
                  <option key={idx} value={cat} className="bg-white dark:bg-slate-900">{cat}</option>
                ))}
              </select>
              <span className="absolute right-3.5 top-3 pointer-events-none text-[8px] text-slate-400">▼</span>
            </div>
          </div>
        </div>

        {/* Drag & Drop Area */}
        <div
          id="uploader-drop-container"
          onDragOver={handleDragOver}
          onDragLeave={handleDragLeave}
          onDrop={handleDrop}
          onClick={() => fileInputRef.current?.click()}
          className={`flex flex-col items-center justify-center border-2 border-dashed rounded-2xl p-6 text-center cursor-pointer transition-all duration-250 ${
            isDragging
              ? 'border-indigo-500 bg-indigo-50/25 dark:bg-indigo-950/20 scale-[0.99]'
              : 'border-slate-200/50 dark:border-white/10 bg-white/10 dark:bg-white/5 backdrop-blur-md hover:bg-white/20 dark:hover:bg-white/10 shadow-3xs'
          }`}
        >
          <input
            id="file-bulk-uploader-input"
            type="file"
            multiple
            ref={fileInputRef}
            onChange={handleFileSelect}
            className="hidden"
            accept=".txt,.md,.json,.csv"
          />
          <FileUp className={`h-8 w-8 mb-2 ${isDragging ? 'text-indigo-505 animate-bounce' : 'text-slate-400 dark:text-slate-500'}`} />
          <h4 className="text-xs font-bold text-slate-700 dark:text-slate-200">
            {isDragging ? 'Drop your documents here!' : 'Drag & drop text / markdown docs'}
          </h4>
          <p className="text-[10px] text-slate-400 dark:text-slate-500 mt-1.5 max-w-sm leading-relaxed">
            Accepts <code className="font-mono bg-slate-150/40 dark:bg-slate-900 rounded px-1 text-[9.5px]">.txt</code>, <code className="font-mono bg-slate-150/40 dark:bg-slate-900 rounded px-1 text-[9.5px]">.md</code> files. Automated multi-document indexing.
          </p>
        </div>

        {/* Error notification */}
        <AnimatePresence>
          {errorMsg && (
            <motion.div
              initial={{ opacity: 0, height: 0 }}
              animate={{ opacity: 1, height: 'auto' }}
              exit={{ opacity: 0, height: 0 }}
              className="p-3.5 bg-rose-50 dark:bg-rose-950/20 text-rose-600 dark:text-rose-400 border border-rose-100 dark:border-rose-950 rounded-xl text-xs flex items-start gap-2"
            >
              <AlertCircle className="h-4 w-4 shrink-0 mt-0.5" />
              <div>
                <span className="font-semibold">Validation Error:</span> {errorMsg}
              </div>
            </motion.div>
          )}
        </AnimatePresence>

        {/* Bulk Action Panel Overlay (visible when items selected) */}
        <AnimatePresence>
          {selectedIds.length > 0 && (
            <motion.div
              initial={{ opacity: 0, y: -10 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0, y: -10 }}
              className="bg-indigo-50 dark:bg-indigo-950/30 border border-indigo-100 dark:border-indigo-900/50 rounded-2xl p-4 shadow-sm flex flex-col gap-3"
            >
              <div className="flex justify-between items-center">
                <div className="flex items-center gap-2">
                  <CheckSquare className="h-4 w-4 text-indigo-600" />
                  <span className="text-xs font-bold text-slate-700 dark:text-indigo-300">
                    {selectedIds.length} Document(s) Checked
                  </span>
                </div>
                <div className="flex gap-2">
                  <button
                    id="btn-bulk-edit-toggle"
                    onClick={() => setIsBulkEditing(!isBulkEditing)}
                    className="text-[11px] font-bold text-indigo-600 dark:text-indigo-400 hover:underline cursor-pointer"
                  >
                    {isBulkEditing ? 'Cancel Edit' : 'Batch Update Properties'}
                  </button>
                  <span className="text-slate-300 dark:text-slate-700">|</span>
                  <button
                    id="btn-bulk-delete"
                    onClick={handleBatchDelete}
                    className="text-[11px] font-bold text-rose-500 hover:underline flex items-center gap-1 cursor-pointer"
                  >
                    <Trash2 className="h-3 w-3" />
                    Delete Selected
                  </button>
                </div>
              </div>

              {isBulkEditing && (
                <form id="form-bulk-properties" onSubmit={handleBatchUpdate} className="flex flex-col sm:flex-row gap-2 border-t border-indigo-100/40 dark:border-indigo-950 pt-3">
                  <div className="flex-1">
                    <input
                      id="inp-bulk-tags"
                      type="text"
                      placeholder="Add tag(s) (comma separated)"
                      value={bulkTag}
                      onChange={(e) => setBulkTag(e.target.value)}
                      className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 rounded-xl py-1.5 px-3 text-[11px] focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all"
                    />
                  </div>
                  <div className="flex-1">
                    <input
                      id="inp-bulk-category"
                      type="text"
                      placeholder="Set Category (e.g. Finance)"
                      value={bulkCategory}
                      onChange={(e) => setBulkCategory(e.target.value)}
                      className="w-full bg-white dark:bg-slate-900 border border-slate-200 dark:border-slate-800 text-slate-800 dark:text-slate-200 placeholder-slate-400 dark:placeholder-slate-500 rounded-xl py-1.5 px-3 text-[11px] focus:outline-none focus:ring-1 focus:ring-indigo-500 transition-all"
                    />
                  </div>
                  <button
                    id="btn-bulk-dispatch-save"
                    type="submit"
                    className="bg-indigo-600 hover:bg-indigo-500 dark:bg-indigo-500 text-white font-semibold text-[11px] py-1.5 px-4 rounded-xl cursor-pointer shadow-sm transition-all"
                  >
                    Apply Changes
                  </button>
                </form>
              )}
            </motion.div>
          )}
        </AnimatePresence>

        {/* Real-time document selection cards */}
        <div className="flex flex-col gap-2 max-h-[460px] overflow-y-auto pr-1">
          {filteredDocs.length === 0 ? (
            <div className="text-center py-12 bg-white dark:bg-slate-900/60 border border-slate-200 dark:border-slate-800 rounded-2xl">
              <FileText className="h-10 w-10 text-slate-300 dark:text-slate-600 mx-auto mb-2" />
              <p className="text-xs text-slate-500 dark:text-slate-400">No documents matches your search metrics.</p>
            </div>
          ) : (
            filteredDocs.map((doc) => {
              const checked = selectedIds.includes(doc.id);
              const inScopeForChat = selectedChatDocIds.includes(doc.id);

              return (
                <div
                  id={`doc-card-${doc.id}`}
                  key={doc.id}
                  className={`p-4 rounded-2xl glass-card transition-all flex flex-col sm:flex-row justify-between items-start sm:items-center gap-3 relative overflow-hidden ${
                    inScopeForChat
                      ? 'border-indigo-400 dark:border-indigo-500/40 bg-indigo-50/10 dark:bg-indigo-950/10 ring-1 ring-indigo-400/20 shadow-xs'
                      : 'hover:bg-white/40 dark:hover:bg-slate-900/30 hover:border-slate-300/40 dark:hover:border-white/10'
                  }`}
                >
                  <div className="flex items-start gap-3 flex-1 min-w-0 relative z-10">
                    <button
                      id={`chk-doc-toggle-${doc.id}`}
                      onClick={() => toggleSelectDoc(doc.id)}
                      className="mt-1 text-slate-400 hover:text-indigo-500 cursor-pointer focus:outline-none shrink-0"
                    >
                      {checked ? (
                        <CheckSquare className="h-4.5 w-4.5 text-indigo-600" />
                      ) : (
                        <Square className="h-4.5 w-4.5 text-slate-300 dark:text-slate-700" />
                      )}
                    </button>
                    
                    <div className="min-w-0 flex-1">
                      <div className="flex items-center gap-2">
                        <FileText className="h-4 w-4 text-indigo-500 shrink-0" />
                        <h4 className="text-xs font-bold text-slate-800 dark:text-slate-100 truncate">{doc.title}</h4>
                      </div>
                      
                      <p className="text-[10px] text-slate-400 dark:text-slate-500 truncate mt-1">
                        {doc.content.substring(0, 80)}...
                      </p>
                      
                      <div className="flex flex-wrap gap-1.5 mt-2">
                        <span className="text-[9px] font-semibold bg-slate-100 dark:bg-slate-800 text-slate-600 dark:text-slate-400 px-2 py-0.5 rounded-lg flex items-center gap-1">
                          <Folder className="h-2.5 w-2.5" />
                          {doc.category}
                        </span>
                        
                        {doc.tags.map((tag, i) => (
                          <span key={i} className="text-[9px] font-medium bg-indigo-50 dark:bg-indigo-950/40 text-indigo-600 dark:text-indigo-400 px-1.5 py-0.5 rounded-md">
                            #{tag}
                          </span>
                        ))}
                      </div>
                    </div>
                  </div>

                  <div className="flex sm:flex-col items-end gap-2.5 w-full sm:w-auto mt-2 sm:mt-0 justify-between sm:justify-start border-t sm:border-t-0 pt-2 sm:pt-0 border-slate-100 dark:border-slate-800">
                    <div className="flex items-center gap-1.5">
                      <span className="text-[9px] font-mono font-medium text-slate-500">
                        {doc.wordCount} words
                      </span>
                      <span className={`text-[9px] font-semibold px-2 py-0.5 rounded-full ${
                        doc.sentiment === 'Positive' ? 'bg-emerald-50 text-emerald-600 dark:bg-emerald-950/20 dark:text-emerald-400' :
                        doc.sentiment === 'Analytical' ? 'bg-indigo-50 text-indigo-600 dark:bg-indigo-950/20 dark:text-indigo-400' :
                        doc.sentiment === 'Critical' ? 'bg-rose-55 dark:bg-rose-95a/20 text-rose-600 dark:text-rose-455' :
                        'bg-sky-50 text-sky-600 dark:bg-sky-950/20 dark:text-sky-400'
                      }`}>
                        {doc.sentiment}
                      </span>
                    </div>

                    <div className="flex items-center gap-2">
                      <button
                        id={`btn-scope-selector-${doc.id}`}
                        onClick={() => {
                          if (inScopeForChat) {
                            onSelectDocForChat(selectedChatDocIds.filter(id => id !== doc.id));
                          } else {
                            onSelectDocForChat([...selectedChatDocIds, doc.id]);
                          }
                        }}
                        className={`text-[9px] font-bold py-1 px-2.5 rounded-lg border cursor-pointer transition-all ${
                          inScopeForChat
                            ? 'bg-indigo-650 text-white border-transparent'
                            : 'bg-white dark:bg-slate-900 hover:bg-slate-50 text-slate-600 dark:text-slate-400 border-slate-200 dark:border-slate-800'
                        }`}
                      >
                        {inScopeForChat ? 'Selected' : 'Chat Target'}
                      </button>

                      <button
                        id={`btn-edit-inline-${doc.id}`}
                        onClick={() => handleStartEdit(doc)}
                        className="p-1 px-1.5 hover:bg-slate-100 dark:hover:bg-slate-800 rounded text-slate-500 dark:text-slate-400 hover:text-slate-800 cursor-pointer transition-all"
                      >
                        <Edit className="h-3.5 w-3.5" />
                      </button>
                      
                      <button
                        id={`btn-delete-inline-${doc.id}`}
                        onClick={() => handleDelete(doc.id)}
                        className="p-1 px-1.5 hover:bg-rose-50 dark:hover:bg-rose-950/20 rounded text-slate-400 hover:text-rose-600 cursor-pointer transition-all"
                      >
                        <Trash2 className="h-3.5 w-3.5" />
                      </button>
                    </div>
                  </div>
                </div>
              );
            })
          )}
        </div>
      </div>

      {/* Editor Panel */}
      <div className="lg:col-span-5">
        <AnimatePresence mode="wait">
          {editingDoc ? (
            <motion.div
              id="form-panel-active"
              key="active-editor"
              initial={{ opacity: 0, scale: 0.98 }}
              animate={{ opacity: 1, scale: 1 }}
              exit={{ opacity: 0, scale: 0.98 }}
              className="glass-panel rounded-2xl shadow-md p-5 flex flex-col gap-4 relative overflow-hidden"
            >
              {/* Highlight bar */}
              <div className="absolute top-0 left-0 right-0 h-[3px] bg-gradient-to-r from-indigo-500 to-purple-600" />
              
              <div className="flex justify-between items-center pb-2 border-b border-slate-100 dark:border-slate-800">
                <div className="flex items-center gap-1.5">
                  <Sparkles className="h-4 w-4 text-indigo-500" />
                  <h3 className="font-bold text-xs text-slate-700 dark:text-slate-300">
                    {editingDoc.id ? 'EDIT DOCUMENT PORTAL' : 'CREATE NEW DOCUMENT'}
                  </h3>
                </div>
                <button
                  id="btn-close-editor"
                  onClick={() => setEditingDoc(null)}
                  className="p-1 rounded-full text-slate-400 hover:bg-slate-100 dark:hover:bg-slate-800 hover:text-slate-600 transition-all cursor-pointer"
                >
                  <X className="h-4 w-4" />
                </button>
              </div>

              <form onSubmit={handleSave} className="flex flex-col gap-3">
                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-slate-505 dark:text-slate-400 uppercase tracking-widest">Document Title</label>
                  <input
                    id="inp-form-title"
                    type="text"
                    value={formTitle}
                    onChange={(e) => setFormTitle(e.target.value)}
                    placeholder="Enter document title..."
                    className="w-full glass-input text-slate-800 dark:text-slate-100 rounded-xl py-2 px-3 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all"
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-slate-505 dark:text-slate-400 uppercase tracking-widest">Category / Sector</label>
                  <input
                    id="inp-form-category"
                    type="text"
                    value={formCategory}
                    onChange={(e) => setFormCategory(e.target.value)}
                    placeholder="e.g. Executive Summary, Statistics..."
                    className="w-full glass-input text-slate-800 dark:text-slate-100 rounded-xl py-2 px-3 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all"
                  />
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-slate-505 dark:text-slate-400 uppercase tracking-widest">Tags (Comma-separated)</label>
                  <div className="relative">
                    <input
                      id="inp-form-tags"
                      type="text"
                      value={formTags}
                      onChange={(e) => setFormTags(e.target.value)}
                      placeholder="tag1, tag2, roadmap..."
                      className="w-full glass-input text-slate-800 dark:text-slate-100 rounded-xl py-2 pl-3 pr-8 text-xs focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all"
                    />
                    <Tag className="absolute right-3 top-2.5 h-3.5 w-3.5 text-slate-400" />
                  </div>
                </div>

                <div className="flex flex-col gap-1">
                  <label className="text-[10px] font-bold text-slate-505 dark:text-slate-400 uppercase tracking-widest">Document Text Content</label>
                  <textarea
                    id="txt-form-content"
                    value={formContent}
                    onChange={(e) => setFormContent(e.target.value)}
                    placeholder="Type or paste the document content here..."
                    rows={12}
                    className="w-full glass-input text-slate-800 dark:text-slate-100 rounded-xl p-3 text-xs font-sans focus:outline-none focus:ring-1.5 focus:ring-indigo-500 transition-all leading-relaxed resize-y"
                  />
                </div>

                <button
                  id="btn-form-dispatch-save"
                  type="submit"
                  className="w-full py-2.5 px-4 bg-indigo-650 hover:bg-indigo-600 dark:bg-indigo-500 dark:hover:bg-indigo-500 text-white font-bold text-xs rounded-xl shadow-md cursor-pointer transition-all flex items-center justify-center gap-1.5"
                >
                  <Save className="h-4 w-4" />
                  Save Database Entry
                </button>
              </form>
            </motion.div>
          ) : (
            <motion.div
              id="form-panel-inactive"
              key="inactive-editor"
              initial={{ opacity: 0 }}
              animate={{ opacity: 1 }}
              exit={{ opacity: 0 }}
              className="glass-panel border-2 border-dashed border-slate-200 dark:border-slate-800 rounded-2xl p-8 text-center flex flex-col items-center justify-center min-h-[380px] shadow-3xs"
            >
              <FileText className="h-10 w-10 text-slate-400/80 mb-3 animate-pulse" />
              <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">Document Hub Editor</h3>
              <p className="text-xs text-slate-400 max-w-xs mt-1 leading-relaxed">
                Choose a document from the warehouse to modify its core content, categorisations, or create a brand new indexed entry.
              </p>
              <button
                id="btn-trigger-new-placeholder"
                onClick={handleStartNew}
                className="mt-4 flex items-center gap-1.5 px-3 py-1.5 hover:bg-slate-100 dark:hover:bg-slate-800/80 text-xs font-bold text-indigo-500 rounded-xl transition-all border border-indigo-200/50 dark:border-indigo-950 cursor-pointer"
              >
                <Plus className="h-3.5 w-3.5" />
                Initialize Empty Entry
              </button>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    </div>
  );
}
