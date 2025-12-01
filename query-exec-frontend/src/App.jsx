import React from "react";

const API_BASE = "";

/* ---------------------------------------------------
 * JWT Helpers
 * --------------------------------------------------- */
function decodeRole(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    return payload.role || payload.authorities || null;
  } catch (_) { return null; }
}

function isTokenExpired(token) {
  try {
    const payload = JSON.parse(atob(token.split('.')[1]));
    if (!payload.exp) return false;
    return payload.exp * 1000 < Date.now();
  } catch (_) { return false; }
}

/* ---------------------------------------------------
 * Hook: Polling job
 * --------------------------------------------------- */
const usePollingJob = (token, jobId, onComplete, onUnauthorized) => {
  React.useEffect(() => {
    if (!jobId || !token) return;
    let cancelled = false;

    const poll = async () => {
      try {
        const res = await fetch(`${API_BASE}/queries/job/${jobId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (res.status === 401) {
          if (onUnauthorized) onUnauthorized();
          return;
        }

        const data = await res.json();
        if (cancelled) return;

        onComplete(data);

        if (data.status === "RUNNING") {
          setTimeout(poll, 1000);
        }

      } catch (err) {
        if (!cancelled) {
          onComplete({ status: "FAILED", error: err.message });
        }
      }
    };

    poll();
    return () => { cancelled = true; };

  }, [token, jobId, onComplete]);
};

/* ---------------------------------------------------
 * JobItem
 * --------------------------------------------------- */
const JobItem = ({ job, token, onUpdate, onUnauthorized }) => {
  const handleJobUpdate = React.useCallback((data) => {
    onUpdate(job.id, data);
  }, [job.id, onUpdate]);

  usePollingJob(token, job.id, handleJobUpdate, onUnauthorized);

  return (
    <div className="card stack">
      <div className="status-line">
        <div>
          <div className="muted">Job #{job.id}</div>
          <div>{job.message || job.result || job.error || ""}</div>
        </div>

        {job.status === "RUNNING" && <span className="pill info">Running...</span>}
        {job.status === "SUCCEEDED" && <span className="pill success">Succeeded</span>}
        {job.status === "FAILED" && <span className="pill error">Failed</span>}
      </div>

      {job.error && <div className="pill error">{job.error}</div>}
      {job.result && <div className="panel">{job.result}</div>}
    </div>
  );
};

/* ---------------------------------------------------
 * LoginForm
 * --------------------------------------------------- */
function LoginForm({ onAuthenticated }) {
  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [error, setError] = React.useState(null);
  const [loading, setLoading] = React.useState(false);

  const handleSubmit = async (e) => {
    e.preventDefault();
    setLoading(true);
    setError(null);

    try {
      const res = await fetch(`${API_BASE}/auth/login`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });

      const data = await res.json();

      if (!res.ok) throw new Error(data.message || data.error || "Login failed");

      onAuthenticated(data.token);

    } catch (err) {
      setError(err.message);
    } finally {
      setLoading(false);
    }
  };

  return (
    <div className="card" style={{ maxWidth: 460, margin: "0 auto" }}>
      <h2>Sign in</h2>
      <form className="stack" onSubmit={handleSubmit}>
        <div>
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} required />
        </div>

        <div>
          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} required />
        </div>

        {error && <div className="pill error">{error}</div>}

        <button type="submit" disabled={loading}>
          {loading ? "Signing in..." : "Login"}
        </button>
      </form>
    </div>
  );
}

/* ---------------------------------------------------
 * Query Workspace
 * --------------------------------------------------- */
function QueryWorkspace({ token, role, onUnauthorized }) {
  const [queries, setQueries] = React.useState([]);
  const [newQuery, setNewQuery] = React.useState("");
  const [createError, setCreateError] = React.useState(null);
  const [executeTarget, setExecuteTarget] = React.useState("");
  const [jobs, setJobs] = React.useState([]);
  const [userMsg, setUserMsg] = React.useState(null);

  const fetchQueries = React.useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/queries`, {
        headers: { Authorization: `Bearer ${token}` }
      });

      if (res.status === 401) {
        if (onUnauthorized) onUnauthorized();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Failed to load queries");
      setQueries(data);

    } catch (err) {
      setUserMsg({ type: "error", text: err.message });
    }
  }, [token]);

  React.useEffect(() => { fetchQueries(); }, [fetchQueries]);

  const submitQuery = async () => {
    setCreateError(null);

    try {
      const res = await fetch(`${API_BASE}/queries`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "text/plain",
        },
        body: newQuery
      });

      if (res.status === 401) {
        if (onUnauthorized) onUnauthorized();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Unable to save query");

      setNewQuery("");
      setExecuteTarget(data.id);
      setUserMsg({ type: "info", text: "Query saved successfully." });

      fetchQueries();

    } catch (err) {
      setCreateError(err.message);
    }
  };

  const executeQuery = async () => {
    if (!executeTarget) return;

    setUserMsg(null);
    const pendingJob = { id: null, status: "RUNNING", message: "Submitting job..." };
    setJobs(prev => [...prev, pendingJob]);

    try {
      const res = await fetch(`${API_BASE}/queries/execute?queryId=${executeTarget}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.status === 401) {
        if (onUnauthorized) onUnauthorized();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Failed to execute");

      setJobs(prev => prev.map(job => job === pendingJob
        ? { ...job, id: data.jobId, status: data.status, message: "Job submitted." }
        : job
      ));

    } catch (err) {
      setJobs(prev => prev.map(job => job === pendingJob
        ? { ...job, status: "FAILED", error: err.message }
        : job
      ));
      setUserMsg({ type: "error", text: err.message });
    }
  };

  const handleJobUpdate = React.useCallback((jobId, data) => {
    setJobs(prev => prev.map(job =>
      job.id === jobId ? { ...job, ...data } : job
    ));

    if (data.status && data.status !== "RUNNING") {
      setUserMsg({
        type: data.status === "SUCCEEDED" ? "success" : "error",
        text: data.error || data.message || `Job ${data.status.toLowerCase()}`
      });
    }
  }, []);

  return (
    <div className="stack">
      <div className="card">
        <div className="status-line">
          <h2>Queries</h2>
          <button onClick={fetchQueries}>Refresh</button>
        </div>

        <div className="panel">
          {queries.length === 0 ? (
            <div className="muted">No queries yet.</div>
          ) : (
            queries.map(q => (
              <div key={q.id} style={{ marginBottom: 10 }}>
                <div className="muted">#{q.id}</div>
                <div style={{ fontWeight: 600 }}>{q.query}</div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="grid">
        <div className="card stack">
          <label>Store a new query</label>
          <textarea rows="5" value={newQuery}
            onChange={(e) => setNewQuery(e.target.value)}
            placeholder="Write SQL here..."
          />
          {createError && (
            <div className="pill error" style={{ marginTop: 8 }}>{createError}</div>
          )}
          <button onClick={submitQuery} disabled={!newQuery.trim()}>
            Save query
          </button>
        </div>

        <div className="card stack">
          <label>Select query to execute</label>
          <select value={executeTarget} onChange={(e) => setExecuteTarget(e.target.value)}>
            <option value="">Choose query</option>
            {queries.map(q => (
              <option key={q.id} value={q.id}>
                #{q.id} — {q.query.slice(0, 35)}
              </option>
            ))}
          </select>
          <button onClick={executeQuery} disabled={!executeTarget}>Run query</button>
        </div>
      </div>

      <div className="card stack">
        <div className="status-line">
          <div className="muted">Jobs</div>
          <div className="pill info">{jobs.length} total</div>
        </div>

        {jobs.length === 0 && <div className="muted">No jobs yet.</div>}

        {jobs.map((job, idx) => (
          job.id ? (
            <JobItem
              key={job.id}
              job={job}
              token={token}
              onUpdate={handleJobUpdate}
              onUnauthorized={onUnauthorized}
            />
          ) : (
            <div className="card stack" key={`pending-${idx}`}>
              <div className="status-line">
                <div className="muted">Submitting job...</div>
                {job.status === "FAILED" && <span className="pill error">Failed</span>}
              </div>
              {job.error && <div className="pill error">{job.error}</div>}
            </div>
          )
        ))}
      </div>

      {userMsg && (
        <div className={`pill ${userMsg.type}`}>
          {userMsg.text}
        </div>
      )}
    </div>
  );
}

/* ---------------------------------------------------
 * Admin Panel
 * --------------------------------------------------- */
function AdminPanel({ token, onUnauthorized }) {
  const [username, setUsername] = React.useState("");
  const [password, setPassword] = React.useState("");
  const [role, setRole] = React.useState("USER");
  const [feedback, setFeedback] = React.useState(null);

  const createUser = async () => {
    setFeedback(null);

    try {
      const res = await fetch(`${API_BASE}/admin/users`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "application/json",
        },
        body: JSON.stringify({ username, password, role })
      });

      if (res.status === 401) {
        if (onUnauthorized) onUnauthorized();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Failed to create user");

      setFeedback({ type: "success", text: `User ${data.username} created.` });
      setUsername("");
      setPassword("");
      setRole("USER");

    } catch (err) {
      setFeedback({ type: "error", text: err.message });
    }
  };

  return (
    <div className="card" style={{ marginTop: 16 }}>
      <h2>Admin — Create user</h2>

      <div className="grid">
        <div className="stack">
          <label>Username</label>
          <input value={username} onChange={(e) => setUsername(e.target.value)} />

          <label>Password</label>
          <input type="password" value={password} onChange={(e) => setPassword(e.target.value)} />

          <label>Role</label>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>

          <button onClick={createUser} disabled={!username || !password}>
            Create user
          </button>

          {feedback && (
            <div className={`pill ${feedback.type}`}>
              {feedback.text}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}

/* ---------------------------------------------------
 * APP ROOT
 * --------------------------------------------------- */
export default function App() {
  const [token, setToken] = React.useState(null);
  const [role, setRole] = React.useState(token ? decodeRole(token) : null);
  const [sessionMessage, setSessionMessage] = React.useState(null);

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

  React.useEffect(() => {
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
      />

      {role === "ADMIN" && (
        <AdminPanel token={token} onUnauthorized={handleUnauthorized} />
      )}
    </div>
  );
}
