-- =============================================================================
-- MILA Hospitality Consultant — Client Reviews, Admin Replies & Moderation
-- Idempotent migration: safe to run multiple times in Supabase SQL Editor.
-- =============================================================================

-- 1. Extend public.testimonials with star rating, user attribution, and admin reply fields
ALTER TABLE public.testimonials
  ADD COLUMN IF NOT EXISTS rating integer NOT NULL DEFAULT 5,
  ADD COLUMN IF NOT EXISTS reviewer_user_id uuid REFERENCES auth.users(id) ON DELETE SET NULL,
  ADD COLUMN IF NOT EXISTS reviewer_email text NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS source text NOT NULL DEFAULT 'ADMIN',
  ADD COLUMN IF NOT EXISTS admin_reply text NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS admin_replied_by text NOT NULL DEFAULT '',
  ADD COLUMN IF NOT EXISTS admin_replied_at timestamptz;

DO $$
BEGIN
  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'testimonials_rating_check'
  ) THEN
    ALTER TABLE public.testimonials
      ADD CONSTRAINT testimonials_rating_check CHECK (rating BETWEEN 1 AND 5);
  END IF;

  IF NOT EXISTS (
    SELECT 1 FROM pg_constraint WHERE conname = 'testimonials_source_check'
  ) THEN
    ALTER TABLE public.testimonials
      ADD CONSTRAINT testimonials_source_check CHECK (source IN ('ADMIN', 'APP_USER'));
  END IF;
END;
$$;

CREATE INDEX IF NOT EXISTS idx_testimonials_published_order
  ON public.testimonials (published, sort_order, created_at DESC);

-- 2. Enforce strict server-side RLS policies on public.testimonials
ALTER TABLE public.testimonials ENABLE ROW LEVEL SECURITY;

DROP POLICY IF EXISTS testimonials_public_read ON public.testimonials;
CREATE POLICY testimonials_public_read ON public.testimonials
  FOR SELECT TO anon, authenticated
  USING (
    published = true
    OR public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'CONTENT_ADMIN', 'REPORTING_ADMIN'])
  );

DROP POLICY IF EXISTS testimonials_app_user_insert ON public.testimonials;
CREATE POLICY testimonials_app_user_insert ON public.testimonials
  FOR INSERT TO authenticated
  WITH CHECK (
    reviewer_user_id = auth.uid()
    AND source = 'APP_USER'
    AND published = false
    AND admin_reply = ''
    AND admin_replied_by = ''
    AND admin_replied_at IS NULL
    AND rating BETWEEN 1 AND 5
    AND char_length(trim(quote)) BETWEEN 10 AND 1200
    AND char_length(trim(attribution_role)) BETWEEN 2 AND 120
  );

DROP POLICY IF EXISTS testimonials_content_admin_write ON public.testimonials;
CREATE POLICY testimonials_content_admin_write ON public.testimonials
  FOR ALL TO authenticated
  USING (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'CONTENT_ADMIN']))
  WITH CHECK (public.is_admin_with_role(ARRAY['SUPER_ADMIN', 'CONTENT_ADMIN']));

-- 3. Seed the 3 initial MILA partner testimonials ONLY if the table is empty
INSERT INTO public.testimonials (
  quote,
  attribution_role,
  engagement_context,
  rating,
  source,
  sort_order,
  published
)
SELECT v.quote, v.attribution_role, v.engagement_context, v.rating, 'ADMIN', v.sort_order, true
FROM (
  VALUES
    (
      'MILA brought calm, structured pre-opening discipline to our resort launch—aligning kitchen design, service SOPs, and department head hiring ahead of schedule.',
      'Managing Partner',
      'Luxury Heritage Resort Pre-Opening • Rajasthan',
      5,
      1
    ),
    (
      'From espresso bar ergonomics to food-cost calibration and opening staff training, their café setup methodology helped us achieve operational stability from week one.',
      'Founder',
      'Specialty Café & Bakery Concept • Bengaluru',
      5,
      2
    ),
    (
      'Their beverage program and bar-team training transformed both our guest experience and pour-cost discipline across our evening lounge.',
      'Director of Operations',
      'Fine-Dining & Cocktail Lounge • Mumbai',
      5,
      3
    )
) AS v(quote, attribution_role, engagement_context, rating, sort_order)
WHERE NOT EXISTS (SELECT 1 FROM public.testimonials LIMIT 1);
