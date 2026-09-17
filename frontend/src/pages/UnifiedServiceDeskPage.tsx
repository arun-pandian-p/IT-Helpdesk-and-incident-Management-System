import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { serviceRequestService } from '../services/api';
import { UnifiedWorkItem } from '../types';
import { PriorityBadge, SLABadge, StatusBadge, SRStatusBadge, SupportTeamBadge } from '../components/Badges';
import {
  AlertTriangle,
  ArrowRight,
  CheckCircle2,
  Clock,
  Filter,
  Layers,
  Package,
  PlusCircle,
  RefreshCw,
  Search,
  SlidersHorizontal,
  Ticket,
} from 'lucide-react';

export const UnifiedServiceDeskPage: React.FC = () => {
  const { user, isEmployee, isSupport, isAdmin } = useAuth();
  const navigate = useNavigate();

  const [items, setItems] = useState<UnifiedWorkItem[]>([]);
  const [loading, setLoading] = useState(true);
  const [page, setPage] = useState(1);

  // Filters
  const [itemType, setItemType] = useState<string>(''); // '' | 'Incident' | 'Service Request'
  const [search, setSearch] = useState('');
  const [priority, setPriority] = useState('');
  const [statusFilter, setStatusFilter] = useState('');

  const fetchUnifiedWork = async () => {
    try {
      setLoading(true);
      const params: Record<string, any> = {
        page,
        page_size: 30,
        item_type: itemType || undefined,
        search: search.trim() || undefined,
        priority: priority || undefined,
        status: statusFilter || undefined,
      };
      const res = await serviceRequestService.getUnifiedServiceDeskWork(params);
      setItems(res);
    } catch (err) {
      console.error('Failed to load unified service desk work items:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchUnifiedWork();
  }, [page, itemType, priority, statusFilter]);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setPage(1);
    fetchUnifiedWork();
  };

  const clearFilters = () => {
    setSearch('');
    setItemType('');
    setPriority('');
    setStatusFilter('');
    setPage(1);
  };

  // KPIs
  const incidentCount = items.filter((i) => i.item_type === 'Incident').length;
  const requestCount = items.filter((i) => i.item_type === 'Service Request').length;
  const breachedCount = items.filter((i) => i.is_breached).length;

  return (
    <div className="space-y-6 pb-12 max-w-7xl mx-auto">
      {/* Top Banner */}
      <div className="bg-white p-6 rounded-2xl border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-slate-100 text-slate-800 border border-slate-200">
            <Layers className="w-3.5 h-3.5 text-indigo-600" /> Unified Service Desk
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Unified IT Work Queue</h1>
          <p className="text-xs text-slate-500">
            Unified view combining technical break/fix incidents and routine service catalog requests into a consolidated queue.
          </p>
        </div>

        <div className="flex items-center gap-2.5">
          <Link
            to="/tickets/new"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl border border-rose-200 bg-rose-50 hover:bg-rose-100 text-rose-700 text-xs font-bold transition"
          >
            <Ticket className="w-4 h-4" />
            <span>Report Incident</span>
          </Link>
          <Link
            to="/service-requests/create"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold transition shadow-xs hover:shadow"
          >
            <Package className="w-4 h-4" />
            <span>Order Service</span>
          </Link>
        </div>
      </div>

      {/* Metric Highlights */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
        <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-1">
          <span className="text-[11px] font-semibold text-slate-500">Total Work Items Visible</span>
          <div className="text-2xl font-bold text-slate-900">{items.length}</div>
          <div className="text-[10px] text-slate-400">Current active slice</div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-rose-200 shadow-xs space-y-1">
          <span className="text-[11px] font-semibold text-rose-700 flex items-center gap-1">
            <AlertTriangle className="w-3.5 h-3.5 text-rose-600" /> Incidents (Break/Fix)
          </span>
          <div className="text-2xl font-bold text-rose-900">{incidentCount}</div>
          <div className="text-[10px] text-rose-500">Prefix: INC-2026-XXXXXX</div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-indigo-200 shadow-xs space-y-1">
          <span className="text-[11px] font-semibold text-indigo-700 flex items-center gap-1">
            <Package className="w-3.5 h-3.5 text-indigo-600" /> Service Requests (Catalog)
          </span>
          <div className="text-2xl font-bold text-indigo-900">{requestCount}</div>
          <div className="text-[10px] text-indigo-500">Prefix: SR-2026-XXXXXX</div>
        </div>

        <div className="bg-white p-4 rounded-xl border border-amber-200 shadow-xs space-y-1">
          <span className="text-[11px] font-semibold text-amber-800 flex items-center gap-1">
            <Clock className="w-3.5 h-3.5 text-amber-600" /> SLA Breached / Critical
          </span>
          <div className="text-2xl font-bold text-amber-900">{breachedCount}</div>
          <div className="text-[10px] text-amber-600">Requires prompt escalation</div>
        </div>
      </div>

      {/* Filter Tabs & Toolbar */}
      <div className="bg-white p-4 rounded-2xl border border-slate-200 shadow-xs space-y-4">
        {/* Type Tabs */}
        <div className="flex items-center gap-2 border-b border-slate-100 pb-3">
          <button
            onClick={() => {
              setItemType('');
              setPage(1);
            }}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition ${
              itemType === ''
                ? 'bg-slate-900 text-white'
                : 'bg-slate-100 text-slate-600 hover:bg-slate-200'
            }`}
          >
            All Work Types
          </button>
          <button
            onClick={() => {
              setItemType('Incident');
              setPage(1);
            }}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
              itemType === 'Incident'
                ? 'bg-rose-600 text-white'
                : 'bg-rose-50 text-rose-700 hover:bg-rose-100 border border-rose-200'
            }`}
          >
            <Ticket className="w-3 h-3" /> Incidents Only
          </button>
          <button
            onClick={() => {
              setItemType('Service Request');
              setPage(1);
            }}
            className={`px-3.5 py-1.5 rounded-lg text-xs font-semibold transition flex items-center gap-1.5 ${
              itemType === 'Service Request'
                ? 'bg-indigo-600 text-white'
                : 'bg-indigo-50 text-indigo-700 hover:bg-indigo-100 border border-indigo-200'
            }`}
          >
            <Package className="w-3 h-3" /> Service Requests Only
          </button>
        </div>

        {/* Inputs */}
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-5 gap-3">
          <div className="lg:col-span-2 relative">
            <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search reference #, title, requester..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-slate-300 focus:outline-none focus:border-indigo-500"
            />
          </div>

          <div>
            <select
              value={priority}
              onChange={(e) => {
                setPriority(e.target.value);
                setPage(1);
              }}
              className="w-full py-2 px-2.5 text-xs rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">All Priorities</option>
              <option value="Critical">Critical</option>
              <option value="High">High</option>
              <option value="Medium">Medium</option>
              <option value="Low">Low</option>
            </select>
          </div>

          <div>
            <select
              value={statusFilter}
              onChange={(e) => {
                setStatusFilter(e.target.value);
                setPage(1);
              }}
              className="w-full py-2 px-2.5 text-xs rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">All Statuses</option>
              <option value="Open">Open</option>
              <option value="Submitted">Submitted</option>
              <option value="Pending Approval">Pending Approval</option>
              <option value="In Progress">In Progress</option>
              <option value="Waiting for User">Waiting for User</option>
              <option value="Resolved">Resolved</option>
              <option value="Fulfilled">Fulfilled</option>
              <option value="Closed">Closed</option>
            </select>
          </div>

          <div className="flex items-center gap-2">
            <button
              type="submit"
              className="flex-1 py-2 px-3 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold transition"
            >
              Apply Filter
            </button>
            <button
              type="button"
              onClick={clearFilters}
              title="Reset"
              className="p-2 border border-slate-300 rounded-lg text-slate-500 hover:bg-slate-50"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </form>
      </div>

      {/* Unified Table */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center p-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : items.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
              <Layers className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-slate-800">No Work Items Found</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              No matching incidents or service requests found for the active filter set.
            </p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50/75 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="py-3.5 px-4">Item Type</th>
                  <th className="py-3.5 px-4">Reference #</th>
                  <th className="py-3.5 px-4">Subject & Scope</th>
                  <th className="py-3.5 px-4">Requester</th>
                  <th className="py-3.5 px-3">Priority</th>
                  <th className="py-3.5 px-3">Status</th>
                  <th className="py-3.5 px-4">Assignee</th>
                  <th className="py-3.5 px-3">SLA Status</th>
                  <th className="py-3.5 px-4 text-right">Target Due</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {items.map((item) => {
                  const isIncident = item.item_type === 'Incident';
                  const detailUrl = isIncident ? `/tickets/${item.id}` : `/service-requests/${item.id}`;

                  return (
                    <tr
                      key={`${item.item_type}-${item.id}`}
                      onClick={() => navigate(detailUrl)}
                      className="hover:bg-slate-50/75 transition cursor-pointer group"
                    >
                      {/* Item Type Badge */}
                      <td className="py-3 px-4">
                        {isIncident ? (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-rose-50 text-rose-700 border border-rose-200">
                            <Ticket className="w-3 h-3 text-rose-600" /> Incident
                          </span>
                        ) : (
                          <span className="inline-flex items-center gap-1 px-2.5 py-0.5 rounded-full text-[10px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                            <Package className="w-3 h-3 text-indigo-600" /> Service Request
                          </span>
                        )}
                      </td>

                      {/* Reference Number */}
                      <td className="py-3 px-4 font-mono font-bold">
                        <Link
                          to={detailUrl}
                          className={`hover:underline ${isIncident ? 'text-rose-600' : 'text-indigo-600'}`}
                        >
                          {item.reference_number}
                        </Link>
                      </td>

                      {/* Subject & Category */}
                      <td className="py-3 px-4 max-w-xs">
                        <Link
                          to={detailUrl}
                          className="font-bold text-slate-900 group-hover:text-indigo-600 block line-clamp-1"
                        >
                          {item.title}
                        </Link>
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          {item.category}
                        </div>
                      </td>

                      {/* Requester */}
                      <td className="py-3 px-4">
                        <div className="font-semibold text-slate-800">{item.requester_name}</div>
                        <div className="text-[10px] text-slate-400">{item.requester_department}</div>
                      </td>

                      {/* Priority */}
                      <td className="py-3 px-3">
                        <PriorityBadge priority={item.priority} />
                      </td>

                      {/* Status */}
                      <td className="py-3 px-3">
                        {isIncident ? (
                          <StatusBadge status={item.status} />
                        ) : (
                          <SRStatusBadge status={item.status} />
                        )}
                      </td>

                      {/* Assignee */}
                      <td className="py-3 px-4">
                        <div className="font-medium text-slate-800">
                          {item.assigned_to_name || 'Unassigned'}
                        </div>
                        <div className="text-[10px] text-slate-400">{item.assigned_team}</div>
                      </td>

                      {/* SLA Status */}
                      <td className="py-3 px-3">
                        <SLABadge status={item.sla_status} />
                      </td>

                      {/* Target Due */}
                      <td className="py-3 px-4 text-right text-[11px] text-slate-500 whitespace-nowrap">
                        {new Date(item.due_at).toLocaleString([], {
                          month: 'short',
                          day: 'numeric',
                          hour: '2-digit',
                          minute: '2-digit',
                        })}
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        )}
      </div>
    </div>
  );
};
