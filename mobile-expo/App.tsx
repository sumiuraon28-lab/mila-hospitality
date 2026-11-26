import * as SplashScreen from 'expo-splash-screen';
import { StatusBar } from 'expo-status-bar';
import React, { useEffect } from 'react';
import { Image, StyleSheet, Text, View } from 'react-native';
import { SafeAreaProvider } from 'react-native-safe-area-context';
import { MILA_LOGO_ICON } from './src/constants/brandAssets';
import { MilaAppProvider, useMilaApp } from './src/context/MilaAppContext';
import { AppNavigator } from './src/navigation/AppNavigator';
import { MilaColors, MilaFonts } from './src/theme/milaTheme';

SplashScreen.preventAutoHideAsync().catch(() => undefined);

const MilaRootGate: React.FC = () => {
  const { initializing } = useMilaApp();

  useEffect(() => {
    if (!initializing) {
      SplashScreen.hideAsync().catch(() => undefined);
    }
  }, [initializing]);

  if (initializing) {
    return (
      <View style={styles.splashFallback}>
        <Image source={MILA_LOGO_ICON} style={styles.splashCrest} accessibilityLabel="MILA Crest" />
        <Text style={styles.splashTitle}>MILA</Text>
        <Text style={styles.splashSub}>HOSPITALITY CONSULTANT</Text>
      </View>
    );
  }

  return <AppNavigator />;
};

export default function App() {
  return (
    <SafeAreaProvider>
      <StatusBar style="light" backgroundColor={MilaColors.charcoal} />
      <MilaAppProvider>
        <MilaRootGate />
      </MilaAppProvider>
    </SafeAreaProvider>
  );
}

const styles = StyleSheet.create({
  splashFallback: {
    flex: 1,
    backgroundColor: MilaColors.charcoal,
    alignItems: 'center',
    justifyContent: 'center',
  },
  splashCrest: {
    width: 96,
    height: 96,
    borderRadius: 24,
    borderWidth: 1,
    borderColor: MilaColors.goldBorder,
    marginBottom: 18,
  },
  splashTitle: {
    fontFamily: MilaFonts.serif,
    fontSize: 30,
    letterSpacing: 8,
    color: MilaColors.champagneGold,
    fontWeight: '700',
  },
  splashSub: {
    fontSize: 11,
    letterSpacing: 3.5,
    color: MilaColors.ivoryMuted,
    marginTop: 6,
  },
});
