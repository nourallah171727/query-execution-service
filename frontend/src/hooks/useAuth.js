import { useCallback, useEffect, useMemo, useState } from 'react';

const STORAGE_KEY = 'jwt';

const decodeJwt = (token) => {
  if (!token) return null;
  try {
    const [, payload] = token.split('.');
    const decoded = JSON.parse(atob(payload));
    return {
      role: decoded.role || decoded.authorities || null,
      exp: decoded.exp,
    };
  } catch (error) {
    console.warn('Unable to decode token', error);
    return null;
  }
};

export function useAuth() {
  const [token, setToken] = useState(localStorage.getItem(STORAGE_KEY));
  const [sessionMessage, setSessionMessage] = useState(null);

  const decoded = useMemo(() => decodeJwt(token), [token]);

  const logout = useCallback((message) => {
    localStorage.removeItem(STORAGE_KEY);
    setToken(null);
    if (message) {
      setSessionMessage({ type: 'error', text: message });
    }
  }, []);

  const login = useCallback((newToken) => {
    localStorage.setItem(STORAGE_KEY, newToken);
    setToken(newToken);
    setSessionMessage(null);
  }, []);

  useEffect(() => {
    if (!decoded?.exp) return undefined;
    const msUntilExpiry = decoded.exp * 1000 - Date.now();
    if (msUntilExpiry <= 0) {
      logout('Your session expired. Please sign in again.');
      return undefined;
    }

    const timer = setTimeout(() => {
      logout('Your session expired. Please sign in again.');
    }, msUntilExpiry);

    return () => clearTimeout(timer);
  }, [decoded, logout]);

  return {
    token,
    role: decoded?.role,
    sessionMessage,
    clearSessionMessage: () => setSessionMessage(null),
    login,
    logout,
  };
}
