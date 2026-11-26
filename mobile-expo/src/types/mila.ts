export type AccountType = 'CLIENT' | 'CANDIDATE' | 'ADMIN';

export type PortalMode = 'CLIENT' | 'TALENT';

export type BusinessCategory =
  | 'LUXURY_HOTEL_RESORT'
  | 'HERITAGE_PALACE'
  | 'FINE_DINING_RESTAURANT'
  | 'SPECIALTY_CAFE_BAKERY'
  | 'COCKTAIL_BAR_LOUNGE'
  | 'HOSPITALITY_GROUP_INVESTOR';

export const BUSINESS_CATEGORY_OPTIONS: Array<{ value: BusinessCategory; label: string }> = [
  { value: 'LUXURY_HOTEL_RESORT', label: 'Luxury Hotel / Resort' },
  { value: 'HERITAGE_PALACE', label: 'Heritage / Palace Property' },
  { value: 'FINE_DINING_RESTAURANT', label: 'Fine-Dining Restaurant' },
  { value: 'SPECIALTY_CAFE_BAKERY', label: 'Specialty Café / Bakery' },
  { value: 'COCKTAIL_BAR_LOUNGE', label: 'Cocktail Bar / Lounge' },
  { value: 'HOSPITALITY_GROUP_INVESTOR', label: 'Hospitality Group / Investor' },
];

export type RoleCategory =
  | 'ALL'
  | 'EXECUTIVE'
  | 'CULINARY'
  | 'MIXOLOGY'
  | 'PRE_OPENING'
  | 'FNB_SERVICE'
  | 'ROOMS';

export const ROLE_CATEGORY_OPTIONS: Array<{ value: RoleCategory; label: string }> = [
  { value: 'ALL', label: 'All Departments' },
  { value: 'EXECUTIVE', label: 'Executive Leadership' },
  { value: 'PRE_OPENING', label: 'Pre-Opening' },
  { value: 'CULINARY', label: 'Culinary' },
  { value: 'MIXOLOGY', label: 'Mixology & Bar' },
  { value: 'FNB_SERVICE', label: 'F&B Service' },
  { value: 'ROOMS', label: 'Rooms Division' },
];

export type HospitalityDepartment =
  | 'EXECUTIVE'
  | 'CULINARY'
  | 'FNB_SERVICE'
  | 'MIXOLOGY'
  | 'PRE_OPENING'
  | 'ROOMS'
  | 'HOUSEKEEPING_WELLNESS'
  | 'SALES_MARKETING';

export const DEPARTMENT_OPTIONS: Array<{ value: HospitalityDepartment; label: string }> = [
  { value: 'EXECUTIVE', label: 'Executive / GM' },
  { value: 'PRE_OPENING', label: 'Pre-Opening Taskforce' },
  { value: 'CULINARY', label: 'Culinary & Pastry' },
  { value: 'MIXOLOGY', label: 'Bar & Mixology' },
  { value: 'FNB_SERVICE', label: 'F&B Service' },
  { value: 'ROOMS', label: 'Rooms & Front Office' },
  { value: 'HOUSEKEEPING_WELLNESS', label: 'Housekeeping & Wellness' },
  { value: 'SALES_MARKETING', label: 'Sales & Marketing' },
];

export type CandidateAvailability =
  | 'IMMEDIATE'
  | 'WITHIN_15_DAYS'
  | 'WITHIN_30_DAYS'
  | 'WITHIN_60_DAYS'
  | 'OPEN_TO_OPPORTUNITIES';

export const AVAILABILITY_OPTIONS: Array<{ value: CandidateAvailability; label: string }> = [
  { value: 'IMMEDIATE', label: 'Immediate Joiner' },
  { value: 'WITHIN_15_DAYS', label: 'Within 15 Days' },
  { value: 'WITHIN_30_DAYS', label: 'Within 30 Days' },
  { value: 'WITHIN_60_DAYS', label: 'Within 60 Days' },
  { value: 'OPEN_TO_OPPORTUNITIES', label: 'Open to Right Role' },
];

export const REGION_FILTERS = [
  'All India',
  'Mumbai',
  'New Delhi NCR',
  'Bengaluru',
  'Udaipur & Rajasthan',
  'Goa',
  'Hyderabad',
] as const;

export type ExperienceFilter = 'ANY' | '0_5' | '5_10' | '10_PLUS';

export const EXPERIENCE_FILTERS: Array<{ value: ExperienceFilter; label: string }> = [
  { value: 'ANY', label: 'Any Experience' },
  { value: '0_5', label: '0–5 Years' },
  { value: '5_10', label: '5–10 Years' },
  { value: '10_PLUS', label: '10+ Years' },
];

