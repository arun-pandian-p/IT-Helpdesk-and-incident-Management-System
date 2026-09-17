import React, { createContext, useContext, useEffect, useState } from 'react';
import { User, UserRole } from '../types';
import { authService } from '../services/api';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  isLoading: boolean;
  login: (email: string, pass: string) => Promise<User>;
  logout: () => void;
  isEmployee: boolean;
  isSupport: boolean;
  isAdmin: boolean;
  canManageTickets: boolean;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('helpdesk_user');
    return saved ? JSON.parse(saved) : null;
  });
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('helpdesk_token'));
  const [isLoading, setIsLoading] = useState(true);

  useEffect(() => {
    const fetchUser = async () => {
      if (token) {
        try {
          const freshUser = await authService.getMe();
          setUser(freshUser);
          localStorage.setItem('helpdesk_user', JSON.stringify(freshUser));
        } catch {
          localStorage.removeItem('helpdesk_token');
          localStorage.removeItem('helpdesk_user');
          setToken(null);
          setUser(null);
        }
      }
      setIsLoading(false);
    };

    fetchUser();
  }, [token]);

  const login = async (email: string, pass: string): Promise<User> => {
    setIsLoading(true);
    try {
      const data = await authService.login({ email, password: pass });
      localStorage.setItem('helpdesk_token', data.access_token);
      localStorage.setItem('helpdesk_user', JSON.stringify(data.user));
      setToken(data.access_token);
      setUser(data.user);
      return data.user;
    } finally {
      setIsLoading(false);
    }
  };

  const logout = () => {
    localStorage.removeItem('helpdesk_token');
    localStorage.removeItem('helpdesk_user');
    setToken(null);
    setUser(null);
    window.location.href = '/login';
  };

  const isEmployee = user?.role === 'EMPLOYEE';
  const isSupport = user?.role === 'SUPPORT';
  const isAdmin = user?.role === 'ADMIN';
  const canManageTickets = user?.role === 'SUPPORT' || user?.role === 'ADMIN';

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!user && !!token,
        isLoading,
        login,
        logout,
        isEmployee,
        isSupport,
        isAdmin,
        canManageTickets,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const context = useContext(AuthContext);
  if (!context) {
    throw new Error('useAuth must be used within an AuthProvider');
  }
  return context;
};
