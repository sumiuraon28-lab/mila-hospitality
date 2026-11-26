import { ImageSourcePropType } from 'react-native';
import {
  BusinessDivision,
  ConsultancyService,
  ImpactMetric,
  JobVacancy,
  Testimonial,
} from '../types/mila';

export const MILA_IMAGES: Record<string, ImageSourcePropType> = {
  hero_hotel: require('../../assets/images/hero-hotel.jpg'),
  palace_resort: require('../../assets/images/palace-resort.jpg'),
  dining_interior: require('../../assets/images/fine-dining.jpg'),
  mixology_bar: require('../../assets/images/mixology-bar.jpg'),
  hospitality_training: require('../../assets/images/hospitality-training.jpg'),
  operations_mgmt: require('../../assets/images/management-solutions.jpg'),
  talent_recruitment: require('../../assets/images/talent-recruitment.jpg'),
  concept_dev: require('../../assets/images/concept-design.jpg'),
  management_solutions: require('../../assets/images/management-solutions.jpg'),
};

export const MILA_LOGO_ICON: ImageSourcePropType = require('../../assets/icon.png');
export const MILA_SPLASH_ART: ImageSourcePropType = require('../../assets/splash.png');

export function resolveMilaImage(imageKey?: string | null): ImageSourcePropType {
  if (imageKey && MILA_IMAGES[imageKey]) {
    return MILA_IMAGES[imageKey];
  }
  return MILA_IMAGES.palace_resort;
}

export const IMPACT_METRICS: ImpactMetric[] = [
  { figure: '15+', label: 'Years Experience', detail: 'Indian & International Hospitality' },
  { figure: '30+', label: 'Pre-Opening Projects', detail: 'Hotels, Resorts & Heritage Properties' },
  { figure: '20+', label: 'Café & Restaurant Setups', detail: 'Fine Dining, Roasteries & Lounges' },
  { figure: '3×', label: 'Champion Mixologist', detail: 'Award-Winning Beverage Specialist' },
];

export const BUSINESS_DIVISIONS: BusinessDivision[] = [
  {
    id: 'div_consultancy',
    code: '01',
    name: 'MILA Consultancy',
    tagline: 'Pre-Openings, Café Setups & Turnkey Advisory',
    description:
      'End-to-end advisory for hotel and resort pre-openings (30+ projects), café and restaurant launches (20+ setups), SOP architecture, and operational profitability.',
    highlights: [
      'Turnkey hotel & resort pre-opening execution',
      'Café, bakery & restaurant concept setup',
      'P&L audits & operational SOP frameworks',
    ],
  },
  {
    id: 'div_talent',
    code: '02',
    name: 'MILA Talent',
    tagline: 'Executive Search & Hospitality Recruitment',
    description:
      'Dedicated hospitality recruitment connecting Indian hotels, luxury resorts, and dining groups with vetted General Managers, Chefs, Mixologists, and opening brigades.',
    highlights: [
      'Executive leadership & HOD placements',
      'Pre-opening taskforce & brigade mobilization',
      'Dedicated candidate registration & career portal',
    ],
  },
  {
    id: 'div_academy',
    code: '03',
    name: 'MILA Academy & Expertise',
    tagline: 'Specialist Mixology, Training & Service Mastery',
    description:
      'Led by an award-winning three-time champion mixologist—delivering signature beverage curation, bar engineering, service training academies, and culinary calibration.',
    highlights: [
      'Three-time champion mixologist beverage labs',
      'Front-of-house, barista & bartender upskilling',
      'Pre-opening service choreography & standards',
    ],
  },
];

export const LEADERSHIP_PRINCIPLES: Array<{ title: string; detail: string }> = [
  {
    title: 'Owner-Centric Commercial Rigor',
    detail:
      'Every concept, staffing structure, and menu decision is aligned with sustainable revenue and margin discipline.',
  },
  {
    title: 'Pre-Opening Discipline',
    detail:
      'Structured critical-path timelines, OS&E advisory, and live service rehearsals before welcoming the first guest.',
  },
  {
    title: 'Specialist Mixology Pedigree',
    detail:
      'Guided by an award-winning three-time champion mixologist uniting beverage theatre with strict pour-cost controls.',
  },
  {
    title: 'Dedicated Hospitality Recruitment',
    detail:
      'Competency-assessed executive search and pre-opening talent mobilization across India.',
  },
];

