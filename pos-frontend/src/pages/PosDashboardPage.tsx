import React, { useState, useEffect, useCallback } from 'react';
import { useAuth } from '../context/AuthContext';
import { apiClient } from '../api/client';
import { Product, ApiResponse, StockItem } from '../types';
import { SagaModal } from '../components/SagaModal';
import { CartSidebar } from '../components/CartSidebar';
import { ProductGrid } from '../components/ProductGrid';
import { PosHeader } from '../components/PosHeader';
import { SearchBar } from '../components/SearchBar';
import { StockManagerModal } from '../components/StockManagerModal';
import { AddProductModal } from '../components/AddProductModal';
import { usePosCart } from '../hooks/usePosCart';

export const PosDashboardPage: React.FC = () => {
  const { user, logout } = useAuth();
  const [products, setProducts] = useState<Product[]>([]);
  const [stockMap, setStockMap] = useState<Record<string, number> | null>(null);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState<string | null>(null);
  const [search, setSearch] = useState('');
  const [selectedCat, setSelectedCat] = useState('All');

  const [stockModalOpen, setStockModalOpen] = useState(false);
  const [addProductModalOpen, setAddProductModalOpen] = useState(false);

  const fetchStockMap = useCallback(async () => {
    try {
      const res = await apiClient.get<ApiResponse<StockItem[]>>('/api/inventory/stocks?branchCode=MAIN');
      const map: Record<string, number> = {};
      (res.data.data || []).forEach((s) => {
        map[s.sku] = s.quantity;
      });
      setStockMap(map);
    } catch {
      setStockMap(null);
    }
  }, []);

  const fetchProducts = useCallback(async () => {
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
  }, []);

  const refreshAll = useCallback(() => {
    fetchProducts();
    fetchStockMap();
  }, [fetchProducts, fetchStockMap]);

  useEffect(() => {
    refreshAll();
  }, [refreshAll]);

  const cartState = usePosCart(stockMap, fetchStockMap);

  const categories = ['All', ...Array.from(new Set(products.map((p) => p.category)))];

  const filtered = products.filter((p) => {
    const matchSearch =
      p.name.toLowerCase().includes(search.toLowerCase()) ||
      p.sku.toLowerCase().includes(search.toLowerCase());
    const matchCat = selectedCat === 'All' || p.category === selectedCat;
    return matchSearch && matchCat;
  });

  return (
    <div className="min-h-screen bg-slate-950 text-slate-100 flex flex-col">
      <PosHeader
        user={user}
        onLogout={logout}
        onOpenStockManager={() => setStockModalOpen(true)}
        onOpenAddProduct={() => setAddProductModalOpen(true)}
      />
      <div className="flex-1 flex overflow-hidden">
        <main className="flex-1 p-6 overflow-y-auto">
          <SearchBar
            search={search}
            setSearch={setSearch}
            categories={categories}
            selectedCat={selectedCat}
            setSelectedCat={setSelectedCat}
            onRefresh={refreshAll}
            loading={loading}
          />

          {error && (
            <div className="mb-6 p-4 bg-red-950/40 border border-red-800/60 rounded-xl text-red-200 text-sm flex items-center justify-between">
              <span>{error}</span>
              <button onClick={refreshAll} className="underline text-xs font-semibold ml-4">Retry</button>
            </div>
          )}

          <ProductGrid
            products={filtered}
            loading={loading}
            cart={cartState.cart}
            onAddToCart={cartState.addToCart}
            stockMap={stockMap}
          />
        </main>

        <CartSidebar
          cart={cartState.cart}
          discount={cartState.discount}
          setDiscount={cartState.setDiscount}
          updateQuantity={cartState.updateQuantity}
          removeFromCart={cartState.removeFromCart}
          clearCart={cartState.clearCart}
          onCheckout={cartState.handleCheckout}
          isCheckingOut={cartState.isCheckingOut}
          checkoutError={cartState.checkoutError}
        />
      </div>

      <SagaModal
        order={cartState.completedOrder}
        onClose={() => {
          cartState.setCompletedOrder(null);
          refreshAll();
        }}
      />

      <StockManagerModal
        open={stockModalOpen}
        onClose={() => setStockModalOpen(false)}
        products={products}
        stockMap={stockMap}
        onUpdated={fetchStockMap}
      />

      <AddProductModal
        open={addProductModalOpen}
        onClose={() => setAddProductModalOpen(false)}
        onCreated={refreshAll}
      />
    </div>
  );
};

