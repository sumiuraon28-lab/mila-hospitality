import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { Alert, Pressable, StyleSheet, Text, View } from 'react-native';
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
import { removeSavedJob, withdrawJobApplication } from '../../services/milaApi';
import { MilaColors, MilaFonts, MilaRadii } from '../../theme/milaTheme';
import { ApplicationStatus } from '../../types/mila';

type TabNav = CompositeNavigationProp<
  BottomTabNavigationProp<TalentTabParamList, 'Applications'>,
  NativeStackNavigationProp<TalentStackParamList>
>;

const PIPELINE_STAGES: Array<{ key: ApplicationStatus; label: string }> = [
  { key: 'APPLIED', label: 'Applied' },
  { key: 'UNDER_REVIEW', label: 'Review' },
  { key: 'SHORTLISTED', label: 'Shortlisted' },
  { key: 'INTERVIEW_SCHEDULED', label: 'Interview' },
  { key: 'OFFER_EXTENDED', label: 'Offer' },
  { key: 'HIRED', label: 'Hired' },
];

function stageIndex(status: ApplicationStatus): number {
  return PIPELINE_STAGES.findIndex((s) => s.key === status);
}

function statusTone(
  status: ApplicationStatus
): 'gold' | 'emerald' | 'amber' | 'terracotta' | 'neutral' {
  switch (status) {
    case 'HIRED':
    case 'OFFER_EXTENDED':
      return 'emerald';
    case 'INTERVIEW_SCHEDULED':
    case 'SHORTLISTED':
      return 'gold';
    case 'UNDER_REVIEW':
      return 'amber';
    case 'DECLINED':
    case 'WITHDRAWN':
      return 'terracotta';
    default:
      return 'neutral';
  }
}

export const ApplicationsScreen: React.FC = () => {
  const navigation = useNavigation<TabNav>();
  const { user, applications, savedJobs, vacancies, refreshAccountData } = useMilaApp();
  const [refreshing, setRefreshing] = useState(false);

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshAccountData();
    setRefreshing(false);
  };

  const handleWithdraw = (applicationId: string, roleTitle: string) => {
    Alert.alert(
      'Withdraw Application',
      `Are you sure you want to withdraw your application for ${roleTitle}?`,
      [
        { text: 'Cancel', style: 'cancel' },
        {
          text: 'Withdraw',
          style: 'destructive',
          onPress: async () => {
            try {
              await withdrawJobApplication(applicationId);
              await refreshAccountData();
            } catch (err) {
              Alert.alert('Error', err instanceof Error ? err.message : 'Unable to withdraw.');
            }
          },
        },
      ]
    );
  };

  return (
    <MilaScreenShell
      showBrandHeader
      refreshing={refreshing}
      onRefresh={onRefresh}
      rightHeaderAction={
        <Pressable
          accessibilityRole="button"
          onPress={() => navigation.navigate('SavedJobs')}
          style={styles.savedPill}
        >
          <Ionicons name="bookmark" size={14} color={MilaColors.champagneGold} />
          <Text style={styles.savedPillText}>{`Saved (${savedJobs.length})`}</Text>
        </Pressable>
      }
    >
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="RECRUITMENT TRACKER"
        title="Job Applications & Status"
        subtitle="Track live recruitment progress across every MILA Talent mandate you have applied to."
      />

      {!user ? (
        <MilaCard highlighted>
          <Text style={styles.emptyTitle}>Sign In to Track Applications</Text>
          <Text style={styles.emptyBody}>
            Sign in with your MILA Talent account to monitor interview invitations, recruiter
            reviews, and saved hospitality roles.
          </Text>
          <MilaButton
            label="Candidate Login"
            icon="log-in-outline"
            onPress={() => navigation.navigate('CandidateLogin')}
          />
        </MilaCard>
      ) : applications.length === 0 ? (
        <MilaCard>
          <Text style={styles.emptyTitle}>No Active Applications Yet</Text>
          <Text style={styles.emptyBody}>
            Explore open roles across India's luxury hotels, resorts, fine-dining restaurants, and
            bars to submit your first application.
          </Text>
          <MilaButton
            label="Search Open Vacancies"
            icon="search-outline"
            onPress={() => navigation.navigate('JobSearch')}
          />
        </MilaCard>
      ) : (
        applications.map((app) => {
          const idx = stageIndex(app.status);
          const isTerminalClosed = app.status === 'DECLINED' || app.status === 'WITHDRAWN';
          const matchedVacancy = vacancies.find(
            (v) => v.id === app.vacancy_id || v.slug === app.role_id || v.id === app.role_id
          );

          return (
            <MilaCard key={app.id} highlighted={!isTerminalClosed}>
              <View style={styles.topRow}>
                <MilaStatusBadge
                  label={app.status.replace(/_/g, ' ')}
                  tone={statusTone(app.status)}
                />
                <Text style={styles.dateText}>
                  {new Date(app.created_at).toLocaleDateString()}
                </Text>
              </View>

              <Text style={styles.roleTitle}>{app.role_title}</Text>
              <Text style={styles.roleMeta}>{app.location}</Text>
              {app.cv_file_name ? (
                <Text style={styles.cvAttached}>Private CV: {app.cv_file_name}</Text>
              ) : null}

              {/* 6-Stage Progress Tracker */}
              {!isTerminalClosed ? (
                <View style={styles.stepperWrap}>
                  {PIPELINE_STAGES.map((stage, i) => {
                    const reached = idx >= i;
                    return (
                      <View key={stage.key} style={styles.stepItem}>
                        <View style={[styles.stepDot, reached && styles.stepDotReached]} />
                        <Text
                          style={[styles.stepLabel, reached && styles.stepLabelReached]}
                          numberOfLines={1}
                        >
                          {stage.label}
                        </Text>
                      </View>
                    );
                  })}
                </View>
              ) : null}

              <View style={styles.actionsRow}>
                {matchedVacancy ? (
                  <MilaButton
                    label="View Vacancy"
                    variant="outlined"
                    onPress={() => navigation.navigate('JobDetail', { vacancy: matchedVacancy })}
                    style={styles.flexBtn}
                  />
                ) : null}
                {!isTerminalClosed && app.status !== 'HIRED' ? (
                  <MilaButton
                    label="Withdraw"
                    variant="danger"
                    onPress={() => handleWithdraw(app.id, app.role_title)}
                    style={styles.flexBtn}
                  />
                ) : null}
              </View>
            </MilaCard>
          );
        })
      )}
    </MilaScreenShell>
  );
};

