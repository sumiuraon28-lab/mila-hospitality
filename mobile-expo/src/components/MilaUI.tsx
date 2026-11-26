import { Ionicons } from '@expo/vector-icons';
import React from 'react';
import {
  ActivityIndicator,
  Image,
  KeyboardAvoidingView,
  Platform,
  Pressable,
  RefreshControl,
  ScrollView,
  StyleProp,
  StyleSheet,
  Text,
  TextInput,
  TextInputProps,
  View,
  ViewStyle,
} from 'react-native';
import { SafeAreaView } from 'react-native-safe-area-context';
import { MILA_LOGO_ICON } from '../constants/brandAssets';
import { useMilaApp } from '../context/MilaAppContext';
import {
  MilaColors,
  MilaFonts,
  MilaRadii,
  MilaSpacing,
  useResponsiveLayout,
} from '../theme/milaTheme';
import { PortalMode } from '../types/mila';

export const MilaBrandHeader: React.FC<{
  showPortalSwitcher?: boolean;
  rightAction?: React.ReactNode;
}> = ({ showPortalSwitcher = true, rightAction }) => {
  const { portalMode, setPortalMode } = useMilaApp();

  const renderToggle = (mode: PortalMode, label: string) => {
    const active = portalMode === mode;
    return (
      <Pressable
        key={mode}
        accessibilityRole="button"
        accessibilityLabel={`Switch to ${label}`}
        onPress={() => setPortalMode(mode)}
        style={[styles.switchPill, active && styles.switchPillActive]}
      >
        <Text style={[styles.switchPillText, active && styles.switchPillTextActive]}>
          {label}
        </Text>
      </Pressable>
    );
  };

  return (
    <View style={styles.brandHeader}>
      <View style={styles.brandRow}>
        <Image source={MILA_LOGO_ICON} style={styles.brandLogo} accessibilityLabel="MILA Crest" />
        <View style={styles.brandTextWrap}>
          <Text style={styles.brandTitle}>MILA</Text>
          <Text style={styles.brandSubtitle}>HOSPITALITY CONSULTANT</Text>
        </View>
        {rightAction}
      </View>

      {showPortalSwitcher ? (
        <View style={styles.switcherTrack}>
          {renderToggle('CLIENT', 'Client Advisory')}
          {renderToggle('TALENT', 'MILA Talent')}
        </View>
      ) : null}
    </View>
  );
};

export const MilaScreenShell: React.FC<{
  children: React.ReactNode;
  showBrandHeader?: boolean;
  refreshing?: boolean;
  onRefresh?: () => void;
  scrollable?: boolean;
  rightHeaderAction?: React.ReactNode;
  contentContainerStyle?: StyleProp<ViewStyle>;
}> = ({
  children,
  showBrandHeader = false,
  refreshing = false,
  onRefresh,
  scrollable = true,
  rightHeaderAction,
  contentContainerStyle,
}) => {
  const layout = useResponsiveLayout();

  const innerContent = (
    <View
      style={[
        styles.centeredContent,
        {
          maxWidth: layout.maxContentWidth,
          paddingHorizontal: layout.horizontalPadding,
        },
        contentContainerStyle,
      ]}
    >
      {children}
    </View>
  );

  return (
    <SafeAreaView style={styles.safeArea} edges={['top', 'left', 'right']}>
      <KeyboardAvoidingView
        style={styles.flex}
        behavior={Platform.OS === 'ios' ? 'padding' : undefined}
      >
        {showBrandHeader ? (
          <View
            style={[
              styles.centeredHeader,
              {
                maxWidth: layout.maxContentWidth,
                paddingHorizontal: layout.horizontalPadding,
              },
            ]}
          >
            <MilaBrandHeader rightAction={rightHeaderAction} />
          </View>
        ) : null}

        {scrollable ? (
          <ScrollView
            style={styles.flex}
            contentContainerStyle={styles.scrollPaddingBottom}
            keyboardShouldPersistTaps="handled"
            showsVerticalScrollIndicator={false}
            refreshControl={
              onRefresh ? (
                <RefreshControl
                  refreshing={refreshing}
                  onRefresh={onRefresh}
                  tintColor={MilaColors.champagneGold}
                  colors={[MilaColors.champagneGold]}
                />
              ) : undefined
            }
          >
            {innerContent}
          </ScrollView>
        ) : (
          <View style={styles.flex}>{innerContent}</View>
        )}
      </KeyboardAvoidingView>
    </SafeAreaView>
  );
};

