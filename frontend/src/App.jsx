import { useCallback } from 'react';
import LoginForm from './components/LoginForm.jsx';
import QueryWorkspace from './components/QueryWorkspace.jsx';
import AdminPanel from './components/AdminPanel.jsx';
import { useAuth } from './hooks/useAuth.js';

export default function App() {
  const { token, role, sessionMessage, clearSessionMessage, login, logout } = useAuth();

  const handleUnauthorized = useCallback(() => {
    logout('Your session has expired. Please sign in again.');
  }, [logout]);

  if (!token) {
    return (
      <div className="app">
        <h1>Query Execution Service</h1>
        <p className="muted">Sign in to manage and execute queries.</p>
        <LoginForm onAuthenticated={login} onUnauthorized={handleUnauthorized} />
        {sessionMessage && (
          <div className={`pill ${sessionMessage.type}`} style={{ marginTop: 12 }}>
            {sessionMessage.text}
          </div>
        )}
      </div>
    );
  }

  return (
    <div className="app">
      <div className="status-line">
        <div>
          <h1>Query Execution Service</h1>
          <div className="muted">Signed in as <strong>{role || 'User'}</strong></div>
        </div>
        <div className="flex">
          <button className="logout-button" onClick={() => logout()}>
            Logout
          </button>
        </div>
      </div>

      <QueryWorkspace token={token} role={role} onUnauthorized={handleUnauthorized} />
      {role === 'ADMIN' && <AdminPanel token={token} onUnauthorized={handleUnauthorized} />}
      {sessionMessage && (
        <div className={`pill ${sessionMessage.type} session-banner`}>
          {sessionMessage.text}
          <button
            onClick={clearSessionMessage}
            style={{ marginLeft: 12, background: 'transparent', color: 'inherit', padding: 0 }}
          >
            ×
          </button>
        </div>
      )}
    </div>
  );
}
