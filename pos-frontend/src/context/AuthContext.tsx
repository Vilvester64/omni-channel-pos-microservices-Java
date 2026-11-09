import React, { createContext, useContext, useState, useEffect } from 'react';
import { apiClient } from '../api/client';
import { ApiResponse, User } from '../types';

interface AuthContextType {
  user: User | null;
  token: string | null;
  isAuthenticated: boolean;
  login: (username: string, password: string) => Promise<void>;
  logout: () => void;
}

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [token, setToken] = useState<string | null>(() => localStorage.getItem('pos_token'));
  const [user, setUser] = useState<User | null>(() => {
    const saved = localStorage.getItem('pos_user');
    return saved ? JSON.parse(saved) : null;
  });

  useEffect(() => {
    if (!token) {
      setUser(null);
    }
  }, [token]);

  const login = async (username: string, password: string) => {
    const res = await apiClient.post<ApiResponse<{
      token: string;
      tokenType: string;
      userId: string;
      username: string;
      roles: string;
      expiresInMs: number;
    }>>('/api/auth/login', { username, password });

    const data = res.data.data;
    const authenticatedUser: User = {
      userId: data.userId,
      username: data.username,
      roles: data.roles,
      token: data.token,
    };

    localStorage.setItem('pos_token', data.token);
    localStorage.setItem('pos_user', JSON.stringify(authenticatedUser));
    setToken(data.token);
    setUser(authenticatedUser);
  };

  const logout = () => {
    localStorage.removeItem('pos_token');
    localStorage.removeItem('pos_user');
    setToken(null);
    setUser(null);
  };

  return (
    <AuthContext.Provider
      value={{
        user,
        token,
        isAuthenticated: !!token,
        login,
        logout,
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
