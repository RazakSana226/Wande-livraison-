import React, { useState } from 'react';
import { DriverProfile } from '../../types';
import { storageService } from '../../services/storageService';
import { X, ShieldCheck, Upload, CheckCircle2, AlertCircle } from 'lucide-react';

interface DriverVerificationModalProps {
  isOpen: boolean;
  onClose: () => void;
  driver: DriverProfile;
  onUpdated: (profile: DriverProfile) => void;
}

export const DriverVerificationModal: React.FC<DriverVerificationModalProps> = ({
  isOpen,
  onClose,
  driver,
  onUpdated,
}) => {
  const [vehicleType, setVehicleType] = useState<'MOTO' | 'VOITURE' | 'TRICYCLE'>(driver.vehicleType || 'MOTO');
  const [vehiclePlate, setVehiclePlate] = useState(driver.vehiclePlate || 'CI-8492-AB01');
  const [cniSubmitted, setCniSubmitted] = useState(true);
  const [licenseSubmitted, setLicenseSubmitted] = useState(true);
  const [isSubmitting, setIsSubmitting] = useState(false);

  if (!isOpen) return null;

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    setIsSubmitting(true);

    setTimeout(() => {
      const updated: DriverProfile = {
        ...driver,
        vehicleType,
        vehiclePlate,
        verificationStatus: 'VERIFIED',
      };
      storageService.saveDriverProfile(updated);
      onUpdated(updated);
      setIsSubmitting(false);
      onClose();
    }, 600);
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 overflow-y-auto bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-lg bg-white rounded-3xl shadow-2xl overflow-hidden my-auto flex flex-col">
        {/* Header */}
        <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between">
          <div className="flex items-center gap-2.5">
            <ShieldCheck className="w-6 h-6 text-blue-400" />
            <h2 className="text-base font-extrabold text-white">Vérification du Livreur WÀNDÉ</h2>
          </div>
          <button onClick={onClose} className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white">
            <X className="w-5 h-5" />
          </button>
        </div>

        <form onSubmit={handleSubmit} className="p-6 space-y-5">
          <p className="text-xs text-slate-600">
            Pour garantir la sécurité de la communauté WÀNDÉ, chaque coursier doit enregistrer ses pièces d'identité et les détails de son moyen de transport.
          </p>

          {/* Véhicule */}
          <div className="space-y-3 bg-slate-50 border border-slate-200 p-4 rounded-2xl">
            <div className="text-xs font-bold text-slate-800">1. Moyen de transport</div>

            <div className="grid grid-cols-3 gap-2">
              {(['MOTO', 'VOITURE', 'TRICYCLE'] as const).map((type) => (
                <button
                  key={type}
                  type="button"
                  onClick={() => setVehicleType(type)}
                  className={`p-2.5 rounded-xl border text-xs font-bold transition-all ${
                    vehicleType === type
                      ? 'border-blue-600 bg-blue-50 text-blue-900 ring-2 ring-blue-500/20'
                      : 'border-slate-200 bg-white text-slate-700'
                  }`}
                >
                  {type === 'MOTO' ? '🛵 Moto' : type === 'VOITURE' ? '🚗 Voiture' : '🛺 Tricycle'}
                </button>
              ))}
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Numéro d'immatriculation / Plaque :
              </label>
              <input
                type="text"
                value={vehiclePlate}
                onChange={(e) => setVehiclePlate(e.target.value)}
                placeholder="Ex: CI-8492-AB01"
                className="w-full text-xs font-bold px-3.5 py-2.5 rounded-xl border border-slate-300 uppercase tracking-wider"
                required
              />
            </div>
          </div>

          {/* Documents */}
          <div className="space-y-3 bg-slate-50 border border-slate-200 p-4 rounded-2xl">
            <div className="text-xs font-bold text-slate-800">2. Pièces administratives</div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-white border border-slate-200 text-xs">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span className="font-semibold text-slate-800">Carte Nationale d'Identité (CNI)</span>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800">
                Téléversé
              </span>
            </div>

            <div className="flex items-center justify-between p-3 rounded-xl bg-white border border-slate-200 text-xs">
              <div className="flex items-center gap-2">
                <CheckCircle2 className="w-4 h-4 text-emerald-600" />
                <span className="font-semibold text-slate-800">Permis de conduire</span>
              </div>
              <span className="text-[10px] font-bold px-2 py-0.5 rounded bg-emerald-100 text-emerald-800">
                Téléversé
              </span>
            </div>
          </div>

          <button
            type="submit"
            disabled={isSubmitting}
            className="w-full py-3 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-xs sm:text-sm shadow-md shadow-blue-500/25 transition-all"
          >
            {isSubmitting ? 'Validation...' : 'Enregistrer mon profil livreur certifié'}
          </button>
        </form>
      </div>
    </div>
  );
};
