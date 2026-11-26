import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useMemo, useState } from 'react';
import { Image, Pressable, ScrollView, StyleSheet, Text, View } from 'react-native';
import {
  MilaChip,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { resolveMilaImage } from '../../constants/brandAssets';
import { useMilaApp } from '../../context/MilaAppContext';
import { ClientStackParamList, ClientTabParamList } from '../../navigation/types';
import {
  MilaColors,
  MilaFonts,
  MilaRadii,
  MilaSpacing,
  useResponsiveLayout,
} from '../../theme/milaTheme';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<ClientTabParamList, 'ClientServices'>,
  NativeStackNavigationProp<ClientStackParamList>
>;

const DIVISION_FILTERS = ['All', 'MILA Consultancy', 'MILA Academy & Expertise', 'MILA Talent'];

export const ServicesScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const layout = useResponsiveLayout();
  const { services, refreshAll } = useMilaApp();
  const [selectedDivision, setSelectedDivision] = useState('All');
  const [refreshing, setRefreshing] = useState(false);

  const filtered = useMemo(() => {
    if (selectedDivision === 'All') return services;
    return services.filter((s) => s.division_name === selectedDivision);
  }, [selectedDivision, services]);

  const onRefresh = async () => {
    setRefreshing(true);
    await refreshAll();
    setRefreshing(false);
  };

  return (
    <MilaScreenShell showBrandHeader refreshing={refreshing} onRefresh={onRefresh}>
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="MILA HOSPITALITY CONSULTANCY"
        title="Our Services Catalogue"
        subtitle="Explore all published consultancy mandates synced directly with the MILA Supabase catalog."
      />

      <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.filterRow}>
        {DIVISION_FILTERS.map((division) => (
          <MilaChip
            key={division}
            label={division}
            selected={selectedDivision === division}
            onPress={() => setSelectedDivision(division)}
          />
        ))}
      </ScrollView>

      <View style={layout.isTablet ? styles.grid : undefined}>
        {filtered.map((service) => (
          <Pressable
            key={service.id}
            accessibilityRole="button"
            accessibilityLabel={`Open ${service.title}`}
            onPress={() => navigation.navigate('ServiceDetail', { service })}
            style={[styles.card, layout.isTablet && styles.gridCard]}
          >
            <Image
              source={resolveMilaImage(service.image_key)}
              style={styles.heroPhoto}
              accessibilityLabel={service.title}
            />
            <View style={styles.cardBody}>
              <View style={styles.metaRow}>
                <MilaStatusBadge label={service.division_name.toUpperCase()} />
                {service.typical_timeline ? (
                  <Text style={styles.timeline}>{service.typical_timeline}</Text>
                ) : null}
              </View>
              <Text style={styles.title}>{service.title}</Text>
              {service.subtitle ? <Text style={styles.subtitle}>{service.subtitle}</Text> : null}
              <Text style={styles.summary}>{service.summary}</Text>

              <View style={styles.actionRow}>
                <Text style={styles.detailLink}>Explore Deliverables</Text>
                <Ionicons name="arrow-forward" size={16} color={MilaColors.champagneGold} />
              </View>
            </View>
          </Pressable>
        ))}
      </View>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  filterRow: {
    marginBottom: MilaSpacing.md,
  },
  grid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  gridCard: {
    width: '48.8%',
  },
  card: {
    backgroundColor: MilaColors.charcoalSurface,
    borderRadius: MilaRadii.lg,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    overflow: 'hidden',
    marginBottom: MilaSpacing.md,
  },
  heroPhoto: {
    width: '100%',
    height: 175,
    resizeMode: 'cover',
  },
  cardBody: {
    padding: MilaSpacing.md,
  },
  metaRow: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'space-between',
    marginBottom: 10,
    gap: 8,
  },
  timeline: {
    fontSize: 12,
    color: MilaColors.warmStone,
    fontWeight: '600',
  },
  title: {
    fontFamily: MilaFonts.serif,
    fontSize: 19,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  subtitle: {
    fontSize: 13,
    color: MilaColors.champagneGold,
    marginTop: 3,
  },
  summary: {
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivoryMuted,
    marginTop: 8,
    marginBottom: 14,
  },
  actionRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  detailLink: {
    fontSize: 13,
    fontWeight: '700',
    color: MilaColors.champagneGold,
  },
});