export const MilaCard: React.FC<{
  children: React.ReactNode;
  highlighted?: boolean;
  style?: StyleProp<ViewStyle>;
}> = ({ children, highlighted = false, style }) => (
  <View style={[styles.card, highlighted && styles.cardHighlighted, style]}>{children}</View>
);

export const MilaSectionHeading: React.FC<{
  eyebrow: string;
  title: string;
  subtitle?: string;
}> = ({ eyebrow, title, subtitle }) => (
  <View style={styles.sectionHeading}>
    <Text style={styles.eyebrow}>{eyebrow.toUpperCase()}</Text>
    <Text style={styles.sectionTitle}>{title}</Text>
    {subtitle ? <Text style={styles.sectionSubtitle}>{subtitle}</Text> : null}
  </View>
);

export const MilaButton: React.FC<{
  label: string;
  onPress: () => void;
  variant?: 'primary' | 'outlined' | 'ghost' | 'danger';
  icon?: keyof typeof Ionicons.glyphMap;
  loading?: boolean;
  disabled?: boolean;
  style?: StyleProp<ViewStyle>;
}> = ({
  label,
  onPress,
  variant = 'primary',
  icon,
  loading = false,
  disabled = false,
  style,
}) => {
  const isDisabled = disabled || loading;
  return (
    <Pressable
      accessibilityRole="button"
      accessibilityLabel={label}
      disabled={isDisabled}
      onPress={onPress}
      style={({ pressed }) => [
        styles.buttonBase,
        variant === 'primary' && styles.buttonPrimary,
        variant === 'outlined' && styles.buttonOutlined,
        variant === 'ghost' && styles.buttonGhost,
        variant === 'danger' && styles.buttonDanger,
        pressed && !isDisabled && styles.buttonPressed,
        isDisabled && styles.buttonDisabled,
        style,
      ]}
    >
      {loading ? (
        <ActivityIndicator
          size="small"
          color={variant === 'primary' ? MilaColors.charcoalDeep : MilaColors.champagneGold}
        />
      ) : (
        <View style={styles.buttonContent}>
          {icon ? (
            <Ionicons
              name={icon}
              size={18}
              color={
                variant === 'primary'
                  ? MilaColors.charcoalDeep
                  : variant === 'danger'
                    ? MilaColors.terracotta
                    : MilaColors.champagneGold
              }
              style={styles.buttonIcon}
            />
          ) : null}
          <Text
            style={[
              styles.buttonText,
              variant === 'primary' && styles.buttonTextPrimary,
              variant === 'outlined' && styles.buttonTextOutlined,
              variant === 'ghost' && styles.buttonTextGhost,
              variant === 'danger' && styles.buttonTextDanger,
            ]}
          >
            {label}
          </Text>
        </View>
      )}
    </Pressable>
  );
};

export const MilaInput: React.FC<
  TextInputProps & {
    label: string;
    helperText?: string;
    containerStyle?: StyleProp<ViewStyle>;
  }
> = ({ label, helperText, containerStyle, style, multiline, ...rest }) => (
  <View style={[styles.inputGroup, containerStyle]}>
    <Text style={styles.inputLabel}>{label}</Text>
    <TextInput
      placeholderTextColor={MilaColors.warmStone}
      multiline={multiline}
      textAlignVertical={multiline ? 'top' : 'center'}
      style={[styles.inputField, multiline && styles.inputMultiline, style]}
      {...rest}
    />
    {helperText ? <Text style={styles.helperText}>{helperText}</Text> : null}
  </View>
);

export const MilaChip: React.FC<{
  label: string;
  selected: boolean;
  onPress: () => void;
}> = ({ label, selected, onPress }) => (
  <Pressable
    accessibilityRole="button"
    accessibilityState={{ selected }}
    onPress={onPress}
    style={[styles.chip, selected && styles.chipSelected]}
  >
    <Text style={[styles.chipText, selected && styles.chipTextSelected]}>{label}</Text>
  </Pressable>
);

