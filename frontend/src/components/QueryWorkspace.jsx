import { useCallback, useEffect, useState } from 'react';
import { apiFetch } from '../lib/api.js';
import { usePollingJob } from '../hooks/usePollingJob.js';

const statusPill = (state) => {
  if (!state) return null;
  if (state.status === 'RUNNING') return <span className="pill info">Running...</span>;
  if (state.status === 'SUCCEEDED') return <span className="pill success">Succeeded</span>;
  return <span className="pill error">Failed</span>;
};

export default function QueryWorkspace({ token, role, onUnauthorized }) {
  const [queries, setQueries] = useState([]);
  const [newQuery, setNewQuery] = useState('');
  const [createError, setCreateError] = useState(null);
  const [executeTarget, setExecuteTarget] = useState('');
  const [jobId, setJobId] = useState(null);
  const [jobState, setJobState] = useState(null);
  const [userMsg, setUserMsg] = useState(null);

  const fetchQueries = useCallback(async () => {
    try {
      const data = await apiFetch('/queries', { token }, { onUnauthorized });
      setQueries(data);
    } catch (err) {
      setUserMsg({ type: 'error', text: err.message });
    }
  }, [token, onUnauthorized]);

  useEffect(() => {
    fetchQueries();
  }, [fetchQueries]);

  const submitQuery = async () => {
    setCreateError(null);
    try {
      const data = await apiFetch(
        '/queries',
        {
          token,
          method: 'POST',
          headers: { 'Content-Type': 'text/plain' },
          body: newQuery,
        },
        { onUnauthorized },
      );
      setNewQuery('');
      setExecuteTarget(data.id);
      setUserMsg({ type: 'info', text: 'Query saved successfully.' });
      fetchQueries();
    } catch (err) {
      setCreateError(err.message);
    }
  };

  const executeQuery = async () => {
    if (!executeTarget) return;
    setUserMsg(null);
    setJobState({ status: 'RUNNING', message: 'Submitting job...' });
    try {
      const data = await apiFetch(
        `/queries/execute?queryId=${executeTarget}`,
        {
          token,
          method: 'POST',
        },
        { onUnauthorized },
      );
      setJobId(data.jobId);
      setJobState({ status: data.status, message: 'Job submitted.' });
    } catch (err) {
      setJobId(null);
      setJobState({ status: 'FAILED', error: err.message });
      setUserMsg({ type: 'error', text: err.message });
    }
  };

  const handleJobUpdate = useCallback((data) => {
    setJobState(data);
    if (data.status && data.status !== 'RUNNING') {
      setUserMsg({
        type: data.status === 'SUCCEEDED' ? 'success' : 'error',
        text: data.error || data.message || `Job ${data.status.toLowerCase()}`,
      });
    }
  }, []);

  usePollingJob(token, jobId, handleJobUpdate, onUnauthorized);

  return (
    <div className="stack">
      <div className="card">
        <div className="status-line">
          <h2>Queries</h2>
          <button onClick={fetchQueries}>Refresh</button>
        </div>
        <div className="panel" aria-label="Existing queries">
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
        <div className="card stack">
          <div>
            <label htmlFor="new-query">Store a new query</label>
            <textarea
              id="new-query"
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
        </div>

        <div className="card stack">
          <div className="stack">
            <div>
              <label htmlFor="query-select">Select query to execute</label>
              <select
                id="query-select"
                value={executeTarget}
                onChange={(e) => setExecuteTarget(e.target.value)}
              >
                <option value="">Choose query</option>
                {queries.map((q) => (
                  <option key={q.id} value={q.id}>
                    #{q.id} — {q.query.slice(0, 35)}
                  </option>
                ))}
              </select>
            </div>
            <button onClick={executeQuery} disabled={!executeTarget}>
              Run query
            </button>
            <div className="divider" />
            <div className="status-line">
              <div>
                <div className="muted">Job status</div>
                <div>{jobId ? `Job #${jobId}` : 'No job yet'}</div>
              </div>
              {statusPill(jobState)}
            </div>
            {jobState && jobState.error && <div className="pill error">{jobState.error}</div>}
            {jobState && jobState.result && <div className="panel">{jobState.result}</div>}
            {jobState && jobState.message && !jobState.result && (
              <div className="muted">{jobState.message}</div>
            )}
          </div>
        </div>
      </div>

      {userMsg && (
        <div className={`pill ${userMsg.type === 'success' ? 'success' : userMsg.type === 'error' ? 'error' : 'info'}`}>
          {userMsg.text}
        </div>
      )}

      {role === 'ADMIN' && (
        <div className="muted">Admin capabilities available below.</div>
      )}
    </div>
  );
}
