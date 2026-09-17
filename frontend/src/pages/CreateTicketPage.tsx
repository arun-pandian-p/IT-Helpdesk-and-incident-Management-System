import React, { useEffect, useState } from 'react';
import { useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ticketService, categoryService, assetService } from '../services/api';
import { Asset, TicketCategory } from '../types';
import {
  LifeBuoy,
  Laptop,
  AlertCircle,
  HelpCircle,
  Sparkles,
  ArrowRight,
  ShieldCheck,
  Building,
  Monitor,
  CheckCircle2,
} from 'lucide-react';

export const CreateTicketPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();

  const [categories, setCategories] = useState<TicketCategory[]>([]);
  const [userAssets, setUserAssets] = useState<Asset[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  // Form State
  const [title, setTitle] = useState('');
  const [categoryId, setCategoryId] = useState('');
  const [description, setDescription] = useState('');
  const [deviceType, setDeviceType] = useState('Laptop');
  const [assetId, setAssetId] = useState('');
  const [osName, setOsName] = useState('Windows 11');
  const [applicationName, setApplicationName] = useState('Outlook');
  const [location, setLocation] = useState(user?.location || 'Headquarters');
  const [impact, setImpact] = useState('Individual');
  const [urgency, setUrgency] = useState('Medium');

  useEffect(() => {
    categoryService.getCategories().then((cats) => {
      setCategories(cats);
      if (cats.length > 0) setCategoryId(cats[0].id);
    });
    assetService.getMyDevices().then((devices) => {
      setUserAssets(devices);
      if (devices.length > 0) {
        setAssetId(devices[0].id);
        setDeviceType(devices[0].device_type);
      }
    });
  }, []);

  // Compute suggested priority from impact and urgency
  const calculateSuggestedPriority = (imp: string, urg: string): { priority: string; hours: number } => {
    if (imp === 'Company-wide' && (urg === 'Critical' || urg === 'High')) {
      return { priority: 'Critical', hours: 4 };
    }
    if (imp === 'Multiple Departments' && urg === 'Critical') {
      return { priority: 'Critical', hours: 4 };
    }
    if (imp === 'Department' && urg === 'Critical') {
      return { priority: 'High', hours: 24 };
    }
    if (imp === 'Company-wide' || urg === 'Critical' || urg === 'High') {
      return { priority: 'High', hours: 24 };
    }
    if (urg === 'Medium' || imp === 'Department' || imp === 'Team') {
      return { priority: 'Medium', hours: 48 };
    }
    return { priority: 'Low', hours: 72 };
  };

  const calculated = calculateSuggestedPriority(impact, urgency);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setError(null);

    if (!title.trim() || !description.trim()) {
      setError('Please provide a title and detailed problem description.');
      return;
    }

    setLoading(true);
    try {
      const created = await ticketService.createTicket({
        title: title.trim(),
        description: description.trim(),
        category_id: categoryId,
        impact,
        urgency,
        priority: calculated.priority,
        asset_id: assetId || undefined,
        device_type: deviceType,
        os_name: osName,
        application_name: applicationName,
        location,
      });
      navigate(`/tickets/${created.id}`);
    } catch (err: any) {
      setError(err.response?.data?.detail || 'Failed to submit incident ticket. Please verify inputs.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="max-w-4xl mx-auto space-y-6">
      <div>
        <h1 className="text-xl font-bold text-slate-900 tracking-tight">
          Report a Technical Problem or Incident
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Provide complete hardware, operating system, and application context to expedite resolution.
        </p>
      </div>

      {error && (
        <div className="p-3 bg-red-50 border border-red-200 rounded-lg flex items-center gap-2 text-xs text-red-700">
          <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
          <span>{error}</span>
        </div>
      )}

      <form onSubmit={handleSubmit} className="space-y-6">
        {/* Section 1: Problem Summary */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
            <LifeBuoy className="w-4 h-4 text-indigo-600" />
            1. What problem are you experiencing?
          </h2>

          <div>
            <label className="block text-xs font-semibold text-slate-700 mb-1">
              Incident Summary / Subject <span className="text-red-500">*</span>
            </label>
            <input
              type="text"
              required
              value={title}
              onChange={(e) => setTitle(e.target.value)}
              placeholder="e.g. Unable to connect to company VPN after credential change"
              className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
            />
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Incident Category <span className="text-red-500">*</span>
              </label>
              <select
                value={categoryId}
                onChange={(e) => setCategoryId(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                {categories.map((c) => (
                  <option key={c.id} value={c.id}>
                    {c.name}
                  </option>
                ))}
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Office / Work Location
              </label>
              <input
                type="text"
                value={location}
                onChange={(e) => setLocation(e.target.value)}
                placeholder="e.g. Headquarters - Floor 3, Remote Home Office"
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
              />
            </div>
          </div>
        </div>

        {/* Section 2: Environment & Device Details */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
            <Laptop className="w-4 h-4 text-indigo-600" />
            2. Device & Technical Environment Context
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-4">
            {/* Device Type */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Affected Device</label>
              <select
                value={deviceType}
                onChange={(e) => setDeviceType(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="Laptop">Laptop Workstation</option>
                <option value="Desktop">Desktop PC</option>
                <option value="Monitor">External Monitor / Display</option>
                <option value="Printer">Office Printer / Copier</option>
                <option value="Docking Station">Thunderbolt Dock</option>
                <option value="Mobile Phone">Mobile Phone (Corporate)</option>
                <option value="Tablet">iPad / Android Tablet</option>
                <option value="Network Device">Network / Wi-Fi Access Point</option>
                <option value="Other">Other Hardware</option>
              </select>
            </div>

            {/* Operating System */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Operating System</label>
              <select
                value={osName}
                onChange={(e) => setOsName(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="Windows 11">Windows 11 Enterprise</option>
                <option value="Windows 10">Windows 10 Enterprise</option>
                <option value="macOS Sonoma / Ventura">macOS (Apple Mac)</option>
                <option value="iOS">Apple iOS (iPhone / iPad)</option>
                <option value="Android">Android Enterprise</option>
                <option value="Linux">Ubuntu / Red Hat Linux</option>
                <option value="Other">Other OS</option>
              </select>
            </div>

            {/* Application */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Impacted Application</label>
              <select
                value={applicationName}
                onChange={(e) => setApplicationName(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="Outlook">Outlook / Email</option>
                <option value="Microsoft Teams">Microsoft Teams</option>
                <option value="OneDrive">OneDrive Sync Client</option>
                <option value="GlobalProtect VPN">GlobalProtect VPN</option>
                <option value="Gmail">Gmail</option>
                <option value="Google Drive">Google Drive Desktop</option>
                <option value="Active Directory / Login">Windows Login / MFA</option>
                <option value="Chrome / Edge Browser">Chrome / Edge Browser</option>
                <option value="Hardware Chassis / Port">Hardware Chassis / Port</option>
                <option value="Other">Other Application</option>
              </select>
            </div>
          </div>

          {/* User's allocated hardware asset link if available */}
          {userAssets.length > 0 && (
            <div className="pt-2">
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Link to Registered Corporate Asset
              </label>
              <select
                value={assetId}
                onChange={(e) => setAssetId(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="">No specific asset / Not listed</option>
                {userAssets.map((a) => (
                  <option key={a.id} value={a.id}>
                    {a.asset_tag} — {a.manufacturer} {a.model} (S/N: {a.serial_number})
                  </option>
                ))}
              </select>
            </div>
          )}
        </div>

        {/* Section 3: Impact, Urgency & Priority Matrix */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
            <Sparkles className="w-4 h-4 text-indigo-600" />
            3. Business Impact & Urgency Matrix (ITIL-Inspired)
          </h2>

          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Business Impact</label>
              <select
                value={impact}
                onChange={(e) => setImpact(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="Individual">Individual (Only affects me)</option>
                <option value="Team">Team (Affects multiple colleagues in my group)</option>
                <option value="Department">Department (Entire department blocked)</option>
                <option value="Multiple Departments">Multiple Departments</option>
                <option value="Company-wide">Company-wide Outage</option>
              </select>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Urgency</label>
              <select
                value={urgency}
                onChange={(e) => setUrgency(e.target.value)}
                className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg bg-white focus:ring-2 focus:ring-indigo-500 outline-none"
              >
                <option value="Low">Low (Minor inconvenience, workaround exists)</option>
                <option value="Medium">Medium (Affects normal daily productivity)</option>
                <option value="High">High (Completely unable to perform core work)</option>
                <option value="Critical">Critical (Executive or revenue critical deadline)</option>
              </select>
            </div>
          </div>

          {/* Matrix Result Banner */}
          <div className="p-3 bg-indigo-50/70 border border-indigo-200 rounded-lg flex items-center justify-between">
            <div className="flex items-center gap-2.5">
              <ShieldCheck className="w-5 h-5 text-indigo-600" />
              <div>
                <span className="text-xs font-semibold text-slate-900">
                  Calculated Priority: <span className="text-indigo-700">{calculated.priority}</span>
                </span>
                <p className="text-[11px] text-slate-500">
                  Standard SLA Target Resolution Window: {calculated.hours} Hours
                </p>
              </div>
            </div>
            <span className="text-[11px] font-mono text-slate-500 bg-white px-2 py-1 rounded border border-indigo-100">
              {impact} × {urgency}
            </span>
          </div>
        </div>

        {/* Section 4: Detailed Description */}
        <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-3">
          <label className="block text-xs font-bold text-slate-900 uppercase tracking-wider">
            4. Detailed Symptoms, Error Messages, or Steps to Reproduce <span className="text-red-500">*</span>
          </label>
          <textarea
            required
            rows={5}
            value={description}
            onChange={(e) => setDescription(e.target.value)}
            placeholder="Please detail: What happened? What were you doing when it occurred? Include exact error dialog text or error codes if visible."
            className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none leading-relaxed font-sans"
          />
        </div>

        {/* Submit Button */}
        <div className="flex items-center justify-end gap-3 pt-2">
          <button
            type="button"
            onClick={() => navigate(-1)}
            className="px-4 py-2 border border-slate-300 rounded-lg text-xs font-semibold text-slate-700 hover:bg-slate-50 transition"
          >
            Cancel
          </button>
          <button
            type="submit"
            disabled={loading}
            className="inline-flex items-center gap-2 px-5 py-2.5 bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-semibold rounded-lg shadow-xs transition disabled:opacity-50"
          >
            {loading ? 'Submitting ticket...' : 'Submit Technical Incident'}
            <ArrowRight className="w-4 h-4" />
          </button>
        </div>
      </form>
    </div>
  );
};
