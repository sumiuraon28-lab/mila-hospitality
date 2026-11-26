import { Ionicons } from '@expo/vector-icons';
import { createBottomTabNavigator } from '@react-navigation/bottom-tabs';
import { DarkTheme, NavigationContainer, Theme } from '@react-navigation/native';
import { createNativeStackNavigator } from '@react-navigation/native-stack';
import * as Notifications from 'expo-notifications';
import React, { useEffect } from 'react';
import { Platform } from 'react-native';
import { useMilaApp } from '../context/MilaAppContext';
import { AboutUsScreen } from '../screens/client/AboutUsScreen';
import { ClientLoginScreen, ClientRegisterScreen } from '../screens/client/ClientAuthScreens';
import { ClientHomeScreen } from '../screens/client/ClientHomeScreen';
import { ContactScreen } from '../screens/client/ContactScreen';
import { EnquiryFormScreen } from '../screens/client/EnquiryFormScreen';
import { ServiceDetailScreen } from '../screens/client/ServiceDetailScreen';
import { ServicesScreen } from '../screens/client/ServicesScreen';
import { AlertSettingsScreen } from '../screens/talent/AlertSettingsScreen';
import { CandidateLoginScreen, CandidateRegisterScreen } from '../screens/talent/CandidateAuthScreens';
import { CandidateProfileScreen } from '../screens/talent/CandidateProfileScreen';
import { CareerHomeScreen } from '../screens/talent/CareerHomeScreen';
import { JobDetailScreen } from '../screens/talent/JobDetailScreen';
import { JobSearchScreen } from '../screens/talent/JobSearchScreen';
import {
  ApplicationsScreen,
  SavedJobsScreen,
} from '../screens/talent/SavedAndApplicationsScreens';
import { MilaColors, MilaFonts } from '../theme/milaTheme';
import {
  ClientStackParamList,
  ClientTabParamList,
  TalentStackParamList,
  TalentTabParamList,
} from './types';

const ClientTab = createBottomTabNavigator<ClientTabParamList>();
const ClientStack = createNativeStackNavigator<ClientStackParamList>();
const TalentTab = createBottomTabNavigator<TalentTabParamList>();
const TalentStack = createNativeStackNavigator<TalentStackParamList>();

const MilaNavigationTheme: Theme = {
  ...DarkTheme,
  colors: {
    ...DarkTheme.colors,
    primary: MilaColors.champagneGold,
    background: MilaColors.charcoal,
    card: MilaColors.charcoalDeep,
    text: MilaColors.ivory,
    border: MilaColors.charcoalBorder,
    notification: MilaColors.champagneGold,
  },
};

const sharedTabBarStyle = {
  backgroundColor: MilaColors.charcoalDeep,
  borderTopColor: MilaColors.charcoalBorder,
  borderTopWidth: 1,
  height: Platform.OS === 'ios' ? 84 : 64,
  paddingTop: 6,
  paddingBottom: Platform.OS === 'ios' ? 24 : 8,
};

const sharedStackHeaderOptions = {
  headerStyle: { backgroundColor: MilaColors.charcoalDeep },
  headerTintColor: MilaColors.champagneGold,
  headerTitleStyle: {
    fontFamily: MilaFonts.serif,
    color: MilaColors.ivory,
    fontSize: 17,
  },
  headerShadowVisible: false,
  headerBackTitleVisible: false,
};

const ClientTabsNavigator: React.FC = () => (
  <ClientTab.Navigator
    screenOptions={({ route }) => ({
      headerShown: false,
      tabBarActiveTintColor: MilaColors.champagneGold,
      tabBarInactiveTintColor: MilaColors.warmStone,
      tabBarStyle: sharedTabBarStyle,
      tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
      tabBarIcon: ({ color, size, focused }) => {
        const iconMap: Record<keyof ClientTabParamList, [keyof typeof Ionicons.glyphMap, keyof typeof Ionicons.glyphMap]> = {
          ClientHome: ['home', 'home-outline'],
          ClientServices: ['briefcase', 'briefcase-outline'],
          ClientAbout: ['ribbon', 'ribbon-outline'],
          ClientEnquiry: ['paper-plane', 'paper-plane-outline'],
          ClientContact: ['call', 'call-outline'],
        };
        const [activeIcon, inactiveIcon] = iconMap[route.name];
        return <Ionicons name={focused ? activeIcon : inactiveIcon} size={size} color={color} />;
      },
    })}
  >
    <ClientTab.Screen name="ClientHome" component={ClientHomeScreen} options={{ title: 'Home' }} />
    <ClientTab.Screen name="ClientServices" component={ServicesScreen} options={{ title: 'Services' }} />
    <ClientTab.Screen name="ClientAbout" component={AboutUsScreen} options={{ title: 'About Us' }} />
    <ClientTab.Screen name="ClientEnquiry" component={EnquiryFormScreen} options={{ title: 'Enquire' }} />
    <ClientTab.Screen name="ClientContact" component={ContactScreen} options={{ title: 'Contact' }} />
  </ClientTab.Navigator>
);

