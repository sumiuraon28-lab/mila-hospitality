import { Session, User } from '@supabase/supabase-js';
import React, { createContext, useCallback, useContext, useEffect, useMemo, useState } from 'react';
import {
  FALLBACK_SERVICES,
  FALLBACK_TESTIMONIALS,
  FALLBACK_VACANCIES,
} from '../constants/brandAssets';
import { isSupabaseConfigured, supabase } from '../lib/supabase';
import {
  fetchMyApplications,
  fetchMyCandidatePreferences,
  fetchMyCandidateProfile,
  fetchMyClientProfile,
  fetchMyNotifications,
  fetchMySavedJobs,
  fetchMyServiceEnquiries,
  fetchPublishedServices,
  fetchPublishedTestimonials,
  fetchPublishedVacancies,
  markNotificationAsRead,
  removeSavedJob,
  saveJobToShortlist,
  signOutAccount,
  splitCsv,
  upsertCandidatePreferences,
  upsertCandidateProfile,
} from '../services/milaApi';
import {
  ensureNotificationChannels,
  PushRegistrationStatus,
  registerDevicePushToken,
} from '../services/pushNotifications';
import {
  CandidateNotification,
  CandidatePreferences,
  CandidateProfile,
  ClientProfile,
  ConsultancyService,
  JobApplication,
  JobVacancy,
  PortalMode,
  SavedJob,
  ServiceEnquiry,
  Testimonial,
} from '../types/mila';

interface MilaAppContextValue {
  portalMode: PortalMode;
  setPortalMode: (mode: PortalMode) => void;
  session: Session | null;
  user: User | null;
  initializing: boolean;
  isSupabaseReady: boolean;

  services: ConsultancyService[];
  testimonials: Testimonial[];
  vacancies: JobVacancy[];

  clientProfile: ClientProfile | null;
  enquiries: ServiceEnquiry[];

  candidateProfile: CandidateProfile | null;
  candidatePreferences: CandidatePreferences | null;
  applications: JobApplication[];
  savedJobs: SavedJob[];
  notifications: CandidateNotification[];
  unreadNotificationsCount: number;
  pushStatus: PushRegistrationStatus | null;

  refreshAll: () => Promise<void>;
  refreshAccountData: () => Promise<void>;
  toggleLookingForJob: (nextEnabled: boolean) => Promise<void>;
  toggleSaveJob: (vacancy: JobVacancy) => Promise<boolean>;
  isJobSaved: (vacancy: JobVacancy) => boolean;
  hasAppliedToJob: (vacancy: JobVacancy) => JobApplication | undefined;
  markNotificationRead: (id: string) => Promise<void>;
  syncPushNotifications: (enabled: boolean) => Promise<PushRegistrationStatus | null>;
  signOut: () => Promise<void>;
}

const MilaAppContext = createContext<MilaAppContextValue | undefined>(undefined);

