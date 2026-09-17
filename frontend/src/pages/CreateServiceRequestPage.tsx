import React, { useState, useEffect } from 'react';
import { useNavigate, useSearchParams, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { serviceRequestService, knowledgeService, assetService } from '../services/api';
import { ServiceRequestType, KnowledgeArticle, Asset, TicketPriority } from '../types';
import {
  AlertCircle,
  ArrowLeft,
  ArrowRight,
  BookOpen,
  CheckCircle2,
  Clock,
  ExternalLink,
  HelpCircle,
  Info,
  Key,
  Laptop,
  Package,
  ShieldCheck,
  Sparkles,
} from 'lucide-react';

export const CreateServiceRequestPage: React.FC = () => {
  const { user } = useAuth();
  const navigate = useNavigate();
  const [searchParams] = useSearchParams();
  const preselectedTypeId = searchParams.get('type_id');

  const [types, setTypes] = useState<ServiceRequestType[]>([]);
  const [selectedType, setSelectedType] = useState<ServiceRequestType | null>(null);
  const [userAssets, setUserAssets] = useState<Asset[]>([]);
  const [articles, setArticles] = useState<KnowledgeArticle[]>([]);
  const [loading, setLoading] = useState(true);
  const [submitting, setSubmitting] = useState(false);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);

  // Form Fields
  const [title, setTitle] = useState('');
  const [description, setDescription] = useState('');
  const [businessJustification, setBusinessJustification] = useState('');
  const [priority, setPriority] = useState<TicketPriority>('Medium');
  const [requiredDate, setRequiredDate] = useState('');
  const [assetId, setAssetId] = useState('');

  // Conditional Fields
  const [deviceType, setDeviceType] = useState('MacBook Pro 16" (Apple M3 Pro)');
  const [applicationName, setApplicationName] = useState('AWS Production Console');
  const [accessLevel, setAccessLevel] = useState('Developer / Contributor');
  const [softwareName, setSoftwareName] = useState('JetBrains All Products Pack');
  const [softwareVersion, setSoftwareVersion] = useState('Latest LTS');

  useEffect(() => {
    const loadInitialData = async () => {
      try {
        setLoading(true);
        const [typesData, kbData, assetsData] = await Promise.all([
          serviceRequestService.getRequestTypes(),
          knowledgeService.getArticles(),
          assetService.getAssets({ user_id: user?.id }),
        ]);
        setTypes(typesData);
        setArticles(kbData.slice(0, 3));
        setUserAssets(assetsData);

        if (preselectedTypeId) {
          const match = typesData.find((t) => t.id === preselectedTypeId);
          if (match) {
            applySelectedType(match);
          }
        } else if (typesData.length > 0) {
          applySelectedType(typesData[0]);
        }
      } catch (err) {
        console.error('Failed to load request types:', err);
        setErrorMessage('Unable to load service catalog types. Please refresh the page.');
      } finally {
        setLoading(false);
      }
    };
    loadInitialData();
  }, [preselectedTypeId, user?.id]);

  const applySelectedType = (type: ServiceRequestType) => {
    setSelectedType(type);
    setTitle(type.name);
    setDescription(type.description);
    setPriority(type.default_priority);

    // Initialize reasonable defaults for conditional categories
    if (type.category === 'Hardware') {
      setDeviceType('MacBook Pro 16" (Apple M3 Pro)');
    } else if (type.category === 'Access & Identity') {
      if (type.name.toLowerCase().includes('aws')) {
        setApplicationName('AWS Cloud Infrastructure');
      } else if (type.name.toLowerCase().includes('github')) {
        setApplicationName('GitHub Enterprise');
      } else {
        setApplicationName('Salesforce CRM');
      }
      setAccessLevel('Developer / Contributor');
    } else if (type.category === 'Software & Licenses') {
      if (type.name.toLowerCase().includes('figma')) {
        setSoftwareName('Figma Enterprise');
      } else if (type.name.toLowerCase().includes('jetbrains')) {
        setSoftwareName('JetBrains All Products Pack');
      } else {
        setSoftwareName('Adobe Creative Cloud');
      }
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!selectedType) return;
    if (!businessJustification.trim()) {
      setErrorMessage('Business justification is required for service request fulfillment.');
      return;
    }

    try {
      setSubmitting(true);
      setErrorMessage(null);

      const payload: Record<string, any> = {
        request_type_id: selectedType.id,
        title,
        description,
        business_justification: businessJustification,
        priority,
        required_date: requiredDate ? new Date(requiredDate).toISOString() : undefined,
        asset_id: assetId || undefined,
        device_type: selectedType.category === 'Hardware' ? deviceType : undefined,
        application_name: selectedType.category === 'Access & Identity' ? applicationName : undefined,
        access_level: selectedType.category === 'Access & Identity' ? accessLevel : undefined,
        software_name: selectedType.category === 'Software & Licenses' ? softwareName : undefined,
        software_version: selectedType.category === 'Software & Licenses' ? softwareVersion : undefined,
      };

      const created = await serviceRequestService.createServiceRequest(payload);
      navigate(`/service-requests/${created.id}`);
    } catch (err: any) {
      console.error('Failed to create service request:', err);
      const detail = err.response?.data?.detail;
      setErrorMessage(typeof detail === 'string' ? detail : 'Failed to submit service request. Please check required fields.');
    } finally {
      setSubmitting(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  return (
    <div className="max-w-5xl mx-auto space-y-6 pb-12">
      {/* Back Navigation */}
      <div className="flex items-center justify-between">
        <button
          type="button"
          onClick={() => navigate('/service-desk')}
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Service Desk Hub
        </button>
        <Link
          to="/tickets/new"
          className="text-xs font-medium text-rose-600 hover:text-rose-800 flex items-center gap-1"
        >
          Is something broken instead? Report an Incident <ArrowRight className="w-3.5 h-3.5" />
        </Link>
      </div>

      {/* Page Header */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs flex flex-col md:flex-row md:items-center justify-between gap-4">
        <div className="space-y-1">
          <div className="inline-flex items-center gap-1.5 px-2.5 py-0.5 rounded-full text-[11px] font-bold bg-indigo-50 text-indigo-700 border border-indigo-200">
            <Package className="w-3.5 h-3.5" /> Service Request Submission
          </div>
          <h1 className="text-2xl font-bold text-slate-900">Order Standard IT Service or Access</h1>
          <p className="text-xs text-slate-500">
            Select a service catalog offering, provide operational business justification, and submit for fulfillment.
          </p>
        </div>

        {selectedType && (
          <div className="bg-slate-50 border border-slate-200 rounded-xl p-3 text-xs space-y-1 self-start md:self-auto min-w-[200px]">
            <div className="text-[10px] font-bold uppercase tracking-wider text-slate-400">Target SLA</div>
            <div className="font-bold text-slate-800 flex items-center gap-1.5">
              <Clock className="w-4 h-4 text-indigo-600" />
              {selectedType.default_sla_hours} Business Hours
            </div>
            <div className="text-[11px] text-slate-500">
              {selectedType.approval_required ? 'Requires Manager Approval' : 'Pre-Approved Fulfillment'}
            </div>
          </div>
        )}
      </div>

      {errorMessage && (
        <div className="p-4 bg-rose-50 border border-rose-200 rounded-xl text-rose-800 text-xs flex items-center gap-2">
          <AlertCircle className="w-4 h-4 flex-shrink-0 text-rose-600" />
          <span>{errorMessage}</span>
        </div>
      )}

      {/* Form Container */}
      <form onSubmit={handleSubmit} className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Columns: Catalog Selection & Form Inputs */}
        <div className="lg:col-span-2 space-y-6">
          {/* Step 1: Catalog Offering Picker */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center">
                  1
                </span>
                Select Service Catalog Item
              </h2>
              <span className="text-[11px] text-slate-400">{types.length} Available Offerings</span>
            </div>

            <div className="grid grid-cols-1 sm:grid-cols-2 gap-2.5 max-h-60 overflow-y-auto pr-1">
              {types.map((type) => {
                const isSelected = selectedType?.id === type.id;
                return (
                  <button
                    key={type.id}
                    type="button"
                    onClick={() => applySelectedType(type)}
                    className={`p-3 rounded-xl border text-left transition flex flex-col justify-between ${
                      isSelected
                        ? 'border-indigo-600 bg-indigo-50/50 shadow-xs ring-1 ring-indigo-500'
                        : 'border-slate-200 hover:border-slate-300 hover:bg-slate-50'
                    }`}
                  >
                    <div>
                      <span className="text-[9px] font-bold tracking-wider uppercase px-1.5 py-0.5 rounded bg-white border border-slate-200 text-slate-600">
                        {type.category}
                      </span>
                      <div className={`text-xs font-bold mt-1.5 ${isSelected ? 'text-indigo-900' : 'text-slate-800'}`}>
                        {type.name}
                      </div>
                    </div>
                    <div className="text-[10px] text-slate-400 mt-2 flex items-center justify-between">
                      <span>SLA: {type.default_sla_hours}h</span>
                      {type.approval_required && (
                        <span className="text-amber-600 font-medium">Approval</span>
                      )}
                    </div>
                  </button>
                );
              })}
            </div>
          </div>

          {/* Step 2: Request Details & Conditional Fields */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-5">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <span className="w-5 h-5 rounded-full bg-indigo-600 text-white text-[10px] font-bold flex items-center justify-center">
                2
              </span>
              Specify Service Parameters
            </h2>

            {/* Password Reset Self-Service Banner */}
            {selectedType?.name.toLowerCase().includes('password') && (
              <div className="bg-sky-50 border border-sky-200 rounded-xl p-4 text-xs text-sky-900 space-y-2">
                <div className="font-bold flex items-center gap-1.5 text-sky-950">
                  <Sparkles className="w-4 h-4 text-sky-600" />
                  Self-Service Password Reset (SSPR) Available
                </div>
                <p className="text-[11px] text-sky-800 leading-relaxed">
                  You can reset your Microsoft 365 or Google Workspace account password instantly without waiting for IT Helpdesk approval.
                </p>
                <a
                  href="https://passwordreset.microsoftonline.com"
                  target="_blank"
                  rel="noreferrer"
                  className="inline-flex items-center gap-1.5 px-3 py-1.5 bg-sky-600 hover:bg-sky-700 text-white rounded-lg text-xs font-semibold transition"
                >
                  Launch Self-Service Password Reset <ExternalLink className="w-3 h-3" />
                </a>
              </div>
            )}

            {/* Conditional Fields: Hardware */}
            {selectedType?.category === 'Hardware' && (
              <div className="space-y-4 p-4 bg-slate-50 border border-slate-200 rounded-xl">
                <div className="font-semibold text-xs text-slate-800 flex items-center gap-1.5">
                  <Laptop className="w-4 h-4 text-indigo-600" />
                  Hardware Specifications
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      Standard Device Model
                    </label>
                    <select
                      value={deviceType}
                      onChange={(e) => setDeviceType(e.target.value)}
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                    >
                      <option value='MacBook Pro 16" (Apple M3 Pro)'>MacBook Pro 16" (Apple M3 Pro)</option>
                      <option value='MacBook Air 15" (Apple M3)'>MacBook Air 15" (Apple M3)</option>
                      <option value='Dell XPS 15 (Windows 11 Pro)'>Dell XPS 15 (Windows 11 Pro)</option>
                      <option value='Lenovo ThinkPad P16s (Engineering)'>Lenovo ThinkPad P16s (Engineering)</option>
                      <option value='Dell UltraSharp 27" 4K Monitor'>Dell UltraSharp 27" 4K Monitor</option>
                      <option value='CalDigit TS4 Thunderbolt Dock'>CalDigit TS4 Thunderbolt Dock</option>
                      <option value='Ergonomic Mouse & Mechanical Keyboard'>Ergonomic Mouse & Mechanical Keyboard</option>
                    </select>
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      Target / Existing Asset Tag (Optional)
                    </label>
                    <select
                      value={assetId}
                      onChange={(e) => setAssetId(e.target.value)}
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                    >
                      <option value="">-- None / New Hardware Order --</option>
                      {userAssets.map((a) => (
                        <option key={a.id} value={a.id}>
                          {a.asset_tag} - {a.model} ({a.status})
                        </option>
                      ))}
                    </select>
                  </div>
                </div>
              </div>
            )}

            {/* Conditional Fields: Access & Identity */}
            {selectedType?.category === 'Access & Identity' && (
              <div className="space-y-4 p-4 bg-slate-50 border border-slate-200 rounded-xl">
                <div className="font-semibold text-xs text-slate-800 flex items-center gap-1.5">
                  <Key className="w-4 h-4 text-indigo-600" />
                  Access & System Details
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      System / Application Name
                    </label>
                    <input
                      type="text"
                      value={applicationName}
                      onChange={(e) => setApplicationName(e.target.value)}
                      placeholder="e.g. AWS Console, GitHub Enterprise, Jira Admin"
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                      required
                    />
                  </div>

                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      Requested Role / Access Level
                    </label>
                    <select
                      value={accessLevel}
                      onChange={(e) => setAccessLevel(e.target.value)}
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                    >
                      <option value="Read-Only / Auditor">Read-Only / Auditor</option>
                      <option value="Standard User">Standard User</option>
                      <option value="Developer / Contributor">Developer / Contributor</option>
                      <option value="Elevated Engineer">Elevated Engineer</option>
                      <option value="System Administrator">System Administrator (Requires VP Approval)</option>
                    </select>
                  </div>
                </div>
              </div>
            )}

            {/* Conditional Fields: Software & Licenses */}
            {selectedType?.category === 'Software & Licenses' && (
              <div className="space-y-4 p-4 bg-slate-50 border border-slate-200 rounded-xl">
                <div className="font-semibold text-xs text-slate-800 flex items-center gap-1.5">
                  <Package className="w-4 h-4 text-indigo-600" />
                  Software License Details
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      Software Title
                    </label>
                    <input
                      type="text"
                      value={softwareName}
                      onChange={(e) => setSoftwareName(e.target.value)}
                      placeholder="e.g. JetBrains, Figma, Adobe CC"
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                      required
                    />
                  </div>
                  <div>
                    <label className="block text-[11px] font-semibold text-slate-700 mb-1">
                      Version / Subscription Tier
                    </label>
                    <input
                      type="text"
                      value={softwareVersion}
                      onChange={(e) => setSoftwareVersion(e.target.value)}
                      placeholder="e.g. Latest LTS, Enterprise License"
                      className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2 bg-white border"
                    />
                  </div>
                </div>
              </div>
            )}

            {/* Request Title */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-1">
                Request Subject / Title <span className="text-rose-500">*</span>
              </label>
              <input
                type="text"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                placeholder="Brief summary of what you are requesting"
                className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2.5 bg-white border"
                required
              />
            </div>

            {/* Business Justification */}
            <div>
              <div className="flex items-center justify-between mb-1">
                <label className="block text-xs font-semibold text-slate-800">
                  Business Justification <span className="text-rose-500">*</span>
                </label>
                <span className="text-[10px] text-slate-400">Required for approval review</span>
              </div>
              <textarea
                value={businessJustification}
                onChange={(e) => setBusinessJustification(e.target.value)}
                rows={3}
                placeholder="State the business project, team requirement, or client deliverable necessitating this request..."
                className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2.5 bg-white border"
                required
              />
            </div>

            {/* Optional Description / Special Instructions */}
            <div>
              <label className="block text-xs font-semibold text-slate-800 mb-1">
                Special Delivery Instructions or Technical Notes (Optional)
              </label>
              <textarea
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                rows={2}
                placeholder="Provide office desk location, shipping address, or specific environment setup requests..."
                className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2.5 bg-white border"
              />
            </div>

            {/* Date & Priority */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
              <div>
                <label className="block text-xs font-semibold text-slate-800 mb-1">
                  Required By Date (Optional)
                </label>
                <input
                  type="date"
                  value={requiredDate}
                  onChange={(e) => setRequiredDate(e.target.value)}
                  className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2.5 bg-white border"
                />
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-800 mb-1">
                  Priority
                </label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value as TicketPriority)}
                  className="w-full text-xs rounded-lg border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-2.5 bg-white border"
                >
                  <option value="Low">Low (Routine Order)</option>
                  <option value="Medium">Medium (Standard)</option>
                  <option value="High">High (Impacting Project Timeline)</option>
                  <option value="Critical">Critical (Executive / Blocking Onboarding)</option>
                </select>
              </div>
            </div>
          </div>

          {/* Submit Action */}
          <div className="flex items-center justify-end gap-3 pt-2">
            <button
              type="button"
              onClick={() => navigate('/service-desk')}
              className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-xs font-semibold hover:bg-slate-50 transition"
            >
              Cancel
            </button>
            <button
              type="submit"
              disabled={submitting}
              className="px-6 py-2.5 rounded-xl bg-indigo-600 hover:bg-indigo-700 text-white text-xs font-bold shadow-sm hover:shadow transition disabled:opacity-50 flex items-center gap-2"
            >
              {submitting ? (
                <>
                  <div className="w-3.5 h-3.5 border-2 border-white border-t-transparent rounded-full animate-spin"></div>
                  <span>Submitting Request...</span>
                </>
              ) : (
                <>
                  <span>Submit Service Request</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </div>

        {/* Right 1 Column: Policy, Guidelines & Recommended Articles */}
        <div className="space-y-6">
          {/* SLA & Governance Summary Card */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center gap-2 text-indigo-700 font-bold text-xs">
              <ShieldCheck className="w-4 h-4" />
              <span>Service Fulfillment Policy</span>
            </div>
            <p className="text-[11px] text-slate-600 leading-relaxed">
              Standard service requests are handled by dedicated Endpoint and Application Support engineers in adherence with Enterprise ITIL SLA agreements.
            </p>

            <div className="space-y-2 pt-2 border-t border-slate-100 text-[11px]">
              <div className="flex items-center justify-between text-slate-600">
                <span>Requester:</span>
                <span className="font-semibold text-slate-900">{user?.name}</span>
              </div>
              <div className="flex items-center justify-between text-slate-600">
                <span>Department:</span>
                <span className="font-semibold text-slate-900">{user?.department || 'Corporate'}</span>
              </div>
              <div className="flex items-center justify-between text-slate-600">
                <span>Approval:</span>
                <span className="font-semibold text-slate-900">
                  {selectedType?.approval_required ? 'Required (Manager / IT)' : 'Not Required'}
                </span>
              </div>
              <div className="flex items-center justify-between text-slate-600">
                <span>Target SLA:</span>
                <span className="font-semibold text-indigo-700">{selectedType?.default_sla_hours} hours</span>
              </div>
            </div>
          </div>

          {/* Recommended Knowledge Articles */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center gap-2 text-slate-800 font-bold text-xs">
              <BookOpen className="w-4 h-4 text-indigo-600" />
              <span>Recommended Guides</span>
            </div>
            <div className="space-y-2">
              {articles.map((art) => (
                <Link
                  key={art.id}
                  to={`/knowledge/${art.id}`}
                  className="block p-2.5 rounded-lg border border-slate-100 hover:border-slate-300 hover:bg-slate-50 transition"
                >
                  <div className="text-xs font-semibold text-slate-800 hover:text-indigo-600 line-clamp-1">
                    {art.title}
                  </div>
                  <div className="text-[10px] text-slate-400 mt-0.5">
                    {art.category} • {art.helpful_count} helpful votes
                  </div>
                </Link>
              ))}
            </div>
          </div>

          {/* Incident Callout */}
          <div className="bg-rose-50 border border-rose-200 rounded-2xl p-5 text-xs text-rose-900 space-y-2">
            <div className="font-bold flex items-center gap-1.5 text-rose-950">
              <AlertCircle className="w-4 h-4 text-rose-600" />
              Is this an operational issue?
            </div>
            <p className="text-[11px] text-rose-800 leading-relaxed">
              If your existing computer is broken, a system is offline, or email is failing, submit an Incident ticket rather than a Service Request.
            </p>
            <Link
              to="/tickets/new"
              className="inline-block text-xs font-bold text-rose-700 hover:underline pt-1"
            >
              Go to Incident Submission &rarr;
            </Link>
          </div>
        </div>
      </form>
    </div>
  );
};
