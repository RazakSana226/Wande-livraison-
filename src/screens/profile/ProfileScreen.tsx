import React from 'react';
import { useAuth } from '../../context/AuthContext';
import { isFirebaseConfigured } from '../../services/firebase';
import {
  User,
  Phone,
  Mail,
  MapPin,
  ShieldCheck,
  Bike,
  Sparkles,
  LogOut,
  ChevronRight,
  Database,
  Globe
} from 'lucide-react';

interface ProfileScreenProps {
  onNavigate: (screen: string) => void;
}

export const ProfileScreen: React.FC<ProfileScreenProps> = ({ onNavigate }) => {
  const { currentUser, activeRole, switchRole, loginAsDemoClient, loginAsDemoDriver, logout } = useAuth();
  const isFbOnline = isFirebaseConfigured();

  return (
    <div className="max-w-2xl mx-auto px-4 py-4 space-y-5 pb-24">
      {/* Header */}
      <div>
        <h1 className="text-xl sm:text-2xl font-black text-slate-900">Mon Profil</h1>
        <p className="text-xs text-slate-500 mt-0.5">
          Gérez vos informations personnelles et préférences
        </p>
      </div>

      {/* User Card */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-card flex items-center gap-4">
        {currentUser?.avatarUrl ? (
          <img
            src={currentUser.avatarUrl}
            alt={currentUser.name}
            className="w-16 h-16 rounded-2xl object-cover ring-4 ring-blue-500/20"
          />
        ) : (
          <div className="w-16 h-16 rounded-2xl bg-blue-100 text-blue-700 font-black text-2xl flex items-center justify-center">
            {currentUser?.name?.charAt(0) || 'U'}
          </div>
        )}

        <div className="flex-1">
          <div className="flex items-center gap-2">
            <h2 className="text-base sm:text-lg font-black text-slate-900">{currentUser?.name}</h2>
            <ShieldCheck className="w-4 h-4 text-blue-600" />
          </div>
          <p className="text-xs text-slate-500">{currentUser?.email}</p>
          <p className="text-xs font-semibold text-slate-700 mt-0.5">{currentUser?.phone}</p>
        </div>
      </div>

      {/* Role Switcher Card */}
      <div className="bg-slate-100/80 rounded-2xl p-4 border border-slate-200 space-y-3">
        <span className="text-xs font-extrabold text-slate-800 uppercase tracking-wider block">
          Mode d'utilisation actif :
        </span>
        <div className="grid grid-cols-2 gap-2">
          <button
            onClick={() => {
              switchRole('client');
              onNavigate('client_home');
            }}
            className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1 ${
              activeRole === 'client'
                ? 'bg-blue-600 text-white font-black shadow-md border-blue-600'
                : 'bg-white text-slate-700 font-semibold border-slate-200'
            }`}
          >
            <User className="w-5 h-5" />
            <span className="text-xs">Espace Client</span>
          </button>

          <button
            onClick={() => {
              switchRole('driver');
              onNavigate('driver_home');
            }}
            className={`p-3 rounded-xl border text-center transition-all flex flex-col items-center gap-1 ${
              activeRole === 'driver'
                ? 'bg-blue-600 text-white font-black shadow-md border-blue-600'
                : 'bg-white text-slate-700 font-semibold border-slate-200'
            }`}
          >
            <Bike className="w-5 h-5" />
            <span className="text-xs">Espace Livreur</span>
          </button>
        </div>
      </div>

      {/* Demo Switchers */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card space-y-2">
        <h3 className="text-xs font-black text-slate-900 uppercase tracking-wider flex items-center gap-1.5">
          <Sparkles className="w-4 h-4 text-amber-500" />
          <span>Bascule rapide comptes Démo (Test)</span>
        </h3>
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-2 pt-1">
          <button
            onClick={() => {
              loginAsDemoClient();
              onNavigate('client_home');
            }}
            className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 text-left transition-all text-xs"
          >
            <p className="font-bold text-slate-900">👤 Awa Traoré (Client)</p>
            <p className="text-[11px] text-slate-500">Créer des courses & négocier</p>
          </button>

          <button
            onClick={() => {
              loginAsDemoDriver();
              onNavigate('driver_home');
            }}
            className="p-3 rounded-xl bg-slate-50 hover:bg-blue-50 border border-slate-200 text-left transition-all text-xs"
          >
            <p className="font-bold text-slate-900">🛵 Bakary Koné (Livreur)</p>
            <p className="text-[11px] text-slate-500">Accepter & faire des contre-offres</p>
          </button>
        </div>
      </div>

      {/* Cloud / Firebase Status Banner */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card flex items-center justify-between text-xs">
        <div className="flex items-center gap-2.5">
          <Database className={`w-5 h-5 ${isFbOnline ? 'text-emerald-600' : 'text-blue-600'}`} />
          <div>
            <p className="font-bold text-slate-900">
              {isFbOnline ? 'Synchronisation Cloud Firebase' : 'Mode Stockage Local Réactif'}
            </p>
            <p className="text-[11px] text-slate-500">
              {isFbOnline
                ? 'Connecté à Firestore en temps réel'
                : '100% fonctionnel hors-ligne & instantané'}
            </p>
          </div>
        </div>
        <span
          className={`px-2.5 py-1 rounded-full font-black text-[10px] ${
            isFbOnline
              ? 'bg-emerald-50 text-emerald-700 border border-emerald-200'
              : 'bg-blue-50 text-blue-700 border border-blue-200'
          }`}
        >
          {isFbOnline ? 'En ligne' : 'Local + Offline'}
        </span>
      </div>

      {/* App Version Info */}
      <div className="text-center text-xs text-slate-400 pt-2 space-y-1">
        <p className="font-bold text-slate-600">WÀNDÉ Web Application v1.0.0</p>
        <p>Compatible Android, iOS, Tablette & Ordinateur • Déploiement GitHub Pages</p>
      </div>
    </div>
  );
};
