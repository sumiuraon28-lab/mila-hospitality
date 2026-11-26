import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useState } from 'react';
import { Alert, ImageBackground, Pressable, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { MILA_IMAGES } from '../../constants/brandAssets';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList, TalentTabParamList } from '../../navigation/types';
import {
  MilaColors,
  MilaFonts,
  MilaRadii,
  MilaSpacing,
  useResponsiveLayout,
} from '../../theme/milaTheme';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<TalentTabParamList, 'CareerHome'>,
  NativeStackNavigationProp<TalentStackParamList>
>;

export const CareerHomeScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const layout = useResponsiveLayout();
  const {
    user,
    candidateProfile,
    vacancies,
    savedJobs,
    applications,
    unreadNotificationsCount,
    toggleLookingForJob,
    refreshAll,
  } = useMilaApp();

  const [refreshing, setRefreshing] = useState(false);
  const [toggling, setToggling] = useState(false);

  const alertsActive = candidateProfile?.job_alerts_enabled ?? false;

  const handleRefresh = async () => {
    setRefreshing(true);
    await refreshAll();
    setRefreshing(false);
  };

  const handleLookingForJobPress = async () => {
    if (!user) {
      Alert.alert(
        'Activate Job Alerts',
        'Sign in or register on MILA Talent to activate "I AM LOOKING FOR A JOB" and receive matched vacancy push notifications.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Sign In', onPress: () => navigation.navigate('CandidateLogin') },
          { text: 'Register', onPress: () => navigation.navigate('CandidateRegister') },
        ]
      );
      return;
    }
    if (!candidateProfile) {
      navigation.navigate('CandidateProfile');
      return;
    }
    setToggling(true);
    try {
      await toggleLookingForJob(!alertsActive);
      Alert.alert(
        !alertsActive ? 'Job Alerts Activated' : 'Job Alerts Paused',
        !alertsActive
          ? 'You will now receive targeted Android/iOS push and in-app alerts matching your department, preferred locations, experience, and skills.'
          : 'Automated vacancy alerts have been paused. You can re-activate anytime.'
      );
    } catch (err) {
      Alert.alert('Update Failed', err instanceof Error ? err.message : 'Unable to update status.');
    } finally {
      setToggling(false);
    }
  };

  const urgentVacancies = vacancies.filter((v) => v.is_urgent).slice(0, 3);
  const displayVacancies = urgentVacancies.length > 0 ? urgentVacancies : vacancies.slice(0, 3);

  return (
    <MilaScreenShell
      showBrandHeader
      refreshing={refreshing}
      onRefresh={handleRefresh}
      rightHeaderAction={
        <Pressable
          accessibilityRole="button"
          onPress={() =>
            user ? navigation.navigate('CandidateProfile') : navigation.navigate('CandidateLogin')
          }
          style={styles.loginPill}
        >
          <Ionicons
            name={user ? 'person-circle' : 'log-in-outline'}
            size={18}
            color={MilaColors.champagneGold}
          />
          <Text style={styles.loginPillText} numberOfLines={1}>
            {candidateProfile?.full_name?.split(' ')[0] ?? (user ? 'Profile' : 'Talent Login')}
          </Text>
        </Pressable>
      }
    >
      <SupabaseConfigNotice />

      {/* Career Hero */}
      <View style={[styles.heroCard, { height: layout.isTablet ? 310 : 255 }]}>
        <ImageBackground
          source={MILA_IMAGES.talent_recruitment}
          style={styles.heroBg}
          imageStyle={styles.heroImg}
        >
          <LinearGradient
            colors={['rgba(17,20,18,0.28)', 'rgba(17,20,18,0.84)', 'rgba(25,29,27,0.97)']}
            style={styles.heroGradient}
          >
            <MilaStatusBadge label="MILA TALENT • HOSPITALITY CAREERS" />
            <Text style={styles.heroTitle}>Elevate Your Hospitality Career</Text>
            <Text style={styles.heroSub}>
              Confidential executive search and pre-opening leadership placements across India's
              luxury hotels, heritage resorts, fine-dining venues, and craft bars.
            </Text>
          </LinearGradient>
        </ImageBackground>
      </View>

      {/* Prominent "I AM LOOKING FOR A JOB" Action Card */}
      <MilaCard highlighted>
        <View style={styles.lookingTop}>
          <View style={styles.lookingCopy}>
            <Text style={styles.lookingEyebrow}>MATCHED VACANCY RADAR</Text>
            <Text style={styles.lookingTitle}>
              {alertsActive ? 'Active Job-Seeking Status: ON' : 'Signal MILA Recruiters'}
            </Text>
            <Text style={styles.lookingDesc}>
              {alertsActive
                ? 'Your profile is prioritized for matching vacancies in your department, preferred cities, and experience band.'
                : 'Tap below to activate targeted push & in-app alerts whenever a matching vacancy is published.'}
            </Text>
          </View>
          <MilaStatusBadge
            label={alertsActive ? 'ACTIVE' : 'PAUSED'}
            tone={alertsActive ? 'emerald' : 'neutral'}
          />
        </View>
        <MilaButton
          label={alertsActive ? 'I AM LOOKING FOR A JOB (ACTIVE)' : 'I AM LOOKING FOR A JOB'}
          icon={alertsActive ? 'notifications' : 'notifications-outline'}
          loading={toggling}
          onPress={handleLookingForJobPress}
        />
      </MilaCard>

      {/* Candidate Auth Prompt if guest */}
      {!user ? (
        <MilaCard>
          <Text style={styles.cardTitle}>Separate Candidate Registration</Text>
          <Text style={styles.cardBody}>
            Create your private MILA Talent profile to upload your CV securely, save roles, apply in
            one tap, and track application stages.
          </Text>
          <View style={styles.rowGap}>
            <MilaButton
              label="Candidate Register"
              onPress={() => navigation.navigate('CandidateRegister')}
              style={styles.flexBtn}
            />
            <MilaButton
              label="Talent Login"
              variant="outlined"
              onPress={() => navigation.navigate('CandidateLogin')}
              style={styles.flexBtn}
            />
          </View>
        </MilaCard>
      ) : null}

      {/* Quick Career Metrics */}
      <View style={styles.statsGrid}>
        <Pressable
          accessibilityRole="button"
          onPress={() => navigation.navigate('JobSearch')}
          style={styles.statTile}
        >
          <Text style={styles.statNum}>{vacancies.length}</Text>
          <Text style={styles.statLabel}>Open Roles</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          onPress={() => navigation.navigate('SavedJobs')}
          style={styles.statTile}
        >
          <Text style={styles.statNum}>{savedJobs.length}</Text>
          <Text style={styles.statLabel}>Saved Jobs</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          onPress={() => navigation.navigate('Applications')}
          style={styles.statTile}
        >
          <Text style={styles.statNum}>{applications.length}</Text>
          <Text style={styles.statLabel}>Applications</Text>
        </Pressable>
        <Pressable
          accessibilityRole="button"
          onPress={() => navigation.navigate('AlertSettings')}
          style={styles.statTile}
        >
          <Text style={styles.statNum}>{unreadNotificationsCount}</Text>
          <Text style={styles.statLabel}>Unread Alerts</Text>
        </Pressable>
      </View>

      {/* Featured Urgent Mandates */}
      <MilaSectionHeading
        eyebrow="PRIORITY MANDATES"
        title="Featured Hospitality Openings"
        subtitle="Curated roles published directly by MILA Recruitment Administrators."
      />
      {displayVacancies.map((vacancy) => (
        <Pressable
          key={vacancy.id}
          accessibilityRole="button"
          onPress={() => navigation.navigate('JobDetail', { vacancy })}
        >
          <MilaCard>
            <View style={styles.vacancyTop}>
              <MilaStatusBadge label={vacancy.category.replace(/_/g, ' ')} />
              {vacancy.is_urgent ? <MilaStatusBadge label="URGENT" tone="amber" /> : null}
            </View>
            <Text style={styles.vacancyTitle}>{vacancy.title}</Text>
            <Text style={styles.vacancyTier}>{vacancy.property_tier}</Text>
            <View style={styles.metaStrip}>
              <View style={styles.metaItem}>
                <Ionicons name="location-outline" size={14} color={MilaColors.champagneGold} />
                <Text style={styles.metaText}>{vacancy.location}</Text>
              </View>
              <View style={styles.metaItem}>
                <Ionicons name="time-outline" size={14} color={MilaColors.champagneGold} />
                <Text style={styles.metaText}>{vacancy.experience_required}</Text>
              </View>
            </View>
            <View style={styles.viewMoreRow}>
              <Text style={styles.viewMoreText}>View Role & Apply</Text>
              <Ionicons name="arrow-forward" size={15} color={MilaColors.champagneGold} />
            </View>
          </MilaCard>
        </Pressable>
      ))}

      <View style={styles.rowGap}>
        <MilaButton
          label="Search All Vacancies"
          icon="search-outline"
          onPress={() => navigation.navigate('JobSearch')}
          style={styles.flexBtn}
        />
        <MilaButton
          label="Saved Jobs"
          variant="outlined"
          icon="bookmark-outline"
          onPress={() => navigation.navigate('SavedJobs')}
          style={styles.flexBtn}
        />
      </View>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  loginPill: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    borderRadius: MilaRadii.pill,
    paddingHorizontal: 12,
    minHeight: 36,
    maxWidth: 135,
  },
  loginPillText: {
    color: MilaColors.champagneGold,
    fontSize: 12,
    fontWeight: '700',
  },
  heroCard: {
    borderRadius: MilaRadii.xl,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    marginBottom: MilaSpacing.md,
  },
  heroBg: {
    flex: 1,
    justifyContent: 'flex-end',
  },
  heroImg: {
    resizeMode: 'cover',
  },
  heroGradient: {
    flex: 1,
    justifyContent: 'flex-end',
    padding: MilaSpacing.md,
    gap: 8,
  },
  heroTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 26,
    lineHeight: 32,
    color: MilaColors.ivory,
    fontWeight: '700',
  },
  heroSub: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
  },
  lookingTop: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: 14,
  },
  lookingCopy: {
    flex: 1,
  },
  lookingEyebrow: {
    fontSize: 10,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  lookingTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 19,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginTop: 4,
  },
  lookingDesc: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginTop: 4,
  },
  cardTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 6,
  },
  cardBody: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginBottom: 14,
  },
  rowGap: {
    flexDirection: 'row',
    gap: 10,
  },
  flexBtn: {
    flex: 1,
  },
  statsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    gap: 10,
    marginBottom: MilaSpacing.lg,
  },
  statTile: {
    width: '48%',
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    borderRadius: MilaRadii.md,
    padding: 14,
  },
  statNum: {
    fontFamily: MilaFonts.serif,
    fontSize: 24,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  statLabel: {
    fontSize: 12,
    color: MilaColors.ivoryMuted,
    marginTop: 2,
    fontWeight: '600',
  },
  vacancyTop: {
    flexDirection: 'row',
    gap: 8,
    marginBottom: 8,
  },
  vacancyTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  vacancyTier: {
    fontSize: 13,
    color: MilaColors.champagneGold,
    marginTop: 2,
    marginBottom: 8,
  },
  metaStrip: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 14,
    marginBottom: 10,
  },
  metaItem: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 4,
  },
  metaText: {
    fontSize: 12,
    color: MilaColors.ivoryMuted,
  },
  viewMoreRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  viewMoreText: {
    fontSize: 13,
    fontWeight: '700',
    color: MilaColors.champagneGold,
  },
});
