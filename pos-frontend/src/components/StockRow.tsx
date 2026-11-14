import React from 'react';
import { Product } from '../types';
import { Plus } from 'lucide-react';

interface StockRowProps {
  product: Product;
  hasRecord: boolean;
  quantity: number;
  inputValue: string;
  onInputChange: (value: string) => void;
  onAdd: () => void;
  busy: boolean;
  disabled: boolean;
}

export const StockRow: React.FC<StockRowProps> = ({
  product,
  hasRecord,
  quantity,
  inputValue,
  onInputChange,
  onAdd,
  busy,
  disabled,
}) => (
  <tr className="border-t border-slate-800">
    <td className="py-2.5 px-4">
      <p className="text-xs font-semibold text-white">{product.name}</p>
      <p className="text-[10px] font-mono text-slate-500">{product.sku}</p>
    </td>
    <td className="py-2.5 px-4 text-center">
      <span
        className={`text-xs font-bold px-2 py-0.5 rounded-full ${
          quantity > 0 ? 'bg-emerald-500/10 text-emerald-400' : 'bg-red-500/10 text-red-400'
        }`}
      >
        {hasRecord ? quantity : 'Not registered'}
      </span>
    </td>
    <td className="py-2.5 px-4">
      <div className="flex items-center justify-end gap-2">
        <input
          type="number"
          min="1"
          value={inputValue}
          onChange={(e) => onInputChange(e.target.value)}
          placeholder="Qty"
          className="w-20 bg-slate-950 border border-slate-800 rounded-lg py-1.5 px-2 text-xs text-white text-right focus:outline-none focus:ring-2 focus:ring-blue-500"
        />
        <button
          onClick={onAdd}
          disabled={busy || disabled}
          className="py-1.5 px-3 bg-blue-600 hover:bg-blue-500 text-white text-xs font-medium rounded-lg flex items-center gap-1 transition disabled:opacity-50"
        >
          <Plus className="w-3.5 h-3.5" /> {busy ? 'Saving...' : 'Add'}
        </button>
      </div>
    </td>
  </tr>
);
