import React from 'react';
import { CartItem } from '../types';
import { ShoppingBag, CreditCard, Plus, Minus, Trash2, Tag, Sparkles } from 'lucide-react';

interface CartSidebarProps {
  cart: CartItem[];
  discount: number;
  setDiscount: (d: number) => void;
  updateQuantity: (id: number, delta: number) => void;
  removeFromCart: (id: number) => void;
  clearCart: () => void;
  onCheckout: () => void;
  isCheckingOut: boolean;
  checkoutError: string | null;
}

export const CartSidebar: React.FC<CartSidebarProps> = ({
  cart,
  discount,
  setDiscount,
  updateQuantity,
  removeFromCart,
  clearCart,
  onCheckout,
  isCheckingOut,
  checkoutError,
}) => {
  const formatIDR = (v: number) =>
    new Intl.NumberFormat('id-ID', { style: 'currency', currency: 'IDR', maximumFractionDigits: 0 }).format(v);

  const subtotal = cart.reduce((acc, i) => acc + i.product.price * i.quantity, 0);
  const finalTotal = Math.max(0, subtotal - discount);

  return (
    <aside className="w-96 border-l border-slate-800 bg-slate-900/70 flex flex-col h-[calc(100vh-4rem)]">
      <div className="p-4 border-b border-slate-800 flex items-center justify-between">
        <div className="flex items-center gap-2">
          <CreditCard className="w-5 h-5 text-blue-400" />
          <h2 className="font-semibold text-sm text-white">Current Order</h2>
          <span className="text-xs bg-slate-800 text-slate-300 font-bold px-2 py-0.5 rounded-full">
            {cart.reduce((a, b) => a + b.quantity, 0)}
          </span>
        </div>
        {cart.length > 0 && (
          <button onClick={clearCart} className="text-[11px] text-slate-400 hover:text-red-400">Clear</button>
        )}
      </div>

      <div className="flex-1 overflow-y-auto p-4 space-y-3">
        {cart.length === 0 ? (
          <div className="h-full flex flex-col items-center justify-center text-center text-slate-500 p-6">
            <ShoppingBag className="w-12 h-12 text-slate-700 mb-3" />
            <p className="text-sm font-medium text-slate-400">Cart is empty</p>
          </div>
        ) : (
          cart.map((item) => (
            <div key={item.product.id} className="bg-slate-950/80 border border-slate-800/80 rounded-xl p-3 flex flex-col gap-2">
              <div className="flex items-start justify-between gap-2">
                <div className="flex-1">
                  <p className="text-xs font-semibold text-white line-clamp-1">{item.product.name}</p>
                  <p className="text-[10px] text-slate-500 font-mono">{item.product.sku}</p>
                </div>
                <button onClick={() => removeFromCart(item.product.id)} className="text-slate-500 hover:text-red-400">
                  <Trash2 className="w-3.5 h-3.5" />
                </button>
              </div>
              <div className="flex items-center justify-between pt-1 border-t border-slate-900">
                <span className="text-xs font-semibold text-emerald-400">{formatIDR(item.product.price * item.quantity)}</span>
                <div className="flex items-center gap-1 bg-slate-900 rounded-lg p-0.5 border border-slate-800">
                  <button onClick={() => updateQuantity(item.product.id, -1)} className="p-1 text-slate-400 hover:text-white">
                    <Minus className="w-3 h-3" />
                  </button>
                  <span className="text-xs font-bold w-6 text-center text-white">{item.quantity}</span>
                  <button onClick={() => updateQuantity(item.product.id, 1)} className="p-1 text-slate-400 hover:text-white">
                    <Plus className="w-3 h-3" />
                  </button>
                </div>
              </div>
            </div>
          ))
        )}
      </div>

      {cart.length > 0 && (
        <div className="p-4 border-t border-slate-800 bg-slate-950/90 space-y-3">
          <div className="flex items-center justify-between gap-2 bg-slate-900 border border-slate-800 rounded-xl px-3 py-1.5">
            <span className="text-xs text-slate-400 flex items-center gap-1"><Tag className="w-3.5 h-3.5 text-blue-400" /> Discount</span>
            <input
              type="number"
              min="0"
              value={discount === 0 ? '' : discount}
              onChange={(e) => setDiscount(Math.max(0, Number(e.target.value) || 0))}
              placeholder="0"
              className="bg-transparent text-right text-xs font-semibold text-white focus:outline-none w-28"
            />
          </div>
          <div className="space-y-1 text-xs">
            <div className="flex justify-between text-slate-400"><span>Subtotal</span><span>{formatIDR(subtotal)}</span></div>
            {discount > 0 && <div className="flex justify-between text-red-400"><span>Discount</span><span>-{formatIDR(discount)}</span></div>}
            <div className="flex justify-between text-base font-bold text-white pt-2 border-t border-slate-800">
              <span>Total</span><span className="text-emerald-400">{formatIDR(finalTotal)}</span>
            </div>
          </div>
          {checkoutError && <div className="p-2 bg-red-950/60 border border-red-800 text-red-200 text-xs rounded-xl">{checkoutError}</div>}
          <button
            onClick={onCheckout}
            disabled={isCheckingOut}
            className="w-full bg-gradient-to-r from-emerald-600 to-emerald-500 hover:from-emerald-500 text-white font-semibold py-3 px-4 rounded-xl shadow-lg flex items-center justify-center gap-2 transition disabled:opacity-50"
          >
            {isCheckingOut ? (
              <span className="flex items-center gap-2">
                <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                Executing Saga...
              </span>
            ) : (
              <><Sparkles className="w-4 h-4 text-emerald-200" /> Pay {formatIDR(finalTotal)}</>
            )}
          </button>
        </div>
      )}
    </aside>
  );
};
