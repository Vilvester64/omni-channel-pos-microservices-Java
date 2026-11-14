import { useState } from 'react';
import { apiClient } from '../api/client';
import { CartItem, Product, ApiResponse, OrderResponse } from '../types';

export const usePosCart = (
  stockMap: Record<string, number> | null,
  onOrderSettled: () => void
) => {
  const [cart, setCart] = useState<CartItem[]>([]);
  const [discount, setDiscount] = useState(0);
  const [isCheckingOut, setIsCheckingOut] = useState(false);
  const [completedOrder, setCompletedOrder] = useState<OrderResponse | null>(null);
  const [checkoutError, setCheckoutError] = useState<string | null>(null);

  const addToCart = (product: Product) => {
    const available = stockMap?.[product.sku];
    setCart((prev) => {
      const existing = prev.find((item) => item.product.id === product.id);
      const currentQty = existing ? existing.quantity : 0;
      if (available !== undefined && currentQty + 1 > available) return prev;
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
        .map((i) => {
          if (i.product.id !== id) return i;
          const target = i.quantity + delta;
          if (target <= 0) return null;
          const available = stockMap?.[i.product.sku];
          if (available !== undefined && target > available) return i;
          return { ...i, quantity: target };
        })
        .filter((i): i is CartItem => i !== null)
    );
  };

  const removeFromCart = (id: number) => setCart((prev) => prev.filter((i) => i.product.id !== id));
  const clearCart = () => {
    setCart([]);
    setDiscount(0);
  };

  const handleCheckout = async () => {
    if (cart.length === 0) return;
    setIsCheckingOut(true);
    setCheckoutError(null);
    setCompletedOrder(null);

    const payload = {
      discountAmount: Number(discount) || 0,
      items: cart.map((i) => ({
        sku: i.product.sku,
        productName: i.product.name,
        quantity: i.quantity,
        price: i.product.price,
      })),
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
          if (poll.data.data.status !== 'PENDING' || count > 8) {
            clearInterval(interval);
            onOrderSettled();
          }
        } catch {
          clearInterval(interval);
        }
      }, 1000);

      clearCart();
    } catch (err: any) {
      setCheckoutError(err.response?.data?.message || 'Checkout failed. Verify inventory stock.');
    } finally {
      setIsCheckingOut(false);
    }
  };

  return {
    cart,
    discount,
    setDiscount,
    addToCart,
    updateQuantity,
    removeFromCart,
    clearCart,
    handleCheckout,
    isCheckingOut,
    completedOrder,
    setCompletedOrder,
    checkoutError,
  };
};
