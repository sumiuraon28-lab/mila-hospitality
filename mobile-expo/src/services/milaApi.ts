import { decode } from 'base64-arraybuffer';
import * as FileSystem from 'expo-file-system';
import { Platform } from 'react-native';
import {
  FALLBACK_SERVICES,
  FALLBACK_TESTIMONIALS,
  FALLBACK_VACANCIES,
} from '../constants/brandAssets';
import { assertSupabaseReady, isSupabaseConfigured, supabase } from '../lib/supabase';
import {
  CandidateNotification,
  CandidatePreferences,
  CandidateProfile,
  ClientProfile,
  ConsultancyService,
  JobApplication,
  JobVacancy,
  SavedJob,
  ServiceEnquiry,
  Testimonial,
} from '../types/mila';

const CV_BUCKET = 'candidate-cvs';
const ALLOWED_CV_MIMES = new Set([
  'application/pdf',
  'application/msword',
  'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
]);
const MAX_CV_BYTES = 10 * 1024 * 1024; // 10 MB — matches Supabase migration 0003

function sanitizeFilename(name: string): string {
  return name.replace(/[^a-zA-Z0-9._-]/g, '_').slice(0, 80);
}

// ============================================================================
// PUBLIC CMS READS (published services, testimonials, job vacancies)
// ============================================================================

export async function fetchPublishedServices(): Promise<ConsultancyService[]> {
  if (!isSupabaseConfigured) return FALLBACK_SERVICES;
  const { data, error } = await supabase
    .from('services')
    .select('*')
    .eq('published', true)
    .order('sort_order', { ascending: true });

  if (error || !data || data.length === 0) {
    return FALLBACK_SERVICES;
  }
  return data as ConsultancyService[];
}

export async function fetchPublishedTestimonials(): Promise<Testimonial[]> {
  if (!isSupabaseConfigured) return FALLBACK_TESTIMONIALS;
  const { data, error } = await supabase
    .from('testimonials')
    .select('*')
    .eq('published', true)
    .order('sort_order', { ascending: true });

  if (error || !data || data.length === 0) {
    return FALLBACK_TESTIMONIALS;
  }
  return data as Testimonial[];
}

export async function fetchPublishedVacancies(): Promise<JobVacancy[]> {
  if (!isSupabaseConfigured) return FALLBACK_VACANCIES;
  const { data, error } = await supabase
    .from('job_vacancies')
    .select('*')
    .eq('published', true)
    .order('is_urgent', { ascending: false })
    .order('created_at', { ascending: false });

  if (error || !data || data.length === 0) {
    return FALLBACK_VACANCIES;
  }
  return data as JobVacancy[];
}

// ============================================================================
// AUTHENTICATION (Shared Supabase Auth — strict role separation via tables)
// ============================================================================

export async function registerClientAccount(input: {
  fullName: string;
  email: string;
  mobileNumber: string;
  companyName: string;
  designation: string;
  city: string;
  businessCategory: string;
  password: string;
}): Promise<{ userId: string; requiresEmailConfirmation: boolean }> {
  assertSupabaseReady();
  const cleanEmail = input.email.trim().toLowerCase();

  const { data, error } = await supabase.auth.signUp({
    email: cleanEmail,
    password: input.password,
    options: {
      data: {
        account_role: 'client',
        full_name: input.fullName.trim(),
        mobile_number: input.mobileNumber.trim(),
      },
    },
  });

  if (error) throw new Error(error.message);
  const userId = data.user?.id;
  if (!userId) throw new Error('Account creation failed. Please try again.');

  const requiresEmailConfirmation = !data.session;

  if (data.session) {
    await upsertClientProfile({
      user_id: userId,
      full_name: input.fullName.trim(),
      email: cleanEmail,
      mobile_number: input.mobileNumber.trim(),
      company_name: input.companyName.trim(),
      designation: input.designation.trim(),
      city: input.city.trim(),
      business_category: input.businessCategory,
    });
  }

  return { userId, requiresEmailConfirmation };
}

