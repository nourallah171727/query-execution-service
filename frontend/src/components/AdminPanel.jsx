import { useState } from 'react';
import { apiFetch } from '../lib/api.js';

export default function AdminPanel({ token, onUnauthorized }) {
  const [username, setUsername] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState('USER');
  const [feedback, setFeedback] = useState(null);

  const createUser = async () => {
    setFeedback(null);
    try {
      const data = await apiFetch(
        '/admin/users',
        {
          token,
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ username, password, role }),
        },
        { onUnauthorized },
      );
      setFeedback({ type: 'success', text: `User ${data.username} created.` });
      setUsername('');
      setPassword('');
      setRole('USER');
    } catch (err) {
      setFeedback({ type: 'error', text: err.message });
    }
  };

  return (
    <div className="card" style={{ marginTop: 16 }}>
      <h2>Admin — Create user</h2>
      <div className="grid">
        <div className="stack">
          <div>
            <label htmlFor="new-username">Username</label>
            <input
              id="new-username"
              value={username}
              onChange={(e) => setUsername(e.target.value)}
              placeholder="new username"
            />
          </div>
          <div>
            <label htmlFor="new-password">Password</label>
            <input
              id="new-password"
              type="password"
              value={password}
              onChange={(e) => setPassword(e.target.value)}
              placeholder="temporary password"
            />
          </div>
          <div>
            <label htmlFor="role">Role</label>
            <select id="role" value={role} onChange={(e) => setRole(e.target.value)}>
              <option value="USER">USER</option>
              <option value="ADMIN">ADMIN</option>
            </select>
          </div>
          <button onClick={createUser} disabled={!username || !password}>
            Create user
          </button>
          {feedback && (
            <div className={`pill ${feedback.type === 'success' ? 'success' : 'error'}`}>
              {feedback.text}
            </div>
          )}
        </div>
      </div>
    </div>
  );
}
