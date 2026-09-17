import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { knowledgeService } from '../services/api';
import { KnowledgeArticle } from '../types';
import {
  BookOpen,
  Search,
  ThumbsUp,
  ThumbsDown,
  Eye,
  Plus,
  Tag,
  CheckCircle2,
  Sparkles,
  Monitor,
  Apple,
  Smartphone,
} from 'lucide-react';

export const KnowledgeBasePage: React.FC = () => {
  const { user, canManageTickets } = useAuth();
  const [articles, setArticles] = useState<KnowledgeArticle[]>([]);
  const [search, setSearch] = useState('');
  const [osFilter, setOsFilter] = useState('All');
  const [categoryFilter, setCategoryFilter] = useState('');
  const [loading, setLoading] = useState(true);

  // Selected Article Detail Modal
  const [selectedArticle, setSelectedArticle] = useState<KnowledgeArticle | null>(null);
  const [voted, setVoted] = useState<boolean | null>(null);

  // New Article Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newTitle, setNewTitle] = useState('');
  const [newCategory, setNewCategory] = useState('Network');
  const [newOs, setNewOs] = useState('All');
  const [newContent, setNewContent] = useState('');
  const [newTags, setNewTags] = useState('');

  const loadArticles = async () => {
    setLoading(true);
    try {
      const data = await knowledgeService.getArticles({
        search: search || undefined,
        os_target: osFilter || undefined,
        category: categoryFilter || undefined,
      });
      setArticles(data);
    } catch (err) {
      console.error(err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadArticles();
  }, [search, osFilter, categoryFilter]);

  const handleOpenArticle = async (a: KnowledgeArticle) => {
    setSelectedArticle(a);
    setVoted(null);
    try {
      const fresh = await knowledgeService.getArticle(a.id);
      setSelectedArticle(fresh);
    } catch (err) {
      console.error(err);
    }
  };

  const handleVote = async (helpful: boolean) => {
    if (!selectedArticle) return;
    try {
      const updated = await knowledgeService.voteArticle(selectedArticle.id, helpful);
      setSelectedArticle(updated);
      setVoted(helpful);
    } catch (err) {
      console.error(err);
    }
  };

  const handleCreateArticle = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await knowledgeService.createArticle({
        title: newTitle,
        category: newCategory,
        os_target: newOs,
        content: newContent,
        tags: newTags,
      });
      setShowAddModal(false);
      setNewTitle('');
      setNewContent('');
      await loadArticles();
    } catch (err) {
      console.error(err);
    }
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            IT Knowledge Base & Operating System Support Guides
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Self-service resolution steps, VPN configuration, password reset guides, and macOS/Windows setup
          </p>
        </div>

        {canManageTickets && (
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <Plus className="w-4 h-4" />
            Publish Support Guide
          </button>
        )}
      </div>

      {/* Search & OS Tabs Bar */}
      <div className="bg-white p-4 rounded-xl border border-slate-200 shadow-xs space-y-3">
        <div className="flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="Search knowledge articles (e.g. VPN, Outlook sync, Wi-Fi, password reset, MFA)..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
            />
          </div>

          <select
            value={categoryFilter}
            onChange={(e) => setCategoryFilter(e.target.value)}
            className="w-full sm:w-48 py-2 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 outline-none"
          >
            <option value="">All Categories</option>
            <option value="Network">Network & Wi-Fi</option>
            <option value="Microsoft 365">Microsoft 365 / Office</option>
            <option value="Google Workspace">Google Workspace</option>
            <option value="Hardware">Hardware & Docks</option>
            <option value="Mobile Device">Mobile Devices & MDM</option>
            <option value="Access/Login">Access & Passwords</option>
            <option value="Security">Security & Phishing</option>
          </select>
        </div>

        {/* OS Filter Tabs */}
        <div className="flex items-center gap-1.5 pt-2 border-t border-slate-100 overflow-x-auto text-xs">
          <span className="text-[11px] font-semibold text-slate-400 mr-2 uppercase">Platform:</span>
          {[
            { id: 'All', label: 'All OS Platforms', icon: Sparkles },
            { id: 'Windows', label: 'Windows 11 / 10', icon: Monitor },
            { id: 'macOS', label: 'macOS (Mac)', icon: Apple },
            { id: 'iOS', label: 'Apple iOS', icon: Smartphone },
            { id: 'Android', label: 'Android Enterprise', icon: Smartphone },
          ].map((tab) => (
            <button
              key={tab.id}
              onClick={() => setOsFilter(tab.id)}
              className={`flex items-center gap-1.5 px-3 py-1.5 rounded-lg font-medium whitespace-nowrap transition ${
                osFilter === tab.id
                  ? 'bg-indigo-600 text-white font-semibold'
                  : 'text-slate-600 hover:bg-slate-100'
              }`}
            >
              <tab.icon className="w-3.5 h-3.5" />
              <span>{tab.label}</span>
            </button>
          ))}
        </div>
      </div>

      {/* Article Cards Grid */}
      {loading ? (
        <div className="py-20 flex justify-center">
          <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
        </div>
      ) : articles.length === 0 ? (
        <div className="bg-white p-12 rounded-xl border border-slate-200 text-center">
          <BookOpen className="w-8 h-8 text-slate-400 mx-auto mb-2" />
          <p className="text-xs font-semibold text-slate-800">No support articles found matching criteria.</p>
        </div>
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
          {articles.map((a) => (
            <div
              key={a.id}
              onClick={() => handleOpenArticle(a)}
              className="bg-white p-5 rounded-xl border border-slate-200 shadow-xs hover:border-indigo-400 hover:shadow-xs transition cursor-pointer flex flex-col justify-between group"
            >
              <div>
                <div className="flex items-center justify-between mb-2">
                  <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {a.category}
                  </span>
                  {a.os_target && a.os_target !== 'All' && (
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
                      {a.os_target}
                    </span>
                  )}
                </div>

                <h3 className="text-sm font-bold text-slate-900 group-hover:text-indigo-600 transition leading-snug">
                  {a.title}
                </h3>
                <p className="text-xs text-slate-500 mt-2 line-clamp-3 leading-relaxed font-sans">
                  {a.content.replace(/###/g, '').replace(/\*\*/g, '').slice(0, 140)}...
                </p>
              </div>

              <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-[11px] text-slate-400">
                <span className="flex items-center gap-1">
                  <Eye className="w-3.5 h-3.5 text-slate-400" />
                  {a.view_count} views
                </span>
                <span className="flex items-center gap-1 text-emerald-600 font-medium">
                  <ThumbsUp className="w-3 h-3" />
                  {a.helpful_count} helpful
                </span>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Article Detail Reader Modal */}
      {selectedArticle && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <div className="bg-white rounded-xl max-w-2xl w-full p-6 shadow-xl border border-slate-200 max-h-[90vh] flex flex-col">
            <div className="flex items-start justify-between border-b border-slate-200 pb-3 mb-4">
              <div>
                <div className="flex items-center gap-2 mb-1">
                  <span className="text-[10px] font-bold uppercase tracking-wider px-2 py-0.5 rounded bg-slate-100 text-slate-700">
                    {selectedArticle.category}
                  </span>
                  {selectedArticle.os_target && (
                    <span className="text-[10px] font-semibold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
                      OS Target: {selectedArticle.os_target}
                    </span>
                  )}
                </div>
                <h2 className="text-base font-bold text-slate-900 leading-snug">
                  {selectedArticle.title}
                </h2>
              </div>
              <button
                onClick={() => setSelectedArticle(null)}
                className="text-slate-400 hover:text-slate-600 text-xl font-bold"
              >
                &times;
              </button>
            </div>

            <div className="flex-1 overflow-y-auto pr-1 text-xs leading-relaxed text-slate-800 space-y-3 font-sans whitespace-pre-line bg-slate-50/50 p-4 rounded-lg border border-slate-100">
              {selectedArticle.content}
            </div>

            {/* Helpful Feedback Section */}
            <div className="pt-4 border-t border-slate-200 mt-4 flex items-center justify-between">
              <div className="flex items-center gap-2">
                <span className="text-xs text-slate-600 font-medium">Was this guide helpful?</span>
                <button
                  onClick={() => handleVote(true)}
                  disabled={voted !== null}
                  className={`inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-semibold border transition ${
                    voted === true
                      ? 'bg-emerald-50 border-emerald-300 text-emerald-700'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <ThumbsUp className="w-3.5 h-3.5" />
                  Yes ({selectedArticle.helpful_count})
                </button>
                <button
                  onClick={() => handleVote(false)}
                  disabled={voted !== null}
                  className={`inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-semibold border transition ${
                    voted === false
                      ? 'bg-rose-50 border-rose-300 text-rose-700'
                      : 'border-slate-200 hover:bg-slate-50 text-slate-700'
                  }`}
                >
                  <ThumbsDown className="w-3.5 h-3.5" />
                  No ({selectedArticle.not_helpful_count})
                </button>
              </div>

              <button
                onClick={() => setSelectedArticle(null)}
                className="px-4 py-1.5 bg-slate-800 hover:bg-slate-900 text-white text-xs font-semibold rounded-lg"
              >
                Done Reading
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Publish Article Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <form onSubmit={handleCreateArticle} className="bg-white rounded-xl max-w-lg w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Publish New IT Support Guide</h3>
            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Article Title</label>
              <input
                type="text"
                required
                placeholder="How to Configure Outlook 365 on macOS Sonoma"
                value={newTitle}
                onChange={(e) => setNewTitle(e.target.value)}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Category</label>
                <select
                  value={newCategory}
                  onChange={(e) => setNewCategory(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="Network">Network & Wi-Fi</option>
                  <option value="Microsoft 365">Microsoft 365 / Office</option>
                  <option value="Google Workspace">Google Workspace</option>
                  <option value="Hardware">Hardware & Peripherals</option>
                  <option value="Mobile Device">Mobile Device</option>
                  <option value="Access/Login">Access & Passwords</option>
                  <option value="Security">Security</option>
                </select>
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Target Operating System</label>
                <select
                  value={newOs}
                  onChange={(e) => setNewOs(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="All">All Platforms</option>
                  <option value="Windows">Windows</option>
                  <option value="macOS">macOS</option>
                  <option value="iOS">Apple iOS</option>
                  <option value="Android">Android</option>
                </select>
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Guide Instructions / Markdown</label>
              <textarea
                rows={6}
                required
                value={newContent}
                onChange={(e) => setNewContent(e.target.value)}
                placeholder="### Overview&#10;Steps to resolve..."
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none font-mono"
              />
            </div>

            <div className="flex justify-end gap-2 pt-2">
              <button
                type="button"
                onClick={() => setShowAddModal(false)}
                className="px-3 py-1.5 rounded-lg border text-xs font-semibold text-slate-700"
              >
                Cancel
              </button>
              <button
                type="submit"
                className="px-4 py-1.5 rounded-lg bg-indigo-600 hover:bg-indigo-700 text-xs font-bold text-white"
              >
                Publish Guide
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
