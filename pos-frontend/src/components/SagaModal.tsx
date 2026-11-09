import React from 'react';
import { OrderResponse } from '../types';
import { CheckCircle2, XCircle, Clock, X } from 'lucide-react';

interface SagaModalProps {
  order: OrderResponse | null;
  onClose: () => void;
}

export const SagaModal: React.FC<SagaModalProps> = ({ order, onClose }) => {
  if (!order) return null;

  const formatIDR = (val: number) => {
    return new Intl.NumberFormat('id-ID', {
      style: 'currency',
      currency: 'IDR',
      maximumFractionDigits: 0,
    }).format(val);
  };

  return (
    <div className="fixed inset-0 bg-slate-950/80 backdrop-blur-sm z-50 flex items-center justify-center p-4">
      <div className="bg-slate-900 border border-slate-800 rounded-2xl max-w-lg w-full p-6 shadow-2xl relative">
        <button
          onClick={onClose}
          className="absolute top-4 right-4 text-slate-400 hover:text-white p-1 rounded-lg"
        >
          <X className="w-5 h-5" />
        </button>

        <div className="text-center mb-6">
          {order.status === 'SUCCESS' ? (
            <div className="w-14 h-14 bg-emerald-500/10 border border-emerald-500/30 text-emerald-400 rounded-2xl flex items-center justify-center mx-auto mb-3 shadow-lg shadow-emerald-500/10">
              <CheckCircle2 className="w-8 h-8" />
            </div>
          ) : order.status === 'FAILED' ? (
            <div className="w-14 h-14 bg-red-500/10 border border-red-500/30 text-red-400 rounded-2xl flex items-center justify-center mx-auto mb-3">
              <XCircle className="w-8 h-8" />
            </div>
          ) : (
            <div className="w-14 h-14 bg-blue-500/10 border border-blue-500/30 text-blue-400 rounded-2xl flex items-center justify-center mx-auto mb-3 animate-spin">
              <Clock className="w-8 h-8" />
            </div>
          )}

          <h2 className="text-xl font-bold text-white">
            {order.status === 'SUCCESS'
              ? 'Saga Transaction Completed'
              : order.status === 'FAILED'
              ? 'Order Failed (Stock Compensation)'
              : 'Saga Choreography in Progress'}
          </h2>
          <p className="text-xs text-slate-400 mt-1">Order #{order.orderNumber}</p>
        </div>

        {/* Saga Steps */}
        <div className="bg-slate-950/70 border border-slate-800/80 rounded-xl p-4 mb-4 text-xs space-y-2">
          <div className="flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold text-[10px]">
              1
            </span>
            <span className="text-slate-300">Order Service: Created PENDING in pos_order</span>
          </div>
          <div className="flex items-center gap-2">
            <span className="w-5 h-5 rounded-full bg-blue-500/20 text-blue-400 flex items-center justify-center font-bold text-[10px]">
              2
            </span>
            <span className="text-slate-300">Kafka Broker: Produced to order-created-topic</span>
          </div>
          <div className="flex items-center gap-2">
            <span
              className={`w-5 h-5 rounded-full flex items-center justify-center font-bold text-[10px] ${
                order.status === 'SUCCESS'
                  ? 'bg-emerald-500/20 text-emerald-400'
                  : order.status === 'FAILED'
                  ? 'bg-red-500/20 text-red-400'
                  : 'bg-slate-700 text-slate-400'
              }`}
            >
              3
            </span>
            <span className="text-slate-300">
              {order.status === 'SUCCESS'
                ? 'Inventory Service: Stock deducted & emitted inventory-reserved-topic'
                : order.status === 'FAILED'
                ? `Inventory Service: ${order.failureReason || 'Stock Insufficient'}`
                : 'Inventory Service: Reserving stock...'}
            </span>
          </div>
          <div className="flex items-center gap-2">
            <span
              className={`w-5 h-5 rounded-full flex items-center justify-center font-bold text-[10px] ${
                order.status === 'SUCCESS'
                  ? 'bg-emerald-500/20 text-emerald-400'
                  : order.status === 'FAILED'
                  ? 'bg-red-500/20 text-red-400'
                  : 'bg-slate-700 text-slate-400'
              }`}
            >
              4
            </span>
            <span className="font-semibold text-white">Final Order Status: {order.status}</span>
          </div>
        </div>

        <div className="border-t border-slate-800 pt-3 flex justify-between text-xs text-slate-300">
          <span>Cashier: {order.cashierId}</span>
          <span className="font-bold text-emerald-400 text-sm">{formatIDR(order.finalAmount)}</span>
        </div>

        <button
          onClick={onClose}
          className="mt-6 w-full py-2.5 bg-blue-600 hover:bg-blue-500 text-white font-medium rounded-xl text-sm transition"
        >
          Done & Return to POS
        </button>
      </div>
    </div>
  );
};
