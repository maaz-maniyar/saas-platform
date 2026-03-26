const API_BASE_URL = import.meta.env.VITE_API_BASE_URL || "http://localhost:8080";

function buildHeaders(token, tenantId, includeJson = true) {
  const headers = {};

  if (includeJson) {
    headers["Content-Type"] = "application/json";
  }

  if (token) {
    headers.Authorization = `Bearer ${token}`;
  }

  if (tenantId && tenantId !== "public") {
    headers["X-Tenant-ID"] = tenantId;
  }

  return headers;
}

async function request(path, options = {}) {
  const response = await fetch(`${API_BASE_URL}${path}`, options);

  if (!response.ok) {
    let message = "Request failed";

    try {
      const data = await response.json();
      message = data.error || data.message || message;
    } catch {
      message = response.statusText || message;
    }

    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
}

export const api = {
  login(payload) {
    return request("/auth/login", {
      method: "POST",
      headers: buildHeaders(null, null),
      body: JSON.stringify(payload)
    });
  },

  getTenantUsers(session) {
    return request("/tenant/users", {
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  },

  getPlatformUsers(session) {
    return request("/platform/users", {
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  },

  getTenantUsersAsSuperAdmin(session, tenantId) {
    return request(`/platform/tenants/${tenantId}/users`, {
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  },

  getBugs(session) {
    return request("/api/bugs", {
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  },

  createBug(session, payload) {
    return request("/api/bugs", {
      method: "POST",
      headers: buildHeaders(session.token, session.tenantId),
      body: JSON.stringify(payload)
    });
  },

  patchBug(session, bugId, payload) {
    return request(`/api/bugs/${bugId}`, {
      method: "PATCH",
      headers: buildHeaders(session.token, session.tenantId),
      body: JSON.stringify(payload)
    });
  },

  generateBugVideoUploadUrl(session, bugId, file) {
    return request(`/api/bugs/${bugId}/video/upload-url`, {
      method: "POST",
      headers: buildHeaders(session.token, session.tenantId),
      body: JSON.stringify({
        contentType: file.type || "video/mp4",
        sizeBytes: file.size
      })
    });
  },

  async uploadFileToPresignedUrl(uploadUrl, file) {
    let response;

    try {
      response = await fetch(uploadUrl, {
        method: "PUT",
        headers: {
          "Content-Type": file.type || "video/mp4"
        },
        body: file
      });
    } catch {
      throw new Error(
        "Browser could not reach S3. Check the bucket CORS rules for http://localhost:5173."
      );
    }

    if (!response.ok) {
      throw new Error(`Direct upload to S3 failed with status ${response.status}.`);
    }
  },

  confirmBugVideoUpload(session, bugId, s3Key) {
    return request(`/api/bugs/${bugId}/video/complete`, {
      method: "POST",
      headers: buildHeaders(session.token, session.tenantId),
      body: JSON.stringify({ s3Key })
    });
  },

  getBugVideoAccessUrl(session, bugId) {
    return request(`/api/bugs/${bugId}/video/access-url`, {
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  },

  resetBugVideo(session, bugId) {
    return request(`/api/bugs/${bugId}/video`, {
      method: "DELETE",
      headers: buildHeaders(session.token, session.tenantId, false)
    });
  }
};
