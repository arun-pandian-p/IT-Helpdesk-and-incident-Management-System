import React from 'react';
import { NavLink } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import {
  LayoutDashboard,
  Ticket,
  PlusCircle,
  Laptop,
  UserCheck,
  Key,
  BookOpen,
  TrendingUp,
  Users,
  ShieldCheck,
  FolderLock,
  Headphones,
} from 'lucide-react';

export const Sidebar: React.FC = () => {
  const { user, isEmployee, isSupport, isAdmin } = useAuth();

  const navLinkClass = ({ isActive }: { isActive: boolean }) =>
    `flex items-center gap-3 px-3 py-2 rounded-md text-xs font-medium transition ${
      isActive
        ? 'bg-indigo-50 text-indigo-700 font-semibold'
        : 'text-slate-600 hover:bg-slate-100 hover:text-slate-900'
    }`;

  return (
    <aside className="w-64 bg-white border-r border-slate-200 flex flex-col justify-between p-4 flex-shrink-0 min-h-[calc(100vh-4rem)]">
      <div className="space-y-6">
        {/* Quick Action */}
        <div className="px-1">
          <NavLink
            to="/tickets/new"
            className="w-full flex items-center justify-center gap-2 px-3 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Report Technical Incident</span>
          </NavLink>
        </div>

        {/* Navigation Sections */}
        <div className="space-y-1">
          <div className="px-3 text-[10px] font-bold tracking-wider text-slate-400 uppercase mb-2">
            Service Desk
          </div>

          <NavLink to="/" end className={navLinkClass}>
            <LayoutDashboard className="w-4 h-4" />
            <span>{isEmployee ? 'Employee Portal' : 'Operational Dashboard'}</span>
          </NavLink>

          <NavLink to="/tickets" end className={navLinkClass}>
            <Ticket className="w-4 h-4" />
            <span>{isEmployee ? 'My Incidents' : 'Incident Queue'}</span>
          </NavLink>

          <NavLink to="/knowledge" className={navLinkClass}>
            <BookOpen className="w-4 h-4" />
            <span>Knowledge & OS Guides</span>
          </NavLink>
        </div>

        {/* Hardware & Access */}
        <div className="space-y-1">
          <div className="px-3 text-[10px] font-bold tracking-wider text-slate-400 uppercase mb-2">
            Assets & Access
          </div>

          <NavLink to="/assets" className={navLinkClass}>
            <Laptop className="w-4 h-4" />
            <span>{isEmployee ? 'My Assigned Devices' : 'Hardware & Asset Inventory'}</span>
          </NavLink>

          <NavLink to="/access-requests" className={navLinkClass}>
            <Key className="w-4 h-4" />
            <span>{isEmployee ? 'Request Access / MFA' : 'Access & Account Requests'}</span>
          </NavLink>

          {(!isEmployee || isAdmin) && (
            <NavLink to="/onboarding" className={navLinkClass}>
              <UserCheck className="w-4 h-4" />
              <span>IT Onboarding Workflows</span>
            </NavLink>
          )}
        </div>

        {/* ITSM Management & Improvement */}
        {(!isEmployee || isAdmin) && (
          <div className="space-y-1">
            <div className="px-3 text-[10px] font-bold tracking-wider text-slate-400 uppercase mb-2">
              Service Management
            </div>

            <NavLink to="/improvements" className={navLinkClass}>
              <TrendingUp className="w-4 h-4" />
              <span>Continuous Improvements</span>
            </NavLink>

            {isAdmin && (
              <>
                <NavLink to="/users" className={navLinkClass}>
                  <Users className="w-4 h-4" />
                  <span>User & Staff Directory</span>
                </NavLink>

                <NavLink to="/audit-logs" className={navLinkClass}>
                  <ShieldCheck className="w-4 h-4" />
                  <span>Audit Trail & Compliance</span>
                </NavLink>
              </>
            )}
          </div>
        )}
      </div>

      {/* Support Info Box */}
      <div className="p-3 bg-slate-50 border border-slate-200 rounded-lg text-[11px] text-slate-600 mt-6">
        <div className="flex items-center gap-1.5 font-semibold text-slate-800 mb-1">
          <Headphones className="w-3.5 h-3.5 text-indigo-600" />
          IT Helpdesk Emergency
        </div>
        <p className="text-slate-500 mb-1">
          Urgent severity 1 outages or lost devices:
        </p>
        <span className="font-mono font-semibold text-slate-900 bg-white px-1.5 py-0.5 rounded border border-slate-200">
          Ext. 9111 • (555) 0199
        </span>
      </div>
    </aside>
  );
};
