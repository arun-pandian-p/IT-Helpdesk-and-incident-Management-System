import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { improvementService } from '../services/api';
import { ImprovementRequest } from '../types';
import { TrendingUp, Plus, Sparkles, CheckCircle2, Clock, Lightbulb } from 'lucide-react';

export const ImprovementsPage: React.FC = () => {
  const { user, canManageTickets, isAdmin } = useAuth();
  const [initiatives, setInitiatives] = useState<ImprovementRequest[]>([]);
  const [loading, setLoading] = useState(true);

  // New Initiative Modal
  const [showModal, setShowModal] = useState(false);
  const [title, setTitle] = useState('');
  const [category, setCategory] = useState('Process');
  const [description, setDescription] = useState('');
  const [benefit, setBenefit] = useState('');
  const [priority, setPriority] = useState('Medium');

  const loadImprovements = async () => {
    setLoading(true);
    try {
      const data = await improvementService.getImprovements();
      setInitiatives(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadImprovements();
  }, []);

  const handleCreate = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await improvementService.createImprovement({
        title,
        category,
        description,
        business_benefit: benefit,
        priority,
      });
      setShowModal(false);
      setTitle('');
      setDescription('');
      setBenefit('');
      await loadImprovements();
    } catch (err) {
      console.error(err);
    }
  };

  const handleUpdateStatus = async (id: string, status: string) => {
    if (!isAdmin) return;
    try {
      await improvementService.updateImprovement(id, { status });
      await loadImprovements();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            Continuous Service & Technology Improvement (CSI)
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            ITIL-inspired continual service improvement log, automation initiatives, and support process optimization
          </p>
        </div>

        <button
          onClick={() => setShowModal(true)}
          className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
        >
          <Plus className="w-4 h-4" />
          Propose Improvement Initiative
        </button>
      </div>

      {/* Initiatives Grid */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        {initiatives.map((item) => (
          <div
            key={item.id}
            className="bg-white rounded-xl border border-slate-200 p-5 shadow-xs flex flex-col justify-between"
          >
            <div>
              <div className="flex items-center justify-between mb-2">
                <span className="text-[10px] uppercase font-bold tracking-wider px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
                  {item.category}
                </span>
                <span
                  className={`text-[10px] font-bold px-2 py-0.5 rounded ${
                    item.status === 'Implemented'
                      ? 'bg-emerald-100 text-emerald-800'
                      : item.status === 'In Progress'
                      ? 'bg-blue-100 text-blue-800'
                      : 'bg-amber-100 text-amber-800'
                  }`}
                >
                  {item.status}
                </span>
              </div>

              <h3 className="text-sm font-bold text-slate-900 leading-snug">{item.title}</h3>
              <p className="text-xs text-slate-600 mt-2 leading-relaxed">{item.description}</p>

              <div className="mt-3 p-2.5 bg-slate-50 rounded-lg border border-slate-100 text-xs">
                <span className="text-[10px] uppercase font-bold text-slate-500 block mb-0.5">
                  Business Benefit / ROI:
                </span>
                <p className="text-slate-700">{item.business_benefit}</p>
              </div>
            </div>

            <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
              <span className="text-[11px] text-slate-400">
                Priority: <strong className="text-slate-700">{item.priority}</strong>
              </span>

              {isAdmin && item.status !== 'Implemented' && (
                <button
                  onClick={() =>
                    handleUpdateStatus(
                      item.id,
                      item.status === 'Proposed' ? 'In Progress' : 'Implemented'
                    )
                  }
                  className="text-[11px] font-semibold text-indigo-600 hover:text-indigo-800"
                >
                  Advance Status &rarr;
                </button>
              )}
            </div>
          </div>
        ))}
      </div>

      {/* Propose Modal */}
      {showModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <form onSubmit={handleCreate} className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900 flex items-center gap-2">
              <Lightbulb className="w-4 h-4 text-amber-500" />
              Propose Continuous Service Improvement
            </h3>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Initiative Title</label>
              <input
                type="text"
                required
                placeholder="e.g. Automated Intune Windows Autopilot Direct-Ship Provisioning"
                value={title}
                onChange={(e) => setTitle(e.target.value)}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Category</label>
                <select
                  value={category}
                  onChange={(e) => setCategory(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="Process">Process Optimization</option>
                  <option value="Technology">Technology & Automation</option>
                  <option value="Documentation">Support Documentation</option>
                  <option value="Training">Staff Training</option>
                </select>
              </div>

              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Priority</label>
                <select
                  value={priority}
                  onChange={(e) => setPriority(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="High">High</option>
                  <option value="Medium">Medium</option>
                  <option value="Low">Low</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Detailed Description</label>
              <textarea
                rows={3}
                required
                value={description}
                onChange={(e) => setDescription(e.target.value)}
                placeholder="What is the current friction point, and what process change is proposed?"
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Expected Business Benefit</label>
              <textarea
                rows={2}
                required
                value={benefit}
                onChange={(e) => setBenefit(e.target.value)}
                placeholder="e.g. Saves 15 hours of manual laptop setup per week and deflects 25% of password tickets."
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
                Submit Initiative
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
