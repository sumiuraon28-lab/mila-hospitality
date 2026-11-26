// ============================================================================
// MILA HOSPITALITY CONSULTANT — Edge Function: dispatch-job-notifications
//
// Server-side multi-channel dispatcher for:
//   - VACANCY_PUBLISHED: matches ONLY eligible candidates via DB matching RPC
//     (department, preferred location, experience, job preferences, active status)
//   - APPLICATION_STATUS_CHANGED: notifies the specific applicant
//
// Channels:
//   - In-App (`public.notifications`)
//   - Android Push via Firebase Cloud Messaging HTTP v1 API
//   - iOS Push via Firebase Cloud Messaging HTTP v1 API (APNs payload)
//   - Optional Email via transactional email provider (RESEND_API_KEY)
//
// Records every delivery attempt/outcome in `public.notification_delivery_logs`.
// NEVER expose service-role or Firebase service-account secrets to client apps.
// ============================================================================

import { createClient } from "https://esm.sh/@supabase/supabase-js@2.49.1";

const corsHeaders = {
  "Access-Control-Allow-Origin": "*",
  "Access-Control-Allow-Headers": "authorization, x-client-info, apikey, content-type",
  "Access-Control-Allow-Methods": "POST, OPTIONS",
};

interface FcmServiceAccount {
  project_id: string;
  client_email: string;
  private_key: string;
  token_uri?: string;
}

function jsonResponse(body: unknown, status = 200): Response {
  return new Response(JSON.stringify(body), {
    status,
    headers: { ...corsHeaders, "Content-Type": "application/json" },
  });
}

