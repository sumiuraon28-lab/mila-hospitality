import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useEffect, useState } from 'react';
import { Alert, ScrollView, StyleSheet, Text, View } from 'react-native';
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
import { ClientStackParamList, ClientTabParamList } from '../../navigation/types';
import { submitServiceEnquiry } from '../../services/milaApi';
import { MilaColors, MilaFonts, MilaSpacing } from '../../theme/milaTheme';
import { ServiceEnquiry } from '../../types/mila';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<ClientTabParamList, 'ClientEnquiry'>,
  NativeStackNavigationProp<ClientStackParamList>
>;

export const EnquiryFormScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const route = useRoute<RouteProp<ClientTabParamList, 'ClientEnquiry'>>();
  const { user, clientProfile, services, enquiries, refreshAccountData } = useMilaApp();

  const [selectedServiceType, setSelectedServiceType] = useState<string>(
    route.params?.preselectedServiceType ?? services[0]?.service_type ?? 'HOSPITALITY_PRE_OPENING'
  );
  const [propertyName, setPropertyName] = useState(clientProfile?.company_name ?? '');
  const [city, setCity] = useState(clientProfile?.city ?? '');
  const [scaleSummary, setScaleSummary] = useState('');
  const [contactName, setContactName] = useState(clientProfile?.full_name ?? '');
  const [contactEmail, setContactEmail] = useState(clientProfile?.email ?? user?.email ?? '');
  const [contactPhone, setContactPhone] = useState(clientProfile?.mobile_number ?? '');
  const [briefNotes, setBriefNotes] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [submittedConfirmation, setSubmittedConfirmation] = useState<ServiceEnquiry | null>(null);

  useEffect(() => {
    if (route.params?.preselectedServiceType) {
      setSelectedServiceType(route.params.preselectedServiceType);
    }
  }, [route.params?.preselectedServiceType]);

  useEffect(() => {
    if (clientProfile) {
      setPropertyName((prev) => prev || clientProfile.company_name);
      setCity((prev) => prev || clientProfile.city);
      setContactName((prev) => prev || clientProfile.full_name);
      setContactEmail((prev) => prev || clientProfile.email);
      setContactPhone((prev) => prev || clientProfile.mobile_number);
    }
  }, [clientProfile]);

  const selectedService =
    services.find((s) => s.service_type === selectedServiceType) ?? services[0];

  const handleSubmit = async () => {
    if (!user) {
      Alert.alert(
        'Client Account Required',
        'Please sign in or register as a MILA client so your private enquiry is linked securely to your account.',
        [
          { text: 'Cancel', style: 'cancel' },
          { text: 'Client Login', onPress: () => navigation.navigate('ClientLogin') },
          { text: 'Register', onPress: () => navigation.navigate('ClientRegister') },
        ]
      );
      return;
    }

    if (propertyName.trim().length < 2 || city.trim().length < 2 || scaleSummary.trim().length < 2) {
      Alert.alert('Missing Details', 'Please provide Property/Project Name, City, and Project Scale.');
      return;
    }
    if (contactName.trim().length < 2 || !contactEmail.includes('@') || contactPhone.trim().length < 7) {
      Alert.alert('Contact Details', 'Please enter a valid contact name, email, and mobile number.');
      return;
    }

    setSubmitting(true);
    try {
      const created = await submitServiceEnquiry({
        userId: user.id,
        clientProfileId: clientProfile?.id ?? null,
        serviceId: selectedService?.id ?? null,
        serviceType: selectedServiceType,
        propertyName,
        city,
        scaleSummary,
        contactName,
        contactEmail,
        contactPhone,
        briefNotes,
      });
      setSubmittedConfirmation(created);
      setScaleSummary('');
      setBriefNotes('');
      await refreshAccountData();
    } catch (err) {
      Alert.alert('Submission Failed', err instanceof Error ? err.message : 'Unable to submit enquiry.');
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <MilaScreenShell showBrandHeader>
      <SupabaseConfigNotice />

      <MilaSectionHeading
        eyebrow="CLIENT CONSULTANCY BRIEF"
        title="Submit a Service Enquiry"
        subtitle="Select the MILA consultancy mandate you require and share your project requirements. All client enquiries are strictly confidential."
      />

      {!user ? (
        <MilaCard highlighted>
          <View style={styles.authNoticeRow}>
            <Ionicons name="lock-closed-outline" size={20} color={MilaColors.champagneGold} />
            <Text style={styles.authNoticeText}>
              Sign in or create a MILA client account to submit and track confidential consultancy
              enquiries. You will stay right here on MILA—no complex dashboard.
            </Text>
          </View>
          <View style={styles.authBtnRow}>
            <MilaButton
              label="Client Login"
              onPress={() => navigation.navigate('ClientLogin')}
              style={styles.flexBtn}
            />
            <MilaButton
              label="Register"
              variant="outlined"
              onPress={() => navigation.navigate('ClientRegister')}
              style={styles.flexBtn}
            />
          </View>
        </MilaCard>
      ) : null}

      {submittedConfirmation ? (
        <MilaCard highlighted>
          <MilaStatusBadge label="ENQUIRY CONFIRMED" tone="emerald" />
          <Text style={styles.confirmTitle}>
            Reference: {submittedConfirmation.reference_code}
          </Text>
          <Text style={styles.confirmBody}>
            Thank you, {submittedConfirmation.contact_name}. Your brief for{' '}
            {selectedService?.title ?? submittedConfirmation.service_type} ({submittedConfirmation.property_name},{' '}
            {submittedConfirmation.city}) has been recorded with MILA Hospitality Consultant.
          </Text>
          <MilaButton
            label="Submit Another Enquiry"
            variant="outlined"
            onPress={() => setSubmittedConfirmation(null)}
          />
        </MilaCard>
      ) : null}

      <MilaCard>
        <Text style={styles.fieldGroupLabel}>SELECT CONSULTANCY SERVICE</Text>
        <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipsScroll}>
          {services.map((srv) => (
            <MilaChip
              key={srv.service_type}
              label={srv.title}
              selected={selectedServiceType === srv.service_type}
              onPress={() => setSelectedServiceType(srv.service_type)}
            />
          ))}
        </ScrollView>

        <MilaInput
          label="Property / Brand / Project Name *"
          placeholder="e.g., Heritage Courtyard Resort or Artisanal Café"
          value={propertyName}
          onChangeText={setPropertyName}
        />
        <MilaInput
          label="City / Destination *"
          placeholder="e.g., Mumbai, Udaipur, Bengaluru, Goa"
          value={city}
          onChangeText={setCity}
        />
        <MilaInput
          label="Project Scale & Target Timeline *"
          placeholder="e.g., 65-key boutique resort • Opening Q4"
          value={scaleSummary}
          onChangeText={setScaleSummary}
        />
        <MilaInput
          label="Primary Contact Name *"
          placeholder="Full name"
          value={contactName}
          onChangeText={setContactName}
        />
        <MilaInput
          label="Business Email *"
          placeholder="name@company.com"
          keyboardType="email-address"
          autoCapitalize="none"
          value={contactEmail}
          onChangeText={setContactEmail}
        />
        <MilaInput
          label="Mobile Number *"
          placeholder="+91 98XXX XXXXX"
          keyboardType="phone-pad"
          value={contactPhone}
          onChangeText={setContactPhone}
        />
        <MilaInput
          label="Business Requirements & Scope Brief"
          placeholder="Share key objectives, current project phase, or specialist advisory requirements..."
          multiline
          value={briefNotes}
          onChangeText={setBriefNotes}
        />

        <MilaButton
          label="Submit Confidential Enquiry"
          icon="paper-plane-outline"
          loading={submitting}
          onPress={handleSubmit}
        />
      </MilaCard>

      {user && enquiries.length > 0 ? (
        <View style={styles.historySection}>
          <MilaSectionHeading
            eyebrow="YOUR PRIVATE ENQUIRIES"
            title={`Submitted Briefs (${enquiries.length})`}
          />
          {enquiries.map((enq) => (
            <MilaCard key={enq.id}>
              <View style={styles.enquiryTop}>
                <Text style={styles.enquiryRef}>{enq.reference_code}</Text>
                <MilaStatusBadge
                  label={enq.status.replace(/_/g, ' ')}
                  tone={enq.status === 'CONVERTED' ? 'emerald' : 'gold'}
                />
              </View>
              <Text style={styles.enquiryProperty}>
                {enq.property_name} • {enq.city}
              </Text>
              <Text style={styles.enquiryMeta}>
                {enq.service_type.replace(/_/g, ' ')} — {enq.scale_summary}
              </Text>
            </MilaCard>
          ))}
        </View>
      ) : null}
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  authNoticeRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: 14,
  },
  authNoticeText: {
    flex: 1,
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivory,
  },
  authBtnRow: {
    flexDirection: 'row',
    gap: 10,
  },
  flexBtn: {
    flex: 1,
  },
  confirmTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 20,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginTop: 10,
    marginBottom: 6,
  },
  confirmBody: {
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivory,
    marginBottom: 14,
  },
  fieldGroupLabel: {
    fontSize: 11,
    letterSpacing: 1.6,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 8,
  },
  chipsScroll: {
    marginBottom: 14,
  },
  historySection: {
    marginTop: MilaSpacing.md,
  },
  enquiryTop: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 6,
  },
  enquiryRef: {
    fontSize: 13,
    fontWeight: '700',
    letterSpacing: 1,
    color: MilaColors.champagneGold,
  },
  enquiryProperty: {
    fontFamily: MilaFonts.serif,
    fontSize: 17,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  enquiryMeta: {
    fontSize: 13,
    color: MilaColors.ivoryMuted,
    marginTop: 4,
  },
});
