import React, { useState, useEffect } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../api/client';
import { Product, CartItem, ApiResponse, OrderResponse } from '../types';
import { SagaModal } from '../components/SagaModal';
import { CartSidebar } from '../components/CartSidebar';
import { ProductGrid } from '../components/ProductGrid';
import { PosHeader } from '../components/PosHeader';
import { SearchBar } from '../components/SearchBar';

export const PosDashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [selectedCat, setSelectedCat] = useState('All');
  const [cart, setCart] = useState<CartItem[]>([]);
  const [discount, setDiscount] = useState(0);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [completedOrder, setCompletedOrder] = useState<OrderResponse | null>(null);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);

  const fetchProducts = async () => {
    setLoading(true);
    setError(null);
    try {
      const res = await apiClient.get<ApiResponse<Product[]>>('/api/catalog/products');
      setProducts(res.data.data || []);
    } catch (err: any) {
      setError(err.response?.data?.message || 'Failed to load catalog');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchProducts();
  }, []);

  const categories = ['All', ...Array.from(new Set(products.map((p) => p.category)))];

  const filtered = products.filter((p) => {
    const matchSearch =
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.sku.toLowerCase().includes(search.toLowerCase());
    const matchCat = selectedCat === 'All' || p.category === selectedCat;
    return matchSearch && matchCat;
  });

  const addToCart = (product: Product) => {
    setCart((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      if (existing) {
        return prev.map((item) =>
          item.product.id === product.id ? { ...item, quantity: item.quantity + 1 } : item
        );
      }
      return [...prev, { product, quantity: 1 }];
    });
  };

  const updateQuantity = (id: number, delta: number) => {
    setCart((prev) =>
      prev
        .map((i) => (i.product.id === id ? (i.quantity + delta > 0 ? { ...i, quantity: i.quantity + delta } : null) : i))
        .filter((i): i is CartItem => i !== null)
    );
  };

  const removeFromCart = (id: number) => setCart((prev) => prev.filter((i) => i.product.id !== id));
  const clearCart = () => { setCart([]); setDiscount(0); };

  const handleCheckout = async () => {
    if (cart.length === 0) return;
    setIsCheckingOut(true);
    setCheckoutError(null);
    setCompletedOrder(null);

    const payload = {
      discountAmount: Number(discount) || 0,
      items: cart.map((i) => ({ sku: i.product.sku, productName: i.product.name, quantity: i.quantity, price: i.product.price })),
    };

    try {
      const res = await apiClient.post<ApiResponse<OrderResponse>>('/api/order/orders', payload);
      const created = res.data.data;
      setCompletedOrder(created);

      let count = 0;
      const interval = setInterval(async () => {
        count++;
        try {
          const poll = await apiClient.get<ApiResponse<OrderResponse>>(`/api/order/orders/${created.id}`);
          setCompletedOrder(poll.data.data);
          if (poll.data.data.status !== 'PENDING' || count > 8) clearInterval(interval);
        } catch { clearInterval(interval); }
      }, 1000);

      clearCart();
    } catch (err: any) {
      setCheckoutError(err.response?.data?.message || 'Checkout failed. Verify inventory stock.');
    } finally {
      setIsCheckingOut(false);
    }
  };

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <PosHeader user={user} onLogout={logout} />
      <div className="flex-1 flex overflow-hidden">
        <main className="flex-1 p-6 overflow-y-auto">
          <SearchBar
            search={search}
            setSearch={setSearch}
            categories={categories}
            selectedCat={selectedCat}
            setSelectedCat={setSelectedCat}
            onRefresh={fetchProducts}
            loading={loading}
          />

          {error && (
            <div className="mb-6 p-4 bg-red-950/40 border border-red-800/60 rounded-xl text-red-200 text-sm flex items-center justify-between">
              <span>{error}</span>
              <button onClick={fetchProducts} className="underline text-xs font-semibold ml-4">Retry</button>
            </div>
          )}

          <ProductGrid products={filtered} loading={loading} cart={cart} onAddToCart={addToCart} />
        </main>

        <CartSidebar
          cart={cart}
          discount={discount}
          setDiscount={setDiscount}
          updateQuantity={updateQuantity}
          removeFromCart={removeFromCart}
          clearCart={clearCart}
          onCheckout={handleCheckout}
          isCheckingOut={isCheckingOut}
          checkoutError={checkoutError}
        />
      </div>

      <SagaModal order={completedOrder} onClose={() => { setCompletedOrder(null); fetchProducts(); }} />
    </div>
  );
};
