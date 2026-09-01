import React, { useEffect, useState } from 'react';
import { DeliveryRequest } from '../../types';
import { storageService } from '../../services/storageService';
import { InteractiveMap } from '../../components/map/InteractiveMap';
import { StatusBadge } from '../../components/common/StatusBadge';
import { CounterOfferCard } from '../../components/delivery/CounterOfferCard';
import { formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import {
  ArrowLeft,
  KeyRound,
  Copy,
  Share2,
  Phone,
  MessageSquare,
  Bike,
  ShieldCheck,
  CheckCircle2,
  TrendingUp,
  Clock,
  Sparkles,
  MapPin,
  Check,
  AlertCircle
} from 'lucide-react';

interface TrackingScreenProps {
  deliveryId: string;
  onNavigate: (screen: string) => void;
}

export const TrackingScreen: React.FC<TrackingScreenProps> = ({ deliveryId, onNavigate }) => {
  const [delivery, setDelivery] = useState<DeliveryRequest | undefined>(undefined);
  const [copiedOtp, setCopiedOtp] = useState(false);
  const [showBooster, setShowBooster] = useState(false);
  const [boostAmount, setBoostAmount] = useState('2000');
  const [isProcessing, setIsProcessing] = useState(false);

  useEffect(() => {
    const unsub = storageService.subscribe((list) => {
      const found = list.find((d) => d.id === deliveryId);
      setDelivery(found);
    });
    return () => unsub();
  }, [deliveryId]);

  if (!delivery) {
    return (
      <div className="max-w-md mx-auto p-6 text-center space-y-4">
        <AlertCircle className="w-12 h-12 text-slate-400 mx-auto" />
        <h2 className="text-xl font-bold text-slate-900">Livraison introuvable</h2>
        <p className="text-xs text-slate-500">
          La livraison demandée n'existe pas ou a été archivée.
        </p>
        <button
          onClick={() => onNavigate('client_home')}
          className="bg-blue-600 text-white font-bold px-4 py-2 rounded-xl text-sm"
        >
          Retour à l'accueil
        </button>
      </div>
    );
  }

  const handleCopyOtp = () => {
    navigator.clipboard.writeText(delivery.otpCode);
    setCopiedOtp(true);
    setTimeout(() => setCopiedOtp(false), 2000);
  };

  const handleShareWhatsApp = () => {
    const msg = `Bonjour ${delivery.recipientName}, votre colis WÀNDÉ arrive bientôt ! Voici votre code secret de livraison : *${delivery.otpCode}*. Donnez-le au livreur pour valider la réception.`;
    window.open(`https://wa.me/?text=${encodeURIComponent(msg)}`, '_blank');
  };

  const handleAcceptCounterOffer = async () => {
    try {
      setIsProcessing(true);
      await storageService.clientAcceptCounterOffer(delivery.id);
    } catch (e: any) {
      alert(e.message || 'Erreur lors de la validation de la contre-offre');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleRejectCounterOffer = async () => {
    try {
      setIsProcessing(true);
      await storageService.clientRejectCounterOffer(delivery.id);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleApplyBoost = async () => {
    const amount = parseInt(boostAmount, 10) || 0;
    if (amount < MIN_PRICE_XOF) {
      alert(`Le montant minimum est de ${formatCurrency(MIN_PRICE_XOF)}`);
      return;
    }
    try {
      await storageService.clientUpdateOffer(delivery.id, amount);
      setShowBooster(false);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const statusSteps = [
    { key: 'SEARCHING_DRIVER', label: 'Recherche' },
    { key: 'DRIVER_ASSIGNED', label: 'Assigné' },
    { key: 'DRIVER_ARRIVING', label: 'En route' },
    { key: 'PACKAGE_PICKED_UP', label: 'Récupéré' },
    { key: 'IN_TRANSIT', label: 'En livraison' },
    { key: 'DELIVERED', label: 'Livré' },
  ];

  const getStepIndex = (status: string) => {
    switch (status) {
      case 'SEARCHING_DRIVER':
      case 'DRIVER_COUNTER_OFFERED':
      case 'COUNTER_OFFER_REJECTED':
        return 0;
      case 'DRIVER_ASSIGNED':
      case 'COUNTER_OFFER_ACCEPTED':
        return 1;
      case 'DRIVER_ARRIVING':
        return 2;
      case 'PACKAGE_PICKED_UP':
        return 3;
      case 'IN_TRANSIT':
      case 'DRIVER_ARRIVED':
        return 4;
      case 'DELIVERED':
        return 5;
      default:
        return 0;
    }
  };

  const currentStepIdx = getStepIndex(delivery.status);

  return (
    <div className="max-w-2xl mx-auto px-4 py-4 space-y-4 pb-24">
      {/* Top Header */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => onNavigate('client_home')}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-slate-900 bg-white px-3 py-2 rounded-xl border border-slate-200 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>Accueil</span>
        </button>

        <div className="text-right">
          <span className="text-xs font-black text-slate-400">Course #{delivery.id}</span>
          <StatusBadge status={delivery.status} size="sm" className="ml-2" />
        </div>
      </div>

      {/* Live Map */}
      <InteractiveMap
        pickupLat={delivery.pickupLat}
        pickupLng={delivery.pickupLng}
        destinationLat={delivery.destinationLat}
        destinationLng={delivery.destinationLng}
        driverLat={delivery.currentDriverLat}
        driverLng={delivery.currentDriverLng}
        pickupAddress={delivery.pickupAddress}
        destinationAddress={delivery.destinationAddress}
        driverName={delivery.driverName}
        className="h-64 sm:h-80"
      />

      {/* Stepper Timeline */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card">
        <div className="flex items-center justify-between relative">
          <div className="absolute top-1/2 left-0 right-0 h-1 bg-slate-100 -translate-y-1/2 z-0"></div>
          <div
            className="absolute top-1/2 left-0 h-1 bg-blue-600 -translate-y-1/2 z-0 transition-all duration-500"
            style={{ width: `${(currentStepIdx / (statusSteps.length - 1)) * 100}%` }}
          ></div>

          {statusSteps.map((s, idx) => {
            const isCompleted = idx <= currentStepIdx;
            const isCurrent = idx === currentStepIdx;
            return (
              <div key={s.key} className="relative z-10 flex flex-col items-center">
                <div
                  className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-black transition-all ${
                    isCompleted
                      ? 'bg-blue-600 text-white ring-4 ring-blue-100'
                      : 'bg-white border-2 border-slate-300 text-slate-400'
                  }`}
                >
                  {isCompleted && idx < currentStepIdx ? (
                    <Check className="w-3.5 h-3.5 stroke-[3]" />
                  ) : (
                    idx + 1
                  )}
                </div>
                <span
                  className={`text-[10px] mt-1 font-bold ${
                    isCurrent ? 'text-blue-600' : 'text-slate-400'
                  }`}
                >
                  {s.label}
                </span>
              </div>
            );
          })}
        </div>
      </div>

      {/* DRIVER COUNTER-OFFER DECISION CARD (If active counter-offer) */}
      {delivery.status === 'DRIVER_COUNTER_OFFERED' && delivery.driverCounterOffer && (
        <CounterOfferCard
          delivery={delivery}
          onAccept={handleAcceptCounterOffer}
          onReject={handleRejectCounterOffer}
          isLoading={isProcessing}
        />
      )}

      {/* CUSTOMER OFFER STATUS & BOOSTER (When searching for a driver) */}
      {delivery.status === 'SEARCHING_DRIVER' && (
        <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card space-y-3">
          <div className="flex items-center justify-between">
            <div>
              <span className="text-[11px] text-slate-500 font-semibold">Votre offre actuelle :</span>
              <p className="text-lg font-black text-blue-600">
                {formatCurrency(delivery.customerInitialOffer)}
              </p>
            </div>
            <div className="text-right">
              <span className="text-[11px] text-slate-500 font-semibold">Total payé (avec 10%) :</span>
              <p className="text-sm font-bold text-slate-800">
                {formatCurrency(delivery.customerTotalPaidXof)}
              </p>
            </div>
          </div>

          {!showBooster ? (
            <button
              onClick={() => setShowBooster(true)}
              className="w-full py-2.5 px-3 rounded-xl border border-blue-200 bg-blue-50/70 hover:bg-blue-100 text-blue-700 text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
            >
              <TrendingUp className="w-4 h-4" />
              <span>Augmenter l'offre pour trouver plus vite</span>
            </button>
          ) : (
            <div className="space-y-2 pt-2 border-t border-slate-100">
              <label className="text-xs font-bold text-slate-700 block">
                Ajuster votre proposition :
              </label>
              <div className="flex gap-2">
                {[1500, 2000, 2500].map((p) => (
                  <button
                    key={p}
                    type="button"
                    onClick={() => setBoostAmount(p.toString())}
                    className={`flex-1 py-2 rounded-xl text-xs font-black border transition-all ${
                      boostAmount === p.toString()
                        ? 'border-blue-600 bg-blue-50 text-blue-700'
                        : 'border-slate-200 bg-slate-50 text-slate-700'
                    }`}
                  >
                    {formatCurrency(p)}
                  </button>
                ))}
              </div>
              <div className="flex gap-2">
                <input
                  type="text"
                  value={boostAmount}
                  onChange={(e) => setBoostAmount(e.target.value.replace(/\D/g, ''))}
                  placeholder="Montant FCFA"
                  className="flex-1 bg-slate-50 border border-slate-300 rounded-xl px-3 py-2 text-xs font-bold text-slate-900"
                />
                <button
                  onClick={handleApplyBoost}
                  className="bg-blue-600 text-white px-4 py-2 rounded-xl text-xs font-black shadow-sm"
                >
                  Valider
                </button>
              </div>
            </div>
          )}
        </div>
      )}

      {/* OTP SECRET CODE CARD (Prominent Security Card) */}
      <div className="bg-gradient-to-r from-blue-600 to-indigo-700 text-white rounded-2xl p-4 sm:p-5 shadow-xl shadow-blue-600/20 space-y-3">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-white/20 flex items-center justify-center">
              <KeyRound className="w-4 h-4 text-amber-300" />
            </div>
            <div>
              <h3 className="font-extrabold text-sm sm:text-base">Code Secret OTP</h3>
              <p className="text-[11px] text-blue-100">À donner au livreur à l'arrivée</p>
            </div>
          </div>

          <div className="bg-white text-slate-900 font-black text-2xl tracking-widest px-4 py-1.5 rounded-xl shadow-md border-2 border-amber-400">
            {delivery.otpCode}
          </div>
        </div>

        {/* Share Buttons */}
        <div className="flex gap-2 pt-1">
          <button
            onClick={handleCopyOtp}
            className="flex-1 bg-white/15 hover:bg-white/25 border border-white/20 py-2 px-3 rounded-xl text-xs font-bold flex items-center justify-center gap-1.5 transition-all"
          >
            {copiedOtp ? <Check className="w-4 h-4 text-emerald-300" /> : <Copy className="w-4 h-4" />}
            <span>{copiedOtp ? 'Copié !' : 'Copier le code'}</span>
          </button>

          <button
            onClick={handleShareWhatsApp}
            className="flex-1 bg-emerald-500 hover:bg-emerald-600 py-2 px-3 rounded-xl text-xs font-black flex items-center justify-center gap-1.5 transition-all shadow-md"
          >
            <Share2 className="w-4 h-4" />
            <span>Envoyer WhatsApp</span>
          </button>
        </div>
      </div>

      {/* ASSIGNED DRIVER CARD (When assigned) */}
      {delivery.driverId && (
        <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card space-y-3">
          <div className="flex items-center justify-between">
            <div className="flex items-center gap-3">
              <img
                src={delivery.driverPhoto || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80'}
                alt={delivery.driverName}
                className="w-12 h-12 rounded-2xl object-cover ring-2 ring-blue-500/20"
              />
              <div>
                <div className="flex items-center gap-1.5">
                  <h4 className="font-extrabold text-sm text-slate-900">{delivery.driverName}</h4>
                  <ShieldCheck className="w-4 h-4 text-blue-600" />
                </div>
                <p className="text-[11px] text-slate-500">{delivery.driverVehicle || 'Yamaha YBR 125'}</p>
                <div className="flex items-center gap-1 text-[11px] font-bold text-amber-600 mt-0.5">
                  <span>★ {delivery.driverRating || 4.9}</span>
                  <span className="text-slate-400">• Livreur Certifié WÀNDÉ</span>
                </div>
              </div>
            </div>

            {/* Direct Contact Buttons */}
            <div className="flex items-center gap-2">
              <a
                href={`tel:${delivery.driverPhone || '+2250799887766'}`}
                className="w-10 h-10 rounded-xl bg-blue-50 text-blue-600 hover:bg-blue-600 hover:text-white flex items-center justify-center transition-colors border border-blue-100"
              >
                <Phone className="w-4 h-4" />
              </a>
              <a
                href={`https://wa.me/${(delivery.driverPhone || '+2250799887766').replace(/\D/g, '')}`}
                target="_blank"
                rel="noreferrer"
                className="w-10 h-10 rounded-xl bg-emerald-50 text-emerald-600 hover:bg-emerald-600 hover:text-white flex items-center justify-center transition-colors border border-emerald-100"
              >
                <MessageSquare className="w-4 h-4" />
              </a>
            </div>
          </div>
        </div>
      )}

      {/* Itinerary Details */}
      <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card space-y-3 text-xs">
        <h4 className="font-bold text-slate-900 uppercase text-[11px] tracking-wider text-slate-400">
          Détails de l'expédition
        </h4>

        <div className="space-y-2">
          <div className="flex items-start gap-2">
            <span className="w-4 h-4 rounded-full bg-blue-100 text-blue-700 font-black text-[10px] flex items-center justify-center mt-0.5">
              A
            </span>
            <div>
              <span className="text-slate-400 text-[10px] font-bold uppercase">Départ</span>
              <p className="text-slate-800 font-semibold">{delivery.pickupAddress}</p>
            </div>
          </div>

          <div className="flex items-start gap-2">
            <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-700 font-black text-[10px] flex items-center justify-center mt-0.5">
              B
            </span>
            <div>
              <span className="text-slate-400 text-[10px] font-bold uppercase">Arrivée</span>
              <p className="text-slate-800 font-semibold">{delivery.destinationAddress}</p>
            </div>
          </div>
        </div>

        <div className="pt-2 border-t border-slate-100 flex justify-between text-slate-600">
          <span>Colis : <strong>{delivery.packageDescription}</strong></span>
          <span>Format : <strong>{delivery.packageSize}</strong></span>
        </div>
      </div>
    </div>
  );
};
