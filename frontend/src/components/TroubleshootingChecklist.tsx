import React, { useState } from 'react';
import { CheckSquare, Square, Save, CheckCircle2, ShieldCheck, Sparkles } from 'lucide-react';
import { ticketService } from '../services/api';

interface ChecklistStep {
  id: string;
  step: string;
}

interface Props {
  ticketId: string;
  category: string;
  deviceType?: string;
  applicationName?: string;
  osName?: string;
  savedState?: string;
  canEdit: boolean;
  onStateSaved?: (newState: string) => void;
}

// Built-in diagnostic step libraries matching backend templates
const getDiagnosticSteps = (category: string, device?: string, app?: string, os?: string): ChecklistStep[] => {
  const cat = (category || '').toLowerCase();
  const dev = (device || '').toLowerCase();
  const a = (app || '').toLowerCase();
  const o = (os || '').toLowerCase();

  if (cat.includes('printer') || dev.includes('printer')) {
    return [
      { id: 'step_1', step: 'Inspect printer physical display panel for active error codes' },
      { id: 'step_2', step: 'Verify network connectivity via ping and web embedded management console' },
      { id: 'step_3', step: 'Restart local Windows Print Spooler service' },
      { id: 'step_4', step: 'Clear paper trays and verify tray 2 sensor alignment' },
      { id: 'step_5', step: 'Verify toner levels and replace low toner cartridges' },
      { id: 'step_6', step: 'Reinstall corporate PCL6 print driver from print server' },
    ];
  }

  if (a.includes('outlook') || (cat.includes('email') && cat.includes('microsoft'))) {
    return [
      { id: 'step_1', step: 'Check Microsoft 365 Service Health Dashboard for tenant Exchange incident' },
      { id: 'step_2', step: 'Verify user mailbox connectivity in Outlook Web App (OWA)' },
      { id: 'step_3', step: 'Verify mailbox storage quota is below 95% in Exchange Online' },
      { id: 'step_4', step: 'Launch Outlook in Safe Mode (outlook.exe /safe) to isolate third-party add-ins' },
      { id: 'step_5', step: 'Clear corrupted credentials in Windows Credential Manager' },
      { id: 'step_6', step: 'Recreate user Outlook mail profile in Control Panel > Mail' },
    ];
  }

  if (a.includes('teams')) {
    return [
      { id: 'step_1', step: 'Verify Microsoft Teams desktop client version and trigger update check' },
      { id: 'step_2', step: 'Perform Teams Settings > Devices test call to isolate audio peripherals' },
      { id: 'step_3', step: 'Clear local Teams desktop cache folder (%appdata%\\Microsoft\\Teams)' },
      { id: 'step_4', step: 'Verify microphone privacy permissions in Windows / macOS security settings' },
      { id: 'step_5', step: 'Test call connection in Teams Web Client (teams.microsoft.com)' },
    ];
  }

  if (cat.includes('vpn') || a.includes('vpn') || cat.includes('network')) {
    return [
      { id: 'step_1', step: 'Verify local internet connection speed and default gateway ping' },
      { id: 'step_2', step: 'Check corporate VPN concentrator gateway status and certificate validity' },
      { id: 'step_3', step: 'Verify user Active Directory / Entra ID security group membership for VPN' },
      { id: 'step_4', step: 'Inspect virtual network adapter (TAP/GlobalProtect adapter) in device settings' },
      { id: 'step_5', step: 'Verify routing table and flush local DNS cache (ipconfig /flushdns)' },
    ];
  }

  if (dev.includes('laptop') || cat.includes('hardware')) {
    return [
      { id: 'step_1', step: 'Verify physical AC adapter power connection and LED charging indicator' },
      { id: 'step_2', step: 'Disconnect from docking station, external monitors, and USB peripherals' },
      { id: 'step_3', step: 'Perform hard 30-second power drain reset with power button held down' },
      { id: 'step_4', step: 'Test machine with known-good verified OEM AC power adapter' },
      { id: 'step_5', step: 'Inspect USB-C charging port for physical pin damage or debris' },
      { id: 'step_6', step: 'Escalate to OEM hardware warranty dispatch (Dell / Lenovo / Apple)' },
    ];
  }

  if (o.includes('mac')) {
    return [
      { id: 'step_1', step: 'Verify Wi-Fi IP configuration and DNS gateway resolution' },
      { id: 'step_2', step: 'Check Jamf Pro / MDM configuration profile sync status in System Settings' },
      { id: 'step_3', step: 'Review Privacy & Security permissions (Full Disk Access, Screen Recording)' },
      { id: 'step_4', step: 'Verify FileVault encryption key escrow status in Jamf Pro' },
    ];
  }

  // Windows standard checklist
  return [
    { id: 'step_1', step: 'Verify network connectivity and domain controller DNS resolution' },
    { id: 'step_2', step: 'Inspect Windows Event Viewer (System and Application event logs)' },
    { id: 'step_3', step: 'Run system integrity scan: sfc /scannow & DISM restore health' },
    { id: 'step_4', step: 'Verify Intune device compliance status and pending security updates' },
    { id: 'step_5', step: 'Re-register Entra ID state: dsregcmd /status & gpupdate /force' },
  ];
};

