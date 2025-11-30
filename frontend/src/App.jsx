import React, { useEffect, useState } from "react";
import LoginForm from "./components/LoginForm.jsx";
import QueryWorkspace from "./components/QueryWorkspace.jsx";
import AdminPanel from "./components/AdminPanel.jsx";
import { API_BASE } from "./utils/api.js";
import "./App.css";

function decodeRole(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    return payload.role || payload.authorities || null;
  } catch (err) {
    return null;
  }
}

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split(".")[1]));
    if (!payload.exp) return false;
    return payload.exp * 1000 < Date.now();
  } catch (err) {
    return false;
  }
}

function App() {
  const [token, setToken] = useState(null);
  const [role, setRole] = useState(null);
  const [sessionMessage, setSessionMessage] = useState(null);

  const handleAuth = (newToken) => {
    setToken(newToken);
    setRole(decodeRole(newToken));
    setSessionMessage(null);
  };

  const logout = () => {
    setToken(null);
    setRole(null);
  };

  const handleUnauthorized = () => {
    logout();
    setSessionMessage("Session expired. Please sign in again.");
  };

  useEffect(() => {
    if (token && isTokenExpired(token)) {
      handleUnauthorized();
    }
  }, [token]);

  if (!token) {
    return (
      <div className="app">
        <h1>Query Execution Service</h1>
        {sessionMessage && <div className="pill error">{sessionMessage}</div>}
        <p className="muted">Sign in to manage and execute queries.</p>
        <LoginForm onAuthenticated={handleAuth} />
      </div>
    );
  }

  return (
    <div className="app">
      <div className="status-line">
        <div>
          <h1>Query Execution Service</h1>
          <div className="role-toggle">
            Signed in as <strong>{role || "User"}</strong>
          </div>
        </div>

        <div className="flex">
          <button onClick={logout} style={{ background: "#ef4444" }}>
            Logout
          </button>
        </div>
      </div>

      <QueryWorkspace
        token={token}
        role={role}
        onUnauthorized={handleUnauthorized}
        API_BASE={API_BASE}
      />

      {role === "ADMIN" && (
        <AdminPanel token={token} onUnauthorized={handleUnauthorized} API_BASE={API_BASE} />
      )}
    </div>
  );
}

export default App;
