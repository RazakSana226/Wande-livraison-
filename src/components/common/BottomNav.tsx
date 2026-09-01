import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Home, PlusCircle, History, User, Bike, ShieldCheck } from 'lucide-react';

interface BottomNavProps {
  currentScreen: string;
  onNavigate: (screen: string) => void;
}

export const BottomNav: React.FC<BottomNavProps> = ({ currentScreen, onNavigate }) => {
  const { activeRole } = useAuth();

  return (
    <div className="md:hidden fixed bottom-0 left-0 right-0 z-40 bg-white/95 backdrop-blur-md border-t border-slate-200 safe-area-pb">
      <div className="flex items-center justify-around h-16 px-2 max-w-lg mx-auto">
        {activeRole === 'client' ? (
          <>
            <button
              onClick={() => onNavigate('client_home')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'client_home' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <Home className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Accueil</span>
            </button>

            <button
              onClick={() => onNavigate('create_delivery')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'create_delivery' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <div className="w-9 h-9 -mt-4 bg-blue-600 text-white rounded-full flex items-center justify-center shadow-lg shadow-blue-500/30 border-2 border-white">
                <PlusCircle className="w-5 h-5" />
              </div>
              <span className="text-[11px] mt-0.5">Envoyer</span>
            </button>

            <button
              onClick={() => onNavigate('history')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'history' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <History className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Historique</span>
            </button>

            <button
              onClick={() => onNavigate('profile')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'profile' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <User className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Profil</span>
            </button>
          </>
        ) : (
          <>
            <button
              onClick={() => onNavigate('driver_home')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'driver_home' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <Bike className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Courses</span>
            </button>

            <button
              onClick={() => onNavigate('driver_verification')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'driver_verification' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <ShieldCheck className="w-5 h-5 mb-1" />
              <span className="text-[11px]">KYC</span>
            </button>

            <button
              onClick={() => onNavigate('history')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'history' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <History className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Historique</span>
            </button>

            <button
              onClick={() => onNavigate('profile')}
              className={`flex flex-col items-center justify-center flex-1 py-1 transition-all ${
                currentScreen === 'profile' ? 'text-blue-600 font-bold' : 'text-slate-500 font-medium'
              }`}
            >
              <User className="w-5 h-5 mb-1" />
              <span className="text-[11px]">Profil</span>
            </button>
          </>
        )}
      </div>
    </div>
  );
};
