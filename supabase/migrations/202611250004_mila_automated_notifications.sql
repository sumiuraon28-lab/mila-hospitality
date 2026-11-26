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