export async function registerCandidateAccount(input: {
  fullName: string;
  email: string;
  mobileNumber: string;
  location: string;
  currentDesignation: string;
  department: string;
  totalExperienceYears: number;
  currentEmployer: string;
  previousEmployers: string;
  education: string;
  skills: string;
  preferredJobLocations: string;
  expectedSalary: string;
  availability: string;
  noticePeriod: string;
  password: string;
}): Promise<{ userId: string; requiresEmailConfirmation: boolean }> {
  assertSupabaseReady();
  const cleanEmail = input.email.trim().toLowerCase();

  const { data, error } = await supabase.auth.signUp({
    email: cleanEmail,
    password: input.password,
    options: {
      data: {
        account_role: 'job_seeker',
        full_name: input.fullName.trim(),
        mobile_number: input.mobileNumber.trim(),
      },
    },
  });

  if (error) throw new Error(error.message);
  const userId = data.user?.id;
  if (!userId) throw new Error('Candidate registration failed. Please try again.');

  const requiresEmailConfirmation = !data.session;

  if (data.session) {
    const savedProfile = await upsertCandidateProfile({
      user_id: userId,
      full_name: input.fullName.trim(),
      email: cleanEmail,
      mobile_number: input.mobileNumber.trim(),
      location: input.location.trim(),
      current_designation: input.currentDesignation.trim(),
      department: input.department,
      total_experience_years: input.totalExperienceYears,
      current_employer: input.currentEmployer.trim(),
      previous_employers: input.previousEmployers.trim(),
      education: input.education.trim(),
      skills: input.skills.trim(),
      preferred_job_locations: input.preferredJobLocations.trim() || input.location.trim(),
      expected_salary: input.expectedSalary.trim(),
      availability: input.availability,
      notice_period: input.noticePeriod.trim() || '30 Days',
      job_alerts_enabled: true,
    });

    await upsertCandidatePreferences({
      user_id: userId,
      candidate_profile_id: savedProfile.id ?? null,
      job_alerts_enabled: true,
      preferred_locations: splitCsv(input.preferredJobLocations || input.location),
      preferred_departments: [input.department],
      preferred_skills: splitCsv(input.skills),
      min_expected_salary: input.expectedSalary.trim(),
      availability: input.availability,
      notice_period: input.noticePeriod.trim() || '30 Days',
      push_notifications_enabled: true,
      in_app_notifications_enabled: true,
      email_notifications_enabled: false,
      application_status_alerts_enabled: true,
    });
  }

  return { userId, requiresEmailConfirmation };
}

export async function signInWithEmailPassword(email: string, password: string) {
  assertSupabaseReady();
  const { data, error } = await supabase.auth.signInWithPassword({
    email: email.trim().toLowerCase(),
    password,
  });
  if (error) throw new Error(error.message);
  return data;
}

export async function requestPasswordReset(email: string): Promise<void> {
  assertSupabaseReady();
  const { error } = await supabase.auth.resetPasswordForEmail(email.trim().toLowerCase());
  if (error) throw new Error(error.message);
}

export async function signOutAccount(): Promise<void> {
  if (!isSupabaseConfigured) return;
  const { error } = await supabase.auth.signOut();
  if (error) throw new Error(error.message);
}

// ============================================================================
// CLIENT PROFILE & SERVICE ENQUIRIES (RLS isolated to user_id = auth.uid())
// ============================================================================

export async function fetchMyClientProfile(userId: string): Promise<ClientProfile | null> {
  if (!isSupabaseConfigured) return null;
  const { data, error } = await supabase
    .from('client_profiles')
    .select('*')
    .eq('user_id', userId)
    .maybeSingle();

  if (error) throw new Error(error.message);
  return (data as ClientProfile | null) ?? null;
}

export async function upsertClientProfile(profile: ClientProfile): Promise<ClientProfile> {
  assertSupabaseReady();
  const { data, error } = await supabase
    .from('client_profiles')
    .upsert(profile, { onConflict: 'user_id' })
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as ClientProfile;
}

export async function fetchMyServiceEnquiries(userId: string): Promise<ServiceEnquiry[]> {
  if (!isSupabaseConfigured) return [];
  const { data, error } = await supabase
    .from('service_enquiries')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) throw new Error(error.message);
  return (data as ServiceEnquiry[]) ?? [];
}