export const MilaStatusBadge: React.FC<{
  label: string;
  tone?: 'gold' | 'emerald' | 'amber' | 'terracotta' | 'neutral';
}> = ({ label, tone = 'gold' }) => {
  const palette = {
    gold: { bg: MilaColors.goldSubtle, border: MilaColors.goldBorder, text: MilaColors.champagneGold },
    emerald: { bg: MilaColors.emeraldSubtle, border: MilaColors.emerald, text: MilaColors.emerald },
    amber: { bg: MilaColors.amberSubtle, border: MilaColors.amber, text: MilaColors.amber },
    terracotta: {
      bg: MilaColors.terracottaSubtle,
      border: MilaColors.terracotta,
      text: MilaColors.terracotta,
    },
    neutral: {
      bg: MilaColors.charcoalElevated,
      border: MilaColors.charcoalBorder,
      text: MilaColors.ivoryMuted,
    },
  }[tone];

  return (
    <View style={[styles.badge, { backgroundColor: palette.bg, borderColor: palette.border }]}>
      <Text style={[styles.badgeText, { color: palette.text }]}>{label}</Text>
    </View>
  );
};

export const SupabaseConfigNotice: React.FC = () => {
  const { isSupabaseReady } = useMilaApp();
  if (isSupabaseReady) return null;

  return (
    <View style={styles.configBanner}>
      <Ionicons name="cloud-offline-outline" size={18} color={MilaColors.amber} />
      <Text style={styles.configBannerText}>
        Supabase env not set yet — browsing MILA editorial catalog. Add EXPO_PUBLIC_SUPABASE_URL &
        EXPO_PUBLIC_SUPABASE_ANON_KEY in mobile-expo/.env for live auth, enquiries, CV uploads &
        push sync.
      </Text>
    </View>
  );
};