export const TroubleshootingChecklist: React.FC<Props> = ({
  ticketId,
  category,
  deviceType,
  applicationName,
  osName,
  savedState,
  canEdit,
  onStateSaved,
}) => {
  const steps = getDiagnosticSteps(category, deviceType, applicationName, osName);

  const [state, setState] = useState<Record<string, boolean>>(() => {
    if (!savedState) return {};
    try {
      return JSON.parse(savedState);
    } catch {
      return {};
    }
  });

  const [isSaving, setIsSaving] = useState(false);
  const [saveSuccess, setSaveSuccess] = useState(false);

  const toggleStep = (stepId: string) => {
    if (!canEdit) return;
    setState((prev) => ({
      ...prev,
      [stepId]: !prev[stepId],
    }));
    setSaveSuccess(false);
  };

  const handleSave = async () => {
    setIsSaving(true);
    try {
      const jsonStr = JSON.stringify(state);
      await ticketService.updateChecklist(ticketId, jsonStr);
      setSaveSuccess(true);
      if (onStateSaved) onStateSaved(jsonStr);
      setTimeout(() => setSaveSuccess(false), 3000);
    } catch (err) {
      console.error('Failed to save checklist state', err);
    } finally {
      setIsSaving(false);
    }
  };

  const completedCount = steps.filter((s) => state[s.id]).length;
  const progressPercent = Math.round((completedCount / steps.length) * 100);

  return (
    <div className="bg-white rounded-lg border border-slate-200 p-5 shadow-sm">
      <div className="flex items-center justify-between mb-3">
        <div className="flex items-center gap-2">
          <ShieldCheck className="w-5 h-5 text-indigo-600" />
          <h3 className="font-semibold text-slate-800 text-sm">
            Interactive Diagnostic Troubleshooting Checklist
          </h3>
        </div>
        <span className="text-xs font-semibold px-2 py-0.5 rounded bg-indigo-50 text-indigo-700">
          {completedCount} of {steps.length} completed ({progressPercent}%)
        </span>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-slate-100 rounded-full h-2 mb-4 overflow-hidden">
        <div
          className="bg-indigo-600 h-2 rounded-full transition-all duration-300"
          style={{ width: `${progressPercent}%` }}
        ></div>
      </div>

      <p className="text-xs text-slate-500 mb-3">
        {canEdit
          ? 'Check off diagnostic troubleshooting steps as you execute them with the employee.'
          : 'Support staff diagnostic execution history.'}
      </p>

      <div className="space-y-2">
        {steps.map((s, idx) => {
          const isChecked = !!state[s.id];
          return (
            <div
              key={s.id}
              onClick={() => toggleStep(s.id)}
              className={`flex items-start gap-3 p-2.5 rounded-md border text-sm transition-colors ${
                canEdit ? 'cursor-pointer hover:bg-slate-50' : 'cursor-default'
              } ${
                isChecked
                  ? 'bg-emerald-50/60 border-emerald-200 text-slate-800'
                  : 'bg-white border-slate-200 text-slate-700'
              }`}
            >
              <button
                type="button"
                className="mt-0.5 text-indigo-600 focus:outline-none flex-shrink-0"
                disabled={!canEdit}
              >
                {isChecked ? (
                  <CheckSquare className="w-4 h-4 text-emerald-600" />
                ) : (
                  <Square className="w-4 h-4 text-slate-400" />
                )}
              </button>
              <div className="flex-1">
                <span className={`text-xs font-semibold mr-1.5 ${isChecked ? 'text-emerald-700' : 'text-slate-500'}`}>
                  Step {idx + 1}:
                </span>
                <span className={isChecked ? 'line-through text-slate-600 font-medium' : ''}>
                  {s.step}
                </span>
              </div>
            </div>
          );
        })}
      </div>

      {canEdit && (
        <div className="mt-4 flex items-center justify-between pt-3 border-t border-slate-100">
          <div className="text-xs text-slate-500 flex items-center gap-1">
            <Sparkles className="w-3.5 h-3.5 text-amber-500" />
            Standardized ITIL support procedure
          </div>
          <button
            onClick={handleSave}
            disabled={isSaving}
            className="inline-flex items-center gap-1.5 px-3 py-1.5 rounded-md text-xs font-medium bg-indigo-600 text-white hover:bg-indigo-700 transition disabled:opacity-50"
          >
            {saveSuccess ? (
              <>
                <CheckCircle2 className="w-3.5 h-3.5 text-emerald-300" />
                Saved!
              </>
            ) : (
              <>
                <Save className="w-3.5 h-3.5" />
                {isSaving ? 'Saving...' : 'Save Diagnostic Progress'}
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
};
