import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { Logo } from '../../components/common/Logo';
import { UserRole } from '../../types';
import { Mail, Lock, User, Sparkles, ArrowRight, ShieldCheck } from 'lucide-react';

interface AuthScreenProps {
  onSuccess: () => void;
}

export const AuthScreen: React.FC<AuthScreenProps> = ({ onSuccess }) => {
  const { loginWithEmail, registerWithEmail, loginAsDemoClient, loginAsDemoDriver } = useAuth();
  const [isRegister, setIsRegister] = useState(false);
  const [name, setName] = useState('');
  const [email, setEmail] = useState('');
  const [password, setPassword] = useState('');
  const [role, setRole] = useState<UserRole>('client');
  const [errorMsg, setErrorMsg] = useState<string | null>(null);
  const [isLoading, setIsLoading] = useState(false);

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    setErrorMsg(null);
    if (!email || !password || (isRegister && !name)) {
      setErrorMsg('Veuillez remplir tous les champs');
      return;
    }

    try {
      setIsLoading(true);
      if (isRegister) {
        await registerWithEmail(name, email, password, role);
      } else {
        await loginWithEmail(email, password);
      }
      setIsLoading(false);
      onSuccess();
    } catch (e: any) {
      setIsLoading(false);
      setErrorMsg(e?.message || 'Erreur lors de la connexion');
    }
  };

  return (
    <div className="min-h-screen bg-slate-50 flex items-center justify-center p-4">
      <div className="bg-white rounded-3xl max-w-md w-full p-6 sm:p-8 shadow-2xl border border-slate-200 space-y-6">
        {/* Logo */}
        <div className="text-center space-y-2">
          <Logo size="lg" showTagline={true} className="justify-center" />
          <h2 className="text-xl font-black text-slate-900 pt-2">
            {isRegister ? 'Créer un compte WÀNDÉ' : 'Bon retour sur WÀNDÉ'}
          </h2>
          <p className="text-xs text-slate-500">
            {isRegister
              ? 'Rejoignez la plateforme de livraison ultra-rapide'
              : 'Connectez-vous pour envoyer ou livrer des colis'}
          </p>
        </div>

        {/* Demo Fast Access Buttons */}
        <div className="bg-blue-50 border border-blue-200 rounded-2xl p-3.5 space-y-2 text-xs">
          <div className="flex items-center gap-1.5 font-extrabold text-blue-900">
            <Sparkles className="w-4 h-4 text-amber-500" />
            <span>Accès Démo Instantané en 1 Clic :</span>
          </div>
          <div className="grid grid-cols-2 gap-2">
            <button
              type="button"
              onClick={() => {
                loginAsDemoClient();
                onSuccess();
              }}
              className="bg-white hover:bg-blue-100 text-blue-700 font-bold py-2 px-3 rounded-xl border border-blue-200 shadow-sm transition-all text-xs"
            >
              👤 Client Démo
            </button>
            <button
              type="button"
              onClick={() => {
                loginAsDemoDriver();
                onSuccess();
              }}
              className="bg-blue-600 hover:bg-blue-700 text-white font-bold py-2 px-3 rounded-xl shadow-sm transition-all text-xs"
            >
              🛵 Livreur Démo
            </button>
          </div>
        </div>

        <div className="relative flex items-center justify-center">
          <div className="border-t border-slate-200 w-full"></div>
          <span className="bg-white px-3 text-xs font-bold text-slate-400 uppercase">Ou</span>
        </div>

        {/* Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          {isRegister && (
            <>
              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-700 block">Nom complet :</label>
                <div className="relative">
                  <input
                    type="text"
                    value={name}
                    onChange={(e) => setName(e.target.value)}
                    placeholder="Ex: Awa Traoré"
                    className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 pl-10 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                  />
                  <User className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
                </div>
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-700 block">Vous êtes :</label>
                <div className="grid grid-cols-2 gap-2">
                  <button
                    type="button"
                    onClick={() => setRole('client')}
                    className={`py-2 rounded-xl text-xs font-bold border transition-all ${
                      role === 'client'
                        ? 'bg-blue-50 border-blue-600 text-blue-700'
                        : 'bg-slate-50 border-slate-200 text-slate-600'
                    }`}
                  >
                    👤 Client (Envoyer)
                  </button>
                  <button
                    type="button"
                    onClick={() => setRole('driver')}
                    className={`py-2 rounded-xl text-xs font-bold border transition-all ${
                      role === 'driver'
                        ? 'bg-blue-50 border-blue-600 text-blue-700'
                        : 'bg-slate-50 border-slate-200 text-slate-600'
                    }`}
                  >
                    🛵 Livreur (Gagner)
                  </button>
                </div>
              </div>
            </>
          )}

          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700 block">Adresse Email :</label>
            <div className="relative">
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                placeholder="votre.email@exemple.com"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 pl-10 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
              <Mail className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            </div>
          </div>

          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700 block">Mot de passe :</label>
            <div className="relative">
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                placeholder="••••••••"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 pl-10 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
              <Lock className="w-4 h-4 text-slate-400 absolute left-3.5 top-1/2 -translate-y-1/2" />
            </div>
          </div>

          {errorMsg && (
            <p className="text-xs text-red-600 font-bold bg-red-50 p-2 rounded-lg border border-red-100">
              {errorMsg}
            </p>
          )}

          <button
            type="submit"
            disabled={isLoading}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3.5 rounded-2xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 text-sm transition-all"
          >
            <span>{isRegister ? "S'inscrire" : 'Se connecter'}</span>
            <ArrowRight className="w-4 h-4" />
          </button>
        </form>

        <div className="text-center">
          <button
            type="button"
            onClick={() => {
              setIsRegister(!isRegister);
              setErrorMsg(null);
            }}
            className="text-xs font-bold text-blue-600 hover:underline"
          >
            {isRegister
              ? 'Déjà un compte ? Se connecter'
              : "Pas encore de compte ? S'inscrire gratuitement"}
          </button>
        </div>
      </div>
    </div>
  );
};
