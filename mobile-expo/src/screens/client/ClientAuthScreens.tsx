import { useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { Alert, StyleSheet, Text, View } from 'react-native';
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
import { ClientStackParamList } from '../../navigation/types';
import {
  registerClientAccount,
  requestPasswordReset,
  signInWithEmailPassword,
  upsertClientProfile,
} from '../../services/milaApi';
import { MilaColors, MilaSpacing } from '../../theme/milaTheme';
import { BUSINESS_CATEGORY_OPTIONS, BusinessCategory } from '../../types/mila';

type Nav = NativeStackNavigationProp<ClientStackParamList>;

export const ClientLoginScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { user, clientProfile, refreshAccountData, signOut } = useMilaApp();

  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  // Profile editor state when already signed in
  const [fullName, setFullName] = useState(clientProfile?.full_name ?? '');
  const [mobileNumber, setMobileNumber] = useState(clientProfile?.mobile_number ?? '');
  const [companyName, setCompanyName] = useState(clientProfile?.company_name ?? '');
  const [designation, setDesignation] = useState(clientProfile?.designation ?? '');
  const [city, setCity] = useState(clientProfile?.city ?? '');
  const [businessCategory, setBusinessCategory] = useState<string>(
    clientProfile?.business_category ?? 'LUXURY_HOTEL_RESORT'
  );

  useEffect(() => {
    if (clientProfile) {
      setFullName(clientProfile.full_name);
      setMobileNumber(clientProfile.mobile_number);
      setCompanyName(clientProfile.company_name);
      setDesignation(clientProfile.designation);
      setCity(clientProfile.city);
      setBusinessCategory(clientProfile.business_category);
    }
  }, [clientProfile]);

  const handleLogin = async () => {
    if (!email.includes('@') || password.length < 6) {
      Alert.alert('Invalid Credentials', 'Please enter a valid email address and password.');
      return;
    }
    setLoading(true);
    try {
      await signInWithEmailPassword(email, password);
      await refreshAccountData();
      // Return client directly to normal MILA browsing experience — never a complex dashboard
      navigation.goBack();
    } catch (err) {
      Alert.alert('Sign In Failed', err instanceof Error ? err.message : 'Unable to sign in.');
    } finally {
      setLoading(false);
    }
  };

  const handlePasswordReset = async () => {
    if (!email.includes('@')) {
      Alert.alert('Enter Email', 'Enter your account email above to receive a password reset link.');
      return;
    }
    setLoading(true);
    try {
      await requestPasswordReset(email);
      Alert.alert('Reset Email Sent', `Password recovery instructions were sent to ${email.trim()}.`);
    } catch (err) {
      Alert.alert('Reset Failed', err instanceof Error ? err.message : 'Unable to send reset link.');
    } finally {
      setLoading(false);
    }
  };

  const handleSaveProfile = async () => {
    if (!user) return;
    if (
      fullName.trim().length < 2 ||
      mobileNumber.trim().length < 7 ||
      companyName.trim().length < 2 ||
      designation.trim().length < 2 ||
      city.trim().length < 2
    ) {
      Alert.alert('Incomplete Profile', 'Please complete all client profile fields.');
      return;
    }
    setLoading(true);
    try {
      await upsertClientProfile({
        user_id: user.id,
        full_name: fullName.trim(),
        email: (user.email ?? clientProfile?.email ?? '').toLowerCase(),
        mobile_number: mobileNumber.trim(),
        company_name: companyName.trim(),
        designation: designation.trim(),
        city: city.trim(),
        business_category: businessCategory,
      });
      await refreshAccountData();
      Alert.alert('Profile Updated', 'Your confidential client details have been saved.');
    } catch (err) {
      Alert.alert('Save Failed', err instanceof Error ? err.message : 'Unable to save profile.');
    } finally {
      setLoading(false);
    }
  };

  if (user) {
    return (
      <MilaScreenShell>
        <SupabaseConfigNotice />
        <MilaSectionHeading
          eyebrow="PRIVATE CLIENT PROFILE"
          title="Your MILA Account"
          subtitle="Keep your company details updated for faster consultancy enquiries. Your data is strictly isolated and never shared with other clients."
        />

        <MilaCard highlighted>
          <MilaStatusBadge label={`SIGNED IN: ${user.email ?? ''}`} tone="emerald" />
          <View style={styles.spacer} />
          <MilaInput label="Full Name *" value={fullName} onChangeText={setFullName} />
          <MilaInput
            label="Mobile Number *"
            keyboardType="phone-pad"
            value={mobileNumber}
            onChangeText={setMobileNumber}
          />
          <MilaInput label="Company / Property Name *" value={companyName} onChangeText={setCompanyName} />
          <MilaInput label="Designation *" value={designation} onChangeText={setDesignation} />
          <MilaInput label="City *" value={city} onChangeText={setCity} />

          <Text style={styles.chipLabel}>Business Category</Text>
          <View style={styles.chipsWrap}>
            {BUSINESS_CATEGORY_OPTIONS.map((opt) => (
              <MilaChip
                key={opt.value}
                label={opt.label}
                selected={businessCategory === opt.value}
                onPress={() => setBusinessCategory(opt.value)}
              />
            ))}
          </View>

          <MilaButton
            label="Save Client Profile"
            icon="checkmark-circle-outline"
            loading={loading}
            onPress={handleSaveProfile}
          />
        </MilaCard>

        <View style={styles.actionStack}>
          <MilaButton
            label="Continue Exploring MILA Services"
            variant="outlined"
            onPress={() => navigation.goBack()}
          />
          <MilaButton
            label="Sign Out"
            variant="danger"
            icon="log-out-outline"
            onPress={async () => {
              await signOut();
              navigation.goBack();
            }}
          />
        </View>
      </MilaScreenShell>
    );
  }

  return (
    <MilaScreenShell>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="MILA CLIENT ACCESS"
        title="Client Sign In"
        subtitle="Sign in to submit consultancy enquiries and explore services seamlessly—with no complicated dashboard."
      />

      <MilaCard highlighted>
        <MilaInput
          label="Business Email *"
          placeholder="name@company.com"
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
          label="Sign In to MILA"
          icon="log-in-outline"
          loading={loading}
          onPress={handleLogin}
        />

        <View style={styles.secondaryActions}>
          <MilaButton
            label="Reset Password"
            variant="ghost"
            onPress={handlePasswordReset}
          />
          <MilaButton
            label="Create Client Account"
            variant="outlined"
            icon="person-add-outline"
            onPress={() => navigation.replace('ClientRegister')}
          />
        </View>
      </MilaCard>
    </MilaScreenShell>
  );
};

