import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { storageService } from '../../services/storageService';
import {
  ArrowLeft,
  ShieldCheck,
  Upload,
  CheckCircle2,
  FileText,
  AlertTriangle,
  Camera,
  Bike,
  Sparkles,
  Phone
} from 'lucide-react';

interface DriverVerificationScreenProps {
  onNavigate: (screen: string) => void;
}

export const DriverVerificationScreen: React.FC<DriverVerificationScreenProps> = ({ onNavigate }) => {
  const driverProfile = storageService.getDriverProfile();
  const [vehicleType, setVehicleType] = useState<'MOTO' | 'VOITURE' | 'TRICYCLE'>(driverProfile.vehicleType || 'MOTO');
  const [vehiclePlate, setVehiclePlate] = useState(driverProfile.vehiclePlate || 'CI-8492-AB01');
  const [mobileMoneyNumber, setMobileMoneyNumber] = useState(driverProfile.phone || '+225 07 99 88 77 66');

  const [cniUploaded, setCniUploaded] = useState(true);
  const [licenseUploaded, setLicenseUploaded] = useState(true);
  const [vehiclePhotoUploaded, setVehiclePhotoUploaded] = useState(true);
  const [isSaved, setIsSaved] = useState(false);

  const handleSave = () => {
    const updated = {
      ...driverProfile,
      vehicleType,
      vehiclePlate,
      phone: mobileMoneyNumber,
      verificationStatus: 'VERIFIED' as const,
    };
    storageService.saveDriverProfile(updated);
    setIsSaved(true);
    setTimeout(() => {
      onNavigate('driver_home');
    }, 1500);
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-4 space-y-5 pb-24">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => onNavigate('driver_home')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-slate-900 bg-white px-3 py-2 rounded-xl border border-slate-200 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Espace Livreur</span>
        </button>

        <span className="text-xs font-black text-emerald-700 bg-emerald-50 px-3 py-1 rounded-full border border-emerald-200 flex items-center gap-1">
          <ShieldCheck className="w-3.5 h-3.5" />
          <span>Statut : Vérifié</span>
        </span>
      </div>

      {/* Hero */}
      <div>
        <h1 className="text-xl sm:text-2xl font-black text-slate-900">
          Vérification KYC & Véhicule Livreur
        </h1>
        <p className="text-xs text-slate-500 mt-1">
          Assurez la conformité de votre compte pour débloquer les paiements instantanés.
        </p>
      </div>

      {/* Main KYC Form */}
      <div className="bg-white rounded-2xl border border-slate-200 p-5 shadow-card space-y-5">
        {/* Vehicle Selection */}
        <div className="space-y-2">
          <label className="text-xs font-bold text-slate-700 block">Type de véhicule de livraison :</label>
          <div className="grid grid-cols-3 gap-2">
            {[
              { id: 'MOTO', label: 'Moto 2 Roues', icon: '🛵' },
              { id: 'TRICYCLE', label: 'Tricycle / Cargo', icon: '🛺' },
              { id: 'VOITURE', label: 'Voiture / Van', icon: '🚗' },
            ].map((v) => (
              <button
                key={v.id}
                type="button"
                onClick={() => setVehicleType(v.id as any)}
                className={`p-3 rounded-xl border-2 text-center transition-all flex flex-col items-center justify-center ${
                  vehicleType === v.id
                    ? 'border-blue-600 bg-blue-50/60 font-black text-blue-700'
                    : 'border-slate-200 bg-slate-50 text-slate-700 font-semibold'
                }`}
              >
                <span className="text-2xl mb-1">{v.icon}</span>
                <span className="text-xs font-bold">{v.label}</span>
              </button>
            ))}
          </div>
        </div>

        {/* Vehicle Plate & MoMo */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700 block">Immatriculation / Plaque :</label>
            <input
              type="text"
              value={vehiclePlate}
              onChange={(e) => setVehiclePlate(e.target.value)}
              placeholder="Ex: CI-8492-AB01"
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>

          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700 block">Numéro Mobile Money (Retraits) :</label>
            <input
              type="tel"
              value={mobileMoneyNumber}
              onChange={(e) => setMobileMoneyNumber(e.target.value)}
              placeholder="+225 07..."
              className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
            />
          </div>
        </div>

        {/* Document Upload Simulation */}
        <div className="space-y-3 pt-2 border-t border-slate-100">
          <label className="text-xs font-extrabold text-slate-900 block">Documents obligatoires :</label>

          {/* CNI */}
          <div className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-slate-50">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-blue-100 text-blue-700 flex items-center justify-center font-bold">
                <FileText className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-bold text-slate-800">Pièce d'identité (CNI ou Passeport)</p>
                <p className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Validé & Conforme
                </p>
              </div>
            </div>
            <button
              type="button"
              className="text-xs font-bold text-blue-600 hover:underline"
            >
              Remplacer
            </button>
          </div>

          {/* Permis */}
          <div className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-slate-50">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold">
                <ShieldCheck className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-bold text-slate-800">Permis de conduire (Catégorie A/B)</p>
                <p className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Validé & Conforme
                </p>
              </div>
            </div>
            <button
              type="button"
              className="text-xs font-bold text-blue-600 hover:underline"
            >
              Remplacer
            </button>
          </div>

          {/* Photo Véhicule */}
          <div className="flex items-center justify-between p-3 rounded-xl border border-slate-200 bg-slate-50">
            <div className="flex items-center gap-2.5">
              <div className="w-8 h-8 rounded-lg bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
                <Camera className="w-4 h-4" />
              </div>
              <div>
                <p className="text-xs font-bold text-slate-800">Photo du véhicule & Plaque visible</p>
                <p className="text-[11px] text-emerald-600 font-semibold flex items-center gap-1">
                  <CheckCircle2 className="w-3 h-3" /> Photo nette validée
                </p>
              </div>
            </div>
            <button
              type="button"
              className="text-xs font-bold text-blue-600 hover:underline"
            >
              Remplacer
            </button>
          </div>
        </div>

        {/* Submit */}
        <button
          onClick={handleSave}
          className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3.5 rounded-2xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 text-sm sm:text-base transition-all"
        >
          {isSaved ? (
            <span className="flex items-center gap-2 text-emerald-300">
              <CheckCircle2 className="w-5 h-5" /> Informations enregistrées avec succès !
            </span>
          ) : (
            <span>Enregistrer et mettre à jour mon profil</span>
          )}
        </button>
      </div>
    </div>
  );
};
