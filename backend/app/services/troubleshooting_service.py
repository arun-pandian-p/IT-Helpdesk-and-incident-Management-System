from typing import Any, Dict, List


TROUBLESHOOTING_TEMPLATES: Dict[str, List[Dict[str, Any]]] = {
    # -----------------------------------------------------------------------
    # HARDWARE TROUBLESHOOTING
    # -----------------------------------------------------------------------
    "Hardware:Laptop:Power": [
        {"id": "hw_pwr_1", "step": "Verify physical connection of AC power adapter"},
        {"id": "hw_pwr_2", "step": "Check LED charging indicator status on chassis"},
        {"id": "hw_pwr_3", "step": "Disconnect from docking station and external peripherals"},
        {"id": "hw_pwr_4", "step": "Perform hard 30-second power drain reset"},
        {"id": "hw_pwr_5", "step": "Test with known-good verified OEM power adapter"},
        {"id": "hw_pwr_6", "step": "Inspect USB-C / barrel power port for pin damage"},
        {"id": "hw_pwr_7", "step": "Escalate to Dell/Lenovo/Apple warranty repair dispatch"},
    ],
    "Hardware:Printer": [
        {"id": "hw_prn_1", "step": "Verify printer display panel status and error codes"},
        {"id": "hw_prn_2", "step": "Confirm network connectivity via ping and web admin interface"},
        {"id": "hw_prn_3", "step": "Check print spooler service on print server / client machine"},
        {"id": "hw_prn_4", "step": "Clear paper path sensors and inspect paper trays"},
        {"id": "hw_prn_5", "step": "Verify toner/drum levels and replace depleted consumables"},
        {"id": "hw_prn_6", "step": "Reinstall PCL6 / PostScript driver via Print Management"},
    ],
    "Hardware:Monitor": [
        {"id": "hw_mon_1", "step": "Check power cable and status LED on monitor bezel"},
        {"id": "hw_mon_2", "step": "Inspect DisplayPort / HDMI / USB-C video cables for damage"},
        {"id": "hw_mon_3", "step": "Test alternate input source via monitor OSD menu"},
        {"id": "hw_mon_4", "step": "Bypass docking station and plug directly into laptop"},
        {"id": "hw_mon_5", "step": "Update display adapter graphics drivers"},
        {"id": "hw_mon_6", "step": "Arrange replacement monitor from IT stockroom"},
    ],
    "Hardware:Generic": [
        {"id": "hw_gen_1", "step": "Verify physical cabling and power status"},
        {"id": "hw_gen_2", "step": "Inspect asset tags and serial number barcode"},
        {"id": "hw_gen_3", "step": "Disconnect and reconnect device to isolate port failure"},
        {"id": "hw_gen_4", "step": "Test device on a secondary workstation"},
        {"id": "hw_gen_5", "step": "Check Device Manager / System Information for device errors"},
        {"id": "hw_gen_6", "step": "Log hardware warranty dispatch or replace from stock"},
    ],

    # -----------------------------------------------------------------------
    # OPERATING SYSTEM TROUBLESHOOTING
    # -----------------------------------------------------------------------
    "OS:Windows": [
        {"id": "os_win_1", "step": "Verify network connectivity and default gateway ping"},
        {"id": "os_win_2", "step": "Check Windows Event Viewer (System and Application logs)"},
        {"id": "os_win_3", "step": "Run system file integrity check: sfc /scannow & DISM"},
        {"id": "os_win_4", "step": "Verify pending Windows Updates and reboot status"},
        {"id": "os_win_5", "step": "Check Company Portal / Intune device compliance status"},
        {"id": "os_win_6", "step": "Re-register Microsoft Entra ID (dsregcmd /status)"},
    ],
    "OS:macOS": [
        {"id": "os_mac_1", "step": "Verify Wi-Fi IP configuration and DNS resolution"},
        {"id": "os_mac_2", "step": "Check Jamf Pro / MDM profile sync status in System Settings"},
        {"id": "os_mac_3", "step": "Inspect Privacy & Security permissions (Full Disk, Screen Recording)"},
        {"id": "os_mac_4", "step": "Boot into macOS Recovery or Safe Mode for diagnostic verification"},
        {"id": "os_mac_5", "step": "Reset local keychain if encountering persistent authentication prompts"},
        {"id": "os_mac_6", "step": "Verify FileVault encryption key escrow in MDM"},
    ],
    "OS:Mobile": [
        {"id": "os_mob_1", "step": "Verify mobile device OS version and security patch status"},
        {"id": "os_mob_2", "step": "Check Company Portal / Workspace ONE enrollment status"},
        {"id": "os_mob_3", "step": "Verify Microsoft Authenticator / MFA push notification settings"},
        {"id": "os_mob_4", "step": "Confirm Work Profile or MAM container isolation"},
        {"id": "os_mob_5", "step": "Remove and push managed corporate email profile"},
    ],

    # -----------------------------------------------------------------------
    # MICROSOFT 365 TROUBLESHOOTING
    # -----------------------------------------------------------------------
    "M365:Outlook": [
        {"id": "m365_out_1", "step": "Check Microsoft 365 Service Health Dashboard for tenant outages"},
        {"id": "m365_out_2", "step": "Verify mailbox connectivity in Outlook Web App (OWA)"},
        {"id": "m365_out_3", "step": "Check mailbox storage quota in Exchange Online Admin Center"},
        {"id": "m365_out_4", "step": "Start Outlook in Safe Mode (outlook.exe /safe) to isolate add-ins"},
        {"id": "m365_out_5", "step": "Clear cached credentials in Windows Credential Manager"},
        {"id": "m365_out_6", "step": "Recreate Outlook profile in Control Panel > Mail"},
    ],
    "M365:Teams": [
        {"id": "m365_tms_1", "step": "Verify Teams desktop client version and available updates"},
        {"id": "m365_tms_2", "step": "Test audio/video device in Teams Settings > Devices test call"},
        {"id": "m365_tms_3", "step": "Clear Teams cache folder (%appdata%\\Microsoft\\Teams or new Teams cache)"},
        {"id": "m365_tms_4", "step": "Verify microphone privacy permissions in OS settings"},
        {"id": "m365_tms_5", "step": "Test connection in Teams Web Client (teams.microsoft.com)"},
    ],
    "M365:Generic": [
        {"id": "m365_gen_1", "step": "Verify user M365 license assignment in Microsoft Entra admin center"},
        {"id": "m365_gen_2", "step": "Check Conditional Access policies and sign-in logs for block events"},
        {"id": "m365_gen_3", "step": "Test user authentication across Microsoft Edge InPrivate session"},
        {"id": "m365_gen_4", "step": "Verify MFA registration status and initiate temporary access pass if needed"},
    ],

    # -----------------------------------------------------------------------
    # GOOGLE WORKSPACE TROUBLESHOOTING
    # -----------------------------------------------------------------------
    "Google:Workspace": [
        {"id": "gw_1", "step": "Check Google Workspace Status Dashboard for service disruptions"},
        {"id": "gw_2", "step": "Confirm user account status in Google Workspace Admin Console"},
        {"id": "gw_3", "step": "Inspect mailbox routing, spam filters, and MX records"},
        {"id": "gw_4", "step": "Check Google Drive storage quota and trash retention"},
        {"id": "gw_5", "step": "Verify 2-Step Verification and security keys in Google Account"},
        {"id": "gw_6", "step": "Test in Chrome Incognito mode with third-party extensions disabled"},
    ],

    # -----------------------------------------------------------------------
    # NETWORK & VPN TROUBLESHOOTING
    # -----------------------------------------------------------------------
    "Network:VPN": [
        {"id": "net_vpn_1", "step": "Verify local internet connection and DNS resolution"},
        {"id": "net_vpn_2", "step": "Check VPN concentrator gateway status and certificate validity"},
        {"id": "net_vpn_3", "step": "Confirm user AD / Entra security group membership for VPN access"},
        {"id": "net_vpn_4", "step": "Inspect virtual adapter (TAP / AnyConnect / GlobalProtect adapter) in OS"},
        {"id": "net_vpn_5", "step": "Verify routing table and split-tunnel routes"},
        {"id": "net_vpn_6", "step": "Reset TCP/IP stack and flush DNS cache (ipconfig /flushdns)"},
    ],
}


