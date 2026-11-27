(() => {
  "use strict";

  const PUBLIC_ENDPOINT_STORAGE_KEY = "mila_admin_public_endpoint_v1";

  function normalizeSupabaseUrl(raw) {
    let u = String(raw || "").trim().replace(/^["']|["']$/g, "").replace(/\/+$/, "");
    if (u && !/^https?:\/\//i.test(u) && u.includes(".supabase.co")) {
      u = "https://" + u;
    }
    return u;
  }

  function normalizeSupabaseKey(raw) {
    return String(raw || "").trim().replace(/^["']|["']$/g, "").replace(/\s+/g, "");
  }

  function isSecretSupabaseKey(raw) {
    const k = normalizeSupabaseKey(raw);
    if (!k) return false;
    if (k.startsWith("sb_secret_")) return true;
    // Detect legacy JWT with role=service_role
    const parts = k.split(".");
    if (parts.length === 3 && parts[0].startsWith("eyJ")) {
      try {
        const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
        const payload = JSON.parse(atob(b64));
        if (payload && payload.role === "service_role") return true;
      } catch {
        // ignore non-JSON payload
      }
    }
    return false;
  }

  function loadSavedPublicEndpoint() {
    try {
      const raw = window.localStorage.getItem(PUBLIC_ENDPOINT_STORAGE_KEY);
      if (!raw) return { url: "", key: "" };
      const parsed = JSON.parse(raw);
      const url = String(parsed.url || "").trim().replace(/\/+$/, "");
      const key = String(parsed.key || "").trim();
      if (!url || !key || isSecretSupabaseKey(key)) return { url: "", key: "" };
      return { url, key };
    } catch {
      return { url: "", key: "" };
    }
  }

  function savePublicEndpoint(url, key) {
    if (!url || !key || isSecretSupabaseKey(key)) return;
    try {
      window.localStorage.setItem(PUBLIC_ENDPOINT_STORAGE_KEY, JSON.stringify({ url, key }));
    } catch {
      // Ignore storage quota / private mode errors
    }
  }

  const savedEndpoint = loadSavedPublicEndpoint();
  const buildUrl = ((window.MILA_ADMIN_CONFIG && window.MILA_ADMIN_CONFIG.supabaseUrl) || "").trim().replace(/\/+$/, "");
  const buildKey = ((window.MILA_ADMIN_CONFIG && window.MILA_ADMIN_CONFIG.supabaseAnonKey) || "").trim();

  // In-memory runtime state (short-lived access token stays strictly in memory;
  // rotating Supabase refresh_token is stored in IndexedDB until explicit Sign Out)
  const SESSION_DB_NAME = "mila_admin_session_db_v1";
  const SESSION_STORE_NAME = "sessions";
  const SESSION_RECORD_KEY = "active";

  function openSessionDb() {
    return new Promise((resolve, reject) => {
      if (typeof indexedDB === "undefined") {
        reject(new Error("IndexedDB unavailable"));
        return;
      }
      const req = indexedDB.open(SESSION_DB_NAME, 1);
      req.onupgradeneeded = () => {
        const db = req.result;
        if (!db.objectStoreNames.contains(SESSION_STORE_NAME)) {
          db.createObjectStore(SESSION_STORE_NAME, { keyPath: "id" });
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error || new Error("Could not open session DB"));
    });
  }

  async function readSavedSession() {
    try {
      const db = await openSessionDb();
      return await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE_NAME, "readonly");
        const req = tx.objectStore(SESSION_STORE_NAME).get(SESSION_RECORD_KEY);
        req.onsuccess = () => resolve(req.result || null);
        req.onerror = () => resolve(null);
      });
    } catch {
      return null;
    }
  }

  async function writeSavedSession(patch) {
    try {
      const existing = (await readSavedSession()) || { id: SESSION_RECORD_KEY };
      const next = Object.assign({}, existing, patch, {
        id: SESSION_RECORD_KEY,
        updatedAt: Date.now()
      });
      if (!next.refreshToken) return;
      const db = await openSessionDb();
      await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE_NAME, "readwrite");
        tx.objectStore(SESSION_STORE_NAME).put(next);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // Best-effort persistence (e.g. restricted private browsing)
    }
  }

  async function clearSavedSession() {
    try {
      const db = await openSessionDb();
      await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE_NAME, "readwrite");
        tx.objectStore(SESSION_STORE_NAME).delete(SESSION_RECORD_KEY);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // Ignore cleanup errors
    }
  }

  const state = {
    supabaseUrl: buildUrl || savedEndpoint.url,
    supabaseAnonKey: (buildKey && !isSecretSupabaseKey(buildKey)) ? buildKey : savedEndpoint.key,
    token: null,
    tokenExpiresAt: 0,
    admin: null,
    activeModule: "overview",
    searchQuery: "",
    statusFilter: "ALL",
    liveSyncEnabled: true,
    knownEnquiryIds: new Set(),
    knownUserIds: new Set(),
    knownReviewIds: new Set(),
    hasInitialSync: false,
    pollTimer: null,
    data: {
      users: [],
      clients: [],
      enquiries: [],
      services: [],
      vacancies: [],
      candidates: [],
      applications: [],
      notifications: [],
      portfolio: [],
      testimonials: [],
      team: [],
      pages: [],
      admins: [],
      invites: [],
      settings: null,
      logs: []
    }
  };

  const ROLE_LABELS = {
    SUPER_ADMIN: "Super Admin",
    CONSULTANT: "Consultant",
    RECRUITMENT_ADMIN: "Recruitment Admin",
    CONTENT_ADMIN: "Content Admin",
    REPORTING_ADMIN: "Reporting Admin"
  };

  const ENQUIRY_STATUSES = [
    "SUBMITTED", "UNDER_REVIEW", "PROPOSAL_SHARED", "CONVERTED", "WITHDRAWN"
  ];

  const APPLICATION_STAGES = [
    "APPLIED", "UNDER_REVIEW", "SHORTLISTED", "INTERVIEW_SCHEDULED", "OFFER_EXTENDED", "HIRED", "DECLINED"
  ];

  const BUSINESS_CATEGORIES = [
    "LUXURY_HOTEL_RESORT", "FINE_DINING_RESTAURANT", "LOUNGE_BAR_NIGHTLIFE", "CAFE_BAKERY_CHAIN", "HOSPITALITY_GROUP"
  ];

  const DEPARTMENTS = [
    "EXECUTIVE", "PRE_OPENING", "CULINARY", "FNB_SERVICE", "MIXOLOGY", "ROOMS", "SALES_REVENUE", "HR_TRAINING", "HOUSEKEEPING_WELLNESS"
  ];

  const ROLE_CATEGORIES = [
    "PRE_OPENING", "EXECUTIVE", "CULINARY", "FNB_SERVICE", "MIXOLOGY", "ROOMS"
  ];

  const CITY_REGIONS = [
    "Mumbai", "New Delhi NCR", "Bengaluru", "Udaipur & Rajasthan", "Goa", "Hyderabad", "Pan-India"
  ];

  const AVAILABILITIES = [
    "IMMEDIATE", "WITHIN_15_DAYS", "WITHIN_30_DAYS", "WITHIN_60_DAYS", "NOT_ACTIVELY_LOOKING"
  ];

  const MODULES = [
    { id: "overview", label: "Dashboard Overview", group: "Executive", roles: ["SUPER_ADMIN", "CONSULTANT", "RECRUITMENT_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"] },
    { id: "users", label: "Logged-In App Users", group: "Client Operations", roles: ["SUPER_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN"] },
    { id: "clients", label: "Client Management", group: "Client Operations", roles: ["SUPER_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN"] },
    { id: "enquiries", label: "Service Enquiries", group: "Client Operations", roles: ["SUPER_ADMIN", "CONSULTANT", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONSULTANT"] },
    { id: "vacancies", label: "Job Vacancies", group: "Recruitment & Talent", roles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN"] },
    { id: "candidates", label: "Candidate Management", group: "Recruitment & Talent", roles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN"] },
    { id: "cvs", label: "CV Database & Downloads", group: "Recruitment & Talent", roles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN"] },
    { id: "applications", label: "Applications & Hiring Status", group: "Recruitment & Talent", roles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN"] },
    { id: "notifications", label: "Candidate Notifications", group: "Recruitment & Talent", roles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "RECRUITMENT_ADMIN"] },
    { id: "banners", label: "Top Banner & Popup Alerts", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "services", label: "Services", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "portfolio", label: "Portfolio Projects", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "testimonials", label: "Reviews & Testimonials", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "contact_info", label: "Contact Email & Phone", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "team", label: "Team Members", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "pages", label: "Website Content", group: "Content & Branding", roles: ["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN", "CONTENT_ADMIN"] },
    { id: "admins", label: "Admin Users & Invites", group: "Governance", roles: ["SUPER_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN"] },
    { id: "settings", label: "Platform Settings", group: "Governance", roles: ["SUPER_ADMIN", "REPORTING_ADMIN"], writeRoles: ["SUPER_ADMIN"] },
    { id: "logs", label: "Activity Audit Logs", group: "Governance", roles: ["SUPER_ADMIN", "CONSULTANT", "RECRUITMENT_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"], writeRoles: [] }
  ];

  // Safe DOM creation helpers (no innerHTML with dynamic content)
  function el(tag, props, children) {
    const node = document.createElement(tag);
    if (props) {
      Object.entries(props).forEach(([key, val]) => {
        if (val === null || val === undefined) return;
        if (key === "className") node.className = val;
        else if (key === "text") node.textContent = String(val);
        else if (key === "onClick") node.addEventListener("click", val);
        else if (key === "onInput") node.addEventListener("input", val);
        else if (key === "onChange") node.addEventListener("change", val);
        else if (key === "type" || key === "value" || key === "placeholder" || key === "checked" || key === "disabled" || key === "required") {
          node[key] = val;
        } else {
          node.setAttribute(key, String(val));
        }
      });
    }
    if (children) {
      const list = Array.isArray(children) ? children : [children];
      list.forEach((child) => {
        if (child === null || child === undefined || child === false) return;
        if (typeof child === "string" || typeof child === "number") {
          node.appendChild(document.createTextNode(String(child)));
        } else {
          node.appendChild(child);
        }
      });
    }
    return node;
  }

  function clearNode(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  function pill(text, tone) {
    return el("span", { className: "pill pill-" + (tone || "gold"), text });
  }

  function isValidSupabaseUrl(raw) {
    try {
      const parsed = new URL(raw);
      return parsed.protocol === "https:" && Boolean(parsed.hostname);
    } catch {
      return false;
    }
  }

  function formatTimestamp(iso) {
    if (!iso) return "";
    try {
      const d = new Date(iso);
      if (Number.isNaN(d.getTime())) return "";
      return d.toLocaleString(undefined, { dateStyle: "medium", timeStyle: "short" });
    } catch {
      return "";
    }
  }

  function updateConnectionBadge() {
    const badge = document.getElementById("connStatusBadge");
    const details = document.getElementById("connectionDetails");
    const configured = isValidSupabaseUrl(state.supabaseUrl) && Boolean(state.supabaseAnonKey) && !isSecretSupabaseKey(state.supabaseAnonKey);
    if (badge) {
      if (configured) {
        badge.textContent = "Connected";
        badge.className = "pill pill-emerald";
      } else {
        badge.textContent = "Setup Required";
        badge.className = "pill pill-alert";
      }
    }
    if (details) {
      // Keep the login screen clean (email + password only) whenever backend is already connected
      details.classList.toggle("hidden", configured);
    }
  }

  function startLiveSyncPolling() {
    stopLiveSyncPolling();
    if (!state.liveSyncEnabled || !state.token) return;
    state.pollTimer = setInterval(() => {
      if (!state.token || !state.admin || !state.liveSyncEnabled) return;
      const modalOpen = !document.getElementById("modalBackdrop").classList.contains("hidden");
      const activeEl = document.activeElement;
      const typingInSearch = activeEl && (activeEl.tagName === "INPUT" || activeEl.tagName === "TEXTAREA" || activeEl.tagName === "SELECT");
      if (modalOpen || typingInSearch) return;
      refreshAllData(false, true);
    }, 10000);
  }

  function stopLiveSyncPolling() {
    if (state.pollTimer) {
      clearInterval(state.pollTimer);
      state.pollTimer = null;
    }
  }

  let toastTimer = null;
  function showToast(message) {
    const toast = document.getElementById("toast");
    toast.textContent = message;
    toast.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => toast.classList.add("hidden"), 3800);
  }

  function canWrite(moduleId) {
    const mod = MODULES.find((m) => m.id === moduleId);
    if (!mod || !state.admin) return false;
    return (mod.writeRoles || []).includes(state.admin.role);
  }

  function allowedModules() {
    if (!state.admin) return [];
    return MODULES.filter((m) => m.roles.includes(state.admin.role));
  }

  // Concurrency-safe Supabase session refresh (rotating single-use refresh tokens)
  let refreshSessionPromise = null;

  async function verifyAdminTokenOnServer(accessToken) {
    const verifyRes = await fetch(state.supabaseUrl + "/rest/v1/rpc/verify_admin_session", {
      method: "POST",
      headers: {
        apikey: state.supabaseAnonKey,
        Authorization: "Bearer " + accessToken,
        "Content-Type": "application/json"
      },
      body: "{}"
    });
    const verifyJson = await verifyRes.json().catch(() => null);
    return { status: verifyRes.status, ok: verifyRes.ok, json: verifyJson };
  }

  function applyAuthenticatedAdmin(verifyJson, fallbackEmail) {
    state.admin = {
      id: verifyJson.id || "",
      uid: verifyJson.uid || "",
      email: verifyJson.email || (fallbackEmail || "").trim().toLowerCase(),
      displayName: verifyJson.display_name || "MILA Administrator",
      role: verifyJson.role || "REPORTING_ADMIN"
    };
    document.getElementById("sidebarRoleLabel").textContent = ROLE_LABELS[state.admin.role] || state.admin.role;
    document.getElementById("sidebarAdminName").textContent = state.admin.displayName;
    document.getElementById("sidebarAdminEmail").textContent = state.admin.email;
  }

  function forceSessionExpiredSignOut(reason) {
    stopLiveSyncPolling();
    clearSavedSession();
    state.token = null;
    state.tokenExpiresAt = 0;
    state.admin = null;
    document.getElementById("dashboardView").classList.add("hidden");
    document.getElementById("loginView").classList.remove("hidden");
    if (reason) showToast(reason);
  }

  async function refreshAdminSession(options) {
    const requireVerify = Boolean(options && options.requireVerify);
    if (refreshSessionPromise) return refreshSessionPromise;

    refreshSessionPromise = (async () => {
      if (!isValidSupabaseUrl(state.supabaseUrl) || !state.supabaseAnonKey) {
        return { ok: false, fatal: false, message: "Supabase endpoint not configured." };
      }
      const saved = await readSavedSession();
      if (!saved || !saved.refreshToken) {
        return { ok: false, fatal: true, message: "No saved administrator session." };
      }

      try {
        const res = await fetch(state.supabaseUrl + "/auth/v1/token?grant_type=refresh_token", {
          method: "POST",
          headers: {
            apikey: state.supabaseAnonKey,
            "Content-Type": "application/json"
          },
          body: JSON.stringify({ refresh_token: saved.refreshToken })
        });
        const json = await res.json().catch(() => null);

        if (!res.ok || !json || !json.access_token) {
          const isAuthRevoked = res.status === 400 || res.status === 401 || res.status === 403;
          if (isAuthRevoked) {
            await clearSavedSession();
          }
          return {
            ok: false,
            fatal: isAuthRevoked,
            message: (json && (json.error_description || json.msg || json.message)) || "Session expired. Please sign in again."
          };
        }

        const nextToken = json.access_token;
        const expiresInSec = Number(json.expires_in || 3600);
        state.token = nextToken;
        state.tokenExpiresAt = Date.now() + Math.max(60, expiresInSec) * 1000;

        // Immediately persist rotated refresh token so mobile Chrome never drops the session
        if (json.refresh_token) {
          await writeSavedSession({ refreshToken: json.refresh_token });
        }

        if (requireVerify || !state.admin) {
          const verified = await verifyAdminTokenOnServer(nextToken);
          if (verified.status === 404) {
            return {
              ok: false,
              fatal: false,
              message: "MILA database functions are not installed yet."
            };
          }
          if (!verified.ok || !verified.json || verified.json.authorized !== true || verified.json.active === false) {
            await fetch(state.supabaseUrl + "/auth/v1/logout", {
              method: "POST",
              headers: { apikey: state.supabaseAnonKey, Authorization: "Bearer " + nextToken }
            }).catch(() => {});
            await clearSavedSession();
            return {
              ok: false,
              fatal: true,
              message: "Administrator access revoked or inactive."
            };
          }
          applyAuthenticatedAdmin(verified.json, (json.user && json.user.email) || "");
        }

        return { ok: true, fatal: false, savedModule: saved.activeModule || "overview" };
      } catch {
        // Offline or transient network hiccup: keep saved refresh token intact!
        return { ok: false, fatal: false, message: "Network temporarily unavailable." };
      }
    })();

    try {
      return await refreshSessionPromise;
    } finally {
      refreshSessionPromise = null;
    }
  }

  async function ensureFreshTokenIfNeeded() {
    if (!state.token || !state.admin) return;
    // Proactively refresh if within 5 minutes of JWT expiry
    if (state.tokenExpiresAt > 0 && Date.now() >= state.tokenExpiresAt - 300_000) {
      await refreshAdminSession({ requireVerify: false });
    }
  }

  // Supabase REST / Auth / Storage HTTP Client (with automatic 401 refresh + retry)
  async function apiFetch(path, options, _retried) {
    if (!isValidSupabaseUrl(state.supabaseUrl) || !state.supabaseAnonKey) {
      throw new Error("Configure your Supabase Project URL and Publishable/Anon Key first.");
    }
    if (!_retried) {
      await ensureFreshTokenIfNeeded();
    }
    const headers = Object.assign(
      {
        apikey: state.supabaseAnonKey,
        Accept: "application/json"
      },
      options && options.headers ? options.headers : {}
    );
    if (state.token && !headers.Authorization) {
      headers.Authorization = "Bearer " + state.token;
    }
    const res = await fetch(state.supabaseUrl + path, Object.assign({}, options || {}, { headers }));
    if (res.status === 401 && !_retried && state.token) {
      const refreshed = await refreshAdminSession({ requireVerify: false });
      if (refreshed.ok) {
        const retryOpts = Object.assign({}, options || {});
        if (retryOpts.headers) {
          retryOpts.headers = Object.assign({}, retryOpts.headers);
          delete retryOpts.headers.Authorization;
        }
        return apiFetch(path, retryOpts, true);
      }
      if (refreshed.fatal) {
        forceSessionExpiredSignOut("Your administrator session ended. Please sign in again.");
      }
    }
    const text = await res.text();
    let json = null;
    try {
      json = text ? JSON.parse(text) : null;
    } catch {
      json = null;
    }
    if (!res.ok) {
      const msg =
        (json && (json.msg || json.message || json.error_description || json.hint)) ||
        "Request failed (" + res.status + ")";
      throw new Error(msg);
    }
    return json;
  }

  async function logAudit(moduleName, action, targetId, summary) {
    if (!state.admin || state.admin.role === "REPORTING_ADMIN") return;
    try {
      await apiFetch("/rest/v1/activity_logs", {
        method: "POST",
        headers: {
          "Content-Type": "application/json",
          Prefer: "return=minimal"
        },
        body: JSON.stringify({
          actor_uid: state.admin.uid || undefined,
          actor_email: state.admin.email || "admin@milahospitality.in",
          actor_role: state.admin.role,
          module: moduleName,
          action,
          target_id: String(targetId || "record"),
          summary: String(summary || "").slice(0, 400)
        })
      });
    } catch {
      // Best-effort audit log
    }
  }

  async function mutateTable(method, path, bodyObj, auditMeta) {
    const prefer = (auditMeta && auditMeta.prefer) || "return=minimal";
    const opts = {
      method,
      headers: {
        "Content-Type": "application/json",
        Prefer: prefer
      }
    };
    if (bodyObj !== null && bodyObj !== undefined) {
      opts.body = JSON.stringify(bodyObj);
    }
    await apiFetch(path, opts);
    if (auditMeta) {
      await logAudit(auditMeta.module, auditMeta.action, auditMeta.targetId, auditMeta.summary);
    }
  }

  // Real authenticated private CV download from Supabase Storage bucket `candidate-cvs`
  async function downloadCandidateCv(candidate) {
    if (!candidate.cv_storage_path) {
      showToast("No private CV file path stored for " + candidate.full_name + ".");
      return;
    }
    try {
      await ensureFreshTokenIfNeeded();
      showToast("Downloading CV for " + candidate.full_name + "...");
      const cleanPath = String(candidate.cv_storage_path).trim().replace(/^\/+/, "").replace(/^candidate-cvs\//, "");
      const encodedPath = cleanPath.split("/").map((seg) => encodeURIComponent(seg)).join("/");
      const cvUrl = state.supabaseUrl + "/storage/v1/object/authenticated/candidate-cvs/" + encodedPath;
      let res = await fetch(cvUrl, {
        method: "GET",
        headers: {
          apikey: state.supabaseAnonKey,
          Authorization: "Bearer " + state.token
        }
      });
      if (res.status === 401) {
        const ref = await refreshAdminSession({ requireVerify: false });
        if (ref.ok) {
          res = await fetch(cvUrl, {
            method: "GET",
            headers: {
              apikey: state.supabaseAnonKey,
              Authorization: "Bearer " + state.token
            }
          });
        }
      }
      if (!res.ok) {
        const errText = await res.text();
        let errMsg = "Could not download CV (" + res.status + ").";
        try {
          const parsed = JSON.parse(errText);
          errMsg = parsed.message || parsed.error || errMsg;
        } catch {
          // ignore
        }
        throw new Error(errMsg);
      }
      const blob = await res.blob();
      const safeName = (candidate.cv_file_name || cleanPath.split("/").pop() || "candidate_cv.pdf").replace(/[^\w.\-() ]+/g, "_");
      const blobUrl = URL.createObjectURL(blob);
      const link = document.createElement("a");
      link.href = blobUrl;
      link.download = safeName;
      document.body.appendChild(link);
      link.click();
      document.body.removeChild(link);
      setTimeout(() => URL.revokeObjectURL(blobUrl), 5000);
      await logAudit("CV_DATABASE", "DOWNLOAD_CV", candidate.id || candidate.user_id, "Downloaded CV " + safeName + " for " + candidate.full_name);
      showToast("Downloaded " + safeName);
    } catch (err) {
      showToast(err.message || "Unable to download CV.");
    }
  }

  // Auth Flow (Server-side verify_admin_session RBAC)
  async function handleLogin(email, password) {
    const errBox = document.getElementById("loginError");
    const submitBtn = document.getElementById("loginSubmitBtn");
    errBox.classList.add("hidden");

    if (!isValidSupabaseUrl(state.supabaseUrl) || !state.supabaseAnonKey) {
      document.getElementById("connectionDetails").open = true;
      errBox.textContent = "Enter your Supabase Project URL and Publishable/Anon Key in Supabase Backend Connection below.";
      errBox.classList.remove("hidden");
      return;
    }

    if (isSecretSupabaseKey(state.supabaseAnonKey)) {
      document.getElementById("connectionDetails").open = true;
      errBox.textContent = "You pasted a Secret / service_role key. Please paste the public Publishable (sb_publishable_...) or Legacy anon key (eyJ...) instead.";
      errBox.classList.remove("hidden");
      return;
    }

    submitBtn.disabled = true;
    submitBtn.textContent = "Verifying Administrator Access...";
    try {
      const authRes = await fetch(state.supabaseUrl + "/auth/v1/token?grant_type=password", {
        method: "POST",
        headers: {
          apikey: state.supabaseAnonKey,
          "Content-Type": "application/json"
        },
        body: JSON.stringify({ email: email.trim(), password })
      });
      const authJson = await authRes.json().catch(() => null);
      if (!authRes.ok || !authJson || !authJson.access_token) {
        throw new Error((authJson && (authJson.error_description || authJson.msg || authJson.message)) || "Invalid administrator email or password.");
      }

      // Persist only the non-secret public backend URL + publishable/anon key for next visit
      savePublicEndpoint(state.supabaseUrl, state.supabaseAnonKey);

      const token = authJson.access_token;
      const verified = await verifyAdminTokenOnServer(token);
      if (verified.status === 404) {
        throw new Error("MILA database tables/functions are not installed in this Supabase project yet. Run supabase/ALL_IN_ONE_SETUP.sql in your Supabase SQL Editor first.");
      }
      if (!verified.ok || !verified.json || verified.json.authorized !== true || verified.json.active === false) {
        await fetch(state.supabaseUrl + "/auth/v1/logout", {
          method: "POST",
          headers: { apikey: state.supabaseAnonKey, Authorization: "Bearer " + token }
        }).catch(() => {});
        throw new Error("This account is not authorised in MILA admin_users. Ordinary client and candidate accounts cannot access the Admin Console.");
      }

      state.token = token;
      state.tokenExpiresAt = Date.now() + Math.max(60, Number(authJson.expires_in || 3600)) * 1000;
      applyAuthenticatedAdmin(verified.json, email);

      // Persist rotating refresh token in IndexedDB so admin stays signed in until Sign Out
      if (authJson.refresh_token) {
        await writeSavedSession({
          refreshToken: authJson.refresh_token,
          activeModule: "overview"
        });
      }

      document.getElementById("adminPassword").value = "";
      document.getElementById("loginView").classList.add("hidden");
      document.getElementById("dashboardView").classList.remove("hidden");

      state.activeModule = "overview";
      state.knownEnquiryIds = new Set();
      state.knownUserIds = new Set();
      state.knownReviewIds = new Set();
      state.hasInitialSync = false;
      await refreshAllData(false, false);
      startLiveSyncPolling();
    } catch (err) {
      const msg = err.message || "Administrator verification failed.";
      if (/invalid api key/i.test(msg)) {
        const details = document.getElementById("connectionDetails");
        if (details) {
          details.classList.remove("hidden");
          details.open = true;
        }
      }
      errBox.textContent = msg;
      errBox.classList.remove("hidden");
    } finally {
      submitBtn.disabled = false;
      submitBtn.textContent = "Sign In to Admin Dashboard";
    }
  }

  async function restoreSavedAdminSession() {
    if (!isValidSupabaseUrl(state.supabaseUrl) || !state.supabaseAnonKey) return false;
    const saved = await readSavedSession();
    if (!saved || !saved.refreshToken) return false;

    const submitBtn = document.getElementById("loginSubmitBtn");
    const errBox = document.getElementById("loginError");
    if (submitBtn) {
      submitBtn.disabled = true;
      submitBtn.textContent = "Restoring Admin Session...";
    }
    try {
      const res = await refreshAdminSession({ requireVerify: true });
      if (!res.ok || !state.admin || !state.token) {
        if (!res.fatal && res.message && errBox) {
          errBox.textContent = res.message + " Reconnect and reload or sign in again.";
          errBox.classList.remove("hidden");
        }
        return false;
      }

      const allowed = allowedModules().map((m) => m.id);
      const targetModule = res.savedModule && allowed.includes(res.savedModule) ? res.savedModule : "overview";
      state.activeModule = targetModule;
      state.knownEnquiryIds = new Set();
      state.knownUserIds = new Set();
      state.knownReviewIds = new Set();
      state.hasInitialSync = false;

      document.getElementById("loginView").classList.add("hidden");
      document.getElementById("dashboardView").classList.remove("hidden");
      await refreshAllData(false, false);
      startLiveSyncPolling();
      return true;
    } finally {
      if (submitBtn) {
        submitBtn.disabled = false;
        submitBtn.textContent = "Sign In to Admin Dashboard";
      }
    }
  }

  async function handleSignOut() {
    stopLiveSyncPolling();
    const oldToken = state.token;
    state.token = null;
    state.tokenExpiresAt = 0;
    state.admin = null;
    state.knownEnquiryIds = new Set();
    state.knownUserIds = new Set();
    state.knownReviewIds = new Set();
    state.hasInitialSync = false;
    await clearSavedSession();
    Object.keys(state.data).forEach((k) => {
      state.data[k] = Array.isArray(state.data[k]) ? [] : null;
    });
    if (oldToken && isValidSupabaseUrl(state.supabaseUrl)) {
      fetch(state.supabaseUrl + "/auth/v1/logout", {
        method: "POST",
        headers: { apikey: state.supabaseAnonKey, Authorization: "Bearer " + oldToken }
      }).catch(() => {});
    }
    document.getElementById("dashboardView").classList.add("hidden");
    document.getElementById("loginView").classList.remove("hidden");
    showToast("Signed out of MILA Admin Control Center.");
  }

  async function safeFetchList(path) {
    try {
      const res = await apiFetch(path);
      return Array.isArray(res) ? res : [];
    } catch {
      return [];
    }
  }

  async function refreshAllData(notifyUser, isBackgroundPoll) {
    if (!state.admin || !state.token) return;
    const role = state.admin.role;
    const refreshBtn = document.getElementById("refreshBtn");
    if (!isBackgroundPoll) {
      refreshBtn.disabled = true;
      refreshBtn.textContent = "Refreshing...";
    }
    try {
      const tasks = [
        safeFetchList("/rest/v1/activity_logs?order=created_at.desc&limit=100").then((r) => (state.data.logs = r))
      ];
      if (["SUPER_ADMIN", "REPORTING_ADMIN"].includes(role)) {
        tasks.push(safeFetchList("/rest/v1/user_profiles?order=updated_at.desc").then((r) => (state.data.users = r)));
        tasks.push(safeFetchList("/rest/v1/client_profiles?order=updated_at.desc").then((r) => (state.data.clients = r)));
        tasks.push(safeFetchList("/rest/v1/admin_users?order=created_at.desc").then((r) => (state.data.admins = r)));
        tasks.push(safeFetchList("/rest/v1/platform_settings?id=eq.platform&limit=1").then((r) => (state.data.settings = r[0] || null)));
      }
      if (role === "SUPER_ADMIN") {
        tasks.push(safeFetchList("/rest/v1/admin_invites?order=created_at.desc").then((r) => (state.data.invites = r)));
      }
      if (["SUPER_ADMIN", "CONSULTANT", "REPORTING_ADMIN"].includes(role)) {
        tasks.push(safeFetchList("/rest/v1/service_enquiries?order=created_at.desc").then((r) => (state.data.enquiries = r)));
      }
      if (["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"].includes(role)) {
        tasks.push(safeFetchList("/rest/v1/job_vacancies?order=created_at.desc").then((r) => (state.data.vacancies = r)));
        tasks.push(safeFetchList("/rest/v1/candidate_profiles?order=created_at.desc").then((r) => (state.data.candidates = r)));
        tasks.push(safeFetchList("/rest/v1/job_applications?order=created_at.desc").then((r) => (state.data.applications = r)));
        tasks.push(safeFetchList("/rest/v1/notifications?order=created_at.desc&limit=100").then((r) => (state.data.notifications = r)));
      }
      if (["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"].includes(role)) {
        tasks.push(safeFetchList("/rest/v1/services?order=sort_order.asc").then((r) => (state.data.services = r)));
        tasks.push(safeFetchList("/rest/v1/portfolio_projects?order=sort_order.asc").then((r) => (state.data.portfolio = r)));
        tasks.push(safeFetchList("/rest/v1/testimonials?order=created_at.desc,sort_order.asc").then((r) => (state.data.testimonials = r)));
        tasks.push(safeFetchList("/rest/v1/team_members?order=sort_order.asc").then((r) => (state.data.team = r)));
        tasks.push(safeFetchList("/rest/v1/website_content?order=updated_at.desc").then((r) => (state.data.pages = r)));
      }
      await Promise.all(tasks);

      // Detect newly arrived client enquiries, reviews, or new app logins during live sync
      const incomingEnquiries = state.data.enquiries || [];
      const incomingUsers = buildAppUsersDirectory();
      const incomingReviews = state.data.testimonials || [];
      if (state.hasInitialSync) {
        const brandNew = incomingEnquiries.filter((e) => e.id && !state.knownEnquiryIds.has(e.id));
        if (brandNew.length > 0) {
          const newest = brandNew[0];
          const refLabel = newest.reference_code ? " (" + newest.reference_code + ")" : "";
          showToast("New client enquiry received: " + (newest.property_name || "New Enquiry") + refLabel);
        } else {
          const brandNewReviews = incomingReviews.filter((r) => r.id && !state.knownReviewIds.has(r.id));
          if (brandNewReviews.length > 0) {
            const newestRev = brandNewReviews[0];
            showToast("New client review awaiting approval from " + (newestRev.attribution_role || "Client"));
          } else {
            const brandNewUsers = incomingUsers.filter((u) => u.userId && !state.knownUserIds.has(u.userId));
            if (brandNewUsers.length > 0) {
              const u = brandNewUsers[0];
              showToast("New app user logged in: " + u.fullName + (u.mobileNumber ? " (" + u.mobileNumber + ")" : ""));
            }
          }
        }
      }
      state.knownEnquiryIds = new Set(incomingEnquiries.map((e) => e.id).filter(Boolean));
      state.knownUserIds = new Set(incomingUsers.map((u) => u.userId).filter(Boolean));
      state.knownReviewIds = new Set(incomingReviews.map((r) => r.id).filter(Boolean));
      state.hasInitialSync = true;

      renderSidebar();
      renderActiveModule();
      if (notifyUser) showToast("Live records synchronized.");
    } finally {
      if (!isBackgroundPoll) {
        refreshBtn.disabled = false;
        refreshBtn.textContent = "Refresh Live Data";
      }
    }
  }

  // Modal helpers
  function openModal(title, bodyNodes, footerButtons) {
    document.getElementById("modalTitle").textContent = title;
    const body = document.getElementById("modalBody");
    const footer = document.getElementById("modalFooter");
    clearNode(body);
    clearNode(footer);
    bodyNodes.forEach((n) => n && body.appendChild(n));
    footerButtons.forEach((b) => b && footer.appendChild(b));
    document.getElementById("modalBackdrop").classList.remove("hidden");
  }

  function closeModal() {
    document.getElementById("modalBackdrop").classList.add("hidden");
  }

  function confirmDelete(title, message, onConfirm) {
    openModal(
      title,
      [el("p", { className: "record-meta", text: message })],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-danger",
          type: "button",
          text: "Delete Permanently",
          onClick: async () => {
            closeModal();
            try {
              await onConfirm();
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Delete failed.");
            }
          }
        })
      ]
    );
  }

  function buildFormField(labelText, inputEl) {
    return el("div", { className: "field" }, [el("label", { text: labelText }), inputEl]);
  }

  function buildSelect(options, selectedVal) {
    const select = el("select");
    options.forEach((opt) => {
      const value = typeof opt === "string" ? opt : opt.value;
      const label = typeof opt === "string" ? opt.replace(/_/g, " ") : opt.label;
      const o = el("option", { value, text: label });
      if (value === selectedVal) o.selected = true;
      select.appendChild(o);
    });
    return select;
  }

  function isPlaceholderName(name) {
    const n = String(name || "").trim().toLowerCase();
    return !n || n === "mila guest" || n === "mila member" || n === "mila client" || n === "unnamed";
  }

  function isSyntheticPhoneEmail(email) {
    return String(email || "").trim().toLowerCase().endsWith("@phone.mila.local");
  }

  function buildAppUsersDirectory() {
    const d = state.data;
    const byId = new Map();
    const adminEmails = new Set((d.admins || []).map((a) => String(a.email || "").toLowerCase()));
    const adminUids = new Set((d.admins || []).map((a) => a.user_id).filter(Boolean));

    const ensureEntry = (uid) => {
      if (!byId.has(uid)) {
        byId.set(uid, {
          userId: uid,
          fullName: "",
          mobileNumber: "",
          email: "",
          accountType: "CLIENT",
          loginMethod: "Email OTP",
          city: "",
          companyName: "",
          designation: "",
          hasCv: false,
          cvFileName: "",
          enquiriesCount: 0,
          applicationsCount: 0,
          createdAt: "",
          lastActiveAt: "",
          clientRow: null,
          candidateRow: null,
          isAdmin: false
        });
      }
      return byId.get(uid);
    };

    (d.users || []).forEach((u) => {
      if (!u.id) return;
      const entry = ensureEntry(u.id);
      if (!isPlaceholderName(u.full_name)) entry.fullName = u.full_name.trim();
      else if (!entry.fullName && u.full_name) entry.fullName = u.full_name.trim();
      if (u.mobile_number && u.mobile_number !== "Not Provided") entry.mobileNumber = u.mobile_number.trim();
      if (u.email) {
        if (isSyntheticPhoneEmail(u.email)) {
          entry.loginMethod = "Mobile";
        } else {
          entry.email = u.email.trim();
          entry.loginMethod = "Email OTP";
        }
      }
      entry.accountType = u.account_type || entry.accountType;
      entry.createdAt = u.created_at || entry.createdAt;
      entry.lastActiveAt = u.updated_at || u.created_at || entry.lastActiveAt;
      if (u.account_type === "ADMIN" || adminUids.has(u.id) || adminEmails.has(String(u.email || "").toLowerCase())) {
        entry.isAdmin = true;
      }
    });

    (d.clients || []).forEach((c) => {
      const uid = c.user_id || c.id;
      if (!uid) return;
      const entry = ensureEntry(uid);
      entry.clientRow = c;
      if (isPlaceholderName(entry.fullName) && !isPlaceholderName(c.full_name)) entry.fullName = c.full_name.trim();
      if (!entry.mobileNumber && c.mobile_number && c.mobile_number !== "Not Provided") entry.mobileNumber = c.mobile_number.trim();
      if (c.email && !isSyntheticPhoneEmail(c.email)) {
        entry.email = c.email.trim();
        entry.loginMethod = "Email OTP";
      } else if (isSyntheticPhoneEmail(c.email) && !entry.email) {
        entry.loginMethod = "Mobile";
      }
      if (c.city && c.city !== "India") entry.city = c.city;
      if (c.company_name && c.company_name !== "MILA Hospitality Member") entry.companyName = c.company_name;
      if (c.designation && c.designation !== "Client") entry.designation = c.designation;
      if (!entry.createdAt && c.created_at) entry.createdAt = c.created_at;
      const ts = c.updated_at || c.created_at || "";
      if (ts && (!entry.lastActiveAt || ts > entry.lastActiveAt)) entry.lastActiveAt = ts;
    });

    (d.candidates || []).forEach((cand) => {
      const uid = cand.user_id || cand.id;
      if (!uid) return;
      const entry = ensureEntry(uid);
      entry.candidateRow = cand;
      if (isPlaceholderName(entry.fullName) && !isPlaceholderName(cand.full_name)) entry.fullName = cand.full_name.trim();
      if (!entry.mobileNumber && cand.mobile_number && cand.mobile_number !== "Not Provided") entry.mobileNumber = cand.mobile_number.trim();
      if (!entry.email && cand.email && !isSyntheticPhoneEmail(cand.email)) entry.email = cand.email.trim();
      if (cand.cv_file_name && cand.cv_storage_path) {
        entry.hasCv = true;
        entry.cvFileName = cand.cv_file_name;
      }
      if (!entry.city && cand.location) entry.city = cand.location;
      const ts = cand.updated_at || cand.created_at || "";
      if (ts && (!entry.lastActiveAt || ts > entry.lastActiveAt)) entry.lastActiveAt = ts;
    });

    (d.enquiries || []).forEach((enq) => {
      if (!enq.user_id) return;
      const entry = ensureEntry(enq.user_id);
      entry.enquiriesCount += 1;
      if (isPlaceholderName(entry.fullName) && !isPlaceholderName(enq.contact_name)) entry.fullName = enq.contact_name.trim();
      if (!entry.mobileNumber && enq.contact_phone) entry.mobileNumber = enq.contact_phone.trim();
      if (!entry.email && enq.contact_email && !isSyntheticPhoneEmail(enq.contact_email)) entry.email = enq.contact_email.trim();
      const ts = enq.created_at || "";
      if (ts && (!entry.lastActiveAt || ts > entry.lastActiveAt)) entry.lastActiveAt = ts;
    });

    (d.applications || []).forEach((app) => {
      if (!app.user_id) return;
      const entry = ensureEntry(app.user_id);
      entry.applicationsCount += 1;
      if (isPlaceholderName(entry.fullName) && !isPlaceholderName(app.candidate_name)) entry.fullName = app.candidate_name.trim();
      if (!entry.mobileNumber && app.candidate_phone) entry.mobileNumber = app.candidate_phone.trim();
      if (!entry.email && app.candidate_email && !isSyntheticPhoneEmail(app.candidate_email)) entry.email = app.candidate_email.trim();
      const ts = app.created_at || "";
      if (ts && (!entry.lastActiveAt || ts > entry.lastActiveAt)) entry.lastActiveAt = ts;
    });

    return Array.from(byId.values())
      .filter((u) => !u.isAdmin)
      .map((u) => {
        if (!u.fullName || isPlaceholderName(u.fullName)) {
          u.fullName = u.mobileNumber ? "App Member (" + u.mobileNumber + ")" : (u.email || "MILA App User");
        }
        return u;
      })
      .sort((a, b) => String(b.lastActiveAt || b.createdAt || "").localeCompare(String(a.lastActiveAt || a.createdAt || "")));
  }

  // Sidebar Rendering
  function moduleCount(id) {
    const d = state.data;
    switch (id) {
      case "users": return buildAppUsersDirectory().length;
      case "clients": return d.clients.length;
      case "enquiries": return d.enquiries.length;
      case "vacancies": return d.vacancies.length;
      case "candidates": return d.candidates.length;
      case "cvs": return d.candidates.filter((c) => c.cv_file_name && c.cv_storage_path).length;
      case "applications": return d.applications.length;
      case "notifications": return d.notifications.length;
      case "banners": return getPromoBanners().length;
      case "services": return d.services.length;
      case "portfolio": return d.portfolio.length;
      case "testimonials": return d.testimonials.length;
      case "team": return d.team.length;
      case "pages": return (d.pages || []).filter((p) => !isPromoBannerRow(p) && !isContactDetailsRow(p)).length;
      case "contact_info": return 1;
      case "admins": return d.admins.length;
      case "logs": return d.logs.length;
      default: return null;
    }
  }

  function renderSidebar() {
    const nav = document.getElementById("sidebarNav");
    clearNode(nav);
    const mods = allowedModules();
    const groups = {};
    mods.forEach((m) => {
      groups[m.group] = groups[m.group] || [];
      groups[m.group].push(m);
    });
    Object.entries(groups).forEach(([group, items]) => {
      nav.appendChild(el("div", { className: "nav-group-label", text: group }));
      items.forEach((mod) => {
        const count = moduleCount(mod.id);
        const btn = el(
          "button",
          {
            type: "button",
            className: "nav-item" + (state.activeModule === mod.id ? " active" : ""),
            onClick: () => {
              state.activeModule = mod.id;
              state.searchQuery = "";
              state.statusFilter = "ALL";
              writeSavedSession({ activeModule: mod.id });
              document.getElementById("sidebar").classList.remove("open");
              const backdrop = document.getElementById("sidebarBackdrop");
              if (backdrop) backdrop.classList.add("hidden");
              renderSidebar();
              renderActiveModule();
            }
          },
          [
            el("span", { text: mod.label }),
            count !== null ? el("span", { className: "nav-count", text: count }) : null
          ]
        );
        nav.appendChild(btn);
      });
    });
  }

  function renderModuleHeader(title, subtitle, primaryActionBtn) {
    return el("div", { className: "module-header" }, [
      el("div", null, [
        el("div", { className: "eyebrow", text: ROLE_LABELS[state.admin.role] || state.admin.role }),
        el("h2", { className: "serif", text: title }),
        el("p", { text: subtitle })
      ]),
      primaryActionBtn || null
    ]);
  }

  function renderSearchBar(placeholder, filterOptions) {
    const bar = el("div", { className: "toolbar" });
    const search = el("input", {
      className: "search-input",
      type: "search",
      placeholder,
      value: state.searchQuery,
      onInput: (e) => {
        state.searchQuery = e.target.value;
        renderActiveModule();
      }
    });
    bar.appendChild(search);
    if (filterOptions && filterOptions.length) {
      const sel = buildSelect(["ALL", ...filterOptions], state.statusFilter);
      sel.className = "filter-select";
      sel.addEventListener("change", (e) => {
        state.statusFilter = e.target.value;
        renderActiveModule();
      });
      bar.appendChild(sel);
    }
    return bar;
  }

  // =========================================================================
  // MODULE RENDERERS
  // =========================================================================

  function renderOverview(container) {
    const d = state.data;
    const cvCount = d.candidates.filter((c) => c.cv_file_name && c.cv_storage_path).length;
    const canManageJobs = canWrite("vacancies");
    const canManageBanners = canWrite("banners");
    const promoBanners = getPromoBanners();
    const headerActions = (canManageJobs || canManageBanners)
      ? el("div", { style: "display:flex;gap:8px;flex-wrap:wrap;" }, [
          canManageBanners
            ? el("button", {
                className: "btn btn-gold",
                type: "button",
                text: "+ Create / Upload Banner",
                onClick: () => openBannerModal(null)
              })
            : null,
          canManageJobs
            ? el("button", {
                className: "btn btn-outline",
                type: "button",
                text: "+ Post New Job",
                onClick: () => openVacancyModal(null)
              })
            : null
        ])
      : null;

    container.appendChild(
      renderModuleHeader(
        "Executive Command Overview",
        "Live snapshot across MILA Hospitality users, top promotional banners & popups, enquiries, admin-only job postings, talent dossiers, and CVs.",
        headerActions
      )
    );

    const appUsers = buildAppUsersDirectory();
    const kpis = [
      { label: "Logged-In App Users", value: appUsers.length, mod: "users" },
      { label: "Top Banners & Popups", value: promoBanners.filter((b) => b.published).length + " / " + promoBanners.length, mod: "banners" },
      { label: "Service Enquiries", value: d.enquiries.length, mod: "enquiries" },
      { label: "Open Job Vacancies", value: d.vacancies.filter((v) => v.published).length + " / " + d.vacancies.length, mod: "vacancies" },
      { label: "Candidate Profiles", value: d.candidates.length, mod: "candidates" },
      { label: "Downloadable CVs", value: cvCount, mod: "cvs" },
      { label: "Job Applications", value: d.applications.length, mod: "applications" },
      { label: "Published Services", value: d.services.filter((s) => s.published).length, mod: "services" }
    ];

    const grid = el("div", { className: "kpi-grid" });
    const allowedIds = new Set(allowedModules().map((m) => m.id));
    kpis.forEach((k) => {
      const clickable = allowedIds.has(k.mod);
      grid.appendChild(
        el(
          "div",
          {
            className: "kpi-card",
            style: clickable ? "cursor:pointer;" : "",
            onClick: clickable
              ? () => {
                  state.activeModule = k.mod;
                  state.searchQuery = "";
                  state.statusFilter = "ALL";
                  renderSidebar();
                  renderActiveModule();
                }
              : null
          },
          [
            el("div", { className: "eyebrow", text: k.label }),
            el("div", { className: "kpi-value", text: k.value })
          ]
        )
      );
    });
    container.appendChild(grid);

    if (["SUPER_ADMIN", "REPORTING_ADMIN"].includes(state.admin.role)) {
      container.appendChild(
        el("div", { className: "module-header", style: "margin-top:16px;margin-bottom:10px;" }, [
          el("div", null, [
            el("h3", { className: "serif", text: "Logged-In App Users (Name & Mobile)" }),
            el("p", {
              className: "record-small",
              text: "Everyone currently logged in and using the MILA app, sorted by latest activity."
            })
          ]),
          el("button", {
            className: "btn btn-outline btn-sm",
            type: "button",
            text: "View All App Users (" + appUsers.length + ")",
            onClick: () => {
              state.activeModule = "users";
              state.searchQuery = "";
              state.statusFilter = "ALL";
              renderSidebar();
              renderActiveModule();
            }
          })
        ])
      );

      const usersList = el("div", { className: "records-list", style: "margin-bottom:18px;" });
      if (!appUsers.length) {
        usersList.appendChild(
          el("div", {
            className: "empty-state",
            text: "No app users logged in yet. As soon as someone signs in with OTP or Google, their Name and Mobile Number will appear here."
          })
        );
      } else {
        appUsers.slice(0, 6).forEach((u) => {
          usersList.appendChild(buildAppUserCard(u, canWrite("users")));
        });
      }
      container.appendChild(usersList);
    }

    if (["SUPER_ADMIN", "CONTENT_ADMIN", "REPORTING_ADMIN"].includes(state.admin.role)) {
      const bannerActions = el("div", { style: "display:flex;gap:8px;flex-wrap:wrap;" }, [
        canManageBanners
          ? el("button", {
              className: "btn btn-gold btn-sm",
              type: "button",
              text: "+ Create / Upload Banner",
              onClick: () => openBannerModal(null)
            })
          : null,
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Manage All Banners (" + promoBanners.length + ")",
          onClick: () => {
            state.activeModule = "banners";
            state.searchQuery = "";
            state.statusFilter = "ALL";
            renderSidebar();
            renderActiveModule();
          }
        })
      ]);

      container.appendChild(
        el("div", { className: "module-header", style: "margin-top:16px;margin-bottom:10px;" }, [
          el("div", null, [
            el("h3", { className: "serif", text: "Top Home Banner & Popup Alerts" }),
            el("p", {
              className: "record-small",
              text: "Design or upload ads, new opening alerts, and upcoming project banners shown at the top of the app Home screen."
            })
          ]),
          bannerActions
        ])
      );

      const bannerList = el("div", { className: "records-list", style: "margin-bottom:18px;" });
      if (!promoBanners.length) {
        bannerList.appendChild(
          el("div", {
            className: "empty-state",
            text: "No top banner posted yet. Tap '+ Create / Upload Banner' to post a new opening alert, ad, or upcoming project."
          })
        );
      } else {
        promoBanners.slice(0, 3).forEach((b) => {
          bannerList.appendChild(buildBannerCard(b, canManageBanners));
        });
      }
      container.appendChild(bannerList);
    }

    if (["SUPER_ADMIN", "RECRUITMENT_ADMIN", "REPORTING_ADMIN"].includes(state.admin.role)) {
      const jobsActions = el("div", { style: "display:flex;gap:8px;flex-wrap:wrap;" }, [
        canManageJobs
          ? el("button", {
              className: "btn btn-gold btn-sm",
              type: "button",
              text: "+ Post New Job",
              onClick: () => openVacancyModal(null)
            })
          : null,
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Manage All Jobs (" + d.vacancies.length + ")",
          onClick: () => {
            state.activeModule = "vacancies";
            state.searchQuery = "";
            state.statusFilter = "ALL";
            renderSidebar();
            renderActiveModule();
          }
        })
      ]);

      container.appendChild(
        el("div", { className: "module-header", style: "margin-top:16px;margin-bottom:10px;" }, [
          el("div", null, [
            el("h3", { className: "serif", text: "Job Vacancies (MILA Admin Only)" }),
            el("p", {
              className: "record-small",
              text: "Only MILA Admin can post, edit, publish, or delete jobs. Changes appear live in the Careers tab."
            })
          ]),
          jobsActions
        ])
      );

      const jobList = el("div", { className: "records-list", style: "margin-bottom:18px;" });
      if (!d.vacancies.length) {
        jobList.appendChild(
          el("div", {
            className: "empty-state",
            text: "No job vacancies posted yet. Tap '+ Post New Job' above to publish your first hospitality role."
          })
        );
      } else {
        d.vacancies.slice(0, 5).forEach((job) => {
          jobList.appendChild(buildVacancyCard(job, canManageJobs));
        });
      }
      container.appendChild(jobList);
    }

    if (["SUPER_ADMIN", "CONSULTANT", "REPORTING_ADMIN"].includes(state.admin.role)) {
      const enqHeader = el("div", { className: "module-header", style: "margin-top:16px;margin-bottom:10px;" }, [
        el("h3", { className: "serif", text: "Latest Incoming Client Enquiries" }),
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Open All Service Enquiries (" + d.enquiries.length + ")",
          onClick: () => {
            state.activeModule = "enquiries";
            state.searchQuery = "";
            state.statusFilter = "ALL";
            renderSidebar();
            renderActiveModule();
          }
        })
      ]);
      container.appendChild(enqHeader);

      const enqList = el("div", { className: "records-list", style: "margin-bottom:18px;" });
      if (!d.enquiries.length) {
        enqList.appendChild(
          el("div", {
            className: "empty-state",
            text: "No client enquiries received yet. Submit a test enquiry in the MILA Client App and it will appear here automatically within 10 seconds."
          })
        );
      } else {
        d.enquiries.slice(0, 5).forEach((enq) => {
          const when = formatTimestamp(enq.created_at);
          enqList.appendChild(
            el("div", { className: "record-card" }, [
              el("div", { className: "record-top" }, [
                el("div", { style: "display:flex;gap:8px;align-items:center;flex-wrap:wrap;" }, [
                  pill((enq.status || "SUBMITTED").replace(/_/g, " "), enq.status === "WITHDRAWN" ? "alert" : "gold"),
                  enq.reference_code ? pill(enq.reference_code, "emerald") : null
                ]),
                el("span", { className: "record-small", text: when })
              ]),
              el("div", { className: "record-title", text: (enq.property_name || "") + " — " + (enq.city || "") }),
              el("div", { className: "record-sub", text: (enq.contact_name || "") + " • " + (enq.contact_email || "") + " • " + (enq.contact_phone || "") }),
              enq.brief_notes ? el("div", { className: "record-small", text: "Brief: " + enq.brief_notes }) : null
            ])
          );
        });
      }
      container.appendChild(enqList);
    }

    container.appendChild(el("h3", { className: "serif", text: "Recent Admin Activity", style: "margin:12px 0;" }));
    const list = el("div", { className: "records-list" });
    if (!d.logs.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No admin activity logs recorded yet." }));
    } else {
      d.logs.slice(0, 12).forEach((log) => {
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill(log.module || "ADMIN", "gold"),
              el("span", { className: "record-small", text: (log.action || "") + " • " + (log.actor_email || "") })
            ]),
            el("div", { className: "record-meta", text: log.summary || "" })
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function buildAppUserCard(user, writable) {
    const activityBits = [];
    if (user.enquiriesCount > 0) activityBits.push(user.enquiriesCount + (user.enquiriesCount === 1 ? " Service Enquiry" : " Service Enquiries"));
    if (user.applicationsCount > 0) activityBits.push(user.applicationsCount + (user.applicationsCount === 1 ? " Job Application" : " Job Applications"));
    if (user.hasCv) activityBits.push("CV Uploaded");
    const activityText = activityBits.length ? activityBits.join(" • ") : "Exploring App";

    const actions = [];
    if (user.mobileNumber) {
      actions.push(
        el("a", {
          className: "btn btn-gold btn-sm",
          href: "tel:" + user.mobileNumber.replace(/[^\d+]/g, ""),
          style: "text-decoration:none;display:inline-flex;align-items:center;",
          text: "Call " + user.mobileNumber
        })
      );
    }
    if (user.hasCv && user.candidateRow) {
      actions.push(
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Download CV",
          onClick: () => downloadCandidateCv(user.candidateRow)
        })
      );
    }
    if (writable && user.clientRow) {
      actions.push(
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Edit Name / Number",
          onClick: () => openEditClientModal(user.clientRow)
        })
      );
    }

    return el("div", { className: "record-card" }, [
      el("div", { className: "record-top" }, [
        el("div", { style: "display:flex;gap:8px;align-items:center;flex-wrap:wrap;" }, [
          pill(user.loginMethod, user.loginMethod === "Email OTP" ? "emerald" : "gold"),
          user.enquiriesCount > 0 ? pill(user.enquiriesCount + " Enquiries", "gold") : null,
          user.hasCv ? pill("CV Ready", "emerald") : null
        ]),
        user.lastActiveAt
          ? el("span", { className: "record-small", text: "Last Active: " + formatTimestamp(user.lastActiveAt) })
          : null
      ]),
      el("div", { className: "record-title", text: user.fullName }),
      el("div", {
        className: "record-sub",
        text: "Mobile: " + (user.mobileNumber || "Not provided") + (user.email ? " • " + user.email : "")
      }),
      el("div", {
        className: "record-meta",
        text:
          "Activity: " +
          activityText +
          (user.city ? " • " + user.city : "") +
          (user.createdAt ? " • Joined " + formatTimestamp(user.createdAt) : "")
      }),
      actions.length ? el("div", { className: "record-actions" }, actions) : null
    ]);
  }

  function renderAppUsers(container) {
    const writable = canWrite("users");
    const allUsers = buildAppUsersDirectory();
    container.appendChild(
      renderModuleHeader(
        "Logged-In App Users",
        "Live directory of everyone logged in and using the MILA Hospitality app — with Name, Mobile Number, Email, last active time, and enquiry/job activity (" + allUsers.length + " total)."
      )
    );
    container.appendChild(
      renderSearchBar("Search by name, mobile number, email, or city...", ["EMAIL_OTP", "WITH_ENQUIRIES", "JOB_SEEKERS"])
    );

    const q = state.searchQuery.trim().toLowerCase();
    const rows = allUsers.filter((u) => {
      const matchFilter =
        state.statusFilter === "ALL" ||
        (state.statusFilter === "EMAIL_OTP" && u.loginMethod === "Email OTP") ||
        (state.statusFilter === "WITH_ENQUIRIES" && u.enquiriesCount > 0) ||
        (state.statusFilter === "JOB_SEEKERS" && (u.applicationsCount > 0 || u.hasCv || Boolean(u.candidateRow)));
      const matchQ =
        !q ||
        (u.fullName || "").toLowerCase().includes(q) ||
        (u.mobileNumber || "").toLowerCase().includes(q) ||
        (u.email || "").toLowerCase().includes(q) ||
        (u.city || "").toLowerCase().includes(q);
      return matchFilter && matchQ;
    });

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(
        el("div", {
          className: "empty-state",
          text: allUsers.length
            ? "No app users match this search or filter."
            : "No app users logged in yet."
        })
      );
    } else {
      rows.forEach((u) => {
        list.appendChild(buildAppUserCard(u, writable));
      });
    }
    container.appendChild(list);
  }

  function renderClients(container) {
    const writable = canWrite("clients");
    container.appendChild(
      renderModuleHeader("Client Management", "Search, edit, or delete MILA Hospitality client records (" + state.data.clients.length + ").")
    );
    container.appendChild(renderSearchBar("Search client name, mobile number, email, company, or city..."));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = state.data.clients.filter(
      (c) =>
        !q ||
        (c.full_name || "").toLowerCase().includes(q) ||
        (c.mobile_number || "").toLowerCase().includes(q) ||
        (c.email || "").toLowerCase().includes(q) ||
        (c.company_name || "").toLowerCase().includes(q) ||
        (c.city || "").toLowerCase().includes(q)
    );

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No clients match this filter." }));
    } else {
      rows.forEach((client) => {
        const actions = [];
        if (writable) {
          actions.push(
            el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Edit Client", onClick: () => openEditClientModal(client) }),
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete",
              onClick: () =>
                confirmDelete(
                  "Delete Client Profile?",
                  "Permanently delete " + (client.full_name || client.company_name) + "?",
                  async () => {
                    await mutateTable("DELETE", "/rest/v1/client_profiles?id=eq." + encodeURIComponent(client.id), null, {
                      module: "CLIENT_MANAGEMENT",
                      action: "DELETE",
                      targetId: client.id,
                      summary: "Deleted client " + client.company_name
                    });
                    showToast("Client deleted.");
                  }
                )
            })
          );
        }
        const cleanEmail = isSyntheticPhoneEmail(client.email) ? "Mobile Login Member" : (client.email || "");
        const lastSeen = formatTimestamp(client.updated_at || client.created_at);
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill((client.business_category || "CLIENT").replace(/_/g, " "), "gold"),
              el("span", { className: "record-small", text: lastSeen ? "Last Active: " + lastSeen : (client.city || "") })
            ]),
            el("div", { className: "record-title", text: (client.full_name || "Unnamed") + (client.company_name && client.company_name !== "MILA Hospitality Member" ? " — " + client.company_name : "") }),
            el("div", { className: "record-sub", text: "Mobile: " + (client.mobile_number || "Not provided") + " • " + cleanEmail }),
            el("div", { className: "record-meta", text: (client.designation || "Client") + " • " + (client.city || "India") }),
            actions.length ? el("div", { className: "record-actions" }, actions) : null
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function openEditClientModal(client) {
    const fName = el("input", { value: client.full_name || "" });
    const fEmail = el("input", { value: client.email || "", type: "email" });
    const fMobile = el("input", { value: client.mobile_number || "" });
    const fCompany = el("input", { value: client.company_name || "" });
    const fDesig = el("input", { value: client.designation || "" });
    const fCity = el("input", { value: client.city || "" });
    const fCat = buildSelect(BUSINESS_CATEGORIES, client.business_category || "LUXURY_HOTEL_RESORT");

    openModal(
      "Edit Client Profile",
      [
        buildFormField("Full Name", fName),
        buildFormField("Email", fEmail),
        buildFormField("Mobile Number", fMobile),
        buildFormField("Company / Property", fCompany),
        buildFormField("Designation", fDesig),
        buildFormField("City", fCity),
        buildFormField("Business Category", fCat)
      ],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Changes",
          onClick: async () => {
            try {
              await mutateTable(
                "PATCH",
                "/rest/v1/client_profiles?id=eq." + encodeURIComponent(client.id),
                {
                  full_name: fName.value.trim(),
                  email: fEmail.value.trim().toLowerCase(),
                  mobile_number: fMobile.value.trim(),
                  company_name: fCompany.value.trim(),
                  designation: fDesig.value.trim(),
                  city: fCity.value.trim(),
                  business_category: fCat.value
                },
                {
                  module: "CLIENT_MANAGEMENT",
                  action: "UPDATE",
                  targetId: client.id,
                  summary: "Updated client " + fCompany.value.trim()
                }
              );
              closeModal();
              showToast("Client updated.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Update failed.");
            }
          }
        })
      ]
    );
  }

  function renderEnquiries(container) {
    const writable = canWrite("enquiries");
    container.appendChild(
      renderModuleHeader("Service Enquiries", "Manage hospitality consultation requests, update status/notes, or delete enquiries (" + state.data.enquiries.length + ").")
    );
    container.appendChild(renderSearchBar("Search property, city, contact name, or email...", ENQUIRY_STATUSES));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = state.data.enquiries.filter((e) => {
      const matchSt = state.statusFilter === "ALL" || e.status === state.statusFilter;
      const matchQ =
        !q ||
        (e.property_name || "").toLowerCase().includes(q) ||
        (e.city || "").toLowerCase().includes(q) ||
        (e.contact_name || "").toLowerCase().includes(q) ||
        (e.contact_email || "").toLowerCase().includes(q);
      return matchSt && matchQ;
    });

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No service enquiries match this filter." }));
    } else {
      rows.forEach((enq) => {
        const actions = [];
        if (writable) {
          actions.push(
            el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Edit Status & Notes", onClick: () => openEditEnquiryModal(enq) })
          );
          if (state.admin.role === "SUPER_ADMIN") {
            actions.push(
              el("button", {
                className: "btn btn-danger btn-sm",
                type: "button",
                text: "Delete",
                onClick: () =>
                  confirmDelete("Delete Service Enquiry?", "Delete enquiry for " + enq.property_name + "?", async () => {
                    await mutateTable("DELETE", "/rest/v1/service_enquiries?id=eq." + encodeURIComponent(enq.id), null, {
                      module: "CLIENT_ENQUIRIES",
                      action: "DELETE",
                      targetId: enq.id,
                      summary: "Deleted enquiry " + enq.property_name
                    });
                    showToast("Enquiry deleted.");
                  })
              })
            );
          }
        }
        const when = formatTimestamp(enq.created_at);
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              el("div", { style: "display:flex;gap:8px;align-items:center;flex-wrap:wrap;" }, [
                pill((enq.status || "SUBMITTED").replace(/_/g, " "), enq.status === "WITHDRAWN" ? "alert" : "gold"),
                enq.reference_code ? pill(enq.reference_code, "emerald") : null
              ]),
              el("span", { className: "record-small", text: ((enq.service_type || "").replace(/_/g, " ")) + (when ? " • " + when : "") })
            ]),
            el("div", { className: "record-title", text: (enq.property_name || "") + " — " + (enq.city || "") }),
            el("div", { className: "record-sub", text: (enq.contact_name || "") + " • " + (enq.contact_email || "") + " • " + (enq.contact_phone || "") }),
            el("div", { className: "record-meta", text: "Scale: " + (enq.scale_summary || "N/A") }),
            enq.brief_notes ? el("div", { className: "record-small", text: "Client Brief: " + enq.brief_notes }) : null,
            enq.consultant_notes ? el("div", { className: "record-small", text: "Consultant Notes: " + enq.consultant_notes }) : null,
            actions.length ? el("div", { className: "record-actions" }, actions) : null
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function openEditEnquiryModal(enq) {
    const fStatus = buildSelect(ENQUIRY_STATUSES, enq.status || "SUBMITTED");
    const fNotes = el("textarea", { value: enq.consultant_notes || "", placeholder: "Internal consultant notes..." });
    openModal(
      "Update Enquiry: " + enq.property_name,
      [buildFormField("Enquiry Status", fStatus), buildFormField("Consultant Notes", fNotes)],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Update",
          onClick: async () => {
            try {
              await mutateTable(
                "PATCH",
                "/rest/v1/service_enquiries?id=eq." + encodeURIComponent(enq.id),
                {
                  status: fStatus.value,
                  consultant_notes: fNotes.value.trim() || null
                },
                {
                  module: "CLIENT_ENQUIRIES",
                  action: "UPDATE",
                  targetId: enq.id,
                  summary: "Updated enquiry " + enq.property_name + " to " + fStatus.value
                }
              );
              closeModal();
              showToast("Enquiry updated.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Update failed.");
            }
          }
        })
      ]
    );
  }

  function buildVacancyCard(job, writable) {
    const actions = [];
    if (writable) {
      actions.push(
        el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Edit Job", onClick: () => openVacancyModal(job) }),
        el("button", {
          className: "btn btn-ghost btn-sm",
          type: "button",
          text: job.published ? "Unpublish" : "Publish",
          onClick: async () => {
            try {
              await mutateTable(
                "PATCH",
                "/rest/v1/job_vacancies?id=eq." + encodeURIComponent(job.id),
                { published: !job.published },
                {
                  module: "JOB_VACANCY_MANAGEMENT",
                  action: "UPDATE",
                  targetId: job.id,
                  summary: (job.published ? "Unpublished " : "Published ") + job.title
                }
              );
              showToast(job.published ? "Job unpublished (hidden from app)." : "Job published live to MILA App.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Unable to update job status.");
            }
          }
        }),
        el("button", {
          className: "btn btn-danger btn-sm",
          type: "button",
          text: "Delete Job",
          onClick: () =>
            confirmDelete("Delete Job Vacancy?", "Permanently delete '" + job.title + "' from MILA Talent?", async () => {
              await mutateTable("DELETE", "/rest/v1/job_vacancies?id=eq." + encodeURIComponent(job.id), null, {
                module: "JOB_VACANCY_MANAGEMENT",
                action: "DELETE",
                targetId: job.id,
                summary: "Deleted vacancy " + job.title
              });
              showToast("Job deleted.");
            })
        })
      );
    }
    const when = formatTimestamp(job.created_at);
    return el("div", { className: "record-card" }, [
      el("div", { className: "record-top" }, [
        el("div", { style: "display:flex;gap:8px;align-items:center;flex-wrap:wrap;" }, [
          pill(job.published ? "Published Live" : "Draft", job.published ? "emerald" : "muted"),
          pill((job.category || "PRE_OPENING").replace(/_/g, " "), "gold"),
          job.is_urgent ? pill("Urgent Mandate", "alert") : null
        ]),
        when ? el("span", { className: "record-small", text: when }) : null
      ]),
      el("div", { className: "record-title", text: job.title || "" }),
      el("div", { className: "record-sub", text: (job.property_tier || "") + " • " + (job.location || "") }),
      el("div", { className: "record-meta", text: (job.compensation || "") + " • " + (job.experience_required || "") }),
      job.summary ? el("div", { className: "record-small", text: job.summary }) : null,
      actions.length ? el("div", { className: "record-actions" }, actions) : null
    ]);
  }

  function renderVacancies(container) {
    const writable = canWrite("vacancies");
    const addBtn = writable
      ? el("button", { className: "btn btn-gold", type: "button", text: "+ Post New Job", onClick: () => openVacancyModal(null) })
      : null;
    container.appendChild(
      renderModuleHeader(
        "Job Vacancies (MILA Admin Only)",
        "Only MILA Admin can post, edit, publish/unpublish, or delete open roles (" + state.data.vacancies.length + " total). Published jobs appear live in the MILA App Careers tab.",
        addBtn
      )
    );
    container.appendChild(renderSearchBar("Search job title, location, hotel tier, or department...", ["PUBLISHED", "DRAFT"]));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = state.data.vacancies.filter((v) => {
      const matchStatus =
        state.statusFilter === "ALL" ||
        (state.statusFilter === "PUBLISHED" && Boolean(v.published)) ||
        (state.statusFilter === "DRAFT" && !v.published);
      const matchQuery =
        !q ||
        (v.title || "").toLowerCase().includes(q) ||
        (v.location || "").toLowerCase().includes(q) ||
        (v.city_region || "").toLowerCase().includes(q) ||
        (v.property_tier || "").toLowerCase().includes(q) ||
        (v.category || "").toLowerCase().includes(q);
      return matchStatus && matchQuery;
    });

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(
        el("div", {
          className: "empty-state",
          text: state.data.vacancies.length
            ? "No job vacancies match this search or filter."
            : "No job vacancies posted yet. Click '+ Post New Job' above to publish a role."
        })
      );
    } else {
      rows.forEach((job) => {
        list.appendChild(buildVacancyCard(job, writable));
      });
    }
    container.appendChild(list);
  }

  function openVacancyModal(job) {
    const isEdit = Boolean(job && job.id);
    const fTitle = el("input", { value: (job && job.title) || "", placeholder: "e.g., Executive Chef — Luxury Resort" });
    const fTier = el("input", { value: (job && job.property_tier) || "5-Star Luxury Hotel", placeholder: "e.g., 5-Star Luxury Hotel / Heritage Palace" });
    const fLocation = el("input", { value: (job && job.location) || "Mumbai, Maharashtra", placeholder: "e.g., Mumbai, Maharashtra" });
    const currentRegion = (job && job.city_region) || "Mumbai";
    const regionOptions = CITY_REGIONS.includes(currentRegion) ? CITY_REGIONS : [currentRegion, ...CITY_REGIONS];
    const fRegion = buildSelect(regionOptions, currentRegion);
    const fComp = el("input", { value: (job && job.compensation) || "Executive Package", placeholder: "e.g., ₹24–32 LPA + Benefits" });
    const fExp = el("input", { value: (job && job.experience_required) || "5+ Years", placeholder: "e.g., 5–10 Years" });
    const fMinYrs = el("input", { type: "number", value: String((job && job.min_experience_years) ?? 5) });
    const fCat = buildSelect(ROLE_CATEGORIES, (job && job.category) || "PRE_OPENING");
    const fSummary = el("textarea", {
      value: (job && job.summary) || "",
      placeholder: "Brief overview of the hospitality role, property, and leadership scope..."
    });
    const existingMandates = Array.isArray(job && job.key_mandates) ? job.key_mandates.join("\n") : "";
    const existingSkills = Array.isArray(job && job.desired_skills) ? job.desired_skills.join(", ") : "";
    const fMandates = el("textarea", {
      value: existingMandates,
      placeholder: "One key responsibility per line (optional)"
    });
    const fSkills = el("input", {
      value: existingSkills,
      placeholder: "Comma-separated skills, e.g., Pre-Opening, Luxury F&B, P&L"
    });
    const fUrgent = el("input", { type: "checkbox", checked: Boolean(job && job.is_urgent) });
    const fPublished = el("input", { type: "checkbox", checked: job ? Boolean(job.published) : true });

    openModal(
      isEdit ? "Edit Job Vacancy" : "Post New Job (MILA Admin)",
      [
        buildFormField("Job Title *", fTitle),
        buildFormField("Property Tier / Brand", fTier),
        buildFormField("Location (City, State) *", fLocation),
        buildFormField("City Region Filter", fRegion),
        buildFormField("Department / Category", fCat),
        buildFormField("Compensation / Salary", fComp),
        buildFormField("Experience Required", fExp),
        buildFormField("Minimum Years Experience", fMinYrs),
        buildFormField("Job Summary", fSummary),
        buildFormField("Key Responsibilities (1 per line)", fMandates),
        buildFormField("Desired Skills (comma separated)", fSkills),
        el("label", { className: "checkbox-row" }, [fUrgent, "Mark as Priority / Urgent Search"]),
        el("label", { className: "checkbox-row" }, [fPublished, "Publish live to MILA App immediately"])
      ],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: isEdit ? "Save Changes" : "Post Job Now",
          onClick: async () => {
            const title = fTitle.value.trim();
            const location = fLocation.value.trim();
            if (!title) {
              showToast("Job title is required.");
              return;
            }
            if (!location) {
              showToast("Location is required.");
              return;
            }
            const slug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-|-$/g, "") + "-" + (Date.now() % 10000);
            const mandates = fMandates.value
              .split("\n")
              .map((s) => s.trim())
              .filter(Boolean);
            const skills = fSkills.value
              .split(",")
              .map((s) => s.trim())
              .filter(Boolean);
            const payload = {
              title,
              property_tier: fTier.value.trim() || "Luxury Hospitality Property",
              location,
              city_region: fRegion.value,
              compensation: fComp.value.trim() || "Competitive Package",
              experience_required: fExp.value.trim() || "3+ Years",
              min_experience_years: Math.max(0, Math.min(50, parseInt(fMinYrs.value, 10) || 0)),
              category: fCat.value,
              summary: fSummary.value.trim() || "Hospitality role posted and managed exclusively by MILA Hospitality.",
              key_mandates: mandates.length ? mandates : ["Operational leadership & luxury guest service excellence"],
              desired_skills: skills.length ? skills : ["Luxury Hospitality"],
              is_urgent: fUrgent.checked,
              published: fPublished.checked
            };
            if (!isEdit) {
              payload.slug = slug;
              if (state.admin && state.admin.uid) {
                payload.created_by = state.admin.uid;
              }
            }
            try {
              await mutateTable(
                isEdit ? "PATCH" : "POST",
                isEdit ? "/rest/v1/job_vacancies?id=eq." + encodeURIComponent(job.id) : "/rest/v1/job_vacancies",
                payload,
                {
                  module: "JOB_VACANCY_MANAGEMENT",
                  action: isEdit ? "UPDATE" : "CREATE",
                  targetId: isEdit ? job.id : slug,
                  summary: (isEdit ? "Updated job " : "Posted job ") + title
                }
              );
              closeModal();
              showToast(isEdit ? "Job updated." : (fPublished.checked ? "Job posted and live in MILA App." : "Job saved as draft."));
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Failed to save job.");
            }
          }
        })
      ]
    );
  }

  function renderCandidates(container) {
    const writable = canWrite("candidates");
    container.appendChild(
      renderModuleHeader("Candidate Management", "Inspect, edit, download private CVs, or delete candidate profiles (" + state.data.candidates.length + ").")
    );
    container.appendChild(renderSearchBar("Search candidate name, designation, city, or skills..."));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = state.data.candidates.filter(
      (c) =>
        !q ||
        (c.full_name || "").toLowerCase().includes(q) ||
        (c.current_designation || "").toLowerCase().includes(q) ||
        (c.location || "").toLowerCase().includes(q) ||
        (c.skills || "").toLowerCase().includes(q)
    );

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No candidates match this search." }));
    } else {
      rows.forEach((cand) => {
        const hasCv = Boolean(cand.cv_file_name && cand.cv_storage_path);
        const actions = [
          el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Inspect Dossier", onClick: () => openCandidateDossierModal(cand, writable) })
        ];
        if (hasCv) {
          actions.push(
            el("button", { className: "btn btn-gold btn-sm", type: "button", text: "Download CV", onClick: () => downloadCandidateCv(cand) })
          );
        }
        if (writable) {
          actions.push(
            el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Edit", onClick: () => openEditCandidateModal(cand) }),
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete",
              onClick: () =>
                confirmDelete("Delete Candidate Profile?", "Permanently delete " + cand.full_name + "'s candidate record?", async () => {
                  await mutateTable("DELETE", "/rest/v1/candidate_profiles?id=eq." + encodeURIComponent(cand.id), null, {
                    module: "CANDIDATE_MANAGEMENT",
                    action: "DELETE",
                    targetId: cand.id,
                    summary: "Deleted candidate " + cand.full_name
                  });
                  showToast("Candidate deleted.");
                })
            })
          );
        }
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill((cand.department || "EXECUTIVE").replace(/_/g, " "), "gold"),
              pill(hasCv ? "CV Ready" : "No CV", hasCv ? "emerald" : "muted")
            ]),
            el("div", { className: "record-title", text: cand.full_name || "Unnamed Candidate" }),
            el("div", { className: "record-sub", text: (cand.current_designation || "") + " • " + (cand.current_employer || "") + " (" + (cand.total_experience_years || 0) + " yrs)" }),
            el("div", { className: "record-meta", text: (cand.location || "") + " • Expected: " + (cand.expected_salary || "N/A") + " • Notice: " + (cand.notice_period || "N/A") }),
            hasCv ? el("div", { className: "record-small", text: "Uploaded CV: " + cand.cv_file_name }) : null,
            el("div", { className: "record-actions" }, actions)
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function openCandidateDossierModal(cand, writable) {
    const pairs = [
      ["Email", cand.email],
      ["Mobile", cand.mobile_number],
      ["Location", cand.location],
      ["Designation", cand.current_designation],
      ["Department", (cand.department || "").replace(/_/g, " ")],
      ["Experience", (cand.total_experience_years || 0) + " yrs"],
      ["Current Employer", cand.current_employer],
      ["Previous Employers", cand.previous_employers],
      ["Education", cand.education],
      ["Skills", cand.skills],
      ["Preferred Locations", cand.preferred_job_locations],
      ["Expected Salary", cand.expected_salary],
      ["Availability", (cand.availability || "").replace(/_/g, " ")],
      ["Notice Period", cand.notice_period],
      ["Uploaded CV", cand.cv_file_name || "None"]
    ];
    const body = pairs.map(([k, v]) =>
      el("div", { className: "dossier-row" }, [el("strong", { text: k + ": " }), el("span", { text: v || "—" })])
    );
    const footer = [el("button", { className: "btn btn-ghost", type: "button", text: "Close", onClick: closeModal })];
    if (cand.cv_file_name && cand.cv_storage_path) {
      footer.push(el("button", { className: "btn btn-gold", type: "button", text: "Download CV", onClick: () => downloadCandidateCv(cand) }));
    }
    if (writable) {
      footer.push(
        el("button", {
          className: "btn btn-outline",
          type: "button",
          text: "Edit Candidate",
          onClick: () => {
            closeModal();
            openEditCandidateModal(cand);
          }
        })
      );
    }
    openModal(cand.full_name || "Candidate Dossier", body, footer);
  }

  function openEditCandidateModal(cand) {
    const fName = el("input", { value: cand.full_name || "" });
    const fEmail = el("input", { value: cand.email || "", type: "email" });
    const fMobile = el("input", { value: cand.mobile_number || "" });
    const fLoc = el("input", { value: cand.location || "" });
    const fDesig = el("input", { value: cand.current_designation || "" });
    const fDept = buildSelect(DEPARTMENTS, cand.department || "EXECUTIVE");
    const fYrs = el("input", { type: "number", value: String(cand.total_experience_years ?? 0) });
    const fEmployer = el("input", { value: cand.current_employer || "" });
    const fSkills = el("textarea", { value: cand.skills || "" });
    const fPrefLoc = el("input", { value: cand.preferred_job_locations || "" });
    const fSalary = el("input", { value: cand.expected_salary || "" });
    const fAvail = buildSelect(AVAILABILITIES, cand.availability || "WITHIN_30_DAYS");
    const fNotice = el("input", { value: cand.notice_period || "30 Days" });

    openModal(
      "Edit Candidate Profile",
      [
        buildFormField("Full Name", fName),
        buildFormField("Email", fEmail),
        buildFormField("Mobile Number", fMobile),
        buildFormField("Location", fLoc),
        buildFormField("Current Designation", fDesig),
        buildFormField("Department", fDept),
        buildFormField("Total Experience (Years)", fYrs),
        buildFormField("Current Employer", fEmployer),
        buildFormField("Skills", fSkills),
        buildFormField("Preferred Locations", fPrefLoc),
        buildFormField("Expected Salary", fSalary),
        buildFormField("Availability", fAvail),
        buildFormField("Notice Period", fNotice)
      ],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Candidate",
          onClick: async () => {
            try {
              await mutateTable(
                "PATCH",
                "/rest/v1/candidate_profiles?id=eq." + encodeURIComponent(cand.id),
                {
                  full_name: fName.value.trim(),
                  email: fEmail.value.trim().toLowerCase(),
                  mobile_number: fMobile.value.trim(),
                  location: fLoc.value.trim(),
                  current_designation: fDesig.value.trim(),
                  department: fDept.value,
                  total_experience_years: Math.max(0, Math.min(60, parseInt(fYrs.value, 10) || 0)),
                  current_employer: fEmployer.value.trim(),
                  skills: fSkills.value.trim(),
                  preferred_job_locations: fPrefLoc.value.trim(),
                  expected_salary: fSalary.value.trim(),
                  availability: fAvail.value,
                  notice_period: fNotice.value.trim()
                },
                { module: "CANDIDATE_MANAGEMENT", action: "UPDATE", targetId: cand.id, summary: "Updated candidate " + fName.value.trim() }
              );
              closeModal();
              showToast("Candidate updated.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Update failed.");
            }
          }
        })
      ]
    );
  }

  function renderCvDatabase(container) {
    const writable = canWrite("cvs");
    const withCv = state.data.candidates.filter((c) => c.cv_file_name && c.cv_storage_path);
    container.appendChild(
      renderModuleHeader("Private CV Database", "Securely download any candidate CV directly from the private Supabase vault (" + withCv.length + " stored).")
    );
    container.appendChild(renderSearchBar("Search candidate name, role, or CV filename..."));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = withCv.filter(
      (c) =>
        !q ||
        (c.full_name || "").toLowerCase().includes(q) ||
        (c.cv_file_name || "").toLowerCase().includes(q) ||
        (c.current_designation || "").toLowerCase().includes(q)
    );

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No candidate CVs found." }));
    } else {
      rows.forEach((cand) => {
        const sizeKb = Math.max(1, Math.round((cand.cv_file_size_bytes || 0) / 1024));
        const actions = [
          el("button", { className: "btn btn-gold btn-sm", type: "button", text: "Download CV", onClick: () => downloadCandidateCv(cand) })
        ];
        if (writable) {
          actions.push(
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Remove CV Record",
              onClick: () =>
                confirmDelete("Remove CV Record?", "Clear " + cand.cv_file_name + " from " + cand.full_name + "'s profile?", async () => {
                  await mutateTable(
                    "PATCH",
                    "/rest/v1/candidate_profiles?id=eq." + encodeURIComponent(cand.id),
                    { cv_file_name: null, cv_mime_type: null, cv_file_size_bytes: null, cv_storage_path: null },
                    { module: "CV_DATABASE", action: "PURGE_CV", targetId: cand.id, summary: "Cleared CV record for " + cand.full_name }
                  );
                  showToast("CV record removed.");
                })
            })
          );
        }
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [pill("Private Vault CV", "gold"), el("span", { className: "record-small", text: sizeKb + " KB" })]),
            el("div", { className: "record-title", text: cand.cv_file_name }),
            el("div", { className: "record-sub", text: cand.full_name + " • " + (cand.current_designation || "") }),
            el("div", { className: "record-small", text: "Storage Path: " + cand.cv_storage_path }),
            el("div", { className: "record-actions" }, actions)
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function renderApplications(container) {
    const writable = canWrite("applications");
    container.appendChild(
      renderModuleHeader("Job Applications & Recruitment Status", "Download candidate CVs, update hiring stages with live alerts, or delete applications (" + state.data.applications.length + ").")
    );
    container.appendChild(renderSearchBar("Search candidate, role title, or location...", APPLICATION_STAGES));

    const candByUser = new Map(state.data.candidates.map((c) => [c.user_id, c]));
    const candByEmail = new Map(state.data.candidates.map((c) => [(c.email || "").toLowerCase(), c]));

    const q = state.searchQuery.trim().toLowerCase();
    const rows = state.data.applications.filter((a) => {
      const matchSt = state.statusFilter === "ALL" || a.status === state.statusFilter;
      const matchQ =
        !q ||
        (a.candidate_name || "").toLowerCase().includes(q) ||
        (a.role_title || "").toLowerCase().includes(q) ||
        (a.location || "").toLowerCase().includes(q);
      return matchSt && matchQ;
    });

    const list = el("div", { className: "records-list" });
    if (!rows.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No job applications match this filter." }));
    } else {
      rows.forEach((app) => {
        const matchedCand = candByUser.get(app.user_id) || candByEmail.get((app.candidate_email || "").toLowerCase());
        const stageRow = el("div", { className: "stage-chips" });
        if (writable) {
          APPLICATION_STAGES.slice(1).forEach((stage) => {
            stageRow.appendChild(
              el("button", {
                type: "button",
                className: "chip-btn" + (app.status === stage ? " active" : ""),
                text: stage.replace(/_/g, " "),
                onClick: async () => {
                  try {
                    await mutateTable(
                      "PATCH",
                      "/rest/v1/job_applications?id=eq." + encodeURIComponent(app.id),
                      { status: stage },
                      { module: "JOB_APPLICATIONS", action: "STATUS_UPDATE", targetId: app.id, summary: app.candidate_name + " -> " + stage }
                    );
                    if (app.user_id) {
                      await mutateTable("POST", "/rest/v1/notifications", {
                        user_id: app.user_id,
                        role_id: app.role_id || "status_update",
                        title: "Application Update: " + app.role_title,
                        message: "Your MILA Talent application status is now: " + stage.replace(/_/g, " ") + ".",
                        department: "Recruitment",
                        location: app.location || "India",
                        is_read: false
                      });
                    }
                    showToast("Updated " + app.candidate_name + " to " + stage.replace(/_/g, " "));
                    await refreshAllData(false);
                  } catch (err) {
                    showToast(err.message || "Status update failed.");
                  }
                }
              })
            );
          });
        }

        const actions = [];
        if (matchedCand && matchedCand.cv_storage_path) {
          actions.push(
            el("button", { className: "btn btn-gold btn-sm", type: "button", text: "Download Candidate CV", onClick: () => downloadCandidateCv(matchedCand) })
          );
        }
        if (writable) {
          actions.push(
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete Application",
              onClick: () =>
                confirmDelete("Delete Application?", "Remove " + app.candidate_name + "'s application for " + app.role_title + "?", async () => {
                  await mutateTable("DELETE", "/rest/v1/job_applications?id=eq." + encodeURIComponent(app.id), null, {
                    module: "JOB_APPLICATIONS",
                    action: "DELETE",
                    targetId: app.id,
                    summary: "Deleted application from " + app.candidate_name
                  });
                  showToast("Application deleted.");
                })
            })
          );
        }

        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill((app.status || "APPLIED").replace(/_/g, " "), ["HIRED", "SHORTLISTED", "OFFER_EXTENDED"].includes(app.status) ? "emerald" : "gold"),
              el("span", { className: "record-small", text: (app.years_experience || 0) + " yrs exp" })
            ]),
            el("div", { className: "record-title", text: (app.candidate_name || "") + " — " + (app.role_title || "") }),
            el("div", { className: "record-sub", text: (app.current_property || "") + " • " + (app.location || "") }),
            el("div", { className: "record-meta", text: (app.candidate_email || "") + " • " + (app.candidate_phone || "") }),
            app.cv_file_name ? el("div", { className: "record-small", text: "Attached CV: " + app.cv_file_name }) : null,
            writable ? stageRow : null,
            actions.length ? el("div", { className: "record-actions" }, actions) : null
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function renderNotifications(container) {
    const writable = canWrite("notifications");
    const addBtn =
      writable && state.data.candidates.length
        ? el("button", { className: "btn btn-gold", type: "button", text: "+ Send Notification", onClick: openSendNotificationModal })
        : null;
    container.appendChild(
      renderModuleHeader("Candidate Notifications", "Dispatch direct role alerts or delete notification history (" + state.data.notifications.length + ").", addBtn)
    );
    const list = el("div", { className: "records-list" });
    if (!state.data.notifications.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No notifications dispatched yet." }));
    } else {
      state.data.notifications.forEach((n) => {
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [pill(n.department || "MILA Talent", "gold"), pill(n.is_read ? "Read" : "Unread", "muted")]),
            el("div", { className: "record-title", text: n.title || "" }),
            el("div", { className: "record-meta", text: n.message || "" }),
            writable
              ? el("div", { className: "record-actions" }, [
                  el("button", {
                    className: "btn btn-danger btn-sm",
                    type: "button",
                    text: "Delete",
                    onClick: () =>
                      confirmDelete("Delete Notification?", "Delete notification '" + n.title + "'?", async () => {
                        await mutateTable("DELETE", "/rest/v1/notifications?id=eq." + encodeURIComponent(n.id), null, {
                          module: "NOTIFICATION_MANAGEMENT",
                          action: "DELETE",
                          targetId: n.id,
                          summary: "Deleted notification " + n.title
                        });
                        showToast("Notification deleted.");
                      })
                  })
                ])
              : null
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function openSendNotificationModal() {
    const fTarget = buildSelect(
      state.data.candidates.map((c) => ({ value: c.user_id, label: c.full_name + " (" + c.location + ")" })),
      state.data.candidates[0].user_id
    );
    const fTitle = el("input", { value: "Executive Search Invitation" });
    const fMsg = el("textarea", { value: "MILA Talent has a confidential leadership mandate matching your profile." });
    openModal(
      "Send Candidate Notification",
      [buildFormField("Recipient Candidate", fTarget), buildFormField("Title", fTitle), buildFormField("Message", fMsg)],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Dispatch Alert",
          onClick: async () => {
            try {
              await mutateTable(
                "POST",
                "/rest/v1/notifications",
                {
                  user_id: fTarget.value,
                  role_id: "direct_admin_alert",
                  title: fTitle.value.trim(),
                  message: fMsg.value.trim(),
                  department: "MILA Talent",
                  location: "India",
                  is_read: false
                },
                { module: "NOTIFICATION_MANAGEMENT", action: "SEND", targetId: fTarget.value, summary: "Sent notification: " + fTitle.value.trim() }
              );
              closeModal();
              showToast("Notification dispatched.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Dispatch failed.");
            }
          }
        })
      ]
    );
  }

  function formatStarRating(rating) {
    const safe = Math.max(1, Math.min(5, parseInt(rating, 10) || 5));
    return "★".repeat(safe) + "☆".repeat(5 - safe) + " (" + safe + "/5)";
  }

  function renderReviewsManager(container) {
    const writable = canWrite("testimonials");
    const allItems = state.data.testimonials || [];
    const pendingCount = allItems.filter((r) => !r.published).length;
    const needsReplyCount = allItems.filter((r) => !(r.admin_reply || "").trim()).length;
    const addBtn = writable
      ? el("button", { className: "btn btn-gold", type: "button", text: "+ Add Review", onClick: () => openReviewEditorModal(null) })
      : null;

    container.appendChild(
      renderModuleHeader(
        "Client Reviews & Testimonials",
        "Approve or delete pending client reviews, publish official MILA replies, or manage featured testimonials (" + allItems.length + " total • " + pendingCount + " pending approval • " + needsReplyCount + " awaiting reply).",
        addBtn
      )
    );

    const filterOptions = [
      { id: "ALL", label: "All (" + allItems.length + ")" },
      { id: "HIDDEN", label: "Pending Approval (" + pendingCount + ")" },
      { id: "NEEDS_REPLY", label: "Awaiting Reply (" + needsReplyCount + ")" },
      { id: "REPLIED", label: "Replied (" + (allItems.length - needsReplyCount) + ")" },
      { id: "APP_USER", label: "App Users (" + allItems.filter((r) => r.source === "APP_USER").length + ")" }
    ];

    const activeFilter = filterOptions.some((f) => f.id === state.statusFilter) ? state.statusFilter : "ALL";
    const chips = el(
      "div",
      { className: "stage-chips", style: { marginBottom: "14px" } },
      filterOptions.map((opt) =>
        el("button", {
          className: "chip-btn" + (activeFilter === opt.id ? " active" : ""),
          type: "button",
          text: opt.label,
          onClick: () => {
            state.statusFilter = opt.id;
            renderActiveModule();
          }
        })
      )
    );

    const searchInput = el("input", {
      className: "search-input",
      placeholder: "Search by reviewer, property, review text, or reply...",
      value: state.searchQuery,
      onInput: (e) => {
        state.searchQuery = e.target.value;
        renderActiveModule();
      }
    });
    container.appendChild(el("div", { className: "toolbar" }, [searchInput]));
    container.appendChild(chips);

    const q = (state.searchQuery || "").trim().toLowerCase();
    const filtered = allItems.filter((item) => {
      const hasReply = Boolean((item.admin_reply || "").trim());
      if (activeFilter === "NEEDS_REPLY" && hasReply) return false;
      if (activeFilter === "REPLIED" && !hasReply) return false;
      if (activeFilter === "APP_USER" && item.source !== "APP_USER") return false;
      if (activeFilter === "HIDDEN" && item.published) return false;
      if (q) {
        const hay = [
          item.attribution_role,
          item.engagement_context,
          item.reviewer_email,
          item.quote,
          item.admin_reply
        ].join(" ").toLowerCase();
        if (!hay.includes(q)) return false;
      }
      return true;
    });

    const list = el("div", { className: "records-list" });
    if (!filtered.length) {
      list.appendChild(
        el("div", {
          className: "empty-state",
          text: allItems.length
            ? "No reviews match your current filter."
            : "No reviews in database yet. Run the Reviews SQL migration or click '+ Add Review' to add one."
        })
      );
    } else {
      filtered.forEach((item) => {
        const hasReply = Boolean((item.admin_reply || "").trim());
        const isAppUser = item.source === "APP_USER";
        const topLeftPills = el("div", { className: "stage-chips" }, [
          pill(
            item.published ? "Published" : (isAppUser ? "Pending Approval" : "Hidden"),
            item.published ? "emerald" : "alert"
          ),
          pill(isAppUser ? "App User Review" : "Admin Testimonial", isAppUser ? "gold" : "muted"),
          pill(hasReply ? "Replied" : "Awaiting Reply", hasReply ? "emerald" : "muted")
        ]);

        const metaParts = [item.engagement_context || ""];
        if (item.reviewer_email) metaParts.push(item.reviewer_email);
        if (item.created_at) metaParts.push(fmtDate(item.created_at));
        const metaLine = metaParts.filter(Boolean).join(" • ");

        const cardNodes = [
          el("div", { className: "record-top" }, [
            topLeftPills,
            el("span", { className: "review-stars", text: formatStarRating(item.rating) })
          ]),
          el("div", { className: "record-title", text: item.attribution_role || "Client Reviewer" }),
          metaLine ? el("div", { className: "record-small", text: metaLine }) : null,
          el("div", { className: "review-quote", text: "\"" + (item.quote || "") + "\"" })
        ];

        if (hasReply) {
          const replyMeta = [
            item.admin_replied_by ? "By " + item.admin_replied_by : "MILA Hospitality",
            item.admin_replied_at ? fmtDate(item.admin_replied_at) : ""
          ].filter(Boolean).join(" • ");
          cardNodes.push(
            el("div", { className: "review-reply-box" }, [
              el("div", { className: "review-reply-label", text: "MILA HOSPITALITY REPLY" }),
              el("div", { className: "review-reply-text", text: item.admin_reply }),
              replyMeta ? el("div", { className: "record-small", text: replyMeta }) : null
            ])
          );
        }

        if (writable) {
          const actions = [];
          if (!item.published) {
            actions.push(
              el("button", {
                className: "btn btn-gold btn-sm",
                type: "button",
                text: "Approve & Publish",
                onClick: async () => {
                  await mutateTable(
                    "PATCH",
                    "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id),
                    { published: true },
                    { module: "TESTIMONIALS", action: "UPDATE", targetId: item.id, summary: "Approved & published review from " + (item.attribution_role || "Client") }
                  );
                  showToast("Review approved and published to the MILA app.");
                  await refreshAllData(false);
                }
              })
            );
          }
          actions.push(
            el("button", {
              className: (item.published && !hasReply) ? "btn btn-gold btn-sm" : "btn btn-outline btn-sm",
              type: "button",
              text: hasReply ? "Edit Reply" : "Reply",
              onClick: () => openReviewReplyModal(item)
            }),
            el("button", {
              className: "btn btn-outline btn-sm",
              type: "button",
              text: "Edit",
              onClick: () => openReviewEditorModal(item)
            })
          );
          if (item.published) {
            actions.push(
              el("button", {
                className: "btn btn-ghost btn-sm",
                type: "button",
                text: "Hide",
                onClick: async () => {
                  await mutateTable(
                    "PATCH",
                    "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id),
                    { published: false },
                    { module: "TESTIMONIALS", action: "UPDATE", targetId: item.id, summary: "Hid review from " + (item.attribution_role || "Client") }
                  );
                  showToast("Review hidden from app.");
                  await refreshAllData(false);
                }
              })
            );
          }
          actions.push(
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete",
              onClick: () =>
                confirmDelete(
                  "Delete Review?",
                  "Permanently delete the review by '" + (item.attribution_role || "Client") + "'? It will be removed from the MILA app immediately.",
                  async () => {
                    await mutateTable("DELETE", "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id), null, {
                      module: "TESTIMONIALS",
                      action: "DELETE",
                      targetId: item.id,
                      summary: "Deleted review by " + (item.attribution_role || "Client")
                    });
                    showToast("Review deleted permanently.");
                  }
                )
            })
          );
          cardNodes.push(el("div", { className: "record-actions" }, actions));
        }

        list.appendChild(el("div", { className: "record-card" }, cardNodes));
      });
    }
    container.appendChild(list);
  }

  function openReviewReplyModal(item) {
    const existingReply = (item.admin_reply || "").trim();
    const replyInput = el("textarea", {
      value: existingReply,
      placeholder: "Write an official MILA Hospitality reply (shown publicly below this review in the app)..."
    });

    const bodyNodes = [
      el("div", { className: "dossier-row" }, [
        el("strong", { text: "Reviewer: " }),
        document.createTextNode((item.attribution_role || "Client") + " • " + formatStarRating(item.rating))
      ]),
      el("div", { className: "review-quote", style: { margin: "10px 0 14px 0" }, text: "\"" + (item.quote || "") + "\"" }),
      buildFormField("Official MILA Hospitality Reply *", replyInput)
    ];

    const footerButtons = [
      el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal })
    ];

    if (existingReply) {
      footerButtons.push(
        el("button", {
          className: "btn btn-danger",
          type: "button",
          text: "Clear Reply",
          onClick: async () => {
            try {
              await mutateTable(
                "PATCH",
                "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id),
                { admin_reply: "", admin_replied_by: "", admin_replied_at: null },
                { module: "TESTIMONIALS", action: "UPDATE", targetId: item.id, summary: "Cleared admin reply on review by " + (item.attribution_role || "Client") }
              );
              closeModal();
              showToast("Reply removed.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Could not clear reply.");
            }
          }
        })
      );
    }

    footerButtons.push(
      el("button", {
        className: "btn btn-gold",
        type: "button",
        text: "Save Reply",
        onClick: async () => {
          const replyText = replyInput.value.trim();
          if (!replyText) {
            showToast("Please enter a reply before saving.");
            return;
          }
          const replierName = (state.admin && (state.admin.full_name || state.admin.email)) || "MILA Hospitality Team";
          try {
            await mutateTable(
              "PATCH",
              "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id),
              {
                admin_reply: replyText,
                admin_replied_by: replierName,
                admin_replied_at: new Date().toISOString()
              },
              { module: "TESTIMONIALS", action: "UPDATE", targetId: item.id, summary: "Replied to review by " + (item.attribution_role || "Client") }
            );
            closeModal();
            showToast("Reply saved and published to the MILA app.");
            await refreshAllData(false);
          } catch (err) {
            const msg = String(err.message || "");
            if (msg.toLowerCase().includes("admin_reply")) {
              showToast("Please run the Reviews & Replies SQL update in Supabase SQL Editor first.");
            } else {
              showToast(msg || "Failed to save reply.");
            }
          }
        }
      })
    );

    openModal(existingReply ? "Edit MILA Reply" : "Reply to Client Review", bodyNodes, footerButtons);
  }

  function openReviewEditorModal(item) {
    const isEdit = Boolean(item && item.id);
    const fQuote = el("textarea", { value: (item && item.quote) || "", placeholder: "Client review / testimonial text..." });
    const fRole = el("input", { value: (item && item.attribution_role) || "", placeholder: "e.g., Managing Partner or Client Name" });
    const fContext = el("input", { value: (item && item.engagement_context) || "", placeholder: "e.g., Luxury Heritage Resort • Rajasthan" });
    const fRating = el("select", {}, [5, 4, 3, 2, 1].map((n) =>
      el("option", { value: String(n), text: "★".repeat(n) + " (" + n + " / 5)" })
    ));
    fRating.value = String((item && item.rating) || 5);
    const fOrder = el("input", { type: "number", value: String((item && item.sort_order) || 1) });
    const fPub = el("input", { type: "checkbox", checked: item ? Boolean(item.published) : true });

    const bodyNodes = [
      buildFormField("Review Text *", fQuote),
      buildFormField("Reviewer Name / Role *", fRole),
      buildFormField("Property / Engagement Context", fContext),
      buildFormField("Star Rating", fRating),
      buildFormField("Sort Order", fOrder),
      el("label", { className: "checkbox-row" }, [fPub, "Published (visible in MILA app)"])
    ];

    openModal(
      isEdit ? "Edit Review" : "Add Verified Client Review",
      bodyNodes,
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Review",
          onClick: async () => {
            const quote = fQuote.value.trim();
            const role = fRole.value.trim();
            if (quote.length < 5 || role.length < 2) {
              showToast("Please enter both the review text and reviewer name/role.");
              return;
            }
            const payload = {
              quote,
              attribution_role: role,
              engagement_context: fContext.value.trim(),
              rating: parseInt(fRating.value, 10) || 5,
              sort_order: parseInt(fOrder.value, 10) || 1,
              published: fPub.checked
            };
            if (!isEdit) payload.source = "ADMIN";
            try {
              await mutateTable(
                isEdit ? "PATCH" : "POST",
                isEdit ? "/rest/v1/testimonials?id=eq." + encodeURIComponent(item.id) : "/rest/v1/testimonials",
                payload,
                { module: "TESTIMONIALS", action: isEdit ? "UPDATE" : "CREATE", targetId: isEdit ? item.id : "new", summary: (isEdit ? "Updated" : "Added") + " review by " + role }
              );
              closeModal();
              showToast("Review saved.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Could not save review.");
            }
          }
        })
      ]
    );
  }

  function parseLinesToArray(raw) {
    return String(raw || "")
      .split(/\r?\n/)
      .map((line) => line.replace(/^[\s•\-\*]+/, "").trim())
      .filter(Boolean);
  }

  function arrayToLines(arr) {
    if (!Array.isArray(arr)) return "";
    return arr.map((s) => String(s || "").trim()).filter(Boolean).join("\n");
  }

  function isSafeServiceImageSource(raw) {
    const val = String(raw || "").trim();
    if (!val) return true;
    if (val.startsWith("data:image/")) return true;
    try {
      const parsed = new URL(val);
      return parsed.protocol === "https:";
    } catch {
      return false;
    }
  }

  const SERVICE_DIVISION_OPTIONS = [
    "MILA Consultancy",
    "MILA Academy & Expertise",
    "MILA Talent"
  ];

  const SERVICE_TYPE_OPTIONS = [
    { key: "HOSPITALITY_PRE_OPENING", label: "Hospitality Pre-Opening Consultancy" },
    { key: "RESTAURANT_CAFE_DEVELOPMENT", label: "Restaurant and Café Development" },
    { key: "BAR_BEVERAGE_CONSULTANCY", label: "Bar and Beverage Consultancy" },
    { key: "HOSPITALITY_TRAINING", label: "Hospitality Training" },
    { key: "OPERATIONS_MANAGEMENT", label: "Operations Management" },
    { key: "RECRUITMENT_TALENT_ACQUISITION", label: "Recruitment and Talent Acquisition" },
    { key: "BUSINESS_CONCEPT_DEVELOPMENT", label: "Business and Concept Development" },
    { key: "HOSPITALITY_MANAGEMENT_SOLUTIONS", label: "Hospitality Management Solutions" }
  ];

  const SERVICE_IMAGE_KEY_OPTIONS = [
    { key: "palace_resort", label: "Luxury Palace & Resort" },
    { key: "fine_dining", label: "Fine Dining & Café Interior" },
    { key: "mixology_bar", label: "Mixology & Craft Bar" },
    { key: "hospitality_training", label: "Hospitality Training Academy" },
    { key: "hero_hotel", label: "Luxury Hotel Lobby & Operations" },
    { key: "executive_recruitment", label: "Executive Recruitment & Talent" },
    { key: "concept_design", label: "Concept & Space Design" },
    { key: "management_solutions", label: "Hospitality Management Solutions" }
  ];

  function renderServicesManager(container) {
    const writable = canWrite("services");
    const allServices = (state.data.services || []).slice().sort((a, b) => Number(a.sort_order || 99) - Number(b.sort_order || 99));
    const publishedCount = allServices.filter((s) => s.published !== false).length;
    const hiddenCount = allServices.length - publishedCount;

    const addBtn = writable
      ? el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "+ Add New Service",
          onClick: () => openServiceEditorModal(null)
        })
      : null;

    container.appendChild(
      renderModuleHeader(
        "Services Control Center",
        "Every service shown in the MILA app is managed live here (" + allServices.length + " total • " + publishedCount + " live • " + hiddenCount + " hidden). Edit, reorder, hide, add, or delete any service anytime.",
        addBtn
      )
    );

    const searchInput = el("input", {
      className: "search-input",
      type: "search",
      placeholder: "Search services by title, division, or summary...",
      value: state.searchQuery
    });
    const listWrap = el("div", { className: "records-list" });

    function drawList() {
      clearNode(listWrap);
      const q = (searchInput.value || "").trim().toLowerCase();
      const filtered = allServices.filter((s) => {
        if (!q) return true;
        const hay = [s.title, s.subtitle, s.division_name, s.summary, s.typical_timeline].join(" ").toLowerCase();
        return hay.includes(q);
      });

      if (!filtered.length) {
        listWrap.appendChild(
          el("div", {
            className: "empty-state",
            text: allServices.length
              ? "No services match your search."
              : "No services exist yet. Tap '+ Add New Service' to publish your first service to the MILA app."
          })
        );
        return;
      }

      filtered.forEach((svc, idx) => {
        const badgeNum = String(svc.sort_order || idx + 1).padStart(2, "0");
        const includedCount = Array.isArray(svc.services_included) ? svc.services_included.length : 0;
        const benefitsCount = Array.isArray(svc.owner_benefits) ? svc.owner_benefits.length : 0;
        const deliverablesCount = Array.isArray(svc.deliverables) ? svc.deliverables.length : 0;

        const actions = [];
        if (writable) {
          actions.push(
            el("button", {
              className: "btn btn-gold btn-sm",
              type: "button",
              text: "Edit Service",
              onClick: () => openServiceEditorModal(svc)
            }),
            el("button", {
              className: "btn btn-outline btn-sm",
              type: "button",
              text: svc.published ? "Hide from App" : "Publish Live",
              onClick: async () => {
                try {
                  await mutateTable(
                    "PATCH",
                    "/rest/v1/services?id=eq." + encodeURIComponent(svc.id),
                    { published: !svc.published },
                    {
                      module: "SERVICE_MANAGEMENT",
                      action: "UPDATE",
                      targetId: svc.id,
                      summary: (svc.published ? "Hid" : "Published") + " service: " + (svc.title || svc.slug)
                    }
                  );
                  showToast(svc.published ? "Service hidden from the MILA app." : "Service published live to the MILA app!");
                  await refreshAllData(false);
                } catch (err) {
                  showToast(err.message || "Could not update service visibility.");
                }
              }
            }),
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete",
              onClick: () =>
                confirmDelete(
                  "Delete Service Permanently?",
                  "Delete '" + (svc.title || "Service") + "'? It will be removed immediately from the MILA app.",
                  async () => {
                    await mutateTable(
                      "DELETE",
                      "/rest/v1/services?id=eq." + encodeURIComponent(svc.id),
                      null,
                      {
                        module: "SERVICE_MANAGEMENT",
                        action: "DELETE",
                        targetId: svc.id,
                        summary: "Deleted service: " + (svc.title || svc.slug)
                      }
                    );
                    showToast("Service deleted from MILA app.");
                  }
                )
            })
          );
        }

        const customPhoto = svc.image_url && isSafeServiceImageSource(svc.image_url)
          ? el("img", {
              src: svc.image_url,
              alt: svc.title || "Service cover",
              style: "width:100%;max-height:140px;object-fit:cover;border-radius:10px;border:1px solid rgba(212,175,122,0.28);margin-bottom:10px;"
            })
          : null;

        listWrap.appendChild(
          el("div", { className: "record-card" }, [
            customPhoto,
            el("div", { className: "record-top" }, [
              el("div", { style: "display:flex;gap:6px;flex-wrap:wrap;align-items:center;" }, [
                pill("#" + badgeNum, "gold"),
                pill(svc.published ? "LIVE IN APP" : "HIDDEN", svc.published ? "emerald" : "muted"),
                svc.division_name ? pill(svc.division_name, "muted") : null
              ]),
              el("span", { className: "record-small", text: svc.typical_timeline || "" })
            ]),
            el("div", { className: "record-title", text: svc.title || "Untitled Service" }),
            svc.subtitle ? el("div", { className: "record-small", style: "color:#D4AF7A;margin-top:2px;", text: svc.subtitle }) : null,
            svc.summary ? el("div", { className: "record-meta", style: "margin-top:6px;", text: svc.summary }) : null,
            el("div", {
              className: "record-small",
              style: "margin-top:8px;",
              text: includedCount + " Services Included • " + benefitsCount + " Owner Benefits • " + deliverablesCount + " Deliverables"
            }),
            actions.length ? el("div", { className: "record-actions" }, actions) : null
          ])
        );
      });
    }

    searchInput.addEventListener("input", () => {
      state.searchQuery = searchInput.value;
      drawList();
    });

    container.appendChild(el("div", { className: "toolbar" }, [searchInput]));
    container.appendChild(listWrap);
    drawList();
  }

  function openServiceEditorModal(existing) {
    const isEdit = Boolean(existing && existing.id);
    const nextOrder = (state.data.services || []).length + 1;

    const modalErr = el("div", { className: "error-box hidden" });
    const fTitle = el("input", {
      value: existing ? (existing.title || "") : "",
      placeholder: "e.g. Hospitality Pre-Opening Consultancy"
    });
    const fSubtitle = el("input", {
      value: existing ? (existing.subtitle || "") : "",
      placeholder: "e.g. Turnkey Critical Path & Opening Governance"
    });

    const fDivision = el(
      "select",
      { style: "width:100%;" },
      SERVICE_DIVISION_OPTIONS.map((d) => el("option", { value: d, text: d }))
    );
    const initDiv = existing && existing.division_name ? existing.division_name : "MILA Consultancy";
    if (!SERVICE_DIVISION_OPTIONS.includes(initDiv)) {
      fDivision.appendChild(el("option", { value: initDiv, text: initDiv }));
    }
    fDivision.value = initDiv;

    const fServiceType = el(
      "select",
      { style: "width:100%;" },
      SERVICE_TYPE_OPTIONS.map((o) => el("option", { value: o.key, text: o.label }))
    );
    fServiceType.value = (existing && existing.service_type) || "HOSPITALITY_PRE_OPENING";

    const fTimeline = el("input", {
      value: existing ? (existing.typical_timeline || "") : "6 – 12 Weeks",
      placeholder: "e.g. 4 – 12 Months"
    });
    const fOrder = el("input", {
      type: "number",
      value: String(existing && existing.sort_order ? existing.sort_order : nextOrder)
    });
    const fSummary = el("textarea", {
      rows: "3",
      placeholder: "Short summary shown on the Services list card..."
    });
    fSummary.value = existing ? (existing.summary || "") : "";

    const fDetailed = el("textarea", {
      rows: "4",
      placeholder: "Detailed overview & advisory mandate shown on the Service Detail page..."
    });
    fDetailed.value = existing ? (existing.detailed_description || "") : "";

    const fIncluded = el("textarea", {
      rows: "4",
      placeholder: "Enter one bullet point per line..."
    });
    fIncluded.value = existing ? arrayToLines(existing.services_included) : "";

    const fBenefits = el("textarea", {
      rows: "4",
      placeholder: "Enter one owner benefit per line..."
    });
    fBenefits.value = existing ? arrayToLines(existing.owner_benefits) : "";

    const fDeliverables = el("textarea", {
      rows: "4",
      placeholder: "Enter one deliverable per line..."
    });
    fDeliverables.value = existing ? arrayToLines(existing.deliverables) : "";

    const fImageKey = el(
      "select",
      { style: "width:100%;" },
      SERVICE_IMAGE_KEY_OPTIONS.map((o) => el("option", { value: o.key, text: o.label }))
    );
    const currentImageKey = existing && existing.image_key ? existing.image_key : "palace_resort";
    const keyAliasMap = {
      dining_interior: "fine_dining",
      operations_mgmt: "hero_hotel",
      talent_recruitment: "executive_recruitment",
      concept_dev: "concept_design"
    };
    const normalizedKey = keyAliasMap[currentImageKey] || currentImageKey;
    if (SERVICE_IMAGE_KEY_OPTIONS.some((o) => o.key === normalizedKey)) {
      fImageKey.value = normalizedKey;
    }

    let customImageData = existing && existing.image_url ? String(existing.image_url).trim() : "";
    const imgStatus = el("div", {
      className: "record-small",
      text: customImageData ? "Custom cover photo attached." : "Using built-in luxury cover photo theme selected above."
    });
    const fImageUrl = el("input", {
      type: "url",
      placeholder: "Or paste HTTPS image URL (https://...)",
      value: customImageData.startsWith("https://") ? customImageData : ""
    });
    fImageUrl.addEventListener("input", () => {
      const v = fImageUrl.value.trim();
      if (v) {
        customImageData = v;
        imgStatus.textContent = "Using custom HTTPS image URL.";
      } else if (customImageData.startsWith("https://")) {
        customImageData = "";
        imgStatus.textContent = "Using built-in luxury cover photo theme.";
      }
    });

    const fFile = el("input", { type: "file", accept: "image/*", style: "display:none;" });
    const uploadBtn = el("button", {
      className: "btn btn-outline btn-full",
      type: "button",
      text: customImageData ? "Change Cover Photo from Gallery" : "Upload Custom Cover Photo (Optional)",
      onClick: () => fFile.click()
    });
    fFile.addEventListener("change", async () => {
      const file = fFile.files && fFile.files[0];
      if (!file) return;
      uploadBtn.disabled = true;
      uploadBtn.textContent = "Optimizing photo...";
      try {
        customImageData = await compressBannerImageFile(file);
        fImageUrl.value = "";
        const kb = Math.round((customImageData.length * 0.75) / 1024);
        imgStatus.textContent = "Custom photo ready (" + kb + " KB). Tap Save Service below.";
      } catch (err) {
        showToast(err.message || "Could not process image.");
      } finally {
        uploadBtn.disabled = false;
        uploadBtn.textContent = customImageData ? "Change Cover Photo from Gallery" : "Upload Custom Cover Photo (Optional)";
      }
    });

    const clearImgBtn = el("button", {
      className: "btn btn-ghost btn-sm",
      type: "button",
      text: "Use Built-In Photo",
      onClick: () => {
        customImageData = "";
        fImageUrl.value = "";
        fFile.value = "";
        uploadBtn.textContent = "Upload Custom Cover Photo (Optional)";
        imgStatus.textContent = "Reverted to built-in luxury cover photo theme.";
      }
    });

    const fPub = el("input", { type: "checkbox", checked: existing ? Boolean(existing.published) : true });

    const saveBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: isEdit ? "Save Service Changes" : "Publish New Service",
      onClick: async () => {
        modalErr.classList.add("hidden");
        const title = fTitle.value.trim();
        if (!title) {
          modalErr.textContent = "Please enter a Service Title.";
          modalErr.classList.remove("hidden");
          return;
        }
        if (!isSafeServiceImageSource(customImageData)) {
          modalErr.textContent = "Custom Image URL must start with https://";
          modalErr.classList.remove("hidden");
          return;
        }

        const payload = {
          title,
          subtitle: fSubtitle.value.trim(),
          division_name: fDivision.value,
          service_type: fServiceType.value,
          typical_timeline: fTimeline.value.trim(),
          sort_order: Math.max(1, parseInt(fOrder.value, 10) || nextOrder),
          summary: fSummary.value.trim(),
          detailed_description: fDetailed.value.trim() || fSummary.value.trim(),
          services_included: parseLinesToArray(fIncluded.value),
          owner_benefits: parseLinesToArray(fBenefits.value),
          deliverables: parseLinesToArray(fDeliverables.value),
          image_key: fImageKey.value,
          image_url: customImageData || null,
          published: fPub.checked
        };

        if (!isEdit) {
          const baseSlug = title.toLowerCase().replace(/[^a-z0-9]+/g, "-").replace(/^-+|-+$/g, "").slice(0, 48) || "service";
          payload.slug = baseSlug + "-" + String(Date.now()).slice(-4);
        }

        saveBtn.disabled = true;
        saveBtn.textContent = "Saving...";
        try {
          await mutateTable(
            isEdit ? "PATCH" : "POST",
            isEdit ? "/rest/v1/services?id=eq." + encodeURIComponent(existing.id) : "/rest/v1/services",
            payload,
            {
              module: "SERVICE_MANAGEMENT",
              action: isEdit ? "UPDATE" : "CREATE",
              targetId: isEdit ? existing.id : payload.slug,
              summary: (isEdit ? "Updated" : "Created") + " service: " + title
            }
          );
          closeModal();
          showToast(isEdit ? "Service updated live in the MILA app!" : "New service published live to the MILA app!");
          await refreshAllData(false);
        } catch (err) {
          modalErr.textContent = (err && err.message) || "Could not save service.";
          modalErr.classList.remove("hidden");
          saveBtn.disabled = false;
          saveBtn.textContent = isEdit ? "Save Service Changes" : "Publish New Service";
        }
      }
    });

    openModal(
      isEdit ? "Edit Service: " + (existing.title || "") : "Add New Service",
      [
        modalErr,
        buildFormField("Service Title *", fTitle),
        buildFormField("Subtitle / Tagline", fSubtitle),
        buildFormField("Business Division", fDivision),
        buildFormField("Enquiry Category Mapping", fServiceType),
        buildFormField("Typical Timeline", fTimeline),
        buildFormField("Display Order (#)", fOrder),
        buildFormField("Short Card Summary", fSummary),
        buildFormField("Full Overview & Advisory Mandate", fDetailed),
        buildFormField("Services Included (one item per line)", fIncluded),
        buildFormField("Benefits for Business Owners (one item per line)", fBenefits),
        buildFormField("Deliverables (one item per line)", fDeliverables),
        buildFormField("Built-In Cover Photo Theme", fImageKey),
        el("div", { className: "field" }, [
          el("label", { text: "Custom Cover Photo (Optional)" }),
          fFile,
          uploadBtn,
          el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:8px;margin-top:8px;flex-wrap:wrap;" }, [
            imgStatus,
            clearImgBtn
          ])
        ]),
        buildFormField("Or Paste Custom HTTPS Image URL", fImageUrl),
        el("label", { className: "checkbox-row" }, [fPub, "Published (Show live in the MILA app)"])
      ],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        saveBtn
      ]
    );
  }

  // Generic CMS list renderer for Portfolio, Team, Pages
  function renderCmsTable(container, cfg) {
    const writable = canWrite(cfg.moduleId);
    const addBtn = writable
      ? el("button", { className: "btn btn-gold", type: "button", text: "+ " + cfg.addLabel, onClick: () => openGenericCmsModal(cfg, null) })
      : null;
    container.appendChild(renderModuleHeader(cfg.title, cfg.subtitle + " (" + cfg.items.length + ").", addBtn));
    const list = el("div", { className: "records-list" });
    if (!cfg.items.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No records found." }));
    } else {
      cfg.items.forEach((item) => {
        const actions = [];
        if (writable) {
          actions.push(
            el("button", { className: "btn btn-outline btn-sm", type: "button", text: "Edit", onClick: () => openGenericCmsModal(cfg, item) }),
            el("button", {
              className: "btn btn-ghost btn-sm",
              type: "button",
              text: item.published ? "Unpublish" : "Publish",
              onClick: async () => {
                await mutateTable(
                  "PATCH",
                  "/rest/v1/" + cfg.table + "?id=eq." + encodeURIComponent(item.id),
                  { published: !item.published },
                  { module: cfg.auditModule, action: "UPDATE", targetId: item.id, summary: "Toggled publish on " + cfg.getTitle(item) }
                );
                showToast("Visibility updated.");
                await refreshAllData(false);
              }
            }),
            el("button", {
              className: "btn btn-danger btn-sm",
              type: "button",
              text: "Delete",
              onClick: () =>
                confirmDelete("Delete Record?", "Delete '" + cfg.getTitle(item) + "'?", async () => {
                  await mutateTable("DELETE", "/rest/v1/" + cfg.table + "?id=eq." + encodeURIComponent(item.id), null, {
                    module: cfg.auditModule,
                    action: "DELETE",
                    targetId: item.id,
                    summary: "Deleted " + cfg.getTitle(item)
                  });
                  showToast("Record deleted.");
                })
            })
          );
        }
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill(item.published ? "Published" : "Hidden", item.published ? "emerald" : "muted"),
              el("span", { className: "record-small", text: cfg.getBadge(item) || "" })
            ]),
            el("div", { className: "record-title", text: cfg.getTitle(item) }),
            el("div", { className: "record-meta", text: cfg.getBody(item) }),
            actions.length ? el("div", { className: "record-actions" }, actions) : null
          ])
        );
      });
    }
    container.appendChild(list);
  }

  function openGenericCmsModal(cfg, item) {
    const isEdit = Boolean(item && item.id);
    const controls = {};
    const bodyNodes = cfg.fields.map((f) => {
      const rawVal = (item && item[f.key] !== undefined && item[f.key] !== null) ? item[f.key] : (f.defaultValue ?? "");
      let input;
      if (f.type === "lines") {
        input = el("textarea", { rows: "4", placeholder: "Enter one item per line..." });
        input.value = Array.isArray(rawVal) ? arrayToLines(rawVal) : String(rawVal || "");
      } else if (f.type === "textarea") {
        input = el("textarea", { value: String(rawVal) });
      } else if (f.type === "number") {
        input = el("input", { type: "number", value: String(rawVal) });
      } else {
        input = el("input", { value: String(rawVal) });
      }
      controls[f.key] = { input, type: f.type };
      return buildFormField(f.label, input);
    });
    const fPub = el("input", { type: "checkbox", checked: item ? Boolean(item.published) : true });
    bodyNodes.push(el("label", { className: "checkbox-row" }, [fPub, "Published"]));

    openModal(
      isEdit ? "Edit " + cfg.singular : "Add " + cfg.singular,
      bodyNodes,
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save",
          onClick: async () => {
            const payload = { published: fPub.checked };
            Object.entries(controls).forEach(([k, meta]) => {
              if (meta.type === "number") {
                payload[k] = parseInt(meta.input.value, 10) || 1;
              } else if (meta.type === "lines") {
                payload[k] = parseLinesToArray(meta.input.value);
              } else {
                payload[k] = meta.input.value.trim();
              }
            });
            if (!isEdit && cfg.onBeforeCreate) cfg.onBeforeCreate(payload);
            try {
              await mutateTable(
                isEdit ? "PATCH" : "POST",
                isEdit ? "/rest/v1/" + cfg.table + "?id=eq." + encodeURIComponent(item.id) : "/rest/v1/" + cfg.table,
                payload,
                { module: cfg.auditModule, action: isEdit ? "UPDATE" : "CREATE", targetId: isEdit ? item.id : "new", summary: "Saved " + cfg.singular }
              );
              closeModal();
              showToast(cfg.singular + " saved.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Save failed.");
            }
          }
        })
      ]
    );
  }

  function renderAdmins(container) {
    const writable = canWrite("admins");
    const addBtn = writable
      ? el("button", { className: "btn btn-gold", type: "button", text: "+ Invite Admin Staff", onClick: openInviteAdminModal })
      : null;
    container.appendChild(
      renderModuleHeader("Admin Users & Staff Invites", "Control who can sign in to the external MILA Executive Console (" + state.data.admins.length + " admins).", addBtn)
    );
    const list = el("div", { className: "records-list" });
    state.data.admins.forEach((adm) => {
      const actions = [];
      if (writable && adm.email !== state.admin.email) {
        actions.push(
          el("button", {
            className: "btn btn-outline btn-sm",
            type: "button",
            text: adm.active ? "Suspend Admin" : "Activate Admin",
            onClick: async () => {
              await mutateTable(
                "PATCH",
                "/rest/v1/admin_users?id=eq." + encodeURIComponent(adm.id),
                { active: !adm.active },
                { module: "ADMIN_USER_MANAGEMENT", action: "UPDATE_ROLE", targetId: adm.email, summary: "Set active=" + !adm.active + " for " + adm.email }
              );
              showToast("Admin status updated.");
              await refreshAllData(false);
            }
          })
        );
      }
      list.appendChild(
        el("div", { className: "record-card" }, [
          el("div", { className: "record-top" }, [
            pill(ROLE_LABELS[adm.role] || adm.role, "gold"),
            pill(adm.active ? "Active" : "Suspended", adm.active ? "emerald" : "alert")
          ]),
          el("div", { className: "record-title", text: adm.display_name || "MILA Staff" }),
          el("div", { className: "record-meta", text: adm.email || "" }),
          actions.length ? el("div", { className: "record-actions" }, actions) : null
        ])
      );
    });

    if (state.data.invites.length) {
      container.appendChild(list);
      container.appendChild(el("h3", { className: "serif", text: "Pending Staff Invites", style: "margin:20px 0 12px;" }));
      const invList = el("div", { className: "records-list" });
      state.data.invites.forEach((inv) => {
        invList.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [pill(ROLE_LABELS[inv.role] || inv.role, "gold"), pill("Pending Invite", "muted")]),
            el("div", { className: "record-title", text: (inv.display_name || "Staff") + " — " + inv.email }),
            writable
              ? el("div", { className: "record-actions" }, [
                  el("button", {
                    className: "btn btn-danger btn-sm",
                    type: "button",
                    text: "Revoke Invite",
                    onClick: () =>
                      confirmDelete("Revoke Staff Invite?", "Revoke invite for " + inv.email + "?", async () => {
                        await mutateTable("DELETE", "/rest/v1/admin_invites?id=eq." + encodeURIComponent(inv.id), null, {
                          module: "ADMIN_USER_MANAGEMENT",
                          action: "REVOKE_INVITE",
                          targetId: inv.email,
                          summary: "Revoked invite for " + inv.email
                        });
                        showToast("Invite revoked.");
                      })
                  })
                ])
              : null
          ])
        );
      });
      container.appendChild(invList);
    } else {
      container.appendChild(list);
    }
  }

  function openInviteAdminModal() {
    const fEmail = el("input", { type: "email", placeholder: "staff@milahospitality.in" });
    const fName = el("input", { placeholder: "Full Name" });
    const fRole = buildSelect(Object.keys(ROLE_LABELS), "CONSULTANT");
    openModal(
      "Invite MILA Admin Staff",
      [buildFormField("Staff Email *", fEmail), buildFormField("Display Name *", fName), buildFormField("Role", fRole)],
      [
        el("button", { className: "btn btn-ghost", type: "button", text: "Cancel", onClick: closeModal }),
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Invite",
          onClick: async () => {
            const email = fEmail.value.trim().toLowerCase();
            if (!email || !email.includes("@")) {
              showToast("Enter a valid email address.");
              return;
            }
            try {
              await mutateTable(
                "POST",
                "/rest/v1/admin_invites?on_conflict=email",
                {
                  email,
                  display_name: fName.value.trim() || "MILA Staff",
                  role: fRole.value,
                  active: true,
                  created_by: state.admin.uid || undefined
                },
                {
                  module: "ADMIN_USER_MANAGEMENT",
                  action: "INVITE",
                  targetId: email,
                  summary: "Invited " + email + " as " + fRole.value,
                  prefer: "resolution=merge-duplicates,return=minimal"
                }
              );
              closeModal();
              showToast("Staff invite saved.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Invite failed.");
            }
          }
        })
      ]
    );
  }

  function renderSettings(container) {
    const writable = canWrite("settings");
    const s = state.data.settings || {
      maintenance_mode: false,
      allow_client_registrations: true,
      allow_candidate_registrations: true,
      require_cv_for_applications: false,
      default_enquiry_sla_hours: 24,
      support_email: "advisory@milahospitality.in",
      office_regions: "Mumbai • New Delhi NCR • Bengaluru • Rajasthan & Goa"
    };
    container.appendChild(renderModuleHeader("Platform Settings", "Global operational switches for MILA Hospitality."));

    const fMaint = el("input", { type: "checkbox", checked: Boolean(s.maintenance_mode), disabled: !writable });
    const fClientReg = el("input", { type: "checkbox", checked: Boolean(s.allow_client_registrations), disabled: !writable });
    const fCandReg = el("input", { type: "checkbox", checked: Boolean(s.allow_candidate_registrations), disabled: !writable });
    const fReqCv = el("input", { type: "checkbox", checked: Boolean(s.require_cv_for_applications), disabled: !writable });
    const fSla = el("input", { type: "number", value: String(s.default_enquiry_sla_hours ?? 24), disabled: !writable });
    const fEmail = el("input", { value: s.support_email || "", disabled: !writable });
    const fRegions = el("input", { value: s.office_regions || "", disabled: !writable });

    const card = el("div", { className: "record-card" }, [
      el("label", { className: "checkbox-row" }, [fMaint, "Maintenance Mode"]),
      el("label", { className: "checkbox-row" }, [fClientReg, "Allow Client Registrations"]),
      el("label", { className: "checkbox-row" }, [fCandReg, "Allow Candidate Registrations"]),
      el("label", { className: "checkbox-row" }, [fReqCv, "Require CV for Job Applications"]),
      buildFormField("Default Enquiry SLA (Hours)", fSla),
      buildFormField("Support Email", fEmail),
      buildFormField("Office Regions", fRegions),
      writable
        ? el("button", {
            className: "btn btn-gold",
            type: "button",
            text: "Save Platform Settings",
            onClick: async () => {
              try {
                await mutateTable(
                  "PATCH",
                  "/rest/v1/platform_settings?id=eq.platform",
                  {
                    maintenance_mode: fMaint.checked,
                    allow_client_registrations: fClientReg.checked,
                    allow_candidate_registrations: fCandReg.checked,
                    require_cv_for_applications: fReqCv.checked,
                    default_enquiry_sla_hours: Math.max(1, Math.min(168, parseInt(fSla.value, 10) || 24)),
                    support_email: fEmail.value.trim(),
                    office_regions: fRegions.value.trim()
                  },
                  { module: "SETTINGS", action: "UPDATE", targetId: "platform", summary: "Updated platform settings" }
                );
                showToast("Platform settings saved.");
                await refreshAllData(false);
              } catch (err) {
                showToast(err.message || "Failed to save settings.");
              }
            }
          })
        : null
    ]);
    container.appendChild(card);
  }

  function renderLogs(container) {
    container.appendChild(renderModuleHeader("Activity Audit Logs", "Immutable record of every admin mutation and CV download (" + state.data.logs.length + ")."));
    const list = el("div", { className: "records-list" });
    if (!state.data.logs.length) {
      list.appendChild(el("div", { className: "empty-state", text: "No audit logs yet." }));
    } else {
      state.data.logs.forEach((log) => {
        list.appendChild(
          el("div", { className: "record-card" }, [
            el("div", { className: "record-top" }, [
              pill(log.module || "ADMIN", "gold"),
              pill(log.action || "EVENT", "muted")
            ]),
            el("div", { className: "record-title", text: log.summary || "" }),
            el("div", { className: "record-small", text: (log.actor_email || "") + " (" + (log.actor_role || "") + ") • Target: " + (log.target_id || "") })
          ])
        );
      });
    }
    container.appendChild(list);
  }

  // =========================================================================
  // TOP HOME BANNER & POPUP ALERTS (DESIGNER + IMAGE UPLOADER)
  // Stored cleanly in public.website_content with page_key prefix "banner_"
  // =========================================================================

  const BANNER_TAG_PRESETS = [
    "NEW OPENING ALERT",
    "UPCOMING PROJECT",
    "FEATURED AD",
    "SPECIAL ANNOUNCEMENT",
    "URGENT HIRING",
    "EXCLUSIVE OFFER"
  ];

  const BANNER_THEMES = [
    { key: "gold", label: "Champagne Gold (Luxury)", accent: "#D4AF7A", bg: "linear-gradient(135deg, #2B2418, #141715)" },
    { key: "emerald", label: "Emerald Launch", accent: "#4CAF7D", bg: "linear-gradient(135deg, #122720, #111614)" },
    { key: "obsidian", label: "Royal Obsidian", accent: "#E3C796", bg: "linear-gradient(135deg, #1E2228, #0E1110)" },
    { key: "terracotta", label: "Terracotta Spotlight", accent: "#D97B66", bg: "linear-gradient(135deg, #2C1B17, #151312)" }
  ];

  const BANNER_ACTIONS = [
    { key: "contact_us", label: "Open Service Enquiry Form" },
    { key: "our_services", label: "Go to Our Services" },
    { key: "careers", label: "Go to Careers / Jobs" },
    { key: "our_portfolio", label: "Go to Our Portfolio" },
    { key: "popup_only", label: "Show Popup Details Only" }
  ];

  function isPromoBannerRow(row) {
    return Boolean(row && String(row.page_key || "").toLowerCase().startsWith("banner_"));
  }

  function isContactDetailsRow(row) {
    return Boolean(row && String(row.page_key || "").toLowerCase() === "contact_details");
  }

  function renderContactInfoManager(container) {
    const writable = canWrite("contact_info");
    const existing = (state.data.pages || []).find(isContactDetailsRow) || null;
    const hasSavedRecord = Boolean(existing && existing.id);
    const currentEmail = hasSavedRecord ? String(existing.eyebrow || "").trim() : "mdkhanrashid2@gmail.com";
    const currentPhone = hasSavedRecord ? String(existing.headline || "").trim() : "+916363262421";
    const currentWindow = (existing && existing.subheadline) ? existing.subheadline : "Partner Response Window: Within 1 Business Day (IST)";
    const currentNote = (existing && existing.body_copy) ? existing.body_copy : "Reach our partner desk directly by phone or email for consultancy mandates, pre-opening advisory, or talent partnerships.";

    container.appendChild(
      renderModuleHeader(
        "Contact Email & Phone",
        "Manage the official email ID and phone number shown whenever users tap 'Contact Us' in the MILA app. Edit or delete either field anytime — changes go live immediately."
      )
    );

    async function saveContactPayload(nextEmail, nextPhone, nextWindow, nextNote, summaryMsg, toastMsg) {
      const payload = {
        page_key: "contact_details",
        page_title: "Contact Details",
        eyebrow: nextEmail,
        headline: nextPhone,
        subheadline: nextWindow || "Partner Response Window: Within 1 Business Day (IST)",
        body_copy: nextNote || "",
        hero_image_key: "contact_desk",
        cta_label: "Contact Us",
        published: true
      };
      if (existing && existing.id) {
        await mutateTable(
          "PATCH",
          "/rest/v1/website_content?id=eq." + encodeURIComponent(existing.id),
          payload,
          { module: "WEBSITE_CONTENT_MANAGEMENT", action: "UPDATE", targetId: existing.id, summary: summaryMsg }
        );
      } else {
        await mutateTable(
          "POST",
          "/rest/v1/website_content",
          payload,
          { module: "WEBSITE_CONTENT_MANAGEMENT", action: "CREATE", targetId: "contact_details", summary: summaryMsg }
        );
      }
      showToast(toastMsg);
      await refreshAllData(false);
    }

    // Live summary card showing current phone & email with quick Delete buttons
    const liveSummaryRows = [];
    liveSummaryRows.push(
      el("div", { className: "record-top" }, [
        pill(currentEmail || currentPhone ? "Live on Contact Us Screen" : "Hidden / Cleared", currentEmail || currentPhone ? "emerald" : "muted"),
        el("span", { className: "record-small", text: existing && existing.updated_at ? "Updated " + formatTimestamp(existing.updated_at) : "Default MILA Contact Active" })
      ])
    );

    liveSummaryRows.push(
      el("div", { style: "display:grid;gap:10px;margin:10px 0 14px;" }, [
        el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;padding:12px;border-radius:10px;border:1px solid rgba(212,175,122,0.22);background:rgba(15,18,16,0.55);" }, [
          el("div", null, [
            el("div", { className: "record-small", text: "CURRENT EMAIL ID" }),
            el("div", { className: "record-title", style: "margin-top:2px;font-size:15px;", text: currentEmail || "Deleted / Not shown in app" })
          ]),
          writable && currentEmail
            ? el("button", {
                className: "btn btn-danger btn-sm",
                type: "button",
                text: "Delete Email",
                onClick: () =>
                  confirmDelete(
                    "Delete Contact Email?",
                    "Remove '" + currentEmail + "' from the MILA app Contact Us screen?",
                    async () => {
                      await saveContactPayload(
                        "",
                        currentPhone,
                        currentWindow,
                        currentNote,
                        "Deleted official contact email (" + currentEmail + ")",
                        "Contact email deleted from MILA app."
                      );
                    }
                  )
              })
            : null
        ]),
        el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:10px;flex-wrap:wrap;padding:12px;border-radius:10px;border:1px solid rgba(212,175,122,0.22);background:rgba(15,18,16,0.55);" }, [
          el("div", null, [
            el("div", { className: "record-small", text: "CURRENT PHONE NUMBER" }),
            el("div", { className: "record-title", style: "margin-top:2px;font-size:15px;", text: currentPhone || "Deleted / Not shown in app" })
          ]),
          writable && currentPhone
            ? el("button", {
                className: "btn btn-danger btn-sm",
                type: "button",
                text: "Delete Number",
                onClick: () =>
                  confirmDelete(
                    "Delete Contact Phone Number?",
                    "Remove '" + currentPhone + "' from the MILA app Contact Us screen?",
                    async () => {
                      await saveContactPayload(
                        currentEmail,
                        "",
                        currentWindow,
                        currentNote,
                        "Deleted official contact phone (" + currentPhone + ")",
                        "Contact phone number deleted from MILA app."
                      );
                    }
                  )
              })
            : null
        ])
      ])
    );

    const fEmail = el("input", {
      type: "email",
      value: currentEmail,
      placeholder: "mdkhanrashid2@gmail.com (leave blank to hide email)",
      disabled: !writable
    });
    const fPhone = el("input", {
      type: "tel",
      value: currentPhone,
      placeholder: "+916363262421 (leave blank to hide phone)",
      disabled: !writable
    });
    const fWindow = el("input", {
      value: currentWindow,
      placeholder: "Partner Response Window: Within 1 Business Day (IST)",
      disabled: !writable
    });
    const fNote = el("textarea", {
      value: currentNote,
      placeholder: "Short note shown on Contact Us screen...",
      disabled: !writable
    });

    const actionButtons = [];
    if (writable) {
      actionButtons.push(
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "Save Contact Details",
          onClick: async () => {
            const emailVal = fEmail.value.trim();
            const phoneVal = fPhone.value.trim();
            const emailRegex = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;
            if (emailVal && !emailRegex.test(emailVal)) {
              showToast("Please enter a valid email address (or leave it blank to hide).");
              return;
            }
            if (phoneVal && phoneVal.replace(/[^\d+]/g, "").length < 7) {
              showToast("Please enter a valid phone number (or leave it blank to hide).");
              return;
            }
            try {
              await saveContactPayload(
                emailVal,
                phoneVal,
                fWindow.value.trim() || "Partner Response Window: Within 1 Business Day (IST)",
                fNote.value.trim(),
                "Updated official contact email (" + (emailVal || "none") + ") and phone (" + (phoneVal || "none") + ")",
                "Contact email & phone updated live in the MILA app!"
              );
            } catch (err) {
              showToast(err.message || "Could not save contact details.");
            }
          }
        })
      );

      if (currentEmail || currentPhone) {
        actionButtons.push(
          el("button", {
            className: "btn btn-danger btn-sm",
            type: "button",
            text: "Delete Both Email & Number",
            onClick: () =>
              confirmDelete(
                "Delete Both Email & Phone?",
                "Remove both the email ID and phone number from the MILA app Contact Us screen? You can add new ones anytime.",
                async () => {
                  await saveContactPayload(
                    "",
                    "",
                    fWindow.value.trim() || currentWindow,
                    fNote.value.trim(),
                    "Deleted both official contact email and phone number",
                    "Contact email and phone number deleted from MILA app."
                  );
                }
              )
          })
        );
      }
    }

    const card = el("div", { className: "record-card" }, [
      ...liveSummaryRows,
      buildFormField("Edit Official Contact Email", fEmail),
      buildFormField("Edit Official Phone Number", fPhone),
      buildFormField("Partner Response Window", fWindow),
      buildFormField("Contact Desk Note", fNote),
      actionButtons.length ? el("div", { className: "record-actions" }, actionButtons) : null
    ]);

    container.appendChild(card);
  }

  function getPromoBanners() {
    return (state.data.pages || [])
      .filter(isPromoBannerRow)
      .sort((a, b) => String(b.updated_at || "").localeCompare(String(a.updated_at || "")));
  }

  function parseBannerConfig(heroImageKey) {
    const raw = String(heroImageKey || "");
    const parts = {};
    raw.split("|").forEach((seg) => {
      const idx = seg.indexOf(":");
      if (idx > 0) {
        parts[seg.slice(0, idx).trim().toLowerCase()] = seg.slice(idx + 1).trim();
      }
    });
    return {
      theme: parts.theme || "gold",
      showPopup: String(parts.popup || "").toLowerCase() === "on",
      actionRoute: parts.action || "contact_us"
    };
  }

  function serializeBannerConfig(theme, showPopup, actionRoute) {
    return "theme:" + (theme || "gold") + "|popup:" + (showPopup ? "on" : "off") + "|action:" + (actionRoute || "contact_us");
  }

  function bannerThemeStyle(themeKey) {
    return BANNER_THEMES.find((t) => t.key === themeKey) || BANNER_THEMES[0];
  }

  function compressBannerImageFile(file) {
    return new Promise((resolve, reject) => {
      const reader = new FileReader();
      reader.onerror = () => reject(new Error("Could not read selected image file."));
      reader.onload = () => {
        const img = new Image();
        img.onerror = () => reject(new Error("Unsupported image format. Please choose a JPG, PNG, or WebP photo."));
        img.onload = () => {
          try {
            const maxWidth = 960;
            const scale = img.width > maxWidth ? maxWidth / img.width : 1;
            const targetW = Math.max(1, Math.round(img.width * scale));
            const targetH = Math.max(1, Math.round(img.height * scale));
            const canvas = document.createElement("canvas");
            canvas.width = targetW;
            canvas.height = targetH;
            const ctx = canvas.getContext("2d");
            ctx.fillStyle = "#121614";
            ctx.fillRect(0, 0, targetW, targetH);
            ctx.drawImage(img, 0, 0, targetW, targetH);
            const dataUrl = canvas.toDataURL("image/jpeg", 0.75);
            resolve(dataUrl);
          } catch (e) {
            reject(new Error("Could not optimize image. Try a smaller JPG or PNG image."));
          }
        };
        img.src = reader.result;
      };
      reader.readAsDataURL(file);
    });
  }

  function buildBannerCard(banner, canManage) {
    const cfg = parseBannerConfig(banner.hero_image_key);
    const theme = bannerThemeStyle(cfg.theme);
    const actionObj = BANNER_ACTIONS.find((a) => a.key === cfg.actionRoute) || BANNER_ACTIONS[0];
    const updatedLabel = formatTimestamp(banner.updated_at);

    const actions = [];
    if (canManage) {
      actions.push(
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: "Edit Banner",
          onClick: () => openBannerModal(banner)
        })
      );
      actions.push(
        el("button", {
          className: "btn btn-outline btn-sm",
          type: "button",
          text: banner.published ? "Hide Banner" : "Publish Live",
          onClick: async () => {
            try {
              const nextPub = !banner.published;
              await mutateTable(
                "PATCH",
                "/rest/v1/website_content?id=eq." + encodeURIComponent(banner.id),
                { published: nextPub },
                {
                  module: "WEBSITE_CONTENT_MANAGEMENT",
                  action: nextPub ? "PUBLISH_BANNER" : "HIDE_BANNER",
                  targetId: banner.id,
                  summary: (nextPub ? "Published" : "Hid") + " top banner: " + (banner.headline || banner.page_title)
                }
              );
              showToast(nextPub ? "Banner is now live on the app Home screen." : "Banner hidden from users.");
              await refreshAllData(false);
            } catch (err) {
              showToast(err.message || "Could not update banner status.");
            }
          }
        })
      );
      actions.push(
        el("button", {
          className: "btn btn-danger btn-sm",
          type: "button",
          text: "Delete",
          onClick: () =>
            confirmDelete(
              "Delete Top Banner?",
              "Permanently delete '" + (banner.headline || banner.page_title || "Banner") + "' from the MILA app?",
              async () => {
                await mutateTable(
                  "DELETE",
                  "/rest/v1/website_content?id=eq." + encodeURIComponent(banner.id),
                  null,
                  {
                    module: "WEBSITE_CONTENT_MANAGEMENT",
                    action: "DELETE_BANNER",
                    targetId: banner.id,
                    summary: "Deleted top banner: " + (banner.headline || banner.page_title)
                  }
                );
                showToast("Banner deleted.");
              }
            )
        })
      );
    }

    const previewImg = banner.hero_image_url
      ? el("img", {
          src: banner.hero_image_url,
          alt: banner.headline || "Banner preview",
          style: "width:100%;max-height:180px;object-fit:cover;border-radius:10px;border:1px solid rgba(212,175,122,0.35);margin-bottom:8px;"
        })
      : null;

    return el("div", { className: "record-card", style: "background:" + theme.bg + ";border-color:" + theme.accent + "55;" }, [
      previewImg,
      el("div", { className: "record-top" }, [
        el("div", { style: "display:flex;gap:6px;flex-wrap:wrap;align-items:center;" }, [
          pill(banner.page_title || "ANNOUNCEMENT", "gold"),
          pill(banner.published ? "LIVE IN APP" : "HIDDEN DRAFT", banner.published ? "emerald" : "muted"),
          cfg.showPopup ? pill("AUTO-POPUP ON OPEN", "gold") : null
        ])
      ]),
      banner.eyebrow ? el("div", { className: "record-small", style: "color:" + theme.accent + ";font-weight:600;letter-spacing:0.8px;", text: banner.eyebrow.toUpperCase() }) : null,
      el("div", { className: "record-title", text: banner.headline || "Untitled Banner" }),
      banner.subheadline ? el("div", { className: "record-meta", text: banner.subheadline }) : null,
      banner.body_copy && banner.body_copy !== banner.subheadline
        ? el("div", { className: "record-small", text: "Popup Details: " + banner.body_copy.slice(0, 140) + (banner.body_copy.length > 140 ? "..." : "") })
        : null,
      el("div", {
        className: "record-small",
        text: "Button: '" + (banner.cta_label || "Explore Details") + "' → " + actionObj.label + (updatedLabel ? " • Updated: " + updatedLabel : "")
      }),
      actions.length ? el("div", { className: "record-actions" }, actions) : null
    ]);
  }

  function openBannerModal(existing) {
    const isEdit = Boolean(existing && existing.id);
    const existingCfg = parseBannerConfig(existing ? existing.hero_image_key : "");

    let imageDataUrl = existing && existing.hero_image_url ? String(existing.hero_image_url) : "";
    let selectedTag = existing && existing.page_title ? existing.page_title : "NEW OPENING ALERT";
    let selectedTheme = existingCfg.theme || "gold";
    let selectedAction = existingCfg.actionRoute || "contact_us";
    let autoPopupEnabled = existing ? existingCfg.showPopup : true;
    let isPublished = existing ? Boolean(existing.published) : true;

    const tagSelect = el(
      "select",
      { style: "width:100%;" },
      BANNER_TAG_PRESETS.map((t) => el("option", { value: t, text: t }))
    );
    if (BANNER_TAG_PRESETS.includes(selectedTag)) {
      tagSelect.value = selectedTag;
    }

    const customTagInput = el("input", {
      type: "text",
      placeholder: "Or type custom tag (e.g. GRAND LAUNCH, SPECIAL AD)",
      value: selectedTag
    });
    tagSelect.addEventListener("change", () => {
      customTagInput.value = tagSelect.value;
      updateLivePreview();
    });
    customTagInput.addEventListener("input", () => updateLivePreview());

    const eyebrowInput = el("input", {
      type: "text",
      placeholder: "e.g. GOA • OPENING Q3 2027",
      value: existing ? (existing.eyebrow || "") : ""
    });
    const headlineInput = el("input", {
      type: "text",
      placeholder: "e.g. Upcoming Luxury Resort Pre-Opening — North Goa",
      value: existing ? (existing.headline || "") : ""
    });
    const subheadlineInput = el("input", {
      type: "text",
      placeholder: "Short 1-line tagline shown on the top banner",
      value: existing ? (existing.subheadline || "") : ""
    });
    const detailsInput = el("textarea", {
      rows: "4",
      placeholder: "Full announcement, project highlights, dates, or ad details shown when people tap the banner or see the popup..."
    });
    detailsInput.value = existing ? (existing.body_copy || "") : "";

    const ctaInput = el("input", {
      type: "text",
      placeholder: "e.g. Explore Project / Enquire Now / Apply Now",
      value: existing ? (existing.cta_label || "Explore Project") : "Explore Project"
    });

    const themeSelect = el(
      "select",
      { style: "width:100%;" },
      BANNER_THEMES.map((t) => el("option", { value: t.key, text: t.label }))
    );
    themeSelect.value = selectedTheme;
    themeSelect.addEventListener("change", () => {
      selectedTheme = themeSelect.value;
      updateLivePreview();
    });

    const actionSelect = el(
      "select",
      { style: "width:100%;" },
      BANNER_ACTIONS.map((a) => el("option", { value: a.key, text: a.label }))
    );
    actionSelect.value = selectedAction;
    actionSelect.addEventListener("change", () => {
      selectedAction = actionSelect.value;
    });

    const imageStatusText = el("div", {
      className: "record-small",
      text: imageDataUrl ? "Custom banner image attached." : "Optional: Upload any poster, ad graphic, or project photo from your phone gallery."
    });

    const imageUrlInput = el("input", {
      type: "url",
      placeholder: "Or paste an image URL (https://...)",
      value: imageDataUrl.startsWith("http") ? imageDataUrl : ""
    });
    imageUrlInput.addEventListener("input", () => {
      const val = imageUrlInput.value.trim();
      if (val) {
        imageDataUrl = val;
        imageStatusText.textContent = "Using external image URL.";
      } else if (imageDataUrl.startsWith("http")) {
        imageDataUrl = "";
        imageStatusText.textContent = "No image attached (using luxury designed banner).";
      }
      updateLivePreview();
    });

    const fileInput = el("input", {
      type: "file",
      accept: "image/*",
      style: "display:none;"
    });
    const choosePhotoBtn = el("button", {
      className: "btn btn-outline btn-full",
      type: "button",
      text: "Upload Poster / Photo from Phone Gallery",
      onClick: () => fileInput.click()
    });
    fileInput.addEventListener("change", async () => {
      const file = fileInput.files && fileInput.files[0];
      if (!file) return;
      imageStatusText.textContent = "Optimizing image for mobile app...";
      choosePhotoBtn.disabled = true;
      choosePhotoBtn.textContent = "Optimizing photo...";
      try {
        imageDataUrl = await compressBannerImageFile(file);
        imageUrlInput.value = "";
        const kb = Math.round((imageDataUrl.length * 0.75) / 1024);
        imageStatusText.textContent = "Photo uploaded & ready (" + kb + " KB)! Tap 'Post Live Banner' below.";
        updateLivePreview();
      } catch (err) {
        imageStatusText.textContent = err.message || "Image upload failed.";
        showToast(err.message || "Image upload failed.");
      } finally {
        choosePhotoBtn.disabled = false;
        choosePhotoBtn.textContent = imageDataUrl ? "Change Uploaded Poster / Photo" : "Upload Poster / Photo from Phone Gallery";
      }
    });

    const removeImageBtn = el("button", {
      className: "btn btn-outline btn-sm",
      type: "button",
      text: "Remove Image",
      onClick: () => {
        imageDataUrl = "";
        imageUrlInput.value = "";
        fileInput.value = "";
        choosePhotoBtn.textContent = "Upload Poster / Photo from Phone Gallery";
        imageStatusText.textContent = "Image removed. Using MILA luxury designed banner.";
        updateLivePreview();
      }
    });

    const popupCheck = el("input", { type: "checkbox", checked: autoPopupEnabled, style: "width:auto;" });
    popupCheck.addEventListener("change", () => {
      autoPopupEnabled = popupCheck.checked;
      updateLivePreview();
    });

    const pubCheck = el("input", { type: "checkbox", checked: isPublished, style: "width:auto;" });
    pubCheck.addEventListener("change", () => {
      isPublished = pubCheck.checked;
    });

    const modalErrorBox = el("div", { className: "error-box hidden" });

    const previewBox = el("div", {
      style: "border-radius:12px;overflow:hidden;border:1px solid rgba(212,175,122,0.6);margin-bottom:12px;"
    });

    function updateLivePreview() {
      clearNode(previewBox);
      const t = bannerThemeStyle(themeSelect.value);
      previewBox.style.background = t.bg;
      previewBox.style.borderColor = t.accent;
      if (imageDataUrl) {
        previewBox.appendChild(
          el("img", {
            src: imageDataUrl,
            alt: "Live preview",
            style: "width:100%;max-height:170px;object-fit:cover;display:block;"
          })
        );
      }
      previewBox.appendChild(
        el("div", { style: "padding:12px;display:flex;flex-direction:column;gap:5px;" }, [
          el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:6px;flex-wrap:wrap;" }, [
            el("span", {
              style: "font-size:10px;font-weight:700;letter-spacing:1px;padding:3px 8px;border-radius:999px;border:1px solid " + t.accent + ";color:" + t.accent + ";",
              text: (customTagInput.value.trim() || "ANNOUNCEMENT").toUpperCase()
            }),
            popupCheck.checked
              ? el("span", { style: "font-size:10px;color:#F3EFE6;opacity:0.85;", text: "Auto-Popup Enabled" })
              : null
          ]),
          eyebrowInput.value.trim()
            ? el("div", { style: "font-size:11px;color:" + t.accent + ";letter-spacing:1px;font-weight:600;", text: eyebrowInput.value.trim().toUpperCase() })
            : null,
          el("div", { style: "font-size:16px;font-weight:700;color:#F3EFE6;", text: headlineInput.value.trim() || "Your Banner Headline Appears Here" }),
          subheadlineInput.value.trim()
            ? el("div", { style: "font-size:12px;color:#D6D0C4;", text: subheadlineInput.value.trim() })
            : null,
          el("div", { style: "margin-top:4px;display:flex;justify-content:flex-end;" }, [
            el("span", {
              style: "font-size:11px;font-weight:700;padding:5px 10px;border-radius:6px;background:" + t.accent + ";color:#121614;",
              text: (ctaInput.value.trim() || "Explore Project") + " →"
            })
          ])
        ])
      );
    }

    eyebrowInput.addEventListener("input", updateLivePreview);
    headlineInput.addEventListener("input", updateLivePreview);
    subheadlineInput.addEventListener("input", updateLivePreview);
    ctaInput.addEventListener("input", updateLivePreview);
    updateLivePreview();

    const bodyNodes = [
      modalErrorBox,
      el("div", { className: "eyebrow", style: "margin-bottom:6px;", text: "LIVE APP BANNER PREVIEW (TOP OF HOME SCREEN)" }),
      previewBox,
      el("div", { className: "field" }, [
        el("label", { text: "Upload Banner Poster / Ad Image" }),
        fileInput,
        choosePhotoBtn,
        el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:8px;margin-top:8px;flex-wrap:wrap;" }, [
          imageStatusText,
          removeImageBtn
        ])
      ]),
      buildFormField("Or Paste Image URL (Optional)", imageUrlInput),
      buildFormField("Alert Type Preset", tagSelect),
      buildFormField("Banner Tag Label *", customTagInput),
      buildFormField("Small Badge (City / Launch Date)", eyebrowInput),
      buildFormField("Banner Headline *", headlineInput),
      buildFormField("Short Subtitle (Shown on Banner)", subheadlineInput),
      buildFormField("Full Upcoming Project / Ad Details (Shown in Popup)", detailsInput),
      buildFormField("Banner Color Design Theme", themeSelect),
      buildFormField("Button Text", ctaInput),
      buildFormField("When User Taps Button", actionSelect),
      el("label", { style: "display:flex;gap:8px;align-items:center;margin:10px 0;font-size:13px;" }, [
        popupCheck,
        el("span", { text: "Show as a Popup Alert when users open the app" })
      ]),
      el("label", { style: "display:flex;gap:8px;align-items:center;margin:8px 0;font-size:13px;" }, [
        pubCheck,
        el("span", { text: "Published (Show live at the top of the app Home screen right now)" })
      ])
    ];

    const saveBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: isEdit ? "Save Banner Changes" : "Post Live Banner",
      onClick: async () => {
        modalErrorBox.classList.add("hidden");
        const headline = headlineInput.value.trim();
        const tagLabel = customTagInput.value.trim() || "ANNOUNCEMENT";
        if (!headline) {
          modalErrorBox.textContent = "Please enter a Banner Headline before posting.";
          modalErrorBox.classList.remove("hidden");
          showToast("Please enter a Banner Headline.");
          return;
        }
        saveBtn.disabled = true;
        saveBtn.textContent = "Posting...";
        try {
          const payload = {
            page_title: tagLabel,
            eyebrow: eyebrowInput.value.trim(),
            headline: headline,
            subheadline: subheadlineInput.value.trim(),
            body_copy: detailsInput.value.trim() || subheadlineInput.value.trim(),
            hero_image_key: serializeBannerConfig(themeSelect.value, popupCheck.checked, actionSelect.value),
            hero_image_url: imageDataUrl || null,
            cta_label: ctaInput.value.trim() || "Explore Details",
            published: pubCheck.checked,
            updated_by: state.admin && state.admin.uid ? state.admin.uid : null
          };
          if (isEdit) {
            await mutateTable(
              "PATCH",
              "/rest/v1/website_content?id=eq." + encodeURIComponent(existing.id),
              payload,
              {
                module: "WEBSITE_CONTENT_MANAGEMENT",
                action: "UPDATE_BANNER",
                targetId: existing.id,
                summary: "Updated top banner: " + headline
              }
            );
            showToast("Banner updated live!");
          } else {
            const slug = headline.toLowerCase().replace(/[^a-z0-9]+/g, "_").replace(/^_+|_+$/g, "").slice(0, 32) || "promo";
            payload.page_key = "banner_" + slug + "_" + Date.now();
            await mutateTable(
              "POST",
              "/rest/v1/website_content",
              payload,
              {
                module: "WEBSITE_CONTENT_MANAGEMENT",
                action: "CREATE_BANNER",
                targetId: payload.page_key,
                summary: "Posted top banner: " + headline
              }
            );
            showToast("Top banner posted live to the MILA app!");
          }
          closeModal();
          await refreshAllData(false);
        } catch (err) {
          const msg = (err && err.message) || "Could not save banner.";
          modalErrorBox.textContent = msg;
          modalErrorBox.classList.remove("hidden");
          showToast(msg);
          saveBtn.disabled = false;
          saveBtn.textContent = isEdit ? "Save Banner Changes" : "Post Live Banner";
        }
      }
    });

    openModal(
      isEdit ? "Edit Top Banner & Popup" : "Create / Upload Top Banner & Popup",
      bodyNodes,
      [
        el("button", { className: "btn btn-outline", type: "button", text: "Cancel", onClick: closeModal }),
        saveBtn
      ]
    );
  }

  function renderPromoBanners(container) {
    const writable = canWrite("banners");
    const banners = getPromoBanners();
    const addBtn = writable
      ? el("button", {
          className: "btn btn-gold",
          type: "button",
          text: "+ Create / Upload Banner",
          onClick: () => openBannerModal(null)
        })
      : null;

    container.appendChild(
      renderModuleHeader(
        "Top Banner & Popup Alerts",
        "Upload ads, opening alerts, or upcoming project posters. Active banners appear right at the top of the app Home screen and open a rich popup on tap (or automatically on launch).",
        addBtn
      )
    );

    const list = el("div", { className: "records-list" });
    if (!banners.length) {
      list.appendChild(
        el("div", {
          className: "empty-state",
          text: "No banners created yet. Tap '+ Create / Upload Banner' above to design a luxury alert or upload your own poster image."
        })
      );
    } else {
      banners.forEach((b) => list.appendChild(buildBannerCard(b, writable)));
    }
    container.appendChild(list);
  }

  function renderActiveModule() {
    const mod = MODULES.find((m) => m.id === state.activeModule) || MODULES[0];
    document.getElementById("topbarModuleTitle").textContent = mod.label;
    const container = document.getElementById("moduleContent");
    clearNode(container);

    switch (mod.id) {
      case "overview": renderOverview(container); break;
      case "users": renderAppUsers(container); break;
      case "clients": renderClients(container); break;
      case "enquiries": renderEnquiries(container); break;
      case "vacancies": renderVacancies(container); break;
      case "candidates": renderCandidates(container); break;
      case "cvs": renderCvDatabase(container); break;
      case "applications": renderApplications(container); break;
      case "notifications": renderNotifications(container); break;
      case "banners": renderPromoBanners(container); break;
      case "services":
        renderServicesManager(container);
        break;
      case "portfolio":
        renderCmsTable(container, {
          moduleId: "portfolio",
          table: "portfolio_projects",
          auditModule: "PORTFOLIO_MANAGEMENT",
          title: "Portfolio Projects",
          subtitle: "Manage case studies displayed in the client app",
          singular: "Portfolio Project",
          addLabel: "Add Project",
          items: state.data.portfolio,
          getTitle: (p) => (p.property_name || "") + " — " + (p.location || ""),
          getBadge: (p) => p.category || "",
          getBody: (p) => p.summary || "",
          fields: [
            { key: "property_name", label: "Property Name *" },
            { key: "location", label: "Location *" },
            { key: "category", label: "Category", defaultValue: "Luxury Resort" },
            { key: "filter_group", label: "Filter Group", defaultValue: "Pre-Opening" },
            { key: "scale", label: "Scale", defaultValue: "100+ Keys" },
            { key: "summary", label: "Summary", type: "textarea" },
            { key: "outcomes", label: "Key Outcomes (one per line)", type: "lines" },
            { key: "sort_order", label: "Sort Order", type: "number", defaultValue: 1 }
          ],
          onBeforeCreate: (p) => {
            p.slug = (p.property_name || "project").toLowerCase().replace(/[^a-z0-9]+/g, "-") + "-" + (Date.now() % 1000);
            p.image_key = "palace_resort";
            if (!Array.isArray(p.outcomes)) p.outcomes = [];
          }
        });
        break;
      case "testimonials":
        renderReviewsManager(container);
        break;
      case "team":
        renderCmsTable(container, {
          moduleId: "team",
          table: "team_members",
          auditModule: "TEAM_MANAGEMENT",
          title: "Leadership & Team",
          subtitle: "Manage MILA Hospitality advisors and partners",
          singular: "Team Member",
          addLabel: "Add Team Member",
          items: state.data.team,
          getTitle: (t) => (t.full_name || "") + " — " + (t.designation || ""),
          getBadge: (t) => t.division_name || "",
          getBody: (t) => (t.specialty || "") + " • " + (t.credentials || ""),
          fields: [
            { key: "full_name", label: "Full Name *" },
            { key: "designation", label: "Designation *" },
            { key: "division_name", label: "Division", defaultValue: "MILA Consultancy" },
            { key: "specialty", label: "Specialty" },
            { key: "years_experience", label: "Years Experience", type: "number", defaultValue: 12 },
            { key: "credentials", label: "Credentials", type: "textarea" },
            { key: "sort_order", label: "Sort Order", type: "number", defaultValue: 1 }
          ],
          onBeforeCreate: (p) => { p.image_key = "hero_hotel"; }
        });
        break;
      case "contact_info":
        renderContactInfoManager(container);
        break;
      case "pages":
        renderCmsTable(container, {
          moduleId: "pages",
          table: "website_content",
          auditModule: "WEBSITE_CONTENT_MANAGEMENT",
          title: "Website & App Content",
          subtitle: "Edit headlines, copy, and call-to-action labels",
          singular: "Page Content",
          addLabel: "Add Page",
          items: (state.data.pages || []).filter((p) => !isPromoBannerRow(p) && !isContactDetailsRow(p)),
          getTitle: (p) => (p.page_title || "") + " (" + (p.page_key || "") + ")",
          getBadge: (p) => p.eyebrow || "",
          getBody: (p) => p.headline || p.body_copy || "",
          fields: [
            { key: "page_key", label: "Page Key *" },
            { key: "page_title", label: "Page Title *" },
            { key: "eyebrow", label: "Eyebrow" },
            { key: "headline", label: "Headline" },
            { key: "subheadline", label: "Subheadline" },
            { key: "body_copy", label: "Body Copy", type: "textarea" },
            { key: "cta_label", label: "CTA Label", defaultValue: "Explore Services" }
          ],
          onBeforeCreate: (p) => { p.hero_image_key = "hero_hotel"; }
        });
        break;
      case "admins": renderAdmins(container); break;
      case "settings": renderSettings(container); break;
      case "logs": renderLogs(container); break;
    }
  }

  // Bootstrap DOM listeners
  document.addEventListener("DOMContentLoaded", () => {
    const urlInput = document.getElementById("cfgSupabaseUrl");
    const keyInput = document.getElementById("cfgSupabaseKey");
    urlInput.value = state.supabaseUrl;
    keyInput.value = state.supabaseAnonKey;
    updateConnectionBadge();
    if (!state.supabaseUrl || !state.supabaseAnonKey) {
      document.getElementById("connectionDetails").open = true;
    }

    document.getElementById("saveConnBtn").addEventListener("click", () => {
      const nextUrl = normalizeSupabaseUrl(urlInput.value);
      const nextKey = normalizeSupabaseKey(keyInput.value);
      urlInput.value = nextUrl;
      keyInput.value = nextKey;
      if (!isValidSupabaseUrl(nextUrl)) {
        showToast("Please enter a valid https:// Supabase Project URL.");
        return;
      }
      if (!nextKey) {
        showToast("Please enter your Supabase Publishable/Anon Key.");
        return;
      }
      if (isSecretSupabaseKey(nextKey)) {
        showToast("Do not use a Secret / service_role key here. Use the public Publishable or Legacy anon key.");
        return;
      }
      state.supabaseUrl = nextUrl;
      state.supabaseAnonKey = nextKey;
      savePublicEndpoint(nextUrl, nextKey);
      updateConnectionBadge();
      document.getElementById("connectionDetails").open = false;
      showToast("Connection saved on this device — now just sign in with email & password.");
    });

    document.getElementById("loginForm").addEventListener("submit", (e) => {
      e.preventDefault();
      // Auto-apply connection inputs if admin typed them without clicking Save
      const typedUrl = normalizeSupabaseUrl(urlInput.value);
      const typedKey = normalizeSupabaseKey(keyInput.value);
      if (isValidSupabaseUrl(typedUrl) && typedKey) {
        state.supabaseUrl = typedUrl;
        state.supabaseAnonKey = typedKey;
        if (!isSecretSupabaseKey(typedKey)) {
          savePublicEndpoint(typedUrl, typedKey);
        }
        updateConnectionBadge();
      }
      handleLogin(
        document.getElementById("adminEmail").value,
        document.getElementById("adminPassword").value
      );
    });

    const liveSyncBtn = document.getElementById("liveSyncBtn");
    if (liveSyncBtn) {
      liveSyncBtn.addEventListener("click", () => {
        state.liveSyncEnabled = !state.liveSyncEnabled;
        liveSyncBtn.textContent = state.liveSyncEnabled ? "Live Sync: ON" : "Live Sync: OFF";
        if (state.liveSyncEnabled) {
          startLiveSyncPolling();
          showToast("Live Sync enabled (checking every 10s for new enquiries).");
        } else {
          stopLiveSyncPolling();
          showToast("Live Sync paused.");
        }
      });
    }

    document.getElementById("refreshBtn").addEventListener("click", () => refreshAllData(true, false));
    document.getElementById("signOutBtn").addEventListener("click", handleSignOut);
    document.getElementById("modalCloseBtn").addEventListener("click", closeModal);
    document.getElementById("modalBackdrop").addEventListener("click", (e) => {
      if (e.target.id === "modalBackdrop") closeModal();
    });
    const sidebar = document.getElementById("sidebar");
    const sidebarBackdrop = document.getElementById("sidebarBackdrop");
    document.getElementById("mobileMenuBtn").addEventListener("click", () => {
      const isOpen = sidebar.classList.toggle("open");
      if (sidebarBackdrop) sidebarBackdrop.classList.toggle("hidden", !isOpen);
    });
    if (sidebarBackdrop) {
      sidebarBackdrop.addEventListener("click", () => {
        sidebar.classList.remove("open");
        sidebarBackdrop.classList.add("hidden");
      });
    }

    // Keep session alive & fresh whenever admin returns from another app or browser tab
    let lastResumeCheckAt = 0;
    async function handleAppResume() {
      const now = Date.now();
      if (now - lastResumeCheckAt < 3000) return;
      lastResumeCheckAt = now;
      if (state.admin && state.token) {
        if (state.tokenExpiresAt > 0 && now >= state.tokenExpiresAt - 900_000) {
          await refreshAdminSession({ requireVerify: false });
        }
        const modalOpen = !document.getElementById("modalBackdrop").classList.contains("hidden");
        if (!modalOpen) {
          refreshAllData(false, true);
        }
      } else {
        await restoreSavedAdminSession();
      }
    }

    document.addEventListener("visibilitychange", () => {
      if (!document.hidden) handleAppResume();
    });
    window.addEventListener("focus", () => {
      handleAppResume();
    });

    // Automatically restore saved admin session on launch so admin stays logged in until Sign Out
    restoreSavedAdminSession();
  });
})();