export async function submitServiceEnquiry(input: {
  userId: string;
  clientProfileId?: string | null;
  serviceId?: string | null;
  serviceType: string;
  propertyName: string;
  city: string;
  scaleSummary: string;
  contactName: string;
  contactEmail: string;
  contactPhone: string;
  briefNotes?: string;
}): Promise<ServiceEnquiry> {
  assertSupabaseReady();
  const { data, error } = await supabase
    .from('service_enquiries')
    .insert({
      user_id: input.userId,
      client_profile_id: input.clientProfileId ?? null,
      service_id: isUuid(input.serviceId) ? input.serviceId : null,
      service_type: input.serviceType,
      property_name: input.propertyName.trim(),
      city: input.city.trim(),
      scale_summary: input.scaleSummary.trim(),
      contact_name: input.contactName.trim(),
      contact_email: input.contactEmail.trim().toLowerCase(),
      contact_phone: input.contactPhone.trim(),
      brief_notes: input.briefNotes?.trim() || null,
      status: 'SUBMITTED',
    })
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as ServiceEnquiry;
}

// ============================================================================
// CANDIDATE PROFILE, PRIVATE CV STORAGE & PREFERENCES
// ============================================================================

export async function fetchMyCandidateProfile(userId: string): Promise<CandidateProfile | null> {
  if (!isSupabaseConfigured) return null;
  const { data, error } = await supabase
    .from('candidate_profiles')
    .select('*')
    .eq('user_id', userId)
    .maybeSingle();

  if (error) throw new Error(error.message);
  return (data as CandidateProfile | null) ?? null;
}

export async function upsertCandidateProfile(profile: CandidateProfile): Promise<CandidateProfile> {
  assertSupabaseReady();
  const { data, error } = await supabase
    .from('candidate_profiles')
    .upsert(profile, { onConflict: 'user_id' })
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as CandidateProfile;
}

export async function fetchMyCandidatePreferences(
  userId: string
): Promise<CandidatePreferences | null> {
  if (!isSupabaseConfigured) return null;
  const { data, error } = await supabase
    .from('candidate_preferences')
    .select('*')
    .eq('user_id', userId)
    .maybeSingle();

  if (error) throw new Error(error.message);
  return (data as CandidatePreferences | null) ?? null;
}

export async function upsertCandidatePreferences(
  prefs: CandidatePreferences
): Promise<CandidatePreferences> {
  assertSupabaseReady();
  const { data, error } = await supabase
    .from('candidate_preferences')
    .upsert(
      {
        user_id: prefs.user_id,
        candidate_profile_id: prefs.candidate_profile_id ?? null,
        job_alerts_enabled: prefs.job_alerts_enabled,
        preferred_locations: prefs.preferred_locations,
        preferred_departments: prefs.preferred_departments,
        preferred_skills: prefs.preferred_skills,
        min_expected_salary: prefs.min_expected_salary,
        availability: prefs.availability,
        notice_period: prefs.notice_period,
        push_notifications_enabled: prefs.push_notifications_enabled,
        in_app_notifications_enabled: prefs.in_app_notifications_enabled,
        email_notifications_enabled: prefs.email_notifications_enabled,
        application_status_alerts_enabled: prefs.application_status_alerts_enabled,
        push_alerts: prefs.push_notifications_enabled,
        email_alerts: prefs.email_notifications_enabled,
      },
      { onConflict: 'user_id' }
    )
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as CandidatePreferences;
}

/**
 * Uploads or replaces a candidate's CV inside the private Supabase Storage bucket `candidate-cvs`.
 * Strictly enforces PDF/DOC/DOCX MIME types and 10 MB max size. Never generates public URLs.
 */
