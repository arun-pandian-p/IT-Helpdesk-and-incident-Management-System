import React, { useEffect, useState } from 'react';
import { useParams, useNavigate } from 'react-router-dom';
import { useAuth } from '../hooks/useAuth';
import { ticketService, userService } from '../services/api';
import { Ticket, User } from '../types';
import { StatusBadge, PriorityBadge, SLABadge } from '../components/Badges';
import { TroubleshootingChecklist } from '../components/TroubleshootingChecklist';
import {
  Clock,
  UserCheck,
  Laptop,
  CheckCircle2,
  AlertTriangle,
  ArrowUpRight,
  MessageSquare,
  Send,
  Star,
  RotateCcw,
  ShieldCheck,
  Lock,
  Calendar,
  Building,
  User as UserIcon,
} from 'lucide-react';

export const TicketDetailPage: React.FC = () => {
  const { id } = useParams<{ id: string }>();
  const navigate = useNavigate();
  const { user, canManageTickets, isEmployee, isAdmin } = useAuth();

  const [ticket, setTicket] = useState<Ticket | null>(null);
  const [supportStaff, setSupportStaff] = useState<User[]>([]);
  const [loading, setLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [isInternalComment, setIsInternalComment] = useState(false);
  const [submittingComment, setSubmittingComment] = useState(false);

  // Modals & Action States
  const [showAssignModal, setShowAssignModal] = useState(false);
  const [selectedAssignee, setSelectedAssignee] = useState('');
  const [showStatusModal, setShowStatusModal] = useState(false);
  const [selectedStatus, setSelectedStatus] = useState('');
  const [statusReason, setStatusReason] = useState('');
  const [showResolveModal, setShowResolveModal] = useState(false);
  const [resolutionNotes, setResolutionNotes] = useState('');
  const [showEscalateModal, setShowEscalateModal] = useState(false);
  const [escalateReason, setEscalateReason] = useState('');
  const [escalateLevel, setEscalateLevel] = useState(2);
  const [showFeedbackModal, setShowFeedbackModal] = useState(false);
  const [feedbackRating, setFeedbackRating] = useState(5);
  const [feedbackComment, setFeedbackComment] = useState('');
  const [showPriorityModal, setShowPriorityModal] = useState(false);
  const [overridePriority, setOverridePriority] = useState('High');
  const [overrideReason, setOverrideReason] = useState('');

  const loadTicket = async () => {
    if (!id) return;
    try {
      const data = await ticketService.getTicket(id);
      setTicket(data);
    } catch (err) {
      console.error('Failed to load ticket', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadTicket();
    if (canManageTickets) {
      userService.getSupportStaff().then(setSupportStaff).catch(console.error);
    }
  }, [id]);

  const handleAddComment = async (e: React.FormEvent) => {
    e.preventDefault();
    if (!ticket || !commentText.trim()) return;

    setSubmittingComment(true);
    try {
      await ticketService.addComment(ticket.id, commentText.trim(), isInternalComment);
      setCommentText('');
      await loadTicket();
    } catch (err) {
      console.error('Failed to post comment', err);
    } finally {
      setSubmittingComment(false);
    }
  };

  const handleAssign = async () => {
    if (!ticket || !selectedAssignee) return;
    try {
      await ticketService.assignTicket(ticket.id, selectedAssignee);
      setShowAssignModal(false);
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleStatusChange = async () => {
    if (!ticket || !selectedStatus) return;
    try {
      await ticketService.updateStatus(ticket.id, selectedStatus, statusReason);
      setShowStatusModal(false);
      setStatusReason('');
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleResolve = async () => {
    if (!ticket || !resolutionNotes.trim()) return;
    try {
      await ticketService.resolveTicket(ticket.id, resolutionNotes.trim());
      setShowResolveModal(false);
      setResolutionNotes('');
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleClose = async () => {
    if (!ticket) return;
    try {
      await ticketService.closeTicket(ticket.id);
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleReopen = async () => {
    if (!ticket) return;
    try {
      await ticketService.reopenTicket(ticket.id);
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleEscalate = async () => {
    if (!ticket || !escalateReason.trim()) return;
    try {
      await ticketService.escalateTicket(ticket.id, escalateLevel, escalateReason.trim());
      setShowEscalateModal(false);
      setEscalateReason('');
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handlePriorityOverride = async () => {
    if (!ticket || !overrideReason.trim()) return;
    try {
      await ticketService.overridePriority(ticket.id, overridePriority, overrideReason.trim());
      setShowPriorityModal(false);
      setOverrideReason('');
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  const handleFeedbackSubmit = async () => {
    if (!ticket) return;
    try {
      await ticketService.submitFeedback(ticket.id, feedbackRating, feedbackComment);
      setShowFeedbackModal(false);
      await loadTicket();
    } catch (err) {
      console.error(err);
    }
  };

  if (loading) {
    return (
      <div className="flex items-center justify-center min-h-[400px]">
        <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
      </div>
    );
  }

  if (!ticket) {
    return (
      <div className="text-center py-20">
        <h2 className="text-sm font-bold text-slate-900">Incident Not Found</h2>
        <p className="text-xs text-slate-500 mt-1">The requested ticket does not exist or you lack permission.</p>
        <button
          onClick={() => navigate('/tickets')}
          className="mt-4 px-3 py-1.5 bg-indigo-600 text-white rounded text-xs"
        >
          Return to Incidents
        </button>
      </div>
    );
  }

  return (
    <div className="max-w-6xl mx-auto space-y-6">
      {/* Incident Header Card */}
      <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
        <div className="flex flex-col md:flex-row md:items-center justify-between gap-3 mb-2">
          <div className="flex items-center gap-2.5">
            <span className="font-mono text-base font-black text-indigo-600 tracking-tight">
              {ticket.ticket_number}
            </span>
            <StatusBadge status={ticket.status} />
            <PriorityBadge priority={ticket.priority} />
            <SLABadge status={ticket.sla_status} />
          </div>

          <div className="text-xs text-slate-500 flex items-center gap-3">
            <span>
              Reported: <strong className="text-slate-700">{new Date(ticket.created_at).toLocaleString()}</strong>
            </span>
            <span>•</span>
            <span>
              SLA Due: <strong className="text-slate-700">{new Date(ticket.due_at).toLocaleString()}</strong>
            </span>
          </div>
        </div>

        <h1 className="text-lg font-bold text-slate-900 leading-snug">{ticket.title}</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-3 gap-6">
        {/* Left Column (Main Content & Timeline) */}
        <div className="lg:col-span-2 space-y-6">
          {/* Problem Description */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">
              Problem Description & Symptoms
            </h3>
            <p className="text-xs text-slate-800 whitespace-pre-line leading-relaxed font-sans bg-slate-50/70 p-3.5 rounded-lg border border-slate-100">
              {ticket.description}
            </p>
          </div>

          {/* Technical Context & Asset Card */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-3 flex items-center gap-2">
              <Laptop className="w-4 h-4 text-indigo-600" />
              Device & Environment Context
            </h3>
            <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 text-xs">
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Device Type</span>
                <span className="font-medium text-slate-800">{ticket.device_type || 'Unspecified'}</span>
              </div>
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Operating System</span>
                <span className="font-medium text-slate-800">{ticket.os_name || 'Windows 11'}</span>
              </div>
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Application</span>
                <span className="font-medium text-slate-800">{ticket.application_name || 'Standard Client'}</span>
              </div>
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Location</span>
                <span className="font-medium text-slate-800">{ticket.location || 'Headquarters'}</span>
              </div>
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Impact Level</span>
                <span className="font-medium text-slate-800">{ticket.impact}</span>
              </div>
              <div className="p-2.5 rounded bg-slate-50 border border-slate-100">
                <span className="text-slate-400 block text-[10px] uppercase font-semibold">Urgency</span>
                <span className="font-medium text-slate-800">{ticket.urgency}</span>
              </div>
            </div>

            {ticket.asset && (
              <div className="mt-3 p-3 rounded-lg border border-indigo-100 bg-indigo-50/40 text-xs flex items-center justify-between">
                <div>
                  <span className="font-semibold text-indigo-900">
                    Linked Hardware: {ticket.asset.manufacturer} {ticket.asset.model}
                  </span>
                  <p className="text-[11px] text-indigo-700 font-mono">
                    Asset Tag: {ticket.asset.asset_tag} • Serial: {ticket.asset.serial_number}
                  </p>
                </div>
                <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-indigo-200 text-indigo-800">
                  {ticket.asset.status}
                </span>
              </div>
            )}
          </div>

          {/* Interactive Diagnostic Troubleshooting Checklist */}
          <TroubleshootingChecklist
            ticketId={ticket.id}
            category={ticket.category_name || ''}
            deviceType={ticket.device_type}
            applicationName={ticket.application_name}
            osName={ticket.os_name}
            savedState={ticket.checklist_state}
            canEdit={canManageTickets && ticket.status !== 'Closed'}
            onStateSaved={() => loadTicket()}
          />

          {/* Resolution Notes Banner (if resolved/closed) */}
          {ticket.resolution_notes && (
            <div className="bg-emerald-50/80 border border-emerald-200 p-4 rounded-xl shadow-xs">
              <div className="flex items-center gap-2 text-emerald-800 font-bold text-xs uppercase tracking-wider mb-1">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                Resolution Summary
              </div>
              <p className="text-xs text-slate-800 leading-relaxed font-sans whitespace-pre-line">
                {ticket.resolution_notes}
              </p>
              {ticket.resolved_at && (
                <span className="block mt-2 text-[10px] text-emerald-700 font-medium">
                  Resolved at {new Date(ticket.resolved_at).toLocaleString()}
                </span>
              )}
            </div>
          )}

          {/* CSAT Customer Feedback Card */}
          {ticket.feedback && (
            <div className="bg-amber-50/60 border border-amber-200 p-4 rounded-xl">
              <div className="flex items-center justify-between mb-1">
                <span className="text-xs font-bold text-amber-900 uppercase tracking-wider">
                  Employee CSAT Rating
                </span>
                <div className="flex items-center gap-0.5">
                  {[1, 2, 3, 4, 5].map((star) => (
                    <Star
                      key={star}
                      className={`w-4 h-4 ${
                        star <= ticket.feedback!.rating
                          ? 'fill-amber-400 text-amber-400'
                          : 'text-slate-300'
                      }`}
                    />
                  ))}
                </div>
              </div>
              {ticket.feedback.comments && (
                <p className="text-xs text-slate-700 italic mt-1">"{ticket.feedback.comments}"</p>
              )}
            </div>
          )}

          {/* Chronological Unified Incident Timeline */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-4">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider flex items-center gap-2">
              <Clock className="w-4 h-4 text-indigo-600" />
              Incident Audit Timeline & Communication
            </h3>

            {/* Comments & Events List */}
            <div className="space-y-3 pt-2">
              {ticket.comments && ticket.comments.length > 0 ? (
                ticket.comments.map((c) => (
                  <div
                    key={c.id}
                    className={`p-3.5 rounded-lg border text-xs leading-relaxed ${
                      c.is_internal
                        ? 'bg-amber-50/40 border-amber-200'
                        : 'bg-white border-slate-200'
                    }`}
                  >
                    <div className="flex items-center justify-between mb-1.5">
                      <div className="flex items-center gap-2">
                        <span className="font-bold text-slate-900">{c.user_name}</span>
                        <span className="text-[10px] px-1.5 py-0.2 rounded bg-slate-100 text-slate-600 font-semibold">
                          {c.user_role}
                        </span>
                        {c.is_internal && (
                          <span className="text-[10px] font-bold px-1.5 py-0.2 rounded bg-amber-100 text-amber-800 flex items-center gap-1">
                            <Lock className="w-2.5 h-2.5" /> Internal Support Note
                          </span>
                        )}
                      </div>
                      <span className="text-[10px] text-slate-400">
                        {new Date(c.created_at).toLocaleString()}
                      </span>
                    </div>
                    <p className="text-slate-800 whitespace-pre-line">{c.comment}</p>
                  </div>
                ))
              ) : (
                <p className="text-xs text-slate-400 italic py-2">No comments posted yet.</p>
              )}
            </div>

            {/* Add Comment Input */}
            {ticket.status !== 'Closed' && (
              <form onSubmit={handleAddComment} className="pt-4 border-t border-slate-100 space-y-2.5">
                <label className="block text-xs font-semibold text-slate-700">
                  {canManageTickets ? 'Post Communication or Diagnostic Note' : 'Add Information / Reply'}
                </label>
                <textarea
                  rows={3}
                  required
                  value={commentText}
                  onChange={(e) => setCommentText(e.target.value)}
                  placeholder={
                    canManageTickets
                      ? 'Type troubleshooting updates, instructions for employee, or internal technician notes...'
                      : 'Provide additional error details, screenshots info, or answers requested by IT support...'
                  }
                  className="w-full px-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
                />

                <div className="flex items-center justify-between">
                  {canManageTickets ? (
                    <label className="flex items-center gap-2 text-xs text-amber-900 cursor-pointer font-medium">
                      <input
                        type="checkbox"
                        checked={isInternalComment}
                        onChange={(e) => setIsInternalComment(e.target.checked)}
                        className="rounded text-indigo-600"
                      />
                      Internal note (hidden from employee)
                    </label>
                  ) : (
                    <div></div>
                  )}

                  <button
                    type="submit"
                    disabled={submittingComment}
                    className="inline-flex items-center gap-1.5 px-4 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition disabled:opacity-50"
                  >
                    <Send className="w-3.5 h-3.5" />
                    {submittingComment ? 'Posting...' : 'Post Message'}
                  </button>
                </div>
              </form>
            )}
          </div>
        </div>

        {/* Right Column (Attributes & Actions) */}
        <div className="space-y-6">
          {/* Quick Support Actions Panel */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-3">
            <h3 className="text-xs font-bold text-slate-900 uppercase tracking-wider mb-2">
              Incident Management Actions
            </h3>

            {/* Support Engineer Actions */}
            {canManageTickets && ticket.status !== 'Closed' && (
              <div className="space-y-2">
                <button
                  onClick={() => setShowAssignModal(true)}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
                >
                  <UserCheck className="w-4 h-4 text-indigo-600" />
                  Assign / Reassign Engineer
                </button>

                <button
                  onClick={() => setShowStatusModal(true)}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
                >
                  <Clock className="w-4 h-4 text-blue-600" />
                  Transition Lifecycle Status
                </button>

                <button
                  onClick={() => setShowPriorityModal(true)}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border border-slate-200 hover:bg-slate-50 text-xs font-semibold text-slate-700 transition"
                >
                  <AlertTriangle className="w-4 h-4 text-amber-600" />
                  Override Priority Matrix
                </button>

                <button
                  onClick={() => setShowEscalateModal(true)}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border border-orange-200 bg-orange-50/50 hover:bg-orange-50 text-xs font-semibold text-orange-800 transition"
                >
                  <ArrowUpRight className="w-4 h-4 text-orange-600" />
                  Escalate to Tier 2 / Tier 3
                </button>

                <button
                  onClick={() => setShowResolveModal(true)}
                  className="w-full flex items-center justify-center gap-1.5 py-2.5 px-3 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-xs font-bold text-white shadow-xs transition"
                >
                  <CheckCircle2 className="w-4 h-4" />
                  Resolve Incident
                </button>
              </div>
            )}

            {/* Closure Button */}
            {ticket.status === 'Resolved' && (
              <button
                onClick={handleClose}
                className="w-full py-2 px-3 rounded-lg bg-slate-800 hover:bg-slate-900 text-xs font-semibold text-white transition"
              >
                Confirm Final Closure
              </button>
            )}

            {/* Employee Actions */}
            {isEmployee && ticket.status === 'Resolved' && (
              <div className="space-y-2 pt-2">
                <button
                  onClick={handleReopen}
                  className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg border border-amber-300 bg-amber-50 hover:bg-amber-100 text-xs font-semibold text-amber-900 transition"
                >
                  <RotateCcw className="w-4 h-4 text-amber-700" />
                  Issue Persists (Reopen Ticket)
                </button>

                {!ticket.feedback && (
                  <button
                    onClick={() => setShowFeedbackModal(true)}
                    className="w-full flex items-center justify-center gap-1.5 py-2 px-3 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white transition"
                  >
                    <Star className="w-4 h-4 fill-amber-300 text-amber-300" />
                    Rate IT Support (CSAT)
                  </button>
                )}
              </div>
            )}

            {ticket.status === 'Closed' && (
              <div className="p-2.5 rounded bg-slate-50 text-center text-xs text-slate-500 font-medium border border-slate-200">
                This incident is permanently closed.
              </div>
            )}
          </div>

          {/* Incident Properties Card */}
          <div className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs space-y-3 text-xs">
            <h3 className="font-bold text-slate-900 uppercase tracking-wider text-[11px] mb-2">
              Incident Metadata
            </h3>

            <div className="space-y-2.5 divide-y divide-slate-100">
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">Requester</span>
                <span className="font-semibold text-slate-900">{ticket.creator_name}</span>
              </div>
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">Department</span>
                <span className="text-slate-800">{ticket.creator_department}</span>
              </div>
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">Assigned Engineer</span>
                <span className="font-semibold text-indigo-600">
                  {ticket.assignee_name || 'Unassigned'}
                </span>
              </div>
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">First Response</span>
                <span className="text-slate-800">
                  {ticket.first_response_at
                    ? new Date(ticket.first_response_at).toLocaleTimeString()
                    : 'Awaiting'}
                </span>
              </div>
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">Resolution SLA Window</span>
                <span className="text-slate-800 font-mono">
                  {ticket.priority === 'Critical'
                    ? '4 Hours'
                    : ticket.priority === 'High'
                    ? '24 Hours'
                    : ticket.priority === 'Medium'
                    ? '48 Hours'
                    : '72 Hours'}
                </span>
              </div>
              <div className="pt-2 flex justify-between">
                <span className="text-slate-500">Escalation Tier</span>
                <span className="text-slate-800 font-semibold">Tier {ticket.escalation_level}</span>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: RESOLVE INCIDENT */}
      {/* ------------------------------------------------------------------- */}
      {showResolveModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <CheckCircle2 className="w-5 h-5 text-emerald-600" />
              Resolve Incident: {ticket.ticket_number}
            </h3>
            <p className="text-xs text-slate-600">
              Document root cause and resolution actions taken. Resolution notes are mandatory for ITIL incident closure.
            </p>
            <textarea
              rows={4}
              required
              value={resolutionNotes}
              onChange={(e) => setResolutionNotes(e.target.value)}
              placeholder="e.g. Reinstalled corrupted display driver and reseated Thunderbolt cable. Verified external 4K monitor outputs cleanly."
              className="w-full p-2.5 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-emerald-500 outline-none"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowResolveModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700 hover:bg-slate-50"
              >
                Cancel
              </button>
              <button
                onClick={handleResolve}
                disabled={!resolutionNotes.trim()}
                className="px-4 py-1.5 rounded-lg bg-emerald-600 hover:bg-emerald-700 text-xs font-bold text-white disabled:opacity-50"
              >
                Confirm Resolution
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: ASSIGN ENGINEER */}
      {/* ------------------------------------------------------------------- */}
      {showAssignModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Assign Support Engineer</h3>
            <select
              value={selectedAssignee}
              onChange={(e) => setSelectedAssignee(e.target.value)}
              className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
            >
              <option value="">Select Support Engineer...</option>
              {supportStaff.map((s) => (
                <option key={s.id} value={s.id}>
                  {s.name} ({s.job_title || s.role})
                </option>
              ))}
            </select>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowAssignModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleAssign}
                disabled={!selectedAssignee}
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white disabled:opacity-50"
              >
                Assign
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: CHANGE STATUS */}
      {/* ------------------------------------------------------------------- */}
      {showStatusModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Update Incident Lifecycle Status</h3>
            <select
              value={selectedStatus}
              onChange={(e) => setSelectedStatus(e.target.value)}
              className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
            >
              <option value="">Select Target Status...</option>
              <option value="In Progress">In Progress</option>
              <option value="Waiting for User">Waiting for User</option>
              <option value="Waiting for Vendor">Waiting for Vendor</option>
              <option value="Escalated">Escalated</option>
              <option value="Closed">Closed</option>
            </select>
            <textarea
              rows={2}
              value={statusReason}
              onChange={(e) => setStatusReason(e.target.value)}
              placeholder="Reason for status transition (recorded in audit history)..."
              className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowStatusModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleStatusChange}
                disabled={!selectedStatus}
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white disabled:opacity-50"
              >
                Update Status
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: ESCALATE */}
      {/* ------------------------------------------------------------------- */}
      {showEscalateModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <ArrowUpRight className="w-4 h-4 text-orange-600" />
              Escalate Incident
            </h3>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">Target Tier</label>
              <select
                value={escalateLevel}
                onChange={(e) => setEscalateLevel(parseInt(e.target.value, 10))}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
              >
                <option value={2}>Tier 2 (Senior Support / Specialist)</option>
                <option value={3}>Tier 3 (IT Infrastructure & Admin Lead)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Reason for Escalation <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={3}
                required
                value={escalateReason}
                onChange={(e) => setEscalateReason(e.target.value)}
                placeholder="Requires domain administrator permissions or network hardware replacement dispatch..."
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowEscalateModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleEscalate}
                disabled={!escalateReason.trim()}
                className="px-4 py-1.5 rounded-lg bg-orange-600 hover:bg-orange-700 text-xs font-bold text-white disabled:opacity-50"
              >
                Confirm Escalation
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: PRIORITY OVERRIDE */}
      {/* ------------------------------------------------------------------- */}
      {showPriorityModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Override Matrix Priority</h3>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">New Priority</label>
              <select
                value={overridePriority}
                onChange={(e) => setOverridePriority(e.target.value)}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
              >
                <option value="Critical">Critical (4 Hour SLA)</option>
                <option value="High">High (24 Hour SLA)</option>
                <option value="Medium">Medium (48 Hour SLA)</option>
                <option value="Low">Low (72 Hour SLA)</option>
              </select>
            </div>
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Override Justification <span className="text-red-500">*</span>
              </label>
              <textarea
                rows={3}
                required
                value={overrideReason}
                onChange={(e) => setOverrideReason(e.target.value)}
                placeholder="e.g. Executive VP on deadline or customer contract dispute..."
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowPriorityModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handlePriorityOverride}
                disabled={!overrideReason.trim()}
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white disabled:opacity-50"
              >
                Save Override
              </button>
            </div>
          </div>
        </div>
      )}

      {/* ------------------------------------------------------------------- */}
      {/* MODAL: CSAT FEEDBACK */}
      {/* ------------------------------------------------------------------- */}
      {showFeedbackModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Star className="w-5 h-5 fill-amber-400 text-amber-400" />
              Rate IT Support Experience
            </h3>
            <p className="text-xs text-slate-600">
              How satisfied are you with the technical assistance provided for this incident?
            </p>
            <div className="flex items-center justify-center gap-2 py-2">
              {[1, 2, 3, 4, 5].map((star) => (
                <button
                  key={star}
                  type="button"
                  onClick={() => setFeedbackRating(star)}
                  className="p-1 hover:scale-110 transition"
                >
                  <Star
                    className={`w-7 h-7 ${
                      star <= feedbackRating ? 'fill-amber-400 text-amber-400' : 'text-slate-300'
                    }`}
                  />
                </button>
              ))}
            </div>
            <textarea
              rows={3}
              value={feedbackComment}
              onChange={(e) => setFeedbackComment(e.target.value)}
              placeholder="Optional: How did the engineer do? Any comments on resolution speed?"
              className="w-full p-2.5 text-xs border border-slate-300 rounded-lg outline-none"
            />
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setShowFeedbackModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                onClick={handleFeedbackSubmit}
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white"
              >
                Submit CSAT Rating
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  );
};
