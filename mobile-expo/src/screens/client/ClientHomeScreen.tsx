import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import { LinearGradient } from 'expo-linear-gradient';
import React, { useState } from 'react';
import { Image, ImageBackground, Pressable, StyleSheet, Text, View } from 'react-native';
import {
  MilaButton,
  MilaCard,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import {
  BUSINESS_DIVISIONS,
  IMPACT_METRICS,
  MILA_IMAGES,
  resolveMilaImage,
} from '../../constants/brandAssets';
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
  BottomTabNavigationProp<ClientTabParamList, 'ClientHome'>,
  NativeStackNavigationProp<ClientStackParamList>
>;

export const ClientHomeScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const layout = useResponsiveLayout();
  const {
    user,
    clientProfile,
    services,
    testimonials,
    refreshAll,
    setPortalMode,
  } = useMilaApp();
  const [refreshing, setRefreshing] = useState(false);

  const handleRefresh = async () => {
    setRefreshing(true);
    await refreshAll();
    setRefreshing(false);
  };

  return (
    <MilaScreenShell
      showBrandHeader
      refreshing={refreshing}
      onRefresh={handleRefresh}
      rightHeaderAction={
        <Pressable
          accessibilityRole="button"
          accessibilityLabel={user ? 'Client account' : 'Client sign in'}
          onPress={() => navigation.navigate('ClientLogin')}
          style={styles.accountPill}
        >
          <Ionicons
            name={user ? 'person-circle' : 'log-in-outline'}
            size={18}
            color={MilaColors.champagneGold}
          />
          <Text style={styles.accountPillText} numberOfLines={1}>
            {clientProfile?.full_name?.split(' ')[0] ?? (user ? 'Account' : 'Client Login')}
          </Text>
        </Pressable>
      }
    >
      <SupabaseConfigNotice />

      {/* Luxury Hero */}
      <View style={[styles.heroCard, { height: layout.heroHeight }]}>
        <ImageBackground
          source={MILA_IMAGES.hero_hotel}
          style={styles.heroBg}
          imageStyle={styles.heroImg}
        >
          <LinearGradient
            colors={['rgba(17,20,18,0.22)', 'rgba(17,20,18,0.82)', 'rgba(25,29,27,0.97)']}
            style={styles.heroGradient}
          >
            <MilaStatusBadge label="INDIA'S BESPOKE HOSPITALITY ADVISORY" />
            <Text style={styles.heroTitle}>Creating Excellence in Hospitality</Text>
            <Text style={styles.heroSubtitle}>
              Turnkey hotel pre-openings, restaurant & café setups, three-time champion mixology,
              and executive hospitality recruitment across India.
            </Text>
            <View style={styles.heroActions}>
              <MilaButton
                label="Explore Services"
                onPress={() => navigation.navigate('ClientServices')}
                style={styles.heroPrimaryBtn}
              />
              <MilaButton
                label="Enquire Now"
                variant="outlined"
                onPress={() => navigation.navigate('ClientEnquiry')}
                style={styles.heroSecondaryBtn}
              />
            </View>
          </LinearGradient>
        </ImageBackground>
      </View>

      {/* Signed-in Client Welcome (normal website flow — no forced dashboard) */}
      {user ? (
        <MilaCard highlighted>
          <View style={styles.welcomeRow}>
            <View style={styles.welcomeCopy}>
              <Text style={styles.welcomeEyebrow}>CLIENT ACCOUNT ACTIVE</Text>
              <Text style={styles.welcomeName}>
                Welcome back, {clientProfile?.full_name || user.email}
              </Text>
              {clientProfile?.company_name ? (
                <Text style={styles.welcomeSub}>
                  {clientProfile.designation} • {clientProfile.company_name} ({clientProfile.city})
                </Text>
              ) : null}
            </View>
            <Pressable
              accessibilityRole="button"
              onPress={() => navigation.navigate('ClientLogin')}
              style={styles.profileIconBtn}
            >
              <Ionicons name="settings-outline" size={20} color={MilaColors.champagneGold} />
            </Pressable>
          </View>
        </MilaCard>
      ) : null}

      {/* Experience Statistics */}
      <MilaSectionHeading
        eyebrow="PROVEN TRACK RECORD"
        title="Authority Forged Through Operations"
      />
      <View style={styles.metricsGrid}>
        {IMPACT_METRICS.map((metric) => (
          <View
            key={metric.label}
            style={[styles.metricTile, layout.isTablet && styles.metricTileTablet]}
          >
            <Text style={styles.metricFigure}>{metric.figure}</Text>
            <Text style={styles.metricLabel}>{metric.label}</Text>
            <Text style={styles.metricDetail}>{metric.detail}</Text>
          </View>
        ))}
      </View>

      {/* Three Business Divisions */}
      <MilaSectionHeading
        eyebrow="THREE SPECIALIZED DIVISIONS"
        title="Integrated Hospitality Leadership"
        subtitle="Advisory rigor, executive talent acquisition, and champion beverage & service mastery under one house."
      />
      {BUSINESS_DIVISIONS.map((div) => (
        <MilaCard key={div.id}>
          <View style={styles.divisionTop}>
            <Text style={styles.divisionCode}>{div.code}</Text>
            <View style={styles.divisionTitleWrap}>
              <Text style={styles.divisionName}>{div.name}</Text>
              <Text style={styles.divisionTagline}>{div.tagline}</Text>
            </View>
          </View>
          <Text style={styles.bodyCopy}>{div.description}</Text>
          <View style={styles.bulletList}>
            {div.highlights.map((item) => (
              <View key={item} style={styles.bulletRow}>
                <Ionicons name="diamond-outline" size={12} color={MilaColors.champagneGold} />
                <Text style={styles.bulletText}>{item}</Text>
              </View>
            ))}
          </View>
          <MilaButton
            label={div.id === 'div_talent' ? 'Open MILA Talent Portal' : 'Explore Division'}
            variant="outlined"
            onPress={() => {
              if (div.id === 'div_talent') {
                setPortalMode('TALENT');
              } else {
                navigation.navigate('ClientServices');
              }
            }}
          />
        </MilaCard>
      ))}

      {/* Featured Consultancy Services */}
      <MilaSectionHeading
        eyebrow="CONSULTANCY CATALOGUE"
        title="Eight Core Advisory Disciplines"
        subtitle="Select any mandate to review deliverables, owner benefits, and submit a tailored enquiry."
      />
      <View style={layout.isTablet ? styles.twoColGrid : undefined}>
        {services.slice(0, 4).map((service) => (
          <Pressable
            key={service.id}
            accessibilityRole="button"
            onPress={() => navigation.navigate('ServiceDetail', { service })}
            style={[styles.servicePreviewCard, layout.isTablet && styles.twoColItem]}
          >
            <Image
              source={resolveMilaImage(service.image_key)}
              style={styles.serviceThumb}
              accessibilityLabel={service.title}
            />
            <View style={styles.servicePreviewBody}>
              <Text style={styles.serviceDivision}>{service.division_name.toUpperCase()}</Text>
              <Text style={styles.serviceTitle}>{service.title}</Text>
              <Text style={styles.serviceSummary} numberOfLines={2}>
                {service.summary}
              </Text>
              <View style={styles.serviceFooter}>
                <Text style={styles.serviceLink}>View Mandate</Text>
                <Ionicons name="arrow-forward" size={15} color={MilaColors.champagneGold} />
              </View>
            </View>
          </Pressable>
        ))}
      </View>
      <MilaButton
        label={`View All ${services.length} Services`}
        variant="ghost"
        icon="grid-outline"
        onPress={() => navigation.navigate('ClientServices')}
        style={styles.mbLg}
      />

      {/* Specialist Mixology Feature */}
      <MilaCard highlighted>
        <Image
          source={MILA_IMAGES.mixology_bar}
          style={styles.mixologyImg}
          accessibilityLabel="MILA Specialist Mixology"
        />
        <Text style={styles.eyebrowInline}>MILA ACADEMY & EXPERTISE</Text>
        <Text style={styles.cardHeading}>Three-Time Champion Mixologist</Text>
        <Text style={styles.bodyCopy}>
          Bespoke Indian botanical cocktail programs, zero-waste prep laboratories, ergonomic
          speed-bar design, and disciplined pour-cost training for hotels and cocktail lounges.
        </Text>
        <MilaButton
          label="Enquire for Bar & Beverage"
          onPress={() =>
            navigation.navigate('ClientEnquiry', {
              preselectedServiceType: 'BAR_BEVERAGE_CONSULTANCY',
            })
          }
        />
      </MilaCard>

      {/* Testimonials */}
      <MilaSectionHeading eyebrow="CLIENT PERSPECTIVES" title="Trusted Across Indian Hospitality" />
      {testimonials.map((t) => (
        <MilaCard key={t.id}>
          <Text style={styles.quoteMark}>“</Text>
          <Text style={styles.quoteText}>{t.quote}</Text>
          <Text style={styles.quoteAttribution}>{t.attribution_role}</Text>
          <Text style={styles.quoteContext}>{t.engagement_context}</Text>
        </MilaCard>
      ))}

      {/* Recruitment Callout */}
      <MilaCard style={styles.talentBanner}>
        <Text style={styles.eyebrowInline}>MILA TALENT — CAREER PORTAL</Text>
        <Text style={styles.cardHeading}>Looking for Your Next Hospitality Leadership Role?</Text>
        <Text style={styles.bodyCopy}>
          Create your private candidate profile, upload your CV, activate targeted job alerts, and
          apply directly to luxury hospitality mandates.
        </Text>
        <MilaButton
          label="Switch to MILA Talent"
          icon="briefcase-outline"
          onPress={() => setPortalMode('TALENT')}
        />
      </MilaCard>
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  accountPill: {
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
  accountPillText: {
    color: MilaColors.champagneGold,
    fontSize: 12,
    fontWeight: '700',
  },
  heroCard: {
    borderRadius: MilaRadii.xl,
    overflow: 'hidden',
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    marginBottom: MilaSpacing.lg,
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
    gap: 10,
  },
  heroTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 28,
    lineHeight: 34,
    color: MilaColors.ivory,
    fontWeight: '700',
  },
  heroSubtitle: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
  },
  heroActions: {
    flexDirection: 'row',
    gap: 10,
    marginTop: 4,
  },
  heroPrimaryBtn: {
    flex: 1,
  },
  heroSecondaryBtn: {
    flex: 1,
  },
  welcomeRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  welcomeCopy: {
    flex: 1,
  },
  welcomeEyebrow: {
    fontSize: 10,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  welcomeName: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginTop: 2,
  },
  welcomeSub: {
    fontSize: 12,
    color: MilaColors.ivoryMuted,
    marginTop: 2,
  },
  profileIconBtn: {
    width: 44,
    height: 44,
    borderRadius: 22,
    backgroundColor: MilaColors.charcoalDeep,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  metricsGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 10,
    marginBottom: MilaSpacing.lg,
  },
  metricTile: {
    width: '48%',
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    borderRadius: MilaRadii.lg,
    padding: 14,
  },
  metricTileTablet: {
    width: '23.5%',
  },
  metricFigure: {
    fontFamily: MilaFonts.serif,
    fontSize: 28,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  metricLabel: {
    fontSize: 13,
    fontWeight: '700',
    color: MilaColors.ivory,
    marginTop: 4,
  },
  metricDetail: {
    fontSize: 11,
    color: MilaColors.warmStone,
    marginTop: 2,
  },
  divisionTop: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    marginBottom: 10,
  },
  divisionCode: {
    fontFamily: MilaFonts.serif,
    fontSize: 24,
    color: MilaColors.champagneGold,
    marginRight: 12,
    fontWeight: '700',
  },
  divisionTitleWrap: {
    flex: 1,
  },
  divisionName: {
    fontFamily: MilaFonts.serif,
    fontSize: 19,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  divisionTagline: {
    fontSize: 12,
    color: MilaColors.champagneGold,
    marginTop: 2,
  },
  bodyCopy: {
    fontSize: 14,
    lineHeight: 21,
    color: MilaColors.ivoryMuted,
    marginBottom: 12,
  },
  bulletList: {
    gap: 6,
    marginBottom: 14,
  },
  bulletRow: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 8,
  },
  bulletText: {
    flex: 1,
    fontSize: 13,
    color: MilaColors.ivory,
  },
  twoColGrid: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    justifyContent: 'space-between',
  },
  twoColItem: {
    width: '48.8%',
  },
  servicePreviewCard: {
    backgroundColor: MilaColors.charcoalSurface,
    borderRadius: MilaRadii.lg,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    overflow: 'hidden',
    marginBottom: MilaSpacing.md,
  },
  serviceThumb: {
    width: '100%',
    height: 150,
    resizeMode: 'cover',
  },
  servicePreviewBody: {
    padding: 14,
  },
  serviceDivision: {
    fontSize: 10,
    letterSpacing: 1.5,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 4,
  },
  serviceTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 17,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 6,
  },
  serviceSummary: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginBottom: 10,
  },
  serviceFooter: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 6,
  },
  serviceLink: {
    fontSize: 13,
    fontWeight: '700',
    color: MilaColors.champagneGold,
  },
  mbLg: {
    marginBottom: MilaSpacing.lg,
  },
  mixologyImg: {
    width: '100%',
    height: 170,
    borderRadius: MilaRadii.md,
    marginBottom: 12,
    resizeMode: 'cover',
  },
  eyebrowInline: {
    fontSize: 10,
    letterSpacing: 1.8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 4,
  },
  cardHeading: {
    fontFamily: MilaFonts.serif,
    fontSize: 20,
    color: MilaColors.ivory,
    fontWeight: '600',
    marginBottom: 8,
  },
  quoteMark: {
    fontFamily: MilaFonts.serif,
    fontSize: 34,
    lineHeight: 34,
    color: MilaColors.champagneGold,
  },
  quoteText: {
    fontSize: 14,
    lineHeight: 22,
    color: MilaColors.ivory,
    fontStyle: 'italic',
    marginBottom: 10,
  },
  quoteAttribution: {
    fontSize: 13,
    fontWeight: '700',
    color: MilaColors.champagneGold,
  },
  quoteContext: {
    fontSize: 12,
    color: MilaColors.warmStone,
    marginTop: 2,
  },
  talentBanner: {
    borderColor: MilaColors.goldBorder,
  },
});
