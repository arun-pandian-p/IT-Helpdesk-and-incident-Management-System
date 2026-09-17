import React, { useState, useEffect } from 'react';
import { useParams, useNavigate, Link } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { serviceRequestService, userService } from '../services/api';
import { ServiceRequest, User, SupportTeam } from '../types';
import { SRStatusBadge, ApprovalBadge, PriorityBadge, SLABadge, SupportTeamBadge } from '../components/Badges';
import {
  AlertCircle,
  AlertTriangle,
  ArrowLeft,
  Calendar,
  CheckCircle2,
  Clock,
  ExternalLink,
  FileText,
  Key,
  Laptop,
  Lock,
  MessageSquare,
  Package,
  RotateCcw,
  Send,
  Shield,
  ShieldAlert,
  ShieldCheck,
  Tag,
  User as UserIcon,
  UserCheck,
  Users,
  XCircle,
} from 'lucide-react';

export const ServiceRequestDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, isEmployee, isSupport, isAdmin } = useAuth();

  const [request, setRequest] = useState<ServiceRequest | null>(null);
  const [supportStaff, setSupportStaff] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [errorMessage, setErrorMessage] = useState<string | null>(null);
  const [actionSuccess, setActionSuccess] = useState<string | null>(null);

  // Modals & Actions state
  const [showApproveModal, setShowApproveModal] = useState(false);
  const [showRejectModal, setShowRejectModal] = useState(false);
  const [showFulfillModal, setShowFulfillModal] = useState(false);
  const [showConfirmModal, setShowConfirmModal] = useState(false);
  const [showReportProblemModal, setShowReportProblemModal] = useState(false);
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [showCancelModal, setShowCancelModal] = useState(false);

  // Form states for modals
  const [approvalReason, setApprovalReason] = useState('');
  const [rejectionReason, setRejectionReason] = useState('');
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [hardwareAssetTag, setHardwareAssetTag] = useState('');
  const [hardwareSerial, setHardwareSerial] = useState('');
  const [accessGrantedRole, setAccessGrantedRole] = useState('');
  const [softwareLicenseKey, setSoftwareLicenseKey] = useState('');
  const [userFeedbackNotes, setUserFeedbackNotes] = useState('');
  const [problemDescription, setProblemDescription] = useState('');
  const [cancelReason, setCancelReason] = useState('');
  const [selectedTeam, setSelectedTeam] = useState<SupportTeam>('Service Desk');
  const [selectedEngineerId, setSelectedEngineerId] = useState('');

  // Comment state
  const [newComment, setNewComment] = useState('');
  const [isInternalComment, setIsInternalComment] = useState(false);
  const [submittingComment, setSubmittingComment] = useState(false);

  const fetchRequestDetail = async () => {
    if (!id) return;
    try {
      setLoading(true);
      const data = await serviceRequestService.getServiceRequest(id);
      setRequest(data);
      if (data.assigned_team) {
        setSelectedTeam(data.assigned_team);
      }
      if (data.assigned_to) {
        setSelectedEngineerId(data.assigned_to);
      }
    } catch (err: any) {
      console.error('Failed to load service request detail:', err);
      const detail = err.response?.data?.detail;
      setErrorMessage(typeof detail === 'string' ? detail : 'Failed to load request details.');
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    fetchRequestDetail();
    if (!isEmployee || isAdmin) {
      userService.getSupportStaff().then(setSupportStaff).catch(console.error);
    }
  }, [id, isEmployee, isAdmin]);

  const showNotification = (msg: string) => {
    setActionSuccess(msg);
    setTimeout(() => setActionSuccess(null), 4000);
  };

  // --- Handlers ---
  const handleApprove = async () => {
    if (!id) return;
    try {
      const updated = await serviceRequestService.approveServiceRequest(id, approvalReason);
      setRequest(updated);
      setShowApproveModal(false);
      setApprovalReason('');
      showNotification('Service request approved successfully.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to approve request.');
    }
  };

  const handleReject = async () => {
    if (!id) return;
    if (!rejectionReason.trim()) {
      alert('Rejection reason is strictly required.');
      return;
    }
    try {
      const updated = await serviceRequestService.rejectServiceRequest(id, rejectionReason);
      setRequest(updated);
      setShowRejectModal(false);
      setRejectionReason('');
      showNotification('Service request rejected.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to reject request.');
    }
  };

  const handleAssign = async () => {
    if (!id) return;
    try {
      const updated = await serviceRequestService.assignServiceRequest(id, {
        assigned_team: selectedTeam,
        assigned_to: selectedEngineerId || undefined,
      });
      setRequest(updated);
      setShowAssignModal(false);
      showNotification('Assignment updated.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to update assignment.');
    }
  };

  const handleFulfill = async () => {
    if (!id) return;
    if (!resolutionNotes.trim()) {
      alert('Technical resolution notes are mandatory to fulfill a request.');
      return;
    }

    const details: Record<string, any> = {
      completed_at: new Date().toISOString(),
      engineer_name: user?.name,
    };

    if (request?.category === 'Hardware') {
      details.asset_tag = hardwareAssetTag || 'HW-ASSIGNED-01';
      details.serial_number = hardwareSerial || 'SN-789234X';
      details.delivery_method = 'Office Desk Handover / Courier';
    } else if (request?.category === 'Access & Identity') {
      details.granted_role = accessGrantedRole || request.access_level || 'Granted Standard';
      details.provisioned_user = request.requester_email;
      details.sso_group = 'corporate-active-directory';
    } else if (request?.category === 'Software & Licenses') {
      details.license_key = softwareLicenseKey || 'LIC-ENT-9941-PRO';
      details.deployment_method = 'MDM Automated Push';
    }

    try {
      const updated = await serviceRequestService.fulfillServiceRequest(id, {
        fulfillment_details: details,
        resolution_notes: resolutionNotes,
      });
      setRequest(updated);
      setShowFulfillModal(false);
      setResolutionNotes('');
      showNotification('Request fulfilled. Waiting for customer confirmation.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to fulfill request.');
    }
  };

  const handleConfirm = async () => {
    if (!id) return;
    try {
      const updated = await serviceRequestService.confirmServiceRequest(id, userFeedbackNotes);
      setRequest(updated);
      setShowConfirmModal(false);
      setUserFeedbackNotes('');
      showNotification('Thank you! Service request is now closed.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to confirm request completion.');
    }
  };

  const handleReportProblem = async () => {
    if (!id) return;
    if (!problemDescription.trim()) {
      alert('Please describe the problem you are experiencing with this fulfilled service.');
      return;
    }
    try {
      const updated = await serviceRequestService.reportProblem(id, problemDescription);
      setRequest(updated);
      setShowReportProblemModal(false);
      setProblemDescription('');
      showNotification('Problem reported. The request has been returned to In Progress.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to report problem.');
    }
  };

  const handleCancel = async () => {
    if (!id) return;
    try {
      const updated = await serviceRequestService.cancelServiceRequest(id, cancelReason);
      setRequest(updated);
      setShowCancelModal(false);
      setCancelReason('');
      showNotification('Service request cancelled.');
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to cancel request.');
    }
  };

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!id || !newComment.trim()) return;

    try {
      setSubmittingComment(true);
      const added = await serviceRequestService.addComment(id, {
        comment: newComment,
        is_internal: isInternalComment,
      });
      if (request) {
        setRequest({
          ...request,
          comments: [...(request.comments || []), added],
        });
      }
      setNewComment('');
      setIsInternalComment(false);
    } catch (err: any) {
      alert(err.response?.data?.detail || 'Failed to add comment.');
    } finally {
      setSubmittingComment(false);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="animate-spin rounded-full h-8 w-8 border-b-2 border-indigo-600"></div>
      </div>
    );
  }

  if (errorMessage || !request) {
    return (
      <div className="max-w-2xl mx-auto p-6 bg-rose-50 border border-rose-200 rounded-2xl text-center space-y-3 mt-10">
        <AlertCircle className="w-8 h-8 text-rose-600 mx-auto" />
        <h2 className="text-base font-bold text-rose-900">Request Not Found or Access Denied</h2>
        <p className="text-xs text-rose-700">{errorMessage || 'Service request could not be retrieved.'}</p>
        <Link
          to="/service-requests"
          className="inline-block px-4 py-2 bg-rose-600 text-white rounded-xl text-xs font-semibold"
        >
          Return to Service Requests
        </Link>
      </div>
    );
  }

  const isRequester = user?.id === request.requester_id;
  const canApprove =
    (!isEmployee || isAdmin) &&
    request.approval_status === 'Pending' &&
    request.approval_required;
  const separationOfDutiesBlocked = canApprove && isRequester;

  return (
    <div className="max-w-7xl mx-auto space-y-6 pb-16">
      {/* Top Breadcrumb & Alerts */}
      <div className="flex items-center justify-between">
        <Link
          to="/service-requests"
          className="inline-flex items-center gap-1.5 text-xs font-semibold text-slate-500 hover:text-slate-900 transition"
        >
          <ArrowLeft className="w-4 h-4" /> Back to Service Request Queue
        </Link>
        <div className="flex items-center gap-2">
          <span className="text-[11px] font-medium text-slate-400">Target SLA Due:</span>
          <span className="text-xs font-bold text-slate-700">
            {new Date(request.due_at).toLocaleString()}
          </span>
        </div>
      </div>

      {actionSuccess && (
        <div className="p-4 bg-emerald-50 border border-emerald-200 rounded-2xl text-emerald-800 text-xs flex items-center gap-2">
          <CheckCircle2 className="w-4 h-4 text-emerald-600 flex-shrink-0" />
          <span>{actionSuccess}</span>
        </div>
      )}

      {/* Main Header Banner */}
      <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
        <div className="flex flex-col lg:flex-row lg:items-center justify-between gap-4">
          <div className="space-y-2">
            <div className="flex flex-wrap items-center gap-2">
              <span className="text-sm font-mono font-extrabold px-3 py-1 bg-indigo-50 text-indigo-700 border border-indigo-200 rounded-lg">
                {request.request_number}
              </span>
              <SRStatusBadge status={request.status} />
              <PriorityBadge priority={request.priority} />
              <ApprovalBadge status={request.approval_status} />
              <SLABadge status={request.sla_status} />
            </div>
            <h1 className="text-2xl font-bold text-slate-900">{request.title}</h1>
            <p className="text-xs text-slate-500 flex items-center gap-2">
              <span>Category: <strong className="text-slate-700">{request.category}</strong></span>
              <span>•</span>
              <span>Offering: <strong className="text-slate-700">{request.request_type_name || 'Standard Order'}</strong></span>
            </p>
          </div>

          {/* Header Action Buttons */}
          <div className="flex flex-wrap items-center gap-2">
            {/* Requester Actions when Fulfilled */}
            {request.status === 'Fulfilled' && (isRequester || isAdmin) && (
              <>
                <button
                  type="button"
                  onClick={() => setShowConfirmModal(true)}
                  className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5"
                >
                  <CheckCircle2 className="w-3.5 h-3.5" /> Confirm Service Completed
                </button>
                <button
                  type="button"
                  onClick={() => setShowReportProblemModal(true)}
                  className="px-3.5 py-2 bg-rose-50 hover:bg-rose-100 text-rose-700 border border-rose-200 rounded-xl text-xs font-semibold transition flex items-center gap-1.5"
                >
                  <AlertTriangle className="w-3.5 h-3.5" /> Report Issue
                </button>
              </>
            )}

            {/* Support / Admin Technical Actions */}
            {(!isEmployee || isAdmin) && (
              <>
                {/* Approval Actions */}
                {request.approval_status === 'Pending' && (
                  <>
                    <button
                      type="button"
                      disabled={separationOfDutiesBlocked}
                      onClick={() => setShowApproveModal(true)}
                      className="px-3.5 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-bold transition disabled:opacity-40 flex items-center gap-1.5"
                      title={separationOfDutiesBlocked ? 'Requester cannot approve own request' : 'Approve request'}
                    >
                      <CheckCircle2 className="w-3.5 h-3.5" /> Approve
                    </button>
                    <button
                      type="button"
                      disabled={separationOfDutiesBlocked}
                      onClick={() => setShowRejectModal(true)}
                      className="px-3.5 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition disabled:opacity-40 flex items-center gap-1.5"
                      title={separationOfDutiesBlocked ? 'Requester cannot reject own request' : 'Reject request'}
                    >
                      <XCircle className="w-3.5 h-3.5" /> Reject
                    </button>
                  </>
                )}

                {/* Assignment Button */}
                <button
                  type="button"
                  onClick={() => setShowAssignModal(true)}
                  className="px-3 py-2 border border-slate-300 hover:bg-slate-50 text-slate-700 rounded-xl text-xs font-semibold transition flex items-center gap-1.5"
                >
                  <UserCheck className="w-3.5 h-3.5 text-slate-500" /> Assign
                </button>

                {/* Fulfillment Button */}
                {['Approved', 'Assigned', 'In Progress'].includes(request.status) && (
                  <button
                    type="button"
                    onClick={() => setShowFulfillModal(true)}
                    className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5"
                  >
                    <Package className="w-3.5 h-3.5" /> Complete Fulfillment
                  </button>
                )}
              </>
            )}

            {/* Cancel Button (if not closed or fulfilled) */}
            {!['Fulfilled', 'Closed', 'Cancelled'].includes(request.status) && (isRequester || isAdmin) && (
              <button
                type="button"
                onClick={() => setShowCancelModal(true)}
                className="px-3 py-2 text-xs font-medium text-slate-500 hover:text-rose-600 hover:bg-rose-50 rounded-xl border border-slate-200 transition"
              >
                Cancel Request
              </button>
            )}
          </div>
        </div>

        {/* Separation of Duties Warning */}
        {separationOfDutiesBlocked && (
          <div className="p-3 bg-amber-50 border border-amber-200 rounded-xl text-xs text-amber-900 flex items-center gap-2">
            <ShieldAlert className="w-4 h-4 text-amber-600 flex-shrink-0" />
            <span>
              <strong>Separation of Duties Notice:</strong> You submitted this service request ({request.requester_name}). Corporate security policy prohibits requesters from approving their own service requests. Another IT Lead or manager must approve it.
            </span>
          </div>
        )}
      </div>

      {/* Customer Verification Banner when Status is Fulfilled */}
      {request.status === 'Fulfilled' && (
        <div className="bg-emerald-50 border-2 border-emerald-300 rounded-2xl p-6 shadow-sm space-y-4">
          <div className="flex items-start justify-between gap-4">
            <div className="space-y-1">
              <span className="inline-flex items-center gap-1.5 text-xs font-bold text-emerald-800 bg-emerald-100/80 px-2.5 py-0.5 rounded-full">
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
                Service Fulfillment Completed
              </span>
              <h3 className="text-base font-bold text-emerald-950">
                Action Required: Please verify your requested service or hardware
              </h3>
              <p className="text-xs text-emerald-800 leading-relaxed">
                The IT Support team has fulfilled your service request. Please confirm that your hardware has arrived, software license is active, or system access is working properly.
              </p>
            </div>

            <div className="flex items-center gap-2 flex-shrink-0">
              <button
                type="button"
                onClick={() => setShowConfirmModal(true)}
                className="px-4 py-2 bg-emerald-600 hover:bg-emerald-700 text-white rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5"
              >
                <CheckCircle2 className="w-4 h-4" />
                Confirm & Close
              </button>
              <button
                type="button"
                onClick={() => setShowReportProblemModal(true)}
                className="px-4 py-2 bg-white hover:bg-rose-50 text-rose-700 border border-rose-300 rounded-xl text-xs font-bold transition shadow-xs flex items-center gap-1.5"
              >
                <AlertTriangle className="w-4 h-4 text-rose-600" />
                Report Problem
              </button>
            </div>
          </div>

          {request.resolution_notes && (
            <div className="p-3.5 bg-white rounded-xl border border-emerald-200 text-xs text-slate-800">
              <span className="font-bold text-slate-900 block mb-1">Fulfillment Notes:</span>
              <p className="text-slate-700 leading-relaxed">{request.resolution_notes}</p>
            </div>
          )}
        </div>
      )}

      {/* Main Grid: Details (2 Cols) + Sidebar Metadata (1 Col) */}
      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left 2 Columns: Request Scope, Timeline, & Discussion */}
        <div className="lg:col-span-2 space-y-6">
          {/* Card 1: Core Parameters & Technical Specifications */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-5">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <FileText className="w-4 h-4 text-indigo-600" />
              Service Request Details
            </h2>

            {/* Business Justification */}
            <div className="p-4 bg-slate-50 border border-slate-200 rounded-xl space-y-1">
              <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wider">
                Business Justification
              </span>
              <p className="text-xs text-slate-800 leading-relaxed">
                {request.business_justification || 'No justification entered.'}
              </p>
            </div>

            {/* Description / Instructions */}
            {request.description && (
              <div>
                <span className="text-[11px] font-bold text-slate-600 uppercase tracking-wider block mb-1">
                  Special Delivery Instructions / Scope Notes
                </span>
                <p className="text-xs text-slate-700 bg-white p-3 rounded-lg border border-slate-100">
                  {request.description}
                </p>
              </div>
            )}

            {/* Category-Specific Metadata Cards */}
            {request.category === 'Hardware' && (
              <div className="p-4 bg-indigo-50/50 border border-indigo-100 rounded-xl space-y-3">
                <div className="text-xs font-bold text-indigo-950 flex items-center gap-1.5">
                  <Laptop className="w-4 h-4 text-indigo-600" />
                  Hardware Allocation Parameters
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <span className="text-slate-500 block text-[11px]">Requested Device Model:</span>
                    <strong className="text-slate-800">{request.device_type || 'Standard Laptop'}</strong>
                  </div>
                  {request.asset_tag && (
                    <div>
                      <span className="text-slate-500 block text-[11px]">Associated Asset Tag:</span>
                      <strong className="font-mono text-indigo-700">{request.asset_tag}</strong>
                    </div>
                  )}
                </div>
              </div>
            )}

            {request.category === 'Access & Identity' && (
              <div className="p-4 bg-purple-50/50 border border-purple-100 rounded-xl space-y-3">
                <div className="text-xs font-bold text-purple-950 flex items-center gap-1.5">
                  <Key className="w-4 h-4 text-purple-600" />
                  Identity & System Access Parameters
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <span className="text-slate-500 block text-[11px]">Target Application:</span>
                    <strong className="text-slate-800">{request.application_name || 'Enterprise System'}</strong>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[11px]">Requested Permission Level:</span>
                    <strong className="text-slate-800">{request.access_level || 'Standard User'}</strong>
                  </div>
                </div>
              </div>
            )}

            {request.category === 'Software & Licenses' && (
              <div className="p-4 bg-sky-50/50 border border-sky-100 rounded-xl space-y-3">
                <div className="text-xs font-bold text-sky-950 flex items-center gap-1.5">
                  <Package className="w-4 h-4 text-sky-600" />
                  Software License Specification
                </div>
                <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
                  <div>
                    <span className="text-slate-500 block text-[11px]">Software Product:</span>
                    <strong className="text-slate-800">{request.software_name || 'Standard License'}</strong>
                  </div>
                  <div>
                    <span className="text-slate-500 block text-[11px]">Version / Edition:</span>
                    <strong className="text-slate-800">{request.software_version || 'Latest LTS'}</strong>
                  </div>
                </div>
              </div>
            )}

            {/* Technical Fulfillment Record if present */}
            {request.fulfillment_details && (
              <div className="p-4 bg-slate-900 text-white rounded-xl space-y-2">
                <div className="flex items-center justify-between">
                  <span className="text-[11px] font-bold uppercase tracking-wider text-indigo-300">
                    Technical Fulfillment Configuration
                  </span>
                  <span className="text-[10px] text-slate-400">
                    Fulfilled: {request.fulfilled_at ? new Date(request.fulfilled_at).toLocaleString() : 'Done'}
                  </span>
                </div>
                <pre className="text-[11px] font-mono bg-slate-800 p-3 rounded-lg overflow-x-auto text-emerald-400">
                  {typeof request.fulfillment_details === 'string'
                    ? request.fulfillment_details
                    : JSON.stringify(request.fulfillment_details, null, 2)}
                </pre>
              </div>
            )}
          </div>

          {/* Card 2: Chronological Interactive Audit Timeline */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-4">
            <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-600" />
              Service Request Audit Timeline ({request.timeline?.length || 0} Events)
            </h2>

            <div className="relative pl-6 space-y-6 before:absolute before:left-2.5 before:top-2 before:bottom-2 before:w-0.5 before:bg-slate-200">
              {request.timeline && request.timeline.length > 0 ? (
                request.timeline.map((event, idx) => (
                  <div key={event.id || idx} className="relative group">
                    {/* Node marker */}
                    <div className="absolute -left-6 top-1 w-5 h-5 rounded-full bg-white border-2 border-indigo-600 flex items-center justify-center">
                      <div className="w-1.5 h-1.5 rounded-full bg-indigo-600"></div>
                    </div>

                    <div className="space-y-1">
                      <div className="flex items-center justify-between text-xs">
                        <span className="font-bold text-slate-800">{event.title}</span>
                        <span className="text-[10px] text-slate-400">
                          {new Date(event.created_at).toLocaleString()}
                        </span>
                      </div>
                      <div className="text-[11px] text-slate-600">
                        {event.description}
                      </div>
                      <div className="text-[10px] text-slate-400 font-medium">
                        By: <span className="text-slate-600">{event.actor_name || 'System'}</span>{' '}
                        {event.actor_role && <span>({event.actor_role})</span>}
                      </div>
                    </div>
                  </div>
                ))
              ) : (
                <div className="text-xs text-slate-400">No timeline events recorded yet.</div>
              )}
            </div>
          </div>

          {/* Card 3: Discussion & Public Comments / Internal IT Notes */}
          <div className="bg-white rounded-2xl p-6 border border-slate-200 shadow-xs space-y-5">
            <div className="flex items-center justify-between">
              <h2 className="text-sm font-bold text-slate-900 flex items-center gap-2">
                <MessageSquare className="w-4 h-4 text-indigo-600" />
                Communication & Support Activity
              </h2>
              <span className="text-xs text-slate-400">
                {request.comments?.length || 0} Comments
              </span>
            </div>

            {/* Comment Stream */}
            <div className="space-y-3 max-h-96 overflow-y-auto pr-1">
              {request.comments && request.comments.length > 0 ? (
                request.comments.map((c) => (
                  <div
                    key={c.id}
                    className={`p-4 rounded-xl border text-xs space-y-1.5 ${
                      c.is_internal
                        ? 'bg-amber-50/70 border-amber-200 text-amber-950'
                        : 'bg-slate-50 border-slate-200 text-slate-800'
                    }`}
                  >
                    <div className="flex items-center justify-between">
                      <div className="flex items-center gap-2 font-semibold">
                        <span>{c.user_name || 'User'}</span>
                        <span className="text-[10px] px-1.5 py-0.2 rounded bg-white/80 border text-slate-500">
                          {c.user_role || 'STAFF'}
                        </span>
                        {c.is_internal && (
                          <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-amber-200 text-amber-800 flex items-center gap-1">
                            <Lock className="w-3 h-3" /> Internal IT Note
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400">
                        {new Date(c.created_at).toLocaleString()}
                      </span>
                    </div>
                    <p className="leading-relaxed whitespace-pre-wrap">{c.comment}</p>
                  </div>
                ))
              ) : (
                <div className="text-xs text-slate-400 text-center py-6">
                  No comments yet. Start a discussion below.
                </div>
              )}
            </div>

            {/* New Comment Input */}
            <form onSubmit={handleAddComment} className="space-y-3 pt-2 border-t border-slate-100">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Add a Comment or Follow-Up Note
                </label>
                <textarea
                  value={newComment}
                  onChange={(e) => setNewComment(e.target.value)}
                  rows={3}
                  placeholder={
                    isInternalComment
                      ? 'Write a private note visible only to IT Support and Administrators...'
                      : 'Type a message to the customer or IT team...'
                  }
                  className="w-full text-xs rounded-xl border-slate-300 focus:border-indigo-500 focus:ring-indigo-500 p-3 bg-white border"
                  required
                />
              </div>

              <div className="flex items-center justify-between">
                {(!isEmployee || isAdmin) ? (
                  <label className="flex items-center gap-2 cursor-pointer text-xs font-medium text-slate-700">
                    <input
                      type="checkbox"
                      checked={isInternalComment}
                      onChange={(e) => setIsInternalComment(e.target.checked)}
                      className="rounded border-slate-300 text-amber-600 focus:ring-amber-500"
                    />
                    <span className="flex items-center gap-1">
                      <Lock className="w-3.5 h-3.5 text-amber-600" />
                      Mark as Internal Support Note (hidden from requester)
                    </span>
                  </label>
                ) : (
                  <div></div>
                )}

                <button
                  type="submit"
                  disabled={submittingComment || !newComment.trim()}
                  className="px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-xl text-xs font-bold transition disabled:opacity-40 flex items-center gap-1.5"
                >
                  <Send className="w-3.5 h-3.5" />
                  <span>Send Comment</span>
                </button>
              </div>
            </form>
          </div>
        </div>

        {/* Right 1 Column: Metadata, Approval Card, & Assignment */}
        <div className="space-y-6">
          {/* Card: Requester Profile */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <UserIcon className="w-3.5 h-3.5 text-indigo-600" /> Requester Profile
            </h3>
            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Name:</span>
                <span className="font-bold text-slate-900">{request.requester_name}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Email:</span>
                <span className="text-slate-800 font-mono text-[11px]">{request.requester_email}</span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Department:</span>
                <span className="font-semibold text-slate-800">{request.requester_department || 'General'}</span>
              </div>
            </div>
          </div>

          {/* Card: Approval Governance Card */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <ShieldCheck className="w-3.5 h-3.5 text-indigo-600" /> Approval Governance
              </h3>
              <ApprovalBadge status={request.approval_status} />
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Approval Policy:</span>
                <span className="font-semibold text-slate-800">
                  {request.approval_required ? 'Required by Policy' : 'Pre-Approved Order'}
                </span>
              </div>

              {request.approver_name && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Reviewed By:</span>
                  <span className="font-bold text-slate-900">{request.approver_name}</span>
                </div>
              )}

              {request.approved_at && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Decided At:</span>
                  <span className="text-slate-700">{new Date(request.approved_at).toLocaleString()}</span>
                </div>
              )}

              {request.approval_reason && (
                <div className="pt-2 border-t border-slate-100">
                  <span className="text-slate-500 block text-[10px] mb-1">Decision Notes:</span>
                  <p className="text-slate-700 bg-slate-50 p-2.5 rounded-lg border border-slate-200 text-[11px]">
                    {request.approval_reason}
                  </p>
                </div>
              )}
            </div>

            {/* If pending approval and user is authorized */}
            {request.approval_status === 'Pending' && (!isEmployee || isAdmin) && !separationOfDutiesBlocked && (
              <div className="pt-3 border-t border-slate-100 flex gap-2">
                <button
                  type="button"
                  onClick={() => setShowApproveModal(true)}
                  className="flex-1 py-2 bg-teal-600 hover:bg-teal-700 text-white rounded-xl text-xs font-bold transition text-center"
                >
                  Approve
                </button>
                <button
                  type="button"
                  onClick={() => setShowRejectModal(true)}
                  className="flex-1 py-2 bg-rose-600 hover:bg-rose-700 text-white rounded-xl text-xs font-bold transition text-center"
                >
                  Reject
                </button>
              </div>
            )}
          </div>

          {/* Card: Team & Engineer Assignment */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <div className="flex items-center justify-between">
              <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
                <Users className="w-3.5 h-3.5 text-indigo-600" /> Support Assignment
              </h3>
              {(!isEmployee || isAdmin) && (
                <button
                  type="button"
                  onClick={() => setShowAssignModal(true)}
                  className="text-xs text-indigo-600 hover:underline font-semibold"
                >
                  Edit
                </button>
              )}
            </div>

            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Assigned Team:</span>
                <SupportTeamBadge team={request.assigned_team} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Assigned Engineer:</span>
                <span className="font-bold text-slate-800">
                  {request.assigned_to_name || 'Unassigned'}
                </span>
              </div>
            </div>
          </div>

          {/* Card: SLA Tracking & Timestamps */}
          <div className="bg-white rounded-2xl p-5 border border-slate-200 shadow-xs space-y-3">
            <h3 className="text-xs font-bold uppercase tracking-wider text-slate-400 flex items-center gap-1.5">
              <Clock className="w-3.5 h-3.5 text-indigo-600" /> SLA & Milestone Tracking
            </h3>

            <div className="space-y-2 text-xs">
              <div className="flex items-center justify-between">
                <span className="text-slate-500">SLA Status:</span>
                <SLABadge status={request.sla_status} />
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Target Due Date:</span>
                <span className="font-semibold text-slate-800">
                  {new Date(request.due_at).toLocaleString()}
                </span>
              </div>
              <div className="flex items-center justify-between">
                <span className="text-slate-500">Created:</span>
                <span className="text-slate-700">
                  {new Date(request.created_at).toLocaleString()}
                </span>
              </div>
              {request.fulfilled_at && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Fulfilled:</span>
                  <span className="text-emerald-700 font-semibold">
                    {new Date(request.fulfilled_at).toLocaleString()}
                  </span>
                </div>
              )}
              {request.closed_at && (
                <div className="flex items-center justify-between">
                  <span className="text-slate-500">Closed:</span>
                  <span className="text-slate-800 font-semibold">
                    {new Date(request.closed_at).toLocaleString()}
                  </span>
                </div>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* ===================== MODALS ===================== */}

      {/* Modal: Approve Request */}
      {showApproveModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-teal-600" />
              Approve Service Request
            </h3>
            <p className="text-xs text-slate-500">
              Confirm authorization for <strong>{request.title}</strong> requested by {request.requester_name}.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Approval Justification Notes (Optional)
              </label>
              <textarea
                value={approvalReason}
                onChange={(e) => setApprovalReason(e.target.value)}
                rows={3}
                placeholder="e.g. Approved in accordance with FY26 hardware budget."
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowApproveModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleApprove}
                className="px-5 py-2 text-xs font-bold text-white bg-teal-600 hover:bg-teal-700 rounded-xl"
              >
                Confirm Approval
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Reject Request (Strict mandatory reason) */}
      {showRejectModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-rose-900 flex items-center gap-2">
              <XCircle className="w-5 h-5 text-rose-600" />
              Reject Service Request
            </h3>
            <p className="text-xs text-slate-500">
              A clear reason is strictly required when rejecting a service request. The requester will be notified.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Rejection Reason <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={rejectionReason}
                onChange={(e) => setRejectionReason(e.target.value)}
                rows={3}
                placeholder="State why this request cannot be fulfilled (e.g. policy restriction, redundant request, missing VP sign-off)..."
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
                required
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowRejectModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleReject}
                disabled={!rejectionReason.trim()}
                className="px-5 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl disabled:opacity-40"
              >
                Confirm Rejection
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Fulfill Request (Mandatory Technical Completion) */}
      {showFulfillModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-lg w-full p-6 space-y-4 shadow-xl border border-slate-200 max-h-[90vh] overflow-y-auto">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <Package className="w-5 h-5 text-indigo-600" />
              Technical Fulfillment Completion
            </h3>
            <p className="text-xs text-slate-500">
              Record completion metadata for <strong>{request.title}</strong>. This moves the request to <em>Fulfilled</em> and prompts requester confirmation.
            </p>

            {/* Type-Specific Fulfillment Inputs */}
            {request.category === 'Hardware' && (
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-3">
                <span className="text-[11px] font-bold text-slate-700">Hardware Allocation Record</span>
                <div className="grid grid-cols-2 gap-2">
                  <div>
                    <label className="block text-[10px] font-semibold text-slate-600 mb-1">
                      Assigned Asset Tag
                    </label>
                    <input
                      type="text"
                      value={hardwareAssetTag}
                      onChange={(e) => setHardwareAssetTag(e.target.value)}
                      placeholder="e.g. AST-MAC-2026-09"
                      className="w-full text-xs p-2 rounded-lg border border-slate-300"
                    />
                  </div>
                  <div>
                    <label className="block text-[10px] font-semibold text-slate-600 mb-1">
                      Serial Number
                    </label>
                    <input
                      type="text"
                      value={hardwareSerial}
                      onChange={(e) => setHardwareSerial(e.target.value)}
                      placeholder="e.g. C02G90123XYZ"
                      className="w-full text-xs p-2 rounded-lg border border-slate-300"
                    />
                  </div>
                </div>
              </div>
            )}

            {request.category === 'Access & Identity' && (
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <span className="text-[11px] font-bold text-slate-700">Access Provisioning Record</span>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-600 mb-1">
                    Granted System Role / Group
                  </label>
                  <input
                    type="text"
                    value={accessGrantedRole}
                    onChange={(e) => setAccessGrantedRole(e.target.value)}
                    placeholder={request.access_level || 'e.g. Read-Only / Auditor'}
                    className="w-full text-xs p-2 rounded-lg border border-slate-300"
                  />
                </div>
              </div>
            )}

            {request.category === 'Software & Licenses' && (
              <div className="p-3 bg-slate-50 rounded-xl border border-slate-200 space-y-2">
                <span className="text-[11px] font-bold text-slate-700">License Assignment Record</span>
                <div>
                  <label className="block text-[10px] font-semibold text-slate-600 mb-1">
                    License Key / Subscription ID
                  </label>
                  <input
                    type="text"
                    value={softwareLicenseKey}
                    onChange={(e) => setSoftwareLicenseKey(e.target.value)}
                    placeholder="e.g. LIC-2026-JETBRAINS-ENT"
                    className="w-full text-xs p-2 rounded-lg border border-slate-300"
                  />
                </div>
              </div>
            )}

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Technical Resolution Notes <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={resolutionNotes}
                onChange={(e) => setResolutionNotes(e.target.value)}
                rows={3}
                placeholder="Detail technical actions performed, deployment confirmation, and delivery instructions..."
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
                required
              />
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowFulfillModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleFulfill}
                disabled={!resolutionNotes.trim()}
                className="px-5 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl disabled:opacity-40"
              >
                Complete Fulfillment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Confirm Service (Requester) */}
      {showConfirmModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-emerald-900 flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              Confirm Service Completed
            </h3>
            <p className="text-xs text-slate-500">
              Confirming will officially mark this request as <strong>Closed</strong>.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Customer Feedback / Confirmation Note (Optional)
              </label>
              <textarea
                value={userFeedbackNotes}
                onChange={(e) => setUserFeedbackNotes(e.target.value)}
                rows={3}
                placeholder="e.g. Laptop received in great condition and all dev tools working!"
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowConfirmModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleConfirm}
                className="px-5 py-2 text-xs font-bold text-white bg-emerald-600 hover:bg-emerald-700 rounded-xl"
              >
                Confirm & Close Request
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Report Problem (Requester) */}
      {showReportProblemModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-rose-900 flex items-center gap-2">
              <AlertTriangle className="w-5 h-5 text-rose-600" />
              Report Issue with Fulfilled Service
            </h3>
            <p className="text-xs text-slate-500">
              Explain what is not working. The request will automatically revert to <strong>In Progress</strong> and the assigned team will be notified.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Problem Description <span className="text-rose-500">*</span>
              </label>
              <textarea
                value={problemDescription}
                onChange={(e) => setProblemDescription(e.target.value)}
                rows={3}
                placeholder="Describe what went wrong (e.g. license code invalid, monitor cable missing, permission denied on database)..."
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
                required
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowReportProblemModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleReportProblem}
                disabled={!problemDescription.trim()}
                className="px-5 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl disabled:opacity-40"
              >
                Return to In Progress
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Assign Request */}
      {showAssignModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-slate-900 flex items-center gap-2">
              <UserCheck className="w-5 h-5 text-indigo-600" />
              Assign Support Team & Engineer
            </h3>

            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Support Team
                </label>
                <select
                  value={selectedTeam}
                  onChange={(e) => setSelectedTeam(e.target.value as SupportTeam)}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-300 bg-white"
                >
                  <option value="Service Desk">Service Desk</option>
                  <option value="Endpoint Support">Endpoint Support</option>
                  <option value="Network Support">Network Support</option>
                  <option value="Application Support">Application Support</option>
                  <option value="Microsoft 365 Support">Microsoft 365 Support</option>
                  <option value="Google Workspace Support">Google Workspace Support</option>
                  <option value="Hardware Support">Hardware Support</option>
                  <option value="Security">Security</option>
                </select>
              </div>

              <div>
                <label className="block text-xs font-semibold text-slate-700 mb-1">
                  Assign Engineer
                </label>
                <select
                  value={selectedEngineerId}
                  onChange={(e) => setSelectedEngineerId(e.target.value)}
                  className="w-full text-xs p-2.5 rounded-xl border border-slate-300 bg-white"
                >
                  <option value="">-- Unassigned (Team Queue) --</option>
                  {supportStaff.map((staff) => (
                    <option key={staff.id} value={staff.id}>
                      {staff.name} ({staff.role})
                    </option>
                  ))}
                </select>
              </div>
            </div>

            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAssignModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Cancel
              </button>
              <button
                type="button"
                onClick={handleAssign}
                className="px-5 py-2 text-xs font-bold text-white bg-indigo-600 hover:bg-indigo-700 rounded-xl"
              >
                Save Assignment
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Modal: Cancel Request */}
      {showCancelModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-2xl max-w-md w-full p-6 space-y-4 shadow-xl border border-slate-200">
            <h3 className="text-base font-bold text-rose-900 flex items-center gap-2">
              <XCircle className="w-5 h-5 text-rose-600" />
              Cancel Service Request
            </h3>
            <p className="text-xs text-slate-500">
              Are you sure you want to cancel this request? This action cannot be undone.
            </p>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Reason for Cancellation
              </label>
              <textarea
                value={cancelReason}
                onChange={(e) => setCancelReason(e.target.value)}
                rows={2}
                placeholder="e.g. No longer needed, duplicate request..."
                className="w-full text-xs rounded-xl border border-slate-300 p-2.5"
              />
            </div>
            <div className="flex items-center justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCancelModal(false)}
                className="px-4 py-2 text-xs font-semibold text-slate-600 hover:bg-slate-100 rounded-xl"
              >
                Keep Request
              </button>
              <button
                type="button"
                onClick={handleCancel}
                className="px-5 py-2 text-xs font-bold text-white bg-rose-600 hover:bg-rose-700 rounded-xl"
              >
                Confirm Cancellation
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
