import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaInput,
  MilaScreenShell,
  MilaStatusBadge,
} from '../../components/MilaUI';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList } from '../../navigation/types';
import { submitJobApplication } from '../../services/milaApi';
import { MilaColors, MilaFonts, MilaSpacing } from '../../theme/milaTheme';

export const JobDetailScreen: React.FC = () => {
  const route = useRoute<RouteProp<TalentStackParamList, 'JobDetail'>>();
  const navigation = useNavigation<NativeStackNavigationProp<TalentStackParamList>>();
  const { vacancy } = route.params;

  const {
    user,
    candidateProfile,
    isJobSaved,
    toggleSaveJob,
    hasAppliedToJob,
    refreshAccountData,
  } = useMilaApp();

  const saved = isJobSaved(vacancy);
  const existingApplication = hasAppliedToJob(vacancy);

  const [showApplyForm, setShowApplyForm] = useState(false);
  const [candidateName, setCandidateName] = useState(candidateProfile?.full_name ?? '');
  const [candidateEmail, setCandidateEmail] = useState(
    candidateProfile?.email ?? user?.email ?? ''
  );
  const [candidatePhone, setCandidatePhone] = useState(candidateProfile?.mobile_number ?? '');
  const [yearsExp, setYearsExp] = useState(
    candidateProfile ? String(candidateProfile.total_experience_years) : ''
  );
  const [currentProperty, setCurrentProperty] = useState(candidateProfile?.current_employer ?? '');
  const [coverNote, setCoverNote] = useState('');
  const [submitting, setSubmitting] = useState(false);

  const handleSaveToggle = async () => {
    try {
      const nowSaved = await toggleSaveJob(vacancy);
      Alert.alert(nowSaved ? 'Saved to Shortlist' : 'Removed from Saved Jobs');
    } catch (err) {
      Alert.alert('Sign In Required', err instanceof Error ? err.message : 'Sign in first.', [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Talent Login', onPress: () => navigation.navigate('CandidateLogin') },
      ]);
    }
  };

  const handleApply = async () => {
    if (!user) {
      Alert.alert(
        'Candidate Sign In Required',
        'Please sign in or register on MILA Talent to apply and track your application status.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Sign In', onPress: () => navigation.navigate('CandidateLogin') },
          { text: 'Register', onPress: () => navigation.navigate('CandidateRegister') },
        ]
      );
      return;
    }

    if (
      candidateName.trim().length < 2 ||
      !candidateEmail.includes('@') ||
      candidatePhone.trim().length < 7
    ) {
      Alert.alert('Incomplete Details', 'Please provide your full name, email, and mobile number.');
      return;
    }

    setSubmitting(true);
    try {
      await submitJobApplication({
        userId: user.id,
        candidateProfileId: candidateProfile?.id ?? null,
        vacancy,
        candidateName,
        candidateEmail,
        candidatePhone,
        yearsExperience: Math.max(0, Number.parseInt(yearsExp || '0', 10) || 0),
        currentProperty,
        coverNote,
        cvFileName: candidateProfile?.cv_file_name ?? null,
        cvStoragePath: candidateProfile?.cv_storage_path ?? null,
      });
      await refreshAccountData();
      setShowApplyForm(false);
      Alert.alert(
        'Application Submitted',
        `Your application for ${vacancy.title} has been submitted to MILA Talent. Track every recruitment update in the Applications tab.`
      );
    } catch (err) {
      Alert.alert(
        'Application Failed',
        err instanceof Error ? err.message : 'Unable to submit application.'
      );
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <MilaScreenShell>
      <View style={styles.badgeStrip}>
        <MilaStatusBadge label={vacancy.category.replace(/_/g, ' ')} />
        {vacancy.is_urgent ? <MilaStatusBadge label="URGENT MANDATE" tone="amber" /> : null}
        {existingApplication ? (
          <MilaStatusBadge
            label={`STATUS: ${existingApplication.status.replace(/_/g, ' ')}`}
            tone="emerald"
          />
        ) : null}
      </View>

      <Text style={styles.title}>{vacancy.title}</Text>
      <Text style={styles.tier}>{vacancy.property_tier}</Text>

      <MilaCard highlighted>
        <View style={styles.metaGrid}>
          <View style={styles.metaRow}>
            <Ionicons name="location-outline" size={16} color={MilaColors.champagneGold} />
            <Text style={styles.metaValue}>{vacancy.location}</Text>
          </View>
          <View style={styles.metaRow}>
            <Ionicons name="briefcase-outline" size={16} color={MilaColors.champagneGold} />
            <Text style={styles.metaValue}>{vacancy.experience_required}</Text>
          </View>
          {vacancy.compensation ? (
            <View style={styles.metaRow}>
              <Ionicons name="ribbon-outline" size={16} color={MilaColors.champagneGold} />
              <Text style={styles.metaValue}>{vacancy.compensation}</Text>
            </View>
          ) : null}
        </View>
      </MilaCard>

      <MilaCard>
        <Text style={styles.sectionEyebrow}>ROLE OVERVIEW</Text>
        <Text style={styles.bodyText}>{vacancy.summary}</Text>
      </MilaCard>

      {vacancy.key_mandates.length > 0 ? (
        <MilaCard>
          <Text style={styles.sectionEyebrow}>KEY MANDATES</Text>
          {vacancy.key_mandates.map((item) => (
            <View key={item} style={styles.bulletRow}>
              <Ionicons name="checkmark-circle" size={17} color={MilaColors.champagneGold} />
              <Text style={styles.bulletText}>{item}</Text>
            </View>
          ))}
        </MilaCard>
      ) : null}

      {vacancy.desired_skills.length > 0 ? (
        <MilaCard>
          <Text style={styles.sectionEyebrow}>DESIRED COMPETENCIES</Text>
          <View style={styles.skillsWrap}>
            {vacancy.desired_skills.map((skill) => (
              <MilaStatusBadge key={skill} label={skill} tone="neutral" />
            ))}
          </View>
        </MilaCard>
      ) : null}

      {existingApplication ? (
        <MilaCard highlighted>
          <Text style={styles.sectionEyebrow}>YOUR APPLICATION IS ACTIVE</Text>
          <Text style={styles.bodyText}>
            Current Stage: {existingApplication.status.replace(/_/g, ' ')}. You will receive push
            and in-app notifications whenever MILA recruiters update your status.
          </Text>
        </MilaCard>
      ) : showApplyForm ? (
        <MilaCard highlighted>
          <Text style={styles.sectionEyebrow}>SUBMIT APPLICATION</Text>
          {candidateProfile?.cv_file_name ? (
            <View style={styles.cvNotice}>
              <Ionicons name="lock-closed" size={16} color={MilaColors.emerald} />
              <Text style={styles.cvNoticeText}>
                Private CV attached: {candidateProfile.cv_file_name}
              </Text>
            </View>
          ) : (
            <Text style={styles.cvMissingText}>
              Tip: Upload your PDF/DOC/DOCX CV in the Profile tab to attach it securely.
            </Text>
          )}
          <MilaInput label="Full Name *" value={candidateName} onChangeText={setCandidateName} />
          <MilaInput
            label="Email *"
            keyboardType="email-address"
            autoCapitalize="none"
            value={candidateEmail}
            onChangeText={setCandidateEmail}
          />
          <MilaInput
            label="Mobile Number *"
            keyboardType="phone-pad"
            value={candidatePhone}
            onChangeText={setCandidatePhone}
          />
          <MilaInput
            label="Total Years Experience *"
            keyboardType="number-pad"
            value={yearsExp}
            onChangeText={setYearsExp}
          />
          <MilaInput
            label="Current / Most Recent Property *"
            value={currentProperty}
            onChangeText={setCurrentProperty}
          />
          <MilaInput
            label="Executive Cover Note"
            placeholder="Highlight relevant pre-opening, culinary, mixology, or leadership achievements..."
            multiline
            value={coverNote}
            onChangeText={setCoverNote}
          />
          <View style={styles.btnRow}>
            <MilaButton
              label="Cancel"
              variant="ghost"
              onPress={() => setShowApplyForm(false)}
              style={styles.flexBtn}
            />
            <MilaButton
              label="Confirm & Apply"
              icon="paper-plane-outline"
              loading={submitting}
              onPress={handleApply}
              style={styles.flexBtn}
            />
          </View>
        </MilaCard>
      ) : (
        <View style={styles.btnRow}>
          <MilaButton
            label={saved ? 'Saved' : 'Save Job'}
            variant="outlined"
            icon={saved ? 'bookmark' : 'bookmark-outline'}
            onPress={handleSaveToggle}
            style={styles.flexBtn}
          />
          <MilaButton
            label="Apply Now"
            icon="paper-plane-outline"
            onPress={() => {
              if (!user) {
                navigation.navigate('CandidateLogin');
              } else {
                setShowApplyForm(true);
              }
            }}
            style={styles.flexBtn}
          />
        </View>
      )}
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  badgeStrip: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 10,
  },
  title: {
    fontFamily: MilaFonts.serif,
    fontSize: 25,
    lineHeight: 32,
    color: MilaColors.ivory,
    fontWeight: '700',
  },
  tier: {
    fontSize: 14,
    color: MilaColors.champagneGold,
    marginTop: 4,
    marginBottom: MilaSpacing.md,
    fontWeight: '600',
  },
  metaGrid: {
    gap: 8,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  metaValue: {
    fontSize: 14,
    color: MilaColors.ivory,
  },
  sectionEyebrow: {
    fontSize: 11,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 10,
  },
  bodyText: {
    fontSize: 14,
    lineHeight: 22,
    color: MilaColors.ivoryMuted,
  },
  bulletRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
    marginBottom: 8,
  },
  bulletText: {
    flex: 1,
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivory,
  },
  skillsWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
  },
  cvNotice: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    backgroundColor: MilaColors.emeraldSubtle,
    borderColor: MilaColors.emerald,
    borderWidth: 1,
    borderRadius: 10,
    padding: 10,
    marginBottom: 12,
  },
  cvNoticeText: {
    flex: 1,
    fontSize: 12,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  cvMissingText: {
    fontSize: 12,
    color: MilaColors.amber,
    marginBottom: 12,
  },
  btnRow: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 6,
  },
  flexBtn: {
    flex: 1,
  },
});
