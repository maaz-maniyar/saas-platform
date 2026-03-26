import { useEffect, useMemo, useState } from "react";
import { api } from "./api";

const roleOptions = [
  { value: "SUPER_ADMIN", label: "Super Admin", requiresTenant: false },
  { value: "TENANT_ADMIN", label: "Tenant Admin", requiresTenant: true },
  { value: "TESTER", label: "Tester", requiresTenant: true },
  { value: "DEV", label: "Developer", requiresTenant: true }
];

const emptyBugForm = {
  title: "",
  description: "",
  assignedTo: "",
  videoFile: null
};

function normalizeRole(session) {
  if (session.role === "USER" && session.userType === "TESTER") {
    return "TESTER";
  }

  if (session.role === "USER" && session.userType === "DEV") {
    return "DEV";
  }

  return session.role;
}

function App() {
  const [selectedRole, setSelectedRole] = useState("TESTER");
  const [credentials, setCredentials] = useState({
    email: "",
    password: "",
    tenantId: ""
  });
  const [session, setSession] = useState(() => {
    const saved = localStorage.getItem("saas-platform-session");
    return saved ? JSON.parse(saved) : null;
  });
  const [bugs, setBugs] = useState([]);
  const [users, setUsers] = useState([]);
  const [bugForm, setBugForm] = useState(emptyBugForm);
  const [rowUploadFiles, setRowUploadFiles] = useState({});
  const [busy, setBusy] = useState(false);
  const [uploadingBugId, setUploadingBugId] = useState(null);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  const resolvedRole = session ? normalizeRole(session) : null;

  useEffect(() => {
    if (session) {
      localStorage.setItem("saas-platform-session", JSON.stringify(session));
    } else {
      localStorage.removeItem("saas-platform-session");
    }
  }, [session]);

  useEffect(() => {
    if (!session) {
      return;
    }

    void loadDashboard(session);
  }, [session]);

  const assignableUsers = useMemo(() => {
    return users.filter((user) => {
      if (user.role === "TENANT_ADMIN") {
        return true;
      }

      return user.role === "USER" && user.userType === "DEV";
    });
  }, [users]);

  const stats = useMemo(() => {
    return {
      total: bugs.length,
      open: bugs.filter((bug) => bug.status === "OPEN").length,
      inProgress: bugs.filter((bug) => bug.status === "IN_PROGRESS").length,
      resolved: bugs.filter((bug) => bug.status === "RESOLVED").length
    };
  }, [bugs]);

  const canReadTenantUsers = resolvedRole === "TENANT_ADMIN";
  const canAssignFromDirectory = resolvedRole === "TENANT_ADMIN";

  async function loadDashboard(currentSession) {
    setBusy(true);
    setError("");

    try {
      if (normalizeRole(currentSession) === "SUPER_ADMIN") {
        const platformUsers = await api.getPlatformUsers(currentSession);
        setUsers(platformUsers);
        setBugs([]);
      } else if (normalizeRole(currentSession) === "TENANT_ADMIN") {
        const [tenantUsers, tenantBugs] = await Promise.all([
          api.getTenantUsers(currentSession),
          api.getBugs(currentSession)
        ]);
        setUsers(tenantUsers);
        setBugs(tenantBugs);
      } else {
        const tenantBugs = await api.getBugs(currentSession);
        setUsers([]);
        setBugs(tenantBugs);
      }
    } catch (loadError) {
      setError(loadError.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleLogin(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");

    try {
      const selected = roleOptions.find((role) => role.value === selectedRole);
      const payload = {
        email: credentials.email,
        password: credentials.password,
        tenantId: selected.requiresTenant ? credentials.tenantId : "public"
      };

      const response = await api.login(payload);
      const actualRole = normalizeRole(response);

      if (actualRole !== selectedRole) {
        throw new Error(`This account is ${actualRole}, not ${selectedRole}`);
      }

      setSession({
        token: response.accessToken,
        email: response.email,
        role: response.role,
        userType: response.userType,
        tenantId: response.tenantId
      });
      setMessage("Login successful.");
    } catch (loginError) {
      setError(loginError.message);
    } finally {
      setBusy(false);
    }
  }

  async function uploadBugVideo(bugId, file) {
    const uploadMeta = await api.generateBugVideoUploadUrl(session, bugId, file);

    try {
      await api.uploadFileToPresignedUrl(uploadMeta.uploadUrl, file);
      return await api.confirmBugVideoUpload(session, bugId, uploadMeta.s3Key);
    } catch (uploadError) {
      try {
        await api.resetBugVideo(session, bugId);
      } catch (resetError) {
        console.error("Failed to reset bug video state after upload failure", resetError);
      }

      if (uploadError instanceof Error) {
        throw uploadError;
      }

      throw new Error("Video upload failed.");
    }
  }

  async function handleCreateBug(event) {
    event.preventDefault();
    setBusy(true);
    setError("");
    setMessage("");

    try {
      const created = await api.createBug(session, {
        title: bugForm.title,
        description: bugForm.description,
        assignedTo: bugForm.assignedTo || null
      });

      setBugs((current) => [created, ...current]);
      let hydratedBug = created;

      if (bugForm.videoFile) {
        const videoMetadata = await uploadBugVideo(created.id, bugForm.videoFile);
        hydratedBug = { ...created, ...mapVideoMetadata(videoMetadata) };
        replaceBugInState(hydratedBug);
      }

      setBugForm(emptyBugForm);
      setMessage(
        bugForm.videoFile
          ? "Bug created and recording uploaded successfully."
          : "Bug created successfully."
      );
    } catch (createError) {
      setError(
        createError.message.includes("Direct upload to S3 failed")
          ? `Bug was created, but the video upload failed. ${createError.message}`
          : createError.message
      );
    } finally {
      setBusy(false);
    }
  }

  async function handleStatusChange(bugId, status) {
    setBusy(true);
    setError("");

    try {
      const updated = await api.patchBug(session, bugId, { status });
      replaceBugInState(updated);
      setMessage("Bug updated.");
    } catch (updateError) {
      setError(updateError.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleAssignmentChange(bugId, assignedTo) {
    setBusy(true);
    setError("");

    try {
      const updated = await api.patchBug(session, bugId, {
        assignedTo: assignedTo || null
      });
      replaceBugInState(updated);
      setMessage("Assignment updated.");
    } catch (updateError) {
      setError(updateError.message);
    } finally {
      setBusy(false);
    }
  }

  async function handleInlineVideoUpload(bugId) {
    const file = rowUploadFiles[bugId];

    if (!file) {
      setError("Choose a video file before uploading.");
      return;
    }

    setUploadingBugId(bugId);
    setError("");
    setMessage("");

    try {
      const videoMetadata = await uploadBugVideo(bugId, file);
      setBugs((current) =>
        current.map((bug) =>
          bug.id === bugId ? { ...bug, ...mapVideoMetadata(videoMetadata) } : bug
        )
      );
      setRowUploadFiles((current) => {
        const next = { ...current };
        delete next[bugId];
        return next;
      });
      setMessage("Recording uploaded successfully.");
    } catch (uploadError) {
      setBugs((current) =>
        current.map((bug) =>
          bug.id === bugId
            ? {
                ...bug,
                videoS3Key: null,
                videoUrl: null,
                videoContentType: null,
                videoSizeBytes: null,
                videoUploadStatus: "NOT_UPLOADED"
              }
            : bug
        )
      );
      setError(uploadError.message);
    } finally {
      setUploadingBugId(null);
    }
  }

  async function handleOpenVideo(bugId) {
    setError("");
    setMessage("");

    try {
      const response = await api.getBugVideoAccessUrl(session, bugId);
      window.open(response.accessUrl, "_blank", "noopener,noreferrer");
    } catch (openError) {
      setError(openError.message);
    }
  }

  function replaceBugInState(updatedBug) {
    setBugs((current) =>
      current.map((bug) => (bug.id === updatedBug.id ? updatedBug : bug))
    );
  }

  function handleLogout() {
    setSession(null);
    setUsers([]);
    setBugs([]);
    setBugForm(emptyBugForm);
    setRowUploadFiles({});
    setMessage("");
    setError("");
  }

  if (!session) {
    const selected = roleOptions.find((role) => role.value === selectedRole);

    return (
      <div className="app-shell">
        <section className="login-hero">
          <div className="hero-copy">
            <span className="eyebrow">Bug command center</span>
            <h1>Ship fixes faster with a focused dark-mode control room.</h1>
            <p>
              Sign in as super admin, tenant admin, tester, or developer and land
              on the right workflow immediately.
            </p>
          </div>

          <form className="login-card" onSubmit={handleLogin}>
            <h2>Sign in</h2>
            <label>
              Role
              <select
                value={selectedRole}
                onChange={(event) => setSelectedRole(event.target.value)}
              >
                {roleOptions.map((role) => (
                  <option key={role.value} value={role.value}>
                    {role.label}
                  </option>
                ))}
              </select>
            </label>

            <label>
              Email
              <input
                type="email"
                value={credentials.email}
                onChange={(event) =>
                  setCredentials((current) => ({
                    ...current,
                    email: event.target.value
                  }))
                }
                placeholder="you@example.com"
                required
              />
            </label>

            <label>
              Password
              <input
                type="password"
                value={credentials.password}
                onChange={(event) =>
                  setCredentials((current) => ({
                    ...current,
                    password: event.target.value
                  }))
                }
                placeholder="Enter your password"
                required
              />
            </label>

            {selected.requiresTenant && (
              <label>
                Tenant ID
                <input
                  type="text"
                  value={credentials.tenantId}
                  onChange={(event) =>
                    setCredentials((current) => ({
                      ...current,
                      tenantId: event.target.value
                    }))
                  }
                  placeholder="acme"
                  required
                />
              </label>
            )}

            {error && <div className="banner error">{error}</div>}
            {message && <div className="banner success">{message}</div>}

            <button className="primary-button" type="submit" disabled={busy}>
              {busy ? "Signing in..." : `Continue as ${selected.label}`}
            </button>
          </form>
        </section>
      </div>
    );
  }

  return (
    <div className="app-shell">
      <header className="topbar">
        <div>
          <span className="eyebrow">Signed in</span>
          <h1>SaaS Platform Dashboard</h1>
          <p className="subtle">
            {session.email} | {resolvedRole}
            {session.tenantId && session.tenantId !== "public"
              ? ` | tenant ${session.tenantId}`
              : ""}
          </p>
        </div>
        <div className="topbar-actions">
          <button
            className="ghost-button"
            onClick={() => loadDashboard(session)}
            disabled={busy}
          >
            Refresh
          </button>
          <button className="ghost-button" onClick={handleLogout}>
            Logout
          </button>
        </div>
      </header>

      {error && <div className="banner error">{error}</div>}
      {message && <div className="banner success">{message}</div>}

      {resolvedRole === "SUPER_ADMIN" ? (
        <SuperAdminPanel users={users} busy={busy} />
      ) : (
        <main className="dashboard-grid">
          <section className="stats-grid">
            <StatCard label="Total Bugs" value={stats.total} accent="cyan" />
            <StatCard label="Open" value={stats.open} accent="amber" />
            <StatCard label="In Progress" value={stats.inProgress} accent="violet" />
            <StatCard label="Resolved" value={stats.resolved} accent="green" />
          </section>

          <section className="panel">
            <div className="panel-header">
              <div>
                <span className="eyebrow">Bug board</span>
                <h2>Live backlog</h2>
              </div>
            </div>
            <BugTable
              bugs={bugs}
              role={resolvedRole}
              canAssignFromDirectory={canAssignFromDirectory}
              assignableUsers={assignableUsers}
              rowUploadFiles={rowUploadFiles}
              uploadingBugId={uploadingBugId}
              onStatusChange={handleStatusChange}
              onAssignmentChange={handleAssignmentChange}
              onSelectUploadFile={(bugId, file) =>
                setRowUploadFiles((current) => ({
                  ...current,
                  [bugId]: file
                }))
              }
              onUploadVideo={handleInlineVideoUpload}
              onOpenVideo={handleOpenVideo}
            />
          </section>

          {resolvedRole === "TESTER" && (
            <section className="panel">
              <div className="panel-header">
                <div>
                  <span className="eyebrow">New issue</span>
                  <h2>Report a bug</h2>
                </div>
              </div>
              <form className="stacked-form" onSubmit={handleCreateBug}>
                <label>
                  Title
                  <input
                    type="text"
                    value={bugForm.title}
                    onChange={(event) =>
                      setBugForm((current) => ({
                        ...current,
                        title: event.target.value
                      }))
                    }
                    placeholder="Search crashes on submit"
                    required
                  />
                </label>

                <label>
                  Description
                  <textarea
                    value={bugForm.description}
                    onChange={(event) =>
                      setBugForm((current) => ({
                        ...current,
                        description: event.target.value
                      }))
                    }
                    placeholder="Share steps to reproduce, expected behavior, and actual behavior."
                    rows="6"
                  />
                </label>

                <label>
                  Assign to
                  {canAssignFromDirectory ? (
                    <select
                      value={bugForm.assignedTo}
                      onChange={(event) =>
                        setBugForm((current) => ({
                          ...current,
                          assignedTo: event.target.value
                        }))
                      }
                    >
                      <option value="">Unassigned</option>
                      {assignableUsers.map((user) => (
                        <option key={user.id} value={user.email}>
                          {user.email}
                        </option>
                      ))}
                    </select>
                  ) : (
                    <input
                      type="email"
                      value={bugForm.assignedTo}
                      onChange={(event) =>
                        setBugForm((current) => ({
                          ...current,
                          assignedTo: event.target.value
                        }))
                      }
                      placeholder="dev@acme.com"
                    />
                  )}
                </label>

                <label>
                  Recording
                  <input
                    type="file"
                    accept="video/mp4,video/*"
                    onChange={(event) =>
                      setBugForm((current) => ({
                        ...current,
                        videoFile: event.target.files?.[0] || null
                      }))
                    }
                  />
                </label>

                <button className="primary-button" type="submit" disabled={busy}>
                  {busy
                    ? "Submitting..."
                    : bugForm.videoFile
                      ? "Create Bug + Upload Video"
                      : "Create Bug"}
                </button>
              </form>
            </section>
          )}

          {canReadTenantUsers && (
            <section className="panel">
              <div className="panel-header">
                <div>
                  <span className="eyebrow">Team view</span>
                  <h2>Tenant users</h2>
                </div>
              </div>
              <UserList users={users} />
            </section>
          )}
        </main>
      )}
    </div>
  );
}

function SuperAdminPanel({ users, busy }) {
  return (
    <main className="dashboard-grid">
      <section className="panel wide-panel">
        <div className="panel-header">
          <div>
            <span className="eyebrow">Platform view</span>
            <h2>Public users</h2>
          </div>
          <span className="subtle">{busy ? "Refreshing..." : `${users.length} users`}</span>
        </div>
        <UserList users={users} />
      </section>

      <section className="panel accent-panel">
        <span className="eyebrow">What this UI covers</span>
        <h2>Super admins can review the public control plane.</h2>
        <p className="subtle">
          Tenant creation and tenant-user creation are already available in the API.
          This first frontend version focuses on login, role-aware dashboards, bug
          tracking workflows, and direct video uploads.
        </p>
      </section>
    </main>
  );
}

function StatCard({ label, value, accent }) {
  return (
    <div className={`stat-card accent-${accent}`}>
      <span>{label}</span>
      <strong>{value}</strong>
    </div>
  );
}

function BugTable({
  bugs,
  role,
  canAssignFromDirectory,
  assignableUsers,
  rowUploadFiles,
  uploadingBugId,
  onStatusChange,
  onAssignmentChange,
  onSelectUploadFile,
  onUploadVideo,
  onOpenVideo
}) {
  if (!bugs.length) {
    return (
      <div className="empty-state">
        No bugs yet. The board will populate as issues are reported.
      </div>
    );
  }

  const canManageVideo = role === "TESTER" || role === "TENANT_ADMIN";

  return (
    <div className="table-shell">
      <table>
        <thead>
          <tr>
            <th>Title</th>
            <th>Status</th>
            <th>Created By</th>
            <th>Assigned To</th>
            <th>Recording</th>
            <th>Updated</th>
          </tr>
        </thead>
        <tbody>
          {bugs.map((bug) => (
            <tr key={bug.id}>
              <td>
                <div className="table-title">{bug.title}</div>
                <div className="table-subtitle">
                  {bug.description || "No description provided."}
                </div>
              </td>
              <td>
                {role === "DEV" || role === "TESTER" || role === "TENANT_ADMIN" ? (
                  <select
                    value={bug.status}
                    onChange={(event) => onStatusChange(bug.id, event.target.value)}
                  >
                    <option value="OPEN">OPEN</option>
                    <option value="IN_PROGRESS">IN_PROGRESS</option>
                    <option value="RESOLVED">RESOLVED</option>
                  </select>
                ) : (
                  bug.status
                )}
              </td>
              <td>{bug.createdBy}</td>
              <td>
                {canAssignFromDirectory ? (
                  <select
                    value={bug.assignedTo || ""}
                    onChange={(event) => onAssignmentChange(bug.id, event.target.value)}
                  >
                    <option value="">Unassigned</option>
                    {assignableUsers.map((user) => (
                      <option key={user.id} value={user.email}>
                        {user.email}
                      </option>
                    ))}
                  </select>
                ) : (
                  bug.assignedTo || "Unassigned"
                )}
              </td>
              <td>
                <VideoCell
                  bug={bug}
                  canManageVideo={canManageVideo}
                  selectedFile={rowUploadFiles[bug.id]}
                  uploading={uploadingBugId === bug.id}
                  onSelectFile={(file) => onSelectUploadFile(bug.id, file)}
                  onUpload={() => onUploadVideo(bug.id)}
                  onOpen={() => onOpenVideo(bug.id)}
                />
              </td>
              <td>{formatDate(bug.updatedAt)}</td>
            </tr>
          ))}
        </tbody>
      </table>
    </div>
  );
}

function VideoCell({
  bug,
  canManageVideo,
  selectedFile,
  uploading,
  onSelectFile,
  onUpload,
  onOpen
}) {
  return (
    <div className="video-cell">
      <span className={`video-badge status-${(bug.videoUploadStatus || "NOT_UPLOADED").toLowerCase()}`}>
        {bug.videoUploadStatus || "NOT_UPLOADED"}
      </span>
      {bug.videoUrl ? (
        <button className="video-link" type="button" onClick={onOpen}>
          Open video
        </button>
      ) : (
        <span className="table-subtitle">No recording</span>
      )}
      {canManageVideo && (
        <div className="video-actions">
          <input
            className="file-input"
            type="file"
            accept="video/mp4,video/*"
            onChange={(event) => onSelectFile(event.target.files?.[0] || null)}
          />
          <button
            className="ghost-button compact-button"
            type="button"
            disabled={!selectedFile || uploading}
            onClick={onUpload}
          >
            {uploading ? "Uploading..." : bug.videoUrl ? "Replace" : "Upload"}
          </button>
        </div>
      )}
    </div>
  );
}

function UserList({ users }) {
  if (!users.length) {
    return <div className="empty-state">No users found.</div>;
  }

  return (
    <div className="user-grid">
      {users.map((user) => (
        <article className="user-card" key={user.id}>
          <span className="pill">{user.role}</span>
          <h3>{user.email}</h3>
          <p>{user.userType ? `Type: ${user.userType}` : "Platform-level account"}</p>
        </article>
      ))}
    </div>
  );
}

function mapVideoMetadata(videoMetadata) {
  return {
    videoS3Key: videoMetadata.s3Key,
    videoUrl: videoMetadata.url,
    videoContentType: videoMetadata.contentType,
    videoSizeBytes: videoMetadata.sizeBytes,
    videoUploadStatus: videoMetadata.status
  };
}

function formatDate(value) {
  if (!value) {
    return "Just now";
  }

  return new Intl.DateTimeFormat("en-IN", {
    dateStyle: "medium",
    timeStyle: "short"
  }).format(new Date(value));
}

export default App;