export const SERVICE_REGIONS: Array<{ region: string; focus: string }> = [
  {
    region: 'Mumbai & Western India',
    focus: 'Luxury urban hotels, fine dining, craft cocktail lounges, and café launches.',
  },
  {
    region: 'New Delhi & NCR',
    focus: 'Upper-upscale hospitality advisory, diplomatic banqueting, and executive search.',
  },
  {
    region: 'Bengaluru & South India',
    focus: 'Specialty coffee concepts, roasteries, business hotels, and multi-unit F&B.',
  },
  {
    region: 'Rajasthan, Goa & Leisure Corridors',
    focus: 'Heritage resort pre-openings, destination retreats, and seasonal brigades.',
  },
];

export const FALLBACK_TESTIMONIALS: Testimonial[] = [
  {
    id: 'test_1',
    quote:
      'MILA brought calm, structured pre-opening discipline to our resort launch—aligning kitchen design, service SOPs, and department head hiring ahead of schedule.',
    attribution_role: 'Managing Partner',
    engagement_context: 'Luxury Heritage Resort Pre-Opening • Rajasthan',
    sort_order: 1,
    published: true,
  },
  {
    id: 'test_2',
    quote:
      'From espresso bar ergonomics to food-cost calibration and opening staff training, their café setup methodology helped us achieve operational stability from week one.',
    attribution_role: 'Founder',
    engagement_context: 'Specialty Café & Bakery Concept • Bengaluru',
    sort_order: 2,
    published: true,
  },
  {
    id: 'test_3',
    quote:
      'Their beverage program and bar-team training transformed both our guest experience and pour-cost discipline across our evening lounge.',
    attribution_role: 'Director of Operations',
    engagement_context: 'Fine-Dining & Cocktail Lounge • Mumbai',
    sort_order: 3,
    published: true,
  },
];

