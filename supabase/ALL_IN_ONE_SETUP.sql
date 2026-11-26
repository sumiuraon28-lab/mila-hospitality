-- MILA HOSPITALITY CONSULTANT — COMPLETE ONE-CLICK SUPABASE SETUP
-- Paste this entire file into Supabase SQL Editor and click Run.


-- ==================== supabase/migrations/202611250001_mila_core_schema.sql ====================

-- MILA HOSPITALITY CONSULTANT — Core PostgreSQL Schema Migration
-- Covers: user_profiles, client_profiles, candidate_profiles, candidate_preferences,
-- service_categories, services, service_enquiries, job_vacancies, job_applications,
-- saved_jobs, notifications, website_content, portfolio_projects, testimonials,
-- team_members, admin_users, admin_invites, platform_settings, activity_logs.

CREATE EXTENSION IF NOT EXISTS pgcrypto;

-- Shared updated_at trigger helper
CREATE OR REPLACE FUNCTION public.set_updated_at()
RETURNS trigger
LANGUAGE plpgsql
AS $$
BEGIN
  NEW.updated_at = now();
  RETURN NEW;
END;
$$;

-- 1. User Profiles (1:1 mirror of auth.users with safe public/self profile columns)
CREATE TABLE IF NOT EXISTS public.user_profiles (
  id uuid PRIMARY KEY REFERENCES auth.users(id) ON DELETE CASCADE,
  email text NOT NULL UNIQUE,
  full_name text NOT NULL DEFAULT '',
  mobile_number text NOT NULL DEFAULT '',
  account_type text NOT NULL DEFAULT 'CLIENT'
    CHECK (account_type IN ('CLIENT', 'CANDIDATE', 'ADMIN')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_user_profiles_account_type
  ON public.user_profiles (account_type);
CREATE INDEX IF NOT EXISTS idx_user_profiles_email_lower
  ON public.user_profiles (lower(email));

-- 2. Admin Users (server-verified RBAC roster — never self-writable by ordinary users)
CREATE TABLE IF NOT EXISTS public.admin_users (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL UNIQUE REFERENCES auth.users(id) ON DELETE CASCADE,
  email text NOT NULL UNIQUE,
  display_name text NOT NULL,
  role text NOT NULL
    CHECK (role IN ('SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'CONTENT_ADMIN', 'CONSULTANT', 'REPORTING_ADMIN')),
  active boolean NOT NULL DEFAULT true,
  assigned_department text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_users_role_active
  ON public.admin_users (role, active);

-- 3. Admin Invites (pre-approved staff emails that claim their role via verify_admin_session RPC)
CREATE TABLE IF NOT EXISTS public.admin_invites (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  email text NOT NULL UNIQUE,
  display_name text NOT NULL,
  role text NOT NULL
    CHECK (role IN ('SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'CONTENT_ADMIN', 'CONSULTANT', 'REPORTING_ADMIN')),
  active boolean NOT NULL DEFAULT true,
  created_by uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_admin_invites_email_lower
  ON public.admin_invites (lower(email)) WHERE active = true;

-- 4. Client Profiles (strictly isolated per client owner)
CREATE TABLE IF NOT EXISTS public.client_profiles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL UNIQUE REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  full_name text NOT NULL CHECK (char_length(trim(full_name)) >= 2),
  email text NOT NULL,
  mobile_number text NOT NULL CHECK (char_length(trim(mobile_number)) >= 7),
  company_name text NOT NULL CHECK (char_length(trim(company_name)) >= 2),
  designation text NOT NULL CHECK (char_length(trim(designation)) >= 2),
  city text NOT NULL CHECK (char_length(trim(city)) >= 2),
  business_category text NOT NULL DEFAULT 'LUXURY_HOTEL_RESORT',
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_client_profiles_city
  ON public.client_profiles (city);
CREATE INDEX IF NOT EXISTS idx_client_profiles_business_category
  ON public.client_profiles (business_category);

-- 5. Candidate Profiles (15 required MILA Talent fields + private CV vault pointer)
CREATE TABLE IF NOT EXISTS public.candidate_profiles (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL UNIQUE REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  full_name text NOT NULL CHECK (char_length(trim(full_name)) >= 2),
  email text NOT NULL,
  mobile_number text NOT NULL CHECK (char_length(trim(mobile_number)) >= 7),
  location text NOT NULL DEFAULT '',
  current_designation text NOT NULL DEFAULT '',
  department text NOT NULL DEFAULT 'EXECUTIVE',
  total_experience_years integer NOT NULL DEFAULT 0
    CHECK (total_experience_years >= 0 AND total_experience_years <= 60),
  current_employer text NOT NULL DEFAULT '',
  previous_employers text NOT NULL DEFAULT '',
  education text NOT NULL DEFAULT '',
  skills text NOT NULL DEFAULT '',
  preferred_job_locations text NOT NULL DEFAULT '',
  expected_salary text NOT NULL DEFAULT '',
  availability text NOT NULL DEFAULT 'WITHIN_30_DAYS',
  notice_period text NOT NULL DEFAULT '30 Days',
  job_alerts_enabled boolean NOT NULL DEFAULT true,
  cv_file_name text,
  cv_mime_type text
    CHECK (
      cv_mime_type IS NULL OR cv_mime_type IN (
        'application/pdf',
        'application/msword',
        'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
      )
    ),
  cv_file_size_bytes bigint
    CHECK (cv_file_size_bytes IS NULL OR (cv_file_size_bytes > 0 AND cv_file_size_bytes <= 10485760)),
  cv_storage_path text
    CHECK (
      cv_storage_path IS NULL OR (
        cv_storage_path NOT ILIKE 'http%' AND
        cv_storage_path NOT ILIKE '%/object/public/%'
      )
    ),
  cv_uploaded_at timestamptz,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_candidate_profiles_department
  ON public.candidate_profiles (department);
CREATE INDEX IF NOT EXISTS idx_candidate_profiles_location
  ON public.candidate_profiles (location);
CREATE INDEX IF NOT EXISTS idx_candidate_profiles_alerts
  ON public.candidate_profiles (job_alerts_enabled) WHERE job_alerts_enabled = true;
CREATE INDEX IF NOT EXISTS idx_candidate_profiles_has_cv
  ON public.candidate_profiles (cv_storage_path) WHERE cv_storage_path IS NOT NULL;

-- 6. Candidate Preferences (alert settings, target locations, departments, compensation & notice)
CREATE TABLE IF NOT EXISTS public.candidate_preferences (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL UNIQUE REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  candidate_profile_id uuid REFERENCES public.candidate_profiles(id) ON DELETE CASCADE,
  job_alerts_enabled boolean NOT NULL DEFAULT true,
  preferred_locations text[] NOT NULL DEFAULT '{}',
  preferred_departments text[] NOT NULL DEFAULT '{}',
  min_expected_salary text NOT NULL DEFAULT '',
  availability text NOT NULL DEFAULT 'WITHIN_30_DAYS',
  notice_period text NOT NULL DEFAULT '30 Days',
  email_alerts boolean NOT NULL DEFAULT true,
  push_alerts boolean NOT NULL DEFAULT true,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_candidate_preferences_locations
  ON public.candidate_preferences USING GIN (preferred_locations);
CREATE INDEX IF NOT EXISTS idx_candidate_preferences_departments
  ON public.candidate_preferences USING GIN (preferred_departments);

-- 7. Service Categories (CMS)
CREATE TABLE IF NOT EXISTS public.service_categories (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  code text NOT NULL UNIQUE,
  sort_order integer NOT NULL DEFAULT 1,
  title text NOT NULL,
  division_name text NOT NULL DEFAULT 'MILA Consultancy',
  summary text NOT NULL DEFAULT '',
  published boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_categories_published_order
  ON public.service_categories (published, sort_order);

-- 8. Services (8 MILA Consultancy Categories — CMS managed)
CREATE TABLE IF NOT EXISTS public.services (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  category_id uuid REFERENCES public.service_categories(id) ON DELETE SET NULL,
  service_type text NOT NULL UNIQUE,
  slug text NOT NULL UNIQUE,
  sort_order integer NOT NULL DEFAULT 1,
  division_name text NOT NULL DEFAULT 'MILA Consultancy',
  title text NOT NULL,
  subtitle text NOT NULL DEFAULT '',
  summary text NOT NULL,
  detailed_description text NOT NULL,
  services_included text[] NOT NULL DEFAULT '{}',
  owner_benefits text[] NOT NULL DEFAULT '{}',
  deliverables text[] NOT NULL DEFAULT '{}',
  typical_timeline text NOT NULL DEFAULT '',
  image_key text NOT NULL DEFAULT 'palace_resort',
  image_url text,
  published boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_services_published_order
  ON public.services (published, sort_order);

-- 9. Service Enquiries (strictly isolated per client + assigned consultant)
CREATE TABLE IF NOT EXISTS public.service_enquiries (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  reference_code text NOT NULL UNIQUE DEFAULT ('MILA-' || upper(substr(replace(gen_random_uuid()::text, '-', ''), 1, 6))),
  user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  client_profile_id uuid REFERENCES public.client_profiles(id) ON DELETE SET NULL,
  service_id uuid REFERENCES public.services(id) ON DELETE SET NULL,
  service_type text NOT NULL,
  property_name text NOT NULL CHECK (char_length(trim(property_name)) >= 2),
  city text NOT NULL CHECK (char_length(trim(city)) >= 2),
  scale_summary text NOT NULL,
  contact_name text NOT NULL,
  contact_email text NOT NULL,
  contact_phone text NOT NULL,
  brief_notes text,
  status text NOT NULL DEFAULT 'SUBMITTED'
    CHECK (status IN ('SUBMITTED', 'UNDER_REVIEW', 'PROPOSAL_SHARED', 'CONVERTED', 'WITHDRAWN')),
  assigned_consultant_id uuid REFERENCES public.admin_users(user_id) ON DELETE SET NULL,
  assigned_consultant_email text,
  consultant_notes text,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_service_enquiries_user_created
  ON public.service_enquiries (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_service_enquiries_consultant
  ON public.service_enquiries (assigned_consultant_id, status);
CREATE INDEX IF NOT EXISTS idx_service_enquiries_status
  ON public.service_enquiries (status);

-- 10. Job Vacancies (MILA Talent open roles)
CREATE TABLE IF NOT EXISTS public.job_vacancies (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  slug text NOT NULL UNIQUE,
  title text NOT NULL,
  property_tier text NOT NULL DEFAULT '',
  location text NOT NULL,
  city_region text NOT NULL,
  compensation text NOT NULL DEFAULT '',
  experience_required text NOT NULL DEFAULT '',
  min_experience_years integer NOT NULL DEFAULT 0 CHECK (min_experience_years >= 0 AND min_experience_years <= 50),
  category text NOT NULL DEFAULT 'PRE_OPENING',
  summary text NOT NULL,
  key_mandates text[] NOT NULL DEFAULT '{}',
  desired_skills text[] NOT NULL DEFAULT '{}',
  is_urgent boolean NOT NULL DEFAULT false,
  published boolean NOT NULL DEFAULT true,
  created_by uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_job_vacancies_published_region
  ON public.job_vacancies (published, city_region, category);
CREATE INDEX IF NOT EXISTS idx_job_vacancies_min_exp
  ON public.job_vacancies (min_experience_years);

-- 11. Job Applications (candidate applications linked to vacancies & private CV key)
CREATE TABLE IF NOT EXISTS public.job_applications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  candidate_profile_id uuid REFERENCES public.candidate_profiles(id) ON DELETE SET NULL,
  vacancy_id uuid REFERENCES public.job_vacancies(id) ON DELETE SET NULL,
  role_id text NOT NULL,
  role_title text NOT NULL,
  location text NOT NULL,
  candidate_name text NOT NULL,
  candidate_email text NOT NULL,
  candidate_phone text NOT NULL,
  years_experience integer NOT NULL DEFAULT 0 CHECK (years_experience >= 0 AND years_experience <= 60),
  current_property text NOT NULL,
  cover_note text,
  cv_file_name text,
  cv_storage_path text
    CHECK (
      cv_storage_path IS NULL OR (
        cv_storage_path NOT ILIKE 'http%' AND
        cv_storage_path NOT ILIKE '%/object/public/%'
      )
    ),
  status text NOT NULL DEFAULT 'APPLIED'
    CHECK (status IN ('APPLIED', 'UNDER_REVIEW', 'SHORTLISTED', 'INTERVIEW_SCHEDULED', 'OFFER_EXTENDED', 'HIRED', 'DECLINED', 'WITHDRAWN')),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_job_applications_user_created
  ON public.job_applications (user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_job_applications_status
  ON public.job_applications (status);
CREATE INDEX IF NOT EXISTS idx_job_applications_vacancy
  ON public.job_applications (vacancy_id);

-- 12. Saved / Shortlisted Jobs
CREATE TABLE IF NOT EXISTS public.saved_jobs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  vacancy_id uuid REFERENCES public.job_vacancies(id) ON DELETE SET NULL,
  role_id text NOT NULL,
  role_title text NOT NULL,
  location text NOT NULL,
  compensation text NOT NULL DEFAULT '',
  saved_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, role_id)
);

CREATE INDEX IF NOT EXISTS idx_saved_jobs_user
  ON public.saved_jobs (user_id, saved_at DESC);

-- 13. Notifications (job alerts & recruitment stage updates)
CREATE TABLE IF NOT EXISTS public.notifications (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  vacancy_id uuid REFERENCES public.job_vacancies(id) ON DELETE SET NULL,
  role_id text NOT NULL DEFAULT '',
  title text NOT NULL,
  message text NOT NULL,
  department text NOT NULL DEFAULT '',
  location text NOT NULL DEFAULT '',
  is_read boolean NOT NULL DEFAULT false,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notifications_user_unread
  ON public.notifications (user_id, is_read, created_at DESC);

-- 14. Website Content (CMS pages)
CREATE TABLE IF NOT EXISTS public.website_content (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  page_key text NOT NULL UNIQUE,
  page_title text NOT NULL,
  eyebrow text NOT NULL DEFAULT '',
  headline text NOT NULL,
  subheadline text NOT NULL DEFAULT '',
  body_copy text NOT NULL DEFAULT '',
  hero_image_key text NOT NULL DEFAULT 'hero_hotel',
  hero_image_url text,
  cta_label text NOT NULL DEFAULT 'Explore Services',
  published boolean NOT NULL DEFAULT true,
  updated_by uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_website_content_published
  ON public.website_content (published, page_key);

-- 15. Portfolio Projects
CREATE TABLE IF NOT EXISTS public.portfolio_projects (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  slug text NOT NULL UNIQUE,
  property_name text NOT NULL,
  location text NOT NULL,
  category text NOT NULL,
  filter_group text NOT NULL DEFAULT 'Pre-Opening',
  scale text NOT NULL DEFAULT '',
  summary text NOT NULL,
  outcomes text[] NOT NULL DEFAULT '{}',
  image_key text NOT NULL DEFAULT 'palace_resort',
  image_url text,
  sort_order integer NOT NULL DEFAULT 1,
  published boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_portfolio_projects_published_order
  ON public.portfolio_projects (published, sort_order);

-- 16. Testimonials
CREATE TABLE IF NOT EXISTS public.testimonials (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  quote text NOT NULL,
  attribution_role text NOT NULL,
  engagement_context text NOT NULL,
  sort_order integer NOT NULL DEFAULT 1,
  published boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 17. Team Members
CREATE TABLE IF NOT EXISTS public.team_members (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  full_name text NOT NULL,
  designation text NOT NULL,
  division_name text NOT NULL DEFAULT 'MILA Consultancy',
  specialty text NOT NULL DEFAULT '',
  years_experience integer NOT NULL DEFAULT 10 CHECK (years_experience >= 0 AND years_experience <= 60),
  credentials text NOT NULL DEFAULT '',
  image_key text NOT NULL DEFAULT 'hero_hotel',
  image_url text,
  sort_order integer NOT NULL DEFAULT 1,
  published boolean NOT NULL DEFAULT true,
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 18. Platform Settings (singleton row id = 'platform')
CREATE TABLE IF NOT EXISTS public.platform_settings (
  id text PRIMARY KEY DEFAULT 'platform' CHECK (id = 'platform'),
  maintenance_mode boolean NOT NULL DEFAULT false,
  allow_client_registrations boolean NOT NULL DEFAULT true,
  allow_candidate_registrations boolean NOT NULL DEFAULT true,
  require_cv_for_applications boolean NOT NULL DEFAULT false,
  default_enquiry_sla_hours integer NOT NULL DEFAULT 24 CHECK (default_enquiry_sla_hours BETWEEN 1 AND 168),
  support_email text NOT NULL DEFAULT 'advisory@milahospitality.in',
  office_regions text NOT NULL DEFAULT 'Mumbai • New Delhi NCR • Bengaluru • Rajasthan & Goa',
  updated_at timestamptz NOT NULL DEFAULT now()
);

-- 19. Activity Audit Logs (append-only)
CREATE TABLE IF NOT EXISTS public.activity_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  actor_uid uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  actor_email text NOT NULL,
  actor_role text NOT NULL,
  module text NOT NULL,
  action text NOT NULL,
  target_id text NOT NULL,
  summary text NOT NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_activity_logs_created_desc
  ON public.activity_logs (created_at DESC);
CREATE INDEX IF NOT EXISTS idx_activity_logs_module
  ON public.activity_logs (module, created_at DESC);

-- Attach updated_at triggers
DO $$
DECLARE
  tbl text;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'user_profiles', 'admin_users', 'admin_invites', 'client_profiles',
    'candidate_profiles', 'candidate_preferences', 'service_categories',
    'services', 'service_enquiries', 'job_vacancies', 'job_applications',
    'website_content', 'portfolio_projects', 'testimonials', 'team_members',
    'platform_settings'
  ] LOOP
    EXECUTE format('DROP TRIGGER IF EXISTS trg_%I_updated_at ON public.%I;', tbl, tbl);
    EXECUTE format(
      'CREATE TRIGGER trg_%I_updated_at BEFORE UPDATE ON public.%I FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();',
      tbl, tbl
    );
  END LOOP;
END;
$$;

-- Automatically create public.user_profiles on Supabase Auth signup
CREATE OR REPLACE FUNCTION public.handle_new_auth_user()
RETURNS trigger
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
  v_role text;
BEGIN
  v_role := CASE
    WHEN lower(coalesce(NEW.raw_user_meta_data ->> 'account_role', '')) = 'job_seeker' THEN 'CANDIDATE'
    ELSE 'CLIENT'
  END;

  INSERT INTO public.user_profiles (id, email, full_name, mobile_number, account_type)
  VALUES (
    NEW.id,
    lower(coalesce(NEW.email, '')),
    coalesce(NEW.raw_user_meta_data ->> 'full_name', ''),
    coalesce(NEW.raw_user_meta_data ->> 'mobile_number', ''),
    v_role
  )
  ON CONFLICT (id) DO UPDATE
    SET email = EXCLUDED.email,
        full_name = CASE WHEN EXCLUDED.full_name <> '' THEN EXCLUDED.full_name ELSE public.user_profiles.full_name END,
        mobile_number = CASE WHEN EXCLUDED.mobile_number <> '' THEN EXCLUDED.mobile_number ELSE public.user_profiles.mobile_number END,
        updated_at = now();

  RETURN NEW;
END;
$$;

DROP TRIGGER IF EXISTS on_auth_user_created ON auth.users;
CREATE TRIGGER on_auth_user_created
  AFTER INSERT ON auth.users
  FOR EACH ROW
  EXECUTE FUNCTION public.handle_new_auth_user();


-- ==================== supabase/migrations/202611250002_mila_rls_and_role_rpc.sql ====================

-- MILA HOSPITALITY CONSULTANT — Row Level Security & Server-Side Role Verification
-- Enforces strict tenant isolation (clients, candidates) and 5-role Admin RBAC.

-- =============================================================================
-- 1. SERVER-SIDE ROLE VERIFICATION FUNCTIONS (SECURITY DEFINER)
-- =============================================================================
-- Never reads client-editable user_metadata; resolves strictly from public.admin_users.

CREATE OR REPLACE FUNCTION public.current_admin_role()
RETURNS text
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, auth
AS $$
  SELECT a.role
  FROM public.admin_users a
  WHERE a.user_id = auth.uid()
    AND a.active = true
  LIMIT 1;
$$;

CREATE OR REPLACE FUNCTION public.is_admin_with_role(allowed_roles text[])
RETURNS boolean
LANGUAGE sql
STABLE
SECURITY DEFINER
SET search_path = public, auth
AS $$
  SELECT EXISTS (
    SELECT 1
    FROM public.admin_users a
    WHERE a.user_id = auth.uid()
      AND a.active = true
      AND a.role = ANY(allowed_roles)
  );
$$;

-- Server-side RPC callable by Android client (`POST /rest/v1/rpc/verify_admin_session`)
-- Atomically verifies active admin_users record or claims a pre-approved admin_invites entry
-- matching the caller's verified Supabase Auth email. Never trusts client-supplied roles.
CREATE OR REPLACE FUNCTION public.verify_admin_session()
RETURNS jsonb
LANGUAGE plpgsql
SECURITY DEFINER
SET search_path = public, auth
AS $$
DECLARE
  v_uid uuid := auth.uid();
  v_email text;
  v_email_confirmed timestamptz;
  v_admin public.admin_users%ROWTYPE;
  v_invite public.admin_invites%ROWTYPE;
BEGIN
  IF v_uid IS NULL THEN
    RETURN jsonb_build_object('authorized', false, 'reason', 'unauthenticated');
  END IF;

  SELECT lower(u.email), u.email_confirmed_at
    INTO v_email, v_email_confirmed
  FROM auth.users u
  WHERE u.id = v_uid;

  IF v_email IS NULL THEN
    RETURN jsonb_build_object('authorized', false, 'reason', 'missing_email');
  END IF;

  -- Check existing active admin record
  SELECT * INTO v_admin
  FROM public.admin_users
  WHERE user_id = v_uid;

  IF FOUND THEN
    IF NOT v_admin.active THEN
      RETURN jsonb_build_object('authorized', false, 'reason', 'admin_disabled');
    END IF;
    RETURN jsonb_build_object(
      'authorized', true,
      'id', v_admin.id,
      'uid', v_admin.user_id,
      'email', v_admin.email,
      'display_name', v_admin.display_name,
      'role', v_admin.role,
      'active', v_admin.active,
      'assigned_department', v_admin.assigned_department
    );
  END IF;

  -- Claim a pre-approved staff invite if the user's email is verified
  IF v_email_confirmed IS NOT NULL THEN
    SELECT * INTO v_invite
    FROM public.admin_invites
    WHERE lower(email) = v_email
      AND active = true
    LIMIT 1;

    IF FOUND THEN
      INSERT INTO public.admin_users (user_id, email, display_name, role, active)
      VALUES (v_uid, v_email, v_invite.display_name, v_invite.role, true)
      RETURNING * INTO v_admin;

      UPDATE public.user_profiles
      SET account_type = 'ADMIN', updated_at = now()
      WHERE id = v_uid;

      RETURN jsonb_build_object(
        'authorized', true,
        'id', v_admin.id,
        'uid', v_admin.user_id,
        'email', v_admin.email,
        'display_name', v_admin.display_name,
        'role', v_admin.role,
        'active', v_admin.active,
        'assigned_department', v_admin.assigned_department
      );
    END IF;
  END IF;

  RETURN jsonb_build_object('authorized', false, 'reason', 'not_admin');
END;
$$;

REVOKE ALL ON FUNCTION public.current_admin_role() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.current_admin_role() TO authenticated;

REVOKE ALL ON FUNCTION public.is_admin_with_role(text[]) FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.is_admin_with_role(text[]) TO authenticated;

REVOKE ALL ON FUNCTION public.verify_admin_session() FROM PUBLIC;
GRANT EXECUTE ON FUNCTION public.verify_admin_session() TO authenticated;

-- =============================================================================
-- 2. ENABLE & FORCE ROW LEVEL SECURITY ON ALL TABLES
-- =============================================================================
DO $$
DECLARE
  tbl text;
BEGIN
  FOREACH tbl IN ARRAY ARRAY[
    'user_profiles', 'admin_users', 'admin_invites', 'client_profiles',
    'candidate_profiles', 'candidate_preferences', 'service_categories',
    'services', 'service_enquiries', 'job_vacancies', 'job_applications',
    'saved_jobs', 'notifications', 'website_content', 'portfolio_projects',
    'testimonials', 'team_members', 'platform_settings', 'activity_logs'
  ] LOOP
    EXECUTE format('ALTER TABLE public.%I ENABLE ROW LEVEL SECURITY;', tbl);
    EXECUTE format('ALTER TABLE public.%I FORCE ROW LEVEL SECURITY;', tbl);
  END LOOP;
END;
$$;

-- =============================================================================
-- 3. POLICIES: USER PROFILES
-- =============================================================================
DROP POLICY IF EXISTS user_profiles_select_self_or_admin ON public.user_profiles;
CREATE POLICY user_profiles_select_self_or_admin ON public.user_profiles
  FOR SELECT TO authenticated
  USING (
    id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS user_profiles_insert_self ON public.user_profiles;
CREATE POLICY user_profiles_insert_self ON public.user_profiles
  FOR INSERT TO authenticated
  WITH CHECK (
    id = auth.uid()
    AND account_type IN ('CLIENT', 'CANDIDATE')
  );

DROP POLICY IF EXISTS user_profiles_update_self ON public.user_profiles;
CREATE POLICY user_profiles_update_self ON public.user_profiles
  FOR UPDATE TO authenticated
  USING (id = auth.uid())
  WITH CHECK (
    id = auth.uid()
    AND account_type IN ('CLIENT', 'CANDIDATE')
  );

-- =============================================================================
-- 4. POLICIES: CLIENT PROFILES (zero cross-client exposure)
-- =============================================================================
DROP POLICY IF EXISTS client_profiles_select ON public.client_profiles;
CREATE POLICY client_profiles_select ON public.client_profiles
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS client_profiles_insert_self ON public.client_profiles;
CREATE POLICY client_profiles_insert_self ON public.client_profiles
  FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS client_profiles_update ON public.client_profiles;
CREATE POLICY client_profiles_update ON public.client_profiles
  FOR UPDATE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
  )
  WITH CHECK (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
  );

DROP POLICY IF EXISTS client_profiles_delete ON public.client_profiles;
CREATE POLICY client_profiles_delete ON public.client_profiles
  FOR DELETE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
  );

-- =============================================================================
-- 5. POLICIES: SERVICE ENQUIRIES (client self + assigned consultant + super admin)
-- =============================================================================
DROP POLICY IF EXISTS service_enquiries_select ON public.service_enquiries;
CREATE POLICY service_enquiries_select ON public.service_enquiries
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'REPORTING_ADMIN'])
    OR (
      public.is_admin_with_role(ARRAY['CONSULTANT'])
      AND assigned_consultant_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS service_enquiries_insert_client ON public.service_enquiries;
CREATE POLICY service_enquiries_insert_client ON public.service_enquiries
  FOR INSERT TO authenticated
  WITH CHECK (
    user_id = auth.uid()
    AND status = 'SUBMITTED'
    AND assigned_consultant_id IS NULL
    AND consultant_notes IS NULL
  );

DROP POLICY IF EXISTS service_enquiries_update ON public.service_enquiries;
CREATE POLICY service_enquiries_update ON public.service_enquiries
  FOR UPDATE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
    OR (
      public.is_admin_with_role(ARRAY['CONSULTANT'])
      AND assigned_consultant_id = auth.uid()
    )
  )
  WITH CHECK (
    (
      user_id = auth.uid()
      AND status IN ('SUBMITTED', 'UNDER_REVIEW', 'PROPOSAL_SHARED', 'CONVERTED', 'WITHDRAWN')
    )
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
    OR (
      public.is_admin_with_role(ARRAY['CONSULTANT'])
      AND assigned_consultant_id = auth.uid()
    )
  );

DROP POLICY IF EXISTS service_enquiries_delete ON public.service_enquiries;
CREATE POLICY service_enquiries_delete ON public.service_enquiries
  FOR DELETE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
  );

-- =============================================================================
-- 6. POLICIES: CANDIDATE PROFILES & PREFERENCES (candidate self + recruitment admins)
-- =============================================================================
DROP POLICY IF EXISTS candidate_profiles_select ON public.candidate_profiles;
CREATE POLICY candidate_profiles_select ON public.candidate_profiles
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS candidate_profiles_insert_self ON public.candidate_profiles;
CREATE POLICY candidate_profiles_insert_self ON public.candidate_profiles
  FOR INSERT TO authenticated
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS candidate_profiles_update ON public.candidate_profiles;
CREATE POLICY candidate_profiles_update ON public.candidate_profiles
  FOR UPDATE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  )
  WITH CHECK (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

DROP POLICY IF EXISTS candidate_profiles_delete ON public.candidate_profiles;
CREATE POLICY candidate_profiles_delete ON public.candidate_profiles
  FOR DELETE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN'])
  );

DROP POLICY IF EXISTS candidate_preferences_select ON public.candidate_preferences;
CREATE POLICY candidate_preferences_select ON public.candidate_preferences
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS candidate_preferences_write_self ON public.candidate_preferences;
CREATE POLICY candidate_preferences_write_self ON public.candidate_preferences
  FOR ALL TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  )
  WITH CHECK (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

-- =============================================================================
-- 7. POLICIES: JOB APPLICATIONS, SAVED JOBS, NOTIFICATIONS
-- =============================================================================
DROP POLICY IF EXISTS job_applications_select ON public.job_applications;
CREATE POLICY job_applications_select ON public.job_applications
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS job_applications_insert_candidate ON public.job_applications;
CREATE POLICY job_applications_insert_candidate ON public.job_applications
  FOR INSERT TO authenticated
  WITH CHECK (
    user_id = auth.uid()
    AND status = 'APPLIED'
  );

DROP POLICY IF EXISTS job_applications_update ON public.job_applications;
CREATE POLICY job_applications_update ON public.job_applications
  FOR UPDATE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  )
  WITH CHECK (
    (user_id = auth.uid() AND status = 'WITHDRAWN')
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

DROP POLICY IF EXISTS job_applications_delete ON public.job_applications;
CREATE POLICY job_applications_delete ON public.job_applications
  FOR DELETE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

DROP POLICY IF EXISTS saved_jobs_self ON public.saved_jobs;
CREATE POLICY saved_jobs_self ON public.saved_jobs
  FOR ALL TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS notifications_select ON public.notifications;
CREATE POLICY notifications_select ON public.notifications
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS notifications_insert ON public.notifications;
CREATE POLICY notifications_insert ON public.notifications
  FOR INSERT TO authenticated
  WITH CHECK (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

DROP POLICY IF EXISTS notifications_update_self ON public.notifications;
CREATE POLICY notifications_update_self ON public.notifications
  FOR UPDATE TO authenticated
  USING (user_id = auth.uid())
  WITH CHECK (user_id = auth.uid());

DROP POLICY IF EXISTS notifications_delete ON public.notifications;
CREATE POLICY notifications_delete ON public.notifications
  FOR DELETE TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

-- =============================================================================
-- 8. POLICIES: PUBLIC CMS TABLES (published read for anon/auth + role-scoped write)
-- =============================================================================
DO $$
DECLARE
  cms_tbl text;
BEGIN
  FOREACH cms_tbl IN ARRAY ARRAY[
    'service_categories', 'services', 'website_content',
    'portfolio_projects', 'testimonials', 'team_members'
  ] LOOP
    EXECUTE format('DROP POLICY IF EXISTS %I_public_read ON public.%I;', cms_tbl, cms_tbl);
    EXECUTE format(
      'CREATE POLICY %I_public_read ON public.%I FOR SELECT TO anon, authenticated USING (published = true OR public.is_admin_with_role(ARRAY[''SUPER_ADMIN'', ''CONTENT_ADMIN'', ''REPORTING_ADMIN'']));',
      cms_tbl, cms_tbl
    );
    EXECUTE format('DROP POLICY IF EXISTS %I_content_admin_write ON public.%I;', cms_tbl, cms_tbl);
    EXECUTE format(
      'CREATE POLICY %I_content_admin_write ON public.%I FOR ALL TO authenticated USING (public.is_admin_with_role(ARRAY[''SUPER_ADMIN'', ''CONTENT_ADMIN''])) WITH CHECK (public.is_admin_with_role(ARRAY[''SUPER_ADMIN'', ''CONTENT_ADMIN'']));',
      cms_tbl, cms_tbl
    );
  END LOOP;
END;
$$;

-- Job Vacancies: published public read + SUPER_ADMIN / RECRUITMENT_ADMIN write
DROP POLICY IF EXISTS job_vacancies_read ON public.job_vacancies;
CREATE POLICY job_vacancies_read ON public.job_vacancies
  FOR SELECT TO anon, authenticated
  USING (
    published = true
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS job_vacancies_write ON public.job_vacancies;
CREATE POLICY job_vacancies_write ON public.job_vacancies
  FOR ALL TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']));

-- =============================================================================
-- 9. POLICIES: GOVERNANCE TABLES (admin_users, admin_invites, platform_settings, activity_logs)
-- =============================================================================
DROP POLICY IF EXISTS admin_users_select ON public.admin_users;
CREATE POLICY admin_users_select ON public.admin_users
  FOR SELECT TO authenticated
  USING (
    user_id = auth.uid()
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS admin_users_write_super ON public.admin_users;
CREATE POLICY admin_users_write_super ON public.admin_users
  FOR ALL TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN']));

DROP POLICY IF EXISTS admin_invites_super_only ON public.admin_invites;
CREATE POLICY admin_invites_super_only ON public.admin_invites
  FOR ALL TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN']));

DROP POLICY IF EXISTS platform_settings_select ON public.platform_settings;
CREATE POLICY platform_settings_select ON public.platform_settings
  FOR SELECT TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'REPORTING_ADMIN']));

DROP POLICY IF EXISTS platform_settings_write_super ON public.platform_settings;
CREATE POLICY platform_settings_write_super ON public.platform_settings
  FOR ALL TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN']));

DROP POLICY IF EXISTS activity_logs_select_admins ON public.activity_logs;
CREATE POLICY activity_logs_select_admins ON public.activity_logs
  FOR SELECT TO authenticated
  USING (
    public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'CONTENT_ADMIN', 'CONSULTANT', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS activity_logs_insert_writable_admins ON public.activity_logs;
CREATE POLICY activity_logs_insert_writable_admins ON public.activity_logs
  FOR INSERT TO authenticated
  WITH CHECK (
    actor_uid = auth.uid()
    AND public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'CONTENT_ADMIN', 'CONSULTANT'])
  );


-- ==================== supabase/migrations/202611250003_mila_private_cv_storage.sql ====================

-- MILA HOSPITALITY CONSULTANT — Private Candidate CV Storage Bucket & Initial Catalog Seed
-- Ensures candidate CVs are strictly private (public = false) and accessible ONLY to:
--   1. The candidate owner (objects stored under `<auth.uid()>/...`)
--   2. Verified SUPER_ADMIN or RECRUITMENT_ADMIN staff members

-- 1. Create Private Bucket (`candidate-cvs`)
INSERT INTO storage.buckets (id, name, public, file_size_limit, allowed_mime_types)
VALUES (
  'candidate-cvs',
  'candidate-cvs',
  false,
  10485760, -- 10 MB max
  ARRAY[
    'application/pdf',
    'application/msword',
    'application/vnd.openxmlformats-officedocument.wordprocessingml.document'
  ]
)
ON CONFLICT (id) DO UPDATE
  SET public = false,
      file_size_limit = EXCLUDED.file_size_limit,
      allowed_mime_types = EXCLUDED.allowed_mime_types;

-- 2. Strict Storage RLS Policies on `storage.objects` for `candidate-cvs`
DROP POLICY IF EXISTS mila_cv_select_owner_or_recruitment_admin ON storage.objects;
CREATE POLICY mila_cv_select_owner_or_recruitment_admin
  ON storage.objects
  FOR SELECT
  TO authenticated
  USING (
    bucket_id = 'candidate-cvs'
    AND (
      (storage.foldername(name))[1] = auth.uid()::text
      OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
    )
  );

DROP POLICY IF EXISTS mila_cv_insert_owner_only ON storage.objects;
CREATE POLICY mila_cv_insert_owner_only
  ON storage.objects
  FOR INSERT
  TO authenticated
  WITH CHECK (
    bucket_id = 'candidate-cvs'
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

DROP POLICY IF EXISTS mila_cv_update_owner_only ON storage.objects;
CREATE POLICY mila_cv_update_owner_only
  ON storage.objects
  FOR UPDATE
  TO authenticated
  USING (
    bucket_id = 'candidate-cvs'
    AND (storage.foldername(name))[1] = auth.uid()::text
  )
  WITH CHECK (
    bucket_id = 'candidate-cvs'
    AND (storage.foldername(name))[1] = auth.uid()::text
  );

DROP POLICY IF EXISTS mila_cv_delete_owner_or_recruitment_admin ON storage.objects;
CREATE POLICY mila_cv_delete_owner_or_recruitment_admin
  ON storage.objects
  FOR DELETE
  TO authenticated
  USING (
    bucket_id = 'candidate-cvs'
    AND (
      (storage.foldername(name))[1] = auth.uid()::text
      OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
    )
  );

-- 3. Idempotent Platform Settings & Core Service Categories Seed
INSERT INTO public.platform_settings (id)
VALUES ('platform')
ON CONFLICT (id) DO NOTHING;

INSERT INTO public.service_categories (code, sort_order, title, division_name, summary, published)
VALUES
  ('01', 1, 'Hospitality Pre-Opening Consultancy', 'MILA Consultancy', 'Turnkey pre-opening governance across 30+ hotels and resorts.', true),
  ('02', 2, 'Restaurant and Café Development', 'MILA Consultancy', 'End-to-end concept, kitchen workflow, and menu engineering.', true),
  ('03', 3, 'Bar and Beverage Consultancy', 'MILA Academy & Expertise', 'Champion mixology curation, speed-bar layout, and pour-cost engineering.', true),
  ('04', 4, 'Hospitality Training', 'MILA Academy & Expertise', 'Service choreography, bar mastery, and opening-team bootcamps.', true),
  ('05', 5, 'Operations Management', 'MILA Consultancy', 'Operational audits, SOP discipline, and GOP margin turnarounds.', true),
  ('06', 6, 'Recruitment and Talent Acquisition', 'MILA Talent', 'Confidential pan-India hospitality executive search and opening teams.', true),
  ('07', 7, 'Business and Concept Development', 'MILA Consultancy', 'Feasibility studies, brand architecture, and CAPEX-to-ROI modeling.', true),
  ('08', 8, 'Hospitality Management Solutions', 'MILA Consultancy', 'Owner representation, interim management, and asset stewardship.', true)
ON CONFLICT (code) DO NOTHING;

INSERT INTO public.services (
  service_type, slug, sort_order, division_name, title, subtitle, summary,
  detailed_description, services_included, owner_benefits, deliverables,
  typical_timeline, image_key, published
)
VALUES
  (
    'HOSPITALITY_PRE_OPENING', 'hospitality-pre-opening-consultancy', 1, 'MILA Consultancy',
    'Hospitality Pre-Opening Consultancy', 'Turnkey Critical Path & Opening Governance',
    'Complete pre-opening orchestration for luxury hotels, heritage properties, and destination resorts across India.',
    'Drawing on 30+ hospitality pre-opening mandates across India, MILA stewards every phase from architectural handover and OS&E procurement to staffing, trial runs, and commercial launch.',
    ARRAY['Master pre-opening critical path & budget control', 'OS&E / FF&E specification & vendor audits', 'Departmental SOP creation & dry-run commissioning'],
    ARRAY['Eliminate costly launch delays and CAPEX overruns', 'Achieve brand-standard guest readiness from Day 1'],
    ARRAY['Master Pre-Opening Critical Path Tracker', 'Complete Departmental SOP & Quality Manual', 'Opening Readiness & Trial-Run Audit Report'],
    '4 – 12 Months', 'palace_resort', true
  ),
  (
    'RESTAURANT_CAFE_DEVELOPMENT', 'restaurant-cafe-development', 2, 'MILA Consultancy',
    'Restaurant and Café Development', 'Culinary Architecture, Kitchen Flow & Launch',
    'Commercial restaurant, specialty coffee, and dining setups built for repeatable margins and guest retention.',
    'Across 20+ café and restaurant setups, MILA bridges culinary creativity with back-of-house ergonomics, recipe costing, vendor sourcing, and launch execution.',
    ARRAY['Kitchen & café bar ergonomic workflow planning', 'Recipe standardization & food-cost engineering', 'Vendor onboarding & opening brigade training'],
    ARRAY['Disciplined food-cost ratios from opening month', 'Faster table turns without compromising luxury service'],
    ARRAY['BOH & FOH Space Allocation Blueprint', 'Standardized Recipe & Margin Matrix', 'Launch Playbook & Opening Checklist'],
    '2 – 6 Months', 'dining_interior', true
  ),
  (
    'BAR_BEVERAGE_CONSULTANCY', 'bar-beverage-consultancy', 3, 'MILA Academy & Expertise',
    'Bar and Beverage Consultancy', 'Award-Winning Three-Time Champion Mixology',
    'Signature craft cocktail programs, ergonomic speed-bar design, and high-margin beverage curation.',
    'Led by our three-time champion mixologist, MILA designs bespoke beverage identities, zero-waste prep laboratories, glassware standards, and profitable pour-cost controls.',
    ARRAY['Signature cocktail & zero-proof menu engineering', 'Speed-bar station design & ice program setup', 'Pour-cost control, inventory & bartender mastery'],
    ARRAY['Higher beverage check averages and repeat destination traffic', 'Consistent execution across shifts with reduced wastage'],
    ARRAY['Signature Beverage Bible & Spec Cards', 'Ergonomic Bar Station Layout Plan', 'Beverage Costing & Inventory SOPs'],
    '6 – 12 Weeks', 'mixology_bar', true
  ),
  (
    'HOSPITALITY_TRAINING', 'hospitality-training', 4, 'MILA Academy & Expertise',
    'Hospitality Training', 'Service Choreography & Academy Programs',
    'Structured luxury service academies, culinary/bar masterclasses, and leadership readiness modules.',
    'MILA Academy equips opening and established hospitality teams with refined service rituals, upselling frameworks, mixology technique, and supervisory accountability.',
    ARRAY['FOH luxury service choreography & guest recovery', 'Mixology & sommelier craft intensives', 'Supervisor & HOD leadership workshops'],
    ARRAY['Elevated guest satisfaction scores and review ratings', 'Lower staff attrition through structured skill growth'],
    ARRAY['Role-Based Training Curriculum & Handbooks', 'Practical Skill Assessment Scorecards', 'Post-Training Mystery Audit Summary'],
    '2 – 8 Weeks', 'hospitality_training', true
  ),
  (
    'OPERATIONS_MANAGEMENT', 'operations-management', 5, 'MILA Consultancy',
    'Operations Management', 'Margin Optimization & SOP Turnaround',
    'Hands-on operational diagnostics, P&L restructuring, and ongoing quality governance for live properties.',
    'We audit live hotels, restaurants, and lounges to uncover leakage in food/beverage cost, payroll productivity, procurement, and guest experience consistency.',
    ARRAY['360-degree operational & P&L diagnostic', 'Procurement renegotiation & wastage controls', 'Monthly mystery audits & KPI dashboards'],
    ARRAY['Measurable GOP and EBITDA improvement', 'Consistent multi-outlet brand execution'],
    ARRAY['Operational Diagnostic & Turnaround Roadmap', 'Weekly KPI & Cost Control Templates', 'Quarterly Quality Governance Review'],
    '3 – 12 Months', 'operations_mgmt', true
  ),
  (
    'RECRUITMENT_TALENT_ACQUISITION', 'recruitment-talent-acquisition', 6, 'MILA Talent',
    'Recruitment and Talent Acquisition', 'Pan-India Hospitality Executive Search',
    'Confidential recruitment for General Managers, Executive Chefs, Head Mixologists, and complete opening brigades.',
    'MILA Talent pairs deep industry assessment with confidential executive headhunting across luxury hotels, heritage resorts, and high-volume dining groups.',
    ARRAY['Executive search for GM, Culinary, F&B & Mixology heads', 'Complete pre-opening manpower budgeting & hiring', 'Technical trade tests & reference verification'],
    ARRAY['Faster time-to-hire with operationally vetted leaders', 'Stronger cultural and P&L alignment for Indian assets'],
    ARRAY['Manpower Grid & Compensation Benchmark', 'Vetted Candidate Shortlist & Assessment Dossiers', 'Onboarding & Retention Plan'],
    '3 – 8 Weeks', 'talent_recruitment', true
  ),
  (
    'BUSINESS_CONCEPT_DEVELOPMENT', 'business-concept-development', 7, 'MILA Consultancy',
    'Business and Concept Development', 'Feasibility, Brand DNA & Financial Modeling',
    'Data-backed concept positioning, market feasibility, and 5-year CAPEX-to-ROI financial models.',
    'Before capital is committed, MILA validates market demand, competitive whitespace, pricing architecture, and investment payback for new hospitality ventures.',
    ARRAY['Location & competitive market feasibility', 'Brand narrative, guest journey & positioning DNA', '5-year CAPEX, OPEX & ROI financial modeling'],
    ARRAY['De-risked capital allocation for owners and investors', 'Investor-ready pitch deck and operational roadmap'],
    ARRAY['Market Feasibility & Positioning Report', '5-Year Financial Model & Sensitivity Analysis', 'Concept Identity & Guest Journey Dossier'],
    '4 – 10 Weeks', 'concept_dev', true
  ),
  (
    'HOSPITALITY_MANAGEMENT_SOLUTIONS', 'hospitality-management-solutions', 8, 'MILA Consultancy',
    'Hospitality Management Solutions', 'Owner Representation & Asset Stewardship',
    'Interim operational leadership, operator selection, and long-term asset management for hospitality owners.',
    'MILA represents owners across brand negotiations, HMA reviews, interim GM placement, and ongoing asset yield optimization.',
    ARRAY['Hotel Management Agreement (HMA) advisory', 'Interim leadership & asset management oversight', 'Annual budget review & capital preservation'],
    ARRAY['Protected owner returns and transparent reporting', 'Accountable operator governance across every quarter'],
    ARRAY['Owner Asset Stewardship Charter', 'Monthly Financial & Yield Review Pack', 'Annual Strategic Capital Plan'],
    '6 – 24 Months', 'management_solutions', true
  )
ON CONFLICT (service_type) DO NOTHING;

INSERT INTO public.website_content (
  page_key, page_title, eyebrow, headline, subheadline, body_copy, hero_image_key, cta_label, published
)
VALUES
  ('home', 'Home', 'MILA HOSPITALITY CONSULTANT', 'Creating Excellence in Hospitality', '15+ years across 30+ hotel pre-openings, 20+ restaurant setups, and three-time champion mixology.', 'Partnering with Indian hospitality owners, luxury hotels, heritage properties, and culinary leaders.', 'hero_hotel', 'Our Services', true),
  ('about_us', 'About Us', 'OUR HERITAGE', 'Built by Operators for Indian Hospitality', 'Consultancy rigor, executive recruitment, and academy training under one roof.', 'MILA Hospitality Consultant delivers international standards adapted for India''s dynamic hospitality landscape.', 'palace_resort', 'Contact Us', true),
  ('our_expertise', 'Our Expertise', 'MILA ACADEMY & EXPERTISE', 'Three-Time Champion Mixology & Training', 'Bespoke beverage labs, service choreography, and leadership academies.', 'Hands-on mastery that translates craft directly into commercial margins.', 'mixology_bar', 'Request Mandate', true)
ON CONFLICT (page_key) DO NOTHING;


-- ==================== supabase/migrations/202611250004_mila_automated_notifications.sql ====================

-- ============================================================================
-- MILA HOSPITALITY CONSULTANT — Migration 0004: Automated Job Notifications
-- Multi-channel (Android FCM, iOS APNs via FCM, In-App, Optional Email),
-- Candidate Eligibility Matching RPC, Delivery Logs & Admin Controls
-- ============================================================================

-- 1. Extend candidate_preferences with channel opt-ins
ALTER TABLE public.candidate_preferences
  ADD COLUMN IF NOT EXISTS push_notifications_enabled boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS in_app_notifications_enabled boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS email_notifications_enabled boolean NOT NULL DEFAULT false,
  ADD COLUMN IF NOT EXISTS application_status_alerts_enabled boolean NOT NULL DEFAULT true,
  ADD COLUMN IF NOT EXISTS preferred_skills text[] NOT NULL DEFAULT '{}';

-- 2. Device Push Tokens (Android FCM + iOS APNs-via-FCM tokens)
CREATE TABLE IF NOT EXISTS public.device_push_tokens (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  fcm_token text NOT NULL CHECK (char_length(trim(fcm_token)) BETWEEN 20 AND 512),
  platform text NOT NULL CHECK (platform IN ('ANDROID', 'IOS', 'WEB')),
  apns_environment text CHECK (apns_environment IS NULL OR apns_environment IN ('PRODUCTION', 'SANDBOX')),
  device_label text NOT NULL DEFAULT 'Mobile Device' CHECK (char_length(device_label) <= 120),
  is_active boolean NOT NULL DEFAULT true,
  last_seen_at timestamptz NOT NULL DEFAULT now(),
  created_at timestamptz NOT NULL DEFAULT now(),
  updated_at timestamptz NOT NULL DEFAULT now(),
  UNIQUE (user_id, fcm_token)
);

CREATE INDEX IF NOT EXISTS idx_device_push_tokens_user_active
  ON public.device_push_tokens (user_id, is_active, platform);

DROP TRIGGER IF EXISTS trg_device_push_tokens_updated_at ON public.device_push_tokens;
CREATE TRIGGER trg_device_push_tokens_updated_at
  BEFORE UPDATE ON public.device_push_tokens
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 3. Administrator Notification Controls (singleton configuration)
CREATE TABLE IF NOT EXISTS public.notification_controls (
  id text PRIMARY KEY DEFAULT 'global' CHECK (id = 'global'),
  auto_dispatch_on_vacancy_publish boolean NOT NULL DEFAULT true,
  enable_in_app boolean NOT NULL DEFAULT true,
  enable_android_push boolean NOT NULL DEFAULT true,
  enable_ios_push boolean NOT NULL DEFAULT true,
  enable_email_notifications boolean NOT NULL DEFAULT true,
  enable_application_status_notifications boolean NOT NULL DEFAULT true,
  require_department_match boolean NOT NULL DEFAULT true,
  require_location_match boolean NOT NULL DEFAULT true,
  experience_tolerance_years integer NOT NULL DEFAULT 1 CHECK (experience_tolerance_years BETWEEN 0 AND 5),
  max_notifications_per_candidate_per_day integer NOT NULL DEFAULT 5 CHECK (max_notifications_per_candidate_per_day BETWEEN 1 AND 50),
  updated_by uuid REFERENCES public.user_profiles(id) ON DELETE SET NULL,
  updated_at timestamptz NOT NULL DEFAULT now()
);

INSERT INTO public.notification_controls (id)
VALUES ('global')
ON CONFLICT (id) DO NOTHING;

DROP TRIGGER IF EXISTS trg_notification_controls_updated_at ON public.notification_controls;
CREATE TRIGGER trg_notification_controls_updated_at
  BEFORE UPDATE ON public.notification_controls
  FOR EACH ROW EXECUTE FUNCTION public.set_updated_at();

-- 4. Notification Delivery Logs (per-channel audit trail)
CREATE TABLE IF NOT EXISTS public.notification_delivery_logs (
  id uuid PRIMARY KEY DEFAULT gen_random_uuid(),
  notification_id uuid REFERENCES public.notifications(id) ON DELETE SET NULL,
  recipient_user_id uuid NOT NULL REFERENCES public.user_profiles(id) ON DELETE CASCADE,
  recipient_name text NOT NULL DEFAULT '',
  recipient_email text NOT NULL DEFAULT '',
  event_type text NOT NULL CHECK (event_type IN ('VACANCY_MATCH', 'APPLICATION_STATUS', 'ADMIN_DIRECT')),
  vacancy_id uuid REFERENCES public.job_vacancies(id) ON DELETE SET NULL,
  application_id uuid REFERENCES public.job_applications(id) ON DELETE SET NULL,
  channel text NOT NULL CHECK (channel IN ('IN_APP', 'ANDROID_PUSH', 'IOS_PUSH', 'EMAIL')),
  status text NOT NULL CHECK (status IN ('QUEUED', 'DELIVERED', 'SKIPPED', 'FAILED')),
  match_score integer CHECK (match_score IS NULL OR (match_score BETWEEN 0 AND 100)),
  match_reasons text[] NOT NULL DEFAULT '{}',
  provider_message_id text,
  error_detail text,
  dispatched_by uuid REFERENCES public.user_profiles(id) ON DELETE SET NULL,
  created_at timestamptz NOT NULL DEFAULT now()
);

CREATE INDEX IF NOT EXISTS idx_notif_delivery_logs_recipient
  ON public.notification_delivery_logs (recipient_user_id, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_delivery_logs_event
  ON public.notification_delivery_logs (event_type, channel, status, created_at DESC);
CREATE INDEX IF NOT EXISTS idx_notif_delivery_logs_vacancy
  ON public.notification_delivery_logs (vacancy_id, created_at DESC);

-- 5. Enable & Force RLS on new tables
ALTER TABLE public.device_push_tokens ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.device_push_tokens FORCE ROW LEVEL SECURITY;
ALTER TABLE public.notification_controls ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_controls FORCE ROW LEVEL SECURITY;
ALTER TABLE public.notification_delivery_logs ENABLE ROW LEVEL SECURITY;
ALTER TABLE public.notification_delivery_logs FORCE ROW LEVEL SECURITY;

-- device_push_tokens policies: owners manage their own device tokens; recruitment/super admins read active tokens for dispatch
DROP POLICY IF EXISTS device_tokens_owner_select ON public.device_push_tokens;
CREATE POLICY device_tokens_owner_select ON public.device_push_tokens
  FOR SELECT USING (
    auth.uid() = user_id
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN'])
  );

DROP POLICY IF EXISTS device_tokens_owner_insert ON public.device_push_tokens;
CREATE POLICY device_tokens_owner_insert ON public.device_push_tokens
  FOR INSERT WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS device_tokens_owner_update ON public.device_push_tokens;
CREATE POLICY device_tokens_owner_update ON public.device_push_tokens
  FOR UPDATE USING (auth.uid() = user_id)
  WITH CHECK (auth.uid() = user_id);

DROP POLICY IF EXISTS device_tokens_owner_delete ON public.device_push_tokens;
CREATE POLICY device_tokens_owner_delete ON public.device_push_tokens
  FOR DELETE USING (auth.uid() = user_id);

-- notification_controls policies: active admins read; SUPER_ADMIN and RECRUITMENT_ADMIN update
DROP POLICY IF EXISTS notif_controls_admin_select ON public.notification_controls;
CREATE POLICY notif_controls_admin_select ON public.notification_controls
  FOR SELECT USING (
    public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'CONTENT_ADMIN', 'CONSULTANT', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS notif_controls_admin_update ON public.notification_controls;
CREATE POLICY notif_controls_admin_update ON public.notification_controls
  FOR UPDATE USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']));

-- notification_delivery_logs policies: recruitment/super/reporting admins read; recruitment/super insert; immutable audit trail
DROP POLICY IF EXISTS notif_logs_admin_select ON public.notification_delivery_logs;
CREATE POLICY notif_logs_admin_select ON public.notification_delivery_logs
  FOR SELECT USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN', 'REPORTING_ADMIN']));

DROP POLICY IF EXISTS notif_logs_admin_insert ON public.notification_delivery_logs;
CREATE POLICY notif_logs_admin_insert ON public.notification_delivery_logs
  FOR INSERT WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']));

-- 6. Server-side Candidate Matching RPC (strictly filters eligible candidates — NEVER broadcasts to all)
CREATE OR REPLACE FUNCTION public.match_eligible_candidates_for_vacancy(p_vacancy_id uuid)
RETURNS TABLE (
  user_id uuid,
  full_name text,
  email text,
  department text,
  total_experience_years integer,
  preferred_job_locations text,
  push_enabled boolean,
  in_app_enabled boolean,
  email_enabled boolean,
  match_score integer,
  match_reasons text[]
)
LANGUAGE plpgsql
STABLE
SECURITY DEFINER
SET search_path = public
AS $$
DECLARE
  v_vacancy public.job_vacancies%ROWTYPE;
  v_ctrl public.notification_controls%ROWTYPE;
BEGIN
  IF NOT public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'RECRUITMENT_ADMIN']) THEN
    RAISE EXCEPTION 'Unauthorized: recruitment or super admin role required';
  END IF;

  SELECT * INTO v_vacancy FROM public.job_vacancies WHERE id = p_vacancy_id;
  IF NOT FOUND OR v_vacancy.published IS NOT TRUE THEN
    RETURN;
  END IF;

  SELECT * INTO v_ctrl FROM public.notification_controls WHERE id = 'global';

  RETURN QUERY
  SELECT
    cp.user_id,
    cp.full_name,
    cp.email,
    cp.department,
    cp.total_experience_years,
    cp.preferred_job_locations,
    COALESCE(pref.push_notifications_enabled, true) AS push_enabled,
    COALESCE(pref.in_app_notifications_enabled, true) AS in_app_enabled,
    COALESCE(pref.email_notifications_enabled, false) AS email_enabled,
    (
      30
      + CASE WHEN cp.total_experience_years >= v_vacancy.min_experience_years THEN 25 ELSE 15 END
      + CASE
          WHEN lower(cp.preferred_job_locations) LIKE '%' || lower(v_vacancy.city_region) || '%'
            OR lower(cp.preferred_job_locations) LIKE '%pan-india%'
            OR lower(cp.preferred_job_locations) LIKE '%all india%'
          THEN 25 ELSE 10
        END
      + CASE
          WHEN EXISTS (
            SELECT 1 FROM unnest(v_vacancy.desired_skills) AS sk
            WHERE lower(cp.skills) LIKE '%' || lower(sk) || '%'
          ) THEN 20 ELSE 0
        END
    )::integer AS match_score,
    ARRAY_REMOVE(ARRAY[
      'Active job seeker',
      'Department matched (' || cp.department || ')',
      'Experience: ' || cp.total_experience_years || ' yrs (min ' || v_vacancy.min_experience_years || ')',
      'Location matched (' || v_vacancy.city_region || ')'
    ], NULL) AS match_reasons
  FROM public.candidate_profiles cp
  LEFT JOIN public.candidate_preferences pref ON pref.user_id = cp.user_id
  WHERE
    -- 1. Active job-seeking status
    cp.job_alerts_enabled = true
    AND COALESCE(pref.job_alerts_enabled, true) = true
    AND cp.availability <> 'NOT_ACTIVELY_LOOKING'
    -- 2. Experience gate
    AND cp.total_experience_years >= GREATEST(0, v_vacancy.min_experience_years - COALESCE(v_ctrl.experience_tolerance_years, 1))
    -- 3. Department / preference gate
    AND (
      NOT COALESCE(v_ctrl.require_department_match, true)
      OR upper(v_vacancy.category) = 'ALL'
      OR upper(cp.department) = upper(v_vacancy.category)
      OR (upper(v_vacancy.category) = 'PRE_OPENING' AND upper(cp.department) IN ('PRE_OPENING', 'EXECUTIVE', 'FNB_SERVICE', 'CULINARY', 'MIXOLOGY', 'ROOMS'))
      OR (upper(v_vacancy.category) = 'EXECUTIVE' AND upper(cp.department) IN ('EXECUTIVE', 'PRE_OPENING'))
      OR (upper(v_vacancy.category) = 'MIXOLOGY' AND upper(cp.department) IN ('MIXOLOGY', 'FNB_SERVICE', 'BAR_BEVERAGE'))
      OR (upper(v_vacancy.category) = 'FNB_SERVICE' AND upper(cp.department) IN ('FNB_SERVICE', 'MIXOLOGY'))
      OR (upper(v_vacancy.category) = 'ROOMS' AND upper(cp.department) IN ('ROOMS', 'HOUSEKEEPING_WELLNESS'))
      OR upper(v_vacancy.category) = ANY(COALESCE(pref.preferred_departments, '{}'))
    )
    -- 4. Preferred location gate
    AND (
      NOT COALESCE(v_ctrl.require_location_match, true)
      OR lower(cp.preferred_job_locations) LIKE '%' || lower(v_vacancy.city_region) || '%'
      OR lower(cp.preferred_job_locations) LIKE '%' || lower(split_part(v_vacancy.location, ',', 1)) || '%'
      OR lower(cp.location) LIKE '%' || lower(v_vacancy.city_region) || '%'
      OR lower(cp.preferred_job_locations) LIKE '%pan-india%'
      OR lower(cp.preferred_job_locations) LIKE '%all india%'
      OR lower(cp.preferred_job_locations) LIKE '%open to relocate%'
    );
END;
$$;

GRANT EXECUTE ON FUNCTION public.match_eligible_candidates_for_vacancy(uuid) TO authenticated;
