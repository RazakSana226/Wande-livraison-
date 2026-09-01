import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { Logo } from './Logo';
import { UserRole } from '../../types';
import { User, Bike, Sparkles, ShieldCheck, History, Plus } from 'lucide-react';

interface HeaderProps {
  currentScreen?: string;
  onNavigate?: (screen: string, deliveryId?: string) => void;
}

export const Header: React.FC<HeaderProps> = ({ currentScreen, onNavigate }) => {
  const { activeRole, switchRole, currentUser } = useAuth();

  return (
    <header className="sticky top-0 z-40 bg-white/95 backdrop-blur-md border-b border-slate-200 shadow-sm">
      <div className="max-w-5xl mx-auto px-4 sm:px-6 py-3 flex items-center justify-between">
        {/* Brand Logo */}
        <Logo
          size="md"
          showTagline={true}
          onClick={() => onNavigate && onNavigate(activeRole === 'driver' ? 'driver_home' : 'client_home')}
        />

        {/* Desktop Quick Nav */}
        <div className="hidden sm:flex items-center gap-2">
          {activeRole === 'client' ? (
            <>
              <button
                onClick={() => onNavigate && onNavigate('create_delivery')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                  currentScreen === 'create_delivery'
                    ? 'bg-blue-50 text-blue-600 border border-blue-200'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <Plus className="w-3.5 h-3.5" />
                <span>Nouvelle course</span>
              </button>
              <button
                onClick={() => onNavigate && onNavigate('history')}
                className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                  currentScreen === 'history'
                    ? 'bg-blue-50 text-blue-600 border border-blue-200'
                    : 'text-slate-600 hover:text-slate-900'
                }`}
              >
                <History className="w-3.5 h-3.5" />
                <span>Mes livraisons</span>
              </button>
            </>
          ) : (
            <button
              onClick={() => onNavigate && onNavigate('driver_verification')}
              className={`px-3 py-1.5 rounded-xl text-xs font-bold transition-all flex items-center gap-1.5 ${
                currentScreen === 'driver_verification'
                  ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
                  : 'text-slate-600 hover:text-slate-900'
              }`}
            >
              <ShieldCheck className="w-3.5 h-3.5 text-emerald-600" />
              <span>Vérification KYC</span>
            </button>
          )}
        </div>

        {/* Role Switcher Pill */}
        <div className="flex items-center gap-2 sm:gap-3">
          <div className="flex bg-slate-100 p-1 rounded-2xl border border-slate-200">
            <button
              onClick={() => {
                switchRole('client');
                if (onNavigate) onNavigate('client_home');
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold flex items-center gap-1.5 transition-all ${
                activeRole === 'client'
                  ? 'bg-white text-blue-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <User className="w-3.5 h-3.5" />
              <span>Client</span>
            </button>

            <button
              onClick={() => {
                switchRole('driver');
                if (onNavigate) onNavigate('driver_home');
              }}
              className={`px-3 py-1.5 rounded-xl text-xs font-extrabold flex items-center gap-1.5 transition-all ${
                activeRole === 'driver'
                  ? 'bg-white text-blue-600 shadow-sm'
                  : 'text-slate-500 hover:text-slate-900'
              }`}
            >
              <Bike className="w-3.5 h-3.5" />
              <span>Livreur</span>
            </button>
          </div>

          {/* User Profile Avatar */}
          <button
            onClick={() => onNavigate && onNavigate('profile')}
            className="w-9 h-9 rounded-2xl bg-blue-50 hover:bg-blue-100 text-blue-600 border border-blue-200/60 flex items-center justify-center font-bold text-xs transition-colors"
          >
            {currentUser?.avatarUrl ? (
              <img
                src={currentUser.avatarUrl}
                alt={currentUser.name}
                className="w-full h-full rounded-2xl object-cover"
              />
            ) : (
              currentUser?.name?.charAt(0) || 'U'
            )}
          </button>
        </div>
      </div>
    </header>
  );
};
