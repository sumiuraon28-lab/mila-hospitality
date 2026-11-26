import { Platform, useWindowDimensions } from 'react-native';

export const MilaColors = {
  charcoal: '#191D1B',
  charcoalDeep: '#111412',
  charcoalSurface: '#212623',
  charcoalElevated: '#282E2B',
  charcoalBorder: '#343C38',

  champagneGold: '#D6BD96',
  goldMuted: '#A99068',
  goldSubtle: 'rgba(214, 189, 150, 0.16)',
  goldBorder: 'rgba(214, 189, 150, 0.35)',

  ivory: '#F5F1E9',
  ivoryMuted: '#C9C3B8',
  warmStone: '#9C968B',

  emerald: '#6FA082',
  emeraldSubtle: 'rgba(111, 160, 130, 0.16)',
  terracotta: '#C96A5B',
  terracottaSubtle: 'rgba(201, 106, 91, 0.16)',
  amber: '#D4A259',
  amberSubtle: 'rgba(212, 162, 89, 0.16)',
} as const;

export const MilaFonts = {
  serif: Platform.select({
    ios: 'Georgia',
    android: 'serif',
    default: 'Georgia',
  }),
  sans: Platform.select({
    ios: 'System',
    android: 'sans-serif',
    default: 'System',
  }),
} as const;

export const MilaSpacing = {
  xs: 4,
  sm: 8,
  md: 16,
  lg: 24,
  xl: 32,
  xxl: 40,
} as const;

export const MilaRadii = {
  sm: 8,
  md: 12,
  lg: 16,
  xl: 20,
  pill: 999,
} as const;

export interface ResponsiveLayout {
  width: number;
  height: number;
  isTablet: boolean;
  isLandscape: boolean;
  columns: 1 | 2;
  horizontalPadding: number;
  maxContentWidth: number;
  heroHeight: number;
}

export function useResponsiveLayout(): ResponsiveLayout {
  const { width, height } = useWindowDimensions();
  const isTablet = width >= 720;
  const isLandscape = width > height;

  return {
    width,
    height,
    isTablet,
    isLandscape,
    columns: isTablet ? 2 : 1,
    horizontalPadding: isTablet ? 32 : 18,
    maxContentWidth: isTablet ? 960 : 600,
    heroHeight: isTablet ? 360 : 280,
  };
}
