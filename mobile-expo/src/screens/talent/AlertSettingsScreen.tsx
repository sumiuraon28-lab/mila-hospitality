import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { Alert, Platform, Pressable, StyleSheet, Switch, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList, TalentTabParamList } from '../../navigation/types';
import {
  splitCsv,
  upsertCandidatePreferences,
  upsertCandidateProfile,
} from '../../services/milaApi';
import { MilaColors, MilaFonts, MilaRadii, MilaSpacing } from '../../theme/milaTheme';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<TalentTabParamList, 'AlertSettings'>,
  NativeStackNavigationProp<TalentStackParamList>
>;

export const AlertSettingsScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const {
    user,
    candidateProfile,
    candidatePreferences,
    notifications,
    vacancies,
    pushStatus,
    markNotificationRead,
    syncPushNotifications,
    refreshAccountData,
  } = useMilaApp();

  const [jobAlertsEnabled, setJobAlertsEnabled] = useState(true);
  const [pushEnabled, setPushEnabled] = useState(true);
  const [inAppEnabled, setInAppEnabled] = useState(true);
  const [emailEnabled, setEmailEnabled] = useState(false);
  const [appStatusEnabled, setAppStatusEnabled] = useState(true);
  const [saving, setSaving] = useState(false);
  const [refreshing, setRefreshing] = useState(false);

  useEffect(() => {
    setJobAlertsEnabled(
      candidatePreferences?.job_alerts_enabled ?? candidateProfile?.job_alerts_enabled ?? true
    );
    setPushEnabled(candidatePreferences?.push_notifications_enabled ?? true);
    setInAppEnabled(candidatePreferences?.in_app_notifications_enabled ?? true);
    setEmailEnabled(candidatePreferences?.email_notifications_enabled ?? false);
    setAppStatusEnabled(candidatePreferences?.application_status_alerts_enabled ?? true);
  }, [candidatePreferences, candidateProfile]);

  const handleSaveAlertSettings = async () => {
    if (!user || !candidateProfile) {
      Alert.alert('Profile Required', 'Save your candidate profile first to sync alert preferences.');
      return;
    }

    setSaving(true);
    try {
      await upsertCandidateProfile({
        ...candidateProfile,
        job_alerts_enabled: jobAlertsEnabled,
      });

      await upsertCandidatePreferences({
        user_id: user.id,
        candidate_profile_id: candidateProfile.id ?? null,
        job_alerts_enabled: jobAlertsEnabled,
        preferred_locations:
          candidatePreferences?.preferred_locations ??
          splitCsv(candidateProfile.preferred_job_locations || candidateProfile.location),
        preferred_departments:
          candidatePreferences?.preferred_departments ?? [candidateProfile.department],
        preferred_skills:
          candidatePreferences?.preferred_skills ?? splitCsv(candidateProfile.skills),
        min_expected_salary: candidateProfile.expected_salary,
        availability: candidateProfile.availability,
        notice_period: candidateProfile.notice_period,
        push_notifications_enabled: pushEnabled,
        in_app_notifications_enabled: inAppEnabled,
        email_notifications_enabled: emailEnabled,
        application_status_alerts_enabled: appStatusEnabled,
      });

      const tokenResult = await syncPushNotifications(jobAlertsEnabled && pushEnabled);
      await refreshAccountData();
      Alert.alert(
        'Alert Preferences Saved',
        tokenResult?.message ?? 'Your notification channels have been updated in MILA Supabase.'
      );
    } catch (err) {
      Alert.alert('Update Failed', err instanceof Error ? err.message : 'Unable to save settings.');
    } finally {
      setSaving(false);
    }
  };

  const handleRefresh = async () => {
    setRefreshing(true);
    await refreshAccountData();
    setRefreshing(false);
  };

  const renderToggleRow = (
    title: string,
    subtitle: string,
    value: boolean,
    onChange: (next: boolean) => void
  ) => (
    <View style={styles.toggleRow}>
      <View style={styles.toggleCopy}>
        <Text style={styles.toggleTitle}>{title}</Text>
        <Text style={styles.toggleSub}>{subtitle}</Text>
      </View>
      <Switch
        value={value}
        onValueChange={onChange}
        trackColor={{ false: MilaColors.charcoalBorder, true: MilaColors.goldMuted }}
        thumbColor={value ? MilaColors.champagneGold : MilaColors.warmStone}
      />
    </View>
  );

  return (
    <MilaScreenShell showBrandHeader refreshing={refreshing} onRefresh={handleRefresh}>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="PUSH NOTIFICATIONS & ALERTS"
        title="Job Alerts & Inbox"
        subtitle="MILA only notifies you for vacancies matching your department, preferred cities, experience, and skills—never irrelevant broadcasts."
      />

      {!user ? (
        <MilaCard highlighted>
          <Text style={styles.cardTitle}>Sign In for Targeted Job Alerts</Text>
          <Text style={styles.cardSub}>
            Enable Android FCM and iOS APNs push notifications, in-app alerts, and real-time
            application status updates.
          </Text>
          <MilaButton
            label="Candidate Login"
            icon="log-in-outline"
            onPress={() => navigation.navigate('CandidateLogin')}
          />
        </MilaCard>
      ) : (
        <>
          {/* Push Notification Device Sync Status */}
          <MilaCard highlighted>
            <View style={styles.pushHeader}>
              <View style={styles.pushTitleRow}>
                <Ionicons name="notifications-outline" size={20} color={MilaColors.champagneGold} />
                <Text style={styles.cardTitle}>
                  {Platform.OS === 'ios' ? 'iOS APNs Push Status' : 'Android FCM Push Status'}
                </Text>
              </View>
              <MilaStatusBadge
                label={pushStatus?.syncedToSupabase ? 'SYNCED' : 'DEVICE READY'}
                tone={pushStatus?.syncedToSupabase ? 'emerald' : 'gold'}
              />
            </View>
            <Text style={styles.cardSub}>
              {pushStatus?.message ??
                'Enable Mobile Push Alerts below to request device notification permission and register your push token with MILA.'}
            </Text>

            <View style={styles.divider} />

            {renderToggleRow(
              'I AM LOOKING FOR A JOB (Active Alerts)',
              'Master switch controlling whether MILA matches you for newly published vacancies.',
              jobAlertsEnabled,
              setJobAlertsEnabled
            )}
            {renderToggleRow(
              Platform.OS === 'ios' ? 'iOS Push Notifications (APNs)' : 'Android Push Notifications (FCM)',
              'Instant lock-screen alerts for matched vacancies and interview updates.',
              pushEnabled,
              setPushEnabled
            )}
            {renderToggleRow(
              'In-App Notifications',
              'Show matched roles and recruiter updates in your MILA Talent inbox.',
              inAppEnabled,
              setInAppEnabled
            )}
            {renderToggleRow(
              'Optional Email Notifications',
              'Receive email summaries for matched mandates and recruitment stage updates.',
              emailEnabled,
              setEmailEnabled
            )}
            {renderToggleRow(
              'Application Status Notifications',
              'Instant alerts when your status changes (Shortlisted, Interview, Offer).',
              appStatusEnabled,
              setAppStatusEnabled
            )}

            <View style={styles.mtMd}>
              <MilaButton
                label="Save & Sync Notification Channels"
                icon="checkmark-circle-outline"
                loading={saving}
                onPress={handleSaveAlertSettings}
              />
            </View>
          </MilaCard>

          {/* In-App Notifications Inbox */}
          <MilaSectionHeading
            eyebrow="YOUR IN-APP ALERTS"
            title={`Notifications (${notifications.length})`}
          />

          {notifications.length === 0 ? (
            <MilaCard>
              <Text style={styles.emptyText}>
                No job alerts yet. When MILA publishes a vacancy matching your department, location,
                and experience, it will appear right here and via push notification.
              </Text>
            </MilaCard>
          ) : (
            notifications.map((notif) => {
              const matchedVacancy = vacancies.find(
                (v) => v.id === notif.vacancy_id || v.slug === notif.role_id || v.id === notif.role_id
              );
              return (
                <Pressable
                  key={notif.id}
                  accessibilityRole="button"
                  onPress={async () => {
                    if (!notif.is_read) {
                      await markNotificationRead(notif.id).catch(() => undefined);
                    }
                    if (matchedVacancy) {
                      navigation.navigate('JobDetail', { vacancy: matchedVacancy });
                    }
                  }}
                >
                  <MilaCard highlighted={!notif.is_read}>
                    <View style={styles.notifTop}>
                      <MilaStatusBadge
                        label={notif.is_read ? 'READ' : 'NEW MATCH'}
                        tone={notif.is_read ? 'neutral' : 'gold'}
                      />
                      <Text style={styles.notifDate}>
                        {new Date(notif.created_at).toLocaleDateString()}
                      </Text>
                    </View>
                    <Text style={styles.notifTitle}>{notif.title}</Text>
                    <Text style={styles.notifMessage}>{notif.message}</Text>
                    {notif.location ? (
                      <Text style={styles.notifMeta}>
                        {notif.department ? `${notif.department} • ` : ''}
                        {notif.location}
                      </Text>
                    ) : null}
                  </MilaCard>
                </Pressable>
              );
            })
          )}
        </>
      )}
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  pushHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    gap: 8,
    marginBottom: 8,
  },
  pushTitleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    flex: 1,
  },
  cardTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  cardSub: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginBottom: 12,
  },
  divider: {
    height: StyleSheet.hairlineWidth,
    backgroundColor: MilaColors.charcoalBorder,
    marginVertical: 8,
  },
  toggleRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    paddingVertical: 10,
    gap: 12,
  },
  toggleCopy: {
    flex: 1,
  },
  toggleTitle: {
    fontSize: 14,
    fontWeight: '700',
    color: MilaColors.ivory,
  },
  toggleSub: {
    fontSize: 12,
    lineHeight: 17,
    color: MilaColors.warmStone,
    marginTop: 2,
  },
  mtMd: {
    marginTop: MilaSpacing.md,
  },
  notifTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'center',
    marginBottom: 8,
  },
  notifDate: {
    fontSize: 11,
    color: MilaColors.warmStone,
  },
  notifTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 17,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  notifMessage: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginTop: 4,
  },
  notifMeta: {
    fontSize: 12,
    color: MilaColors.champagneGold,
    marginTop: 8,
    fontWeight: '600',
  },
  emptyText: {
    fontSize: 13,
    lineHeight: 20,
    color: MilaColors.ivoryMuted,
    borderRadius: MilaRadii.md,
  },
});