def get_checklist_for_ticket(category: str, device_type: str = "", app_name: str = "", os_name: str = "") -> List[Dict[str, Any]]:
    """Returns appropriate diagnostic checklist template based on incident context."""
    cat_lower = (category or "").lower()
    app_lower = (app_name or "").lower()
    dev_lower = (device_type or "").lower()
    os_lower = (os_name or "").lower()

    if "printer" in cat_lower or "printer" in dev_lower:
        return TROUBLESHOOTING_TEMPLATES["Hardware:Printer"]

    if "outlook" in app_lower or ("email" in cat_lower and "microsoft" in cat_lower):
        return TROUBLESHOOTING_TEMPLATES["M365:Outlook"]

    if "teams" in app_lower:
        return TROUBLESHOOTING_TEMPLATES["M365:Teams"]

    if "microsoft" in cat_lower or "office" in app_lower:
        return TROUBLESHOOTING_TEMPLATES["M365:Generic"]

    if "google" in cat_lower or "gmail" in app_lower or "drive" in app_lower:
        return TROUBLESHOOTING_TEMPLATES["Google:Workspace"]

    if "vpn" in cat_lower or "vpn" in app_lower or "network" in cat_lower:
        return TROUBLESHOOTING_TEMPLATES["Network:VPN"]

    if "laptop" in dev_lower:
        return TROUBLESHOOTING_TEMPLATES["Hardware:Laptop:Power"]

    if "monitor" in dev_lower:
        return TROUBLESHOOTING_TEMPLATES["Hardware:Monitor"]

    if "hardware" in cat_lower:
        return TROUBLESHOOTING_TEMPLATES["Hardware:Generic"]

    if "mac" in os_lower:
        return TROUBLESHOOTING_TEMPLATES["OS:macOS"]

    if "ios" in os_lower or "android" in os_lower or "mobile" in cat_lower:
        return TROUBLESHOOTING_TEMPLATES["OS:Mobile"]

    if "windows" in os_lower:
        return TROUBLESHOOTING_TEMPLATES["OS:Windows"]

    return TROUBLESHOOTING_TEMPLATES["OS:Windows"]
