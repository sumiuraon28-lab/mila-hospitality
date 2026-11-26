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
