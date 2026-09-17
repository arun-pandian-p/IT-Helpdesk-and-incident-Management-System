import React, { useEffect, useState } from 'react';
import { useAuth } from '../hooks/useAuth';
import { assetService } from '../services/api';
import { Asset } from '../types';
import { Laptop, Plus, Search, ShieldCheck, Wrench, CheckCircle2, Clock } from 'lucide-react';

export const AssetsPage: React.FC = () => {
  const { user, canManageTickets, isEmployee } = useAuth();
  const [assets, setAssets] = useState<Asset[]>([]);
  const [search, setSearch] = useState('');
  const [deviceTypeFilter, setDeviceTypeFilter] = useState('');
  const [loading, setLoading] = useState(true);

  // New Asset Modal
  const [showAddModal, setShowAddModal] = useState(false);
  const [newTag, setNewTag] = useState('');
  const [newType, setNewType] = useState('Laptop');
  const [newMfg, setNewMfg] = useState('Dell');
  const [newModel, setNewModel] = useState('');
  const [newSerial, setNewSerial] = useState('');
  const [newNotes, setNewNotes] = useState('');

  const loadAssets = async () => {
    setLoading(true);
    try {
      if (isEmployee) {
        const myDevs = await assetService.getMyDevices();
        setAssets(myDevs);
      } else {
        const allDevs = await assetService.getAssets({
          device_type: deviceTypeFilter || undefined,
          search: search || undefined,
        });
        setAssets(allDevs);
      }
    } catch (err) {
      console.error('Failed to load assets', err);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    loadAssets();
  }, [deviceTypeFilter, search, isEmployee]);

  const handleCreateAsset = async (e: React.FormEvent) => {
    e.preventDefault();
    try {
      await assetService.createAsset({
        asset_tag: newTag,
        device_type: newType,
        manufacturer: newMfg,
        model: newModel,
        serial_number: newSerial,
        status: 'In Stock',
        notes: newNotes,
      });
      setShowAddModal(false);
      setNewTag('');
      setNewModel('');
      setNewSerial('');
      await loadAssets();
    } catch (err) {
      console.error('Failed to create asset', err);
    }
  };

  return (
    <div className="space-y-5">
      <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-xl font-bold text-slate-900 tracking-tight">
            {isEmployee ? 'My Assigned IT Hardware' : 'Hardware & IT Asset Inventory'}
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            {isEmployee
              ? 'Devices and peripherals registered to your employee profile'
              : 'Physical computing equipment, serial tracking, and warranty life-cycles'}
          </p>
        </div>

        {canManageTickets && (
          <button
            onClick={() => setShowAddModal(true)}
            className="inline-flex items-center gap-1.5 px-3.5 py-2 bg-indigo-600 hover:bg-indigo-700 text-white rounded-lg text-xs font-semibold shadow-xs transition"
          >
            <Plus className="w-4 h-4" />
            Add Hardware Asset
          </button>
        )}
      </div>

      {!isEmployee && (
        <div className="bg-white p-3.5 rounded-xl border border-slate-200 shadow-xs flex flex-col sm:flex-row items-center gap-3">
          <div className="relative flex-1 w-full">
            <div className="absolute inset-y-0 left-0 pl-3 flex items-center pointer-events-none text-slate-400">
              <Search className="w-4 h-4" />
            </div>
            <input
              type="text"
              placeholder="Search by Asset Tag (AST-XXXX), Model, or Serial Number..."
              value={search}
              onChange={(e) => setSearch(e.target.value)}
              className="w-full pl-9 pr-3 py-2 text-xs border border-slate-300 rounded-lg focus:ring-2 focus:ring-indigo-500 outline-none"
            />
          </div>

          <select
            value={deviceTypeFilter}
            onChange={(e) => setDeviceTypeFilter(e.target.value)}
            className="w-full sm:w-44 py-2 px-2.5 text-xs border border-slate-300 rounded-lg bg-white text-slate-700 outline-none"
          >
            <option value="">All Device Types</option>
            <option value="Laptop">Laptops</option>
            <option value="Desktop">Desktops</option>
            <option value="Monitor">Monitors</option>
            <option value="Printer">Printers</option>
            <option value="Mobile Phone">Mobile Phones</option>
            <option value="Docking Station">Docking Stations</option>
          </select>
        </div>
      )}

      {/* Asset Table */}
      <div className="bg-white rounded-xl border border-slate-200 shadow-xs overflow-hidden">
        {loading ? (
          <div className="py-20 flex justify-center">
            <div className="w-8 h-8 border-4 border-indigo-600 border-t-transparent rounded-full animate-spin"></div>
          </div>
        ) : assets.length === 0 ? (
          <div className="p-12 text-center">
            <Laptop className="w-8 h-8 text-slate-400 mx-auto mb-2" />
            <p className="text-xs font-semibold text-slate-800">No hardware assets registered.</p>
          </div>
        ) : (
          <div className="overflow-x-auto">
            <table className="w-full text-left text-xs">
              <thead className="bg-slate-50 text-slate-600 font-semibold border-b border-slate-200">
                <tr>
                  <th className="py-3 px-4">Asset Tag</th>
                  <th className="py-3 px-4">Device & Model</th>
                  <th className="py-3 px-4">Manufacturer</th>
                  <th className="py-3 px-4">Serial Number</th>
                  <th className="py-3 px-4">Status</th>
                  <th className="py-3 px-4">Assigned To</th>
                  <th className="py-3 px-4">Warranty Expiry</th>
                </tr>
              </thead>
              <tbody className="divide-y divide-slate-100">
                {assets.map((a) => (
                  <tr key={a.id} className="hover:bg-slate-50/70 transition">
                    <td className="py-3 px-4 font-mono font-bold text-indigo-600">{a.asset_tag}</td>
                    <td className="py-3 px-4 text-slate-900 font-medium">
                      <div className="flex items-center gap-1.5">
                        <Laptop className="w-3.5 h-3.5 text-slate-400" />
                        <span>{a.model}</span>
                      </div>
                      <span className="text-[10px] text-slate-400">{a.device_type}</span>
                    </td>
                    <td className="py-3 px-4 text-slate-700">{a.manufacturer}</td>
                    <td className="py-3 px-4 font-mono text-slate-600">{a.serial_number}</td>
                    <td className="py-3 px-4">
                      <span
                        className={`inline-block px-2 py-0.5 rounded text-[11px] font-semibold ${
                          a.status === 'In Use'
                            ? 'bg-emerald-100 text-emerald-800'
                            : a.status === 'In Stock'
                            ? 'bg-blue-100 text-blue-800'
                            : 'bg-amber-100 text-amber-800'
                        }`}
                      >
                        {a.status}
                      </span>
                    </td>
                    <td className="py-3 px-4 text-slate-700">
                      {a.assigned_user_name ? (
                        <span className="font-medium text-slate-900">{a.assigned_user_name}</span>
                      ) : (
                        <span className="text-slate-400 italic">IT Stockroom</span>
                      )}
                    </td>
                    <td className="py-3 px-4 text-slate-500 font-mono">
                      {a.warranty_expiry || 'Active OEM'}
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        )}
      </div>

      {/* Add Modal */}
      {showAddModal && (
        <div className="fixed inset-0 bg-slate-900/50 backdrop-blur-xs flex items-center justify-center p-4 z-50">
          <form onSubmit={handleCreateAsset} className="bg-white rounded-xl max-w-md w-full p-6 shadow-xl border border-slate-200 space-y-4">
            <h3 className="text-sm font-bold text-slate-900">Add New Hardware Asset</h3>
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Asset Tag</label>
                <input
                  type="text"
                  required
                  placeholder="AST-1026"
                  value={newTag}
                  onChange={(e) => setNewTag(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Device Type</label>
                <select
                  value={newType}
                  onChange={(e) => setNewType(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg bg-white outline-none"
                >
                  <option value="Laptop">Laptop</option>
                  <option value="Desktop">Desktop</option>
                  <option value="Monitor">Monitor</option>
                  <option value="Printer">Printer</option>
                  <option value="Docking Station">Docking Station</option>
                </select>
              </div>
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Manufacturer</label>
                <input
                  type="text"
                  required
                  placeholder="Dell / Lenovo / Apple"
                  value={newMfg}
                  onChange={(e) => setNewMfg(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
              <div>
                <label className="block text-[11px] font-semibold text-slate-700 mb-1">Model Name</label>
                <input
                  type="text"
                  required
                  placeholder="Latitude 5540"
                  value={newModel}
                  onChange={(e) => setNewModel(e.target.value)}
                  className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
                />
              </div>
            </div>

            <div>
              <label className="block text-[11px] font-semibold text-slate-700 mb-1">Serial Number</label>
              <input
                type="text"
                required
                placeholder="SN-DL-82910"
                value={newSerial}
                onChange={(e) => setNewSerial(e.target.value)}
                className="w-full p-2 text-xs border border-slate-300 rounded-lg outline-none"
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
                Save Device
              </button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
};
