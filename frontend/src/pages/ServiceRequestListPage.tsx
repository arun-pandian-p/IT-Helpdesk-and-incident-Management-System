import React, { useState, useEffect } from 'react';
import { Link, useSearchParams } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { serviceRequestService } from '../services/api';
import { ServiceRequest, ServiceRequestType } from '../types';
import { SRStatusBadge, ApprovalBadge, PriorityBadge, SLABadge, SupportTeamBadge } from '../components/Badges';
import {
  AlertCircle,
  ArrowUpDown,
  CheckCircle2,
  Clock,
  Filter,
  Layers,
  Package,
  PlusCircle,
  RefreshCw,
  Search,
  SlidersHorizontal,
  User,
  Users,
} from 'lucide-react';

export const ServiceRequestListPage: React.FC = () => {
  const { user, isEmployee, isSupport, isAdmin } = useAuth();
  const [searchParams, setSearchParams] = useSearchParams();

  const [requests, setRequests] = useState<ServiceRequest[]>([]);
  const [types, setTypes] = useState<ServiceRequestType[]>([]);
  const [total, setTotal] = useState(0);
  const [pages, setPages] = useState(1);
  const [currentPage, setCurrentPage] = useState(1);
  const [loading, setLoading] = useState(true);

  // Filters
  const activeTab = searchParams.get('view') || (isEmployee ? 'my' : 'all');
  const [search, setSearch] = useState(searchParams.get('search') || '');
  const [category, setCategory] = useState(searchParams.get('category') || '');
  const [priority, setPriority] = useState(searchParams.get('priority') || '');
  const [status, setStatus] = useState(searchParams.get('status') || '');
  const [approvalStatus, setApprovalStatus] = useState(searchParams.get('approval_status') || '');
  const [slaStatus, setSlaStatus] = useState(searchParams.get('sla_status') || '');

  // Pending approval count
  const [pendingApprovalsCount, setPendingApprovalsCount] = useState<number>(0);

  const fetchRequests = async () => {
    try {
      setLoading(true);
      const params: Record<string, any> = {
        page: currentPage,
        page_size: 15,
        view: activeTab,
        search: search.trim() || undefined,
        category: category || undefined,
        priority: priority || undefined,
        status: status || undefined,
        approval_status: approvalStatus || undefined,
        sla_status: slaStatus || undefined,
      };

      const res = await serviceRequestService.getServiceRequests(params);
      setRequests(res.items);
      setTotal(res.total);
      setPages(res.pages);

      // Check pending approvals count
      const approvalsRes = await serviceRequestService.getServiceRequests({
        view: 'approvals',
        page_size: 1,
      });
      setPendingApprovalsCount(approvalsRes.total);
    } catch (err) {
      console.error('Failed to fetch service requests:', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequests();
  }, [activeTab, currentPage, category, priority, status, approvalStatus, slaStatus]);

  useEffect(() => {
    const loadTypes = async () => {
      try {
        const t = await serviceRequestService.getRequestTypes();
        setTypes(t);
      } catch (e) {
        console.error('Error fetching request types:', e);
      }
    };
    loadTypes();
  }, []);

  const handleTabChange = (view: string) => {
    setCurrentPage(1);
    const newParams = new URLSearchParams(searchParams);
    newParams.set('view', view);
    setSearchParams(newParams);
  };

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setCurrentPage(1);
    fetchRequests();
  };

  const clearFilters = () => {
    setSearch('');
    setCategory('');
    setPriority('');
    setStatus('');
    setApprovalStatus('');
    setSlaStatus('');
    setCurrentPage(1);
    const newParams = new URLSearchParams();
    newParams.set('view', activeTab);
    setSearchParams(newParams);
  };

  return (
    <div className="space-y-6 pb-12 max-w-7xl mx-auto">
      {/* Top Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-6 rounded-2xl border border-slate-200 shadow-xs">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-2 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Package className="w-3.5 h-3.5" /> Service Request Management
          </div>
          <h1 className="text-2xl font-bold text-slate-900">
            {isEmployee ? 'My IT Service Requests' : 'Service Request Queue'}
          </h1>
          <p className="text-xs text-slate-500">
            Standard workplace orders, hardware allocation, application access grants, and routine IT services.
          </p>
        </div>

        <div className="flex items-center gap-3">
          <Link
            to="/service-desk/all"
            className="inline-flex items-center gap-1.5 px-3.5 py-2.5 rounded-xl border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
          >
            <Layers className="w-4 h-4 text-slate-500" />
            <span>Unified Desk</span>
          </Link>

          <Link
            to="/service-requests/create"
            className="inline-flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-xs hover:shadow"
          >
            <PlusCircle className="w-4 h-4" />
            <span>Submit Service Request</span>
          </Link>
        </div>
      </div>

      {/* Tabs Navigation */}
      <div className="flex items-center gap-1 overflow-x-auto border-b border-slate-200 pb-px">
        {(!isEmployee || isAdmin) && (
          <button
            onClick={() => handleTabChange('all')}
            className={`px-4 py-2.5 text-xs font-semibold rounded-t-xl transition border-b-2 whitespace-nowrap ${
              activeTab === 'all'
                ? 'border-indigo-600 text-indigo-700 bg-indigo-50/50'
                : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            All Requests ({activeTab === 'all' ? total : '•'})
          </button>
        )}

        <button
          onClick={() => handleTabChange('my')}
          className={`px-4 py-2.5 text-xs font-semibold rounded-t-xl transition border-b-2 whitespace-nowrap ${
            activeTab === 'my'
              ? 'border-indigo-600 text-indigo-700 bg-indigo-50/50'
              : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          }`}
        >
          My Requests
        </button>

        <button
          onClick={() => handleTabChange('approvals')}
          className={`px-4 py-2.5 text-xs font-semibold rounded-t-xl transition border-b-2 whitespace-nowrap flex items-center gap-2 ${
            activeTab === 'approvals'
              ? 'border-amber-600 text-amber-800 bg-amber-50/50'
              : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
          }`}
        >
          <span>Pending Approvals</span>
          {pendingApprovalsCount > 0 && (
            <span className="px-1.5 py-0.2 rounded-full text-[10px] font-bold bg-amber-500 text-white">
              {pendingApprovalsCount}
            </span>
          )}
        </button>

        {(!isEmployee || isAdmin) && (
          <button
            onClick={() => handleTabChange('assigned')}
            className={`px-4 py-2.5 text-xs font-semibold rounded-t-xl transition border-b-2 whitespace-nowrap ${
              activeTab === 'assigned'
                ? 'border-indigo-600 text-indigo-700 bg-indigo-50/50'
                : 'border-transparent text-slate-600 hover:text-slate-900 hover:bg-slate-50'
            }`}
          >
            Assigned to Me
          </button>
        )}
      </div>

      {/* Filter Toolbar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <form onSubmit={handleSearchSubmit} className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-6 gap-3">
          {/* Search */}
          <div className="lg:col-span-2 relative">
            <Search className="absolute left-3 top-2.5 w-4 h-4 text-slate-400" />
            <input
              type="text"
              placeholder="Search SR#, title, requester, app..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs rounded-lg border border-slate-300 focus:outline-none focus:border-indigo-500"
            />
          </div>

          {/* Category */}
          <div>
            <select
              value={category}
              onChange={(e) => {
                setCategory(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full py-2 px-2.5 text-xs rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">All Categories</option>
              <option value="Hardware">Hardware</option>
              <option value="Software & Licenses">Software & Licenses</option>
              <option value="Access & Identity">Access & Identity</option>
              <option value="Workplace & Accounts">Workplace & Accounts</option>
              <option value="Cloud & Collaboration">Cloud & Collaboration</option>
            </select>
          </div>

          {/* Status */}
          <div>
            <select
              value={status}
              onChange={(e) => {
                setStatus(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full py-2 px-2.5 text-xs rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">All Statuses</option>
              <option value="Submitted">Submitted</option>
              <option value="Pending Approval">Pending Approval</option>
              <option value="Approved">Approved</option>
              <option value="Assigned">Assigned</option>
              <option value="In Progress">In Progress</option>
              <option value="Waiting for User">Waiting for User</option>
              <option value="Fulfilled">Fulfilled</option>
              <option value="Closed">Closed</option>
              <option value="Cancelled">Cancelled</option>
            </select>
          </div>

          {/* Priority */}
          <div>
            <select
              value={priority}
              onChange={(e) => {
                setPriority(e.target.value);
                setCurrentPage(1);
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

          {/* SLA Filter & Reset */}
          <div className="flex items-center gap-2">
            <select
              value={slaStatus}
              onChange={(e) => {
                setSlaStatus(e.target.value);
                setCurrentPage(1);
              }}
              className="w-full py-2 px-2.5 text-xs rounded-lg border border-slate-300 bg-white focus:outline-none focus:border-indigo-500"
            >
              <option value="">All SLA</option>
              <option value="Within SLA">Within SLA</option>
              <option value="At Risk">At Risk</option>
              <option value="Breached">Breached</option>
            </select>

            <button
              type="button"
              onClick={clearFilters}
              title="Reset filters"
              className="p-2 rounded-lg border border-slate-300 text-slate-500 hover:text-slate-800 hover:bg-slate-50"
            >
              <RefreshCw className="w-3.5 h-3.5" />
            </button>
          </div>
        </form>
      </div>

      {/* Table Content */}
      <div className="bg-white rounded-2xl border border-slate-200 shadow-xs overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center p-12">
            <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
          </div>
        ) : requests.length === 0 ? (
          <div className="p-12 text-center space-y-3">
            <div className="w-12 h-12 bg-slate-100 rounded-full flex items-center justify-center mx-auto text-slate-400">
              <Package className="w-6 h-6" />
            </div>
            <h3 className="text-sm font-bold text-slate-800">No Service Requests Found</h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              There are no service requests matching your selected tab or filter criteria.
            </p>
            <div className="pt-2">
              <Link
                to="/service-requests/create"
                className="inline-flex items-center gap-2 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-semibold transition"
              >
                <PlusCircle className="w-3.5 h-3.5" /> Create Service Request
              </Link>
            </div>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50/75 border-b border-slate-200 text-slate-500 font-semibold uppercase tracking-wider text-[10px]">
                <tr>
                  <th className="py-3.5 px-4">Request #</th>
                  <th className="py-3.5 px-4">Subject & Offering</th>
                  <th className="py-3.5 px-4">Requester</th>
                  <th className="py-3.5 px-3">Priority</th>
                  <th className="py-3.5 px-3">Status</th>
                  <th className="py-3.5 px-3">Approval</th>
                  <th className="py-3.5 px-4">Assigned Team</th>
                  <th className="py-3.5 px-3">SLA Status</th>
                  <th className="py-3.5 px-4 text-right">Created</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.map((sr) => (
                  <tr
                    key={sr.id}
                    className="hover:bg-slate-50/75 transition cursor-pointer group"
                  >
                    {/* SR Number */}
                    <td className="py-3 px-4 font-mono font-bold text-indigo-600">
                      <Link
                        to={`/service-requests/${sr.id}`}
                        className="hover:underline flex items-center gap-1"
                      >
                        {sr.request_number}
                      </Link>
                    </td>

                    {/* Title & Category */}
                    <td className="py-3 px-4 max-w-xs">
                      <Link
                        to={`/service-requests/${sr.id}`}
                        className="font-bold text-slate-900 hover:text-indigo-600 block line-clamp-1"
                      >
                        {sr.title}
                      </Link>
                      <div className="text-[10px] text-slate-400 mt-0.5 flex items-center gap-1.5">
                        <span className="font-semibold text-slate-600">{sr.category}</span>
                        {sr.request_type_name && (
                          <span>• {sr.request_type_name}</span>
                        )}
                        {sr.application_name && (
                          <span>• App: {sr.application_name}</span>
                        )}
                      </div>
                    </td>

                    {/* Requester */}
                    <td className="py-3 px-4">
                      <div className="font-semibold text-slate-800">{sr.requester_name}</div>
                      <div className="text-[10px] text-slate-400">{sr.requester_department || 'Staff'}</div>
                    </td>

                    {/* Priority */}
                    <td className="py-3 px-3">
                      <PriorityBadge priority={sr.priority} />
                    </td>

                    {/* Status */}
                    <td className="py-3 px-3">
                      <SRStatusBadge status={sr.status} />
                    </td>

                    {/* Approval */}
                    <td className="py-3 px-3">
                      <ApprovalBadge status={sr.approval_status} />
                    </td>

                    {/* Assigned Team */}
                    <td className="py-3 px-4">
                      <SupportTeamBadge team={sr.assigned_team} />
                      {sr.assigned_to_name && (
                        <div className="text-[10px] text-slate-400 mt-0.5">
                          {sr.assigned_to_name}
                        </div>
                      )}
                    </td>

                    {/* SLA Status */}
                    <td className="py-3 px-3">
                      <SLABadge status={sr.sla_status} />
                    </td>

                    {/* Created Date */}
                    <td className="py-3 px-4 text-right text-[11px] text-slate-400 whitespace-nowrap">
                      {new Date(sr.created_at).toLocaleDateString()}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}

        {/* Pagination Footer */}
        {pages > 1 && (
          <div className="p-4 border-t border-slate-200 bg-slate-50/50 flex items-center justify-between text-xs text-slate-600">
            <div>
              Showing page <span className="font-bold text-slate-900">{currentPage}</span> of{' '}
              <span className="font-bold text-slate-900">{pages}</span> ({total} items)
            </div>
            <div className="flex items-center gap-2">
              <button
                disabled={currentPage <= 1}
                onClick={() => setCurrentPage((p) => Math.max(1, p - 1))}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white disabled:opacity-40 hover:bg-slate-50 transition font-medium"
              >
                Previous
              </button>
              <button
                disabled={currentPage >= pages}
                onClick={() => setCurrentPage((p) => Math.min(pages, p + 1))}
                className="px-3 py-1.5 rounded-lg border border-slate-200 bg-white disabled:opacity-40 hover:bg-slate-50 transition font-medium"
              >
                Next
              </button>
            </div>
          </div>
        )}
      </div>
    </div>
  );
};
