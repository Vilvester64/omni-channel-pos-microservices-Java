export interface ApiResponse<T> {
  code: number;
  status: string;
  message: string;
  data: T;
  timestamp?: string;
}

export interface User {
  userId: string;
  username: string;
  roles: string;
  token: string;
}

export interface Product {
  id: number;
  sku: string;
  name: string;
  description: string;
  price: number;
  category: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface CartItem {
  product: Product;
  quantity: number;
}

export interface OrderItemRequest {
  sku: string;
  productName: string;
  quantity: number;
  price: number;
}

export interface OrderRequest {
  discountAmount: number;
  items: OrderItemRequest[];
}

export interface OrderItemResponse {
  id: number;
  sku: string;
  productName: string;
  quantity: number;
  price: number;
  subtotal: number;
}

export interface OrderResponse {
  id: number;
  orderNumber: string;
  cashierId: string;
  totalAmount: number;
  discountAmount: number;
  finalAmount: number;
  status: 'PENDING' | 'SUCCESS' | 'FAILED';
  failureReason: string | null;
  createdAt: string;
  items: OrderItemResponse[];
}
