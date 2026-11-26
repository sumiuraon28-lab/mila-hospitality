import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import * as DocumentPicker from 'expo-document-picker';
import React, { useEffect, useState } from 'react';
import { Alert, Linking, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaChip,
  MilaInput,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList, TalentTabParamList } from '../../navigation/types';
import {
  createPrivateCvSignedUrl,
  deletePrivateCandidateCv,
  splitCsv,
  uploadPrivateCandidateCv,
  upsertCandidatePreferences,
  upsertCandidateProfile,
} from '../../services/milaApi';
import { MilaColors, MilaFonts, MilaSpacing } from '../../theme/milaTheme';
import {
  AVAILABILITY_OPTIONS,
  DEPARTMENT_OPTIONS,
} from '../../types/mila';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<TalentTabParamList, 'CandidateProfile'>,
  NativeStackNavigationProp<TalentStackParamList>
>;

export const CandidateProfileScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const {
    user,
    candidateProfile,
    candidatePreferences,
    refreshAccountData,
    signOut,
  } = useMilaApp();

  const [fullName, setFullName] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [location, setLocation] = useState('');
  const [currentDesignation, setCurrentDesignation] = useState('');
  const [department, setDepartment] = useState('EXECUTIVE');
  const [totalExperience, setTotalExperience] = useState('0');
  const [currentEmployer, setCurrentEmployer] = useState('');
  const [previousEmployers, setPreviousEmployers] = useState('');
  const [education, setEducation] = useState('');
  const [skills, setSkills] = useState('');
  const [preferredJobLocations, setPreferredJobLocations] = useState('');
  const [expectedSalary, setExpectedSalary] = useState('');
  const [availability, setAvailability] = useState('WITHIN_30_DAYS');
  const [noticePeriod, setNoticePeriod] = useState('30 Days');

  const [saving, setSaving] = useState(false);
  const [cvBusy, setCvBusy] = useState(false);

  useEffect(() => {
    if (candidateProfile) {
      setFullName(candidateProfile.full_name);
      setMobileNumber(candidateProfile.mobile_number);
      setLocation(candidateProfile.location);
      setCurrentDesignation(candidateProfile.current_designation);
      setDepartment(candidateProfile.department || 'EXECUTIVE');
      setTotalExperience(String(candidateProfile.total_experience_years ?? 0));
      setCurrentEmployer(candidateProfile.current_employer);
      setPreviousEmployers(candidateProfile.previous_employers);
      setEducation(candidateProfile.education);
      setSkills(candidateProfile.skills);
      setPreferredJobLocations(candidateProfile.preferred_job_locations);
      setExpectedSalary(candidateProfile.expected_salary);
      setAvailability(candidateProfile.availability || 'WITHIN_30_DAYS');
      setNoticePeriod(candidateProfile.notice_period || '30 Days');
    }
  }, [candidateProfile]);

  const handleSaveProfile = async () => {
    if (!user) return;
    if (fullName.trim().length < 2 || mobileNumber.trim().length < 7) {
      Alert.alert('Required Details', 'Please provide at least your Full Name and Mobile Number.');
      return;
    }

    const years = Math.max(0, Math.min(60, Number.parseInt(totalExperience || '0', 10) || 0));

    setSaving(true);
    try {
      const saved = await upsertCandidateProfile({
        ...(candidateProfile ?? {}),
        user_id: user.id,
        full_name: fullName.trim(),
        email: (candidateProfile?.email || user.email || '').toLowerCase(),
        mobile_number: mobileNumber.trim(),
        location: location.trim(),
        current_designation: currentDesignation.trim(),
        department,
        total_experience_years: years,
        current_employer: currentEmployer.trim(),
        previous_employers: previousEmployers.trim(),
        education: education.trim(),
        skills: skills.trim(),
        preferred_job_locations: preferredJobLocations.trim() || location.trim(),
        expected_salary: expectedSalary.trim(),
        availability,
        notice_period: noticePeriod.trim() || '30 Days',
        job_alerts_enabled: candidateProfile?.job_alerts_enabled ?? true,
      });

      await upsertCandidatePreferences({
        user_id: user.id,
        candidate_profile_id: saved.id ?? null,
        job_alerts_enabled: saved.job_alerts_enabled,
        preferred_locations: splitCsv(saved.preferred_job_locations || saved.location),
        preferred_departments: [saved.department],
        preferred_skills: splitCsv(saved.skills),
        min_expected_salary: saved.expected_salary,
        availability: saved.availability,
        notice_period: saved.notice_period,
        push_notifications_enabled:
          candidatePreferences?.push_notifications_enabled ?? saved.job_alerts_enabled,
        in_app_notifications_enabled:
          candidatePreferences?.in_app_notifications_enabled ?? true,
        email_notifications_enabled:
          candidatePreferences?.email_notifications_enabled ?? false,
        application_status_alerts_enabled:
          candidatePreferences?.application_status_alerts_enabled ?? true,
      });

      await refreshAccountData();
      Alert.alert('Profile Saved', 'Your MILA Talent profile and job matching preferences are updated.');
    } catch (err) {
      Alert.alert('Save Failed', err instanceof Error ? err.message : 'Unable to save profile.');
    } finally {
      setSaving(false);
    }
  };

  const handlePickAndUploadCv = async () => {
    if (!user) return;
    if (!candidateProfile) {
      Alert.alert('Save Profile First', 'Please save your candidate profile before uploading your CV.');
      return;
    }

    try {
      const picked = await DocumentPicker.getDocumentAsync({
        type: [
          'application/pdf',
          'application/msword',
          'application/vnd.openxmlformats-officedocument.wordprocessingml.document',
        ],
        copyToCacheDirectory: true,
        multiple: false,
      });

      if (picked.canceled || !picked.assets || picked.assets.length === 0) return;

      const file = picked.assets[0];
      setCvBusy(true);
      await uploadPrivateCandidateCv({
        userId: user.id,
        fileUri: file.uri,
        fileName: file.name,
        mimeType: file.mimeType ?? '',
        fileSize: file.size ?? null,
        previousStoragePath: candidateProfile.cv_storage_path,
      });
      await refreshAccountData();
      Alert.alert(
        'CV Stored Privately',
        `${file.name} has been uploaded to MILA's private encrypted CV vault (no public link).`
      );
    } catch (err) {
      Alert.alert('Upload Failed', err instanceof Error ? err.message : 'Unable to upload CV.');
    } finally {
      setCvBusy(false);
    }
  };

  const handlePreviewPrivateCv = async () => {
    if (!candidateProfile?.cv_storage_path) return;
    setCvBusy(true);
    try {
      const signedUrl = await createPrivateCvSignedUrl(candidateProfile.cv_storage_path);
      await Linking.openURL(signedUrl);
    } catch (err) {
      Alert.alert('Preview Error', err instanceof Error ? err.message : 'Unable to open CV.');
    } finally {
      setCvBusy(false);
    }
  };

  const handleDeleteCv = () => {
    if (!user || !candidateProfile?.cv_storage_path) return;
    Alert.alert('Delete CV', 'Remove your CV from MILA private storage?', [
      { text: 'Cancel', style: 'cancel' },
      {
        text: 'Delete',
        style: 'destructive',
        onPress: async () => {
          setCvBusy(true);
          try {
            await deletePrivateCandidateCv(user.id, candidateProfile.cv_storage_path);
            await refreshAccountData();
            Alert.alert('CV Deleted', 'Your CV file has been permanently removed.');
          } catch (err) {
            Alert.alert('Delete Failed', err instanceof Error ? err.message : 'Unable to delete.');
          } finally {
            setCvBusy(false);
          }
        },
      },
    ]);
  };

  if (!user) {
    return (
      <MilaScreenShell showBrandHeader>
        <SupabaseConfigNotice />
        <MilaSectionHeading
          eyebrow="MILA TALENT • PRIVATE PROFILE"
          title="Candidate Profile & CV Vault"
          subtitle="Sign in or register to manage your hospitality profile and upload your CV in PDF, DOC, or DOCX format."
        />
        <MilaCard highlighted>
          <View style={styles.btnStack}>
            <MilaButton
              label="Candidate Login"
              icon="log-in-outline"
              onPress={() => navigation.navigate('CandidateLogin')}
            />
            <MilaButton
              label="Create Candidate Account"
              variant="outlined"
              icon="person-add-outline"
              onPress={() => navigation.navigate('CandidateRegister')}
            />
          </View>
        </MilaCard>
      </MilaScreenShell>
    );
  }

  return (
    <MilaScreenShell showBrandHeader>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="CANDIDATE CREDENTIALS"
        title="Profile & Private CV Vault"
        subtitle="Your profile and CV are strictly confidential—accessible only to you and authorised MILA recruitment administrators."
      />

      {/* Private CV Upload / Replace / Delete Vault */}
      <MilaCard highlighted>
        <View style={styles.cvHeader}>
          <View style={styles.cvHeaderLeft}>
            <Ionicons name="shield-checkmark" size={20} color={MilaColors.champagneGold} />
            <Text style={styles.cvTitle}>Private CV Vault</Text>
          </View>
          <MilaStatusBadge
            label={candidateProfile?.cv_file_name ? 'PRIVATE VAULT' : 'NO CV'}
            tone={candidateProfile?.cv_file_name ? 'emerald' : 'amber'}
          />
        </View>

        {candidateProfile?.cv_file_name ? (
          <View style={styles.cvFileBox}>
            <Ionicons name="document-text" size={22} color={MilaColors.champagneGold} />
            <View style={styles.cvFileMeta}>
              <Text style={styles.cvFileName} numberOfLines={1}>
                {candidateProfile.cv_file_name}
              </Text>
              <Text style={styles.cvFileSub}>
                {candidateProfile.cv_file_size_bytes
                  ? `${Math.max(1, Math.round(candidateProfile.cv_file_size_bytes / 1024))} KB • `
                  : ''}
                Private Storage (No Public Link)
              </Text>
            </View>
          </View>
        ) : (
          <Text style={styles.cvHint}>
            Upload your resume in PDF, DOC, or DOCX (max 10 MB) using the system document picker.
          </Text>
        )}

        <View style={styles.cvBtnRow}>
          <MilaButton
            label={candidateProfile?.cv_file_name ? 'Replace CV' : 'Upload CV'}
            icon="cloud-upload-outline"
            loading={cvBusy}
            onPress={handlePickAndUploadCv}
            style={styles.flexBtn}
          />
          {candidateProfile?.cv_storage_path ? (
            <MilaButton
              label="View"
              variant="outlined"
              icon="eye-outline"
              disabled={cvBusy}
              onPress={handlePreviewPrivateCv}
              style={styles.smallBtn}
            />
          ) : null}
        </View>
        {candidateProfile?.cv_storage_path ? (
          <View style={styles.mtSm}>
            <MilaButton
              label="Delete CV"
              variant="danger"
              icon="trash-outline"
              disabled={cvBusy}
              onPress={handleDeleteCv}
            />
          </View>
        ) : null}
      </MilaCard>

      {/* 15-Field Candidate Profile Form */}
      <MilaCard>
        <Text style={styles.formEyebrow}>CANDIDATE DETAILS</Text>
        <MilaInput label="Full Name *" value={fullName} onChangeText={setFullName} />
        <MilaInput
          label="Email"
          value={candidateProfile?.email || user.email || ''}
          editable={false}
          helperText="Verified via Supabase Auth"
        />
        <MilaInput
          label="Mobile Number *"
          keyboardType="phone-pad"
          value={mobileNumber}
          onChangeText={setMobileNumber}
        />
        <MilaInput label="Current Location" value={location} onChangeText={setLocation} />
        <MilaInput
          label="Current Designation"
          value={currentDesignation}
          onChangeText={setCurrentDesignation}
        />

        <Text style={styles.chipLabel}>Department</Text>
        <View style={styles.chipsWrap}>
          {DEPARTMENT_OPTIONS.map((opt) => (
            <MilaChip
              key={opt.value}
              label={opt.label}
              selected={department === opt.value}
              onPress={() => setDepartment(opt.value)}
            />
          ))}
        </View>

        <MilaInput
          label="Total Experience (Years)"
          keyboardType="number-pad"
          value={totalExperience}
          onChangeText={setTotalExperience}
        />
        <MilaInput
          label="Current Employer"
          value={currentEmployer}
          onChangeText={setCurrentEmployer}
        />
        <MilaInput
          label="Previous Employers"
          value={previousEmployers}
          onChangeText={setPreviousEmployers}
        />
        <MilaInput label="Education" value={education} onChangeText={setEducation} />
        <MilaInput
          label="Skills (comma-separated)"
          value={skills}
          onChangeText={setSkills}
        />
        <MilaInput
          label="Preferred Job Locations"
          value={preferredJobLocations}
          onChangeText={setPreferredJobLocations}
          helperText="Used for targeted automated vacancy alerts (e.g., Mumbai, Goa, Pan-India)"
        />
        <MilaInput
          label="Expected Salary"
          value={expectedSalary}
          onChangeText={setExpectedSalary}
        />

        <Text style={styles.chipLabel}>Availability</Text>
        <View style={styles.chipsWrap}>
          {AVAILABILITY_OPTIONS.map((opt) => (
            <MilaChip
              key={opt.value}
              label={opt.label}
              selected={availability === opt.value}
              onPress={() => setAvailability(opt.value)}
            />
          ))}
        </View>

        <MilaInput label="Notice Period" value={noticePeriod} onChangeText={setNoticePeriod} />

        <MilaButton
          label="Save Profile Changes"
          icon="checkmark-circle-outline"
          loading={saving}
          onPress={handleSaveProfile}
        />
      </MilaCard>

      <MilaButton
        label="Sign Out"
        variant="danger"
        icon="log-out-outline"
        onPress={signOut}
        style={styles.mbLg}
      />
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  btnStack: {
    gap: 10,
  },
  cvHeader: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 12,
  },
  cvHeaderLeft: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  cvTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  cvFileBox: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 10,
    backgroundColor: MilaColors.charcoalDeep,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    borderRadius: 12,
    padding: 12,
    marginBottom: 12,
  },
  cvFileMeta: {
    flex: 1,
  },
  cvFileName: {
    fontSize: 14,
    fontWeight: '700',
    color: MilaColors.ivory,
  },
  cvFileSub: {
    fontSize: 11,
    color: MilaColors.emerald,
    marginTop: 2,
  },
  cvHint: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginBottom: 12,
  },
  cvBtnRow: {
    flexDirection: 'row',
    gap: 10,
  },
  flexBtn: {
    flex: 1,
  },
  smallBtn: {
    minWidth: 100,
  },
  mtSm: {
    marginTop: 10,
  },
  mbLg: {
    marginBottom: MilaSpacing.lg,
  },
  formEyebrow: {
    fontSize: 11,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 12,
  },
  chipLabel: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 0.5,
    color: MilaColors.champagneGold,
    marginBottom: 8,
  },
  chipsWrap: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    marginBottom: 10,
  },
});
