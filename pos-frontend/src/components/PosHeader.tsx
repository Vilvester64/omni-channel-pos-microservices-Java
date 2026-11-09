import React from 'react';
import { ShoppingBag, LogOut } from 'lucide-react';
import { User } from '../types';

interface PosHeaderProps {
  user: User | null;
  onLogout: () => void;
}

export const PosHeader: React.FC<PosHeaderProps> = ({ user, onLogout }) => {
  return (
    <header className="h-16 border-b border-slate-800 bg-slate-900/60 backdrop-blur-md px-6 flex items-center justify-between sticky top-0 z-30">
      <div className="flex items-center gap-3">
        <div className="w-10 h-10 rounded-xl bg-gradient-to-tr from-blue-600 to-emerald-500 flex items-center justify-center shadow-lg shadow-blue-500/20">
          <ShoppingBag className="w-5 h-5 text-white" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-base text-white tracking-tight">Omni-Channel POS</span>
            <span className="text-[10px] bg-blue-500/20 text-blue-400 font-semibold px-2 py-0.5 rounded-full border border-blue-500/30">
              Saga Architecture
            </span>
          </div>
          <p className="text-[11px] text-slate-400">Microservices Cashier Terminal</p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        <div className="hidden md:flex items-center gap-2 text-xs text-slate-400 bg-slate-950/60 border border-slate-800 py-1.5 px-3 rounded-full">
          <span className="w-2 h-2 rounded-full bg-emerald-400 animate-pulse" />
          <span>Eureka & Kafka Active</span>
        </div>

        <div className="flex items-center gap-3 border-l border-slate-800 pl-4">
          <div className="text-right">
            <p className="text-xs font-semibold text-white">{user?.username}</p>
            <p className="text-[10px] text-emerald-400">{user?.roles}</p>
          </div>
          <button
            onClick={onLogout}
            title="Logout"
            className="p-2 bg-slate-800 hover:bg-red-950/60 hover:text-red-400 text-slate-400 rounded-xl border border-slate-700/60 transition"
          >
            <LogOut className="w-4 h-4" />
          </button>
        </div>
      </div>
    </header>
  );
};
