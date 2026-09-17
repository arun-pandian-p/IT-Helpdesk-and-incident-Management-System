import React, { useState, useEffect } from 'react';
import { Link, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { serviceRequestService, knowledgeService } from '../services/api';
import { ServiceRequestType, KnowledgeArticle } from '../types';
import {
  AlertTriangle,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  Clock,
  ExternalLink,
  Headphones,
  Key,
  Laptop,
  Layers,
  Monitor,
  Package,
  PlusCircle,
  Search,
  Shield,
  Sparkles,
  Ticket,
  Wrench,
} from 'lucide-react';

export const ServiceDeskLandingPage: React.FC = () => {
  const { user, isEmployee } = useAuth();
  const navigate = useNavigate();
  const [requestTypes, setRequestTypes] = useState<ServiceRequestType[]>([]);
  const [popularArticles, setPopularArticles] = useState<KnowledgeArticle[]>([]);
  const [searchQuery, setSearchQuery] = useState('');
  const [loading, setLoading] = useState(true);

  useEffect(() => {
    const fetchData = async () => {
      try {
        setLoading(true);
        const [types, articles] = await Promise.all([
          serviceRequestService.getRequestTypes(),
          knowledgeService.getArticles(),
        ]);
        setRequestTypes(types.slice(0, 6));
        setPopularArticles(articles.slice(0, 4));
      } catch (err) {
        console.error('Failed to load Service Desk portal data:', err);
      } finally {
        setLoading(false);
      }
    };
    fetchData();
  }, []);

  const handleSearchSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (!searchQuery.trim()) return;
    navigate(`/knowledge?search=${encodeURIComponent(searchQuery)}`);
  };

  return (
    <div className="max-w-7xl mx-auto space-y-8 pb-12">
      {/* Hero Welcome Banner */}
      <div className="relative overflow-hidden rounded-2xl bg-gradient-to-r from-slate-900 via-indigo-950 to-slate-900 text-white p-8 sm:p-10 shadow-lg border border-slate-800">
        <div className="relative z-10 max-w-3xl space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full text-xs font-semibold bg-indigo-500/20 text-indigo-300 border border-indigo-500/30">
            <Sparkles className="w-3.5 h-3.5 text-indigo-400" />
            Enterprise IT Service Management Hub
          </div>
          <h1 className="text-3xl sm:text-4xl font-extrabold tracking-tight text-white">
            Hello, {user?.name || 'Employee'}. How can IT assist you today?
          </h1>
          <p className="text-slate-300 text-sm sm:text-base leading-relaxed">
            Choose whether something is broken and requires technical troubleshooting, or if you need to request new hardware, software access, or standard workplace services.
          </p>

          {/* Unified Search Input */}
          <form onSubmit={handleSearchSubmit} className="pt-2 flex max-w-xl gap-2">
            <div className="relative flex-1">
              <Search className="absolute left-3.5 top-3 w-4 h-4 text-slate-400" />
              <input
                type="text"
                placeholder="Search troubleshooting guides, software catalog, or how-tos..."
                value={searchQuery}
                onChange={(e) => setSearchQuery(e.target.value)}
                className="w-full pl-10 pr-4 py-2.5 bg-white/10 hover:bg-white/15 focus:bg-white focus:text-slate-900 text-white placeholder-slate-400 text-xs sm:text-sm rounded-xl border border-white/20 focus:border-indigo-400 focus:outline-none transition"
              />
            </div>
            <button
              type="submit"
              className="px-5 py-2.5 bg-indigo-600 hover:bg-indigo-500 text-white font-semibold text-xs sm:text-sm rounded-xl transition shadow-md flex items-center gap-1.5"
            >
              Search
            </button>
          </form>
        </div>

        <div className="absolute -right-12 -bottom-12 w-80 h-80 bg-indigo-500/10 rounded-full blur-3xl pointer-events-none"></div>
      </div>

      {/* ITIL Distinction Split Cards */}
      <div>
        <div className="mb-4">
          <h2 className="text-lg font-bold text-slate-900">Choose Your Request Pathway</h2>
          <p className="text-xs text-slate-500">
            Strict ITIL distinction: incidents resolve operational failures, while service requests provide routine items and authorized access.
          </p>
        </div>

        <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
          {/* Card 1: INCIDENT MANAGEMENT */}
          <div className="bg-white rounded-2xl border-2 border-rose-200 hover:border-rose-400 p-6 sm:p-7 shadow-xs hover:shadow-md transition flex flex-col justify-between group">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-rose-50 text-rose-700 border border-rose-200">
                  <AlertTriangle className="w-3.5 h-3.5 text-rose-600" />
                  INCIDENT (Break / Fix)
                </span>
                <span className="text-[11px] font-mono text-slate-400">Prefix: INC-2026-XXXXXX</span>
              </div>

              <div>
                <h3 className="text-xl font-bold text-slate-900 group-hover:text-rose-600 transition">
                  Something is Broken or Degraded
                </h3>
                <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                  Report an unplanned interruption or reduction in IT service quality. Use this when your computer will not turn on, Wi-Fi is failing, Outlook is crashing, or a critical system is offline.
                </p>
              </div>

              <div className="bg-rose-50/50 rounded-xl p-3 text-xs text-rose-900 border border-rose-100 space-y-1.5">
                <div className="font-semibold flex items-center gap-1.5 text-rose-950">
                  <Wrench className="w-3.5 h-3.5 text-rose-600" /> Common Examples:
                </div>
                <ul className="list-disc list-inside space-y-0.5 text-rose-800 text-[11px]">
                  <li>Blue Screen of Death (BSOD) or Mac Kernel Panic</li>
                  <li>Cannot connect to Corporate VPN or Office Wi-Fi</li>
                  <li>Outlook error sending/receiving critical emails</li>
                  <li>Printer jammed or scanner offline</li>
                </ul>
              </div>
            </div>

            <div className="pt-6 mt-4 border-t border-slate-100 flex items-center justify-between gap-3">
              <Link
                to="/tickets"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900 transition"
              >
                View Incidents Queue
              </Link>
              <Link
                to="/tickets/new"
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition shadow-xs group-hover:shadow"
              >
                <span>Report Incident</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>

          {/* Card 2: SERVICE REQUEST MANAGEMENT */}
          <div className="bg-white rounded-2xl border-2 border-indigo-200 hover:border-indigo-400 p-6 sm:p-7 shadow-xs hover:shadow-md transition flex flex-col justify-between group">
            <div className="space-y-4">
              <div className="flex items-center justify-between">
                <span className="inline-flex items-center gap-1.5 px-3 py-1 rounded-full text-xs font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
                  <Package className="w-3.5 h-3.5 text-indigo-600" />
                  SERVICE REQUEST (Service Catalog)
                </span>
                <span className="text-[11px] font-mono text-slate-400">Prefix: SR-2026-XXXXXX</span>
              </div>

              <div>
                <h3 className="text-xl font-bold text-slate-900 group-hover:text-indigo-600 transition">
                  Request Standard Service or Hardware
                </h3>
                <p className="text-xs text-slate-600 mt-2 leading-relaxed">
                  Request standard business services, pre-approved software, new workstation equipment, application role access, or self-service password reset assistance.
                </p>
              </div>

              <div className="bg-indigo-50/50 rounded-xl p-3 text-xs text-indigo-900 border border-indigo-100 space-y-1.5">
                <div className="font-semibold flex items-center gap-1.5 text-indigo-950">
                  <CheckCircle2 className="w-3.5 h-3.5 text-indigo-600" /> Standard Requests:
                </div>
                <ul className="list-disc list-inside space-y-0.5 text-indigo-800 text-[11px]">
                  <li>New Developer Laptop or secondary display monitor</li>
                  <li>Figma, Adobe Creative Cloud, or JetBrains license</li>
                  <li>GitHub organization or AWS production role access</li>
                  <li>Onboarding hardware prep & standard software deployment</li>
                </ul>
              </div>
            </div>

            <div className="pt-6 mt-4 border-t border-slate-100 flex items-center justify-between gap-3">
              <Link
                to="/service-requests"
                className="text-xs font-semibold text-slate-600 hover:text-slate-900 transition"
              >
                View Request Catalog
              </Link>
              <Link
                to="/service-requests/create"
                className="inline-flex items-center gap-2 px-4 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-xs group-hover:shadow"
              >
                <span>Submit Service Request</span>
                <ArrowRight className="w-4 h-4" />
              </Link>
            </div>
          </div>
        </div>
      </div>

      {/* Featured Service Request Catalog Items */}
      <div className="space-y-4">
        <div className="flex items-center justify-between">
          <div>
            <h2 className="text-lg font-bold text-slate-900">Featured Service Catalog Items</h2>
            <p className="text-xs text-slate-500">Popular routine IT requests with predefined service level targets.</p>
          </div>
          <Link
            to="/service-requests/create"
            className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 flex items-center gap-1"
          >
            Browse Full Catalog <ArrowRight className="w-3.5 h-3.5" />
          </Link>
        </div>

        <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 gap-4">
          {requestTypes.map((type) => (
            <div
              key={type.id}
              onClick={() => navigate(`/service-requests/create?type_id=${type.id}`)}
              className="bg-white rounded-xl p-5 border border-slate-200 hover:border-indigo-300 hover:shadow-sm transition cursor-pointer flex flex-col justify-between group"
            >
              <div className="space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[10px] font-bold tracking-wider text-indigo-700 uppercase bg-indigo-50 px-2 py-0.5 rounded">
                    {type.category}
                  </span>
                  <span className="text-[11px] font-medium text-slate-400 flex items-center gap-1">
                    <Clock className="w-3 h-3 text-slate-400" />
                    SLA: {type.default_sla_hours}h
                  </span>
                </div>
                <h4 className="text-sm font-bold text-slate-800 group-hover:text-indigo-600 transition">
                  {type.name}
                </h4>
                <p className="text-xs text-slate-500 line-clamp-2">
                  {type.description}
                </p>
              </div>

              <div className="pt-4 mt-3 border-t border-slate-100 flex items-center justify-between text-xs font-semibold text-indigo-600">
                <span>{type.approval_required ? 'Approval Required' : 'Pre-Approved'}</span>
                <ArrowRight className="w-3.5 h-3.5 group-hover:translate-x-1 transition" />
              </div>
            </div>
          ))}
        </div>
      </div>

      {/* Quick Self-Service & Emergency Helpdesk Info */}
      <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
        {/* Self-Service Guides */}
        <div className="md:col-span-2 bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-2">
              <BookOpen className="w-4 h-4 text-indigo-600" />
              <h3 className="text-sm font-bold text-slate-900">Self-Service Knowledge & Quick Troubleshooting</h3>
            </div>
            <Link to="/knowledge" className="text-xs text-indigo-600 hover:underline font-semibold">
              All Guides
            </Link>
          </div>

          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            {popularArticles.map((art) => (
              <Link
                key={art.id}
                to={`/knowledge/${art.id}`}
                className="p-3.5 rounded-xl border border-slate-100 hover:border-slate-300 hover:bg-slate-50/50 transition flex flex-col justify-between"
              >
                <div>
                  <span className="text-[10px] font-semibold text-slate-400 uppercase tracking-wider">
                    {art.os_target || 'Multi-OS'} • {art.category}
                  </span>
                  <div className="text-xs font-bold text-slate-800 hover:text-indigo-600 line-clamp-1 mt-1">
                    {art.title}
                  </div>
                  <p className="text-[11px] text-slate-500 line-clamp-2 mt-1">
                    {art.content}
                  </p>
                </div>
                <div className="text-[10px] text-emerald-600 font-semibold mt-2 flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> {art.helpful_count} employees found helpful
                </div>
              </Link>
            ))}
          </div>
        </div>

        {/* Emergency Assistance Card */}
        <div className="bg-slate-900 text-white rounded-2xl p-6 shadow-sm border border-slate-800 flex flex-col justify-between space-y-4">
          <div className="space-y-3">
            <div className="w-10 h-10 rounded-xl bg-indigo-600/30 border border-indigo-500/40 flex items-center justify-center text-indigo-400">
              <Headphones className="w-5 h-5" />
            </div>
            <h3 className="text-base font-bold text-white">Emergency IT Support</h3>
            <p className="text-xs text-slate-400 leading-relaxed">
              If you have lost your corporate laptop, suspect a phishing compromise, or the office local network has completely halted:
            </p>
            <div className="bg-slate-800/80 rounded-xl p-3 border border-slate-700/50 space-y-1">
              <div className="text-[10px] font-semibold tracking-wider text-slate-400 uppercase">
                Hotline (24/7 Severity 1)
              </div>
              <div className="text-base font-mono font-bold text-indigo-300">
                Ext. 9111 • (555) 0199
              </div>
            </div>
          </div>

          <div className="pt-3 border-t border-slate-800 text-[11px] text-slate-400">
            For routine requests, please submit a <Link to="/service-requests/create" className="text-indigo-400 hover:underline">Service Request</Link>.
          </div>
        </div>
      </div>
    </div>
  );
};
