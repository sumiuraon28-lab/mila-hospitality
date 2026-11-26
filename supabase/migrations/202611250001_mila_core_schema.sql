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