export async function uploadPrivateCandidateCv(params: {
  userId: string;
  fileUri: string;
  fileName: string;
  mimeType: string;
  fileSize?: number | null;
  previousStoragePath?: string | null;
}): Promise<{
  storagePath: string;
  fileName: string;
  mimeType: string;
  fileSizeBytes: number;
  uploadedAt: string;
}> {
  assertSupabaseReady();

  const resolvedMime = inferCvMime(params.fileName, params.mimeType);
  if (!ALLOWED_CV_MIMES.has(resolvedMime)) {
    throw new Error('Only PDF, DOC, and DOCX files are accepted for CV uploads.');
  }

  let payloadBuffer: ArrayBuffer;
  let actualSize = params.fileSize && params.fileSize > 0 ? params.fileSize : 0;

  if (Platform.OS === 'web') {
    const response = await fetch(params.fileUri);
    payloadBuffer = await response.arrayBuffer();
    if (actualSize <= 0) {
      actualSize = payloadBuffer.byteLength;
    }
    if (actualSize <= 0 || actualSize > MAX_CV_BYTES) {
      throw new Error('CV file size must be between 1 byte and 10 MB.');
    }
  } else {
    const info = await FileSystem.getInfoAsync(params.fileUri);
    actualSize =
      actualSize > 0
        ? actualSize
        : info.exists && 'size' in info && typeof info.size === 'number'
          ? info.size
          : 0;

    if (actualSize <= 0 || actualSize > MAX_CV_BYTES) {
      throw new Error('CV file size must be between 1 byte and 10 MB.');
    }

    const base64Content = await FileSystem.readAsStringAsync(params.fileUri, {
      encoding: FileSystem.EncodingType.Base64,
    });
    payloadBuffer = decode(base64Content);
  }

  const cleanFileName = sanitizeFilename(params.fileName);
  const storagePath = `${params.userId}/cv_${Date.now()}_${cleanFileName}`;

  const { error: uploadError } = await supabase.storage
    .from(CV_BUCKET)
    .upload(storagePath, payloadBuffer, {
      contentType: resolvedMime,
      upsert: false,
    });

  if (uploadError) throw new Error(uploadError.message);

  // Remove superseded CV object from private storage
  if (params.previousStoragePath && params.previousStoragePath.startsWith(`${params.userId}/`)) {
    await supabase.storage.from(CV_BUCKET).remove([params.previousStoragePath]);
  }

  const uploadedAt = new Date().toISOString();
  const { error: updateError } = await supabase
    .from('candidate_profiles')
    .update({
      cv_file_name: cleanFileName,
      cv_mime_type: resolvedMime,
      cv_file_size_bytes: actualSize,
      cv_storage_path: storagePath,
      cv_uploaded_at: uploadedAt,
    })
    .eq('user_id', params.userId);

  if (updateError) throw new Error(updateError.message);

  return {
    storagePath,
    fileName: cleanFileName,
    mimeType: resolvedMime,
    fileSizeBytes: actualSize,
    uploadedAt,
  };
}

export async function deletePrivateCandidateCv(
  userId: string,
  storagePath?: string | null
): Promise<void> {
  assertSupabaseReady();

  if (storagePath && storagePath.startsWith(`${userId}/`)) {
    const { error: removeError } = await supabase.storage.from(CV_BUCKET).remove([storagePath]);
    if (removeError) throw new Error(removeError.message);
  }

  const { error } = await supabase
    .from('candidate_profiles')
    .update({
      cv_file_name: null,
      cv_mime_type: null,
      cv_file_size_bytes: null,
      cv_storage_path: null,
      cv_uploaded_at: null,
    })
    .eq('user_id', userId);

  if (error) throw new Error(error.message);
}

/**
 * Generates a short-lived (120 second) private signed URL strictly for the owning candidate.
 */
export async function createPrivateCvSignedUrl(storagePath: string): Promise<string> {
  assertSupabaseReady();
  const { data, error } = await supabase.storage
    .from(CV_BUCKET)
    .createSignedUrl(storagePath, 120);

  if (error || !data?.signedUrl) {
    throw new Error(error?.message ?? 'Unable to generate secure CV link.');
  }
  return data.signedUrl;
}

// ============================================================================
// JOB APPLICATIONS, SAVED JOBS & CANDIDATE NOTIFICATIONS
// ============================================================================

export async function fetchMyApplications(userId: string): Promise<JobApplication[]> {
  if (!isSupabaseConfigured) return [];
  const { data, error } = await supabase
    .from('job_applications')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) throw new Error(error.message);
  return (data as JobApplication[]) ?? [];
}

