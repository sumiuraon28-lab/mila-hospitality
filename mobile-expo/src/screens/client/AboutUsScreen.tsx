import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { useNavigation } from '@react-navigation/native';
import React from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaSectionHeading,
} from '../../components/MilaUI';
import {
  BUSINESS_DIVISIONS,
  IMPACT_METRICS,
  LEADERSHIP_PRINCIPLES,
  MILA_IMAGES,
  SERVICE_REGIONS,
} from '../../constants/brandAssets';
import { ClientTabParamList } from '../../navigation/types';
import { MilaColors, MilaFonts, MilaRadii, MilaSpacing } from '../../theme/milaTheme';

export const AboutUsScreen: React.FC = () => {
  const navigation = useNavigation<BottomTabNavigationProp<ClientTabParamList, 'ClientAbout'>>();

  return (
    <MilaScreenShell showBrandHeader>
      <Image
        source={MILA_IMAGES.palace_resort}
        style={styles.bannerImage}
        accessibilityLabel="MILA Luxury Hospitality Property"
      />

      <MilaSectionHeading
        eyebrow="ABOUT MILA HOSPITALITY CONSULTANT"
        title="Built by Operators for Indian Hospitality"
        subtitle="Combining 15+ years of luxury hotel pre-opening leadership, 20+ restaurant & café launches, award-winning three-time champion mixology, and executive recruitment."
      />

      <MilaCard highlighted>
        <Text style={styles.narrative}>
          MILA Hospitality Consultant partners with hospitality owners, developers, heritage
          properties, and culinary entrepreneurs across India. Every engagement is structured around
          commercial viability, disciplined pre-opening execution, and refined guest choreography.
        </Text>
        <View style={styles.statStrip}>
          {IMPACT_METRICS.map((m) => (
            <View key={m.label} style={styles.statItem}>
              <Text style={styles.statNum}>{m.figure}</Text>
              <Text style={styles.statLbl}>{m.label}</Text>
            </View>
          ))}
        </View>
      </MilaCard>

      <MilaSectionHeading eyebrow="OUR PRINCIPLES" title="How MILA Stewards Every Mandate" />
      {LEADERSHIP_PRINCIPLES.map((item, idx) => (
        <MilaCard key={item.title}>
          <Text style={styles.principleIndex}>{`0${idx + 1}`}</Text>
          <Text style={styles.principleTitle}>{item.title}</Text>
          <Text style={styles.principleBody}>{item.detail}</Text>
        </MilaCard>
      ))}

      <MilaSectionHeading eyebrow="OUR DIVISIONS" title="Three Pillars of Practice" />
      {BUSINESS_DIVISIONS.map((div) => (
        <MilaCard key={div.id}>
          <Text style={styles.principleIndex}>{div.code}</Text>
          <Text style={styles.principleTitle}>{div.name}</Text>
          <Text style={styles.principleBody}>{div.description}</Text>
        </MilaCard>
      ))}

      <MilaSectionHeading
        eyebrow="PAN-INDIA PRESENCE"
        title="Key Hospitality Corridors"
      />
      {SERVICE_REGIONS.map((item) => (
        <MilaCard key={item.region}>
          <View style={styles.regionRow}>
            <Ionicons name="location-outline" size={18} color={MilaColors.champagneGold} />
            <Text style={styles.regionTitle}>{item.region}</Text>
          </View>
          <Text style={styles.principleBody}>{item.focus}</Text>
        </MilaCard>
      ))}

      <MilaButton
        label="Start a Consultancy Enquiry"
        icon="paper-plane-outline"
        onPress={() => navigation.navigate('ClientEnquiry')}
      />
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  bannerImage: {
    width: '100%',
    height: 190,
    borderRadius: MilaRadii.lg,
    marginBottom: MilaSpacing.md,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
  },
  narrative: {
    fontSize: 14,
    lineHeight: 22,
    color: MilaColors.ivory,
    marginBottom: 16,
  },
  statStrip: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
    borderTopWidth: StyleSheet.hairlineWidth,
    borderTopColor: MilaColors.charcoalBorder,
    paddingTop: 12,
    gap: 10,
  },
  statItem: {
    width: '46%',
  },
  statNum: {
    fontFamily: MilaFonts.serif,
    fontSize: 22,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  statLbl: {
    fontSize: 12,
    color: MilaColors.ivoryMuted,
  },
  principleIndex: {
    fontFamily: MilaFonts.serif,
    fontSize: 16,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 4,
  },
  principleTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 6,
  },
  principleBody: {
    fontSize: 14,
    lineHeight: 21,
    color: MilaColors.ivoryMuted,
  },
  regionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
    marginBottom: 6,
  },
  regionTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 17,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
});
