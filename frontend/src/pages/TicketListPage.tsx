import React, { useEffect, useState } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ticketService, categoryService } from '../services/api';
import { Ticket, TicketCategory, TicketPriority, TicketStatus } from '../types';
import { StatusBadge, PriorityBadge, SLABadge } from '../components/Badges';
import {
  Search,
  Filter,
  PlusCircle,
  Clock,
  Laptop,
  ChevronLeft,
  ChevronRight,
  ArrowUpDown,
  RefreshCw,
} from 'lucide-react';

export const TicketListPage: React.FC = () => {
  const { user, isEmployee } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [categories, setCategories] = useState<TicketCategory[]>([]);
  const [total, setTotal] = useState(0);
  const [totalPages, setTotalPages] = useState(1);
  const [loading, setLoading] = useState(true);

  // Filters from query params or local state
  const currentPage = parseInt(searchParams.get('page') || '1', 10);
  const statusFilter = searchParams.get('status') || '';
  const priorityFilter = searchParams.get('priority') || '';
  const categoryFilter = searchParams.get('category_id') || '';
  const slaFilter = searchParams.get('sla_status') || '';
  const searchQuery = searchParams.get('search') || '';
  const onlyMine = searchParams.get('mine') === 'true' || isEmployee;

  useEffect(() => {
    categoryService.getCategories().then(setCategories).catch(console.error);
  }, []);

  const fetchTickets = async () => {
    setLoading(true);
    try {
      const data = await ticketService.getTickets({
        page: currentPage,
        page_size: 15,
        status: statusFilter || undefined,
        priority: priorityFilter || undefined,
        category_id: categoryFilter || undefined,
        sla_status: slaFilter || undefined,
        search: searchQuery || undefined,
        only_mine: onlyMine,
      });
      setTickets(data.items);
      setTotal(data.total);
      setTotalPages(data.total_pages);
    } catch (err) {
      console.error('Failed to load tickets', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchTickets();
  }, [searchParams]);

  const updateFilter = (key: string, value: string) => {
    const next = new URLSearchParams(searchParams);
    if (value) {
      next.set(key, value);
    } else {
      next.delete(key);
    }
    next.set('page', '1');
    setSearchParams(next);
  };

  const handlePageChange = (newPage: number) => {
    const next = new URLSearchParams(searchParams);
    next.set('page', newPage.toString());
    setSearchParams(next);
  };

  return (
    <div className="space-y-5">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            {isEmployee ? 'My Technical Incidents' : 'IT Incident Management Queue'}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {isEmployee
              ? 'Track the status and resolution notes for your reported issues'
              : 'Enterprise queue with SLA compliance, device diagnostics, and escalation management'}
          </p>
        </div>
        <Link
          to="/tickets/new"
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
        >
          <PlusCircle className="w-4 h-4" />
          Report Incident
        </Link>
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col md:flex-row items-center gap-3">
          {/* Search */}
          <div className="relative flex-1 w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="Search by Ticket # (e.g. INC-2026-000001), title, or requester name..."
              value={searchQuery}
              onChange={(e) => updateFilter('search', e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 focus:border-indigo-500 outline-none"
            />
          </div>

          {/* Category Filter */}
          <select
            value={categoryFilter}
            onChange={(e) => updateFilter('category_id', e.target.value)}
            className="w-full md:w-44 py-2 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 focus:ring-2 focus:ring-indigo-500 outline-none"
          >
            <option value="">All Categories</option>
            {categories.map((c) => (
              <option key={c.id} value={c.id}>
                {c.name}
              </option>
            ))}
          </select>

          {/* Priority Filter */}
          <select
            value={priorityFilter}
            onChange={(e) => updateFilter('priority', e.target.value)}
            className="w-full md:w-36 py-2 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 focus:ring-2 focus:ring-indigo-500 outline-none"
          >
            <option value="">All Priorities</option>
            <option value="Critical">Critical</option>
            <option value="High">High</option>
            <option value="Medium">Medium</option>
            <option value="Low">Low</option>
          </select>

          {/* SLA Filter */}
          <select
            value={slaFilter}
            onChange={(e) => updateFilter('sla_status', e.target.value)}
            className="w-full md:w-36 py-2 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 focus:ring-2 focus:ring-indigo-500 outline-none"
          >
            <option value="">All SLA States</option>
            <option value="Within SLA">Within SLA</option>
            <option value="At Risk">At Risk</option>
            <option value="Breached">Breached</option>
          </select>

          {!isEmployee && (
            <button
              onClick={() => updateFilter('mine', onlyMine ? '' : 'true')}
              className={`px-3 py-2 text-xs font-semibold rounded-lg border transition ${
                onlyMine
                  ? 'bg-indigo-50 border-indigo-300 text-indigo-700'
                  : 'bg-white border-slate-300 text-slate-700 hover:bg-slate-50'
              }`}
            >
              My Queue
            </button>
          )}

          <button
            onClick={fetchTickets}
            title="Refresh list"
            className="p-2 border border-slate-300 rounded-lg text-slate-600 hover:bg-slate-50 transition"
          >
            <RefreshCw className="w-4 h-4" />
          </button>
        </div>

        {/* Status Filter Tabs */}
        <div className="flex items-center gap-1 overflow-x-auto pt-2 border-t border-slate-100 text-xs">
          {[
            { label: 'All Incidents', val: '' },
            { label: 'Open', val: 'Open' },
            { label: 'In Progress', val: 'In Progress' },
            { label: 'Waiting for User', val: 'Waiting for User' },
            { label: 'Escalated', val: 'Escalated' },
            { label: 'Resolved', val: 'Resolved' },
            { label: 'Closed', val: 'Closed' },
          ].map((tab) => (
            <button
              key={tab.val}
              onClick={() => updateFilter('status', tab.val)}
              className={`px-3 py-1.5 rounded-lg whitespace-nowrap font-medium transition ${
                statusFilter === tab.val
                  ? 'bg-indigo-600 text-white font-semibold'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              {tab.label}
            </button>
          ))}
        </div>
      </div>

      {/* Table Card */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        {loading ? (
          <div className="py-20 flex flex-col items-center justify-center">
            <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
            <span className="text-xs text-slate-500 font-medium mt-2">Loading incidents...</span>
          </div>
        ) : tickets.length === 0 ? (
          <div className="py-20 text-center">
            <Filter className="w-8 h-8 text-slate-400 mx-auto mb-2" />
            <p className="text-xs font-semibold text-slate-800">No matching incidents found.</p>
            <p className="text-[11px] text-slate-500 mt-0.5">Try adjusting your filters or search query.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Ticket</th>
                  <th className="py-3 px-4">Problem Summary</th>
                  <th className="py-3 px-4">Category</th>
                  <th className="py-3 px-4">Environment</th>
                  <th className="py-3 px-4">Priority</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">SLA Target</th>
                  <th className="py-3 px-4">Requester</th>
                  <th className="py-3 px-4">Assigned To</th>
                  <th className="py-3 px-4">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {tickets.map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-3 px-4 font-mono font-bold text-indigo-600">
                      <Link to={`/tickets/${t.id}`} className="hover:underline">
                        {t.ticket_number}
                      </Link>
                    </td>
                    <td className="py-3 px-4 max-w-xs truncate text-slate-900 font-medium">
                      <Link to={`/tickets/${t.id}`} className="hover:text-indigo-600">
                        {t.title}
                      </Link>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{t.category_name}</td>
                    <td className="py-3 px-4 text-slate-500">
                      {t.device_type || t.os_name ? (
                        <span className="inline-flex items-center gap-1 text-[11px] bg-slate-100 px-2 py-0.5 rounded text-slate-700">
                          <Laptop className="w-3 h-3 text-slate-500" />
                          {t.device_type || t.os_name}
                        </span>
                      ) : (
                        '—'
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <PriorityBadge priority={t.priority} />
                    </td>
                    <td className="py-3 px-4">
                      <StatusBadge status={t.status} />
                    </td>
                    <td className="py-3 px-4">
                      <SLABadge status={t.sla_status} />
                    </td>
                    <td className="py-3 px-4 text-slate-700 font-medium">
                      {t.creator_name}
                      <span className="block text-[10px] text-slate-400">{t.creator_department}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-600">
                      {t.assignee_name ? (
                        <span className="font-medium text-slate-800">{t.assignee_name}</span>
                      ) : (
                        <span className="text-slate-400 italic">Unassigned</span>
                      )}
                    </td>
                    <td className="py-3 px-4">
                      <Link
                        to={`/tickets/${t.id}`}
                        className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-semibold text-[11px] transition"
                      >
                        Manage
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Bar */}
        <div className="p-4 border-t border-slate-200 flex items-center justify-between text-xs text-slate-600">
          <div>
            Showing <span className="font-semibold text-slate-900">{tickets.length}</span> of{' '}
            <span className="font-semibold text-slate-900">{total}</span> incidents
          </div>
          <div className="flex items-center gap-2">
            <button
              onClick={() => handlePageChange(currentPage - 1)}
              disabled={currentPage <= 1}
              className="p-1.5 rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-40 transition"
            >
              <ChevronLeft className="w-4 h-4" />
            </button>
            <span className="px-2 font-medium">
              Page {currentPage} of {totalPages}
            </span>
            <button
              onClick={() => handlePageChange(currentPage + 1)}
              disabled={currentPage >= totalPages}
              className="p-1.5 rounded border border-slate-200 hover:bg-slate-50 disabled:opacity-40 transition"
            >
              <ChevronRight className="w-4 h-4" />
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