export const ClientRegisterScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { refreshAccountData } = useMilaApp();

  const [fullName, setFullName] = useState('');
  const [email, setEmail] = useState('');
  const [mobileNumber, setMobileNumber] = useState('');
  const [companyName, setCompanyName] = useState('');
  const [designation, setDesignation] = useState('');
  const [city, setCity] = useState('');
  const [businessCategory, setBusinessCategory] =
    useState<BusinessCategory>('LUXURY_HOTEL_RESORT');
  const [password, setPassword] = useState('');
  const [loading, setLoading] = useState(false);

  const handleRegister = async () => {
    if (
      fullName.trim().length < 2 ||
      !email.includes('@') ||
      mobileNumber.trim().length < 7 ||
      companyName.trim().length < 2 ||
      designation.trim().length < 2 ||
      city.trim().length < 2
    ) {
      Alert.alert('Required Fields', 'Please complete all client registration fields.');
      return;
    }
    if (password.length < 8) {
      Alert.alert('Weak Password', 'Please use a password of at least 8 characters.');
      return;
    }

    setLoading(true);
    try {
      const result = await registerClientAccount({
        fullName,
        email,
        mobileNumber,
        companyName,
        designation,
        city,
        businessCategory,
        password,
      });
      await refreshAccountData();

      if (result.requiresEmailConfirmation) {
        Alert.alert(
          'Verify Your Email',
          'Your client account was created. Please verify your email from the link sent by MILA, then sign in.',
          [{ text: 'Go to Login', onPress: () => navigation.replace('ClientLogin') }]
        );
      } else {
        Alert.alert('Welcome to MILA', 'Your client account is active.', [
          { text: 'Explore MILA', onPress: () => navigation.goBack() },
        ]);
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
        eyebrow="CLIENT REGISTRATION"
        title="Create Your Client Account"
        subtitle="Register with MILA Hospitality Consultant to submit confidential service enquiries and receive tailored proposals."
      />

      <MilaCard highlighted>
        <MilaInput label="Full Name *" placeholder="Your full name" value={fullName} onChangeText={setFullName} />
        <MilaInput
          label="Email *"
          placeholder="name@company.com"
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
          label="Company / Property Name *"
          placeholder="Hotel, Resort, Café, or Hospitality Group"
          value={companyName}
          onChangeText={setCompanyName}
        />
        <MilaInput
          label="Designation *"
          placeholder="Owner, Managing Partner, Director"
          value={designation}
          onChangeText={setDesignation}
        />
        <MilaInput label="City *" placeholder="e.g., Mumbai, New Delhi, Bengaluru" value={city} onChangeText={setCity} />

        <Text style={styles.chipLabel}>Business Category *</Text>
        <View style={styles.chipsWrap}>
          {BUSINESS_CATEGORY_OPTIONS.map((opt) => (
            <MilaChip
              key={opt.value}
              label={opt.label}
              selected={businessCategory === opt.value}
              onPress={() => setBusinessCategory(opt.value)}
            />
          ))}
        </View>

        <MilaInput
          label="Password *"
          placeholder="Minimum 8 characters"
          secureTextEntry
          value={password}
          onChangeText={setPassword}
        />

        <MilaButton
          label="Create Client Account"
          icon="checkmark-circle-outline"
          loading={loading}
          onPress={handleRegister}
        />

        <View style={styles.secondaryActions}>
          <MilaButton
            label="Already Registered? Sign In"
            variant="ghost"
            onPress={() => navigation.replace('ClientLogin')}
          />
        </View>
      </MilaCard>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  spacer: {
    height: 12,
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
  secondaryActions: {
    marginTop: 12,
    gap: 10,
  },
  actionStack: {
    gap: 10,
    marginBottom: MilaSpacing.lg,
  },
});
