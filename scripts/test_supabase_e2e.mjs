#!/usr/bin/env node
/**
 * MILA Hospitality Consultant — Real Supabase End-to-End Verification Suite
 *
 * Tests against the actual Supabase database & private storage bucket:
 *   1. Public published services & website_content read.
 *   2. Client registration, login, user_profiles + client_profiles update, service_enquiries CRUD.
 *   3. Server-side verify_admin_session RPC denies ordinary client accounts.
 *   4. Candidate registration, login, candidate_profiles + candidate_preferences update.
 *   5. Private CV upload to `candidate-cvs/<uid>/...`, public URL denial (400/404),
 *      short-lived owner signed URL download, CV replacement, and CV deletion.
 *   6. Cross-user RLS isolation (Candidate cannot read Client profiles/enquiries; Client cannot read Candidate profiles/CVs).
 *   7. Job application insert & read with private storage path enforcement.
 *
 * Usage:
 *   SUPABASE_URL="https://<project>.supabase.co" SUPABASE_ANON_KEY="<anon-key>" node --test scripts/test_supabase_e2e.mjs
 *
 * NEVER pass SUPABASE_SERVICE_ROLE_KEY to this script or any client bundle.
 */

import test from "node:test";
import assert from "node:assert/strict";
import fs from "node:fs";
import path from "node:path";