const ClientStackNavigator: React.FC = () => (
  <ClientStack.Navigator screenOptions={sharedStackHeaderOptions}>
    <ClientStack.Screen
      name="ClientTabs"
      component={ClientTabsNavigator}
      options={{ headerShown: false }}
    />
    <ClientStack.Screen
      name="ServiceDetail"
      component={ServiceDetailScreen}
      options={({ route }) => ({ title: route.params.service.title })}
    />
    <ClientStack.Screen
      name="ClientLogin"
      component={ClientLoginScreen}
      options={{ title: 'Client Account' }}
    />
    <ClientStack.Screen
      name="ClientRegister"
      component={ClientRegisterScreen}
      options={{ title: 'Client Registration' }}
    />
  </ClientStack.Navigator>
);

const TalentTabsNavigator: React.FC = () => {
  const { unreadNotificationsCount } = useMilaApp();

  return (
    <TalentTab.Navigator
      screenOptions={({ route }) => ({
        headerShown: false,
        tabBarActiveTintColor: MilaColors.champagneGold,
        tabBarInactiveTintColor: MilaColors.warmStone,
        tabBarStyle: sharedTabBarStyle,
        tabBarLabelStyle: { fontSize: 11, fontWeight: '600' },
        tabBarIcon: ({ color, size, focused }) => {
          const iconMap: Record<keyof TalentTabParamList, [keyof typeof Ionicons.glyphMap, keyof typeof Ionicons.glyphMap]> = {
            CareerHome: ['sparkles', 'sparkles-outline'],
            JobSearch: ['search', 'search-outline'],
            Applications: ['document-text', 'document-text-outline'],
            CandidateProfile: ['person', 'person-outline'],
            AlertSettings: ['notifications', 'notifications-outline'],
          };
          const [activeIcon, inactiveIcon] = iconMap[route.name];
          return <Ionicons name={focused ? activeIcon : inactiveIcon} size={size} color={color} />;
        },
      })}
    >
      <TalentTab.Screen name="CareerHome" component={CareerHomeScreen} options={{ title: 'Careers' }} />
      <TalentTab.Screen name="JobSearch" component={JobSearchScreen} options={{ title: 'Jobs' }} />
      <TalentTab.Screen
        name="Applications"
        component={ApplicationsScreen}
        options={{ title: 'Status' }}
      />
      <TalentTab.Screen
        name="CandidateProfile"
        component={CandidateProfileScreen}
        options={{ title: 'Profile & CV' }}
      />
      <TalentTab.Screen
        name="AlertSettings"
        component={AlertSettingsScreen}
        options={{
          title: 'Alerts',
          tabBarBadge: unreadNotificationsCount > 0 ? unreadNotificationsCount : undefined,
          tabBarBadgeStyle: {
            backgroundColor: MilaColors.champagneGold,
            color: MilaColors.charcoalDeep,
            fontSize: 10,
            fontWeight: '700',
          },
        }}
      />
    </TalentTab.Navigator>
  );
};

const TalentStackNavigator: React.FC = () => (
  <TalentStack.Navigator screenOptions={sharedStackHeaderOptions}>
    <TalentStack.Screen
      name="TalentTabs"
      component={TalentTabsNavigator}
      options={{ headerShown: false }}
    />
    <TalentStack.Screen
      name="JobDetail"
      component={JobDetailScreen}
      options={{ title: 'Vacancy Details' }}
    />
    <TalentStack.Screen
      name="SavedJobs"
      component={SavedJobsScreen}
      options={{ title: 'Saved Jobs' }}
    />
    <TalentStack.Screen
      name="CandidateLogin"
      component={CandidateLoginScreen}
      options={{ title: 'Candidate Login' }}
    />
    <TalentStack.Screen
      name="CandidateRegister"
      component={CandidateRegisterScreen}
      options={{ title: 'Candidate Registration' }}
    />
  </TalentStack.Navigator>
);

export const AppNavigator: React.FC = () => {
  const { portalMode, setPortalMode, refreshAccountData } = useMilaApp();

  // Route candidate push notification taps cleanly to MILA Talent and refresh inbox
  useEffect(() => {
    if (Platform.OS === 'web') return;
    const subscription = Notifications.addNotificationResponseReceivedListener(() => {
      setPortalMode('TALENT');
      refreshAccountData().catch(() => undefined);
    });
    return () => subscription.remove();
  }, [refreshAccountData, setPortalMode]);

  return (
    <NavigationContainer theme={MilaNavigationTheme}>
      {portalMode === 'CLIENT' ? <ClientStackNavigator /> : <TalentStackNavigator />}
    </NavigationContainer>
  );
};
