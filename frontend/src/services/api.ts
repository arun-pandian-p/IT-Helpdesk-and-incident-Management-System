import axios from 'axios';
import {
  AccessRequest,
  Asset,
  DashboardSummary,
  ImprovementRequest,
  KnowledgeArticle,
  OnboardingRequest,
  OnboardingTask,
  Ticket,
  TicketCategory,
  TicketComment,
  TicketFeedback,
  TicketListResponse,
  User,
} from '../types';

const api = axios.create({
  baseURL: import.meta.env.VITE_API_URL ? `${import.meta.env.VITE_API_URL}/api` : '/api',
  headers: {
    'Content-Type': 'application/json',
  },
});

// Request Interceptor to inject Bearer Token
api.interceptors.request.use(
  (config) => {
    const token = localStorage.getItem('helpdesk_token');
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error)
);

// Response Interceptor to catch 401 Unauthorized
api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response && error.response.status === 401) {
      if (!window.location.pathname.includes('/login')) {
        localStorage.removeItem('helpdesk_token');
        localStorage.removeItem('helpdesk_user');
        window.location.href = '/login';
      }
    }
    return Promise.reject(error);
  }
);

// ---------------------------------------------------------------------------
// AUTH SERVICES
// ---------------------------------------------------------------------------
export const authService = {
  login: async (credentials: { email: string; password: string }) => {
    const res = await api.post<{ access_token: string; user: User }>('/auth/login', credentials);
    return res.data;
  },
  register: async (payload: any) => {
    const res = await api.post<{ access_token: string; user: User }>('/auth/register', payload);
    return res.data;
  },
  getMe: async () => {
    const res = await api.get<User>('/auth/me');
    return res.data;
  },
};

// ---------------------------------------------------------------------------
// TICKET SERVICES
// ---------------------------------------------------------------------------
export const ticketService = {
  getTickets: async (params?: Record<string, any>) => {
    const res = await api.get<TicketListResponse>('/tickets', { params });
    return res.data;
  },
  getTicket: async (id: string) => {
    const res = await api.get<Ticket>(`/tickets/${id}`);
    return res.data;
  },
  createTicket: async (payload: any) => {
    const res = await api.post<Ticket>('/tickets', payload);
    return res.data;
  },
  assignTicket: async (id: string, assignedTo: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/assign`, { assigned_to: assignedTo });
    return res.data;
  },
  updateStatus: async (id: string, status: string, reason?: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/status`, { status, reason });
    return res.data;
  },
  overridePriority: async (id: string, priority: string, reason: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/override-priority`, { priority, reason });
    return res.data;
  },
  escalateTicket: async (id: string, level: number, reason: string, toEngineer?: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/escalate`, {
      escalation_level: level,
      reason,
      to_engineer: toEngineer,
    });
    return res.data;
  },
  resolveTicket: async (id: string, resolutionNotes: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/resolve`, { resolution_notes: resolutionNotes });
    return res.data;
  },
  closeTicket: async (id: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/close`);
    return res.data;
  },
  reopenTicket: async (id: string) => {
    const res = await api.post<Ticket>(`/tickets/${id}/reopen`);
    return res.data;
  },
  addComment: async (id: string, comment: string, isInternal: boolean = false) => {
    const res = await api.post<TicketComment>(`/tickets/${id}/comments`, { comment, is_internal: isInternal });
    return res.data;
  },
  updateChecklist: async (id: string, checklistState: string) => {
    const res = await api.patch<Ticket>(`/tickets/${id}/checklist`, { checklist_state: checklistState });
    return res.data;
  },
  submitFeedback: async (id: string, rating: number, comments?: string) => {
    const res = await api.post<TicketFeedback>(`/tickets/${id}/feedback`, { rating, comments });
    return res.data;
  },
};

// ---------------------------------------------------------------------------
// CATEGORIES & ASSETS
// ---------------------------------------------------------------------------
export const categoryService = {
  getCategories: async () => {
    const res = await api.get<TicketCategory[]>('/categories');
    return res.data;
  },
  createCategory: async (payload: { name: string; description?: string }) => {
    const res = await api.post<TicketCategory>('/categories', payload);
    return res.data;
  },
};