function base64UrlEncode(input: Uint8Array | string): string {
  const bytes = typeof input === "string" ? new TextEncoder().encode(input) : input;
  let binary = "";
  for (const b of bytes) binary += String.fromCharCode(b);
  return btoa(binary).replace(/\+/g, "-").replace(/\//g, "_").replace(/=+$/, "");
}

function pemToArrayBuffer(pem: string): ArrayBuffer {
  const clean = pem
    .replace(/-----BEGIN PRIVATE KEY-----/g, "")
    .replace(/-----END PRIVATE KEY-----/g, "")
    .replace(/\s+/g, "");
  const bin = atob(clean);
  const bytes = new Uint8Array(bin.length);
  for (let i = 0; i < bin.length; i++) bytes[i] = bin.charCodeAt(i);
  return bytes.buffer;
}

async function getFcmAccessToken(sa: FcmServiceAccount): Promise<string> {
  const now = Math.floor(Date.now() / 1000);
  const header = base64UrlEncode(JSON.stringify({ alg: "RS256", typ: "JWT" }));
  const claim = base64UrlEncode(
    JSON.stringify({
      iss: sa.client_email,
      scope: "https://www.googleapis.com/auth/firebase.messaging",
      aud: sa.token_uri ?? "https://oauth2.googleapis.com/token",
      iat: now,
      exp: now + 3600,
    }),
  );
  const signingInput = `${header}.${claim}`;
  const key = await crypto.subtle.importKey(
    "pkcs8",
    pemToArrayBuffer(sa.private_key),
    { name: "RSASSA-PKCS1-v1_5", hash: "SHA-256" },
    false,
    ["sign"],
  );
  const sig = await crypto.subtle.sign(
    "RSASSA-PKCS1-v1_5",
    key,
    new TextEncoder().encode(signingInput),
  );
  const assertion = `${signingInput}.${base64UrlEncode(new Uint8Array(sig))}`;

  const tokenRes = await fetch(sa.token_uri ?? "https://oauth2.googleapis.com/token", {
    method: "POST",
    headers: { "Content-Type": "application/x-www-form-urlencoded" },
    body: new URLSearchParams({
      grant_type: "urn:ietf:params:oauth:grant-type:jwt-bearer",
      assertion,
    }),
  });
  const tokenJson = await tokenRes.json();
  if (!tokenRes.ok || !tokenJson.access_token) {
    throw new Error(`FCM OAuth token exchange failed: ${JSON.stringify(tokenJson)}`);
  }
  return tokenJson.access_token as string;
}

async function sendFcmPush(
  projectId: string,
  oauthToken: string,
  deviceToken: string,
  platform: "ANDROID" | "IOS",
  title: string,
  body: string,
  dataPayload: Record<string, string>,
  androidChannelId: string,
): Promise<{ ok: boolean; messageId?: string; error?: string }> {
  const message: Record<string, unknown> = {
    token: deviceToken,
    notification: { title, body },
    data: dataPayload,
  };

  if (platform === "ANDROID") {
    message.android = {
      priority: "HIGH",
      notification: {
        channel_id: androidChannelId,
        sound: "default",
      },
    };
  } else {
    message.apns = {
      headers: {
        "apns-priority": "10",
        "apns-push-type": "alert",
      },
      payload: {
        aps: {
          alert: { title, body },
          sound: "default",
          badge: 1,
          "mutable-content": 1,
        },
      },
    };
  }

  const res = await fetch(
    `https://fcm.googleapis.com/v1/projects/${projectId}/messages:send`,
    {
      method: "POST",
      headers: {
        Authorization: `Bearer ${oauthToken}`,
        "Content-Type": "application/json",
      },
      body: JSON.stringify({ message }),
    },
  );
  const json = await res.json().catch(() => ({}));
  if (res.ok && json.name) {
    return { ok: true, messageId: String(json.name) };
  }
  return { ok: false, error: json?.error?.message ?? `FCM HTTP ${res.status}` };
}

async function sendOptionalEmail(
  resendApiKey: string,
  fromAddress: string,
  toEmail: string,
  subject: string,
  textBody: string,
): Promise<{ ok: boolean; messageId?: string; error?: string }> {
  const res = await fetch("https://api.resend.com/emails", {
    method: "POST",
    headers: {
      Authorization: `Bearer ${resendApiKey}`,
      "Content-Type": "application/json",
    },
    body: JSON.stringify({
      from: fromAddress,
      to: [toEmail],
      subject,
      text: textBody,
    }),
  });
  const json = await res.json().catch(() => ({}));
  if (res.ok && json.id) {
    return { ok: true, messageId: String(json.id) };
  }
  return { ok: false, error: json?.message ?? `Email provider HTTP ${res.status}` };
}

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response("ok", { headers: corsHeaders });
  }
  if (req.method !== "POST") {
    return jsonResponse({ error: "Method not allowed" }, 405);
  }

  const supabaseUrl = Deno.env.get("SUPABASE_URL");
  const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
  if (!supabaseUrl || !serviceRoleKey) {
    return jsonResponse({ error: "Server environment misconfigured" }, 500);
  }

  const authHeader = req.headers.get("Authorization") ?? "";
  const jwt = authHeader.replace(/^Bearer\s+/i, "").trim();
  if (!jwt) {
    return jsonResponse({ error: "Missing bearer token" }, 401);
  }

  const adminClient = createClient(supabaseUrl, serviceRoleKey, {
    auth: { persistSession: false, autoRefreshToken: false },
  });

  // 1. Server-side verify caller is SUPER_ADMIN or RECRUITMENT_ADMIN
  const { data: userData, error: userErr } = await adminClient.auth.getUser(jwt);
  if (userErr || !userData?.user) {
    return jsonResponse({ error: "Invalid session" }, 401);
  }
  const actorUid = userData.user.id;
  const { data: adminRow } = await adminClient
    .from("admin_users")
    .select("role, active")
    .eq("user_id", actorUid)
    .maybeSingle();

  if (!adminRow?.active || !["SUPER_ADMIN", "RECRUITMENT_ADMIN"].includes(adminRow.role)) {
    return jsonResponse({ error: "Forbidden: recruitment or super admin role required" }, 403);
  }

  const payload = await req.json().catch(() => ({}));
  const eventType = String(payload?.event_type ?? "");

  // Load administrator notification controls
  const { data: controls } = await adminClient
    .from("notification_controls")
    .select("*")
    .eq("id", "global")
    .single();

  // Optional FCM service account & email provider from server secrets
  let fcmSa: FcmServiceAccount | null = null;
  let fcmToken: string | null = null;
  const rawFcmSa = Deno.env.get("FIREBASE_SERVICE_ACCOUNT_JSON");
  if (rawFcmSa) {
    try {
      fcmSa = JSON.parse(rawFcmSa) as FcmServiceAccount;
      fcmToken = await getFcmAccessToken(fcmSa);
    } catch (err) {
      console.error("FCM initialization failed:", err);
    }
  }
  const resendApiKey = Deno.env.get("RESEND_API_KEY") ?? "";
  const emailFrom = Deno.env.get("MILA_EMAIL_FROM") ?? "MILA Talent <talent@milahospitality.in>";

  const deliveryLogs: Record<string, unknown>[] = [];
  let matchedCount = 0;

  if (eventType === "VACANCY_PUBLISHED") {
    const vacancyId = String(payload?.vacancy_id ?? "").trim();
    if (!vacancyId) return jsonResponse({ error: "vacancy_id required" }, 400);

    const { data: vacancy } = await adminClient
      .from("job_vacancies")
      .select("*")
      .eq("id", vacancyId)
      .single();

    if (!vacancy || !vacancy.published) {
      return jsonResponse({ error: "Vacancy not found or not published" }, 404);
    }

    // Use the caller's user-scoped client for RPC or run direct SQL matching filter
    const userClient = createClient(supabaseUrl, Deno.env.get("SUPABASE_ANON_KEY") ?? serviceRoleKey, {
      global: { headers: { Authorization: `Bearer ${jwt}` } },
      auth: { persistSession: false, autoRefreshToken: false },
    });
    const { data: eligibleCandidates, error: matchErr } = await userClient.rpc(
      "match_eligible_candidates_for_vacancy",
      { p_vacancy_id: vacancyId },
    );
    if (matchErr) {
      return jsonResponse({ error: matchErr.message }, 500);
    }

    const candidates = eligibleCandidates ?? [];
    matchedCount = candidates.length;

    const notifTitle = `Matched Vacancy: ${vacancy.title}`;
    const notifBody = `${vacancy.property_tier} • ${vacancy.location} (${vacancy.experience_required})`;

    for (const cand of candidates) {
      let insertedNotifId: string | null = null;

      // 1. In-App notification
      if (controls?.enable_in_app && cand.in_app_enabled) {
        const { data: notifRow, error: nErr } = await adminClient
          .from("notifications")
          .insert({
            user_id: cand.user_id,
            vacancy_id: vacancy.id,
            role_id: vacancy.slug,
            title: notifTitle,
            message: notifBody,
            department: vacancy.category,
            location: vacancy.location,
            is_read: false,
          })
          .select("id")
          .single();

        insertedNotifId = notifRow?.id ?? null;
        deliveryLogs.push({
          notification_id: insertedNotifId,
          recipient_user_id: cand.user_id,
          recipient_name: cand.full_name,
          recipient_email: cand.email,
          event_type: "VACANCY_MATCH",
          vacancy_id: vacancy.id,
          channel: "IN_APP",
          status: nErr ? "FAILED" : "DELIVERED",
          match_score: cand.match_score,
          match_reasons: cand.match_reasons ?? [],
          error_detail: nErr?.message ?? null,
          dispatched_by: actorUid,
        });
      }

      // 2. Android + iOS Push via FCM
      const { data: tokens } = await adminClient
        .from("device_push_tokens")
        .select("fcm_token, platform")
        .eq("user_id", cand.user_id)
        .eq("is_active", true);

      for (const dev of tokens ?? []) {
        const isAndroid = dev.platform === "ANDROID";
        const isIos = dev.platform === "IOS";
        const channel = isAndroid ? "ANDROID_PUSH" : "IOS_PUSH";
        const enabledByAdmin = isAndroid ? controls?.enable_android_push : controls?.enable_ios_push;

        if (!enabledByAdmin || !cand.push_enabled) {
          deliveryLogs.push({
            notification_id: insertedNotifId,
            recipient_user_id: cand.user_id,
            recipient_name: cand.full_name,
            recipient_email: cand.email,
            event_type: "VACANCY_MATCH",
            vacancy_id: vacancy.id,
            channel,
            status: "SKIPPED",
            match_score: cand.match_score,
            match_reasons: cand.match_reasons ?? [],
            error_detail: "Disabled by candidate preference or administrator control",
            dispatched_by: actorUid,
          });
          continue;
        }

        if (fcmSa && fcmToken && (isAndroid || isIos)) {
          const pushRes = await sendFcmPush(
            fcmSa.project_id,
            fcmToken,
            dev.fcm_token,
            isAndroid ? "ANDROID" : "IOS",
            notifTitle,
            notifBody,
            {
              event_type: "VACANCY_MATCH",
              vacancy_id: String(vacancy.id),
              role_slug: String(vacancy.slug),
              route: "careers",
            },
            "mila_job_matches",
          );
          deliveryLogs.push({
            notification_id: insertedNotifId,
            recipient_user_id: cand.user_id,
            recipient_name: cand.full_name,
            recipient_email: cand.email,
            event_type: "VACANCY_MATCH",
            vacancy_id: vacancy.id,
            channel,
            status: pushRes.ok ? "DELIVERED" : "FAILED",
            match_score: cand.match_score,
            match_reasons: cand.match_reasons ?? [],
            provider_message_id: pushRes.messageId ?? null,
            error_detail: pushRes.error ?? null,
            dispatched_by: actorUid,
          });
        } else {
          deliveryLogs.push({
            notification_id: insertedNotifId,
            recipient_user_id: cand.user_id,
            recipient_name: cand.full_name,
            recipient_email: cand.email,
            event_type: "VACANCY_MATCH",
            vacancy_id: vacancy.id,
            channel,
            status: "QUEUED",
            match_score: cand.match_score,
            match_reasons: cand.match_reasons ?? [],
            error_detail: "Configure FIREBASE_SERVICE_ACCOUNT_JSON secret on Edge Function for live FCM delivery",
            dispatched_by: actorUid,
          });
        }
      }

      // 3. Optional Email Notification
      if (controls?.enable_email_notifications && cand.email_enabled && cand.email) {
        if (resendApiKey) {
          const emailRes = await sendOptionalEmail(
            resendApiKey,
            emailFrom,
            cand.email,
            `MILA Talent Match: ${vacancy.title} (${vacancy.location})`,
            `Dear ${cand.full_name},\n\nA new hospitality mandate matches your profile:\n\nRole: ${vacancy.title}\nLocation: ${vacancy.location}\nExperience: ${vacancy.experience_required}\nCompensation: ${vacancy.compensation}\n\nOpen the MILA Hospitality app to review details and apply privately.\n\n— MILA Talent Recruitment`,
          );
          deliveryLogs.push({
            notification_id: insertedNotifId,
            recipient_user_id: cand.user_id,
            recipient_name: cand.full_name,
            recipient_email: cand.email,
            event_type: "VACANCY_MATCH",
            vacancy_id: vacancy.id,
            channel: "EMAIL",
            status: emailRes.ok ? "DELIVERED" : "FAILED",
            match_score: cand.match_score,
            match_reasons: cand.match_reasons ?? [],
            provider_message_id: emailRes.messageId ?? null,
            error_detail: emailRes.error ?? null,
            dispatched_by: actorUid,
          });
        } else {
          deliveryLogs.push({
            notification_id: insertedNotifId,
            recipient_user_id: cand.user_id,
            recipient_name: cand.full_name,
            recipient_email: cand.email,
            event_type: "VACANCY_MATCH",
            vacancy_id: vacancy.id,
            channel: "EMAIL",
            status: "QUEUED",
            match_score: cand.match_score,
            match_reasons: cand.match_reasons ?? [],
            error_detail: "Configure RESEND_API_KEY secret on Edge Function for live email dispatch",
            dispatched_by: actorUid,
          });
        }
      }
    }
  } else if (eventType === "APPLICATION_STATUS_CHANGED") {
    if (controls && !controls.enable_application_status_notifications) {
      return jsonResponse({ dispatched: 0, message: "Application status notifications paused by administrator controls" });
    }
    const applicationId = String(payload?.application_id ?? "").trim();
    const newStatusLabel = String(payload?.status_label ?? "Updated").trim();
    if (!applicationId) return jsonResponse({ error: "application_id required" }, 400);

    const { data: appRow } = await adminClient
      .from("job_applications")
      .select("*")
      .eq("id", applicationId)
      .single();

    if (!appRow) return jsonResponse({ error: "Application not found" }, 404);
    matchedCount = 1;

    const title = `Application Update: ${appRow.role_title}`;
    const body = `Your candidacy status is now: ${newStatusLabel}.`;

    const { data: pref } = await adminClient
      .from("candidate_preferences")
      .select("push_notifications_enabled, in_app_notifications_enabled, email_notifications_enabled, application_status_alerts_enabled")
      .eq("user_id", appRow.user_id)
      .maybeSingle();

    let insertedNotifId: string | null = null;
    if (controls?.enable_in_app !== false && pref?.in_app_notifications_enabled !== false) {
      const { data: notifRow, error: nErr } = await adminClient
        .from("notifications")
        .insert({
          user_id: appRow.user_id,
          role_id: appRow.role_id,
          title,
          message: body,
          department: appRow.department ?? "MILA Talent",
          location: appRow.location,
          is_read: false,
        })
        .select("id")
        .single();
      insertedNotifId = notifRow?.id ?? null;
      deliveryLogs.push({
        notification_id: insertedNotifId,
        recipient_user_id: appRow.user_id,
        recipient_name: appRow.candidate_name,
        recipient_email: appRow.candidate_email,
        event_type: "APPLICATION_STATUS",
        application_id: appRow.id,
        channel: "IN_APP",
        status: nErr ? "FAILED" : "DELIVERED",
        match_reasons: [`Application status -> ${newStatusLabel}`],
        error_detail: nErr?.message ?? null,
        dispatched_by: actorUid,
      });
    }

    const { data: tokens } = await adminClient
      .from("device_push_tokens")
      .select("fcm_token, platform")
      .eq("user_id", appRow.user_id)
      .eq("is_active", true);

    for (const dev of tokens ?? []) {
      const isAndroid = dev.platform === "ANDROID";
      const channel = isAndroid ? "ANDROID_PUSH" : "IOS_PUSH";
      const enabled = isAndroid ? controls?.enable_android_push !== false : controls?.enable_ios_push !== false;
      if (!enabled || pref?.push_notifications_enabled === false) continue;

      if (fcmSa && fcmToken) {
        const pushRes = await sendFcmPush(
          fcmSa.project_id,
          fcmToken,
          dev.fcm_token,
          isAndroid ? "ANDROID" : "IOS",
          title,
          body,
          {
            event_type: "APPLICATION_STATUS",
            application_id: String(appRow.id),
            route: "job_seeker_login",
          },
          "mila_application_updates",
        );
        deliveryLogs.push({
          notification_id: insertedNotifId,
          recipient_user_id: appRow.user_id,
          recipient_name: appRow.candidate_name,
          recipient_email: appRow.candidate_email,
          event_type: "APPLICATION_STATUS",
          application_id: appRow.id,
          channel,
          status: pushRes.ok ? "DELIVERED" : "FAILED",
          match_reasons: [`Application status -> ${newStatusLabel}`],
          provider_message_id: pushRes.messageId ?? null,
          error_detail: pushRes.error ?? null,
          dispatched_by: actorUid,
        });
      }
    }

    if (controls?.enable_email_notifications && pref?.email_notifications_enabled && appRow.candidate_email) {
      if (resendApiKey) {
        const emailRes = await sendOptionalEmail(
          resendApiKey,
          emailFrom,
          appRow.candidate_email,
          `MILA Application Status: ${appRow.role_title}`,
          `Dear ${appRow.candidate_name},\n\nYour application for ${appRow.role_title} (${appRow.location}) has moved to: ${newStatusLabel}.\n\nLog into the MILA Talent portal for details.\n\n— MILA Hospitality Consultant`,
        );
        deliveryLogs.push({
          notification_id: insertedNotifId,
          recipient_user_id: appRow.user_id,
          recipient_name: appRow.candidate_name,
          recipient_email: appRow.candidate_email,
          event_type: "APPLICATION_STATUS",
          application_id: appRow.id,
          channel: "EMAIL",
          status: emailRes.ok ? "DELIVERED" : "FAILED",
          match_reasons: [`Application status -> ${newStatusLabel}`],
          provider_message_id: emailRes.messageId ?? null,
          error_detail: emailRes.error ?? null,
          dispatched_by: actorUid,
        });
      }
    }
  } else {
    return jsonResponse({ error: "Unsupported event_type" }, 400);
  }

  if (deliveryLogs.length > 0) {
    await adminClient.from("notification_delivery_logs").insert(deliveryLogs);
  }

  return jsonResponse({
    event_type: eventType,
    matched_candidates: matchedCount,
    delivery_logs_recorded: deliveryLogs.length,
  });
});