export const SavedJobsScreen: React.FC = () => {
  const navigation = useNavigation<NativeStackNavigationProp<TalentStackParamList>>();
  const { user, savedJobs, vacancies, refreshAccountData } = useMilaApp();

  const handleRemove = async (roleId: string) => {
    if (!user) return;
    try {
      await removeSavedJob(user.id, roleId);
      await refreshAccountData();
    } catch (err) {
      Alert.alert('Remove Failed', err instanceof Error ? err.message : 'Unable to remove.');
    }
  };

  return (
    <MilaScreenShell>
      <MilaSectionHeading
        eyebrow="CANDIDATE SHORTLIST"
        title="Saved Jobs"
        subtitle="Roles you bookmarked for quick review and application."
      />

      {!user ? (
        <MilaCard highlighted>
          <Text style={styles.emptyTitle}>Sign In to View Saved Jobs</Text>
          <Text style={styles.emptyBody}>
            Your bookmarked vacancies sync securely across Android and iOS when signed in.
          </Text>
          <MilaButton
            label="Candidate Login"
            onPress={() => navigation.navigate('CandidateLogin')}
          />
        </MilaCard>
      ) : savedJobs.length === 0 ? (
        <MilaCard>
          <Text style={styles.emptyTitle}>No Saved Jobs</Text>
          <Text style={styles.emptyBody}>
            Tap the bookmark icon on any vacancy in Job Search to save it here.
          </Text>
        </MilaCard>
      ) : (
        savedJobs.map((item) => {
          const vacancy = vacancies.find(
            (v) => v.id === item.vacancy_id || v.slug === item.role_id || v.id === item.role_id
          );
          return (
            <MilaCard key={item.id}>
              <Text style={styles.roleTitle}>{item.role_title}</Text>
              <Text style={styles.roleMeta}>
                {item.location}
                {item.compensation ? ` • ${item.compensation}` : ''}
              </Text>
              <View style={styles.actionsRow}>
                {vacancy ? (
                  <MilaButton
                    label="View & Apply"
                    onPress={() => navigation.navigate('JobDetail', { vacancy })}
                    style={styles.flexBtn}
                  />
                ) : null}
                <MilaButton
                  label="Remove"
                  variant="ghost"
                  icon="trash-outline"
                  onPress={() => handleRemove(item.role_id)}
                  style={styles.flexBtn}
                />
              </View>
            </MilaCard>
          );
        })
      )}
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  savedPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    borderRadius: MilaRadii.pill,
    paddingHorizontal: 12,
    minHeight: 36,
  },
  savedPillText: {
    color: MilaColors.champagneGold,
    fontSize: 12,
    fontWeight: '700',
  },
  topRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 8,
  },
  dateText: {
    fontSize: 11,
    color: MilaColors.warmStone,
  },
  roleTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  roleMeta: {
    fontSize: 13,
    color: MilaColors.champagneGold,
    marginTop: 3,
    marginBottom: 8,
  },
  cvAttached: {
    fontSize: 12,
    color: MilaColors.emerald,
    marginBottom: 10,
  },
  stepperWrap: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    marginVertical: 12,
    paddingVertical: 10,
    borderTopWidth: StyleSheet.hairlineWidth,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderColor: MilaColors.charcoalBorder,
  },
  stepItem: {
    flex: 1,
    alignItems: 'center',
    gap: 5,
  },
  stepDot: {
    width: 10,
    height: 10,
    borderRadius: 5,
    backgroundColor: MilaColors.charcoalBorder,
  },
  stepDotReached: {
    backgroundColor: MilaColors.champagneGold,
  },
  stepLabel: {
    fontSize: 9,
    color: MilaColors.warmStone,
    textAlign: 'center',
  },
  stepLabelReached: {
    color: MilaColors.ivory,
    fontWeight: '700',
  },
  actionsRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 8,
  },
  flexBtn: {
    flex: 1,
  },
  emptyTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 19,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 6,
  },
  emptyBody: {
    fontSize: 13,
    lineHeight: 20,
    color: MilaColors.ivoryMuted,
    marginBottom: 14,
  },
});