export async function submitJobApplication(input: {
  userId: string;
  candidateProfileId?: string | null;
  vacancy: JobVacancy;
  candidateName: string;
  candidateEmail: string;
  candidatePhone: string;
  yearsExperience: number;
  currentProperty: string;
  coverNote?: string;
  cvFileName?: string | null;
  cvStoragePath?: string | null;
}): Promise<JobApplication> {
  assertSupabaseReady();
  const { data, error } = await supabase
    .from('job_applications')
    .insert({
      user_id: input.userId,
      candidate_profile_id: input.candidateProfileId ?? null,
      vacancy_id: isUuid(input.vacancy.id) ? input.vacancy.id : null,
      role_id: input.vacancy.slug || input.vacancy.id,
      role_title: input.vacancy.title,
      location: input.vacancy.location,
      candidate_name: input.candidateName.trim(),
      candidate_email: input.candidateEmail.trim().toLowerCase(),
      candidate_phone: input.candidatePhone.trim(),
      years_experience: input.yearsExperience,
      current_property: input.currentProperty.trim() || 'Independent Hospitality Professional',
      cover_note: input.coverNote?.trim() || null,
      cv_file_name: input.cvFileName ?? null,
      cv_storage_path: input.cvStoragePath ?? null,
      status: 'APPLIED',
    })
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as JobApplication;
}

export async function withdrawJobApplication(applicationId: string): Promise<void> {
  assertSupabaseReady();
  const { error } = await supabase
    .from('job_applications')
    .update({ status: 'WITHDRAWN' })
    .eq('id', applicationId);

  if (error) throw new Error(error.message);
}

export async function fetchMySavedJobs(userId: string): Promise<SavedJob[]> {
  if (!isSupabaseConfigured) return [];
  const { data, error } = await supabase
    .from('saved_jobs')
    .select('*')
    .eq('user_id', userId)
    .order('saved_at', { ascending: false });

  if (error) throw new Error(error.message);
  return (data as SavedJob[]) ?? [];
}

export async function saveJobToShortlist(userId: string, vacancy: JobVacancy): Promise<SavedJob> {
  assertSupabaseReady();
  const roleKey = vacancy.slug || vacancy.id;
  const { data, error } = await supabase
    .from('saved_jobs')
    .upsert(
      {
        user_id: userId,
        vacancy_id: isUuid(vacancy.id) ? vacancy.id : null,
        role_id: roleKey,
        role_title: vacancy.title,
        location: vacancy.location,
        compensation: vacancy.compensation,
      },
      { onConflict: 'user_id,role_id' }
    )
    .select('*')
    .single();

  if (error) throw new Error(error.message);
  return data as SavedJob;
}

export async function removeSavedJob(userId: string, roleId: string): Promise<void> {
  assertSupabaseReady();
  const { error } = await supabase
    .from('saved_jobs')
    .delete()
    .eq('user_id', userId)
    .eq('role_id', roleId);

  if (error) throw new Error(error.message);
}

export async function fetchMyNotifications(userId: string): Promise<CandidateNotification[]> {
  if (!isSupabaseConfigured) return [];
  const { data, error } = await supabase
    .from('notifications')
    .select('*')
    .eq('user_id', userId)
    .order('created_at', { ascending: false });

  if (error) throw new Error(error.message);
  return (data as CandidateNotification[]) ?? [];
}

export async function markNotificationAsRead(notificationId: string): Promise<void> {
  assertSupabaseReady();
  const { error } = await supabase
    .from('notifications')
    .update({ is_read: true })
    .eq('id', notificationId);

  if (error) throw new Error(error.message);
}

// ============================================================================
// HELPERS
// ============================================================================

export function splitCsv(raw: string): string[] {
  return raw
    .split(',')
    .map((item) => item.trim())
    .filter((item) => item.length > 0);
}

function isUuid(value?: string | null): boolean {
  if (!value) return false;
  return /^[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}$/i.test(value);
}

function inferCvMime(fileName: string, mimeType?: string | null): string {
  if (mimeType && ALLOWED_CV_MIMES.has(mimeType)) {
    return mimeType;
  }
  const lower = fileName.toLowerCase();
  if (lower.endsWith('.pdf')) return 'application/pdf';
  if (lower.endsWith('.doc')) return 'application/msword';
  if (lower.endsWith('.docx')) {
    return 'application/vnd.openxmlformats-officedocument.wordprocessingml.document';
  }
  return mimeType ?? '';
}
