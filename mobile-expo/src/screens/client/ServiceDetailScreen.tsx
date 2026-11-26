import { Ionicons } from '@expo/vector-icons';
import { RouteProp, useNavigation, useRoute } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaStatusBadge,
} from '../../components/MilaUI';
import { resolveMilaImage } from '../../constants/brandAssets';
import { ClientStackParamList, ClientTabParamList } from '../../navigation/types';
import { MilaColors, MilaFonts, MilaRadii, MilaSpacing } from '../../theme/milaTheme';

export const ServiceDetailScreen: React.FC = () => {
  const route = useRoute<RouteProp<ClientStackParamList, 'ServiceDetail'>>();
  const navigation = useNavigation<NativeStackNavigationProp<ClientStackParamList>>();
  const { service } = route.params;

  const handleEnquire = () => {
    navigation.navigate('ClientTabs', {
      screen: 'ClientEnquiry',
      params: {
        preselectedServiceType: service.service_type,
        preselectedServiceId: service.id,
      },
    });
  };

  return (
    <MilaScreenShell>
      <Image
        source={resolveMilaImage(service.image_key)}
        style={styles.heroImage}
        accessibilityLabel={service.title}
      />

      <View style={styles.metaRow}>
        <MilaStatusBadge label={service.division_name.toUpperCase()} />
        {service.typical_timeline ? (
          <MilaStatusBadge label={`Timeline: ${service.typical_timeline}`} tone="neutral" />
        ) : null}
      </View>

      <Text style={styles.title}>{service.title}</Text>
      {service.subtitle ? <Text style={styles.subtitle}>{service.subtitle}</Text> : null}
      <Text style={styles.description}>{service.detailed_description}</Text>

      <MilaCard>
        <Text style={styles.blockEyebrow}>SERVICES INCLUDED</Text>
        {service.services_included.map((item) => (
          <View key={item} style={styles.listRow}>
            <Ionicons name="checkmark-circle" size={18} color={MilaColors.champagneGold} />
            <Text style={styles.listText}>{item}</Text>
          </View>
        ))}
      </MilaCard>

      <MilaCard>
        <Text style={styles.blockEyebrow}>BENEFITS FOR BUSINESS OWNERS</Text>
        {service.owner_benefits.map((item) => (
          <View key={item} style={styles.listRow}>
            <Ionicons name="trending-up-outline" size={18} color={MilaColors.emerald} />
            <Text style={styles.listText}>{item}</Text>
          </View>
        ))}
      </MilaCard>

      <MilaCard highlighted>
        <Text style={styles.blockEyebrow}>KEY DELIVERABLES</Text>
        {service.deliverables.map((item) => (
          <View key={item} style={styles.listRow}>
            <Ionicons name="document-text-outline" size={18} color={MilaColors.champagneGold} />
            <Text style={styles.listText}>{item}</Text>
          </View>
        ))}
      </MilaCard>

      <MilaButton
        label="Enquire Now"
        icon="paper-plane-outline"
        onPress={handleEnquire}
        style={styles.ctaBtn}
      />
    </MilaScreenShell>
  );
};

// Keep TypeScript reference to ClientTabParamList cleanly used
export type _ClientTabRef = ClientTabParamList;

const styles = StyleSheet.create({
  heroImage: {
    width: '100%',
    height: 230,
    borderRadius: MilaRadii.lg,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    marginBottom: MilaSpacing.md,
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 8,
    marginBottom: 10,
  },
  title: {
    fontFamily: MilaFonts.serif,
    fontSize: 26,
    lineHeight: 33,
    color: MilaColors.ivory,
    fontWeight: '700',
  },
  subtitle: {
    fontSize: 14,
    color: MilaColors.champagneGold,
    marginTop: 4,
    marginBottom: 12,
    fontWeight: '600',
  },
  description: {
    fontSize: 15,
    lineHeight: 23,
    color: MilaColors.ivoryMuted,
    marginBottom: MilaSpacing.lg,
  },
  blockEyebrow: {
    fontSize: 11,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 12,
  },
  listRow: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
    marginBottom: 10,
  },
  listText: {
    flex: 1,
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivory,
  },
  ctaBtn: {
    marginTop: 6,
  },
});
