import React, { useState } from 'react';
import { apiClient } from '../api/client';
import { PackagePlus, X, AlertCircle } from 'lucide-react';

interface AddProductModalProps {
  open: boolean;
  onClose: () => void;
  onCreated: () => void;
}

const emptyForm = { sku: '', name: '', description: '', category: '', price: '', initialStock: '' };
type FormField = keyof typeof emptyForm;

const inputClass =
  'w-full bg-slate-950 border border-slate-800 rounded-xl py-2 px-3 text-sm text-white placeholder-slate-500 focus:outline-none focus:ring-2 focus:ring-blue-500';

export const AddProductModal: React.FC<AddProductModalProps> = ({ open, onClose, onCreated }) => {
  const [form, setForm] = useState(emptyForm);
  const [saving, setSaving] = useState(false);
  const [error, setError] = useState<string | null>(null);

  if (!open) return null;

  const update = (field: FormField) => (e: React.ChangeEvent<HTMLInputElement | HTMLTextAreaElement>) =>
    setForm((prev) => ({ ...prev, [field]: e.target.value }));

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    const sku = form.sku.trim();
    const price = Number(form.price);
    const initialStock = form.initialStock === '' ? 0 : Number(form.initialStock);

    if (!sku || !form.name.trim() || !form.category.trim()) {
      setError('SKU, name and category are required');
      return;
    }
    if (form.price === '' || Number.isNaN(price) || price < 0) {
      setError('Price must be a non-negative number');
      return;
    }
    if (!Number.isInteger(initialStock) || initialStock < 0) {
      setError('Initial stock must be a non-negative whole number');
      return;
    }

    setSaving(true);
    setError(null);
    try {
      await apiClient.post('/api/catalog/products', {
        sku,
        name: form.name.trim(),
        description: form.description.trim(),
        category: form.category.trim(),
        price,
      });
    } catch (err: any) {
      setError(err.response?.data?.message || err.message || 'Failed to create product');
      setSaving(false);
      return;
    }

    try {
      if (initialStock > 0) {
        await apiClient.post('/api/inventory/stocks', { sku, branchCode: 'MAIN', quantity: initialStock });
      }
      setForm(emptyForm);
      onCreated();
      onClose();
    } catch (err: any) {
      setError(
        `Product created, but initial stock failed: ${err.response?.data?.message || err.message}. Use Manage Stock to retry.`
      );
      onCreated();
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <form onSubmit={handleSubmit} className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <PackagePlus className="w-5 h-5 text-emerald-400" />
            <h2 className="font-semibold text-white">Add New Product</h2>
          </div>
          <button type="button" onClick={onClose} className="text-slate-400 hover:text-white p-1 rounded-lg">
            <X className="w-5 h-5" />
          </button>
        </div>

        {error && (
          <div className="p-2.5 bg-red-950/60 border border-red-800 text-red-200 text-xs rounded-xl flex items-center gap-2">
            <AlertCircle className="w-4 h-4 shrink-0" /> {error}
          </div>
        )}

        <div className="grid grid-cols-2 gap-3">
          <input value={form.sku} onChange={update('sku')} placeholder="SKU (e.g. LAP-002)" className={inputClass} />
          <input value={form.category} onChange={update('category')} placeholder="Category" className={inputClass} />
        </div>
        <input value={form.name} onChange={update('name')} placeholder="Product name" className={inputClass} />
        <textarea value={form.description} onChange={update('description')} placeholder="Description (optional)" rows={2} className={inputClass} />
        <div className="grid grid-cols-2 gap-3">
          <input type="number" min="0" value={form.price} onChange={update('price')} placeholder="Price (IDR)" className={inputClass} />
          <input type="number" min="0" value={form.initialStock} onChange={update('initialStock')} placeholder="Initial stock" className={inputClass} />
        </div>

        <button
          type="submit"
          disabled={saving}
          className="w-full py-2.5 bg-emerald-600 hover:bg-emerald-500 text-white font-medium rounded-xl text-sm transition disabled:opacity-50"
        >
          {saving ? 'Saving...' : 'Create Product'}
        </button>
      </form>
    </div>
  );
};
