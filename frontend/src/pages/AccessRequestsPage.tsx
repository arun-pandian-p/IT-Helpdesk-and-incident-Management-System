import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { accessService } from '../services/api';
import { AccessRequest } from '../types';
import { Key, Plus, CheckCircle2, XCircle, Clock, ShieldCheck, UserCheck } from 'lucide-react';

export const AccessRequestsPage: React.FC = () => {
  const { user, canManageTickets, isEmployee } = useAuth();
  const [requests, setRequests] = useState<AccessRequest[]>([]);
  const [statusFilter, setStatusFilter] = useState('');
  const [loading, setLoading] = useState(true);

  // New Request Modal
  const [showModal, setShowModal] = useState(false);
  const [reqType, setReqType] = useState('Password Reset');
  const [systemName, setSystemName] = useState('Active Directory / Windows Domain');
  const [accessLevel, setAccessLevel] = useState('Standard User');
  const [reason, setReason] = useState('');

  const loadRequests = async () => {
    setLoading(true);
    try {
      const data = await accessService.getRequests(statusFilter || undefined);
      setRequests(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, [statusFilter]);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await accessService.createRequest({
        request_type: reqType,
        requested_system: systemName,
        access_level: accessLevel,
        reason,
      });
      setShowModal(false);
      setReason('');
      await loadRequests();
    } catch (err) {
      console.error(err);
    }
  };

  const handleStatusChange = async (id: string, newStatus: string) => {
    try {
      await accessService.updateStatus(id, newStatus);
      await loadRequests();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            {isEmployee ? 'Request IT Access & Account Management' : 'Access & Account Authorization Queue'}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {isEmployee
              ? 'Self-service requests for password resets, MFA re-registration, shared drives, and application access'
              : 'Security approvals, identity access management (IAM), and permission authorizations'}
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
        >
          <Plus className="w-4 h-4" />
          Submit Access Request
        </button>
      </div>

      {/* Service Catalog Quick Tiles */}
      <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-3">
        {[
          { title: 'Self-Service Password Reset', sys: 'Active Directory / Entra ID', icon: Key, type: 'Password Reset' },
          { title: 'MFA Device Re-enrollment', sys: 'Microsoft Authenticator', icon: ShieldCheck, type: 'MFA Reset' },
          { title: 'Department Shared Drive', sys: 'DFS Share \\\\corp\\dept', icon: UserCheck, type: 'Shared Drive Access' },
          { title: 'GlobalProtect VPN Remote Access', sys: 'GlobalProtect VPN Gateway', icon: Clock, type: 'VPN Access' },
        ].map((tile) => (
          <button
            key={tile.title}
            onClick={() => {
              setReqType(tile.type);
              setSystemName(tile.sys);
              setShowModal(true);
            }}
            className="p-3 bg-white border border-slate-200 rounded-xl hover:border-indigo-300 hover:bg-indigo-50/30 transition text-left flex items-start gap-3 shadow-xs"
          >
            <div className="p-2 rounded-lg bg-indigo-50 text-indigo-600">
              <tile.icon className="w-4 h-4" />
            </div>
            <div>
              <span className="text-xs font-bold text-slate-900 block">{tile.title}</span>
              <span className="text-[10px] text-slate-500">{tile.sys}</span>
            </div>
          </button>
        ))}
      </div>

      {/* Requests Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        <div className="p-4 border-b border-slate-200 flex items-center justify-between">
          <h2 className="text-xs font-bold text-slate-900 uppercase tracking-wider">
            {isEmployee ? 'My Submitted Access Requests' : 'All Identity & Access Service Requests'}
          </h2>
          <select
            value={statusFilter}
            onChange={(e) => setStatusFilter(e.target.value)}
            className="py-1 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 outline-none"
          >
            <option value="">All Statuses</option>
            <option value="Requested">Requested</option>
            <option value="Approved">Approved</option>
            <option value="Completed">Completed</option>
            <option value="Rejected">Rejected</option>
          </select>
        </div>

        {loading ? (
          <div className="py-20 flex justify-center">
            <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : requests.length === 0 ? (
          <div className="p-12 text-center">
            <Key className="w-8 h-8 text-slate-400 mx-auto mb-2" />
            <p className="text-xs font-semibold text-slate-800">No access requests found.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Request Type</th>
                  <th className="py-3 px-4">Target System</th>
                  <th className="py-3 px-4">Requested By</th>
                  <th className="py-3 px-4">Business Reason</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Approved By</th>
                  <th className="py-3 px-4">Date</th>
                  {canManageTickets && <th className="py-3 px-4">Actions</th>}
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {requests.map((r) => (
                  <tr key={r.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-3 px-4 font-bold text-slate-900">{r.request_type}</td>
                    <td className="py-3 px-4 font-medium text-slate-700">{r.requested_system}</td>
                    <td className="py-3 px-4 text-slate-800">{r.requester_name}</td>
                    <td className="py-3 px-4 max-w-xs truncate text-slate-600">{r.reason}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[10px] font-bold ${
                          r.status === 'Completed'
                            ? 'bg-emerald-100 text-emerald-800'
                            : r.status === 'Approved'
                            ? 'bg-blue-100 text-blue-800'
                            : r.status === 'Rejected'
                            ? 'bg-red-100 text-red-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {r.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-600">{r.approver_name || '—'}</td>
                    <td className="py-3 px-4 text-slate-500 font-mono text-[11px]">
                      {new Date(r.created_at).toLocaleDateString()}
                    </td>
                    {canManageTickets && (
                      <td className="py-3 px-4">
                        <div className="flex items-center gap-1.5">
                          {r.status === 'Requested' && (
                            <>
                              <button
                                onClick={() => handleStatusChange(r.id, 'Approved')}
                                className="p-1 text-emerald-600 hover:bg-emerald-50 rounded"
                                title="Approve Request"
                              >
                                <CheckCircle2 className="w-4 h-4" />
                              </button>
                              <button
                                onClick={() => handleStatusChange(r.id, 'Rejected')}
                                className="p-1 text-red-600 hover:bg-red-50 rounded"
                                title="Reject Request"
                              >
                                <XCircle className="w-4 h-4" />
                              </button>
                            </>
                          )}
                          {r.status === 'Approved' && (
                            <button
                              onClick={() => handleStatusChange(r.id, 'Completed')}
                              className="px-2 py-0.5 text-[10px] font-bold rounded bg-emerald-600 text-white hover:bg-emerald-700"
                            >
                              Provision & Complete
                            </button>
                          )}
                        </div>
                      </td>
                    )}
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* New Request Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <form onSubmit={handleCreate} className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Key className="w-4 h-4 text-indigo-600" />
              Submit Service Request for Access / Permissions
            </h3>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Request Type</label>
              <select
                value={reqType}
                onChange={(e) => setReqType(e.target.value)}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
              >
                <option value="Password Reset">Domain Password Reset / Unlock</option>
                <option value="MFA Reset">Microsoft Authenticator / MFA Reset</option>
                <option value="VPN Access">GlobalProtect Corporate VPN</option>
                <option value="Shared Drive Access">Departmental Network Shared Folder</option>
                <option value="Application Access">Cloud Application (Salesforce, AWS, Jira)</option>
              </select>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Target System</label>
              <input
                type="text"
                required
                value={systemName}
                onChange={(e) => setSystemName(e.target.value)}
                placeholder="e.g. \\corp\Finance, Jira Cloud, Microsoft 365"
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Business Justification</label>
              <textarea
                rows={3}
                required
                value={reason}
                onChange={(e) => setReason(e.target.value)}
                placeholder="Detail why access is required, project deadline, or previous error..."
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white"
              >
                Submit Request
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
