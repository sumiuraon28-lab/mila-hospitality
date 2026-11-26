const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const ADMIN_UID = "admin_001";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function buildServicePayload(overrides = {}) {
  return {
    serviceType: "HOSPITALITY_PRE_OPENING",
    sortOrder: 1,
    slug: "hospitality-pre-opening-consultancy",
    divisionName: "MILA Consultancy",
    title: "Hospitality Pre-Opening Consultancy",
    subtitle: "Turnkey Critical Path & Opening Readiness",
    summary: "End-to-end pre-opening advisory for luxury hotels and heritage resorts across India.",
    detailedDescription: "Comprehensive pre-opening stewardship guiding owners from site planning and departmental SOP design through opening-night guest choreography.",
    servicesIncluded: ["Critical path milestone governance", "OS&E procurement advisory"],
    ownerBenefits: ["Faster opening readiness", "Disciplined pre-opening budget control"],
    deliverables: ["180-Day Pre-Opening Critical Path", "Departmental SOP Manuals"],
    typicalTimeline: "4 – 12 Months",
    imageKey: "palace_resort",
    published: true,
    updatedAt: new Date(Date.now() - 3000),
    ...overrides,
  };
}

function buildInquiryPayload(uid, overrides = {}) {
  const ts = new Date(Date.now() - 5000);
  return {
    userId: uid,
    propertyName: "Heritage Courtyard Project",
    city: "Mumbai",
    serviceType: "HOSPITALITY_PRE_OPENING",
    scaleSummary: "90 Keys • Luxury Heritage",
    contactName: "Managing Partner",
    contactEmail: "partner@hospitality.in",
    contactPhone: "+91 98200 11223",
    briefNotes: "Pre-opening taskforce and F&B concept alignment for Q4 launch.",
    status: "SUBMITTED",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

function buildApplicationPayload(uid, overrides = {}) {
  const ts = new Date(Date.now() - 5000);
  return {
    userId: uid,
    roleId: "role_head_mixologist_delhi",
    roleTitle: "Head Mixologist & Beverage Manager",
    location: "New Delhi, NCR",
    candidateName: "Candidate Leader",
    candidateEmail: "candidate@mixology.in",
    candidatePhone: "+91 98111 44556",
    yearsExperience: 9,
    currentProperty: "Luxury Urban Hotel",
    coverNote: "Led craft cocktail bar openings across Mumbai and Goa.",
    status: "APPLIED",
    createdAt: ts,
    updatedAt: ts,
    ...overrides,
  };
}

test("CMS Services: public reads published only; only admin can write or read drafts", async () => {
  const adminDb = testEnv.authenticatedContext(ADMIN_UID, { admin: true }).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();

  // Admin creates a published service and a draft service
  await assertSucceeds(
    adminDb.collection("services").doc("hospitality-pre-opening-consultancy").set(buildServicePayload())
  );
  await assertSucceeds(
    adminDb.collection("services").doc("operations-management").set(
      buildServicePayload({
        serviceType: "OPERATIONS_MANAGEMENT",
        sortOrder: 5,
        slug: "operations-management",
        title: "Operations Management",
        published: false,
      })
    )
  );

  // Public filtered list (matches Kotlin ServiceCatalogRepository query) succeeds
  await assertSucceeds(unauthDb.collection("services").where("published", "==", true).get());
  await assertSucceeds(aliceDb.collection("services").where("published", "==", true).get());

  // Published doc get succeeds; draft doc get fails for non-admin
  await assertSucceeds(unauthDb.collection("services").doc("hospitality-pre-opening-consultancy").get());
  await assertFails(unauthDb.collection("services").doc("operations-management").get());

  // Unfiltered list fails for non-admin, succeeds for admin
  await assertFails(unauthDb.collection("services").get());
  await assertFails(aliceDb.collection("services").get());
  await assertSucceeds(adminDb.collection("services").get());

  // Regular user cannot create, update, or delete services
  await assertFails(
    aliceDb.collection("services").doc("bar-beverage").set(
      buildServicePayload({ serviceType: "BAR_BEVERAGE_CONSULTANCY", slug: "bar-beverage" })
    )
  );
  await assertFails(
    aliceDb.collection("services").doc("hospitality-pre-opening-consultancy").update({
      title: "Tampered Title",
      updatedAt: new Date(Date.now() - 1000),
    })
  );
  await assertFails(
    aliceDb.collection("services").doc("hospitality-pre-opening-consultancy").delete()
  );

  // Admin can update and delete
  await assertSucceeds(
    adminDb.collection("services").doc("operations-management").update({
      published: true,
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(adminDb.collection("services").doc("operations-management").delete());
});

test("Unauthenticated user cannot read or write user-scoped collections", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_1").set(
      buildInquiryPayload(ALICE_UID)
    )
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("jobApplications").get()
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("savedRoles").get()
  );
});

test("ConsultationInquiry: supports all 8 service types, partial update, withdraw, and terminal lock", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("consultationInquiries");

  await assertSucceeds(colRef.doc("inq_1").set(buildInquiryPayload(ALICE_UID)));
  await assertSucceeds(
    colRef.doc("inq_8").set(
      buildInquiryPayload(ALICE_UID, { serviceType: "HOSPITALITY_MANAGEMENT_SOLUTIONS" })
    )
  );

  const noNotesPayload = buildInquiryPayload(ALICE_UID, { serviceType: "HOSPITALITY_TRAINING" });
  delete noNotesPayload.briefNotes;
  await assertSucceeds(colRef.doc("inq_2").set(noNotesPayload));

  await assertSucceeds(colRef.get());

  await assertSucceeds(
    colRef.doc("inq_1").update({
      briefNotes: "Updated target launch window to November.",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  await assertSucceeds(
    colRef.doc("inq_1").update({
      status: "WITHDRAWN",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    colRef.doc("inq_1").update({
      briefNotes: "Reopening withdrawn inquiry",
      updatedAt: new Date(Date.now() - 200),
    })
  );

  await assertFails(
    colRef.doc("inq_bad_1").set(buildInquiryPayload(ALICE_UID, { id: "inq_bad_1" }))
  );
  await assertFails(
    colRef.doc("inq_bad_2").set(buildInquiryPayload(ALICE_UID, { status: "UNDER_REVIEW" }))
  );
  await assertFails(
    colRef.doc("inq_bad_3").set(buildInquiryPayload(ALICE_UID, { createdAt: new Date(Date.now() + 60000) }))
  );
});

test("JobApplication: owner create, list, withdraw, and negative bounds", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const colRef = aliceDb.collection("users").doc(ALICE_UID).collection("jobApplications");

  await assertSucceeds(colRef.doc("app_1").set(buildApplicationPayload(ALICE_UID)));
  await assertSucceeds(colRef.get());

  await assertSucceeds(
    colRef.doc("app_1").update({
      status: "WITHDRAWN",
      updatedAt: new Date(Date.now() - 500),
    })
  );

  await assertFails(
    colRef.doc("app_1").update({
      status: "APPLIED",
      updatedAt: new Date(Date.now() - 100),
    })
  );

  await assertFails(
    colRef.doc("app_bad").set(buildApplicationPayload(ALICE_UID, { yearsExperience: 99 }))
  );
});

test("SavedRole and Cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const roleDoc = aliceDb.collection("users").doc(ALICE_UID).collection("savedRoles").doc("role_exec_chef");

  await assertSucceeds(
    roleDoc.set({
      userId: ALICE_UID,
      roleId: "role_exec_chef",
      roleTitle: "Executive Chef",
      location: "Udaipur",
      compensation: "Competitive",
      category: "Culinary & F&B",
      createdAt: new Date(Date.now() - 2000),
    })
  );

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("savedRoles").get());
  await assertFails(roleDoc.update({ roleTitle: "Tampered" }));
  await assertSucceeds(roleDoc.delete());

  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("clientProfiles").get()
  );
});

test("ClientProfile: strictly private to owner and validates profile fields", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 4000);
  const profileRef = aliceDb.collection("users").doc(ALICE_UID).collection("clientProfiles").doc("primary");

  const validProfile = {
    userId: ALICE_UID,
    fullName: "Aarav Mehta",
    email: "aarav@courtyardgroup.in",
    mobileNumber: "+91 98200 11223",
    companyName: "Courtyard Hospitality Pvt Ltd",
    designation: "Managing Director",
    city: "Mumbai",
    businessCategory: "LUXURY_HOTEL_RESORT",
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(profileRef.set(validProfile));
  await assertSucceeds(profileRef.get());
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).collection("clientProfiles").get());

  await assertSucceeds(
    profileRef.update({
      designation: "Founder & MD",
      city: "Goa",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Cross-client isolation: Bob and unauthenticated users cannot read or tamper with Alice's profile
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("clientProfiles").doc("primary").get()
  );
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("clientProfiles").doc("primary").set(validProfile)
  );
  await assertFails(
    unauthDb.collection("users").doc(ALICE_UID).collection("clientProfiles").doc("primary").get()
  );

  // Password field must never be accepted in Firestore ClientProfile
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("clientProfiles").doc("bad_pw").set({
      ...validProfile,
      password: "ShouldNeverBeStoredInFirestore",
    })
  );
});

