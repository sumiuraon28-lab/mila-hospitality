import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React from 'react';
import { StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaSectionHeading,
} from '../../components/MilaUI';
import { SERVICE_REGIONS } from '../../constants/brandAssets';
import { useMilaApp } from '../../context/MilaAppContext';
import { ClientStackParamList, ClientTabParamList } from '../../navigation/types';
import { MilaColors, MilaFonts, MilaSpacing } from '../../theme/milaTheme';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<ClientTabParamList, 'ClientContact'>,
  NativeStackNavigationProp<ClientStackParamList>
>;

export const ContactScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { user, setPortalMode } = useMilaApp();

  return (
    <MilaScreenShell showBrandHeader>
      <MilaSectionHeading
        eyebrow="CONNECT WITH MILA"
        title="Contact Our Advisory Desk"
        subtitle="Engage MILA Hospitality Consultant for hotel pre-openings, restaurant & café concepts, specialist mixology, and executive recruitment across India."
      />

      <MilaCard highlighted>
        <View style={styles.row}>
          <Ionicons name="business-outline" size={20} color={MilaColors.champagneGold} />
          <View style={styles.rowText}>
            <Text style={styles.label}>CLIENT CONSULTANCY ENQUIRIES</Text>
            <Text style={styles.value}>Confidential brief review within 24 business hours</Text>
          </View>
        </View>

        <View style={styles.row}>
          <Ionicons name="globe-outline" size={20} color={MilaColors.champagneGold} />
          <View style={styles.rowText}>
            <Text style={styles.label}>OPERATING CORRIDORS</Text>
            <Text style={styles.value}>
              Mumbai • New Delhi NCR • Bengaluru • Rajasthan • Goa • Pan-India
            </Text>
          </View>
        </View>

        <View style={styles.row}>
          <Ionicons name="shield-checkmark-outline" size={20} color={MilaColors.emerald} />
          <View style={styles.rowText}>
            <Text style={styles.label}>CLIENT CONFIDENTIALITY</Text>
            <Text style={styles.value}>
              Every project brief and client record is strictly private and isolated per account.
            </Text>
          </View>
        </View>

        <View style={styles.actions}>
          <MilaButton
            label="Submit Consultancy Enquiry"
            icon="paper-plane-outline"
            onPress={() => navigation.navigate('ClientEnquiry')}
          />
          <MilaButton
            label={user ? 'Manage Client Account' : 'Client Login / Register'}
            variant="outlined"
            icon="person-outline"
            onPress={() => navigation.navigate('ClientLogin')}
          />
        </View>
      </MilaCard>

      <MilaSectionHeading eyebrow="REGIONAL COVERAGE" title="Advisory Desks Across India" />
      {SERVICE_REGIONS.map((item) => (
        <MilaCard key={item.region}>
          <Text style={styles.regionTitle}>{item.region}</Text>
          <Text style={styles.regionFocus}>{item.focus}</Text>
        </MilaCard>
      ))}

      <MilaCard>
        <Text style={styles.label}>FOR HOSPITALITY PROFESSIONALS</Text>
        <Text style={styles.regionTitle}>MILA Talent Career Desk</Text>
        <Text style={styles.regionFocus}>
          Looking for leadership, culinary, mixology, or pre-opening roles? Access the dedicated
          job seeker portal to upload your CV and receive matched alerts.
        </Text>
        <View style={styles.mtMd}>
          <MilaButton
            label="Open MILA Talent"
            variant="outlined"
            icon="briefcase-outline"
            onPress={() => setPortalMode('TALENT')}
          />
        </View>
      </MilaCard>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  row: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 12,
    marginBottom: 14,
  },
  rowText: {
    flex: 1,
  },
  label: {
    fontSize: 10,
    letterSpacing: 1.6,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 3,
  },
  value: {
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivory,
  },
  actions: {
    gap: 10,
    marginTop: 6,
  },
  regionTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 17,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 4,
  },
  regionFocus: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
  },
  mtMd: {
    marginTop: MilaSpacing.md,
  },
});
