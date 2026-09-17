export type UserRole = 'EMPLOYEE' | 'SUPPORT' | 'ADMIN';

export type TicketPriority = 'Low' | 'Medium' | 'High' | 'Critical';

export type TicketStatus =
  | 'Open'
  | 'In Progress'
  | 'Waiting for User'
  | 'Waiting for Vendor'
  | 'Escalated'
  | 'Resolved'
  | 'Closed';

export type SLAStatus = 'Within SLA' | 'At Risk' | 'Breached';

export interface User {
  id: string;
  name: string;
  email: string;
  role: UserRole;
  department: string;
  job_title?: string;
  phone?: string;
  location?: string;
  is_active: boolean;
  created_at: string;
}

export interface TicketCategory {
  id: string;
  name: string;
  description?: string;
  is_active: boolean;
  created_at: string;
}

export interface Asset {
  id: string;
  asset_tag: string;
  device_type: string;
  manufacturer: string;
  model: string;
  serial_number: string;
  status: 'In Use' | 'In Stock' | 'In Repair' | 'Retired';
  assigned_user_id?: string;
  assigned_user_name?: string;
  purchase_date?: string;
  warranty_expiry?: string;
  notes?: string;
  created_at: string;
}

export interface TicketComment {
  id: string;
  ticket_id: string;
  user_id: string;
  user_name?: string;
  user_role?: string;
  comment: string;
  is_internal: boolean;
  created_at: string;
}

export interface TicketStatusHistory {
  id: string;
  old_status: string;
  new_status: string;
  changed_by_name?: string;
  changed_at: string;
  reason?: string;
}

export interface TicketEscalation {
  id: string;
  from_engineer_name?: string;
  to_engineer_name?: string;
  reason: string;
  escalation_level: number;
  created_at: string;
  resolved_at?: string;
}

export interface TicketFeedback {
  id: string;
  rating: number;
  comments?: string;
  created_at: string;
}

export interface Ticket {
  id: string;
  ticket_number: string;
  title: string;
  description: string;
  category_id: string;
  category_name?: string;
  priority: TicketPriority;
  status: TicketStatus;
  impact: string;
  urgency: string;
  priority_override_reason?: string;
  asset_id?: string;
  asset?: Asset;
  device_type?: string;
  os_name?: string;
  application_name?: string;
  location?: string;
  created_by: string;
  creator_name?: string;
  creator_email?: string;
  creator_department?: string;
  assigned_to?: string;
  assignee_name?: string;
  created_at: string;
  updated_at: string;
  due_at: string;
  first_response_at?: string;
  resolved_at?: string;
  closed_at?: string;
  resolution_notes?: string;
  is_escalated: boolean;
  escalation_level: number;
  checklist_state?: string;
  sla_status?: SLAStatus;
  feedback?: TicketFeedback;
  comments?: TicketComment[];
  status_history?: TicketStatusHistory[];
  escalations?: TicketEscalation[];
}

export interface TicketListResponse {
  items: Ticket[];
  total: number;
  page: number;
  page_size: number;
  total_pages: number;
}

export interface KnowledgeArticle {
  id: string;
  title: string;
  category: string;
  os_target?: string;
  content: string;
  tags?: string;
  view_count: number;
  helpful_count: number;
  not_helpful_count: number;
  created_by: string;
  created_at: string;
  updated_at: string;
}

export interface OnboardingTask {
  id: string;
  request_id: string;
  task_name: string;
  owner_id?: string;
  owner_name?: string;
  status: 'Not Started' | 'In Progress' | 'Blocked' | 'Completed';
  due_date?: string;
  completed_at?: string;
  notes?: string;
}

export interface OnboardingRequest {
  id: string;
  employee_name: string;
  department: string;
  job_title: string;
  manager_name: string;
  start_date: string;
  location: string;
  device_requirement: string;
  software_requirement?: string;
  access_requirement?: string;
  status: string;
  progress_percent: number;
  created_by: string;
  created_at: string;
  tasks: OnboardingTask[];
}

export interface AccessRequest {
  id: string;
  request_type: string;
  requested_system: string;
  access_level: string;
  reason: string;
  requested_by: string;
  requester_name?: string;
  approved_by?: string;
  approver_name?: string;
  status: 'Requested' | 'Pending Approval' | 'Approved' | 'In Progress' | 'Completed' | 'Rejected';
  completed_at?: string;
  created_at: string;
}

export interface ImprovementRequest {
  id: string;
  title: string;
  category: string;
  description: string;
  business_benefit: string;
  status: string;
  priority: string;
  submitted_by: string;
  submitter_name?: string;
  assigned_to?: string;
  assignee_name?: string;
  created_at: string;
}

