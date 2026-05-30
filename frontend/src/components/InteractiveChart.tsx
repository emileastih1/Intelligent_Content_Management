import React, { useState } from 'react';
import { Document } from '../types';
import { motion, AnimatePresence } from 'motion/react';
import { BarChart2, AreaChart, Smile, Tag, HelpCircle, Activity, Download } from 'lucide-react';

interface InteractiveChartProps {
  documents: Document[];
}

export default function InteractiveChart({ documents }: InteractiveChartProps) {
  const [chartType, setChartType] = useState<'wordCount' | 'sentiment' | 'category'>('wordCount');
  const [hoveredIndex, setHoveredIndex] = useState<number | null>(null);
  const [tooltipPos, setTooltipPos] = useState({ x: 0, y: 0 });

  if (documents.length === 0) {
    return (
      <div className="flex flex-col items-center justify-center p-12 text-center border-2 border-dashed border-slate-200 dark:border-white/10 rounded-2xl glass-panel shadow-3xs">
        <Activity className="h-12 w-12 text-slate-400 mb-3 animate-pulse" />
        <h3 className="font-semibold text-slate-700 dark:text-slate-300">No Analytics Data</h3>
        <p className="text-sm text-slate-500 max-w-sm mt-1">
          Store or upload at least one document in the database to render interactive metrics.
        </p>
      </div>
    );
  }

  // Handle Chart Dimensions
  const padding = 45;
  const chartWidth = 600;
  const chartHeight = 280;

  // 1. Data Preparation - Word Count Data
  const wordCountData = documents.map((doc, i) => ({
    label: doc.title.length > 18 ? doc.title.substring(0, 15) + '...' : doc.title,
    fullTitle: doc.title,
    value: doc.wordCount,
    category: doc.category,
    sentiment: doc.sentiment,
    raw: doc
  }));

  const maxWordCount = Math.max(...wordCountData.map(d => d.value), 100);

  // 2. Data Preparation - Sentiment Count
  const sentimentCounts = documents.reduce((acc, doc) => {
    acc[doc.sentiment] = (acc[doc.sentiment] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const sentimentData = Object.entries(sentimentCounts).map(([key, val]) => ({
    label: key,
    value: val,
    color: key === 'Positive' ? 'bg-emerald-500' :
           key === 'Analytical' ? 'bg-indigo-500' :
           key === 'Informative' ? 'bg-sky-500' :
           key === 'Critical' ? 'bg-rose-500' : 'bg-amber-500'
  }));

  // 3. Data Preparation - Category Count
  const categoryCounts = documents.reduce((acc, doc) => {
    acc[doc.category] = (acc[doc.category] || 0) + 1;
    return acc;
  }, {} as Record<string, number>);

  const categoryData = Object.entries(categoryCounts).map(([key, val]) => ({
    label: key,
    value: val
  }));

  const maxCategoryCount = Math.max(...categoryData.map(d => d.value), 1);

  const handleExportCSV = () => {
    let csvContent = "";
    let fileName = "";

    if (chartType === 'wordCount') {
      const headers = ['Title', 'Word Count', 'Category', 'Tone', 'Character Count'];
      const rows = documents.map(doc => [
        `"${doc.title.replace(/"/g, '""')}"`,
        doc.wordCount,
        `"${doc.category.replace(/"/g, '""')}"`,
        `"${doc.sentiment.replace(/"/g, '""')}"`,
        doc.charCount
      ]);
      csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      fileName = "document_word_densities.csv";
    } else if (chartType === 'category') {
      const headers = ['Category', 'Document Count'];
      const rows = categoryData.map(d => [
        `"${d.label.replace(/"/g, '""')}"`,
        d.value
      ]);
      csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      fileName = "document_categories.csv";
    } else if (chartType === 'sentiment') {
      const headers = ['Tone', 'Document Count', 'Percentage'];
      const totalDocs = documents.length || 1;
      const rows = sentimentData.map(d => [
        `"${d.label.replace(/"/g, '""')}"`,
        d.value,
        `"${Math.round((d.value / totalDocs) * 100)}%"`
      ]);
      csvContent = [headers.join(','), ...rows.map(r => r.join(','))].join('\n');
      fileName = "document_tone_metrics.csv";
    }

    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement("a");
    link.setAttribute("href", url);
    link.setAttribute("download", fileName);
    link.style.visibility = 'hidden';
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  };

  // Render Word Count Area Chart
  const renderWordCountChart = () => {
    const pointsCount = wordCountData.length;
    const stepX = (chartWidth - padding * 2) / Math.max(pointsCount - 1, 1);
    
    // Calculate SVG coordinates
    const coordinates = wordCountData.map((d, i) => {
      const x = padding + (pointsCount === 1 ? (chartWidth - padding * 2) / 2 : i * stepX);
      const ratio = d.value / maxWordCount;
      const y = chartHeight - padding - (ratio * (chartHeight - padding * 2));
      return { x, y, ...d };
    });

    // Construct path string for dynamic Area Fill and smooth Line
    let areaPath = '';
    let linePath = '';

    if (coordinates.length > 0) {
      linePath = `M ${coordinates[0].x} ${coordinates[0].y}`;
      areaPath = `M ${coordinates[0].x} ${chartHeight - padding}`;
      areaPath += ` L ${coordinates[0].x} ${coordinates[0].y}`;

      for (let i = 1; i < coordinates.length; i++) {
        // Curve construction
        const prev = coordinates[i - 1];
        const curr = coordinates[i];
        const cpX1 = prev.x + (curr.x - prev.x) / 2;
        const cpY1 = prev.y;
        const cpX2 = prev.x + (curr.x - prev.x) / 2;
        const cpY2 = curr.y;

        linePath += ` C ${cpX1} ${cpY1}, ${cpX2} ${cpY2}, ${curr.x} ${curr.y}`;
        areaPath += ` C ${cpX1} ${cpY1}, ${cpX2} ${cpY2}, ${curr.x} ${curr.y}`;
      }

      areaPath += ` L ${coordinates[coordinates.length - 1].x} ${chartHeight - padding} Z`;
    }

    return (
      <div className="relative">
        <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full h-auto overflow-visible">
          {/* Horizontal Grid lines */}
          {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
            const hY = chartHeight - padding - ratio * (chartHeight - padding * 2);
            return (
              <g key={idx}>
                <line 
                  x1={padding} 
                  y1={hY} 
                  x2={chartWidth - padding} 
                  y2={hY} 
                  className="stroke-slate-200 dark:stroke-slate-800" 
                  strokeDasharray="4 4" 
                />
                <text 
                  x={padding - 10} 
                  y={hY + 4} 
                  textAnchor="end" 
                  className="fill-slate-400 dark:fill-slate-500 font-mono text-[9px]"
                >
                  {Math.round(ratio * maxWordCount)}
                </text>
              </g>
            );
          })}

          {/* Connected Curved Area */}
          {coordinates.length > 1 && (
            <path
              d={areaPath}
              className="fill-indigo-50/40 dark:fill-indigo-950/20 stroke-none"
            />
          )}

          {/* Primary Trend Line */}
          {coordinates.length > 1 ? (
            <path
              d={linePath}
              fill="none"
              className="stroke-indigo-600 dark:stroke-indigo-400"
              strokeWidth="2.5"
            />
          ) : (
            coordinates.length === 1 && (
              <circle cx={coordinates[0].x} cy={coordinates[0].y} r="6" className="fill-indigo-600 dark:fill-indigo-400" />
            )
          )}

          {/* Interactive Circle Datapoints */}
          {coordinates.map((pt, idx) => (
            <g key={idx}>
              {/* x-axis ticks/labels */}
              <text
                x={pt.x}
                y={chartHeight - padding + 18}
                textAnchor="middle"
                className="fill-slate-400 dark:fill-slate-500 font-sans text-[8px] sm:text-[9px] max-w-[10px] truncate"
              >
                {pt.label}
              </text>

              <circle
                cx={pt.x}
                cy={pt.y}
                r={hoveredIndex === idx ? '7' : '4.5'}
                className="fill-white stroke-indigo-600 dark:stroke-indigo-400 cursor-pointer transition-all duration-150 shadow-md"
                strokeWidth={hoveredIndex === idx ? '3.5' : '2'}
                onMouseEnter={(e) => {
                  setHoveredIndex(idx);
                  const bounds = e.currentTarget.getBoundingClientRect();
                  setTooltipPos({ x: bounds.left, y: bounds.top - 120 });
                }}
                onMouseLeave={() => setHoveredIndex(null)}
              />
            </g>
          ))}
        </svg>

        {/* Hover Tooltip Overlay */}
        <AnimatePresence>
          {hoveredIndex !== null && coordinates[hoveredIndex] && (
            <motion.div
              initial={{ opacity: 0, y: 5 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              style={{
                position: 'fixed',
                left: tooltipPos.x + 10,
                top: tooltipPos.y,
                zIndex: 9999
              }}
              className="pointer-events-none p-3 bg-slate-900 text-white rounded-xl shadow-xl text-xs flex flex-col gap-1 w-52 border border-slate-700/50 backdrop-blur"
            >
              <div className="font-semibold text-indigo-300 truncate">{coordinates[hoveredIndex].fullTitle}</div>
              <div className="flex justify-between items-center mt-1 border-t border-slate-800 pt-1">
                <span className="text-slate-400">Word Count:</span>
                <span className="font-mono font-medium text-slate-100">{coordinates[hoveredIndex].value} words</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Category:</span>
                <span className="text-slate-100 py-0.5 px-1.5 bg-slate-800 rounded">{coordinates[hoveredIndex].category}</span>
              </div>
              <div className="flex justify-between items-center">
                <span className="text-slate-400">Tone:</span>
                <span className="text-indigo-200">{coordinates[hoveredIndex].sentiment}</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    );
  };

  // Render Category Column Chart
  const renderCategoryChart = () => {
    const colCount = categoryData.length;
    const colWidth = (chartWidth - padding * 2) / Math.max(colCount, 1);
    const usableWidth = colWidth * 0.6;

    return (
      <div className="relative">
        <svg viewBox={`0 0 ${chartWidth} ${chartHeight}`} className="w-full h-auto overflow-visible">
          {[0, 0.25, 0.5, 0.75, 1].map((ratio, idx) => {
            const hY = chartHeight - padding - ratio * (chartHeight - padding * 2);
            return (
              <g key={idx}>
                <line 
                  x1={padding} 
                  y1={hY} 
                  x2={chartWidth - padding} 
                  y2={hY} 
                  className="stroke-slate-200 dark:stroke-slate-800" 
                  strokeDasharray="4 4" 
                />
                <text 
                  x={padding - 10} 
                  y={hY + 4} 
                  textAnchor="end" 
                  className="fill-slate-400 dark:fill-slate-500 font-mono text-[9px]"
                >
                  {Math.round(ratio * maxCategoryCount)}
                </text>
              </g>
            );
          })}

          {categoryData.map((d, idx) => {
            const colX = padding + (idx * colWidth) + (colWidth - usableWidth) / 2;
            const ratio = d.value / maxCategoryCount;
            const colH = ratio * (chartHeight - padding * 2);
            const colY = chartHeight - padding - colH;
            const isHovered = hoveredIndex === idx;

            return (
              <g key={idx}>
                {/* Column block */}
                <rect
                  id={`cat-bar-${idx}`}
                  x={colX}
                  y={colY}
                  width={usableWidth}
                  height={Math.max(colH, 1)}
                  rx="6"
                  className={`${
                    isHovered ? 'fill-indigo-500' : 'fill-indigo-600/80 dark:fill-indigo-500/70'
                  } cursor-pointer transition-all duration-200`}
                  onMouseEnter={(e) => {
                    setHoveredIndex(idx);
                    const bounds = e.currentTarget.getBoundingClientRect();
                    setTooltipPos({ x: bounds.left, y: bounds.top - 100 });
                  }}
                  onMouseLeave={() => setHoveredIndex(null)}
                />

                {/* Value labels */}
                {d.value > 0 && (
                  <text
                    x={colX + usableWidth / 2}
                    y={colY - 6}
                    textAnchor="middle"
                    className="fill-slate-700 dark:fill-slate-300 font-mono text-[10px] font-semibold"
                  >
                    {d.value}
                  </text>
                )}

                {/* Category name tag bottom */}
                <text
                  x={colX + usableWidth / 2}
                  y={chartHeight - padding + 18}
                  textAnchor="middle"
                  className="fill-slate-400 dark:fill-slate-500 font-sans text-[8.5px] sm:text-[10px] font-medium"
                >
                  {d.label}
                </text>
              </g>
            );
          })}
        </svg>

        <AnimatePresence>
          {hoveredIndex !== null && categoryData[hoveredIndex] && (
            <motion.div
              initial={{ opacity: 0, y: 5 }}
              animate={{ opacity: 1, y: 0 }}
              exit={{ opacity: 0 }}
              style={{
                position: 'fixed',
                left: tooltipPos.x + 10,
                top: tooltipPos.y,
                zIndex: 9999
              }}
              className="pointer-events-none p-3 bg-slate-900 text-white rounded-xl shadow-xl text-xs flex flex-col gap-1 border border-slate-700/50 backdrop-blur"
            >
              <div className="font-semibold text-indigo-300">Category: {categoryData[hoveredIndex].label}</div>
              <div className="flex justify-between gap-6 items-center border-t border-slate-800 pt-1 mt-1">
                <span className="text-slate-400">Count in DB:</span>
                <span className="font-mono font-semibold text-white">{categoryData[hoveredIndex].value} file(s)</span>
              </div>
            </motion.div>
          )}
        </AnimatePresence>
      </div>
    );
  };

  // Calculations for static totals
  const totalWords = documents.reduce((acc, d) => acc + d.wordCount, 0);
  const avgWords = Math.round(totalWords / documents.length);
  const totalChars = documents.reduce((acc, d) => acc + d.charCount, 0);

  return (
    <div id="chart-panel" className="glass-panel rounded-2xl shadow-sm p-6 overflow-hidden relative">
      <div className="absolute -left-16 -bottom-16 w-36 h-36 bg-purple-500/5 rounded-full blur-2xl pointer-events-none"></div>
      {/* Tab Switcher */}
      <div className="flex flex-col sm:flex-row justify-between items-start sm:items-center gap-4 mb-6 pb-4 border-b border-white/10 dark:border-slate-800/60 relative z-10">
        <div>
          <h2 className="text-lg font-bold text-slate-800 dark:text-slate-100 flex items-center gap-2">
            <Activity className="h-5 w-5 text-indigo-500" />
            Interactive Analysis Center
          </h2>
          <p className="text-xs text-slate-500 dark:text-slate-400 mt-0.5">
            Dynamic visualizations tracking database density, tones, and categories.
          </p>
        </div>

        <div className="flex flex-col sm:flex-row items-stretch sm:items-center gap-2.5 w-full sm:w-auto">
          <div className="flex bg-white/5 dark:bg-slate-950/20 backdrop-blur-md p-0.5 rounded-xl border border-white/10 dark:border-slate-700/50 flex-1 sm:flex-initial">
            <button
              id="btn-wc-metric"
              onClick={() => setChartType('wordCount')}
              className={`flex items-center justify-center gap-1.5 flex-1 sm:flex-initial py-1.5 px-3.5 rounded-lg text-xs font-semibold cursor-pointer transition-all ${
                chartType === 'wordCount'
                  ? 'bg-indigo-600 dark:bg-indigo-500 text-white shadow-sm hover:scale-[1.01]'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-300 hover:bg-white/5'
              }`}
            >
              <AreaChart className="h-3.5 w-3.5" />
              Word Densities
            </button>
            
            <button
              id="btn-cat-metric"
              onClick={() => setChartType('category')}
              className={`flex items-center justify-center gap-1.5 flex-1 sm:flex-initial py-1.5 px-3.5 rounded-lg text-xs font-semibold cursor-pointer transition-all ${
                chartType === 'category'
                  ? 'bg-indigo-600 dark:bg-indigo-500 text-white shadow-sm hover:scale-[1.01]'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-300 hover:bg-white/5'
              }`}
            >
              <BarChart2 className="h-3.5 w-3.5" />
              Categories
            </button>

            <button
              id="btn-senti-metric"
              onClick={() => setChartType('sentiment')}
              className={`flex items-center justify-center gap-1.5 flex-1 sm:flex-initial py-1.5 px-3.5 rounded-lg text-xs font-semibold cursor-pointer transition-all ${
                chartType === 'sentiment'
                  ? 'bg-indigo-600 dark:bg-indigo-500 text-white shadow-sm hover:scale-[1.01]'
                  : 'text-slate-600 dark:text-slate-400 hover:text-slate-800 dark:hover:text-slate-300 hover:bg-white/5'
              }`}
            >
              <Smile className="h-3.5 w-3.5" />
              Tone Metrics
            </button>
          </div>

          <button
            id="btn-export-csv"
            onClick={handleExportCSV}
            className="flex items-center justify-center gap-1.5 py-2 px-3.5 bg-slate-100 hover:bg-slate-205/85 dark:bg-white/10 dark:hover:bg-white/15 text-slate-800 dark:text-slate-200 text-xs font-bold rounded-xl cursor-pointer shadow-3xs transition-all border border-slate-200/65 dark:border-white/5 hover:scale-[1.02]"
            title="Export visualized dataset as CSV"
          >
            <Download className="h-3.5 w-3.5 text-indigo-505" />
            <span>Export CSV</span>
          </button>
        </div>
      </div>

      {/* Grid Dashboard */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-5 mb-6 relative z-10">
        <div id="metric-widget-one" className="p-4 glass-card hover:bg-white/40 dark:hover:bg-slate-900/30 hover:border-slate-300/40 dark:hover:border-white/10 rounded-xl transition-all shadow-3xs cursor-pointer">
          <div className="text-xs text-slate-500 dark:text-indigo-300 font-medium tracking-wide border-b border-slate-150 dark:border-white/5 pb-2 mb-2 flex justify-between items-center">
            <span>TOTAL FILES INDEXED</span>
            <Activity className="h-3.5 w-3.5 text-indigo-500" />
          </div>
          <p className="text-3xl font-extrabold text-slate-800 dark:text-slate-100 font-mono">{documents.length}</p>
          <div className="text-[10px] text-slate-400 dark:text-slate-500 mt-1">Ready for real-time AI contextual chat queries</div>
        </div>

        <div id="metric-widget-two" className="p-4 glass-card hover:bg-white/40 dark:hover:bg-slate-900/30 hover:border-slate-300/40 dark:hover:border-white/10 rounded-xl transition-all shadow-3xs cursor-pointer">
          <div className="text-xs text-slate-500 dark:text-emerald-300 font-medium tracking-wide border-b border-slate-150 dark:border-white/5 pb-2 mb-2 flex justify-between items-center">
            <span>AGGREGATE WORD COUNT</span>
            <Tag className="h-3.5 w-3.5 text-emerald-500" />
          </div>
          <p className="text-3xl font-extrabold text-slate-800 dark:text-slate-100 font-mono">
            {totalWords.toLocaleString()}
          </p>
          <div className="text-[10px] text-slate-400 dark:text-slate-500 mt-1">Average of {avgWords} words per document</div>
        </div>

        <div id="metric-widget-three" className="p-4 glass-card hover:bg-white/40 dark:hover:bg-slate-900/30 hover:border-slate-300/40 dark:hover:border-white/10 rounded-xl transition-all shadow-3xs cursor-pointer">
          <div className="text-xs text-slate-500 dark:text-sky-300 font-medium tracking-wide border-b border-slate-150 dark:border-white/5 pb-2 mb-2 flex justify-between items-center">
            <span>CHARACTER DENSITY</span>
            <HelpCircle className="h-3.5 w-3.5 text-sky-500" />
          </div>
          <p className="text-3xl font-extrabold text-slate-800 dark:text-slate-100 font-mono">
            {totalChars.toLocaleString()}
          </p>
          <div className="text-[10px] text-slate-400 dark:text-slate-500 mt-1">Weighted across all categorized archives</div>
        </div>
      </div>

      {/* Main Chart Stage */}
      <div className="glass-card border border-white/5 bg-white/5 dark:bg-slate-950/15 rounded-2xl p-5 mt-4 relative z-10 transition-all">
        {chartType === 'wordCount' && (
          <div>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">Word Count Waveforms</h3>
              <span className="text-[10px] font-mono bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 px-2 py-0.5 rounded">
                Max Density: {maxWordCount} words
              </span>
            </div>
            {renderWordCountChart()}
          </div>
        )}

        {chartType === 'category' && (
          <div>
            <div className="flex justify-between items-center mb-4">
              <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300">Category Distributions</h3>
              <b className="text-[10px] font-mono bg-indigo-50 dark:bg-indigo-950/60 text-indigo-600 dark:text-indigo-400 px-2 py-0.5 rounded">
                {categoryData.length} category sectors
              </b>
            </div>
            {renderCategoryChart()}
          </div>
        )}

        {chartType === 'sentiment' && (
          <div>
            <h3 className="text-sm font-semibold text-slate-700 dark:text-slate-300 mb-4">AI Sentiment distribution</h3>
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-6 items-center">
              {/* Custom SVG Sentiment ring chart */}
              <div className="flex justify-center py-4">
                <svg width="180" height="180" viewBox="0 0 100 100" className="overflow-visible">
                  <circle cx="50" cy="50" r="38" className="stroke-slate-100 dark:stroke-slate-800 fill-none" strokeWidth="8" />
                  {sentimentData.map((d, index) => {
                    const totalVal = documents.length;
                    const prevSum = sentimentData.slice(0, index).reduce((sum, item) => sum + item.value, 0);
                    const strokeDash = (d.value / totalVal) * 238.76;
                    const strokeOffset = 238.76 - ((prevSum / totalVal) * 238.76);
                    const strokeClass = d.label === 'Positive' ? 'stroke-emerald-500' :
                                        d.label === 'Analytical' ? 'stroke-indigo-500' :
                                        d.label === 'Informative' ? 'stroke-sky-500' :
                                        d.label === 'Critical' ? 'stroke-rose-500' : 'stroke-amber-400';
                    return (
                      <circle
                        key={index}
                        cx="50"
                        cy="50"
                        r="38"
                        className={`${strokeClass} fill-none`}
                        strokeWidth="9.5"
                        strokeDasharray="238.76"
                        strokeDashoffset={strokeOffset}
                        strokeLinecap="round"
                        transform="rotate(-90 50 50)"
                        style={{
                          transition: 'stroke-dashoffset 0.6s ease-in-out'
                        }}
                      />
                    );
                  })}
                  <text x="50" y="53" textAnchor="middle" className="fill-slate-800 dark:fill-slate-100 font-sans font-bold text-xs">
                    {documents.length} Docs
                  </text>
                </svg>
              </div>

              {/* Legend checklist */}
              <div className="flex flex-col gap-3">
                {sentimentData.map((d, idx) => (
                  <div key={idx} className="flex justify-between items-center text-xs pb-1.5 border-b border-slate-100 dark:border-slate-900 last:border-none">
                    <div className="flex items-center gap-2">
                      <span className={`h-3 w-3 rounded-full ${d.color}`} />
                      <span className="font-semibold text-slate-700 dark:text-slate-300">{d.label}</span>
                    </div>
                    <div className="font-mono text-slate-500 dark:text-slate-400 font-medium">
                      {d.value} file(s) ({Math.round((d.value / documents.length) * 100)}%)
                    </div>
                  </div>
                ))}
              </div>
            </div>
          </div>
        )}
      </div>
    </div>
  );
}