export const FALLBACK_SERVICES: ConsultancyService[] = [
  {
    id: 'srv-1',
    service_type: 'HOSPITALITY_PRE_OPENING',
    slug: 'hospitality-pre-opening-consultancy',
    sort_order: 1,
    division_name: 'MILA Consultancy',
    title: 'Hospitality Pre-Opening Consultancy',
    subtitle: 'Turnkey Critical Path & Opening Governance',
    summary:
      'Complete pre-opening orchestration for luxury hotels, heritage properties, and destination resorts across India.',
    detailed_description:
      'Drawing on 30+ hospitality pre-opening mandates across India, MILA stewards every phase from architectural handover and OS&E procurement to staffing, trial runs, and commercial launch.',
    services_included: [
      'Master pre-opening critical path & budget control',
      'OS&E / FF&E specification & vendor audits',
      'Departmental SOP creation & dry-run commissioning',
    ],
    owner_benefits: [
      'Eliminate costly launch delays and CAPEX overruns',
      'Achieve brand-standard guest readiness from Day 1',
    ],
    deliverables: [
      'Master Pre-Opening Critical Path Tracker',
      'Complete Departmental SOP & Quality Manual',
      'Opening Readiness & Trial-Run Audit Report',
    ],
    typical_timeline: '4 – 12 Months',
    image_key: 'palace_resort',
    published: true,
  },
  {
    id: 'srv-2',
    service_type: 'RESTAURANT_CAFE_DEVELOPMENT',
    slug: 'restaurant-cafe-development',
    sort_order: 2,
    division_name: 'MILA Consultancy',
    title: 'Restaurant and Café Development',
    subtitle: 'Culinary Architecture, Kitchen Flow & Launch',
    summary:
      'Commercial restaurant, specialty coffee, and dining setups built for repeatable margins and guest retention.',
    detailed_description:
      'Across 20+ café and restaurant setups, MILA bridges culinary creativity with back-of-house ergonomics, recipe costing, vendor sourcing, and launch execution.',
    services_included: [
      'Kitchen & café bar ergonomic workflow planning',
      'Recipe standardization & food-cost engineering',
      'Vendor onboarding & opening brigade training',
    ],
    owner_benefits: [
      'Disciplined food-cost ratios from opening month',
      'Faster table turns without compromising luxury service',
    ],
    deliverables: [
      'BOH & FOH Space Allocation Blueprint',
      'Standardized Recipe & Margin Matrix',
      'Launch Playbook & Opening Checklist',
    ],
    typical_timeline: '2 – 6 Months',
    image_key: 'dining_interior',
    published: true,
  },
  {
    id: 'srv-3',
    service_type: 'BAR_BEVERAGE_CONSULTANCY',
    slug: 'bar-beverage-consultancy',
    sort_order: 3,
    division_name: 'MILA Academy & Expertise',
    title: 'Bar and Beverage Consultancy',
    subtitle: 'Award-Winning Three-Time Champion Mixology',
    summary:
      'Signature craft cocktail programs, ergonomic speed-bar design, and high-margin beverage curation.',
    detailed_description:
      'Led by our three-time champion mixologist, MILA designs bespoke beverage identities, zero-waste prep laboratories, glassware standards, and profitable pour-cost controls.',
    services_included: [
      'Signature cocktail & zero-proof menu engineering',
      'Speed-bar station design & ice program setup',
      'Pour-cost control, inventory & bartender mastery',
    ],
    owner_benefits: [
      'Higher beverage check averages and repeat destination traffic',
      'Consistent execution across shifts with reduced wastage',
    ],
    deliverables: [
      'Signature Beverage Bible & Spec Cards',
      'Ergonomic Bar Station Layout Plan',
      'Beverage Costing & Inventory SOPs',
    ],
    typical_timeline: '6 – 12 Weeks',
    image_key: 'mixology_bar',
    published: true,
  },
  {
    id: 'srv-4',
    service_type: 'HOSPITALITY_TRAINING',
    slug: 'hospitality-training',
    sort_order: 4,
    division_name: 'MILA Academy & Expertise',
    title: 'Hospitality Training',
    subtitle: 'Service Choreography & Academy Programs',
    summary:
      'Structured luxury service academies, culinary/bar masterclasses, and leadership readiness modules.',
    detailed_description:
      'MILA Academy equips opening and established hospitality teams with refined service rituals, upselling frameworks, mixology technique, and supervisory accountability.',
    services_included: [
      'FOH luxury service choreography & guest recovery',
      'Mixology & sommelier craft intensives',
      'Supervisor & HOD leadership workshops',
    ],
    owner_benefits: [
      'Elevated guest satisfaction scores and review ratings',
      'Lower staff attrition through structured skill growth',
    ],
    deliverables: [
      'Role-Based Training Curriculum & Handbooks',
      'Practical Skill Assessment Scorecards',
      'Post-Training Mystery Audit Summary',
    ],
    typical_timeline: '2 – 8 Weeks',
    image_key: 'hospitality_training',
    published: true,
  },
  {
    id: 'srv-5',
    service_type: 'OPERATIONS_MANAGEMENT',
    slug: 'operations-management',
    sort_order: 5,
    division_name: 'MILA Consultancy',
    title: 'Operations Management',
    subtitle: 'Margin Optimization & SOP Turnaround',
    summary:
      'Hands-on operational diagnostics, P&L restructuring, and ongoing quality governance for live properties.',
    detailed_description:
      'We audit live hotels, restaurants, and lounges to uncover leakage in food/beverage cost, payroll productivity, procurement, and guest experience consistency.',
    services_included: [
      '360-degree operational & P&L diagnostic',
      'Procurement renegotiation & wastage controls',
      'Monthly mystery audits & KPI dashboards',
    ],
    owner_benefits: [
      'Measurable GOP and EBITDA improvement',
      'Consistent multi-outlet brand execution',
    ],
    deliverables: [
      'Operational Diagnostic & Turnaround Roadmap',
      'Weekly KPI & Cost Control Templates',
      'Quarterly Quality Governance Review',
    ],
    typical_timeline: '3 – 12 Months',
    image_key: 'operations_mgmt',
    published: true,
  },
  {
    id: 'srv-6',
    service_type: 'RECRUITMENT_TALENT_ACQUISITION',
    slug: 'recruitment-talent-acquisition',
    sort_order: 6,
    division_name: 'MILA Talent',
    title: 'Recruitment and Talent Acquisition',
    subtitle: 'Pan-India Hospitality Executive Search',
    summary:
      'Confidential recruitment for General Managers, Executive Chefs, Head Mixologists, and complete opening brigades.',
    detailed_description:
      'MILA Talent pairs deep industry assessment with confidential executive headhunting across luxury hotels, heritage resorts, and high-volume dining groups.',
    services_included: [
      'Executive search for GM, Culinary, F&B & Mixology heads',
      'Complete pre-opening manpower budgeting & hiring',
      'Technical trade tests & reference verification',
    ],
    owner_benefits: [
      'Faster time-to-hire with operationally vetted leaders',
      'Stronger cultural and P&L alignment for Indian assets',
    ],
    deliverables: [
      'Manpower Grid & Compensation Benchmark',
      'Vetted Candidate Shortlist & Assessment Dossiers',
      'Onboarding & Retention Plan',
    ],
    typical_timeline: '3 – 8 Weeks',
    image_key: 'talent_recruitment',
    published: true,
  },
  {
    id: 'srv-7',
    service_type: 'BUSINESS_CONCEPT_DEVELOPMENT',
    slug: 'business-concept-development',
    sort_order: 7,
    division_name: 'MILA Consultancy',
    title: 'Business and Concept Development',
    subtitle: 'Feasibility, Brand DNA & Financial Modeling',
    summary:
      'Data-backed concept positioning, market feasibility, and 5-year CAPEX-to-ROI financial models.',
    detailed_description:
      'Before capital is committed, MILA validates market demand, competitive whitespace, pricing architecture, and investment payback for new hospitality ventures.',
    services_included: [
      'Location & competitive market feasibility',
      'Brand narrative, guest journey & positioning DNA',
      '5-year CAPEX, OPEX & ROI financial modeling',
    ],
    owner_benefits: [
      'De-risked capital allocation for owners and investors',
      'Investor-ready pitch deck and operational roadmap',
    ],
    deliverables: [
      'Market Feasibility & Positioning Report',
      '5-Year Financial Model & Sensitivity Analysis',
      'Concept Identity & Guest Journey Dossier',
    ],
    typical_timeline: '4 – 10 Weeks',
    image_key: 'concept_dev',
    published: true,
  },
  {
    id: 'srv-8',
    service_type: 'HOSPITALITY_MANAGEMENT_SOLUTIONS',
    slug: 'hospitality-management-solutions',
    sort_order: 8,
    division_name: 'MILA Consultancy',
    title: 'Hospitality Management Solutions',
    subtitle: 'Owner Representation & Asset Stewardship',
    summary:
      'Interim operational leadership, operator selection, and long-term asset management for hospitality owners.',
    detailed_description:
      'MILA represents owners across brand negotiations, HMA reviews, interim GM placement, and ongoing asset yield optimization.',
    services_included: [
      'Hotel Management Agreement (HMA) advisory',
      'Interim leadership & asset management oversight',
      'Annual budget review & capital preservation',
    ],
    owner_benefits: [
      'Protected owner returns and transparent reporting',
      'Accountable operator governance across every quarter',
    ],
    deliverables: [
      'Owner Asset Stewardship Charter',
      'Monthly Financial & Yield Review Pack',
      'Annual Strategic Capital Plan',
    ],
    typical_timeline: '6 – 24 Months',
    image_key: 'management_solutions',
    published: true,
  },
];

