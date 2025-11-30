const API_BASE = import.meta.env.VITE_API_BASE_URL || '';

const buildHeaders = (token, additionalHeaders = {}) => ({
  ...(token ? { Authorization: `Bearer ${token}` } : {}),
  ...additionalHeaders,
});

export async function apiFetch(path, { token, method = 'GET', headers, body } = {}, { onUnauthorized } = {}) {
  const response = await fetch(`${API_BASE}${path}`, {
    method,
    headers: buildHeaders(token, headers),
    body,
  });

  if (response.status === 401 || response.status === 403) {
    onUnauthorized?.();
  }

  let data = null;
  try {
    data = await response.json();
  } catch (error) {
    // Ignore json parse errors for empty responses
  }

  if (!response.ok) {
    throw new Error(data?.message || data?.error || response.statusText);
  }

  return data;
}