export interface DashboardSummary {
  total_tickets: number;
  open_tickets: number;
  in_progress_tickets: number;
  waiting_tickets: number;
  resolved_tickets: number;
  closed_tickets: number;
  critical_tickets: number;
  sla_breached: number;
  sla_at_risk: number;
  average_resolution_hours: number;
  average_first_response_hours: number;
  csat_average_rating: number;
  total_assets: number;
  active_onboardings: number;
  pending_access_requests: number;
  status_distribution: Record<string, number>;
  priority_distribution: Record<string, number>;
  category_distribution: Record<string, number>;
  os_distribution: Record<string, number>;
  daily_trend: Array<{
    date: string;
    created: number;
    resolved: number;
  }>;
}

// ---------------------------------------------------------------------------
// SERVICE REQUEST MANAGEMENT MODULE TYPES
// ---------------------------------------------------------------------------
export type ServiceRequestStatus =
  | 'Submitted'
  | 'Pending Approval'
  | 'Approved'
  | 'Rejected'
  | 'Assigned'
  | 'In Progress'
  | 'Waiting for User'
  | 'Fulfilled'
  | 'Closed'
  | 'Cancelled';

export type ServiceRequestApprovalStatus = 'None' | 'Pending' | 'Approved' | 'Rejected';

export type SupportTeam =
  | 'Service Desk'
  | 'Endpoint Support'
  | 'Network Support'
  | 'Application Support'
  | 'Microsoft 365 Support'
  | 'Google Workspace Support'
  | 'Hardware Support'
  | 'Security';

export interface ServiceRequestType {
  id: string;
  name: string;
  description: string;
  category: string;
  default_priority: TicketPriority;
  approval_required: boolean;
  default_sla_hours: number;
  is_active: boolean;
  created_at: string;
  updated_at: string;
}

export interface ServiceRequestComment {
  id: string;
  request_id: string;
  user_id: string;
  user_name?: string;
  user_role?: string;
  comment: string;
  is_internal: boolean;
  created_at: string;
}

export interface ServiceRequestTimeline {
  id: string;
  request_id: string;
  event_type: string;
  title: string;
  description?: string;
  actor_id: string;
  actor_name?: string;
  actor_role?: string;
  created_at: string;
}

export interface ServiceRequest {
  id: string;
  request_number: string;
  title: string;
  description: string;
  category: string;
  priority: TicketPriority;
  status: ServiceRequestStatus;
  request_type_id: string;
  request_type_name?: string;
  requester_id: string;
  requester_name?: string;
  requester_email?: string;
  requester_department?: string;
  approval_required: boolean;
  approval_status: ServiceRequestApprovalStatus;
  approver_id?: string;
  approver_name?: string;
  approval_reason?: string;
  approved_at?: string;
  assigned_to?: string;
  assigned_to_name?: string;
  assigned_team?: SupportTeam;
  business_justification: string;
  required_date?: string;
  asset_id?: string;
  asset_tag?: string;
  asset_model?: string;
  application_name?: string;
  access_level?: string;
  software_name?: string;
  software_version?: string;
  device_type?: string;
  fulfillment_details?: string;
  resolution_notes?: string;
  due_at: string;
  created_at: string;
  updated_at: string;
  fulfilled_at?: string;
  closed_at?: string;
  sla_status: SLAStatus;
  is_breached: boolean;
  comments?: ServiceRequestComment[];
  timeline?: ServiceRequestTimeline[];
}

export interface ServiceRequestListResponse {
  items: ServiceRequest[];
  total: number;
  page: number;
  page_size: number;
  pages: number;
}

export interface UnifiedWorkItem {
  id: string;
  item_type: 'Incident' | 'Service Request';
  reference_number: string;
  title: string;
  category: string;
  requester_id: string;
  requester_name: string;
  requester_department: string;
  priority: TicketPriority;
  status: string;
  assigned_to_id?: string;
  assigned_to_name?: string;
  assigned_team?: string;
  sla_status: SLAStatus;
  is_breached: boolean;
  due_at: string;
  created_at: string;
  updated_at: string;
}

export interface ServiceRequestDashboardSummary {
  total_requests: number;
  pending_approval: number;
  unassigned: number;
  in_progress: number;
  fulfilled: number;
  closed: number;
  sla_breached: number;
  sla_at_risk: number;
  sla_compliance_rate: number;
  average_fulfillment_hours: number;
  by_type: Record<string, number>;
  by_category: Record<string, number>;
  by_status: Record<string, number>;
  by_team: Record<string, number>;
  daily_trend: Array<{
    date: string;
    created: number;
    fulfilled: number;
  }>;
}