export const MilaAppProvider: React.FC<{ children: React.ReactNode }> = ({ children }) => {
  const [portalMode, setPortalMode] = useState<PortalMode>('CLIENT');
  const [session, setSession] = useState<Session | null>(null);
  const [initializing, setInitializing] = useState(true);

  const [services, setServices] = useState<ConsultancyService[]>(FALLBACK_SERVICES);
  const [testimonials, setTestimonials] = useState<Testimonial[]>(FALLBACK_TESTIMONIALS);
  const [vacancies, setVacancies] = useState<JobVacancy[]>(FALLBACK_VACANCIES);

  const [clientProfile, setClientProfile] = useState<ClientProfile | null>(null);
  const [enquiries, setEnquiries] = useState<ServiceEnquiry[]>([]);

  const [candidateProfile, setCandidateProfile] = useState<CandidateProfile | null>(null);
  const [candidatePreferences, setCandidatePreferences] = useState<CandidatePreferences | null>(
    null
  );
  const [applications, setApplications] = useState<JobApplication[]>([]);
  const [savedJobs, setSavedJobs] = useState<SavedJob[]>([]);
  const [notifications, setNotifications] = useState<CandidateNotification[]>([]);
  const [pushStatus, setPushStatus] = useState<PushRegistrationStatus | null>(null);

  const loadPublicCatalog = useCallback(async () => {
    const [srv, test, vac] = await Promise.all([
      fetchPublishedServices(),
      fetchPublishedTestimonials(),
      fetchPublishedVacancies(),
    ]);
    setServices(srv);
    setTestimonials(test);
    setVacancies(vac);
  }, []);

  const loadAccountDataForUser = useCallback(async (uid: string) => {
    try {
      const [cProfile, cEnquiries, candProfile, candPrefs, candApps, candSaved, candNotifs] =
        await Promise.all([
          fetchMyClientProfile(uid).catch(() => null),
          fetchMyServiceEnquiries(uid).catch(() => []),
          fetchMyCandidateProfile(uid).catch(() => null),
          fetchMyCandidatePreferences(uid).catch(() => null),
          fetchMyApplications(uid).catch(() => []),
          fetchMySavedJobs(uid).catch(() => []),
          fetchMyNotifications(uid).catch(() => []),
        ]);

      setClientProfile(cProfile);
      setEnquiries(cEnquiries);
      setCandidateProfile(candProfile);
      setCandidatePreferences(candPrefs);
      setApplications(candApps);
      setSavedJobs(candSaved);
      setNotifications(candNotifs);

      if (candProfile) {
        const pushEnabled = candPrefs?.push_notifications_enabled ?? candProfile.job_alerts_enabled;
        registerDevicePushToken(uid, pushEnabled)
          .then((status) => setPushStatus(status))
          .catch(() => undefined);
      }
    } catch {
      // Keep existing state gracefully if network is offline
    }
  }, []);

  const clearAccountData = useCallback(() => {
    setClientProfile(null);
    setEnquiries([]);
    setCandidateProfile(null);
    setCandidatePreferences(null);
    setApplications([]);
    setSavedJobs([]);
    setNotifications([]);
    setPushStatus(null);
  }, []);

  useEffect(() => {
    let active = true;
    ensureNotificationChannels().catch(() => undefined);

    async function bootstrap() {
      await loadPublicCatalog();
      if (isSupabaseConfigured) {
        const { data } = await supabase.auth.getSession();
        if (!active) return;
        setSession(data.session);
        if (data.session?.user?.id) {
          await loadAccountDataForUser(data.session.user.id);
        }
      }
      if (active) setInitializing(false);
    }

    bootstrap().catch(() => {
      if (active) setInitializing(false);
    });

    if (!isSupabaseConfigured) {
      return () => {
        active = false;
      };
    }

    const { data: listener } = supabase.auth.onAuthStateChange((_event, nextSession) => {
      if (!active) return;
      setSession(nextSession);
      if (nextSession?.user?.id) {
        loadAccountDataForUser(nextSession.user.id).catch(() => undefined);
      } else {
        clearAccountData();
      }
    });

    return () => {
      active = false;
      listener.subscription.unsubscribe();
    };
  }, [clearAccountData, loadAccountDataForUser, loadPublicCatalog]);

  const refreshAccountData = useCallback(async () => {
    if (session?.user?.id) {
      await loadAccountDataForUser(session.user.id);
    }
  }, [loadAccountDataForUser, session?.user?.id]);

  const refreshAll = useCallback(async () => {
    await loadPublicCatalog();
    if (session?.user?.id) {
      await loadAccountDataForUser(session.user.id);
    }
  }, [loadAccountDataForUser, loadPublicCatalog, session?.user?.id]);

  const toggleLookingForJob = useCallback(
    async (nextEnabled: boolean) => {
      if (!session?.user?.id || !candidateProfile) {
        throw new Error('Please sign in and complete your MILA Talent profile first.');
      }

      const updatedProfile = await upsertCandidateProfile({
        ...candidateProfile,
        job_alerts_enabled: nextEnabled,
      });
      setCandidateProfile(updatedProfile);

      const updatedPrefs = await upsertCandidatePreferences({
        user_id: session.user.id,
        candidate_profile_id: updatedProfile.id ?? null,
        job_alerts_enabled: nextEnabled,
        preferred_locations:
          candidatePreferences?.preferred_locations ??
          splitCsv(updatedProfile.preferred_job_locations || updatedProfile.location),
        preferred_departments:
          candidatePreferences?.preferred_departments ?? [updatedProfile.department],
        preferred_skills:
          candidatePreferences?.preferred_skills ?? splitCsv(updatedProfile.skills),
        min_expected_salary:
          candidatePreferences?.min_expected_salary ?? updatedProfile.expected_salary,
        availability: updatedProfile.availability,
        notice_period: updatedProfile.notice_period,
        push_notifications_enabled:
          candidatePreferences?.push_notifications_enabled ?? nextEnabled,
        in_app_notifications_enabled:
          candidatePreferences?.in_app_notifications_enabled ?? true,
        email_notifications_enabled:
          candidatePreferences?.email_notifications_enabled ?? false,
        application_status_alerts_enabled:
          candidatePreferences?.application_status_alerts_enabled ?? true,
      });
      setCandidatePreferences(updatedPrefs);

      const status = await registerDevicePushToken(
        session.user.id,
        nextEnabled && updatedPrefs.push_notifications_enabled
      );
      setPushStatus(status);
    },
    [candidatePreferences, candidateProfile, session?.user?.id]
  );

  const isJobSaved = useCallback(
    (vacancy: JobVacancy) => {
      const key = vacancy.slug || vacancy.id;
      return savedJobs.some((item) => item.role_id === key || item.vacancy_id === vacancy.id);
    },
    [savedJobs]
  );

  const hasAppliedToJob = useCallback(
    (vacancy: JobVacancy) => {
      const key = vacancy.slug || vacancy.id;
      return applications.find((item) => item.role_id === key || item.vacancy_id === vacancy.id);
    },
    [applications]
  );

  const toggleSaveJob = useCallback(
    async (vacancy: JobVacancy): Promise<boolean> => {
      if (!session?.user?.id) {
        throw new Error('Sign in with your MILA Talent account to save vacancies.');
      }
      const key = vacancy.slug || vacancy.id;
      if (isJobSaved(vacancy)) {
        await removeSavedJob(session.user.id, key);
        setSavedJobs((prev) => prev.filter((item) => item.role_id !== key));
        return false;
      }
      const created = await saveJobToShortlist(session.user.id, vacancy);
      setSavedJobs((prev) => [created, ...prev.filter((item) => item.role_id !== key)]);
      return true;
    },
    [isJobSaved, session?.user?.id]
  );

  const markNotificationRead = useCallback(async (id: string) => {
    await markNotificationAsRead(id);
    setNotifications((prev) =>
      prev.map((item) => (item.id === id ? { ...item, is_read: true } : item))
    );
  }, []);

  const syncPushNotifications = useCallback(
    async (enabled: boolean) => {
      if (!session?.user?.id) return null;
      const status = await registerDevicePushToken(session.user.id, enabled);
      setPushStatus(status);
      return status;
    },
    [session?.user?.id]
  );

  const signOut = useCallback(async () => {
    await signOutAccount();
    clearAccountData();
  }, [clearAccountData]);

  const unreadNotificationsCount = useMemo(
    () => notifications.filter((n) => !n.is_read).length,
    [notifications]
  );

  const value = useMemo<MilaAppContextValue>(
    () => ({
      portalMode,
      setPortalMode,
      session,
      user: session?.user ?? null,
      initializing,
      isSupabaseReady: isSupabaseConfigured,
      services,
      testimonials,
      vacancies,
      clientProfile,
      enquiries,
      candidateProfile,
      candidatePreferences,
      applications,
      savedJobs,
      notifications,
      unreadNotificationsCount,
      pushStatus,
      refreshAll,
      refreshAccountData,
      toggleLookingForJob,
      toggleSaveJob,
      isJobSaved,
      hasAppliedToJob,
      markNotificationRead,
      syncPushNotifications,
      signOut,
    }),
    [
      applications,
      candidatePreferences,
      candidateProfile,
      clientProfile,
      enquiries,
      hasAppliedToJob,
      initializing,
      isJobSaved,
      markNotificationRead,
      notifications,
      portalMode,
      pushStatus,
      refreshAccountData,
      refreshAll,
      savedJobs,
      services,
      session,
      signOut,
      syncPushNotifications,
      testimonials,
      toggleLookingForJob,
      toggleSaveJob,
      unreadNotificationsCount,
      vacancies,
    ]
  );

  return <MilaAppContext.Provider value={value}>{children}</MilaAppContext.Provider>;
};

export function useMilaApp(): MilaAppContextValue {
  const ctx = useContext(MilaAppContext);
  if (!ctx) {
    throw new Error('useMilaApp must be used inside MilaAppProvider');
  }
  return ctx;
}
