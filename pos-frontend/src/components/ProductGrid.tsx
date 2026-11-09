import React from 'react';
import { Product, CartItem } from '../types';
import { Plus, Layers } from 'lucide-react';

interface ProductGridProps {
  products: Product[];
  loading: boolean;
  cart: CartItem[];
  onAddToCart: (p: Product) => void;
}

export const ProductGrid: React.FC<ProductGridProps> = ({ products, loading, cart, onAddToCart }) => {
  const formatIDR = (val: number) =>
    new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(val);

  if (loading) {
    return (
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
        {[...Array(8)].map((_, i) => (
          <div key={i} className="bg-slate-900/60 border border-slate-800 rounded-2xl p-4 animate-pulse h-48" />
        ))}
      </div>
    );
  }

  if (products.length === 0) {
    return (
      <div className="text-center py-20 border border-dashed border-slate-800 rounded-2xl">
        <Layers className="w-12 h-12 text-slate-600 mx-auto mb-3" />
        <p className="text-slate-400 text-sm font-medium">No products found</p>
        <p className="text-slate-600 text-xs mt-1">Check search query or verify Catalog Service</p>
      </div>
    );
  }

  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-4">
      {products.map((p) => {
        const inCart = cart.find((item) => item.product.id === p.id);
        return (
          <div
            key={p.id}
            className="bg-slate-900/80 hover:bg-slate-900 border border-slate-800 hover:border-slate-700/80 rounded-2xl p-4 flex flex-col justify-between transition group shadow-sm hover:shadow-lg"
          >
            <div>
              <div className="flex items-center justify-between gap-2 mb-2">
                <span className="text-[10px] font-mono font-medium px-2 py-0.5 bg-slate-800 text-slate-300 rounded border border-slate-700/50">
                  {p.sku}
                </span>
                <span className="text-[10px] text-blue-400 bg-blue-500/10 px-2 py-0.5 rounded-full font-medium">
                  {p.category}
                </span>
              </div>
              <h3 className="font-semibold text-white text-sm line-clamp-1 group-hover:text-blue-400 transition">
                {p.name}
              </h3>
              <p className="text-xs text-slate-400 mt-1 line-clamp-2 min-h-[2rem]">
                {p.description || 'No product description available'}
              </p>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-800/80 flex items-center justify-between">
              <div>
                <p className="text-[10px] text-slate-500 uppercase tracking-wider">Price</p>
                <p className="font-bold text-sm text-emerald-400">{formatIDR(p.price)}</p>
              </div>

              <button
                onClick={() => onAddToCart(p)}
                className={`py-1.5 px-3 rounded-xl text-xs font-medium flex items-center gap-1.5 transition ${
                  inCart
                    ? 'bg-emerald-600/20 text-emerald-300 border border-emerald-500/40 hover:bg-emerald-600/30'
                    : 'bg-blue-600 hover:bg-blue-500 text-white shadow-md shadow-blue-600/20'
                }`}
              >
                <Plus className="w-3.5 h-3.5" />
                {inCart ? `Added (${inCart.quantity})` : 'Add'}
              </button>
            </div>
          </div>
        );
      })}
    </div>
  );
};
