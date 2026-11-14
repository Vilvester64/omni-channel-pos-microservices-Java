import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { Product } from '../types';
import { StockRow } from './StockRow';
import { Boxes, X, AlertCircle, CheckCircle2, Search } from 'lucide-react';

interface StockManagerModalProps {
  open: boolean;
  onClose: () => void;
  products: Product[];
  stockMap: Record<string, number> | null;
  onUpdated: () => void;
}

export const StockManagerModal: React.FC<StockManagerModalProps> = ({ open, onClose, products, stockMap, onUpdated }) => {
  const [inputs, setInputs] = useState<Record<string, string>>({});
  const [busySku, setBusySku] = useState<string | null>(null);
  const [error, setError] = useState<string | null>(null);
  const [success, setSuccess] = useState<string | null>(null);
  const [query, setQuery] = useState('');

  if (!open) return null;

  const q = query.toLowerCase();
  const visible = products.filter((p) => p.name.toLowerCase().includes(q) || p.sku.toLowerCase().includes(q));

  const handleAdd = async (sku: string, hasRecord: boolean) => {
    const qty = Number(inputs[sku]);
    if (!Number.isInteger(qty) || qty < 1) {
      setError('Quantity must be a whole number of at least 1');
      return;
    }
    setBusySku(sku);
    setError(null);
    setSuccess(null);
    try {
      if (hasRecord) {
        await apiClient.post('/api/inventory/stocks/add', {
          sku,
          branchCode: 'MAIN',
          quantity: qty,
          reason: 'Admin restock via POS UI',
        });
      } else {
        await apiClient.post('/api/inventory/stocks', { sku, branchCode: 'MAIN', quantity: qty });
      }
      setSuccess(`Added ${qty} unit(s) to ${sku}`);
      setInputs((prev) => ({ ...prev, [sku]: '' }));
      onUpdated();
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to update stock');
    } finally {
      setBusySku(null);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-3xl w-full max-h-[85vh] flex flex-col shadow-2xl">
        <div className="p-5 border-b border-slate-800 flex items-center justify-between">
          <div className="flex items-center gap-2">
            <Boxes className="w-5 h-5 text-blue-400" />
            <h2 className="font-semibold text-white">Manage Inventory Stock</h2>
            <span className="text-[10px] text-slate-400 bg-slate-800 px-2 py-0.5 rounded-full">Branch: MAIN</span>
          </div>
          <button onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        <div className="p-5 space-y-3 border-b border-slate-800">
          <div className="relative">
            <Search className="w-4 h-4 text-slate-400 absolute left-3 top-1/2 -translate-y-1/2" />
            <input
              value={query}
              onChange={(e) => setQuery(e.target.value)}
              placeholder="Search product by SKU or name..."
              className="w-full bg-slate-950 border border-slate-800 rounded-xl py-2 pl-9 pr-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500"
            />
          </div>
          {stockMap === null && (
            <div className="p-2.5 bg-amber-950/50 border border-amber-800 text-amber-200 text-xs rounded-xl">
              Inventory Service unreachable. Make sure inventory-service (port 8082) is running.
            </div>
          )}
          {error && (
            <div className="p-2.5 bg-red-950/60 border border-red-800 text-red-200 text-xs rounded-xl flex items-center gap-2">
              <AlertCircle className="w-4 h-4 shrink-0" /> {error}
            </div>
          )}
          {success && (
            <div className="p-2.5 bg-emerald-950/50 border border-emerald-800 text-emerald-200 text-xs rounded-xl flex items-center gap-2">
              <CheckCircle2 className="w-4 h-4 shrink-0" /> {success}
            </div>
          )}
        </div>

        <div className="flex-1 overflow-y-auto">
          <table className="w-full text-left">
            <thead className="text-[10px] uppercase tracking-wider text-slate-500 bg-slate-950/60 sticky top-0">
              <tr>
                <th className="py-2 px-4">Product</th>
                <th className="py-2 px-4 text-center">Current Stock</th>
                <th className="py-2 px-4 text-right">Add Quantity</th>
              </tr>
            </thead>
            <tbody>
              {visible.map((p) => {
                const hasRecord = stockMap !== null && p.sku in stockMap;
                return (
                  <StockRow
                    key={p.id}
                    product={p}
                    hasRecord={hasRecord}
                    quantity={hasRecord ? stockMap![p.sku] : 0}
                    inputValue={inputs[p.sku] ?? ''}
                    onInputChange={(v) => setInputs((prev) => ({ ...prev, [p.sku]: v }))}
                    onAdd={() => handleAdd(p.sku, hasRecord)}
                    busy={busySku === p.sku}
                    disabled={stockMap === null}
                  />
                );
              })}
              {visible.length === 0 && (
                <tr>
                  <td colSpan={3} className="py-10 text-center text-xs text-slate-500">No products found</td>
                </tr>
              )}
            </tbody>
          </table>
        </div>
      </div>
    </div>
  );
};
