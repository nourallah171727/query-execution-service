import React from "react";

export default function AdminPanel({ token, onUnauthorized, API_BASE }) {
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
        body: JSON.stringify({ username, password, role }),
      });

      if (res.status === 401) {
        onUnauthorized?.();
        return;
      }

      const data = await res.json();
      if (!res.ok) {
        throw new Error(data.message || data.error || "Failed to create user");
      }

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
          <input
            value={username}
            onChange={(e) => setUsername(e.target.value)}
            placeholder="new username"
          />

          <label>Password</label>
          <input
            type="password"
            value={password}
            onChange={(e) => setPassword(e.target.value)}
            placeholder="temporary password"
          />

          <label>Role</label>
          <select value={role} onChange={(e) => setRole(e.target.value)}>
            <option value="USER">USER</option>
            <option value="ADMIN">ADMIN</option>
          </select>

          <button onClick={createUser} disabled={!username || !password}>
            Create user
          </button>

          {feedback && (
            <div className={`pill ${feedback.type}`}>{feedback.text}</div>
          )}
        </div>
      </div>
    </div>
  );
}