export interface ImpactMetric {
  figure: string;
  label: string;
  detail: string;
}

export interface BusinessDivision {
  id: string;
  code: string;
  name: string;
  tagline: string;
  description: string;
  highlights: string[];
}

export interface ConsultancyService {
  id: string;
  service_type: string;
  slug: string;
  sort_order: number;
  division_name: string;
  title: string;
  subtitle: string;
  summary: string;
  detailed_description: string;
  services_included: string[];
  owner_benefits: string[];
  deliverables: string[];
  typical_timeline: string;
  image_key: string;
  image_url?: string | null;
  published: boolean;
}

export interface Testimonial {
  id: string;
  quote: string;
  attribution_role: string;
  engagement_context: string;
  sort_order: number;
  published: boolean;
}

export interface ClientProfile {
  id?: string;
  user_id: string;
  full_name: string;
  email: string;
  mobile_number: string;
  company_name: string;
  designation: string;
  city: string;
  business_category: BusinessCategory | string;
  created_at?: string;
  updated_at?: string;
}

export interface ServiceEnquiry {
  id: string;
  reference_code: string;
  user_id: string;
  client_profile_id?: string | null;
  service_id?: string | null;
  service_type: string;
  property_name: string;
  city: string;
  scale_summary: string;
  contact_name: string;
  contact_email: string;
  contact_phone: string;
  brief_notes?: string | null;
  status: 'SUBMITTED' | 'UNDER_REVIEW' | 'PROPOSAL_SHARED' | 'CONVERTED' | 'WITHDRAWN';
  created_at: string;
}

export interface CandidateProfile {
  id?: string;
  user_id: string;
  full_name: string;
  email: string;
  mobile_number: string;
  location: string;
  current_designation: string;
  department: HospitalityDepartment | string;
  total_experience_years: number;
  current_employer: string;
  previous_employers: string;
  education: string;
  skills: string;
  preferred_job_locations: string;
  expected_salary: string;
  availability: CandidateAvailability | string;
  notice_period: string;
  job_alerts_enabled: boolean;
  cv_file_name?: string | null;
  cv_mime_type?: string | null;
  cv_file_size_bytes?: number | null;
  cv_storage_path?: string | null;
  cv_uploaded_at?: string | null;
  updated_at?: string;
}

export interface CandidatePreferences {
  id?: string;
  user_id: string;
  candidate_profile_id?: string | null;
  job_alerts_enabled: boolean;
  preferred_locations: string[];
  preferred_departments: string[];
  preferred_skills: string[];
  min_expected_salary: string;
  availability: string;
  notice_period: string;
  push_notifications_enabled: boolean;
  in_app_notifications_enabled: boolean;
  email_notifications_enabled: boolean;
  application_status_alerts_enabled: boolean;
}

export interface JobVacancy {
  id: string;
  slug: string;
  title: string;
  property_tier: string;
  location: string;
  city_region: string;
  compensation: string;
  experience_required: string;
  min_experience_years: number;
  category: RoleCategory | string;
  summary: string;
  key_mandates: string[];
  desired_skills: string[];
  is_urgent: boolean;
  published: boolean;
  created_at?: string;
}

export type ApplicationStatus =
  | 'APPLIED'
  | 'UNDER_REVIEW'
  | 'SHORTLISTED'
  | 'INTERVIEW_SCHEDULED'
  | 'OFFER_EXTENDED'
  | 'HIRED'
  | 'DECLINED'
  | 'WITHDRAWN';

export interface JobApplication {
  id: string;
  user_id: string;
  candidate_profile_id?: string | null;
  vacancy_id?: string | null;
  role_id: string;
  role_title: string;
  location: string;
  candidate_name: string;
  candidate_email: string;
  candidate_phone: string;
  years_experience: number;
  current_property: string;
  cover_note?: string | null;
  cv_file_name?: string | null;
  cv_storage_path?: string | null;
  status: ApplicationStatus;
  created_at: string;
  updated_at?: string;
}

export interface SavedJob {
  id: string;
  user_id: string;
  vacancy_id?: string | null;
  role_id: string;
  role_title: string;
  location: string;
  compensation: string;
  saved_at: string;
}

export interface CandidateNotification {
  id: string;
  user_id: string;
  vacancy_id?: string | null;
  role_id: string;
  title: string;
  message: string;
  department: string;
  location: string;
  is_read: boolean;
  created_at: string;
}
