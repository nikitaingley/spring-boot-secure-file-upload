import { useEffect, useRef, useState } from "react";

const API = "http://localhost:8080";
const MAX_MB = 10;

const PROVIDERS = [
  { id: "azure", label: "Upload to Azure", cls: "btn-azure" },
  { id: "aws", label: "Upload to AWS", cls: "btn-aws" },
];

const REPLICA = {
  DONE: { text: "Replicated", cls: "st-ok" },
  PENDING: { text: "Syncing…", cls: "st-wait" },
  FAILED: { text: "Failed", cls: "st-bad" },
};

const fmt = (b = 0) =>
    b < 1024 * 1024 ? `${Math.max(1, Math.round(b / 1024))} KB` : `${(b / 1024 / 1024).toFixed(1)} MB`;

function kind(type = "") {
  if (type.includes("pdf")) return { label: "PDF", cls: "k-pdf" };
  if (type.startsWith("image/")) return { label: "IMG", cls: "k-img" };
  return { label: "FILE", cls: "k-file" };
}

export default function App() {
  const [theme, setTheme] = useState(
      localStorage.getItem("theme") ||
      (window.matchMedia("(prefers-color-scheme: dark)").matches ? "dark" : "light")
  );
  const [token, setToken] = useState(sessionStorage.getItem("token") || "");
  const [user, setUser] = useState(sessionStorage.getItem("user") || "");
  const [mode, setMode] = useState("login");
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [authErr, setAuthErr] = useState("");
  const [files, setFiles] = useState([]);
  const [health, setHealth] = useState({ aws: true, azure: true });
  const [picked, setPicked] = useState(null);
  const [busy, setBusy] = useState("");
  const [over, setOver] = useState(false);
  const [toast, setToast] = useState(null);
  const inputRef = useRef(null);

  const authHeader = { Authorization: `Bearer ${token}` };
  const notify = (text, type = "info") => setToast({ text, type });

  useEffect(() => {
    document.documentElement.dataset.theme = theme;
    localStorage.setItem("theme", theme);
  }, [theme]);

  useEffect(() => {
    if (!toast) return;
    const t = setTimeout(() => setToast(null), 4000);
    return () => clearTimeout(t);
  }, [toast]);

  async function loadFiles(t = token) {
    try {
      const res = await fetch(`${API}/api/v1/storage/files`, { headers: { Authorization: `Bearer ${t}` } });
      if (res.status === 401) { logout(); notify("Session expired, please log in again", "error"); return; }
      if (res.ok) setFiles(await res.json());
    } catch {
      notify("Cannot reach the server. Is the backend running?", "error");
    }
  }

  // Poll which clouds are reachable
  useEffect(() => {
    if (!token) return;
    let stop = false;
    const tick = async () => {
      try {
        const res = await fetch(`${API}/api/v1/storage/health`, { headers: { Authorization: `Bearer ${token}` } });
        if (res.ok && !stop) setHealth(await res.json());
      } catch { /* backend down */ }
    };
    tick();
    const id = setInterval(tick, 5000);
    return () => { stop = true; clearInterval(id); };
  }, [token]);

  // Load files after login, and again whenever a cloud goes up or down (shows failover)
  useEffect(() => {
    if (token) loadFiles(token);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [token, health.aws, health.azure]);

  // While a replica is still being copied, refresh to update its badge
  useEffect(() => {
    if (!files.some((f) => f.replicaStatus === "PENDING")) return;
    const t = setTimeout(() => loadFiles(), 2500);
    return () => clearTimeout(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [files]);

  async function submitAuth(e) {
    e.preventDefault();
    setAuthErr("");
    try {
      const res = await fetch(`${API}/api/auth/${mode}`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        body: JSON.stringify({ username, password }),
      });
      if (!res.ok) {
        setAuthErr(mode === "login"
            ? "Invalid username or password"
            : "Could not register (username taken, or password under 8 characters)");
        return;
      }
      const data = await res.json();
      sessionStorage.setItem("token", data.token);
      sessionStorage.setItem("user", data.username);
      setToken(data.token);
      setUser(data.username);
      setPassword("");
    } catch {
      setAuthErr("Cannot reach the server. Is the backend running?");
    }
  }

  function logout() {
    sessionStorage.clear();
    setToken(""); setUser(""); setFiles([]); setPicked(null);
  }

  function pick(file) {
    if (!file) return;
    if (file.size > MAX_MB * 1024 * 1024) { notify(`File is larger than ${MAX_MB} MB`, "error"); return; }
    setPicked(file);
  }

  async function upload(providerId) {
    if (!picked) { notify("Choose a file first", "error"); return; }
    setBusy(providerId);
    const body = new FormData();
    body.append("file", picked);
    try {
      const res = await fetch(`${API}/api/v1/storage/upload?provider=${providerId}`, {
        method: "POST", headers: authHeader, body,
      });
      if (res.status === 401) { logout(); notify("Session expired, please log in again", "error"); return; }
      if (!res.ok) { notify("Upload rejected (file type, size, virus check, or no cloud reachable)", "error"); return; }
      const doc = await res.json();
      setPicked(null);
      const wanted = providerId.toUpperCase();
      if (doc.provider && doc.provider !== wanted) {
        notify(`${wanted} is unreachable, stored in ${doc.provider} instead`, "info");
      } else {
        notify(`Uploaded to ${doc.provider || wanted}`, "success");
      }
      await loadFiles();
    } catch {
      notify("Cannot reach the server. Is the backend running?", "error");
    } finally {
      setBusy("");
    }
  }

  async function remove(f) {
    if (!window.confirm(`Delete "${f.name}" from both clouds? This cannot be undone.`)) return;
    const res = await fetch(`${API}/api/v1/storage/delete/${f.id}`, { method: "DELETE", headers: authHeader });
    if (res.ok) { setFiles((list) => list.filter((d) => d.id !== f.id)); notify("File deleted", "success"); }
    else notify("Delete failed", "error");
  }

  const themeBtn = (
      <button className="theme-btn" title="Toggle dark / light mode"
              onClick={() => setTheme(theme === "dark" ? "light" : "dark")}>
        {theme === "dark" ? "☀️" : "🌙"}
      </button>
  );

  const toastEl = toast && <div className={`toast ${toast.type}`}>{toast.text}</div>;

  if (!token) {
    return (
        <div className="auth-wrap">
          <div className="auth-top">{themeBtn}</div>
          <form className="panel auth-card" onSubmit={submitAuth}>
            <div className="brand"><div className="logo">🔐</div> Secure File Vault</div>
            <div className="tabs">
              <button type="button" className={`tab ${mode === "login" ? "active" : ""}`}
                      onClick={() => { setMode("login"); setAuthErr(""); }}>Log in</button>
              <button type="button" className={`tab ${mode === "register" ? "active" : ""}`}
                      onClick={() => { setMode("register"); setAuthErr(""); }}>Register</button>
            </div>
            <input className="field" placeholder="Username" value={username}
                   onChange={(e) => setUsername(e.target.value)} />
            <input className="field" type="password" placeholder="Password (min 8 characters)" value={password}
                   onChange={(e) => setPassword(e.target.value)} />
            <button className="btn btn-primary" style={{ width: "100%", justifyContent: "center" }} type="submit">
              {mode === "login" ? "Log in" : "Create account"}
            </button>
            <div className="err">{authErr}</div>
          </form>
        </div>
    );
  }

  const totalSize = files.reduce((a, f) => a + (f.size || 0), 0);

  return (
      <>
        <nav className="nav">
          <div className="brand"><div className="logo">🔐</div> Secure File Vault</div>
          <div className="nav-right">
            <span className="chip">👤 {user}</span>
            {themeBtn}
            <button className="btn btn-ghost btn-sm" onClick={logout}>Log out</button>
          </div>
        </nav>

        <div className="container">
          <div className="stats">
            <div className="stat"><div className="label">Files</div><div className="value">{files.length}</div></div>
            <div className="stat"><div className="label">Storage used</div><div className="value">{fmt(totalSize)}</div></div>
            <div className="stat">
              <div className="label">Cloud status</div>
              <div className="value small">
                <span><span className={`dot ${health.aws ? "up" : "down"}`} />AWS</span>
                <span><span className={`dot ${health.azure ? "up" : "down"}`} />Azure</span>
              </div>
            </div>
          </div>

          <div className="panel">
            <h3>Upload a file</h3>
            <div className={`drop ${over ? "over" : ""}`}
                 onClick={() => inputRef.current.click()}
                 onDragOver={(e) => { e.preventDefault(); setOver(true); }}
                 onDragLeave={() => setOver(false)}
                 onDrop={(e) => { e.preventDefault(); setOver(false); pick(e.dataTransfer.files[0]); }}>
              <div className="big">☁️</div>
              <div><b>Click to choose</b> or drag a file here</div>
              <div className="hint">PDF, PNG or JPG, up to {MAX_MB} MB. Files are type-checked and virus-scanned.</div>
            </div>
            <input ref={inputRef} type="file" hidden
                   onChange={(e) => { pick(e.target.files[0]); e.target.value = ""; }} />

            {picked && (
                <div className="picked">
                  <span>📄 <b>{picked.name}</b> ({fmt(picked.size)})</span>
                  <button className="btn btn-ghost btn-sm" onClick={() => setPicked(null)}>Remove</button>
                </div>
            )}

            <div className="actions">
              {PROVIDERS.map((p) => (
                  <button key={p.id} className={`btn ${p.cls}`}
                          disabled={!picked || busy !== ""}
                          onClick={() => upload(p.id)}>
                    {busy === p.id ? <span className="spinner" /> : "⬆"} {p.label}
                  </button>
              ))}
            </div>
            <div className="note">
              The file goes to the cloud you choose, then is copied to the other cloud in the background.
              If one cloud is down, the other one serves your files.
            </div>
          </div>

          <div className="panel">
            <h3>My files</h3>
            {files.length === 0 ? (
                <div className="empty">You have no files yet. Upload one above.</div>
            ) : (
                <div className="table-wrap">
                  <table>
                    <thead>
                    <tr>
                      <th>Name</th><th>Size</th><th>Stored in</th><th>Replica</th>
                      <th style={{ textAlign: "right" }}>Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    {files.map((f) => {
                      const k = kind(f.type);
                      const r = REPLICA[f.replicaStatus] || { text: "—", cls: "st-none" };
                      const failover = f.servedFrom && f.servedFrom !== f.provider;
                      return (
                          <tr key={f.id}>
                            <td><div className="fname"><span className={`badge ${k.cls}`}>{k.label}</span>{f.name}</div></td>
                            <td>{fmt(f.size)}</td>
                            <td>
                              <span className={`cloud cloud-${(f.provider || "azure").toLowerCase()}`}>{f.provider || "AZURE"}</span>
                              {failover && <span className="cloud cloud-failover">⚠ served from {f.servedFrom}</span>}
                            </td>
                            <td><span className={`st ${r.cls}`}>{r.text}</span></td>
                            <td>
                              <div className="row-actions">
                                {f.url ? (
                                    <a className="btn btn-open btn-sm" href={f.url} target="_blank" rel="noreferrer">Open</a>
                                ) : (
                                    <button className="btn btn-ghost btn-sm" disabled>Unavailable</button>
                                )}
                                <button className="btn btn-danger btn-sm" onClick={() => remove(f)}>Delete</button>
                              </div>
                            </td>
                          </tr>
                      );
                    })}
                    </tbody>
                  </table>
                </div>
            )}
          </div>
        </div>
        {toastEl}
      </>
  );
}