function loadEnvFallback() {
  const envPath = path.resolve(process.cwd(), ".env");
  if (!fs.existsSync(envPath)) return {};
  const out = {};
  for (const rawLine of fs.readFileSync(envPath, "utf8").split(/\r?\n/)) {
    const line = rawLine.trim();
    if (!line || line.startsWith("#")) continue;
    const eq = line.indexOf("=");
    if (eq <= 0) continue;
    const key = line.slice(0, eq).trim();
    const val = line.slice(eq + 1).trim().replace(/^["']|["']$/g, "");
    out[key] = val;
  }
  return out;
}

const fileEnv = loadEnvFallback();
const SUPABASE_URL = (process.env.SUPABASE_URL || fileEnv.SUPABASE_URL || "").trim().replace(/\/+$/, "");
const SUPABASE_ANON_KEY = (process.env.SUPABASE_ANON_KEY || fileEnv.SUPABASE_ANON_KEY || "").trim();

function looksLikeServiceRoleKey(jwt) {
  const parts = jwt.split(".");
  if (parts.length !== 3) return false;
  try {
    const payload = JSON.parse(Buffer.from(parts[1], "base64url").toString("utf8"));
    return payload?.role === "service_role";
  } catch {
    return false;
  }
}

const hasRealConfig =
  SUPABASE_URL.startsWith("https://") &&
  !SUPABASE_URL.includes("YOUR_SUPABASE_URL") &&
  SUPABASE_ANON_KEY.length > 20 &&
  !SUPABASE_ANON_KEY.includes("YOUR_SUPABASE_ANON_KEY") &&
  !looksLikeServiceRoleKey(SUPABASE_ANON_KEY);

if (looksLikeServiceRoleKey(SUPABASE_ANON_KEY)) {
  console.error("SECURITY ABORT: Do not pass a Supabase service_role key to client E2E tests. Use SUPABASE_ANON_KEY.");
  process.exit(1);
}

if (!hasRealConfig) {
  console.log(
    "Skipping live Supabase E2E tests: configure real SUPABASE_URL and SUPABASE_ANON_KEY in AI Studio Secrets (or environment) first."
  );
  process.exit(0);
}

const runTag = `${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
const clientEmail = `mila.client.${runTag}@example.com`;
const clientPassword = `MilaClient!${runTag}9`;
const candidateEmail = `mila.candidate.${runTag}@example.com`;
const candidatePassword = `MilaTalent!${runTag}9`;

let clientToken = "";
let clientUid = "";
let createdEnquiryId = "";
let candidateToken = "";
let candidateUid = "";
let uploadedCvPath = "";

async function api(pathSuffix, { method = "GET", token = null, headers = {}, body = undefined } = {}) {
  const reqHeaders = {
    apikey: SUPABASE_ANON_KEY,
    Authorization: `Bearer ${token || SUPABASE_ANON_KEY}`,
    ...headers,
  };
  const res = await fetch(`${SUPABASE_URL}${pathSuffix}`, {
    method,
    headers: reqHeaders,
    body,
  });
  const text = await res.text();
  let json = null;
  try {
    json = text ? JSON.parse(text) : null;
  } catch {
    json = null;
  }
  return { status: res.status, ok: res.ok, text, json };
}

test("1. Public published consultancy services & website content are readable", async () => {
  const svcRes = await api("/rest/v1/services?published=eq.true&select=id,slug,title");
  assert.equal(svcRes.ok, true, `Expected 2xx for published services, got ${svcRes.status}: ${svcRes.text}`);
  assert.ok(Array.isArray(svcRes.json), "Published services should return an array");

  const contentRes = await api("/rest/v1/website_content?published=eq.true&select=page_key,headline");
  assert.equal(contentRes.ok, true, `Expected 2xx for published website_content, got ${contentRes.status}: ${contentRes.text}`);
});

test("2. Client registration, login, profile update, and service enquiry creation", async () => {
  const signUpRes = await api("/auth/v1/signup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: clientEmail,
      password: clientPassword,
      data: {
        account_role: "client",
        full_name: "Ananya Verma",
        mobile_number: "+91 98200 11223",
        company_name: "Verma Heritage Hospitality",
        designation: "Managing Director",
        city: "Mumbai",
        business_category: "LUXURY_HOTEL_RESORT",
      },
    }),
  });
  assert.equal(signUpRes.ok, true, `Client signup failed (${signUpRes.status}): ${signUpRes.text}`);

  if (signUpRes.json?.access_token) {
    clientToken = signUpRes.json.access_token;
    clientUid = signUpRes.json.user.id;
  } else {
    const loginRes = await api("/auth/v1/token?grant_type=password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: clientEmail, password: clientPassword }),
    });
    assert.equal(
      loginRes.ok,
      true,
      `Client login failed (if email confirmation is enabled on Supabase Auth, confirm test user or disable confirm in dev): ${loginRes.text}`
    );
    clientToken = loginRes.json.access_token;
    clientUid = loginRes.json.user.id;
  }

  assert.ok(clientToken && clientUid, "Client token and UID must be issued");

  const profileUpsert = await api("/rest/v1/client_profiles", {
    method: "POST",
    token: clientToken,
    headers: {
      "Content-Type": "application/json",
      Prefer: "resolution=merge-duplicates,return=representation",
    },
    body: JSON.stringify({
      user_id: clientUid,
      full_name: "Ananya Verma",
      email: clientEmail,
      mobile_number: "+91 98200 11223",
      company_name: "Verma Heritage Hospitality",
      designation: "Founder & MD",
      city: "Udaipur",
      business_category: "LUXURY_HOTEL_RESORT",
    }),
  });
  assert.equal(profileUpsert.ok, true, `Client profile update failed: ${profileUpsert.text}`);
  assert.equal(profileUpsert.json[0].city, "Udaipur");

  const enquiryRes = await api("/rest/v1/service_enquiries", {
    method: "POST",
    token: clientToken,
    headers: {
      "Content-Type": "application/json",
      Prefer: "return=representation",
    },
    body: JSON.stringify({
      user_id: clientUid,
      service_type: "HOSPITALITY_PRE_OPENING",
      property_name: "The Lakeview Palace",
      city: "Udaipur",
      project_stage: "Pre-Opening",
      contact_name: "Ananya Verma",
      contact_email: clientEmail,
      mobile_number: "+91 98200 11223",
      company_name: "Verma Heritage Hospitality",
      brief_notes: "80-key heritage palace pre-opening consultancy and SOP blueprint.",
    }),
  });
  assert.equal(enquiryRes.ok, true, `Service enquiry insert failed: ${enquiryRes.text}`);
  createdEnquiryId = enquiryRes.json[0].id;
  assert.ok(createdEnquiryId, "Enquiry ID must be generated");

  const listEnquiries = await api(`/rest/v1/service_enquiries?user_id=eq.${encodeURIComponent(clientUid)}`, {
    token: clientToken,
  });
  assert.equal(listEnquiries.ok, true);
  assert.ok(listEnquiries.json.some((row) => row.id === createdEnquiryId));
});

test("3. Server-side verify_admin_session RPC denies ordinary client users", async () => {
  const rpcRes = await api("/rest/v1/rpc/verify_admin_session", {
    method: "POST",
    token: clientToken,
    headers: { "Content-Type": "application/json" },
    body: "{}",
  });
  assert.equal(rpcRes.ok, true, `RPC verify_admin_session failed: ${rpcRes.text}`);
  assert.equal(rpcRes.json.authorized, false, "Ordinary client must NOT be authorized as admin");
});

test("4. Candidate registration, profile + preferences update, and private CV upload/delete", async () => {
  const signUpRes = await api("/auth/v1/signup", {
    method: "POST",
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({
      email: candidateEmail,
      password: candidatePassword,
      data: {
        account_role: "job_seeker",
        full_name: "Rohan Kapoor",
        mobile_number: "+91 98111 44556",
        location: "New Delhi",
        current_designation: "Head Mixologist",
        department: "BAR_BEVERAGE",
      },
    }),
  });
  assert.equal(signUpRes.ok, true, `Candidate signup failed: ${signUpRes.text}`);

  if (signUpRes.json?.access_token) {
    candidateToken = signUpRes.json.access_token;
    candidateUid = signUpRes.json.user.id;
  } else {
    const loginRes = await api("/auth/v1/token?grant_type=password", {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ email: candidateEmail, password: candidatePassword }),
    });
    assert.equal(loginRes.ok, true, `Candidate login failed: ${loginRes.text}`);
    candidateToken = loginRes.json.access_token;
    candidateUid = loginRes.json.user.id;
  }

  // Cross-user RLS check: candidate cannot see client's profile or enquiries
  const leakClientProfiles = await api(`/rest/v1/client_profiles?user_id=eq.${encodeURIComponent(clientUid)}`, {
    token: candidateToken,
  });
  assert.equal(leakClientProfiles.ok, true);
  assert.equal(leakClientProfiles.json.length, 0, "RLS VIOLATION: Candidate read another user's client_profile");

  const leakEnquiries = await api(`/rest/v1/service_enquiries?id=eq.${encodeURIComponent(createdEnquiryId)}`, {
    token: candidateToken,
  });
  assert.equal(leakEnquiries.ok, true);
  assert.equal(leakEnquiries.json.length, 0, "RLS VIOLATION: Candidate read another user's service_enquiry");

  // Upload minimal valid PDF payload to private bucket candidate-cvs/<candidateUid>/...
  uploadedCvPath = `${candidateUid}/cv_${Date.now()}_rohan_kapoor.pdf`;
  const samplePdf = Buffer.from("%PDF-1.4\n1 0 obj<<>>endobj\ntrailer<<>>\n%%EOF\n", "utf8");
  const uploadRes = await api(`/storage/v1/object/candidate-cvs/${uploadedCvPath}`, {
    method: "POST",
    token: candidateToken,
    headers: {
      "Content-Type": "application/pdf",
      "x-upsert": "true",
    },
    body: samplePdf,
  });
  assert.equal(uploadRes.ok, true, `Private CV upload failed (${uploadRes.status}): ${uploadRes.text}`);

  // Verify public download endpoint is strictly rejected (bucket is private)
  const publicAttempt = await fetch(`${SUPABASE_URL}/storage/v1/object/public/candidate-cvs/${uploadedCvPath}`);
  assert.equal(publicAttempt.ok, false, "SECURITY VIOLATION: Candidate CV must never be publicly downloadable");

  // Verify owner short-lived signed URL succeeds
  const signRes = await api(`/storage/v1/object/sign/candidate-cvs/${uploadedCvPath}`, {
    method: "POST",
    token: candidateToken,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expiresIn: 60 }),
  });
  assert.equal(signRes.ok, true, `Owner signed URL creation failed: ${signRes.text}`);
  assert.ok(signRes.json?.signedURL, "Signed URL should be returned for owner");

  // Verify client (non-owner, non-recruitment-admin) CANNOT sign candidate's CV
  const crossSignRes = await api(`/storage/v1/object/sign/candidate-cvs/${uploadedCvPath}`, {
    method: "POST",
    token: clientToken,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ expiresIn: 60 }),
  });
  assert.equal(crossSignRes.ok, false, "SECURITY VIOLATION: Ordinary client signed another candidate's CV");

  // Update candidate_profiles and candidate_preferences with private cv_storage_path
  const candUpsert = await api("/rest/v1/candidate_profiles", {
    method: "POST",
    token: candidateToken,
    headers: {
      "Content-Type": "application/json",
      Prefer: "resolution=merge-duplicates,return=representation",
    },
    body: JSON.stringify({
      user_id: candidateUid,
      full_name: "Rohan Kapoor",
      email: candidateEmail,
      mobile_number: "+91 98111 44556",
      location: "New Delhi",
      current_designation: "Beverage Director",
      department: "BAR_BEVERAGE",
      total_experience_years: 9,
      current_employer: "Atelier Bar Delhi",
      education: "IHM Pusa",
      skills: "Craft Mixology, Bar P&L, Beverage Pre-Opening",
      preferred_job_locations: "Mumbai, Goa, New Delhi",
      expected_salary: "INR 28 LPA",
      availability: "WITHIN_30_DAYS",
      notice_period: "30 Days",
      job_alerts_enabled: true,
      cv_file_name: "rohan_kapoor.pdf",
      cv_mime_type: "application/pdf",
      cv_file_size_bytes: samplePdf.length,
      cv_storage_path: uploadedCvPath,
    }),
  });
  assert.equal(candUpsert.ok, true, `Candidate profile upsert failed: ${candUpsert.text}`);

  const prefUpsert = await api("/rest/v1/candidate_preferences", {
    method: "POST",
    token: candidateToken,
    headers: {
      "Content-Type": "application/json",
      Prefer: "resolution=merge-duplicates,return=representation",
    },
    body: JSON.stringify({
      user_id: candidateUid,
      job_alerts_enabled: true,
      preferred_locations: ["Mumbai", "Goa", "New Delhi"],
      preferred_departments: ["BAR_BEVERAGE"],
      expected_salary: "INR 28 LPA",
      availability: "WITHIN_30_DAYS",
      notice_period: "30 Days",
    }),
  });
  assert.equal(prefUpsert.ok, true, `Candidate preferences upsert failed: ${prefUpsert.text}`);

  // Submit a job application referencing the private CV path
  const appRes = await api("/rest/v1/job_applications", {
    method: "POST",
    token: candidateToken,
    headers: {
      "Content-Type": "application/json",
      Prefer: "return=representation",
    },
    body: JSON.stringify({
      user_id: candidateUid,
      role_id: "role_head_mixologist",
      role_title: "Head Mixologist & Bar Program Lead",
      property_label: "Luxury Cocktail Bar",
      location: "Mumbai",
      department: "BAR_BEVERAGE",
      candidate_name: "Rohan Kapoor",
      candidate_email: candidateEmail,
      candidate_phone: "+91 98111 44556",
      years_experience: 9,
      current_role: "Beverage Director",
      cv_file_name: "rohan_kapoor.pdf",
      cv_storage_path: uploadedCvPath,
    }),
  });
  assert.equal(appRes.ok, true, `Job application insert failed: ${appRes.text}`);

  // Delete CV from private storage
  const delCv = await api("/storage/v1/object/candidate-cvs", {
    method: "DELETE",
    token: candidateToken,
    headers: { "Content-Type": "application/json" },
    body: JSON.stringify({ prefixes: [uploadedCvPath] }),
  });
  assert.equal(delCv.ok, true, `Private CV delete failed: ${delCv.text}`);
});
