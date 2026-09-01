import React, { useState, useEffect } from 'react';
import { AuthProvider, useAuth } from './context/AuthContext';
import { Header } from './components/common/Header';
import { BottomNav } from './components/common/BottomNav';
import { ClientHomeScreen } from './screens/client/ClientHomeScreen';
import { CreateDeliveryScreen } from './screens/client/CreateDeliveryScreen';
import { TrackingScreen } from './screens/client/TrackingScreen';
import { HistoryScreen } from './screens/client/HistoryScreen';
import { DriverHomeScreen } from './screens/driver/DriverHomeScreen';
import { DriverVerificationScreen } from './screens/driver/DriverVerificationScreen';
import { ProfileScreen } from './screens/profile/ProfileScreen';
import { AuthScreen } from './screens/auth/AuthScreen';

const MainApp: React.FC = () => {
  const { currentUser, activeRole } = useAuth();
  const [currentScreen, setCurrentScreen] = useState<string>('client_home');
  const [selectedDeliveryId, setSelectedDeliveryId] = useState<string>('WND-73105');

  // Handle URL hash changes for deep linking on GitHub Pages
  useEffect(() => {
    const handleHashChange = () => {
      const hash = window.location.hash.replace('#', '');
      if (hash.startsWith('tracking/')) {
        const id = hash.replace('tracking/', '');
        setSelectedDeliveryId(id);
        setCurrentScreen('tracking');
      } else if (hash) {
        setCurrentScreen(hash);
      }
    };

    window.addEventListener('hashchange', handleHashChange);
    if (window.location.hash) {
      handleHashChange();
    }
    return () => window.removeEventListener('hashchange', handleHashChange);
  }, []);

  const handleNavigate = (screen: string, deliveryId?: string) => {
    if (deliveryId) {
      setSelectedDeliveryId(deliveryId);
      window.location.hash = `tracking/${deliveryId}`;
    } else {
      window.location.hash = screen;
    }
    setCurrentScreen(screen);
    window.scrollTo({ top: 0, behavior: 'smooth' });
  };

  return (
    <div className="min-h-screen bg-slate-50 text-slate-900 flex flex-col selection:bg-blue-500 selection:text-white">
      {/* Top sticky Header */}
      <Header currentScreen={currentScreen} onNavigate={handleNavigate} />

      {/* Main Content Area */}
      <main className="flex-1 w-full max-w-6xl mx-auto">
        {currentScreen === 'client_home' && (
          <ClientHomeScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'create_delivery' && (
          <CreateDeliveryScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'tracking' && (
          <TrackingScreen
            deliveryId={selectedDeliveryId}
            onNavigate={handleNavigate}
          />
        )}

        {currentScreen === 'history' && (
          <HistoryScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'driver_home' && (
          <DriverHomeScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'driver_verification' && (
          <DriverVerificationScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'profile' && (
          <ProfileScreen onNavigate={handleNavigate} />
        )}

        {currentScreen === 'auth' && (
          <AuthScreen onSuccess={() => handleNavigate(activeRole === 'driver' ? 'driver_home' : 'client_home')} />
        )}
      </main>

      {/* Mobile Bottom Navigation */}
      <BottomNav currentScreen={currentScreen} onNavigate={handleNavigate} />
    </div>
  );
};

export function App() {
  return (
    <AuthProvider>
      <MainApp />
    </AuthProvider>
  );
}

export default App;
