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
