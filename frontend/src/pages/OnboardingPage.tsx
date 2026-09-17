import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { onboardingService } from '../services/api';
import { OnboardingRequest, OnboardingTask } from '../types';
import {
  UserCheck,
  Plus,
  Calendar,
  Building,
  CheckCircle2,
  Clock,
  Laptop,
  CheckSquare,
  Square,
  Sparkles,
} from 'lucide-react';

export const OnboardingPage: React.FC = () => {
  const { user, canManageTickets } = useAuth();
  const [requests, setRequests] = useState<OnboardingRequest[]>([]);
  const [loading, setLoading] = useState(true);

  // Task Drawer State
  const [selectedReq, setSelectedReq] = useState<OnboardingRequest | null>(null);

  // New Request Modal
  const [showCreateModal, setShowCreateModal] = useState(false);
  const [empName, setEmpName] = useState('');
  const [dept, setDept] = useState('Engineering');
  const [jobTitle, setJobTitle] = useState('');
  const [manager, setManager] = useState('');
  const [startDate, setStartDate] = useState('');
  const [deviceReq, setDeviceReq] = useState('MacBook Pro 16');

  const loadRequests = async () => {
    setLoading(true);
    try {
      const data = await onboardingService.getRequests();
      setRequests(data);
      if (selectedReq) {
        const updated = data.find((r) => r.id === selectedReq.id);
        if (updated) setSelectedReq(updated);
      }
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadRequests();
  }, []);

  const handleToggleTask = async (task: OnboardingTask) => {
    if (!canManageTickets) return;
    const nextStatus = task.status === 'Completed' ? 'Not Started' : 'Completed';
    try {
      await onboardingService.updateTask(task.id, { status: nextStatus });
      await loadRequests();
    } catch (err) {
      console.error('Failed to update task', err);
    }
  };

  const handleCreateRequest = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await onboardingService.createRequest({
        employee_name: empName,
        department: dept,
        job_title: jobTitle,
        manager_name: manager,
        start_date: startDate || new Date().toISOString().split('T')[0],
        device_requirement: deviceReq,
      });
      setShowCreateModal(false);
      setEmpName('');
      setJobTitle('');
      setManager('');
      await loadRequests();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            IT Onboarding & Day 1 Readiness Workflows
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Automated IT task orchestration, hardware provisioning, and access readiness for new hires
          </p>
        </div>

        <button
          onClick={() => setShowCreateModal(true)}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
        >
          <Plus className="w-4 h-4" />
          New Hire Onboarding Request
        </button>
      </div>

      {/* Grid of Onboarding Requests */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {requests.map((r) => {
          const isDone = r.progress_percent === 100;
          return (
            <div
              key={r.id}
              className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between hover:border-indigo-300 transition"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {r.department}
                  </span>
                  <span
                    className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                      isDone
                        ? 'bg-emerald-100 text-emerald-800'
                        : 'bg-indigo-100 text-indigo-800'
                    }`}
                  >
                    {r.status}
                  </span>
                </div>

                <h3 className="text-sm font-bold text-slate-900">{r.employee_name}</h3>
                <p className="text-xs text-slate-600 font-medium">{r.job_title}</p>
                <p className="text-[11px] text-slate-500 mt-1">Manager: {r.manager_name}</p>

                <div className="mt-3 text-[11px] text-slate-600 flex items-center gap-1.5">
                  <Calendar className="w-3.5 h-3.5 text-slate-400" />
                  <span>Start Date: {r.start_date}</span>
                </div>

                <div className="mt-1 text-[11px] text-slate-600 flex items-center gap-1.5">
                  <Laptop className="w-3.5 h-3.5 text-slate-400" />
                  <span>Hardware: {r.device_requirement}</span>
                </div>

                {/* Progress Bar */}
                <div className="mt-4">
                  <div className="flex justify-between text-[11px] font-semibold mb-1">
                    <span className="text-slate-600">Onboarding Checklist</span>
                    <span className={isDone ? 'text-emerald-600' : 'text-indigo-600'}>
                      {r.progress_percent}%
                    </span>
                  </div>
                  <div className="w-full bg-slate-100 rounded-full h-2 overflow-hidden">
                    <div
                      className={`h-2 rounded-full transition-all duration-300 ${
                        isDone ? 'bg-emerald-500' : 'bg-indigo-600'
                      }`}
                      style={{ width: `${r.progress_percent}%` }}
                    ></div>
                  </div>
                </div>
              </div>

              <div className="mt-5 pt-3 border-t border-slate-100 flex items-center justify-between">
                <span className="text-[11px] text-slate-400">
                  {r.tasks ? r.tasks.filter((t) => t.status === 'Completed').length : 0} of{' '}
                  {r.tasks ? r.tasks.length : 8} tasks complete
                </span>
                <button
                  onClick={() => setSelectedReq(r)}
                  className="text-xs font-semibold text-indigo-600 hover:text-indigo-800 transition"
                >
                  View Tasks &rarr;
                </button>
              </div>
            </div>
          );
        })}
      </div>

      {/* Task Drawer / Modal */}
      {selectedReq && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-xl w-full p-6 shadow-xl border border-slate-200 max-h-[90vh] flex flex-col">
            <div className="flex items-start justify-between border-b border-slate-200 pb-3 mb-4">
              <div>
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
                  {selectedReq.department}
                </span>
                <h3 className="text-base font-bold text-slate-900 mt-1">
                  IT Onboarding: {selectedReq.employee_name}
                </h3>
                <p className="text-xs text-slate-500">
                  {selectedReq.job_title} • Starts {selectedReq.start_date}
                </p>
              </div>
              <button
                onClick={() => setSelectedReq(null)}
                className="text-slate-400 hover:text-slate-600 text-lg font-bold"
              >
                &times;
              </button>
            </div>

            <div className="flex-1 overflow-y-auto space-y-2 pr-1">
              <div className="mb-3 p-3 bg-slate-50 rounded-lg border border-slate-200 text-xs">
                <div className="flex justify-between font-semibold text-slate-800 mb-1">
                  <span>Overall IT Provisioning Readiness</span>
                  <span>{selectedReq.progress_percent}%</span>
                </div>
                <div className="w-full bg-slate-200 rounded-full h-2 overflow-hidden">
                  <div
                    className="bg-indigo-600 h-2 rounded-full transition-all"
                    style={{ width: `${selectedReq.progress_percent}%` }}
                  ></div>
                </div>
              </div>

              {selectedReq.tasks.map((task, idx) => {
                const isCompleted = task.status === 'Completed';
                return (
                  <div
                    key={task.id}
                    onClick={() => handleToggleTask(task)}
                    className={`flex items-start gap-3 p-3 rounded-lg border text-xs transition cursor-pointer ${
                      isCompleted
                        ? 'bg-emerald-50/60 border-emerald-200 text-slate-800'
                        : 'bg-white border-slate-200 text-slate-700 hover:bg-slate-50'
                    }`}
                  >
                    <div className="mt-0.5 text-indigo-600 flex-shrink-0">
                      {isCompleted ? (
                        <CheckSquare className="w-4 h-4 text-emerald-600" />
                      ) : (
                        <Square className="w-4 h-4 text-slate-400" />
                      )}
                    </div>
                    <div className="flex-1">
                      <div className="font-semibold">{task.task_name}</div>
                      {task.notes && <p className="text-[11px] text-slate-500 mt-0.5">{task.notes}</p>}
                    </div>
                    <span
                      className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                        isCompleted
                          ? 'bg-emerald-100 text-emerald-800'
                          : 'bg-slate-100 text-slate-600'
                      }`}
                    >
                      {task.status}
                    </span>
                  </div>
                );
              })}
            </div>

            <div className="pt-4 border-t border-slate-200 mt-4 flex justify-end">
              <button
                onClick={() => setSelectedReq(null)}
                className="px-4 py-2 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-lg transition"
              >
                Close Drawer
              </button>
            </div>
          </div>
        </div>
      )}

      {/* New Hire Request Modal */}
      {showCreateModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <form onSubmit={handleCreateRequest} className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Sparkles className="w-4 h-4 text-indigo-600" />
              Submit New Hire IT Onboarding Request
            </h3>
            <p className="text-xs text-slate-500">
              Submitting triggers automatic creation of 8 standardized IT tasks (Identity, Hardware, MDM, M365, VPN).
            </p>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">New Employee Name</label>
                <input
                  type="text"
                  required
                  placeholder="e.g. Jordan Miller"
                  value={empName}
                  onChange={(e) => setEmpName(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Department</label>
                <select
                  value={dept}
                  onChange={(e) => setDept(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="Engineering">Engineering</option>
                  <option value="Finance">Finance</option>
                  <option value="Product">Product</option>
                  <option value="Sales">Sales</option>
                  <option value="Marketing">Marketing</option>
                  <option value="HR">HR</option>
                  <option value="Legal">Legal</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Job Title</label>
                <input
                  type="text"
                  required
                  placeholder="Senior Software Engineer"
                  value={jobTitle}
                  onChange={(e) => setJobTitle(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Hiring Manager</label>
                <input
                  type="text"
                  required
                  placeholder="David Miller"
                  value={manager}
                  onChange={(e) => setManager(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Start Date</label>
                <input
                  type="date"
                  required
                  value={startDate}
                  onChange={(e) => setStartDate(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Hardware Allocation</label>
                <select
                  value={deviceReq}
                  onChange={(e) => setDeviceReq(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="MacBook Pro 16">Apple MacBook Pro 16-inch M3</option>
                  <option value="Dell Latitude 5540">Dell Latitude 5540 Core i7</option>
                  <option value="ThinkPad X1 Carbon">Lenovo ThinkPad X1 Carbon</option>
                </select>
              </div>
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowCreateModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white"
              >
                Trigger Onboarding Tasks
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
