import { Ionicons } from '@expo/vector-icons';
import { BottomTabNavigationProp } from '@react-navigation/bottom-tabs';
import { CompositeNavigationProp, useNavigation } from '@react-navigation/native';
import { NativeStackNavigationProp } from '@react-navigation/native-stack';
import React, { useMemo, useState } from 'react';
import { Alert, Pressable, ScrollView, StyleSheet, Text, TextInput, View } from 'react-native';
import {
  MilaCard,
  MilaChip,
  MilaScreenShell,
  MilaSectionHeading,
  MilaStatusBadge,
  SupabaseConfigNotice,
} from '../../components/MilaUI';
import { useMilaApp } from '../../context/MilaAppContext';
import { TalentStackParamList, TalentTabParamList } from '../../navigation/types';
import { MilaColors, MilaFonts, MilaRadii, MilaSpacing } from '../../theme/milaTheme';
import {
  EXPERIENCE_FILTERS,
  ExperienceFilter,
  JobVacancy,
  REGION_FILTERS,
  ROLE_CATEGORY_OPTIONS,
  RoleCategory,
} from '../../types/mila';

type Nav = CompositeNavigationProp<
  BottomTabNavigationProp<TalentTabParamList, 'JobSearch'>,
  NativeStackNavigationProp<TalentStackParamList>
>;

export const JobSearchScreen: React.FC = () => {
  const navigation = useNavigation<Nav>();
  const { vacancies, isJobSaved, toggleSaveJob, hasAppliedToJob, refreshAll } = useMilaApp();

  const [query, setQuery] = useState('');
  const [selectedCategory, setSelectedCategory] = useState<RoleCategory>('ALL');
  const [selectedRegion, setSelectedRegion] = useState<string>('All India');
  const [selectedExp, setSelectedExp] = useState<ExperienceFilter>('ANY');
  const [showFilters, setShowFilters] = useState(true);
  const [refreshing, setRefreshing] = useState(false);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    return vacancies.filter((v) => {
      if (selectedCategory !== 'ALL' && v.category !== selectedCategory) return false;
      if (
        selectedRegion !== 'All India' &&
        !v.city_region.toLowerCase().includes(selectedRegion.toLowerCase()) &&
        !v.location.toLowerCase().includes(selectedRegion.toLowerCase())
      ) {
        return false;
      }
      if (selectedExp === '0_5' && v.min_experience_years > 5) return false;
      if (selectedExp === '5_10' && (v.min_experience_years < 5 || v.min_experience_years > 10)) {
        return false;
      }
      if (selectedExp === '10_PLUS' && v.min_experience_years < 10) return false;

      if (!q) return true;
      const haystack = [
        v.title,
        v.property_tier,
        v.location,
        v.city_region,
        v.summary,
        ...v.desired_skills,
      ]
        .join(' ')
        .toLowerCase();
      return haystack.includes(q);
    });
  }, [query, selectedCategory, selectedExp, selectedRegion, vacancies]);

  const handleToggleSave = async (vacancy: JobVacancy) => {
    try {
      await toggleSaveJob(vacancy);
    } catch (err) {
      Alert.alert('Sign In Required', err instanceof Error ? err.message : 'Unable to save job.', [
        { text: 'Cancel', style: 'cancel' },
        { text: 'Candidate Login', onPress: () => navigation.navigate('CandidateLogin') },
      ]);
    }
  };

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
          accessibilityLabel="Open saved jobs"
          onPress={() => navigation.navigate('SavedJobs')}
          style={styles.savedBtn}
        >
          <Ionicons name="bookmark-outline" size={16} color={MilaColors.champagneGold} />
          <Text style={styles.savedBtnText}>Saved</Text>
        </Pressable>
      }
    >
      <SupabaseConfigNotice />
      <MilaSectionHeading
        eyebrow="MILA TALENT DIRECTORY"
        title="Search Hospitality Jobs"
        subtitle={`${filtered.length} published ${filtered.length === 1 ? 'vacancy' : 'vacancies'} matching your criteria.`}
      />

      {/* Search Bar */}
      <View style={styles.searchRow}>
        <View style={styles.searchInputWrap}>
          <Ionicons name="search-outline" size={18} color={MilaColors.champagneGold} />
          <TextInput
            placeholder="Role, city, skill, or property tier..."
            placeholderTextColor={MilaColors.warmStone}
            value={query}
            onChangeText={setQuery}
            style={styles.searchInput}
          />
          {query.length > 0 ? (
            <Pressable accessibilityRole="button" onPress={() => setQuery('')}>
              <Ionicons name="close-circle" size={18} color={MilaColors.warmStone} />
            </Pressable>
          ) : null}
        </View>
        <Pressable
          accessibilityRole="button"
          accessibilityLabel="Toggle job filters"
          onPress={() => setShowFilters((prev) => !prev)}
          style={[styles.filterToggleBtn, showFilters && styles.filterToggleActive]}
        >
          <Ionicons
            name="options-outline"
            size={20}
            color={showFilters ? MilaColors.charcoalDeep : MilaColors.champagneGold}
          />
        </Pressable>
      </View>

      {/* Expandable Filter Panel */}
      {showFilters ? (
        <MilaCard>
          <Text style={styles.filterGroupLabel}>DEPARTMENT</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
            {ROLE_CATEGORY_OPTIONS.map((opt) => (
              <MilaChip
                key={opt.value}
                label={opt.label}
                selected={selectedCategory === opt.value}
                onPress={() => setSelectedCategory(opt.value)}
              />
            ))}
          </ScrollView>

          <Text style={styles.filterGroupLabel}>LOCATION / REGION</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false} style={styles.chipRow}>
            {REGION_FILTERS.map((region) => (
              <MilaChip
                key={region}
                label={region}
                selected={selectedRegion === region}
                onPress={() => setSelectedRegion(region)}
              />
            ))}
          </ScrollView>

          <Text style={styles.filterGroupLabel}>EXPERIENCE LEVEL</Text>
          <ScrollView horizontal showsHorizontalScrollIndicator={false}>
            {EXPERIENCE_FILTERS.map((exp) => (
              <MilaChip
                key={exp.value}
                label={exp.label}
                selected={selectedExp === exp.value}
                onPress={() => setSelectedExp(exp.value)}
              />
            ))}
          </ScrollView>
        </MilaCard>
      ) : null}

      {/* Vacancy Results */}
      {filtered.length === 0 ? (
        <MilaCard>
          <Text style={styles.emptyTitle}>No Matching Vacancies</Text>
          <Text style={styles.emptyBody}>
            Try clearing your department, location, or experience filter to browse all open MILA
            hospitality mandates.
          </Text>
        </MilaCard>
      ) : (
        filtered.map((vacancy) => {
          const saved = isJobSaved(vacancy);
          const existingApp = hasAppliedToJob(vacancy);
          return (
            <Pressable
              key={vacancy.id}
              accessibilityRole="button"
              onPress={() => navigation.navigate('JobDetail', { vacancy })}
            >
              <MilaCard>
                <View style={styles.cardTop}>
                  <View style={styles.badgesRow}>
                    <MilaStatusBadge label={vacancy.category.replace(/_/g, ' ')} />
                    {vacancy.is_urgent ? <MilaStatusBadge label="URGENT" tone="amber" /> : null}
                    {existingApp ? (
                      <MilaStatusBadge
                        label={existingApp.status.replace(/_/g, ' ')}
                        tone="emerald"
                      />
                    ) : null}
                  </View>
                  <Pressable
                    accessibilityRole="button"
                    accessibilityLabel={saved ? 'Remove saved job' : 'Save job'}
                    onPress={() => handleToggleSave(vacancy)}
                    style={styles.bookmarkHit}
                  >
                    <Ionicons
                      name={saved ? 'bookmark' : 'bookmark-outline'}
                      size={22}
                      color={MilaColors.champagneGold}
                    />
                  </Pressable>
                </View>

                <Text style={styles.jobTitle}>{vacancy.title}</Text>
                <Text style={styles.jobTier}>{vacancy.property_tier}</Text>
                <Text style={styles.jobSummary} numberOfLines={2}>
                  {vacancy.summary}
                </Text>

                <View style={styles.metaRow}>
                  <View style={styles.metaChip}>
                    <Ionicons name="location-outline" size={14} color={MilaColors.champagneGold} />
                    <Text style={styles.metaText}>{vacancy.location}</Text>
                  </View>
                  <View style={styles.metaChip}>
                    <Ionicons name="briefcase-outline" size={14} color={MilaColors.champagneGold} />
                    <Text style={styles.metaText}>{vacancy.experience_required}</Text>
                  </View>
                </View>
              </MilaCard>
            </Pressable>
          );
        })
      )}
    </MilaScreenShell>
  );
};