test("CandidateProfile & JobNotification: private 15-field profile, private CV metadata, alerts, and cross-user isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 4000);
  const candidateRef = aliceDb.collection("users").doc(ALICE_UID).collection("candidateProfiles").doc("primary");

  const validCandidate = {
    userId: ALICE_UID,
    fullName: "Kabir Malhotra",
    email: "kabir.malhotra@hospitality.in",
    mobileNumber: "+91 98100 22334",
    location: "Mumbai, Maharashtra",
    currentDesignation: "Executive Sous Chef",
    department: "CULINARY",
    totalExperienceYears: 9,
    currentEmployer: "Luxury Urban Hotel Mumbai",
    previousEmployers: "Palace Resort Udaipur; Coastal Boutique Retreat Goa",
    education: "IHM Pusa, New Delhi — B.Sc. Hospitality & Hotel Administration",
    skills: "Modern Indian Tasting Menus, Kitchen P&L, HACCP, Pre-Opening Brigades",
    preferredJobLocations: "Mumbai, Goa, New Delhi NCR",
    expectedSalary: "₹28 – 34 LPA",
    availability: "WITHIN_30_DAYS",
    noticePeriod: "30 Days",
    jobAlertsEnabled: true,
    cvFileName: "Kabir_Malhotra_CV.pdf",
    cvMimeType: "application/pdf",
    cvFileSizeBytes: 245120,
    cvStoragePath: `${ALICE_UID}/cv_1795637233.pdf`,
    cvUploadedAtMillis: Date.now() - 2000,
    createdAt: ts,
    updatedAt: ts,
  };

  await assertSucceeds(candidateRef.set(validCandidate));
  await assertSucceeds(candidateRef.get());

  // Update job alerts & CV metadata cleanly
  await assertSucceeds(
    candidateRef.update({
      jobAlertsEnabled: false,
      expectedSalary: "₹30 – 36 LPA",
      updatedAt: new Date(Date.now() - 1000),
    })
  );

  // Reject any public HTTP download URL in cvStoragePath
  await assertFails(
    aliceDb.collection("users").doc(ALICE_UID).collection("candidateProfiles").doc("bad_public_cv").set({
      ...validCandidate,
      cvStoragePath: "https://example.supabase.co/storage/v1/object/public/candidate-cvs/leaked.pdf",
    })
  );

  // Cross-user isolation: Bob cannot read or modify Alice's candidate profile or CV metadata
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("candidateProfiles").doc("primary").get()
  );

  // JobNotification: owner create, mark read, cross-user denied
  const notifRef = aliceDb.collection("users").doc(ALICE_UID).collection("jobNotifications").doc("notif_role_gm_udaipur");
  await assertSucceeds(
    notifRef.set({
      userId: ALICE_UID,
      roleId: "role_exec_chef_mumbai",
      title: "New Culinary Mandate Match",
      message: "Executive Chef — Modern Indian opened in Mumbai, Maharashtra.",
      department: "CULINARY",
      location: "Mumbai, Maharashtra",
      isRead: false,
      createdAt: ts,
    })
  );
  await assertSucceeds(notifRef.update({ isRead: true }));
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).collection("jobNotifications").get()
  );
});