export const FALLBACK_VACANCIES: JobVacancy[] = [
  {
    id: 'role_gm_udaipur',
    slug: 'pre-opening-general-manager-udaipur',
    title: 'Pre-Opening General Manager',
    property_tier: 'Luxury Heritage Resort (Pre-Opening)',
    location: 'Udaipur, Rajasthan',
    city_region: 'Udaipur & Rajasthan',
    compensation: 'Competitive Executive Package',
    experience_required: '14+ Years (Luxury GM)',
    min_experience_years: 14,
    category: 'PRE_OPENING',
    summary:
      'Lead pre-opening critical path, owner-operator alignment, luxury positioning, and full department head mobilization.',
    key_mandates: [
      'Own pre-opening readiness timeline and operational budgets',
      'Recruit and mentor Heads of Department',
      'Establish luxury service standards across all guest touchpoints',
    ],
    desired_skills: ['Pre-Opening Governance', 'Luxury Resort P&L', 'Owner Relations', 'HOD Leadership'],
    is_urgent: true,
    published: true,
  },
  {
    id: 'role_exec_chef_mumbai',
    slug: 'executive-chef-modern-indian-mumbai',
    title: 'Executive Chef — Modern Indian',
    property_tier: '5-Star Urban Hotel & Fine Dining',
    location: 'Mumbai, Maharashtra',
    city_region: 'Mumbai',
    compensation: 'Senior Culinary Compensation',
    experience_required: '12+ Years',
    min_experience_years: 12,
    category: 'CULINARY',
    summary:
      'Spearhead culinary direction across a contemporary Indian fine-dining restaurant, banqueting, and all-day dining.',
    key_mandates: [
      'Design seasonal regional-Indian tasting menus',
      'Maintain disciplined food-cost and kitchen hygiene controls',
      'Lead and mentor a multi-outlet culinary brigade',
    ],
    desired_skills: ['Modern Indian Gastronomy', 'Menu Engineering', 'Food Cost Control', 'HACCP'],
    is_urgent: true,
    published: true,
  },
  {
    id: 'role_head_mixologist_delhi',
    slug: 'head-mixologist-beverage-manager-delhi',
    title: 'Head Mixologist & Beverage Manager',
    property_tier: 'Upscale Cocktail Lounge & Speakeasy',
    location: 'New Delhi, NCR',
    city_region: 'New Delhi NCR',
    compensation: 'Competitive + Performance Incentives',
    experience_required: '7+ Years',
    min_experience_years: 7,
    category: 'MIXOLOGY',
    summary:
      'Lead progressive cocktail program development, prep-lab execution, and a high-energy bar & floor service team.',
    key_mandates: [
      'Launch seasonal craft cocktail collections',
      'Maintain disciplined pour-cost and inventory accuracy',
      'Conduct ongoing bartender technique & hospitality training',
    ],
    desired_skills: ['Craft Mixology', 'Beverage R&D', 'Pour-Cost Governance', 'Bar Team Training'],
    is_urgent: true,
    published: true,
  },
  {
    id: 'role_fnb_director_blr',
    slug: 'director-of-food-and-beverage-bengaluru',
    title: 'Director of Food & Beverage',
    property_tier: 'Upper-Upscale Full-Service Hotel',
    location: 'Bengaluru, Karnataka',
    city_region: 'Bengaluru',
    compensation: 'Executive Leadership Package',
    experience_required: '10+ Years',
    min_experience_years: 10,
    category: 'FNB_SERVICE',
    summary:
      'Full P&L ownership across multiple dining outlets, banqueting operations, and specialty coffee lounge.',
    key_mandates: [
      'Drive outlet revenue growth and banquet yield',
      'Optimize departmental labor productivity and margins',
      'Champion guest satisfaction and service consistency',
    ],
    desired_skills: ['Multi-Outlet F&B P&L', 'Banquet Revenue', 'Service Choreography'],
    is_urgent: false,
    published: true,
  },
  {
    id: 'role_rooms_manager_goa',
    slug: 'pre-opening-rooms-division-manager-goa',
    title: 'Pre-Opening Rooms Division Manager',
    property_tier: 'Coastal Boutique Resort (Pre-Opening)',
    location: 'North Goa',
    city_region: 'Goa',
    compensation: 'Competitive Resort Package',
    experience_required: '8+ Years',
    min_experience_years: 8,
    category: 'ROOMS',
    summary:
      'Establish villa arrival protocols, guest concierge journeys, and seamless front office & housekeeping workflows.',
    key_mandates: [
      'Train front office, concierge, and housekeeping associates',
      'Implement PMS and luxury arrival service standards',
      'Coordinate pre-opening OS&E linen and guest amenity staging',
    ],
    desired_skills: ['Rooms Division Ops', 'Guest Concierge', 'PMS Setup', 'OS&E Staging'],
    is_urgent: false,
    published: true,
  },
];
