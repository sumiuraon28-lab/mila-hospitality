// Supabase Edge Function: Server-Side Role Verification & Authorised CV Access
// Deploy with: supabase functions deploy verify-admin-role
// Uses SUPABASE_SERVICE_ROLE_KEY strictly on the server (Deno.env) — NEVER in Android frontend code.

import { createClient } from "https://esm.sh/@supabase/supabase-js@2";

const ALLOWED_ADMIN_ROLES = new Set([
  "SUPER_ADMIN",
  "RECRUITMENT_ADMIN",
  "CONTENT_ADMIN",
  "CONSULTANT",
  "REPORTING_ADMIN",
]);

Deno.serve(async (req: Request) => {
  if (req.method === "OPTIONS") {
    return new Response(null, { status: 204 });
  }

  try {
    const authHeader = req.headers.get("Authorization") ?? "";
    if (!authHeader.startsWith("Bearer ")) {
      return jsonResponse({ authorized: false, error: "Missing bearer token." }, 401);
    }
    const jwt = authHeader.slice("Bearer ".length).trim();

    const supabaseUrl = Deno.env.get("SUPABASE_URL");
    const serviceRoleKey = Deno.env.get("SUPABASE_SERVICE_ROLE_KEY");
    if (!supabaseUrl || !serviceRoleKey) {
      return jsonResponse({ authorized: false, error: "Server environment not configured." }, 500);
    }

    const adminClient = createClient(supabaseUrl, serviceRoleKey, {
      auth: { persistSession: false, autoRefreshToken: false },
    });

    const { data: userData, error: userErr } = await adminClient.auth.getUser(jwt);
    if (userErr || !userData?.user) {
      return jsonResponse({ authorized: false, error: "Invalid or expired session token." }, 401);
    }

    const user = userData.user;
    const normalizedEmail = (user.email ?? "").trim().toLowerCase();

    let { data: adminRecord } = await adminClient
      .from("admin_users")
      .select("id, user_id, email, display_name, role, active, assigned_department")
      .eq("user_id", user.id)
      .maybeSingle();

    if (!adminRecord && normalizedEmail && user.email_confirmed_at) {
      const { data: invite } = await adminClient
        .from("admin_invites")
        .select("id, email, display_name, role, active")
        .eq("email", normalizedEmail)
        .eq("active", true)
        .maybeSingle();

      if (invite && ALLOWED_ADMIN_ROLES.has(invite.role)) {
        const { data: created } = await adminClient
          .from("admin_users")
          .insert({
            user_id: user.id,
            email: normalizedEmail,
            display_name: invite.display_name,
            role: invite.role,
            active: true,
          })
          .select("id, user_id, email, display_name, role, active, assigned_department")
          .single();
        adminRecord = created ?? null;
      }
    }

    if (!adminRecord || !adminRecord.active || !ALLOWED_ADMIN_ROLES.has(adminRecord.role)) {
      return jsonResponse({ authorized: false, error: "Administrator access denied." }, 403);
    }

    // Optional action: generate a 60-second signed URL strictly for authorised Recruitment/Super Admins
    if (req.method === "POST") {
      const body = await req.json().catch(() => ({}));
      if (body?.action === "sign_cv_download") {
        const storagePath = String(body?.storage_path ?? "").trim();
        if (adminRecord.role !== "SUPER_ADMIN" && adminRecord.role !== "RECRUITMENT_ADMIN") {
          return jsonResponse({ authorized: false, error: "CV access restricted to Recruitment Admins." }, 403);
        }
        if (
          !storagePath ||
          storagePath.includes("..") ||
          storagePath.toLowerCase().startsWith("http") ||
          storagePath.includes("/object/public/")
        ) {
          return jsonResponse({ authorized: false, error: "Invalid CV storage path." }, 400);
        }

        const { data: signed, error: signErr } = await adminClient.storage
          .from("candidate-cvs")
          .createSignedUrl(storagePath, 60);

        if (signErr || !signed?.signedUrl) {
          return jsonResponse({ authorized: true, error: "Unable to create short-lived CV URL." }, 404);
        }

        await adminClient.from("activity_logs").insert({
          actor_uid: user.id,
          actor_email: adminRecord.email,
          actor_role: adminRecord.role,
          module: "CV_DATABASE",
          action: "SIGNED_CV_ACCESS",
          target_id: storagePath,
          summary: `Issued 60s private CV access for ${storagePath}`,
        });

        return jsonResponse({
          authorized: true,
          role: adminRecord.role,
          signed_url: signed.signedUrl,
          expires_in_seconds: 60,
        }, 200);
      }
    }

    return jsonResponse({
      authorized: true,
      admin: adminRecord,
    }, 200);
  } catch (_err) {
    return jsonResponse({ authorized: false, error: "Internal verification error." }, 500);
  }
});

function jsonResponse(payload: Record<string, unknown>, status: number): Response {
  return new Response(JSON.stringify(payload), {
    status,
    headers: { "Content-Type": "application/json; charset=utf-8" },
  });
}
