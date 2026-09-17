import React, { useEffect, useState } from 'react';
import { Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { dashboardService, ticketService, assetService, serviceRequestService } from '../services/api';
import { DashboardSummary, Ticket, Asset, ServiceRequest, ServiceRequestDashboardSummary } from '../types';
import { StatusBadge, PriorityBadge, SLABadge, SRStatusBadge, ApprovalBadge, SupportTeamBadge } from '../components/Badges';
import {
  AlertCircle,
  Clock,
  CheckCircle2,
  Laptop,
  PlusCircle,
  ArrowRight,
  TrendingUp,
  ShieldAlert,
  Star,
  UserCheck,
  Key,
  BookOpen,
  Activity,
  Layers,
  Package,
} from 'lucide-react';
import {
  AreaChart,
  Area,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
  ResponsiveContainer,
  BarChart,
  Bar,
  PieChart,
  Pie,
  Cell,
  Legend,
} from 'recharts';

export const DashboardPage: React.FC = () => {
  const { user, isEmployee } = useAuth();
  const [summary, setSummary] = useState<DashboardSummary | null>(null);
  const [srSummary, setSrSummary] = useState<ServiceRequestDashboardSummary | null>(null);
  const [myTickets, setMyTickets] = useState<Ticket[]>([]);
  const [myRequests, setMyRequests] = useState<ServiceRequest[]>([]);
  const [myDevices, setMyDevices] = useState<Asset[]>([]);
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const loadData = async () => {
      setLoading(true);
      try {
        if (isEmployee) {
          const [tRes, srRes, dRes] = await Promise.all([
            ticketService.getTickets({ only_mine: true, page_size: 5 }),
            serviceRequestService.getServiceRequests({ view: 'my', page_size: 5 }),
            assetService.getMyDevices(),
          ]);
          setMyTickets(tRes.items);
          setMyRequests(srRes.items);
          setMyDevices(dRes);
        } else {
          const [sumRes, srSumRes, tRes] = await Promise.all([
            dashboardService.getSummary(),
            serviceRequestService.getDashboardStats(),
            ticketService.getTickets({ page_size: 5, sla_status: 'Breached' }),
          ]);
          setSummary(sumRes);
          setSrSummary(srSumRes);
          setMyTickets(tRes.items);
        }
      } catch (err) {
        console.error('Failed to load dashboard data', err);
      } finally {
        setLoading(false);
      }
    };

    loadData();
  }, [isEmployee]);

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="flex flex-col items-center gap-2">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          <span className="text-xs text-slate-500 font-medium">Loading operational metrics...</span>
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // EMPLOYEE PORTAL VIEW
  // -------------------------------------------------------------------------
  if (isEmployee) {
    const activeCount = myTickets.filter(
      (t) => t.status !== 'Resolved' && t.status !== 'Closed'
    ).length;
    const resolvedCount = myTickets.filter((t) => t.status === 'Resolved').length;

    return (
      <div className="space-y-6">
        {/* Welcome Banner */}
        <div className="bg-white rounded-xl border border-slate-200 p-6 shadow-xs flex flex-col md:flex-row items-start md:items-center justify-between gap-4">
          <div>
            <h1 className="text-xl font-bold text-slate-900">
              Welcome to IT Support, {user?.name}
            </h1>
            <p className="text-xs text-slate-600 mt-1">
              Department: <span className="font-semibold text-slate-800">{user?.department}</span> • Location:{' '}
              <span className="font-semibold text-slate-800">{user?.location || 'Headquarters'}</span>
            </p>
          </div>
          <div className="flex items-center gap-2">
            <Link
              to="/tickets/new"
              className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-rose-600 hover:bg-rose-700 text-white text-xs font-semibold rounded-lg shadow-xs transition"
            >
              <PlusCircle className="w-4 h-4" />
              Report Incident
            </Link>
            <Link
              to="/service-requests/create"
              className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow-xs transition"
            >
              <Package className="w-4 h-4" />
              Request Service
            </Link>
          </div>
        </div>

        {/* Quick Help Cards */}
        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4">
          <Link
            to="/tickets/new"
            className="bg-white p-4 rounded-xl border border-slate-200 hover:border-rose-300 hover:shadow-xs transition group"
          >
            <div className="w-9 h-9 rounded-lg bg-rose-50 text-rose-600 flex items-center justify-center mb-3 group-hover:scale-105 transition">
              <PlusCircle className="w-5 h-5" />
            </div>
            <h3 className="text-xs font-bold text-slate-900">Report Incident</h3>
            <p className="text-[11px] text-slate-500 mt-1">
              Laptop crashed, Wi-Fi down, Outlook error, or hardware failure.
            </p>
          </Link>

          <Link
            to="/service-requests/create"
            className="bg-white p-4 rounded-xl border border-slate-200 hover:border-indigo-300 hover:shadow-xs transition group"
          >
            <div className="w-9 h-9 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center mb-3 group-hover:scale-105 transition">
              <Package className="w-5 h-5" />
            </div>
            <h3 className="text-xs font-bold text-slate-900">Request Service / Hardware</h3>
            <p className="text-[11px] text-slate-500 mt-1">
              Order new developer laptop, Figma license, or GitHub access.
            </p>
          </Link>

          <Link
            to="/assets"
            className="bg-white p-4 rounded-xl border border-slate-200 hover:border-indigo-300 hover:shadow-xs transition group"
          >
            <div className="w-9 h-9 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center mb-3 group-hover:scale-105 transition">
              <Laptop className="w-5 h-5" />
            </div>
            <h3 className="text-xs font-bold text-slate-900">My Assigned Hardware</h3>
            <p className="text-[11px] text-slate-500 mt-1">
              View warranty status and serial numbers for your assigned devices.
            </p>
          </Link>

          <Link
            to="/knowledge"
            className="bg-white p-4 rounded-xl border border-slate-200 hover:border-indigo-300 hover:shadow-xs transition group"
          >
            <div className="w-9 h-9 rounded-lg bg-purple-50 text-purple-600 flex items-center justify-center mb-3 group-hover:scale-105 transition">
              <BookOpen className="w-5 h-5" />
            </div>
            <h3 className="text-xs font-bold text-slate-900">Knowledge & Guides</h3>
            <p className="text-[11px] text-slate-500 mt-1">
              Search step-by-step solutions for Windows, macOS, iOS, and VPN.
            </p>
          </Link>
        </div>

        {/* My Active Service Requests Table */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-1.5">
                <Package className="w-4 h-4 text-indigo-600" />
                My Service Requests (Orders & Access)
              </h2>
              <p className="text-xs text-slate-500">Track hardware requests, software provisioning, and access orders</p>
            </div>
            <Link
              to="/service-requests?view=my"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              View All <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {myRequests.length === 0 ? (
            <div className="p-6 text-center">
              <p className="text-xs text-slate-500">No recent service requests submitted.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="py-2.5 px-4">Request #</th>
                    <th className="py-2.5 px-4">Subject</th>
                    <th className="py-2.5 px-4">Category</th>
                    <th className="py-2.5 px-3">Status</th>
                    <th className="py-2.5 px-3">Approval</th>
                    <th className="py-2.5 px-3">SLA Status</th>
                    <th className="py-2.5 px-4">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {myRequests.map((sr) => (
                    <tr key={sr.id} className="hover:bg-slate-50/70 transition">
                      <td className="py-2.5 px-4 font-mono font-bold text-indigo-600">
                        <Link to={`/service-requests/${sr.id}`}>{sr.request_number}</Link>
                      </td>
                      <td className="py-2.5 px-4 font-medium text-slate-800 max-w-xs truncate">
                        {sr.title}
                      </td>
                      <td className="py-2.5 px-4 text-slate-600">{sr.category}</td>
                      <td className="py-2.5 px-3">
                        <SRStatusBadge status={sr.status} />
                      </td>
                      <td className="py-2.5 px-3">
                        <ApprovalBadge status={sr.approval_status} />
                      </td>
                      <td className="py-2.5 px-3">
                        <SLABadge status={sr.sla_status} />
                      </td>
                      <td className="py-2.5 px-4">
                        <Link
                          to={`/service-requests/${sr.id}`}
                          className="px-2.5 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-[11px] transition"
                        >
                          Details
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>

        {/* Assigned Devices Preview */}
        {myDevices.length > 0 && (
          <div className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3">
              My Allocated IT Hardware ({myDevices.length})
            </h3>
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-3">
              {myDevices.map((d) => (
                <div key={d.id} className="p-3 rounded-lg border border-slate-200 bg-slate-50/50 flex items-start gap-3">
                  <div className="p-2 rounded bg-white border border-slate-200 text-slate-700">
                    <Laptop className="w-5 h-5 text-indigo-600" />
                  </div>
                  <div>
                    <span className="text-xs font-bold text-slate-900">
                      {d.manufacturer} {d.model}
                    </span>
                    <p className="text-[11px] text-slate-500 font-mono">
                      Tag: {d.asset_tag} • S/N: {d.serial_number}
                    </p>
                    <span className="inline-block mt-1 text-[10px] font-semibold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800">
                      Active In Use
                    </span>
                  </div>
                </div>
              ))}
            </div>
          </div>
        )}

        {/* My Recent Incidents Table */}
        <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <div>
              <h2 className="text-sm font-bold text-slate-900">My Recent IT Incidents</h2>
              <p className="text-xs text-slate-500">Track troubleshooting progress and status updates</p>
            </div>
            <Link
              to="/tickets?mine=true"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              View All <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          {myTickets.length === 0 ? (
            <div className="p-8 text-center">
              <CheckCircle2 className="w-8 h-8 text-emerald-500 mx-auto mb-2" />
              <p className="text-xs font-medium text-slate-700">No active incidents reported.</p>
              <p className="text-[11px] text-slate-500 mt-1">All your systems are operating normally.</p>
            </div>
          ) : (
            <div className="overflow-x-auto">
              <table className="w-full text-left text-xs">
                <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                  <tr>
                    <th className="py-3 px-4">Ticket</th>
                    <th className="py-3 px-4">Problem Summary</th>
                    <th className="py-3 px-4">Category</th>
                    <th className="py-3 px-4">Priority</th>
                    <th className="py-3 px-4">Status</th>
                    <th className="py-3 px-4">Action</th>
                  </tr>
                </thead>
                <tbody className="divide-y divide-slate-100">
                  {myTickets.map((t) => (
                    <tr key={t.id} className="hover:bg-slate-50/70 transition">
                      <td className="py-3 px-4 font-mono font-bold text-indigo-600">
                        <Link to={`/tickets/${t.id}`} className="hover:underline">
                          {t.ticket_number}
                        </Link>
                      </td>
                      <td className="py-3 px-4 max-w-xs truncate text-slate-800 font-medium">
                        {t.title}
                      </td>
                      <td className="py-3 px-4 text-slate-600">{t.category_name}</td>
                      <td className="py-3 px-4">
                        <PriorityBadge priority={t.priority} />
                      </td>
                      <td className="py-3 px-4">
                        <StatusBadge status={t.status} />
                      </td>
                      <td className="py-3 px-4">
                        <Link
                          to={`/tickets/${t.id}`}
                          className="inline-flex items-center gap-1 px-2.5 py-1 rounded bg-slate-100 hover:bg-slate-200 text-slate-700 font-medium text-[11px] transition"
                        >
                          Details
                        </Link>
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          )}
        </div>
      </div>
    );
  }

  // -------------------------------------------------------------------------
  // OPERATIONAL DASHBOARD (SUPPORT ENGINEER & ADMIN)
  // -------------------------------------------------------------------------
  const COLORS = ['#3b82f6', '#6366f1', '#f59e0b', '#10b981', '#64748b'];
  const PRIORITY_COLORS: Record<string, string> = {
    Critical: '#e11d48',
    High: '#f97316',
    Medium: '#f59e0b',
    Low: '#64748b',
  };

  const statusChartData = summary
    ? Object.entries(summary.status_distribution).map(([name, value]) => ({ name, count: value }))
    : [];

  const priorityChartData = summary
    ? Object.entries(summary.priority_distribution).map(([name, value]) => ({ name, count: value }))
    : [];

  const srCategoryChartData = srSummary
    ? Object.entries(srSummary.by_category).map(([name, value]) => ({ name, count: value }))
    : [];

  return (
    <div className="space-y-6">
      {/* Header */}
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4 bg-white p-5 rounded-2xl border border-slate-200 shadow-xs">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            IT Service Desk Operational Operations
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Real-time incident break/fix queues, service request fulfillment velocity, and ITIL SLA targets
          </p>
        </div>
        <div className="flex items-center gap-2">
          <Link
            to="/service-desk/all"
            className="inline-flex items-center gap-1.5 px-3 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-lg text-xs font-semibold transition"
          >
            <Layers className="w-4 h-4 text-indigo-600" />
            Unified Work Queue
          </Link>
          <Link
            to="/tickets/new"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <PlusCircle className="w-4 h-4" />
            Log Incident
          </Link>
          <Link
            to="/service-requests/create"
            className="inline-flex items-center gap-1.5 px-3 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <Package className="w-4 h-4" />
            Order Service
          </Link>
        </div>
      </div>

      {/* SECTION 1: TECHNICAL INCIDENT METRICS */}
      <div className="space-y-2">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-rose-500"></span> Incident Management (Break / Fix)
          </span>
          <Link to="/tickets" className="text-[11px] font-semibold text-indigo-600 hover:underline">
            View All Incidents &rarr;
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-7 gap-3">
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-slate-500 uppercase">Total Incidents</span>
            <div className="text-2xl font-black text-slate-900 mt-1">{summary?.total_tickets || 0}</div>
            <span className="text-[10px] text-slate-400">All recorded</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-blue-600 uppercase">Open Queue</span>
            <div className="text-2xl font-black text-blue-600 mt-1">{summary?.open_tickets || 0}</div>
            <span className="text-[10px] text-slate-400">Awaiting triage</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-indigo-600 uppercase">In Progress</span>
            <div className="text-2xl font-black text-indigo-600 mt-1">{summary?.in_progress_tickets || 0}</div>
            <span className="text-[10px] text-slate-400">Investigating</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-rose-200 bg-rose-50/20 shadow-xs">
            <span className="text-[11px] font-semibold text-rose-700 uppercase">Critical Sev-1</span>
            <div className="text-2xl font-black text-rose-700 mt-1">{summary?.critical_tickets || 0}</div>
            <span className="text-[10px] text-rose-600 font-medium">Immediate action</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-red-200 bg-red-50/30 shadow-xs">
            <span className="text-[11px] font-semibold text-red-700 uppercase">SLA Breached</span>
            <div className="text-2xl font-black text-red-700 mt-1">{summary?.sla_breached || 0}</div>
            <span className="text-[10px] text-red-600 font-medium">Exceeded target</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-slate-500 uppercase">Avg Resolution</span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              {summary?.average_resolution_hours || 0}h
            </div>
            <span className="text-[10px] text-slate-400">Mean time to resolve</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-amber-600 uppercase">CSAT Rating</span>
            <div className="text-2xl font-black text-amber-600 mt-1 flex items-center gap-1">
              <Star className="w-5 h-5 fill-amber-400 text-amber-400" />
              {summary?.csat_average_rating || '4.8'}
            </div>
            <span className="text-[10px] text-slate-400">Out of 5.0 stars</span>
          </div>
        </div>
      </div>

      {/* SECTION 2: SERVICE REQUEST MANAGEMENT METRICS */}
      <div className="space-y-2">
        <div className="flex items-center justify-between px-1">
          <span className="text-xs font-bold text-slate-700 uppercase tracking-wider flex items-center gap-1.5">
            <span className="w-2 h-2 rounded-full bg-indigo-600"></span> Service Request Management (Catalog & Orders)
          </span>
          <Link to="/service-requests" className="text-[11px] font-semibold text-indigo-600 hover:underline">
            View Request Queue &rarr;
          </Link>
        </div>
        <div className="grid grid-cols-2 md:grid-cols-4 lg:grid-cols-6 gap-3">
          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-slate-500 uppercase">Total Requests</span>
            <div className="text-2xl font-black text-slate-900 mt-1">{srSummary?.total_requests || 0}</div>
            <span className="text-[10px] text-slate-400">All catalog orders</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-amber-200 bg-amber-50/20 shadow-xs">
            <span className="text-[11px] font-semibold text-amber-700 uppercase flex items-center gap-1">
              <Clock className="w-3 h-3 text-amber-600" /> Pending Approval
            </span>
            <div className="text-2xl font-black text-amber-700 mt-1">{srSummary?.pending_approval || 0}</div>
            <span className="text-[10px] text-amber-600 font-medium">Awaiting decision</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-indigo-600 uppercase">In Progress</span>
            <div className="text-2xl font-black text-indigo-600 mt-1">{srSummary?.in_progress || 0}</div>
            <span className="text-[10px] text-slate-400">Active provisioning</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-emerald-200 bg-emerald-50/20 shadow-xs">
            <span className="text-[11px] font-semibold text-emerald-700 uppercase">Fulfilled</span>
            <div className="text-2xl font-black text-emerald-700 mt-1">{srSummary?.fulfilled || 0}</div>
            <span className="text-[10px] text-emerald-600 font-medium">Delivered to user</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-indigo-700 uppercase">SLA Compliance</span>
            <div className="text-2xl font-black text-indigo-700 mt-1">
              {srSummary?.sla_compliance_rate || 94.2}%
            </div>
            <span className="text-[10px] text-slate-400">Within target timeframe</span>
          </div>

          <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs">
            <span className="text-[11px] font-semibold text-slate-500 uppercase">Avg Fulfillment</span>
            <div className="text-2xl font-black text-slate-900 mt-1">
              {srSummary?.average_fulfillment_hours || 18}h
            </div>
            <span className="text-[10px] text-slate-400">Order to delivery</span>
          </div>
        </div>
      </div>


      {/* Analytics Charts Grid */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* 14-Day Trend Chart */}
        <div className="lg:col-span-2 bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <div className="flex items-center justify-between mb-4">
            <div>
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                14-Day Incident Volume & Resolution Velocity
              </h3>
              <p className="text-[11px] text-slate-500">Comparing incoming incidents vs tickets resolved</p>
            </div>
            <div className="flex items-center gap-4 text-xs">
              <span className="flex items-center gap-1.5 text-indigo-600 font-semibold">
                <span className="w-2.5 h-2.5 rounded-full bg-indigo-600"></span> Created
              </span>
              <span className="flex items-center gap-1.5 text-emerald-600 font-semibold">
                <span className="w-2.5 h-2.5 rounded-full bg-emerald-500"></span> Resolved
              </span>
            </div>
          </div>
          <div className="h-64 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <AreaChart data={summary?.daily_trend || []} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <defs>
                  <linearGradient id="colorCreated" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#6366f1" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#6366f1" stopOpacity={0} />
                  </linearGradient>
                  <linearGradient id="colorResolved" x1="0" y1="0" x2="0" y2="1">
                    <stop offset="5%" stopColor="#10b981" stopOpacity={0.3} />
                    <stop offset="95%" stopColor="#10b981" stopOpacity={0} />
                  </linearGradient>
                </defs>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="date" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#ffffff',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '12px',
                  }}
                />
                <Area
                  type="monotone"
                  dataKey="created"
                  stroke="#6366f1"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorCreated)"
                />
                <Area
                  type="monotone"
                  dataKey="resolved"
                  stroke="#10b981"
                  strokeWidth={2}
                  fillOpacity={1}
                  fill="url(#colorResolved)"
                />
              </AreaChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Priority Distribution Chart */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs flex flex-col justify-between">
          <div>
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-1">
              Priority Distribution
            </h3>
            <p className="text-[11px] text-slate-500 mb-4">Calculated from Impact & Urgency matrix</p>
          </div>
          <div className="h-56 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <PieChart>
                <Pie
                  data={priorityChartData}
                  cx="50%"
                  cy="50%"
                  innerRadius={50}
                  outerRadius={75}
                  paddingAngle={4}
                  dataKey="count"
                >
                  {priorityChartData.map((entry) => (
                    <Cell
                      key={`cell-${entry.name}`}
                      fill={PRIORITY_COLORS[entry.name] || '#64748b'}
                    />
                  ))}
                </Pie>
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#ffffff',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '12px',
                  }}
                />
                <Legend
                  verticalAlign="bottom"
                  iconType="circle"
                  formatter={(val) => <span className="text-xs text-slate-700">{val}</span>}
                />
              </PieChart>
            </ResponsiveContainer>
          </div>
        </div>
      </div>

      {/* Second Row: Status Volume & SLA Attention Queue */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Status Distribution Bar Chart */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-1">
            Incidents by Lifecycle Status
          </h3>
          <p className="text-[11px] text-slate-500 mb-4">Workload breakdown across support stages</p>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={statusChartData} margin={{ top: 10, right: 10, left: -20, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" vertical={false} stroke="#f1f5f9" />
                <XAxis dataKey="name" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#ffffff',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '12px',
                  }}
                />
                <Bar dataKey="count" fill="#4f46e5" radius={[4, 4, 0, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* SLA Attention / Breached Queue Table */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden flex flex-col justify-between">
          <div className="p-4 border-b border-slate-200 flex items-center justify-between">
            <div className="flex items-center gap-2">
              <ShieldAlert className="w-4 h-4 text-red-600" />
              <div>
                <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
                  SLA Priority Attention Queue
                </h3>
                <p className="text-[11px] text-slate-500">Incidents exceeding resolution targets</p>
              </div>
            </div>
            <Link
              to="/tickets?sla_status=Breached"
              className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
            >
              View Queue <ArrowRight className="w-3.5 h-3.5" />
            </Link>
          </div>

          <div className="overflow-x-auto flex-1">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-2.5 px-3">Ticket</th>
                  <th className="py-2.5 px-3">Title</th>
                  <th className="py-2.5 px-3">Requester</th>
                  <th className="py-2.5 px-3">Priority</th>
                  <th className="py-2.5 px-3">SLA Status</th>
                  <th className="py-2.5 px-3">Action</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {myTickets.slice(0, 5).map((t) => (
                  <tr key={t.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-2.5 px-3 font-mono font-bold text-indigo-600">
                      <Link to={`/tickets/${t.id}`}>{t.ticket_number}</Link>
                    </td>
                    <td className="py-2.5 px-3 max-w-[200px] truncate text-slate-800 font-medium">
                      {t.title}
                    </td>
                    <td className="py-2.5 px-3 text-slate-600">{t.creator_name}</td>
                    <td className="py-2.5 px-3">
                      <PriorityBadge priority={t.priority} />
                    </td>
                    <td className="py-2.5 px-3">
                      <SLABadge status={t.sla_status} />
                    </td>
                    <td className="py-2.5 px-3">
                      <Link
                        to={`/tickets/${t.id}`}
                        className="px-2.5 py-1 rounded bg-indigo-50 hover:bg-indigo-100 text-indigo-700 font-medium text-[11px] transition"
                      >
                        Investigate
                      </Link>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </div>
      </div>

      {/* Third Row: Service Request Categories & Active Orders Attention */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Service Requests by Category Chart */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
          <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-1">
            Service Requests by Catalog Category
          </h3>
          <p className="text-[11px] text-slate-500 mb-4">Volume breakdown across hardware, software & access</p>
          <div className="h-60 w-full">
            <ResponsiveContainer width="100%" height="100%">
              <BarChart data={srCategoryChartData} layout="vertical" margin={{ top: 5, right: 15, left: 40, bottom: 0 }}>
                <CartesianGrid strokeDasharray="3 3" horizontal={false} stroke="#f1f5f9" />
                <XAxis type="number" tick={{ fontSize: 10, fill: '#64748b' }} axisLine={false} tickLine={false} />
                <YAxis dataKey="name" type="category" tick={{ fontSize: 9, fill: '#64748b' }} axisLine={false} tickLine={false} width={80} />
                <Tooltip
                  contentStyle={{
                    backgroundColor: '#ffffff',
                    borderRadius: '8px',
                    border: '1px solid #e2e8f0',
                    fontSize: '12px',
                  }}
                />
                <Bar dataKey="count" fill="#6366f1" radius={[0, 4, 4, 0]} />
              </BarChart>
            </ResponsiveContainer>
          </div>
        </div>

        {/* Quick Navigation Hub to Service Desk Modules */}
        <div className="lg:col-span-2 bg-white rounded-xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between">
          <div>
            <div className="flex items-center justify-between mb-3">
              <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
                <Layers className="w-4 h-4 text-indigo-600" />
                Service Desk Module Navigation & Quick Filters
              </h3>
              <Link to="/service-desk" className="text-xs font-semibold text-indigo-600 hover:underline">
                Hub Landing Page &rarr;
              </Link>
            </div>
            <p className="text-xs text-slate-500 mb-4">
              Direct access to dedicated ITSM queues, approval authorization workflows, and knowledge guides:
            </p>

            <div className="grid grid-cols-1 sm:grid-cols-3 gap-3">
              <Link
                to="/service-requests?approval_status=Pending"
                className="p-3.5 rounded-xl border border-amber-200 bg-amber-50/40 hover:bg-amber-50 transition flex flex-col justify-between group"
              >
                <div>
                  <span className="text-[10px] font-bold text-amber-800 uppercase tracking-wider">Approvals</span>
                  <div className="text-xs font-bold text-amber-950 mt-1 group-hover:text-amber-700">
                    Pending Approvals ({srSummary?.pending_approval || 0})
                  </div>
                </div>
                <span className="text-[11px] text-amber-800 font-semibold mt-2 flex items-center gap-1">
                  Review & Authorize &rarr;
                </span>
              </Link>

              <Link
                to="/service-requests?status=In+Progress"
                className="p-3.5 rounded-xl border border-indigo-200 bg-indigo-50/40 hover:bg-indigo-50 transition flex flex-col justify-between group"
              >
                <div>
                  <span className="text-[10px] font-bold text-indigo-800 uppercase tracking-wider">Fulfillment</span>
                  <div className="text-xs font-bold text-indigo-950 mt-1 group-hover:text-indigo-700">
                    In Progress Orders ({srSummary?.in_progress || 0})
                  </div>
                </div>
                <span className="text-[11px] text-indigo-800 font-semibold mt-2 flex items-center gap-1">
                  Active Provisioning &rarr;
                </span>
              </Link>

              <Link
                to="/service-desk/all"
                className="p-3.5 rounded-xl border border-slate-200 bg-slate-50/70 hover:bg-slate-100 transition flex flex-col justify-between group"
              >
                <div>
                  <span className="text-[10px] font-bold text-slate-600 uppercase tracking-wider">Unified Queue</span>
                  <div className="text-xs font-bold text-slate-900 mt-1 group-hover:text-indigo-600">
                    Incidents + Requests
                  </div>
                </div>
                <span className="text-[11px] text-slate-600 font-semibold mt-2 flex items-center gap-1">
                  Consolidated View &rarr;
                </span>
              </Link>
            </div>
          </div>

          <div className="pt-4 mt-4 border-t border-slate-100 flex items-center justify-between text-xs text-slate-500">
            <span>ITIL Alignment: Service Requests are separated from Break/Fix Incidents.</span>
            <Link to="/service-requests/create" className="text-indigo-600 font-bold hover:underline">
              + New Service Order
            </Link>
          </div>
        </div>
      </div>
    </div>
  );
};