const styles = StyleSheet.create({
  savedBtn: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    borderRadius: MilaRadii.pill,
    paddingHorizontal: 12,
    minHeight: 36,
  },
  savedBtnText: {
    color: MilaColors.champagneGold,
    fontSize: 12,
    fontWeight: '700',
  },
  searchRow: {
    flexDirection: 'row',
    gap: 10,
    marginBottom: MilaSpacing.md,
  },
  searchInputWrap: {
    flex: 1,
    flexDirection: 'row',
    alignItems: 'center',
    backgroundColor: MilaColors.charcoalDeep,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    borderRadius: MilaRadii.md,
    paddingHorizontal: 12,
    minHeight: 48,
    gap: 8,
  },
  searchInput: {
    flex: 1,
    color: MilaColors.ivory,
    fontSize: 14,
  },
  filterToggleBtn: {
    width: 48,
    height: 48,
    borderRadius: MilaRadii.md,
    backgroundColor: MilaColors.charcoalSurface,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    alignItems: 'center',
    justifyContent: 'center',
  },
  filterToggleActive: {
    backgroundColor: MilaColors.champagneGold,
  },
  filterGroupLabel: {
    fontSize: 10,
    letterSpacing: 1.6,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 8,
    marginTop: 4,
  },
  chipRow: {
    marginBottom: 10,
  },
  cardTop: {
    flexDirection: 'row',
    justifyContent: 'space-between',
    alignItems: 'flex-start',
    marginBottom: 6,
  },
  badgesRow: {
    flex: 1,
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 6,
  },
  bookmarkHit: {
    width: 40,
    height: 40,
    alignItems: 'flex-end',
    justifyContent: 'flex-start',
  },
  jobTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  jobTier: {
    fontSize: 13,
    color: MilaColors.champagneGold,
    marginTop: 2,
  },
  jobSummary: {
    fontSize: 13,
    lineHeight: 19,
    color: MilaColors.ivoryMuted,
    marginTop: 6,
    marginBottom: 10,
  },
  metaRow: {
    flexDirection: 'row',
    flexWrap: 'wrap',
    gap: 14,
  },
  metaChip: {
    flexDirection: 'row',
    alignItems: 'center',
    gap: 5,
  },
  metaText: {
    fontSize: 12,
    color: MilaColors.ivoryMuted,
  },
  emptyTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    color: MilaColors.ivory,
    marginBottom: 6,
  },
  emptyBody: {
    fontSize: 13,
    color: MilaColors.ivoryMuted,
    lineHeight: 19,
  },
});
