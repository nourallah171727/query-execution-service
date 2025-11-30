import React from "react";

export default function QueryWorkspace({ token, role, onUnauthorized, API_BASE }) {
  const [queries, setQueries] = React.useState([]);
  const [newQuery, setNewQuery] = React.useState("");
  const [createError, setCreateError] = React.useState(null);
  const [executeTarget, setExecuteTarget] = React.useState("");
  const [jobId, setJobId] = React.useState(null);
  const [jobState, setJobState] = React.useState(null);
  const [userMsg, setUserMsg] = React.useState(null);

  /* ---------------------------------------------------
   * Helper: Fetch queries
   * --------------------------------------------------- */
  const fetchQueries = React.useCallback(async () => {
    try {
      const res = await fetch(`${API_BASE}/queries`, {
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.status === 401) {
        onUnauthorized?.();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Failed to load queries");

      setQueries(data);
    } catch (err) {
      setUserMsg({ type: "error", text: err.message });
    }
  }, [token, API_BASE]);

  React.useEffect(() => {
    fetchQueries();
  }, [fetchQueries]);

  /* ---------------------------------------------------
   * Submit new query
   * --------------------------------------------------- */
  const submitQuery = async () => {
    setCreateError(null);

    try {
      const res = await fetch(`${API_BASE}/queries`, {
        method: "POST",
        headers: {
          Authorization: `Bearer ${token}`,
          "Content-Type": "text/plain",
        },
        body: newQuery,
      });

      if (res.status === 401) {
        onUnauthorized?.();
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

  /* ---------------------------------------------------
   * Execute query → triggers async job
   * --------------------------------------------------- */
  const executeQuery = async () => {
    if (!executeTarget) return;

    setUserMsg(null);
    setJobState({ status: "RUNNING", message: "Submitting job..." });

    try {
      const res = await fetch(`${API_BASE}/queries/execute?queryId=${executeTarget}`, {
        method: "POST",
        headers: { Authorization: `Bearer ${token}` },
      });

      if (res.status === 401) {
        onUnauthorized?.();
        return;
      }

      const data = await res.json();
      if (!res.ok) throw new Error(data.message || data.error || "Failed to execute");

      setJobId(data.jobId);
      setJobState({ status: data.status, message: "Job submitted." });
    } catch (err) {
      setJobId(null);
      setJobState({ status: "FAILED", error: err.message });
      setUserMsg({ type: "error", text: err.message });
    }
  };

  /* ---------------------------------------------------
   * Polling helper
   * --------------------------------------------------- */
  const handleJobUpdate = React.useCallback((data) => {
    setJobState(data);

    if (data.status && data.status !== "RUNNING") {
      setUserMsg({
        type: data.status === "SUCCEEDED" ? "success" : "error",
        text: data.error || data.message || `Job ${data.status.toLowerCase()}`,
      });
    }
  }, []);

  React.useEffect(() => {
    if (!jobId || !token) return;

    let cancelled = false;

    const poll = async () => {
      try {
        const res = await fetch(`${API_BASE}/queries/job/${jobId}`, {
          headers: { Authorization: `Bearer ${token}` },
        });

        if (res.status === 401) {
          onUnauthorized?.();
          return;
        }

        const data = await res.json();
        if (cancelled) return;

        handleJobUpdate(data);

        // Continue polling
        if (data.status === "RUNNING") {
          setTimeout(poll, 1000);
        }
      } catch (err) {
        if (!cancelled) {
          handleJobUpdate({ status: "FAILED", error: err.message });
        }
      }
    };

    poll();
    return () => (cancelled = true);
  }, [jobId, token, API_BASE, handleJobUpdate]);


  /* ---------------------------------------------------
   * Status pill helper
   * --------------------------------------------------- */
  const StatusPill = ({ state }) => {
    if (!state) return null;
    if (state.status === "RUNNING") return <span className="pill info">Running...</span>;
    if (state.status === "SUCCEEDED") return <span className="pill success">Succeeded</span>;
    return <span className="pill error">Failed</span>;
  };


  /* ---------------------------------------------------
   * Render UI
   * --------------------------------------------------- */
  return (
    <div className="stack">
      {/* Queries list */}
      <div className="card">
        <div className="status-line">
          <h2>Queries</h2>
          <button onClick={fetchQueries}>Refresh</button>
        </div>

        <div className="panel">
          {queries.length === 0 ? (
            <div className="muted">No queries yet.</div>
          ) : (
            queries.map((q) => (
              <div key={q.id} style={{ marginBottom: 10 }}>
                <div className="muted">#{q.id}</div>
                <div style={{ fontWeight: 600 }}>{q.query}</div>
              </div>
            ))
          )}
        </div>
      </div>

      <div className="grid">
        {/* Store new query */}
        <div className="card stack">
          <label>Store a new query</label>
          <textarea
            rows="5"
            value={newQuery}
            onChange={(e) => setNewQuery(e.target.value)}
            placeholder="Write SQL here..."
          />
          {createError && <div className="pill error" style={{ marginTop: 8 }}>{createError}</div>}

          <button onClick={submitQuery} disabled={!newQuery.trim()}>
            Save query
          </button>
        </div>

        {/* Execute query */}
        <div className="card stack">
          <label>Select query to execute</label>
          <select value={executeTarget} onChange={(e) => setExecuteTarget(e.target.value)}>
            <option value="">Choose query</option>
            {queries.map((q) => (
              <option key={q.id} value={q.id}>
                #{q.id} — {q.query.slice(0, 35)}
              </option>
            ))}
          </select>

          <button onClick={executeQuery} disabled={!executeTarget}>
            Run query
          </button>

          <div className="divider" />

          <div className="status-line">
            <div>
              <div className="muted">Job status</div>
              <div>{jobId ? `Job #${jobId}` : "No job yet"}</div>
            </div>

            <StatusPill state={jobState} />
          </div>

          {jobState?.error && <div className="pill error">{jobState.error}</div>}
          {jobState?.result && <div className="panel">{jobState.result}</div>}
          {jobState?.message && !jobState.result && <div className="muted">{jobState.message}</div>}
        </div>
      </div>

      {userMsg && (
        <div className={`pill ${userMsg.type}`}>
          {userMsg.text}
        </div>
      )}
    </div>
  );
}