const OWNER_UID = "owner_super_uid";
const RECRUITER_UID = "recruiter_admin_uid";
const CONTENT_UID = "content_admin_uid";
const CONSULTANT_UID = "consultant_admin_uid";
const REPORTING_UID = "reporting_admin_uid";

async function seedAdminUser(uid, email, role, active = true) {
  const ts = new Date(Date.now() - 5000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    await ctx.firestore().collection("adminUsers").doc(uid).set({
      uid,
      email,
      displayName: role,
      role,
      active,
      createdAt: ts,
      updatedAt: ts,
    });
  });
}

test("Admin RBAC Bootstrap & Invite Verification: prevents spoofing and self-escalation", async () => {
  const ts = new Date(Date.now() - 4000);
  const ownerCtx = testEnv.authenticatedContext(OWNER_UID, {
    email: "sumiuraon28@gmail.com",
    email_verified: true,
  }).firestore();
  const spoofedOwnerCtx = testEnv.authenticatedContext("spoof_uid", {
    email: "sumiuraon28@gmail.com",
    email_verified: false,
  }).firestore();
  const aliceDb = testEnv.authenticatedContext(ALICE_UID, {
    email: "alice@guest.in",
    email_verified: true,
  }).firestore();

  // Unverified email spoof must fail
  await assertFails(
    spoofedOwnerCtx.collection("adminUsers").doc("spoof_uid").set({
      uid: "spoof_uid",
      email: "sumiuraon28@gmail.com",
      displayName: "Spoof",
      role: "SUPER_ADMIN",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ordinary client cannot self-register as admin
  await assertFails(
    aliceDb.collection("adminUsers").doc(ALICE_UID).set({
      uid: ALICE_UID,
      email: "alice@guest.in",
      displayName: "Alice",
      role: "SUPER_ADMIN",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Verified owner email bootstraps SUPER_ADMIN cleanly
  await assertSucceeds(
    ownerCtx.collection("adminUsers").doc(OWNER_UID).set({
      uid: OWNER_UID,
      email: "sumiuraon28@gmail.com",
      displayName: "Founder Admin",
      role: "SUPER_ADMIN",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // SUPER_ADMIN creates an invite for Recruitment Admin
  await assertSucceeds(
    ownerCtx.collection("adminInvites").doc("recruiter@milahospitality.in").set({
      email: "recruiter@milahospitality.in",
      displayName: "Talent Lead",
      role: "RECRUITMENT_ADMIN",
      active: true,
      createdByUid: OWNER_UID,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  const invitedRecruiterDb = testEnv.authenticatedContext(RECRUITER_UID, {
    email: "recruiter@milahospitality.in",
    email_verified: true,
  }).firestore();

  // Invited user CANNOT claim SUPER_ADMIN when invited as RECRUITMENT_ADMIN
  await assertFails(
    invitedRecruiterDb.collection("adminUsers").doc(RECRUITER_UID).set({
      uid: RECRUITER_UID,
      email: "recruiter@milahospitality.in",
      displayName: "Talent Lead",
      role: "SUPER_ADMIN",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Invited user CAN claim exact invited role RECRUITMENT_ADMIN
  await assertSucceeds(
    invitedRecruiterDb.collection("adminUsers").doc(RECRUITER_UID).set({
      uid: RECRUITER_UID,
      email: "recruiter@milahospitality.in",
      displayName: "Talent Lead",
      role: "RECRUITMENT_ADMIN",
      active: true,
      createdAt: ts,
      updatedAt: ts,
    })
  );

  // Ordinary client cannot read adminUsers or adminSettings
  await assertFails(aliceDb.collection("adminUsers").get());
  await assertFails(aliceDb.collection("adminSettings").get());
  await assertFails(aliceDb.collection("activityLogs").get());
});

test("RBAC Domain Matrix: Recruitment, Content, Consultant (assigned only), and Reporting (read-only)", async () => {
  await seedAdminUser(OWNER_UID, "sumiuraon28@gmail.com", "SUPER_ADMIN");
  await seedAdminUser(RECRUITER_UID, "recruiter@mila.in", "RECRUITMENT_ADMIN");
  await seedAdminUser(CONTENT_UID, "content@mila.in", "CONTENT_ADMIN");
  await seedAdminUser(CONSULTANT_UID, "consultant@mila.in", "CONSULTANT");
  await seedAdminUser(REPORTING_UID, "reports@mila.in", "REPORTING_ADMIN");

  const ts = new Date(Date.now() - 4000);
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const db = ctx.firestore();
    await db.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_assigned").set(
      buildInquiryPayload(ALICE_UID, {
        assignedConsultantUid: CONSULTANT_UID,
        assignedConsultantEmail: "consultant@mila.in",
      })
    );
    await db.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_unassigned").set(
      buildInquiryPayload(ALICE_UID)
    );
    await db.collection("users").doc(ALICE_UID).collection("jobApplications").doc("app_1").set(
      buildApplicationPayload(ALICE_UID)
    );
    await db.collection("adminSettings").doc("platform").set({
      maintenanceMode: false,
      allowClientRegistrations: true,
      allowCandidateRegistrations: true,
      requireCvForApplications: false,
      defaultEnquirySlaHours: 24,
      supportEmail: "advisory@milahospitality.in",
      officeRegions: "Mumbai • New Delhi NCR • Bengaluru • Rajasthan & Goa",
      updatedAt: ts,
    });
  });

  const recruiterDb = testEnv.authenticatedContext(RECRUITER_UID).firestore();
  const contentDb = testEnv.authenticatedContext(CONTENT_UID).firestore();
  const consultantDb = testEnv.authenticatedContext(CONSULTANT_UID).firestore();
  const reportingDb = testEnv.authenticatedContext(REPORTING_UID).firestore();

  // 1) RECRUITMENT_ADMIN: allowed vacancies, applications, notifications; denied CMS content & adminSettings
  await assertSucceeds(
    recruiterDb.collection("jobVacancies").doc("role_gm_test").set({
      title: "Pre-Opening General Manager",
      propertyTier: "Heritage Resort",
      location: "Udaipur, Rajasthan",
      cityRegion: "Udaipur & Rajasthan",
      compensation: "Executive Package",
      experienceRequired: "14+ Years",
      minExperienceYears: 14,
      category: "PRE_OPENING",
      summary: "Lead pre-opening critical path across all departments.",
      keyMandates: ["Critical path governance"],
      desiredSkills: ["Luxury P&L"],
      urgent: true,
      published: true,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    recruiterDb.collection("users").doc(ALICE_UID).collection("jobApplications").doc("app_1").update({
      status: "SHORTLISTED",
      updatedAt: new Date(Date.now() - 500),
    })
  );
  await assertSucceeds(recruiterDb.collectionGroup("jobApplications").get());
  await assertFails(contentDb.collectionGroup("jobApplications").get());
  await assertFails(
    recruiterDb.collection("portfolioItems").doc("case_test").set({
      propertyName: "Blocked",
      location: "Mumbai",
      category: "Advisory",
      filterGroup: "Pre-Opening",
      scale: "90 Keys",
      summary: "Blocked write from recruitment admin.",
      outcomes: ["Outcome"],
      imageKey: "palace_resort",
      sortOrder: 1,
      published: true,
      updatedAt: ts,
    })
  );

  // 2) CONTENT_ADMIN: allowed portfolio, testimonials, team, siteContent, serviceCategories; denied candidate/client PII
  await assertSucceeds(
    contentDb.collection("portfolioItems").doc("case_test").set({
      propertyName: "Palace Courtyard Launch",
      location: "Jaipur, Rajasthan",
      category: "Pre-Opening Advisory",
      filterGroup: "Pre-Opening",
      scale: "85 Keys",
      summary: "Turnkey heritage palace opening advisory.",
      outcomes: ["180-day opening delivered"],
      imageKey: "palace_resort",
      sortOrder: 1,
      published: true,
      updatedAt: ts,
    })
  );
  await assertSucceeds(
    contentDb.collection("testimonials").doc("test_item_1").set({
      quote: "Disciplined pre-opening advisory and training.",
      attributionRole: "Managing Partner",
      engagementContext: "Luxury Resort • Goa",
      sortOrder: 1,
      published: true,
      updatedAt: ts,
    })
  );
  await assertFails(contentDb.collectionGroup("candidateProfiles").get());

  // 3) CONSULTANT: can query & update ONLY assigned inquiries; denied unassigned get or unfiltered collectionGroup list
  await assertSucceeds(
    consultantDb.collectionGroup("consultationInquiries").where("assignedConsultantUid", "==", CONSULTANT_UID).get()
  );
  await assertFails(
    consultantDb.collectionGroup("consultationInquiries").get()
  );
  await assertFails(
    consultantDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_unassigned").get()
  );
  await assertSucceeds(
    consultantDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_assigned").update({
      status: "UNDER_REVIEW",
      consultantNotes: "Scheduled discovery call with owner.",
      updatedAt: new Date(Date.now() - 400),
    })
  );
  await assertFails(
    consultantDb.collection("users").doc(ALICE_UID).collection("consultationInquiries").doc("inq_unassigned").update({
      status: "UNDER_REVIEW",
      updatedAt: new Date(Date.now() - 400),
    })
  );

  // 4) REPORTING_ADMIN: strictly READ-ONLY across analytics collections, denied all writes
  await assertSucceeds(reportingDb.collection("adminSettings").doc("platform").get());
  await assertSucceeds(reportingDb.collectionGroup("consultationInquiries").get());
  await assertSucceeds(reportingDb.collectionGroup("jobApplications").get());
  await assertSucceeds(reportingDb.collection("activityLogs").get());
  await assertFails(
    reportingDb.collection("activityLogs").doc("log_forbidden").set({
      actorUid: REPORTING_UID,
      actorEmail: "reports@mila.in",
      actorRole: "REPORTING_ADMIN",
      module: "REPORTS",
      action: "CREATE",
      targetId: "x",
      summary: "Should be denied",
      createdAt: ts,
    })
  );
  await assertFails(
    reportingDb.collection("users").doc(ALICE_UID).collection("jobApplications").doc("app_1").update({
      status: "HIRED",
      updatedAt: new Date(Date.now() - 300),
    })
  );
});