export const assetService = {
  getAssets: async (params?: Record<string, any>) => {
    const res = await api.get<Asset[]>('/assets', { params });
    return res.data;
  },
  getMyDevices: async () => {
    const res = await api.get<Asset[]>('/assets/my-devices');
    return res.data;
  },
  getAsset: async (id: string) => {
    const res = await api.get<Asset>(`/assets/${id}`);
    return res.data;
  },
  createAsset: async (payload: any) => {
    const res = await api.post<Asset>('/assets', payload);
    return res.data;
  },
  updateAsset: async (id: string, payload: any) => {
    const res = await api.patch<Asset>(`/assets/${id}`, payload);
    return res.data;
  },
};

// ---------------------------------------------------------------------------
// ONBOARDING & ACCESS
// ---------------------------------------------------------------------------
export const onboardingService = {
  getRequests: async (department?: string) => {
    const res = await api.get<OnboardingRequest[]>('/onboarding', { params: { department } });
    return res.data;
  },
  getRequest: async (id: string) => {
    const res = await api.get<OnboardingRequest>(`/onboarding/${id}`);
    return res.data;
  },
  createRequest: async (payload: any) => {
    const res = await api.post<OnboardingRequest>('/onboarding', payload);
    return res.data;
  },
  updateTask: async (taskId: string, payload: { status?: string; notes?: string; owner_id?: string }) => {
    const res = await api.patch<OnboardingTask>(`/onboarding/tasks/${taskId}`, payload);
    return res.data;
  },
};

export const accessService = {
  getRequests: async (status?: string) => {
    const res = await api.get<AccessRequest[]>('/access-requests', { params: { status } });
    return res.data;
  },
  createRequest: async (payload: { request_type: string; requested_system: string; access_level?: string; reason: string }) => {
    const res = await api.post<AccessRequest>('/access-requests', payload);
    return res.data;
  },
  updateStatus: async (id: string, status: string) => {
    const res = await api.patch<AccessRequest>(`/access-requests/${id}/status`, { status });
    return res.data;
  },
};

// ---------------------------------------------------------------------------
// KNOWLEDGE BASE & IMPROVEMENTS
// ---------------------------------------------------------------------------
export const knowledgeService = {
  getArticles: async (params?: { category?: string; os_target?: string; search?: string }) => {
    const res = await api.get<KnowledgeArticle[]>('/knowledge', { params });
    return res.data;
  },
  getArticle: async (id: string) => {
    const res = await api.get<KnowledgeArticle>(`/knowledge/${id}`);
    return res.data;
  },
  createArticle: async (payload: any) => {
    const res = await api.post<KnowledgeArticle>('/knowledge', payload);
    return res.data;
  },
  voteArticle: async (id: string, helpful: boolean) => {
    const res = await api.post<KnowledgeArticle>(`/knowledge/${id}/vote`, null, { params: { helpful } });
    return res.data;
  },
};

export const improvementService = {
  getImprovements: async (params?: Record<string, any>) => {
    const res = await api.get<ImprovementRequest[]>('/improvements', { params });
    return res.data;
  },
  createImprovement: async (payload: any) => {
    const res = await api.post<ImprovementRequest>('/improvements', payload);
    return res.data;
  },
  updateImprovement: async (id: string, payload: any) => {
    const res = await api.patch<ImprovementRequest>(`/improvements/${id}`, payload);
    return res.data;
  },
};

// ---------------------------------------------------------------------------
// USERS & DASHBOARDS
// ---------------------------------------------------------------------------
export const userService = {
  getUsers: async (role?: string, department?: string) => {
    const res = await api.get<User[]>('/users', { params: { role, department } });
    return res.data;
  },
  getSupportStaff: async () => {
    const res = await api.get<User[]>('/users/support-staff');
    return res.data;
  },
  getUser: async (id: string) => {
    const res = await api.get<User>(`/users/${id}`);
    return res.data;
  },
  createUser: async (payload: any) => {
    const res = await api.post<User>('/users', payload);
    return res.data;
  },
  updateUser: async (id: string, payload: any) => {
    const res = await api.patch<User>(`/users/${id}`, payload);
    return res.data;
  },
  getAuditLogs: async (limit: number = 50) => {
    const res = await api.get<any[]>('/users/audit/logs', { params: { limit } });
    return res.data;
  },
};

export const dashboardService = {
  getSummary: async () => {
    const res = await api.get<DashboardSummary>('/dashboard/summary');
    return res.data;
  },
};

export default api;
