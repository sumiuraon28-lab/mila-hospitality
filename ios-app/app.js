(() => {
  "use strict";

  const SHARED_PUBLIC_ENDPOINT_KEY = "mila_admin_public_endpoint_v1";
  const SESSION_DB_NAME = "mila_ios_session_db_v1";
  const SESSION_STORE = "session";
  const SESSION_KEY = "active";

  function isSecretKey(raw) {
    const k = String(raw || "").trim();
    if (!k) return false;
    if (k.startsWith("sb_secret_")) return true;
    const parts = k.split(".");
    if (parts.length === 3 && parts[0].startsWith("eyJ")) {
      try {
        const b64 = parts[1].replace(/-/g, "+").replace(/_/g, "/");
        const payload = JSON.parse(atob(b64));
        if (payload && payload.role === "service_role") return true;
      } catch {
        // ignore
      }
    }
    return false;
  }

  function isValidHttpsUrl(raw) {
    try {
      const u = new URL(String(raw || "").trim());
      return u.protocol === "https:" && Boolean(u.hostname);
    } catch {
      return false;
    }
  }

  function isSafeImageSrc(raw) {
    const val = String(raw || "").trim();
    if (!val) return false;
    if (val.startsWith("data:image/")) return true;
    return isValidHttpsUrl(val);
  }

  function loadPublicConfig() {
    const injected = window.MILA_IOS_CONFIG || {};
    let url = String(injected.supabaseUrl || "").trim().replace(/\/+$/, "");
    let key = String(injected.supabaseAnonKey || "").trim();
    if (!url || !key || isSecretKey(key)) {
      try {
        const saved = JSON.parse(window.localStorage.getItem(SHARED_PUBLIC_ENDPOINT_KEY) || "null");
        if (saved && isValidHttpsUrl(saved.url) && saved.key && !isSecretKey(saved.key)) {
          url = String(saved.url).trim().replace(/\/+$/, "");
          key = String(saved.key).trim();
        }
      } catch {
        // ignore
      }
    }
    return {
      supabaseUrl: isValidHttpsUrl(url) ? url : "",
      supabaseAnonKey: key && !isSecretKey(key) ? key : ""
    };
  }

  // IndexedDB store for rotating Supabase refresh token only (access token stays strictly in memory)
  function openSessionDb() {
    return new Promise((resolve, reject) => {
      if (typeof indexedDB === "undefined") {
        reject(new Error("IndexedDB unavailable"));
        return;
      }
      const req = indexedDB.open(SESSION_DB_NAME, 1);
      req.onupgradeneeded = () => {
        if (!req.result.objectStoreNames.contains(SESSION_STORE)) {
          req.result.createObjectStore(SESSION_STORE, { keyPath: "id" });
        }
      };
      req.onsuccess = () => resolve(req.result);
      req.onerror = () => reject(req.error);
    });
  }

  async function readSavedSession() {
    try {
      const db = await openSessionDb();
      return await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE, "readonly");
        const req = tx.objectStore(SESSION_STORE).get(SESSION_KEY);
        req.onsuccess = () => resolve(req.result || null);
        req.onerror = () => resolve(null);
      });
    } catch {
      return null;
    }
  }

  async function writeSavedSession(patch) {
    try {
      const existing = (await readSavedSession()) || { id: SESSION_KEY };
      const next = Object.assign({}, existing, patch, { id: SESSION_KEY, updatedAt: Date.now() });
      if (!next.refreshToken) return;
      const db = await openSessionDb();
      await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE, "readwrite");
        tx.objectStore(SESSION_STORE).put(next);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }

  async function clearSavedSession() {
    try {
      const db = await openSessionDb();
      await new Promise((resolve) => {
        const tx = db.transaction(SESSION_STORE, "readwrite");
        tx.objectStore(SESSION_STORE).delete(SESSION_KEY);
        tx.oncomplete = () => resolve();
        tx.onerror = () => resolve();
      });
    } catch {
      // ignore
    }
  }

  const DEFAULT_CONTACT = {
    email: "mdkhanrashid2@gmail.com",
    phone: "+916363262421",
    responseWindow: "Partner Response Window: Within 1 Business Day (IST)",
    deskNote: "Reach our partner desk directly by phone or email for consultancy mandates, pre-opening advisory, or talent partnerships."
  };

  const HOSPITALITY_DEPARTMENTS = [
    { code: "HOSTESS", label: "Hostess" },
    { code: "GRE", label: "GRE" },
    { code: "BARTENDER", label: "Bartender" },
    { code: "HOUSEKEEPING_WELLNESS", label: "Housekeeping" },
    { code: "DRIVER", label: "Driver" },
    { code: "STORE_KEEPER", label: "Store Keeper" },
    { code: "STORE_MANAGER", label: "Store Manager" },
    { code: "MAINTENANCE", label: "Maintenance" },
    { code: "FNB_SERVICE", label: "F&B Service" },
    { code: "CULINARY", label: "Culinary & Pastry" },
    { code: "MIXOLOGY", label: "Bar & Mixology" },
    { code: "ROOMS", label: "Front Office & Rooms" },
    { code: "EXECUTIVE", label: "Executive & GM" },
    { code: "PRE_OPENING", label: "Pre-Opening & Projects" },
    { code: "SALES_MARKETING", label: "Sales & Marketing" }
  ];

  function buildDepartmentChips(initialCode, onSelect) {
    let selectedCode = initialCode || HOSPITALITY_DEPARTMENTS[0].code;
    const wrap = el("div", { style: "display:flex;flex-wrap:wrap;gap:8px;" });
    function render() {
      clearNode(wrap);
      HOSPITALITY_DEPARTMENTS.forEach((dept) => {
        const isActive = dept.code === selectedCode;
        wrap.appendChild(
          el("button", {
            type: "button",
            className: "chip" + (isActive ? " active" : ""),
            text: dept.label,
            onClick: () => {
              selectedCode = dept.code;
              render();
              onSelect(dept);
            }
          })
        );
      });
    }
    render();
    return {
      node: wrap,
      getSelected: () => HOSPITALITY_DEPARTMENTS.find((d) => d.code === selectedCode) || HOSPITALITY_DEPARTMENTS[0]
    };
  }

  const DEFAULT_SERVICES = [
    {
      id: "pre-opening",
      slug: "hospitality-pre-opening-consultancy",
      sort_order: 1,
      service_type: "HOSPITALITY_PRE_OPENING",
      division_name: "MILA Consultancy",
      title: "Hospitality Pre-Opening Consultancy",
      subtitle: "Turnkey Critical Path & Opening Governance",
      summary: "Complete pre-opening orchestration for luxury hotels, heritage properties, and destination resorts across India.",
      detailed_description: "Drawing on 30+ hospitality pre-opening mandates across India, MILA stewards every phase from architectural handover and OS&E procurement to staffing, trial runs, and commercial launch.",
      services_included: ["Master pre-opening critical path & budget control", "OS&E / FF&E specification & vendor audits", "Departmental SOP creation & dry-run commissioning"],
      owner_benefits: ["Eliminate costly launch delays and CAPEX overruns", "Achieve brand-standard guest readiness from Day 1"],
      deliverables: ["Master Pre-Opening Critical Path Tracker", "Complete Departmental SOP & Quality Manual", "Opening Readiness & Trial-Run Audit Report"],
      typical_timeline: "4 – 12 Months"
    },
    {
      id: "restaurant-cafe",
      slug: "restaurant-cafe-development",
      sort_order: 2,
      service_type: "RESTAURANT_CAFE_DEVELOPMENT",
      division_name: "MILA Consultancy",
      title: "Restaurant and Café Development",
      subtitle: "Culinary Architecture, Kitchen Flow & Launch",
      summary: "Commercial restaurant, specialty coffee, and dining setups built for repeatable margins and guest retention.",
      detailed_description: "Across 20+ café and restaurant setups, MILA bridges culinary creativity with back-of-house ergonomics, recipe costing, vendor sourcing, and launch execution.",
      services_included: ["Kitchen & café bar ergonomic workflow planning", "Recipe standardization & food-cost engineering", "Vendor onboarding & opening brigade training"],
      owner_benefits: ["Disciplined food-cost ratios from opening month", "Faster table turns without compromising luxury service"],
      deliverables: ["BOH & FOH Space Allocation Blueprint", "Standardized Recipe & Margin Matrix", "Launch Playbook & Opening Checklist"],
      typical_timeline: "2 – 6 Months"
    },
    {
      id: "bar-beverage",
      slug: "bar-beverage-consultancy",
      sort_order: 3,
      service_type: "BAR_BEVERAGE_CONSULTANCY",
      division_name: "MILA Academy & Expertise",
      title: "Bar and Beverage Consultancy",
      subtitle: "Award-Winning Three-Time Champion Mixology",
      summary: "Signature craft cocktail programs, ergonomic speed-bar design, and high-margin beverage curation.",
      detailed_description: "Led by our three-time champion mixologist, MILA designs bespoke beverage identities, zero-waste prep laboratories, glassware standards, and profitable pour-cost controls.",
      services_included: ["Signature cocktail & zero-proof menu engineering", "Speed-bar station design & ice program setup", "Pour-cost control, inventory & bartender mastery"],
      owner_benefits: ["Higher beverage check averages and repeat destination traffic", "Consistent execution across shifts with reduced wastage"],
      deliverables: ["Signature Beverage Bible & Spec Cards", "Ergonomic Bar Station Layout Plan", "Beverage Costing & Inventory SOPs"],
      typical_timeline: "6 – 12 Weeks"
    }
  ];

  const IMPACT_METRICS = [
    { figure: "15+", label: "Years Experience", detail: "Indian & International Hospitality" },
    { figure: "30+", label: "Pre-Opening Projects", detail: "Hotels, Resorts & Heritage Properties" },
    { figure: "20+", label: "Café & Restaurant Setups", detail: "Fine Dining, Roasteries & Lounges" },
    { figure: "3×", label: "Champion Mixologist", detail: "Award-Winning Beverage Specialist" }
  ];

  const DIVISIONS = [
    { code: "01", name: "MILA Consultancy", tagline: "Pre-Openings, Café Setups & Turnkey Advisory", desc: "End-to-end advisory for hotel pre-openings, restaurant launches, SOP architecture, and operational profitability.", tab: "services" },
    { code: "02", name: "MILA Talent", tagline: "Executive Search & Hospitality Recruitment", desc: "Connecting Indian hotels, luxury resorts, and dining groups with vetted General Managers, Chefs, and Mixologists.", tab: "careers" },
    { code: "03", name: "MILA Academy & Expertise", tagline: "Specialist Mixology, Training & Service Mastery", desc: "Led by a three-time champion mixologist delivering signature beverage curation and luxury service academies.", tab: "services" }
  ];

  const SERVICE_REGIONS = [
    ["Mumbai & Western India", "Luxury urban hotels, fine dining, craft cocktail lounges, and café launches."],
    ["New Delhi & NCR", "Upper-upscale hospitality advisory, diplomatic banqueting, and executive search."],
    ["Bengaluru & South India", "Specialty coffee concepts, roasteries, business hotels, and multi-unit F&B."],
    ["Rajasthan, Goa & Leisure Corridors", "Heritage resort pre-openings, destination retreats, and seasonal brigades."]
  ];

  const cfg = loadPublicConfig();
  const state = {
    supabaseUrl: cfg.supabaseUrl,
    supabaseAnonKey: cfg.supabaseAnonKey,
    accessToken: null,
    tokenExpiresAt: 0,
    user: null,
    guestMode: false,
    activeTab: "home",
    selectedServiceSlug: null,
    serviceFilter: "ALL",
    portfolioFilter: "ALL",
    careerFilter: "ALL",
    shownPopupIds: new Set(),
    pendingOtpEmail: "",
    pendingFullName: "",
    loadedServices: false,
    loadedPortfolio: false,
    loadedVacancies: false,
    loadedTestimonials: false,
    data: {
      services: DEFAULT_SERVICES,
      portfolio: [],
      vacancies: [],
      testimonials: [],
      banners: [],
      contact: Object.assign({}, DEFAULT_CONTACT)
    }
  };

  function el(tag, props, children) {
    const node = document.createElement(tag);
    if (props) {
      Object.entries(props).forEach(([k, v]) => {
        if (v === null || v === undefined) return;
        if (k === "className") node.className = v;
        else if (k === "text") node.textContent = String(v);
        else if (k === "onClick") node.addEventListener("click", v);
        else if (k === "onSubmit") node.addEventListener("submit", v);
        else if (k === "type" || k === "value" || k === "placeholder" || k === "disabled" || k === "required" || k === "href") {
          node[k] = v;
        } else {
          node.setAttribute(k, String(v));
        }
      });
    }
    if (children) {
      (Array.isArray(children) ? children : [children]).forEach((c) => {
        if (c === null || c === undefined || c === false) return;
        node.appendChild(typeof c === "string" || typeof c === "number" ? document.createTextNode(String(c)) : c);
      });
    }
    return node;
  }

  function clearNode(node) {
    while (node.firstChild) node.removeChild(node.firstChild);
  }

  let toastTimer = null;
  function showToast(msg) {
    const t = document.getElementById("toast");
    t.textContent = msg;
    t.classList.remove("hidden");
    clearTimeout(toastTimer);
    toastTimer = setTimeout(() => t.classList.add("hidden"), 3500);
  }

  function isConfigured() {
    return Boolean(state.supabaseUrl && state.supabaseAnonKey);
  }

  let refreshPromise = null;
  async function refreshSession() {
    if (!isConfigured()) return false;
    if (refreshPromise) return refreshPromise;
    refreshPromise = (async () => {
      const saved = await readSavedSession();
      if (!saved || !saved.refreshToken) return false;
      try {
        const res = await fetch(state.supabaseUrl + "/auth/v1/token?grant_type=refresh_token", {
          method: "POST",
          headers: { apikey: state.supabaseAnonKey, "Content-Type": "application/json" },
          body: JSON.stringify({ refresh_token: saved.refreshToken })
        });
        const json = await res.json().catch(() => null);
        if (!res.ok || !json || !json.access_token) {
          if (res.status === 400 || res.status === 401) await clearSavedSession();
          return false;
        }
        state.accessToken = json.access_token;
        state.tokenExpiresAt = Date.now() + Math.max(60, Number(json.expires_in || 3600)) * 1000;
        const u = json.user || {};
        state.user = {
          id: u.id || saved.userId || "",
          email: u.email || saved.email || "",
          fullName: (u.user_metadata && u.user_metadata.full_name) || saved.fullName || ""
        };
        if (json.refresh_token) {
          await writeSavedSession({
            refreshToken: json.refresh_token,
            userId: state.user.id,
            email: state.user.email,
            fullName: state.user.fullName
          });
        }
        return true;
      } catch {
        return false;
      }
    })();
    try {
      return await refreshPromise;
    } finally {
      refreshPromise = null;
    }
  }

  async function supaFetch(path, options, retried) {
    if (!isConfigured()) throw new Error("Backend not configured yet.");
    if (!retried && state.accessToken && state.tokenExpiresAt > 0 && Date.now() >= state.tokenExpiresAt - 120_000) {
      await refreshSession();
    }
    const headers = Object.assign(
      { apikey: state.supabaseAnonKey, Accept: "application/json" },
      options && options.headers ? options.headers : {}
    );
    if (state.accessToken && !headers.Authorization) {
      headers.Authorization = "Bearer " + state.accessToken;
    }
    const res = await fetch(state.supabaseUrl + path, Object.assign({}, options || {}, { headers }));
    if (res.status === 401 && !retried && state.accessToken) {
      if (await refreshSession()) {
        const nextOpts = Object.assign({}, options || {});
        if (nextOpts.headers) {
          nextOpts.headers = Object.assign({}, nextOpts.headers);
          delete nextOpts.headers.Authorization;
        }
        return supaFetch(path, nextOpts, true);
      }
    }
    const text = await res.text();
    let json = null;
    try { json = text ? JSON.parse(text) : null; } catch { json = null; }
    if (!res.ok) {
      throw new Error((json && (json.msg || json.message || json.error_description)) || ("Request failed (" + res.status + ")"));
    }
    return json;
  }

  async function syncLoggedInUserProfile() {
    if (!state.user || !state.user.id || !state.accessToken) return;
    const displayName = state.user.fullName || state.user.email.split("@")[0] || "MILA Member";
    const body = {
      id: state.user.id,
      email: state.user.email,
      full_name: displayName,
      account_type: "CLIENT",
      updated_at: new Date().toISOString()
    };
    try {
      await supaFetch("/rest/v1/user_profiles?on_conflict=id", {
        method: "POST",
        headers: { "Content-Type": "application/json", Prefer: "resolution=merge-duplicates,return=minimal" },
        body: JSON.stringify(body)
      });
    } catch {
      // Best-effort profile sync
    }
  }

  function parseBannerConfig(raw) {
    const out = { theme: "gold", showPopup: false, action: "contact_us" };
    String(raw || "").split("|").forEach((seg) => {
      const idx = seg.indexOf(":");
      if (idx > 0) {
        const k = seg.slice(0, idx).trim().toLowerCase();
        const v = seg.slice(idx + 1).trim();
        if (k === "theme") out.theme = v || "gold";
        if (k === "popup") out.showPopup = v.toLowerCase() === "on";
        if (k === "action") out.action = v || "contact_us";
      }
    });
    return out;
  }

  async function refreshPublicCatalog() {
    if (!isConfigured()) return;
    const [svcs, port, vacs, revs, pages] = await Promise.all([
      supaFetch("/rest/v1/services?published=eq.true&order=sort_order.asc").catch(() => null),
      supaFetch("/rest/v1/portfolio_projects?published=eq.true&order=sort_order.asc").catch(() => null),
      supaFetch("/rest/v1/job_vacancies?published=eq.true&order=created_at.desc").catch(() => null),
      supaFetch("/rest/v1/testimonials?published=eq.true&order=sort_order.asc,created_at.desc").catch(() => null),
      supaFetch("/rest/v1/website_content?published=eq.true&order=updated_at.desc").catch(() => null)
    ]);

    if (Array.isArray(svcs)) {
      state.data.services = svcs;
      state.loadedServices = true;
    }
    if (Array.isArray(port)) {
      state.data.portfolio = port;
      state.loadedPortfolio = true;
    }
    if (Array.isArray(vacs)) {
      state.data.vacancies = vacs;
      state.loadedVacancies = true;
    }
    if (Array.isArray(revs)) {
      state.data.testimonials = revs;
      state.loadedTestimonials = true;
    }
    if (Array.isArray(pages)) {
      state.data.banners = pages
        .filter((p) => String(p.page_key || "").toLowerCase().startsWith("banner_"))
        .map((p) => {
          const parsed = parseBannerConfig(p.hero_image_key);
          return {
            id: p.id || p.page_key,
            tagLabel: p.page_title || "ANNOUNCEMENT",
            eyebrow: p.eyebrow || "",
            headline: p.headline || p.page_title || "",
            subheadline: p.subheadline || "",
            detailsCopy: p.body_copy || p.subheadline || "",
            imageUrl: isSafeImageSrc(p.hero_image_url) ? p.hero_image_url : "",
            showAutoPopup: parsed.showPopup,
            actionRoute: parsed.action,
            ctaLabel: p.cta_label || "Explore Details"
          };
        });
      const contactRow = pages.find((p) => String(p.page_key || "").toLowerCase() === "contact_details");
      if (contactRow) {
        state.data.contact = {
          email: String(contactRow.eyebrow || "").trim(),
          phone: String(contactRow.headline || "").trim(),
          responseWindow: String(contactRow.subheadline || "").trim() || DEFAULT_CONTACT.responseWindow,
          deskNote: String(contactRow.body_copy || "").trim()
        };
      }
    }

    renderDrawer();
    renderScreen();
    maybeShowAutoPopupBanner();
  }

  function maybeShowAutoPopupBanner() {
    if (state.activeTab !== "home") return;
    if (!document.getElementById("sheetBackdrop").classList.contains("hidden")) return;
    const b = (state.data.banners || []).find((item) => item.showAutoPopup && !state.shownPopupIds.has(item.id));
    if (b) {
      state.shownPopupIds.add(b.id);
      openBannerSheet(b);
    }
  }

  // Bottom Sheet Helpers
  function openSheet(title, bodyNodes, footerNodes) {
    document.getElementById("sheetTitle").textContent = title;
    const body = document.getElementById("sheetBody");
    const footer = document.getElementById("sheetFooter");
    clearNode(body);
    clearNode(footer);
    (bodyNodes || []).forEach((n) => n && body.appendChild(n));
    (footerNodes || []).forEach((n) => n && footer.appendChild(n));
    document.getElementById("sheetBackdrop").classList.remove("hidden");
  }

  function closeSheet() {
    document.getElementById("sheetBackdrop").classList.add("hidden");
  }

  function openBannerSheet(banner) {
    const nodes = [];
    if (banner.imageUrl) {
      nodes.push(el("img", { src: banner.imageUrl, alt: banner.headline, className: "service-cover" }));
    }
    nodes.push(el("span", { className: "pill pill-gold", text: banner.tagLabel }));
    if (banner.eyebrow) nodes.push(el("div", { className: "eyebrow", text: banner.eyebrow }));
    nodes.push(el("h3", { className: "serif", style: "margin:0;font-size:21px;", text: banner.headline }));
    if (banner.subheadline) nodes.push(el("div", { style: "color:#d4af7a;font-size:14px;font-weight:600;", text: banner.subheadline }));
    if (banner.detailsCopy && banner.detailsCopy !== banner.subheadline) {
      nodes.push(el("p", { className: "muted-copy", text: banner.detailsCopy }));
    }

    const footer = [el("button", { className: "btn btn-outline", type: "button", text: "Close", onClick: closeSheet })];
    if (banner.actionRoute !== "popup_only") {
      footer.push(
        el("button", {
          className: "btn btn-gold",
          type: "button",
          text: banner.ctaLabel || "Explore",
          onClick: () => {
            closeSheet();
            if (banner.actionRoute === "our_services") switchTab("services");
            else if (banner.actionRoute === "careers") switchTab("careers");
            else if (banner.actionRoute === "our_portfolio") switchTab("portfolio");
            else if (banner.actionRoute === "enquire") openEnquirySheet("HOSPITALITY_PRE_OPENING");
            else switchTab("contact");
          }
        })
      );
    }
    openSheet(banner.tagLabel || "Announcement", nodes, footer);
  }

  function openEnquirySheet(serviceType, serviceTitle) {
    if (!state.user || !state.accessToken) {
      showToast("Please sign in with your email code to submit an enquiry.");
      showAuthGate();
      return;
    }
    const fName = el("input", { value: state.user.fullName || "", placeholder: "Your full name *", required: true });
    const fPhone = el("input", { type: "tel", placeholder: "Mobile / WhatsApp number *" });
    const fProperty = el("input", { placeholder: "Hotel / Restaurant / Project Name *" });
    const fCity = el("input", { placeholder: "City / State (e.g. Mumbai, Goa) *" });
    const fScale = el("input", { placeholder: "Keys / Covers / Project Scale (e.g. 60 Keys, 80 Covers)" });
    const fNotes = el("textarea", { placeholder: "Share your project timeline, keys/covers, and advisory goals..." });

    const submitBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: "Submit Enquiry",
      onClick: async () => {
        const contactName = fName.value.trim();
        const phone = fPhone.value.trim();
        const prop = fProperty.value.trim();
        const city = fCity.value.trim() || "India";
        if (contactName.length < 2 || phone.length < 7 || prop.length < 2) {
          showToast("Please enter your full name, valid phone number, and project name.");
          return;
        }
        submitBtn.disabled = true;
        submitBtn.textContent = "Submitting...";
        try {
          await syncLoggedInUserProfile();
          const refCode = "MILA-" + String(Date.now()).slice(-6);
          await supaFetch("/rest/v1/service_enquiries", {
            method: "POST",
            headers: { "Content-Type": "application/json", Prefer: "return=minimal" },
            body: JSON.stringify({
              user_id: state.user.id,
              reference_code: refCode,
              service_type: serviceType || "HOSPITALITY_PRE_OPENING",
              property_name: prop,
              city: city.length >= 2 ? city : "India",
              scale_summary: fScale.value.trim() || (serviceTitle || "Hospitality Consultancy Mandate"),
              contact_name: contactName,
              contact_email: state.user.email,
              contact_phone: phone,
              brief_notes: fNotes.value.trim() || (serviceTitle ? "Enquiry regarding " + serviceTitle : "Consultancy enquiry from MILA iPhone app"),
              status: "SUBMITTED"
            })
          });
          closeSheet();
          showToast("Enquiry submitted (" + refCode + "). Our partner desk will respond within 1 business day.");
        } catch (err) {
          showToast(err.message || "Could not submit enquiry.");
          submitBtn.disabled = false;
          submitBtn.textContent = "Submit Enquiry";
        }
      }
    });

    openSheet(
      serviceTitle ? "Enquire: " + serviceTitle : "Service Enquiry",
      [
        el("div", { className: "field" }, [el("label", { text: "Full Name *" }), fName]),
        el("div", { className: "field" }, [el("label", { text: "Phone / WhatsApp *" }), fPhone]),
        el("div", { className: "field" }, [el("label", { text: "Property / Business Name *" }), fProperty]),
        el("div", { className: "field" }, [el("label", { text: "City / Location *" }), fCity]),
        el("div", { className: "field" }, [el("label", { text: "Project Scale (Optional)" }), fScale]),
        el("div", { className: "field" }, [el("label", { text: "Project Requirements" }), fNotes])
      ],
      [el("button", { className: "btn btn-outline", type: "button", text: "Cancel", onClick: closeSheet }), submitBtn]
    );
  }

  function openWriteReviewSheet() {
    if (!state.user || !state.accessToken) {
      showToast("Please sign in with Email OTP to submit a review.");
      showAuthGate();
      return;
    }
    let selectedRating = 5;
    const starRow = el("div", { className: "btn-row" });
    function renderStars() {
      clearNode(starRow);
      for (let i = 1; i <= 5; i += 1) {
        const idx = i;
        starRow.appendChild(
          el("button", {
            type: "button",
            className: "chip" + (idx <= selectedRating ? " active" : ""),
            text: idx + " ★",
            onClick: () => {
              selectedRating = idx;
              renderStars();
            }
          })
        );
      }
    }
    renderStars();

    const fName = el("input", { value: state.user.fullName || "", placeholder: "Your Name / Designation *" });
    const fCtx = el("input", { placeholder: "Project / Service (e.g. Resort Pre-Opening • Goa)" });
    const fQuote = el("textarea", { placeholder: "Share your experience working with MILA Hospitality..." });

    const submitBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: "Submit for Approval",
      onClick: async () => {
        const name = fName.value.trim();
        const quote = fQuote.value.trim();
        if (name.length < 2 || quote.length < 10) {
          showToast("Please enter your name and at least 10 characters for your review.");
          return;
        }
        submitBtn.disabled = true;
        submitBtn.textContent = "Submitting...";
        try {
          await supaFetch("/rest/v1/testimonials", {
            method: "POST",
            headers: { "Content-Type": "application/json", Prefer: "return=minimal" },
            body: JSON.stringify({
              reviewer_user_id: state.user.id,
              reviewer_email: state.user.email,
              attribution_role: name.slice(0, 120),
              engagement_context: (fCtx.value.trim() || "MILA Hospitality Client").slice(0, 160),
              rating: selectedRating,
              quote: quote.slice(0, 1200),
              source: "APP_USER",
              published: false,
              admin_reply: "",
              admin_replied_by: "",
              admin_replied_at: null
            })
          });
          closeSheet();
          showToast("Thank you! Your review has been submitted for MILA team approval.");
        } catch (err) {
          showToast(err.message || "Could not submit review.");
          submitBtn.disabled = false;
          submitBtn.textContent = "Submit for Approval";
        }
      }
    });

    openSheet(
      "Write a Client Review",
      [
        el("p", { className: "muted-copy", text: "Reviews are reviewed by the MILA team before appearing live in the app." }),
        el("div", { className: "field" }, [el("label", { text: "Your Rating" }), starRow]),
        el("div", { className: "field" }, [el("label", { text: "Your Name or Role *" }), fName]),
        el("div", { className: "field" }, [el("label", { text: "Property / Engagement Context" }), fCtx]),
        el("div", { className: "field" }, [el("label", { text: "Your Review *" }), fQuote])
      ],
      [el("button", { className: "btn btn-outline", type: "button", text: "Cancel", onClick: closeSheet }), submitBtn]
    );
  }

  function openApplyJobSheet(job) {
    if (!state.user || !state.accessToken) {
      showToast("Please sign in with Email OTP to apply for this role.");
      showAuthGate();
      return;
    }
    const matchedDept = HOSPITALITY_DEPARTMENTS.find(
      (d) => d.code.toLowerCase() === String(job.category || "").toLowerCase() ||
        d.label.toLowerCase() === String(job.category || "").toLowerCase()
    );
    let chosenDept = matchedDept || HOSPITALITY_DEPARTMENTS[0];
    const deptPicker = buildDepartmentChips(chosenDept.code, (dept) => {
      chosenDept = dept;
    });

    const fName = el("input", { value: state.user.fullName || "", placeholder: "Full Name *" });
    const fPhone = el("input", { type: "tel", placeholder: "Mobile Number *" });
    const fCity = el("input", { placeholder: "Current City *" });
    const fCurrentProp = el("input", { placeholder: "Current / Last Hotel or Company *" });
    const fExp = el("input", { type: "number", value: "5", placeholder: "Years of hospitality experience" });
    const fNote = el("textarea", { placeholder: "Brief cover note highlighting your hospitality background..." });

    const submitBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: "Submit Application",
      onClick: async () => {
        if (fName.value.trim().length < 2 || fPhone.value.trim().length < 7) {
          showToast("Please enter your full name and valid phone number.");
          return;
        }
        submitBtn.disabled = true;
        submitBtn.textContent = "Submitting...";
        try {
          await syncLoggedInUserProfile();
          const extraNote = fNote.value.trim();
          const combinedNote = "Department: " + chosenDept.label + (extraNote ? " | " + extraNote : "");
          await supaFetch("/rest/v1/job_applications", {
            method: "POST",
            headers: { "Content-Type": "application/json", Prefer: "return=minimal" },
            body: JSON.stringify({
              user_id: state.user.id,
              role_id: String(job.slug || job.id || "hospitality-role"),
              role_title: job.title || "Hospitality Role",
              location: fCity.value.trim() || job.location || "India",
              candidate_name: fName.value.trim(),
              candidate_email: state.user.email,
              candidate_phone: fPhone.value.trim(),
              years_experience: Math.max(0, Math.min(60, parseInt(fExp.value, 10) || 0)),
              current_property: fCurrentProp.value.trim() || "Hospitality Professional",
              cover_note: combinedNote,
              status: "APPLIED"
            })
          });
          closeSheet();
          showToast("Application submitted to MILA Talent!");
        } catch (err) {
          showToast(err.message || "Could not submit application.");
          submitBtn.disabled = false;
          submitBtn.textContent = "Submit Application";
        }
      }
    });

    openSheet(
      "Apply: " + (job.title || "Hospitality Role"),
      [
        el("div", { className: "field" }, [el("label", { text: "Select Your Department / Role (Tap to Choose) *" }), deptPicker.node]),
        el("div", { className: "field" }, [el("label", { text: "Full Name *" }), fName]),
        el("div", { className: "field" }, [el("label", { text: "Phone Number *" }), fPhone]),
        el("div", { className: "field" }, [el("label", { text: "Current City *" }), fCity]),
        el("div", { className: "field" }, [el("label", { text: "Current / Last Hotel or Company *" }), fCurrentProp]),
        el("div", { className: "field" }, [el("label", { text: "Years of Experience" }), fExp]),
        el("div", { className: "field" }, [el("label", { text: "Cover Note (Optional)" }), fNote])
      ],
      [el("button", { className: "btn btn-outline", type: "button", text: "Cancel", onClick: closeSheet }), submitBtn]
    );
  }

  function openCandidateProfileSheet() {
    if (!state.user || !state.accessToken) {
      showToast("Please sign in with Email OTP to update your CV profile.");
      showAuthGate();
      return;
    }
    let chosenDept = HOSPITALITY_DEPARTMENTS[0];
    const fDesignation = el("input", { value: chosenDept.label, placeholder: "Auto-filled when you tap a department above" });
    const deptPicker = buildDepartmentChips(chosenDept.code, (dept) => {
      const prevWasAuto = !fDesignation.value.trim() ||
        HOSPITALITY_DEPARTMENTS.some((d) => d.label.toLowerCase() === fDesignation.value.trim().toLowerCase());
      chosenDept = dept;
      if (prevWasAuto) fDesignation.value = dept.label;
    });

    const fName = el("input", { value: state.user.fullName || "", placeholder: "Your full name *" });
    const fPhone = el("input", { type: "tel", placeholder: "Mobile / WhatsApp number *" });
    const fCity = el("input", { placeholder: "Current City (e.g. Mumbai, Goa) *" });
    const fEmployer = el("input", { placeholder: "Current / Recent Hotel or Restaurant" });
    const fExp = el("input", { type: "number", value: "3", placeholder: "Years of experience" });
    const fSkills = el("textarea", { placeholder: "Key skills or highlights (optional)..." });

    const saveBtn = el("button", {
      className: "btn btn-gold",
      type: "button",
      text: "Save CV Profile",
      onClick: async () => {
        if (fName.value.trim().length < 2 || fPhone.value.trim().length < 7) {
          showToast("Please enter your full name and valid mobile number.");
          return;
        }
        saveBtn.disabled = true;
        saveBtn.textContent = "Saving...";
        try {
          await syncLoggedInUserProfile();
          await supaFetch("/rest/v1/candidate_profiles?on_conflict=user_id", {
            method: "POST",
            headers: { "Content-Type": "application/json", Prefer: "resolution=merge-duplicates,return=minimal" },
            body: JSON.stringify({
              user_id: state.user.id,
              full_name: fName.value.trim(),
              email: state.user.email,
              mobile_number: fPhone.value.trim(),
              location: fCity.value.trim() || "India",
              current_designation: fDesignation.value.trim() || chosenDept.label,
              department: chosenDept.code,
              total_experience_years: Math.max(0, Math.min(60, parseInt(fExp.value, 10) || 0)),
              current_employer: fEmployer.value.trim() || "Hospitality Professional",
              skills: fSkills.value.trim(),
              job_alerts_enabled: true,
              updated_at: new Date().toISOString()
            })
          });
          closeSheet();
          showToast("CV profile saved for " + chosenDept.label + "!");
        } catch (err) {
          showToast(err.message || "Could not save profile.");
          saveBtn.disabled = false;
          saveBtn.textContent = "Save CV Profile";
        }
      }
    });

    openSheet(
      "Update My CV Profile",
      [
        el("div", { className: "field" }, [el("label", { text: "Select Department / Role (Tap to Choose) *" }), deptPicker.node]),
        el("div", { className: "field" }, [el("label", { text: "Full Name *" }), fName]),
        el("div", { className: "field" }, [el("label", { text: "Mobile Number *" }), fPhone]),
        el("div", { className: "field" }, [el("label", { text: "Role / Position" }), fDesignation]),
        el("div", { className: "field" }, [el("label", { text: "Current City" }), fCity]),
        el("div", { className: "field" }, [el("label", { text: "Current / Recent Hotel or Company" }), fEmployer]),
        el("div", { className: "field" }, [el("label", { text: "Years of Experience" }), fExp]),
        el("div", { className: "field" }, [el("label", { text: "Key Skills (Optional)" }), fSkills])
      ],
      [el("button", { className: "btn btn-outline", type: "button", text: "Cancel", onClick: closeSheet }), saveBtn]
    );
  }

  function openAccountSheet() {
    if (!state.user) {
      showAuthGate();
      return;
    }
    openSheet(
      "Your MILA Account",
      [
        el("div", { className: "eyebrow", text: "SIGNED IN VIA EMAIL OTP" }),
        el("div", { className: "serif", style: "font-size:20px;", text: state.user.fullName || "MILA Member" }),
        el("p", { className: "muted-copy", text: state.user.email }),
        el("p", { className: "muted-copy", text: "Your session stays active automatically until you tap Sign Out." }),
        el("div", { className: "btn-row", style: "margin-top:8px;" }, [
          el("a", {
            className: "btn btn-outline btn-sm",
            href: "../legal/privacy-policy.html",
            target: "_blank",
            rel: "noopener noreferrer",
            text: "Privacy Policy"
          }),
          el("a", {
            className: "btn btn-outline btn-sm",
            href: "../legal/delete-account.html",
            target: "_blank",
            rel: "noopener noreferrer",
            text: "Delete Account"
          })
        ])
      ],
      [
        el("button", { className: "btn btn-outline", type: "button", text: "Close", onClick: closeSheet }),
        el("button", {
          className: "btn btn-danger",
          type: "button",
          text: "Sign Out",
          onClick: async () => {
            closeSheet();
            await signOutUser();
          }
        })
      ]
    );
  }

  // Screen Renderers
  function renderHome(root) {
    // Luxury Hero Photo Banner
    root.appendChild(
      el("div", { className: "hero-card-photo" }, [
        el("img", { className: "hero-bg-img", src: "./hero-hotel.jpg", alt: "MILA Luxury Hospitality" }),
        el("div", { className: "hero-scrim" }, [
          el("span", { className: "pill pill-gold", text: "Indian Hospitality Consultancy & Recruitment" }),
          el("h1", { className: "serif", text: "Creating Excellence in Hospitality" }),
          el("p", { className: "muted-copy", text: "Turnkey hotel pre-openings, café and restaurant setups, champion mixology curation, and executive hospitality recruitment." })
        ])
      ])
    );

    // Live Top Promo Banners
    if (state.data.banners && state.data.banners.length) {
      state.data.banners.forEach((b) => {
        root.appendChild(
          el("div", { className: "banner-card", onClick: () => openBannerSheet(b) }, [
            b.imageUrl ? el("img", { src: b.imageUrl, alt: b.headline }) : null,
            el("div", { className: "banner-inner" }, [
              el("span", { className: "pill pill-gold", text: b.tagLabel }),
              b.eyebrow ? el("div", { className: "eyebrow", text: b.eyebrow }) : null,
              el("div", { className: "serif", style: "font-size:19px;", text: b.headline }),
              b.subheadline ? el("p", { className: "muted-copy", text: b.subheadline }) : null,
              el("div", { style: "margin-top:6px;color:#d4af7a;font-size:13px;font-weight:700;", text: (b.ctaLabel || "Explore Details") + " →" })
            ])
          ])
        );
      });
    }

    // Impact Metrics
    const metricsGrid = el("div", { className: "metrics-grid" });
    IMPACT_METRICS.forEach((m) => {
      metricsGrid.appendChild(
        el("div", { className: "metric-tile" }, [
          el("div", { className: "metric-figure", text: m.figure }),
          el("div", { className: "metric-label", text: m.label }),
          el("div", { className: "metric-detail", text: m.detail })
        ])
      );
    });
    root.appendChild(
      el("div", { className: "stack-12" }, [
        el("div", { className: "section-heading" }, [
          el("div", { className: "eyebrow", text: "Experience & Track Record" }),
          el("h2", { className: "serif", text: "15+ Years in Indian Hospitality" })
        ]),
        metricsGrid
      ])
    );

    // Three Core Divisions
    const divWrap = el("div", { className: "stack-12" }, [
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "Three Core Business Divisions" }),
        el("h2", { className: "serif", text: "Consultancy, Talent & Academy" })
      ])
    ]);
    DIVISIONS.forEach((d) => {
      divWrap.appendChild(
        el("div", { className: "card card-elevated", style: "cursor:pointer;", onClick: () => switchTab(d.tab) }, [
          el("div", { className: "eyebrow", text: "DIVISION " + d.code }),
          el("div", { className: "serif", style: "font-size:19px;", text: d.name }),
          el("div", { style: "color:#d4af7a;font-size:13px;font-weight:600;", text: d.tagline }),
          el("p", { className: "muted-copy", text: d.desc })
        ])
      );
    });
    root.appendChild(divWrap);

    // Live Services Catalogue Snapshot
    const svcs = state.data.services || [];
    if (svcs.length) {
      const svcSection = el("div", { className: "stack-12" }, [
        el("div", { className: "section-heading" }, [
          el("div", { className: "eyebrow", text: "Service Categories (" + svcs.length + ")" }),
          el("h2", { className: "serif", text: "Complete Consultancy Catalogue" })
        ])
      ]);
      svcs.forEach((s, idx) => {
        const badge = String(s.sort_order || idx + 1).padStart(2, "0");
        svcSection.appendChild(
          el("div", { className: "card", style: "cursor:pointer;", onClick: () => openServiceDetail(s.slug || s.id) }, [
            el("div", { className: "serif", style: "font-size:17px;", text: badge + ". " + (s.title || "") }),
            s.subtitle ? el("div", { style: "color:#d4af7a;font-size:13px;", text: s.subtitle }) : null,
            el("div", { style: "color:#989082;font-size:12px;", text: (s.division_name || "MILA Consultancy") + " • " + (s.typical_timeline || "Custom") })
          ])
        );
      });
      svcSection.appendChild(
        el("button", { className: "btn btn-outline btn-full", type: "button", text: "Explore All " + svcs.length + " Services", onClick: () => switchTab("services") })
      );
      root.appendChild(svcSection);
    }

    // Live Client Reviews & Testimonials
    const revWrap = el("div", { className: "stack-12" }, [
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "Partner Perspectives" }),
        el("h2", { className: "serif", text: "Client Testimonials" })
      ])
    ]);
    const reviews = state.data.testimonials || [];
    if (!reviews.length) {
      revWrap.appendChild(el("div", { className: "card" }, [el("p", { className: "muted-copy", text: "Be the first client to share your experience with MILA Hospitality." })]));
    } else {
      reviews.forEach((r) => {
        const stars = "★".repeat(Math.max(1, Math.min(5, Number(r.rating || 5))));
        revWrap.appendChild(
          el("div", { className: "card" }, [
            el("div", { className: "stars", text: stars }),
            el("p", { style: "margin:0;font-style:italic;", text: '"' + (r.quote || "") + '"' }),
            el("div", { style: "font-weight:700;color:#d4af7a;font-size:13px;", text: r.attribution_role || "Client Partner" }),
            r.engagement_context ? el("div", { style: "font-size:12px;color:#989082;", text: r.engagement_context }) : null,
            r.admin_reply
              ? el("div", { className: "reply-box" }, [
                  el("div", { className: "eyebrow", style: "margin-bottom:4px;", text: "Response from MILA Hospitality" }),
                  el("div", { text: r.admin_reply })
                ])
              : null
          ])
        );
      });
    }
    revWrap.appendChild(
      el("button", { className: "btn btn-outline btn-full", type: "button", text: "Write a Review", onClick: openWriteReviewSheet })
    );
    root.appendChild(revWrap);

    // Contact Card
    const c = state.data.contact || DEFAULT_CONTACT;
    root.appendChild(
      el("div", { className: "card card-deep" }, [
        el("div", { className: "eyebrow", text: "CONTACT & REGIONAL COVERAGE" }),
        el("div", { className: "serif", style: "font-size:21px;", text: "Partner With MILA Across India" }),
        c.phone ? el("div", { style: "font-size:14px;font-weight:600;", text: "☏  " + c.phone }) : null,
        c.email ? el("div", { style: "font-size:14px;font-weight:600;", text: "✉  " + c.email }) : null,
        el("p", { className: "muted-copy", text: c.responseWindow || DEFAULT_CONTACT.responseWindow }),
        el("div", { className: "btn-row" }, [
          el("button", { className: "btn btn-gold", type: "button", text: "Contact Us", onClick: () => switchTab("contact") }),
          el("button", { className: "btn btn-outline", type: "button", text: "Our Services", onClick: () => switchTab("services") })
        ])
      ])
    );
  }

  function renderServices(root) {
    const allSvcs = state.data.services || [];
    if (state.selectedServiceSlug) {
      const svc = allSvcs.find((s) => (s.slug || s.id) === state.selectedServiceSlug) || allSvcs[0];
      if (svc) {
        renderServiceDetail(root, svc);
        return;
      }
      state.selectedServiceSlug = null;
    }

    root.appendChild(
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "MILA Consultancy • Live Catalogue" }),
        el("h2", { className: "serif", text: "Specialized Hospitality Practices" }),
        el("p", { className: "muted-copy", text: "Select any practice to view scope, owner benefits, deliverables, or submit an enquiry." })
      ])
    );

    const divs = ["ALL"].concat(Array.from(new Set(allSvcs.map((s) => s.division_name).filter(Boolean))));
    const chips = el("div", { className: "chips-row" });
    divs.forEach((d) => {
      chips.appendChild(
        el("button", {
          type: "button",
          className: "chip" + (state.serviceFilter === d ? " active" : ""),
          text: d === "ALL" ? "All (" + allSvcs.length + ")" : d,
          onClick: () => {
            state.serviceFilter = d;
            renderScreen();
          }
        })
      );
    });
    root.appendChild(chips);

    const visible = state.serviceFilter === "ALL" ? allSvcs : allSvcs.filter((s) => s.division_name === state.serviceFilter);
    if (!visible.length) {
      root.appendChild(el("div", { className: "card" }, [el("p", { className: "muted-copy", text: "No published services in this category right now." })]));
      return;
    }

    visible.forEach((s, idx) => {
      const badge = String(s.sort_order || idx + 1).padStart(2, "0");
      const deliverables = Array.isArray(s.deliverables) ? s.deliverables.slice(0, 3) : [];
      root.appendChild(
        el("div", { className: "card" }, [
          isSafeImageSrc(s.image_url) ? el("img", { src: s.image_url, alt: s.title, className: "service-cover" }) : null,
          el("div", { style: "display:flex;justify-content:space-between;align-items:center;gap:8px;flex-wrap:wrap;" }, [
            el("span", { className: "pill pill-gold", text: badge + " • " + (s.division_name || "MILA Consultancy") }),
            el("span", { style: "font-size:12px;color:#989082;", text: s.typical_timeline || "" })
          ]),
          el("div", { className: "serif", style: "font-size:20px;", text: s.title || "" }),
          s.subtitle ? el("div", { style: "color:#d4af7a;font-size:13px;font-weight:600;", text: s.subtitle }) : null,
          s.summary ? el("p", { className: "muted-copy", text: s.summary }) : null,
          deliverables.length
            ? el("ul", { className: "bullet-list" }, deliverables.map((d) => el("li", null, [el("span", { className: "bullet-dot", text: "✓" }), el("span", { text: d })])))
            : null,
          el("div", { className: "btn-row", style: "margin-top:6px;" }, [
            el("button", { className: "btn btn-outline", type: "button", text: "View Details", onClick: () => openServiceDetail(s.slug || s.id) }),
            el("button", { className: "btn btn-gold", type: "button", text: "Enquire Now", onClick: () => openEnquirySheet(s.service_type, s.title) })
          ])
        ])
      );
    });
  }

  function renderServiceDetail(root, svc) {
    root.appendChild(
      el("button", {
        className: "btn btn-outline btn-sm",
        type: "button",
        text: "← Back to All Services",
        onClick: () => {
          state.selectedServiceSlug = null;
          renderScreen();
        }
      })
    );

    root.appendChild(
      el("div", { className: "hero-card" }, [
        isSafeImageSrc(svc.image_url) ? el("img", { src: svc.image_url, alt: svc.title, className: "service-cover" }) : null,
        el("span", { className: "pill pill-gold", text: (svc.division_name || "MILA Consultancy") + (svc.typical_timeline ? " • " + svc.typical_timeline : "") }),
        el("h1", { className: "serif", text: svc.title || "" }),
        svc.subtitle ? el("div", { style: "color:#d4af7a;font-weight:600;", text: svc.subtitle }) : null
      ])
    );

    if (svc.detailed_description || svc.summary) {
      root.appendChild(
        el("div", { className: "card" }, [
          el("div", { className: "eyebrow", text: "OVERVIEW & ADVISORY MANDATE" }),
          el("p", { className: "muted-copy", text: svc.detailed_description || svc.summary })
        ])
      );
    }

    function addListCard(eyebrow, title, items, elevated) {
      if (!Array.isArray(items) || !items.length) return;
      root.appendChild(
        el("div", { className: "card" + (elevated ? " card-elevated" : "") }, [
          el("div", { className: "eyebrow", text: eyebrow }),
          el("div", { className: "serif", style: "font-size:19px;", text: title }),
          el("ul", { className: "bullet-list" }, items.map((p) => el("li", null, [el("span", { className: "bullet-dot", text: "✓" }), el("span", { text: p })])))
        ])
      );
    }

    addListCard("Scope of Engagement", "Services Included", svc.services_included, false);
    addListCard("Commercial & Strategic Value", "Benefits for Business Owners", svc.owner_benefits, true);
    addListCard("Tangible Outputs", "Deliverables", svc.deliverables, false);

    root.appendChild(
      el("div", { className: "card card-deep" }, [
        el("div", { className: "eyebrow", text: "COMMISSION THIS PRACTICE" }),
        el("div", { className: "serif", style: "font-size:19px;", text: "Ready to Discuss " + (svc.title || "This Service") + "?" }),
        el("button", { className: "btn btn-gold btn-full", type: "button", text: "Enquire Now", onClick: () => openEnquirySheet(svc.service_type, svc.title) })
      ])
    );
  }

  function renderPortfolio(root) {
    const items = state.data.portfolio || [];
    root.appendChild(
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "Our Portfolio" }),
        el("h2", { className: "serif", text: "30+ Pre-Openings & 20+ Setups" }),
        el("p", { className: "muted-copy", text: "Representative luxury hotels, heritage palaces, craft cocktail bars, and specialty cafés across India." })
      ])
    );

    if (!items.length) {
      root.appendChild(el("div", { className: "card" }, [el("p", { className: "muted-copy", text: "Portfolio projects published from the MILA Admin Panel appear here live." })]));
      return;
    }

    items.forEach((p) => {
      const outcomes = Array.isArray(p.outcomes) ? p.outcomes : [];
      root.appendChild(
        el("div", { className: "card" }, [
          el("span", { className: "pill pill-gold", text: p.category || "Hospitality Project" }),
          el("div", { className: "serif", style: "font-size:20px;", text: p.property_name || "" }),
          el("div", { style: "color:#d4af7a;font-size:13px;", text: (p.location || "") + (p.scale ? " • " + p.scale : "") }),
          p.summary ? el("p", { className: "muted-copy", text: p.summary }) : null,
          outcomes.length
            ? el("ul", { className: "bullet-list" }, outcomes.map((o) => el("li", null, [el("span", { className: "bullet-dot", text: "✓" }), el("span", { text: o })])))
            : null,
          el("button", { className: "btn btn-outline btn-full", type: "button", text: "Enquire About Similar Mandate", onClick: () => openEnquirySheet("HOSPITALITY_PRE_OPENING", p.property_name) })
        ])
      );
    });
  }

  function renderCareers(root) {
    const roles = state.data.vacancies || [];
    root.appendChild(
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "MILA Talent • Executive Search" }),
        el("h2", { className: "serif", text: "Open Hospitality Careers" }),
        el("p", { className: "muted-copy", text: "Tap your department to update your CV profile or apply directly to live hospitality roles." })
      ])
    );

    root.appendChild(
      el("div", { className: "card card-elevated" }, [
        el("div", { className: "eyebrow", text: "CANDIDATE CV & DEPARTMENT" }),
        el("div", { className: "serif", style: "font-size:18px;", text: "Update Your CV Profile in Seconds" }),
        el("p", { className: "muted-copy", text: "Quickly tap Hostess, GRE, Bartender, Housekeeping, Driver, Store Keeper, Store Manager, Maintenance, or any hospitality department." }),
        el("button", { className: "btn btn-outline btn-full", type: "button", text: "Update My CV / Department", onClick: openCandidateProfileSheet })
      ])
    );

    if (!roles.length) {
      root.appendChild(el("div", { className: "card" }, [el("p", { className: "muted-copy", text: "No open vacancies published right now. Check back soon or contact our partner desk." })]));
      return;
    }

    roles.forEach((job) => {
      root.appendChild(
        el("div", { className: "card" }, [
          el("div", { style: "display:flex;gap:6px;flex-wrap:wrap;" }, [
            job.is_urgent ? el("span", { className: "pill pill-emerald", text: "URGENT HIRING" }) : null,
            job.category ? el("span", { className: "pill pill-gold", text: job.category }) : null
          ]),
          el("div", { className: "serif", style: "font-size:19px;", text: job.title || "" }),
          el("div", { style: "color:#d4af7a;font-size:13px;", text: [job.property_tier, job.location, job.compensation].filter(Boolean).join(" • ") }),
          job.summary ? el("p", { className: "muted-copy", text: job.summary }) : null,
          el("button", { className: "btn btn-gold btn-full", type: "button", text: "Apply Now", onClick: () => openApplyJobSheet(job) })
        ])
      );
    });
  }

  function renderContact(root) {
    const c = state.data.contact || DEFAULT_CONTACT;
    const safePhone = String(c.phone || "").trim();
    const safeEmail = String(c.email || "").trim();
    const dialTarget = safePhone.replace(/[^+\d]/g, "");

    root.appendChild(
      el("div", { className: "section-heading" }, [
        el("div", { className: "eyebrow", text: "Contact Us" }),
        el("h2", { className: "serif", text: "Connect With MILA Partners" }),
        el("p", { className: "muted-copy", text: "Connect with MILA Consultancy, MILA Talent, and MILA Academy & Expertise across India, South Asia, GCC, and international markets." })
      ])
    );

    const directChildren = [el("div", { className: "eyebrow", text: "DIRECT PARTNER CONTACT" })];
    if (safePhone) {
      directChildren.push(
        el("a", { className: "contact-row", href: "tel:" + dialTarget }, [
          el("div", { className: "contact-icon", text: "☏" }),
          el("div", null, [
            el("div", { style: "font-size:11px;color:#989082;", text: "Phone" }),
            el("div", { style: "font-size:16px;font-weight:700;", text: safePhone })
          ])
        ])
      );
    }
    if (safeEmail) {
      directChildren.push(
        el("a", { className: "contact-row", href: "mailto:" + encodeURIComponent(safeEmail) + "?subject=MILA%20Hospitality%20Enquiry" }, [
          el("div", { className: "contact-icon", text: "✉" }),
          el("div", null, [
            el("div", { style: "font-size:11px;color:#989082;", text: "Email" }),
            el("div", { style: "font-size:15px;font-weight:700;word-break:break-all;", text: safeEmail })
          ])
        ])
      );
    }

    if (!safePhone && !safeEmail) {
      directChildren.push(el("p", { className: "muted-copy", text: "Direct phone and email details will be updated soon by the MILA partner desk." }));
    } else {
      const btnRow = el("div", { className: "btn-row", style: "margin-top:4px;" });
      if (safePhone) btnRow.appendChild(el("a", { className: "btn btn-gold", href: "tel:" + dialTarget, text: "Call Now" }));
      if (safeEmail) btnRow.appendChild(el("a", { className: "btn btn-outline", href: "mailto:" + encodeURIComponent(safeEmail) + "?subject=MILA%20Hospitality%20Enquiry", text: "Email Us" }));
      directChildren.push(btnRow);
    }
    root.appendChild(el("div", { className: "card" }, directChildren));

    const regCard = el("div", { className: "card card-deep" }, [
      el("div", { className: "eyebrow", text: "REGIONAL COVERAGE & ENQUIRY DESK" }),
      el("div", { style: "font-size:13px;font-weight:600;", text: c.responseWindow || DEFAULT_CONTACT.responseWindow }),
      c.deskNote ? el("p", { className: "muted-copy", text: c.deskNote }) : null
    ]);
    SERVICE_REGIONS.forEach(([region, detail]) => {
      regCard.appendChild(
        el("div", { style: "border-top:1px solid rgba(212,175,122,0.16);padding-top:10px;" }, [
          el("div", { style: "font-weight:700;font-size:14px;color:#f5f2eb;", text: region }),
          el("div", { style: "font-size:12px;color:#989082;margin-top:2px;", text: detail })
        ])
      );
    });
    root.appendChild(regCard);

    root.appendChild(
      el("div", { className: "card" }, [
        el("div", { className: "eyebrow", text: "PRIVACY & ACCOUNT DATA" }),
        el("p", {
          className: "muted-copy",
          text: "Review how MILA Hospitality safeguards your personal details and resumes, or request permanent deletion of your account and data at any time."
        }),
        el("div", { className: "btn-row", style: "margin-top:6px;" }, [
          el("a", {
            className: "btn btn-outline",
            href: "../legal/privacy-policy.html",
            target: "_blank",
            rel: "noopener noreferrer",
            text: "Privacy Policy"
          }),
          el("a", {
            className: "btn btn-outline",
            href: "../legal/delete-account.html",
            target: "_blank",
            rel: "noopener noreferrer",
            text: "Delete Account"
          })
        ])
      ])
    );
  }

  function renderScreen() {
    const root = document.getElementById("screenRoot");
    clearNode(root);
    document.querySelectorAll(".tab-item").forEach((btn) => {
      btn.classList.toggle("active", btn.getAttribute("data-tab") === state.activeTab);
    });

    if (state.activeTab === "home") renderHome(root);
    else if (state.activeTab === "services") renderServices(root);
    else if (state.activeTab === "portfolio") renderPortfolio(root);
    else if (state.activeTab === "careers") renderCareers(root);
    else if (state.activeTab === "contact") renderContact(root);
    window.scrollTo({ top: 0, behavior: "instant" });
  }

  function switchTab(tabId) {
    state.activeTab = tabId;
    if (tabId !== "services") state.selectedServiceSlug = null;
    closeDrawer();
    renderScreen();
    refreshPublicCatalog();
  }

  function openServiceDetail(slug) {
    state.activeTab = "services";
    state.selectedServiceSlug = slug;
    closeDrawer();
    renderScreen();
  }

  function renderDrawer() {
    const nav = document.getElementById("drawerNavList");
    clearNode(nav);
    const label = document.getElementById("drawerUserLabel");
    label.textContent = state.user ? (state.user.fullName || state.user.email) : "Guest Preview";

    const addGroup = (title) => nav.appendChild(el("div", { className: "drawer-group", text: title }));
    const addLink = (text, onClick, isActive) => nav.appendChild(el("button", { type: "button", className: "drawer-link" + (isActive ? " active" : ""), text, onClick }));

    addGroup("MILA Overview");
    addLink("Home", () => switchTab("home"), state.activeTab === "home");
    addLink("Our Services", () => switchTab("services"), state.activeTab === "services" && !state.selectedServiceSlug);
    addLink("Our Portfolio", () => switchTab("portfolio"), state.activeTab === "portfolio");
    addLink("Careers (MILA Talent)", () => switchTab("careers"), state.activeTab === "careers");
    addLink("Contact Us", () => switchTab("contact"), state.activeTab === "contact");

    const svcs = state.data.services || [];
    if (svcs.length) {
      addGroup("Our Services (" + svcs.length + ")");
      svcs.forEach((s, idx) => {
        const num = String(s.sort_order || idx + 1).padStart(2, "0");
        addLink(num + ". " + (s.title || ""), () => openServiceDetail(s.slug || s.id), state.selectedServiceSlug === (s.slug || s.id));
      });
    }

    if (!isRunningStandalone()) {
      addGroup("Install Free Phone App");
      addLink("Add MILA to iPhone / Home Screen", () => {
        closeDrawer();
        triggerInstallOrGuide();
      }, false);
    }

    addGroup("Account");
    if (state.user) {
      addLink("Sign Out (" + state.user.email + ")", async () => {
        closeDrawer();
        await signOutUser();
      }, false);
    } else {
      addLink("Sign In with Email OTP", () => {
        closeDrawer();
        showAuthGate();
      }, false);
    }

    addGroup("Legal & Privacy");
    nav.appendChild(
      el("a", {
        className: "drawer-link",
        href: "../legal/privacy-policy.html",
        target: "_blank",
        rel: "noopener noreferrer",
        text: "Privacy Policy"
      })
    );
    nav.appendChild(
      el("a", {
        className: "drawer-link",
        href: "../legal/delete-account.html",
        target: "_blank",
        rel: "noopener noreferrer",
        text: "Request Account & Data Deletion"
      })
    );
  }

  function openDrawer() {
    document.getElementById("sideDrawer").classList.add("open");
    document.getElementById("drawerBackdrop").classList.remove("hidden");
  }

  function closeDrawer() {
    document.getElementById("sideDrawer").classList.remove("open");
    document.getElementById("drawerBackdrop").classList.add("hidden");
  }

  function updateAccountAvatar() {
    const btn = document.getElementById("accountBtn");
    const src = state.user ? (state.user.fullName || state.user.email || "MI") : "IN";
    btn.textContent = src.trim().slice(0, 2).toUpperCase();
  }

  function showAuthGate() {
    document.getElementById("appShell").classList.add("hidden");
    document.getElementById("authGateView").classList.remove("hidden");
  }

  function enterAppShell() {
    document.getElementById("authGateView").classList.add("hidden");
    document.getElementById("appShell").classList.remove("hidden");
    updateAccountAvatar();
    renderDrawer();
    renderScreen();
    refreshPublicCatalog();
  }

  async function signOutUser() {
    const tok = state.accessToken;
    state.accessToken = null;
    state.tokenExpiresAt = 0;
    state.user = null;
    state.guestMode = false;
    await clearSavedSession();
    if (tok && isConfigured()) {
      fetch(state.supabaseUrl + "/auth/v1/logout", {
        method: "POST",
        headers: { apikey: state.supabaseAnonKey, Authorization: "Bearer " + tok }
      }).catch(() => {});
    }
    showAuthGate();
    showToast("Signed out.");
  }

  // Auth OTP Flow
  function setAuthMessage(errMsg, okMsg) {
    const e = document.getElementById("authError");
    const s = document.getElementById("authSuccess");
    e.classList.toggle("hidden", !errMsg);
    s.classList.toggle("hidden", !okMsg);
    if (errMsg) e.textContent = errMsg;
    if (okMsg) s.textContent = okMsg;
  }

  async function handleSendOtp(ev) {
    ev.preventDefault();
    setAuthMessage("", "");
    const email = document.getElementById("authEmail").value.trim().toLowerCase();
    const fullName = document.getElementById("authFullName").value.trim();
    if (!email || !email.includes("@")) {
      setAuthMessage("Please enter a valid email address.", "");
      return;
    }
    if (!isConfigured()) {
      setAuthMessage("Connect Supabase secrets during GitHub Actions build or tap Browse Services below.", "");
      return;
    }
    const btn = document.getElementById("sendOtpBtn");
    btn.disabled = true;
    btn.textContent = "Sending 6-Digit Code...";
    try {
      const res = await fetch(state.supabaseUrl + "/auth/v1/otp", {
        method: "POST",
        headers: { apikey: state.supabaseAnonKey, "Content-Type": "application/json" },
        body: JSON.stringify({
          email,
          create_user: true,
          data: fullName ? { full_name: fullName } : {}
        })
      });
      const json = await res.json().catch(() => null);
      if (!res.ok) {
        throw new Error((json && (json.msg || json.message || json.error_description)) || "Could not send login code.");
      }
      state.pendingOtpEmail = email;
      state.pendingFullName = fullName;
      document.getElementById("otpRequestForm").classList.add("hidden");
      document.getElementById("otpVerifyForm").classList.remove("hidden");
      setAuthMessage("", "6-digit verification code sent to " + email + ". Check your inbox.");
    } catch (err) {
      setAuthMessage(err.message || "Could not send verification code.", "");
    } finally {
      btn.disabled = false;
      btn.textContent = "Send 6-Digit Login Code";
    }
  }

  async function handleVerifyOtp(ev) {
    ev.preventDefault();
    setAuthMessage("", "");
    const token = document.getElementById("authOtpCode").value.trim().replace(/\s+/g, "");
    if (token.length < 6) {
      setAuthMessage("Enter the 6-digit verification code from your email.", "");
      return;
    }
    const btn = document.getElementById("verifyOtpBtn");
    btn.disabled = true;
    btn.textContent = "Verifying Code...";
    try {
      let authJson = null;
      for (const otpType of ["email", "signup"]) {
        const res = await fetch(state.supabaseUrl + "/auth/v1/verify", {
          method: "POST",
          headers: { apikey: state.supabaseAnonKey, "Content-Type": "application/json" },
          body: JSON.stringify({ email: state.pendingOtpEmail, token, type: otpType })
        });
        const parsed = await res.json().catch(() => null);
        if (res.ok && parsed && parsed.access_token) {
          authJson = parsed;
          break;
        }
      }
      if (!authJson || !authJson.access_token) {
        throw new Error("Invalid or expired 6-digit code. Please request a fresh code.");
      }
      state.accessToken = authJson.access_token;
      state.tokenExpiresAt = Date.now() + Math.max(60, Number(authJson.expires_in || 3600)) * 1000;
      const u = authJson.user || {};
      state.user = {
        id: u.id || "",
        email: u.email || state.pendingOtpEmail,
        fullName: state.pendingFullName || (u.user_metadata && u.user_metadata.full_name) || ""
      };
      if (authJson.refresh_token) {
        await writeSavedSession({
          refreshToken: authJson.refresh_token,
          userId: state.user.id,
          email: state.user.email,
          fullName: state.user.fullName
        });
      }
      await syncLoggedInUserProfile();
      document.getElementById("authOtpCode").value = "";
      enterAppShell();
      showToast("Welcome to MILA Hospitality!");
    } catch (err) {
      setAuthMessage(err.message || "Verification failed.", "");
    } finally {
      btn.disabled = false;
      btn.textContent = "Verify & Enter MILA App";
    }
  }

  // Free Installable iPhone / PWA Support
  const INSTALL_DISMISS_KEY = "mila_ios_install_banner_dismissed_v1";
  let deferredInstallPrompt = null;

  function isRunningStandalone() {
    return (
      window.navigator.standalone === true ||
      (window.matchMedia && window.matchMedia("(display-mode: standalone)").matches)
    );
  }

  function isIosDevice() {
    const ua = window.navigator.userAgent || "";
    const isIpadDesktop = window.navigator.platform === "MacIntel" && window.navigator.maxTouchPoints > 1;
    return /iPhone|iPad|iPod/i.test(ua) || isIpadDesktop;
  }

  function openInstallGuideSheet() {
    const onIos = isIosDevice();
    const steps = onIos
      ? [
          ["1", "Open this page in Safari", "If you opened the link inside WhatsApp or Instagram, tap the Safari compass icon first."],
          ["2", "Tap the Share button at the bottom", "Look for the square icon with an upward arrow (↑) in Safari's bottom toolbar."],
          ["3", "Tap 'Add to Home Screen' → 'Add'", "The gold MILA icon appears on your iPhone Home Screen and opens full-screen like an App Store app — 100% free."]
        ]
      : [
          ["1", "Open in Safari (iPhone) or Chrome (Android)", "Share this link to your iPhone and open it in Apple Safari."],
          ["2", "Tap Share (iPhone) or Menu ⋮ (Android)", "Choose 'Add to Home Screen' (iPhone) or 'Install App' (Android)."],
          ["3", "Tap Add / Install", "MILA installs right onto your Home Screen with its gold crest icon and stays synced live with your Admin Panel."]
        ];

    const bodyNodes = [
      el("div", { style: "display:flex;align-items:center;gap:12px;" }, [
        el("img", { src: "./apple-touch-icon.png", alt: "MILA Icon", className: "crest-badge-img" }),
        el("div", null, [
          el("div", { className: "serif", style: "font-size:18px;", text: "MILA Hospitality" }),
          el("div", { className: "muted-copy", text: "100% Free • No App Store • Live Synced" })
        ])
      ]),
      ...steps.map(([num, title, desc]) =>
        el("div", { className: "install-step" }, [
          el("div", { className: "install-step-num", text: num }),
          el("div", null, [
            el("div", { className: "install-step-title", text: title }),
            el("div", { className: "install-step-desc", text: desc })
          ])
        ])
      )
    ];

    const footerNodes = [
      el("button", { className: "btn btn-gold btn-full", type: "button", text: "Got It", onClick: closeSheet })
    ];
    openSheet("Install MILA on iPhone (Free)", bodyNodes, footerNodes);
  }

  async function triggerInstallOrGuide() {
    if (deferredInstallPrompt) {
      try {
        deferredInstallPrompt.prompt();
        const choice = await deferredInstallPrompt.userChoice;
        deferredInstallPrompt = null;
        if (choice && choice.outcome === "accepted") {
          hideInstallBanner();
          showToast("MILA Hospitality installed to your Home Screen!");
          return;
        }
      } catch {
        // Fallback to manual guide
      }
    }
    openInstallGuideSheet();
  }

  function hideInstallBanner() {
    const bar = document.getElementById("installBanner");
    if (bar) bar.classList.add("hidden");
    const authBtn = document.getElementById("authInstallBtn");
    if (authBtn) authBtn.classList.add("hidden");
  }

  function setupInstallUi() {
    if (isRunningStandalone()) {
      hideInstallBanner();
      return;
    }
    const authBtn = document.getElementById("authInstallBtn");
    if (authBtn) {
      authBtn.classList.remove("hidden");
      authBtn.addEventListener("click", triggerInstallOrGuide);
    }
    const bar = document.getElementById("installBanner");
    let dismissed = false;
    try {
      dismissed = window.sessionStorage.getItem(INSTALL_DISMISS_KEY) === "1";
    } catch {
      dismissed = false;
    }
    if (bar && !dismissed) {
      bar.classList.remove("hidden");
    }
    const actionBtn = document.getElementById("installBannerAction");
    if (actionBtn) actionBtn.addEventListener("click", triggerInstallOrGuide);
    const dismissBtn = document.getElementById("installBannerDismiss");
    if (dismissBtn) {
      dismissBtn.addEventListener("click", () => {
        if (bar) bar.classList.add("hidden");
        try {
          window.sessionStorage.setItem(INSTALL_DISMISS_KEY, "1");
        } catch {
          // ignore
        }
      });
    }

    window.addEventListener("beforeinstallprompt", (ev) => {
      ev.preventDefault();
      deferredInstallPrompt = ev;
      if (bar) bar.classList.remove("hidden");
    });

    window.addEventListener("appinstalled", () => {
      deferredInstallPrompt = null;
      hideInstallBanner();
      renderDrawer();
      showToast("MILA Hospitality added to your Home Screen!");
    });
  }

  function registerServiceWorker() {
    if (!("serviceWorker" in navigator)) return;
    const doRegister = () => {
      navigator.serviceWorker.register("./sw.js").catch(() => {});
    };
    if (document.readyState === "complete") doRegister();
    else window.addEventListener("load", doRegister, { once: true });
  }

  document.addEventListener("DOMContentLoaded", async () => {
    registerServiceWorker();
    setupInstallUi();

    document.getElementById("otpRequestForm").addEventListener("submit", handleSendOtp);
    document.getElementById("otpVerifyForm").addEventListener("submit", handleVerifyOtp);
    document.getElementById("changeEmailBtn").addEventListener("click", () => {
      document.getElementById("otpVerifyForm").classList.add("hidden");
      document.getElementById("otpRequestForm").classList.remove("hidden");
      setAuthMessage("", "");
    });
    document.getElementById("guestBrowseBtn").addEventListener("click", () => {
      state.guestMode = true;
      enterAppShell();
    });

    document.querySelectorAll(".tab-item").forEach((btn) => {
      btn.addEventListener("click", () => switchTab(btn.getAttribute("data-tab")));
    });
    document.getElementById("menuBtn").addEventListener("click", openDrawer);
    document.getElementById("closeDrawerBtn").addEventListener("click", closeDrawer);
    document.getElementById("drawerBackdrop").addEventListener("click", closeDrawer);
    document.getElementById("accountBtn").addEventListener("click", openAccountSheet);
    document.getElementById("sheetCloseBtn").addEventListener("click", closeSheet);
    document.getElementById("sheetBackdrop").addEventListener("click", (e) => {
      if (e.target.id === "sheetBackdrop") closeSheet();
    });

    document.addEventListener("visibilitychange", () => {
      if (!document.hidden && !document.getElementById("appShell").classList.contains("hidden")) {
        refreshPublicCatalog();
      }
    });

    const restored = await refreshSession();
    if (restored) {
      enterAppShell();
    } else if (!isConfigured()) {
      state.guestMode = true;
      enterAppShell();
    } else {
      showAuthGate();
    }
  });
})();
