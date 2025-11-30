import { useEffect } from 'react';
import { apiFetch } from '../lib/api.js';

export function usePollingJob(token, jobId, onComplete, onUnauthorized) {
  useEffect(() => {
    if (!jobId || !token) return undefined;
    let cancelled = false;

    const poll = async () => {
      try {
        const data = await apiFetch(`/queries/job/${jobId}`, { token }, { onUnauthorized });
        if (cancelled) return;
        onComplete(data);
        if (data.status === 'RUNNING') {
          setTimeout(poll, 1000);
        }
      } catch (error) {
        if (!cancelled) {
          onComplete({ status: 'FAILED', error: error.message });
        }
      }
    };

    poll();
    return () => {
      cancelled = true;
    };
  }, [jobId, token, onComplete, onUnauthorized]);
}