const styles = StyleSheet.create({
  flex: { flex: 1 },
  safeArea: {
    flex: 1,
    backgroundColor: MilaColors.charcoal,
  },
  centeredHeader: {
    width: '100%',
    alignSelf: 'center',
    paddingTop: MilaSpacing.sm,
    paddingBottom: MilaSpacing.xs,
    backgroundColor: MilaColors.charcoal,
    borderBottomWidth: StyleSheet.hairlineWidth,
    borderBottomColor: MilaColors.charcoalBorder,
  },
  centeredContent: {
    width: '100%',
    alignSelf: 'center',
    paddingTop: MilaSpacing.md,
  },
  scrollPaddingBottom: {
    paddingBottom: 44,
  },
  brandHeader: {
    gap: 10,
    paddingBottom: 6,
  },
  brandRow: {
    flexDirection: 'row',
    alignItems: 'center',
  },
  brandLogo: {
    width: 36,
    height: 36,
    borderRadius: 9,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
  },
  brandTextWrap: {
    marginLeft: 10,
    flex: 1,
  },
  brandTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 18,
    letterSpacing: 3.5,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  brandSubtitle: {
    fontSize: 9,
    letterSpacing: 2,
    color: MilaColors.ivoryMuted,
    marginTop: 1,
  },
  switcherTrack: {
    flexDirection: 'row',
    backgroundColor: MilaColors.charcoalDeep,
    borderRadius: MilaRadii.pill,
    padding: 3,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
  },
  switchPill: {
    flex: 1,
    minHeight: 36,
    borderRadius: MilaRadii.pill,
    alignItems: 'center',
    justifyContent: 'center',
  },
  switchPillActive: {
    backgroundColor: MilaColors.champagneGold,
  },
  switchPillText: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 0.6,
    color: MilaColors.ivoryMuted,
  },
  switchPillTextActive: {
    color: MilaColors.charcoalDeep,
    fontWeight: '700',
  },
  card: {
    backgroundColor: MilaColors.charcoalSurface,
    borderRadius: MilaRadii.lg,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    padding: MilaSpacing.md,
    marginBottom: MilaSpacing.md,
  },
  cardHighlighted: {
    borderColor: MilaColors.goldBorder,
    backgroundColor: MilaColors.charcoalElevated,
  },
  sectionHeading: {
    marginBottom: MilaSpacing.md,
  },
  eyebrow: {
    fontSize: 11,
    letterSpacing: 2.2,
    color: MilaColors.champagneGold,
    fontWeight: '700',
    marginBottom: 4,
  },
  sectionTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 23,
    lineHeight: 30,
    color: MilaColors.ivory,
    fontWeight: '600',
  },
  sectionSubtitle: {
    fontSize: 14,
    lineHeight: 20,
    color: MilaColors.ivoryMuted,
    marginTop: 6,
  },
  buttonBase: {
    minHeight: 48,
    borderRadius: MilaRadii.md,
    paddingHorizontal: 18,
    paddingVertical: 12,
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonPrimary: {
    backgroundColor: MilaColors.champagneGold,
  },
  buttonOutlined: {
    backgroundColor: 'transparent',
    borderWidth: 1,
    borderColor: MilaColors.champagneGold,
  },
  buttonGhost: {
    backgroundColor: MilaColors.charcoalElevated,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
  },
  buttonDanger: {
    backgroundColor: MilaColors.terracottaSubtle,
    borderWidth: 1,
    borderColor: MilaColors.terracotta,
  },
  buttonPressed: {
    opacity: 0.85,
  },
  buttonDisabled: {
    opacity: 0.45,
  },
  buttonContent: {
    flexDirection: 'row',
    alignItems: 'center',
    justifyContent: 'center',
  },
  buttonIcon: {
    marginRight: 8,
  },
  buttonText: {
    fontSize: 14,
    fontWeight: '700',
    letterSpacing: 0.5,
  },
  buttonTextPrimary: {
    color: MilaColors.charcoalDeep,
  },
  buttonTextOutlined: {
    color: MilaColors.champagneGold,
  },
  buttonTextGhost: {
    color: MilaColors.ivory,
  },
  buttonTextDanger: {
    color: MilaColors.terracotta,
  },
  inputGroup: {
    marginBottom: 14,
  },
  inputLabel: {
    fontSize: 12,
    fontWeight: '600',
    letterSpacing: 0.5,
    color: MilaColors.champagneGold,
    marginBottom: 6,
  },
  inputField: {
    minHeight: 48,
    backgroundColor: MilaColors.charcoalDeep,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    borderRadius: MilaRadii.md,
    paddingHorizontal: 14,
    paddingVertical: 12,
    fontSize: 15,
    color: MilaColors.ivory,
  },
  inputMultiline: {
    minHeight: 100,
    paddingTop: 12,
  },
  helperText: {
    fontSize: 12,
    color: MilaColors.warmStone,
    marginTop: 4,
  },
  chip: {
    minHeight: 38,
    paddingHorizontal: 14,
    paddingVertical: 8,
    borderRadius: MilaRadii.pill,
    backgroundColor: MilaColors.charcoalDeep,
    borderWidth: 1,
    borderColor: MilaColors.charcoalBorder,
    justifyContent: 'center',
    marginRight: 8,
    marginBottom: 8,
  },
  chipSelected: {
    backgroundColor: MilaColors.goldSubtle,
    borderColor: MilaColors.champagneGold,
  },
  chipText: {
    fontSize: 13,
    fontWeight: '600',
    color: MilaColors.ivoryMuted,
  },
  chipTextSelected: {
    color: MilaColors.champagneGold,
  },
  badge: {
    alignSelf: 'flex-start',
    paddingHorizontal: 10,
    paddingVertical: 4,
    borderRadius: MilaRadii.pill,
    borderWidth: 1,
  },
  badgeText: {
    fontSize: 11,
    fontWeight: '700',
    letterSpacing: 0.6,
  },
  configBanner: {
    flexDirection: 'row',
    alignItems: 'flex-start',
    gap: 10,
    backgroundColor: MilaColors.amberSubtle,
    borderColor: MilaColors.amber,
    borderWidth: 1,
    borderRadius: MilaRadii.md,
    padding: 12,
    marginBottom: MilaSpacing.md,
  },
  configBannerText: {
    flex: 1,
    fontSize: 12,
    lineHeight: 17,
    color: MilaColors.ivory,
  },
});
