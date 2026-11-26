import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaChip,
  MilaInput,
  MilaScreenShell,
  MilaSectionHeading,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList } from '../../navigation/types';
import {
  registerCandidateAccount,
  requestPasswordReset,
  signInWithEmailPassword,
} from '../../services/milaApi';
import { MilaColors } from '../../theme/milaTheme';
import {
  AVAILABILITY_OPTIONS,
  CandidateAvailability,
  DEPARTMENT_OPTIONS,
  HospitalityDepartment,
} from '../../types/mila';

type Nav = NativeStackNavigationProp<TalentStackParamList>;

export const CandidateLoginScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { refreshAccountData } = useMilaApp();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleSignIn = async () => {
    if (!email.includes('@') || password.length < 6) {
      Alert.alert('Invalid Credentials', 'Enter a valid email and password.');
      return;
    }
    setLoading(true);
    try {
      await signInWithEmailPassword(email, password);
      await refreshAccountData();
      navigation.goBack();
    } catch (err) {
      Alert.alert('Login Failed', err instanceof Error ? err.message : 'Unable to sign in.');
    } finally {
      setLoading(false);
    }
  };

  const handleReset = async () => {
    if (!email.includes('@')) {
      Alert.alert('Enter Email', 'Enter your registered candidate email above first.');
      return;
    }
    setLoading(true);
    try {
      await requestPasswordReset(email);
      Alert.alert('Password Reset Sent', `Check ${email.trim()} for recovery instructions.`);
    } catch (err) {
      Alert.alert('Reset Error', err instanceof Error ? err.message : 'Unable to send reset.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <MilaScreenShell>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="MILA TALENT • JOB SEEKER ACCESS"
        title="Candidate Sign In"
        subtitle="Access your private hospitality profile, CV vault, saved vacancies, application status, and job alerts."
      />

      <MilaCard highlighted>
        <MilaInput
          label="Email *"
          placeholder="candidate@email.com"
          keyboardType="email-address"
          autoCapitalize="none"
          value={email}
          onChangeText={setEmail}
        />
        <MilaInput
          label="Password *"
          placeholder="Enter your password"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />
        <MilaButton
          label="Sign In to MILA Talent"
          icon="log-in-outline"
          loading={loading}
          onPress={handleSignIn}
        />

        <View style={styles.secondaryStack}>
          <MilaButton label="Reset Password" variant="ghost" onPress={handleReset} />
          <MilaButton
            label="New Candidate? Register Here"
            variant="outlined"
            icon="person-add-outline"
            onPress={() => navigation.replace('CandidateRegister')}
          />
        </View>
      </MilaCard>
    </MilaScreenShell>
  );
};

export const CandidateRegisterScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { refreshAccountData } = useMilaApp();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [location, setLocation] = useState('');
  const [currentDesignation, setCurrentDesignation] = useState('');
  const [department, setDepartment] = useState<HospitalityDepartment>('EXECUTIVE');
  const [totalExperience, setTotalExperience] = useState('');
  const [currentEmployer, setCurrentEmployer] = useState('');
  const [previousEmployers, setPreviousEmployers] = useState('');
  const [education, setEducation] = useState('');
  const [skills, setSkills] = useState('');
  const [preferredJobLocations, setPreferredJobLocations] = useState('');
  const [expectedSalary, setExpectedSalary] = useState('');
  const [availability, setAvailability] = useState<CandidateAvailability>('WITHIN_30_DAYS');
  const [noticePeriod, setNoticePeriod] = useState('30 Days');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    if (
      fullName.trim().length < 2 ||
      !email.includes('@') ||
      mobileNumber.trim().length < 7 ||
      location.trim().length < 2 ||
      currentDesignation.trim().length < 2
    ) {
      Alert.alert(
        'Missing Required Details',
        'Please complete Full Name, Email, Mobile Number, Location, and Current Designation.'
      );
      return;
    }
    if (password.length < 8) {
      Alert.alert('Weak Password', 'Password must be at least 8 characters.');
      return;
    }

    const years = Math.max(0, Math.min(60, Number.parseInt(totalExperience || '0', 10) || 0));

    setLoading(true);
    try {
      const result = await registerCandidateAccount({
        fullName,
        email,
        mobileNumber,
        location,
        currentDesignation,
        department,
        totalExperienceYears: years,
        currentEmployer,
        previousEmployers,
        education,
        skills,
        preferredJobLocations,
        expectedSalary,
        availability,
        noticePeriod,
        password,
      });
      await refreshAccountData();

      if (result.requiresEmailConfirmation) {
        Alert.alert(
          'Verify Your Email',
          'Your MILA Talent account was created. Confirm your email via the link sent to you, then sign in to upload your CV.',
          [{ text: 'Go to Login', onPress: () => navigation.replace('CandidateLogin') }]
        );
      } else {
        Alert.alert(
          'Welcome to MILA Talent',
          'Your candidate profile is active and job alerts are enabled. Upload your CV anytime from your Profile tab.',
          [{ text: 'Continue', onPress: () => navigation.goBack() }]
        );
      }
    } catch (err) {
      Alert.alert('Registration Failed', err instanceof Error ? err.message : 'Unable to register.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <MilaScreenShell>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="MILA TALENT • CANDIDATE REGISTRATION"
        title="Create Your Career Profile"
        subtitle="Complete your hospitality credentials so MILA matches you exclusively with relevant luxury vacancies."
      />

      <MilaCard highlighted>
        <MilaInput label="Full Name *" placeholder="Your full name" value={fullName} onChangeText={setFullName} />
        <MilaInput
          label="Email *"
          placeholder="candidate@email.com"
          keyboardType="email-address"
          autoCapitalize="none"
          value={email}
          onChangeText={setEmail}
        />
        <MilaInput
          label="Mobile Number *"
          placeholder="+91 98XXX XXXXX"
          keyboardType="phone-pad"
          value={mobileNumber}
          onChangeText={setMobileNumber}
        />
        <MilaInput
          label="Current Location *"
          placeholder="e.g., Mumbai, New Delhi, Bengaluru"
          value={location}
          onChangeText={setLocation}
        />
        <MilaInput
          label="Current Designation *"
          placeholder="e.g., General Manager, Sous Chef, Head Mixologist"
          value={currentDesignation}
          onChangeText={setCurrentDesignation}
        />

        <Text style={styles.chipLabel}>Department *</Text>
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
          label="Total Experience (Years) *"
          placeholder="e.g., 8"
          keyboardType="number-pad"
          value={totalExperience}
          onChangeText={setTotalExperience}
        />
        <MilaInput
          label="Current Employer / Property"
          placeholder="Current hotel, resort, restaurant, or group"
          value={currentEmployer}
          onChangeText={setCurrentEmployer}
        />
        <MilaInput
          label="Previous Employers"
          placeholder="Comma-separated notable properties"
          value={previousEmployers}
          onChangeText={setPreviousEmployers}
        />
        <MilaInput
          label="Education & Hospitality Certifications"
          placeholder="e.g., IHM Mumbai, WSET Level 3, Culinary Diploma"
          value={education}
          onChangeText={setEducation}
        />
        <MilaInput
          label="Core Skills (comma-separated)"
          placeholder="e.g., Pre-Opening Governance, Craft Mixology, P&L, HACCP"
          value={skills}
          onChangeText={setSkills}
        />
        <MilaInput
          label="Preferred Job Locations"
          placeholder="e.g., Mumbai, Goa, Udaipur, Pan-India"
          value={preferredJobLocations}
          onChangeText={setPreferredJobLocations}
        />
        <MilaInput
          label="Expected Salary / CTC"
          placeholder="e.g., INR 24 LPA or Negotiable"
          value={expectedSalary}
          onChangeText={setExpectedSalary}
        />

        <Text style={styles.chipLabel}>Availability *</Text>
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

        <MilaInput
          label="Notice Period"
          placeholder="e.g., Immediate, 15 Days, 30 Days, 60 Days"
          value={noticePeriod}
          onChangeText={setNoticePeriod}
        />
        <MilaInput
          label="Password *"
          placeholder="Minimum 8 characters"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        <MilaButton
          label="Create Candidate Account"
          icon="checkmark-circle-outline"
          loading={loading}
          onPress={handleRegister}
        />

        <View style={styles.secondaryStack}>
          <MilaButton
            label="Already Registered? Candidate Login"
            variant="ghost"
            onPress={() => navigation.replace('CandidateLogin')}
          />
        </View>
      </MilaCard>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
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
  secondaryStack: {
    marginTop: 12,
    gap: 10,
  },
});
