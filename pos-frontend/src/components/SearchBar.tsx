import React from 'react';
import { Search, RefreshCw } from 'lucide-react';

interface SearchBarProps {
  search: string;
  setSearch: (s: string) => void;
  categories: string[];
  selectedCat: string;
  setSelectedCat: (c: string) => void;
  onRefresh: () => void;
  loading: boolean;
}

export const SearchBar: React.FC<SearchBarProps> = ({
  search,
  setSearch,
  categories,
  selectedCat,
  setSelectedCat,
  onRefresh,
  loading,
}) => {
  return (
    <div className="flex flex-col md:flex-row gap-4 justify-between items-start md:items-center mb-6">
      <div className="relative w-full md:w-80">
        <Search className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
        <input
          type="text"
          value={search}
          onChange={(e) => setSearch(e.target.value)}
          placeholder="Search by SKU or Name..."
          className="w-full bg-slate-900 border border-slate-800 rounded-xl py-2 pl-10 pr-4 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
      </div>
      <div className="flex items-center gap-2 overflow-x-auto w-full md:w-auto pb-1">
        {categories.map((cat) => (
          <button
            key={cat}
            onClick={() => setSelectedCat(cat)}
            className={`text-xs px-3.5 py-1.5 rounded-full font-medium transition shrink-0 ${
              selectedCat === cat
                ? 'bg-blue-600 text-white shadow-md'
                : 'bg-slate-900 text-slate-400 hover:text-white border border-slate-800'
            }`}
          >
            {cat}
          </button>
        ))}
        <button
          onClick={onRefresh}
          className="p-2 bg-slate-900 hover:bg-slate-800 text-slate-400 rounded-full border border-slate-800 transition shrink-0"
        >
          <RefreshCw className={`w-3.5 h-3.5 ${loading ? 'animate-spin' : ''}`} />
        </button>
      </div>
    </div>
  );
};
