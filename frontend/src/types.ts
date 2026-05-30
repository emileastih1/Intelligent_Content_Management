export interface Document {
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
}

export interface Message {
  id: string;
  role: 'user' | 'model';
  content: string;
  timestamp: string;
  references?: string[]; // IDs of referenced documents
}

export interface ChatSession {
  id: string;
  title: string;
  messages: Message[];
  documentIds?: string[]; // Filter to specific docs
  createdAt: string;
}

export interface ChartDataPoint {
  label: string;
  value: number;
  secondaryValue?: number;
}
