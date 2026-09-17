import React from 'react';
import { useAuth } from '../hooks/useAuth';
import { LogOut, User as UserIcon, Shield, Laptop, LifeBuoy } from 'lucide-react';

export const Navbar: React.FC = () => {
  const { user, logout } = useAuth();

  const getRoleBadge = (role?: string) => {
    switch (role) {
      case 'ADMIN':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-purple-100 text-purple-800 border border-purple-200">
            ADMIN
          </span>
        );
      case 'SUPPORT':
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-bold bg-indigo-100 text-indigo-800 border border-indigo-200">
            SUPPORT TIER 2
          </span>
        );
      default:
        return (
          <span className="px-2 py-0.5 rounded text-[11px] font-medium bg-slate-100 text-slate-700 border border-slate-200">
            EMPLOYEE
          </span>
        );
    }
  };

  return (
    <header className="h-16 bg-white border-b border-slate-200 flex items-center justify-between px-6 sticky top-0 z-30 shadow-xs">
      <div className="flex items-center gap-3">
        <div className="w-9 h-9 rounded-lg bg-indigo-600 flex items-center justify-center text-white font-bold shadow-xs">
          <LifeBuoy className="w-5 h-5" />
        </div>
        <div>
          <div className="flex items-center gap-2">
            <span className="font-bold text-slate-900 tracking-tight text-base">
              IT Helpdesk & End-User Support
            </span>
            <span className="text-[10px] uppercase font-semibold tracking-wider px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded">
              ITIL-Inspired
            </span>
          </div>
          <p className="text-[11px] text-slate-500 hidden sm:block">
            Internal Enterprise IT Service Management Portal
          </p>
        </div>
      </div>

      <div className="flex items-center gap-4">
        {user && (
          <div className="flex items-center gap-3 pr-2 border-r border-slate-200">
            <div className="text-right hidden md:block">
              <div className="flex items-center justify-end gap-1.5">
                <span className="text-xs font-semibold text-slate-900">{user.name}</span>
                {getRoleBadge(user.role)}
              </div>
              <p className="text-[11px] text-slate-500">
                {user.job_title || 'Staff'} • {user.department}
              </p>
            </div>
            <div className="w-8 h-8 rounded-full bg-slate-100 border border-slate-200 flex items-center justify-center text-slate-600 font-semibold text-xs">
              {user.name.charAt(0)}
            </div>
          </div>
        )}

        <button
          onClick={logout}
          title="Sign out of Helpdesk"
          className="inline-flex items-center gap-1.5 px-3 py-1.5 text-xs font-medium text-slate-600 hover:text-red-600 hover:bg-red-50 rounded-md transition"
        >
          <LogOut className="w-4 h-4" />
          <span className="hidden sm:inline">Sign Out</span>
        </button>
      </div>
    </header>
  );
};
