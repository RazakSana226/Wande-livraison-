import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { DeliveryRequest, DeliveryStatus, DriverProfile } from '../../types';
import { storageService } from '../../services/storageService';
import { StatusBadge } from '../../components/common/StatusBadge';
import { InteractiveMap } from '../../components/map/InteractiveMap';
import { CounterOfferModal } from '../../components/delivery/CounterOfferModal';
import { OtpModal } from '../../components/delivery/OtpModal';
import { formatCurrency } from '../../services/pricingService';
import {
  Bike,
  Wallet,
  Star,
  CheckCircle2,
  Tag,
  ShieldCheck,
  ShieldAlert,
  Power,
  Navigation,
  KeyRound,
  MapPin,
  Clock,
  Phone,
  MessageSquare,
  Sparkles,
  ChevronRight,
  TrendingUp,
  AlertCircle
} from 'lucide-react';

interface DriverHomeScreenProps {
  onNavigate: (screen: string) => void;
}

export const DriverHomeScreen: React.FC<DriverHomeScreenProps> = ({ onNavigate }) => {
  const { currentUser } = useAuth();
  const [driverProfile, setDriverProfile] = useState<DriverProfile>(storageService.getDriverProfile());
  const [deliveries, setDeliveries] = useState<DeliveryRequest[]>([]);
  const [selectedForCounter, setSelectedForCounter] = useState<DeliveryRequest | null>(null);
  const [selectedForOtp, setSelectedForOtp] = useState<DeliveryRequest | null>(null);

  useEffect(() => {
    const unsub = storageService.subscribe((list) => {
      setDeliveries(list);
      setDriverProfile(storageService.getDriverProfile());
    });
    return () => unsub();
  }, []);

  const handleToggleOnline = () => {
    const updated = { ...driverProfile, isOnline: !driverProfile.isOnline };
    storageService.saveDriverProfile(updated);
    setDriverProfile(updated);
  };

  // Open delivery requests (status searching or this driver counter-offered)
  const openDeliveries = deliveries.filter(
    (d) =>
      d.status === 'SEARCHING_DRIVER' ||
      (d.status === 'DRIVER_COUNTER_OFFERED' && d.counterOfferDriverId === driverProfile.id) ||
      (d.status === 'COUNTER_OFFER_REJECTED' && d.counterOfferDriverId === driverProfile.id)
  );

  // Active delivery assigned to this driver and not yet completed
  const activeDelivery = deliveries.find(
    (d) => d.driverId === driverProfile.id && d.status !== 'DELIVERED' && d.status !== 'CANCELLED'
  );

  const handleAcceptCourse = async (deliveryId: string) => {
    try {
      await storageService.driverAcceptDelivery(deliveryId, driverProfile);
    } catch (e: any) {
      alert(e.message || 'Erreur lors de l’acceptation');
    }
  };

  const handleSendCounterOffer = async (amount: number) => {
    if (!selectedForCounter) return;
    try {
      await storageService.driverSubmitCounterOffer(selectedForCounter.id, driverProfile, amount);
      setSelectedForCounter(null);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const handleAdvanceStatus = async (deliveryId: string, next: DeliveryStatus) => {
    try {
      await storageService.driverUpdateStatus(deliveryId, next);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const handleConfirmOtp = async (otp: string) => {
    if (!selectedForOtp) return;
    await storageService.driverCompleteWithOtp(selectedForOtp.id, otp);
    setSelectedForOtp(null);
  };

  return (
    <div className="max-w-4xl mx-auto px-4 py-4 space-y-5 pb-24">
      {/* Top Banner: Driver Stats & Online Toggle */}
      <div className="bg-slate-900 text-white rounded-3xl p-5 sm:p-6 shadow-xl space-y-4">
        <div className="flex items-center justify-between">
          <div className="flex items-center gap-3">
            <img
              src={driverProfile.avatarUrl}
              alt={driverProfile.name}
              className="w-12 h-12 rounded-2xl object-cover ring-2 ring-blue-500"
            />
            <div>
              <div className="flex items-center gap-1.5">
                <h1 className="text-base sm:text-lg font-black">{driverProfile.name}</h1>
                <ShieldCheck className="w-4 h-4 text-emerald-400" />
              </div>
              <p className="text-xs text-slate-400 font-medium">
                {driverProfile.vehicleType} • {driverProfile.vehiclePlate}
              </p>
            </div>
          </div>

          {/* Availability Toggle */}
          <button
            onClick={handleToggleOnline}
            className={`flex items-center gap-2 px-3.5 py-2 rounded-2xl font-black text-xs transition-all shadow-md ${
              driverProfile.isOnline
                ? 'bg-emerald-500 text-white shadow-emerald-500/20'
                : 'bg-slate-700 text-slate-300'
            }`}
          >
            <Power className="w-3.5 h-3.5" />
            <span>{driverProfile.isOnline ? 'En Ligne' : 'Hors Ligne'}</span>
          </button>
        </div>

        {/* Stats Grid */}
        <div className="grid grid-cols-3 gap-2 sm:gap-4 pt-2 border-t border-slate-800 text-center">
          <div className="bg-slate-800/70 p-3 rounded-2xl">
            <span className="text-[10px] sm:text-xs font-semibold text-slate-400 uppercase">
              Solde WÀNDÉ
            </span>
            <p className="text-sm sm:text-lg font-black text-emerald-400 mt-0.5">
              {formatCurrency(driverProfile.walletBalanceXof)}
            </p>
          </div>

          <div className="bg-slate-800/70 p-3 rounded-2xl">
            <span className="text-[10px] sm:text-xs font-semibold text-slate-400 uppercase">
              Courses Réussies
            </span>
            <p className="text-sm sm:text-lg font-black text-white mt-0.5">
              {driverProfile.totalDeliveries}
            </p>
          </div>

          <div className="bg-slate-800/70 p-3 rounded-2xl">
            <span className="text-[10px] sm:text-xs font-semibold text-slate-400 uppercase">
              Note moyenne
            </span>
            <p className="text-sm sm:text-lg font-black text-amber-400 mt-0.5 flex items-center justify-center gap-1">
              <Star className="w-3.5 h-3.5 fill-amber-400" />
              <span>{driverProfile.rating}</span>
            </p>
          </div>
        </div>
      </div>

      {/* KYC Verification Banner (if pending or verified) */}
      <div
        onClick={() => onNavigate('driver_verification')}
        className="bg-blue-50 border border-blue-200 rounded-2xl p-3.5 flex items-center justify-between cursor-pointer hover:bg-blue-100/70 transition-all text-xs"
      >
        <div className="flex items-center gap-2.5">
          <div className="w-8 h-8 rounded-xl bg-blue-600 text-white flex items-center justify-center font-bold">
            <ShieldCheck className="w-4 h-4" />
          </div>
          <div>
            <p className="font-bold text-blue-900">Compte Livreur Vérifié & Actif</p>
            <p className="text-blue-700 text-[11px]">Permis & Pièce d'identité validés par l'équipe</p>
          </div>
        </div>
        <ChevronRight className="w-4 h-4 text-blue-600" />
      </div>

      {/* ACTIVE COURSE IN PROGRESS (If driver currently has an assigned course) */}
      {activeDelivery && (
        <div className="bg-white rounded-3xl border-2 border-blue-600 p-5 shadow-xl space-y-4 animate-slide-up">
          <div className="flex items-center justify-between">
            <span className="inline-flex items-center gap-1.5 bg-blue-600 text-white text-xs font-black px-3 py-1 rounded-full uppercase tracking-wider">
              <Bike className="w-3.5 h-3.5" />
              Course en cours #{activeDelivery.id}
            </span>
            <span className="text-base font-black text-emerald-600">
              Gain net : +{formatCurrency(activeDelivery.driverEarningsXof)}
            </span>
          </div>

          {/* Interactive Map */}
          <InteractiveMap
            pickupLat={activeDelivery.pickupLat}
            pickupLng={activeDelivery.pickupLng}
            destinationLat={activeDelivery.destinationLat}
            destinationLng={activeDelivery.destinationLng}
            driverLat={activeDelivery.currentDriverLat}
            driverLng={activeDelivery.currentDriverLng}
            pickupAddress={activeDelivery.pickupAddress}
            destinationAddress={activeDelivery.destinationAddress}
            className="h-56"
          />

          {/* Client & Recipient Contact */}
          <div className="grid grid-cols-2 gap-3 bg-slate-50 p-3 rounded-2xl border border-slate-200 text-xs">
            <div>
              <span className="text-slate-400 font-semibold block text-[10px] uppercase">Expéditeur</span>
              <p className="font-bold text-slate-800">{activeDelivery.clientName}</p>
              <a
                href={`tel:${activeDelivery.clientPhone}`}
                className="text-blue-600 font-bold flex items-center gap-1 mt-0.5"
              >
                <Phone className="w-3 h-3" />
                <span>Appeler</span>
              </a>
            </div>

            <div>
              <span className="text-slate-400 font-semibold block text-[10px] uppercase">Destinataire</span>
              <p className="font-bold text-slate-800">{activeDelivery.recipientName}</p>
              <a
                href={`tel:${activeDelivery.recipientPhone}`}
                className="text-emerald-600 font-bold flex items-center gap-1 mt-0.5"
              >
                <Phone className="w-3 h-3" />
                <span>Appeler</span>
              </a>
            </div>
          </div>

          {/* Step Actions for Driver */}
          <div className="space-y-2 pt-1">
            {activeDelivery.status === 'DRIVER_ASSIGNED' && (
              <button
                onClick={() => handleAdvanceStatus(activeDelivery.id, 'DRIVER_ARRIVING')}
                className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3 rounded-xl shadow-md flex items-center justify-center gap-2 text-sm"
              >
                <Navigation className="w-4 h-4" />
                <span>Je suis en route vers l'expéditeur</span>
              </button>
            )}

            {activeDelivery.status === 'DRIVER_ARRIVING' && (
              <button
                onClick={() => handleAdvanceStatus(activeDelivery.id, 'PACKAGE_PICKED_UP')}
                className="w-full bg-indigo-600 hover:bg-indigo-700 text-white font-black py-3 rounded-xl shadow-md flex items-center justify-center gap-2 text-sm"
              >
                <CheckCircle2 className="w-4 h-4" />
                <span>Colis récupéré • En route vers le destinataire</span>
              </button>
            )}

            {(activeDelivery.status === 'PACKAGE_PICKED_UP' || activeDelivery.status === 'IN_TRANSIT') && (
              <button
                onClick={() => handleAdvanceStatus(activeDelivery.id, 'DRIVER_ARRIVED')}
                className="w-full bg-amber-600 hover:bg-amber-700 text-white font-black py-3 rounded-xl shadow-md flex items-center justify-center gap-2 text-sm"
              >
                <MapPin className="w-4 h-4" />
                <span>Je suis arrivé chez le destinataire</span>
              </button>
            )}

            {activeDelivery.status === 'DRIVER_ARRIVED' && (
              <button
                onClick={() => setSelectedForOtp(activeDelivery)}
                className="w-full bg-emerald-600 hover:bg-emerald-700 text-white font-black py-3.5 rounded-xl shadow-lg shadow-emerald-600/30 flex items-center justify-center gap-2 text-sm sm:text-base animate-pulse"
              >
                <KeyRound className="w-5 h-5" />
                <span>Valider la livraison avec le code OTP</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* OPEN INCOMING REQUESTS (FEED) */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-base sm:text-lg font-black text-slate-900 flex items-center gap-2">
            <span>Demandes de livraison disponibles</span>
            <span className="bg-blue-600 text-white text-xs font-black px-2 py-0.5 rounded-full">
              {openDeliveries.length}
            </span>
          </h2>
          <span className="text-xs text-slate-500 font-semibold">Abidjan & Environs</span>
        </div>

        {openDeliveries.length === 0 ? (
          <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
              <Clock className="w-6 h-6" />
            </div>
            <h3 className="font-extrabold text-slate-800 text-sm">
              En attente de nouvelles courses
            </h3>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Gardez votre statut en ligne. Les nouvelles demandes s'afficheront ici instantanément.
            </p>
          </div>
        ) : (
          <div className="space-y-3">
            {openDeliveries.map((delivery) => {
              const isThisDriverCounterOffering =
                delivery.counterOfferDriverId === driverProfile.id;
              const hasCounterOfferPending =
                delivery.status === 'DRIVER_COUNTER_OFFERED' && isThisDriverCounterOffering;
              const isCounterOfferRejected =
                delivery.status === 'COUNTER_OFFER_REJECTED' && isThisDriverCounterOffering;

              const clientOffer = delivery.customerInitialOffer;
              const driverNetAtClientOffer = Math.round(clientOffer * 0.90);

              return (
                <div
                  key={delivery.id}
                  className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-card space-y-3.5 hover:border-blue-300 transition-all"
                >
                  {/* Header */}
                  <div className="flex items-center justify-between">
                    <span className="text-[11px] font-black uppercase text-blue-600 bg-blue-50 px-2 py-0.5 rounded-md">
                      📦 {delivery.packageSize} • #{delivery.id}
                    </span>
                    <div className="text-right">
                      <span className="text-xs text-slate-400 block font-semibold">Offre client</span>
                      <span className="text-sm sm:text-base font-black text-slate-900">
                        {formatCurrency(clientOffer)}
                      </span>
                    </div>
                  </div>

                  {/* Parcel description */}
                  <div>
                    <h4 className="font-extrabold text-sm text-slate-900">
                      {delivery.packageDescription}
                    </h4>
                    {delivery.specialNotes && (
                      <p className="text-xs text-slate-500 italic mt-0.5">
                        "{delivery.specialNotes}"
                      </p>
                    )}
                  </div>

                  {/* Itinerary */}
                  <div className="space-y-2 text-xs bg-slate-50 p-3 rounded-xl border border-slate-200">
                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-blue-100 text-blue-700 font-black text-[10px] flex items-center justify-center mt-0.5">
                        A
                      </span>
                      <p className="text-slate-800 font-medium truncate">{delivery.pickupAddress}</p>
                    </div>
                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-700 font-black text-[10px] flex items-center justify-center mt-0.5">
                        B
                      </span>
                      <p className="text-slate-800 font-medium truncate">{delivery.destinationAddress}</p>
                    </div>
                  </div>

                  {/* Driver Earnings Preview */}
                  <div className="flex justify-between items-center text-xs font-bold px-1">
                    <span className="text-slate-500">Votre gain net (90%) :</span>
                    <span className="text-emerald-600 font-black text-sm">
                      +{formatCurrency(driverNetAtClientOffer)}
                    </span>
                  </div>

                  {/* STRICT 2-OPTION ACTION BUTTONS OR STATUS */}
                  {hasCounterOfferPending ? (
                    <div className="bg-amber-50 border border-amber-200 rounded-xl p-3 text-xs text-amber-900 space-y-1">
                      <div className="flex items-center gap-1.5 font-bold">
                        <Tag className="w-4 h-4 text-amber-700" />
                        <span>Contre-offre de {formatCurrency(delivery.driverCounterOffer || 0)} envoyée</span>
                      </div>
                      <p className="text-amber-800 text-[11px]">
                        En attente de la décision du client...
                      </p>
                    </div>
                  ) : isCounterOfferRejected ? (
                    <div className="space-y-2">
                      <div className="bg-red-50 border border-red-200 rounded-xl p-2.5 text-xs text-red-800 flex items-center gap-1.5">
                        <AlertCircle className="w-4 h-4 text-red-600 flex-shrink-0" />
                        <span>Contre-offre déclinée par le client. Vous pouvez toujours accepter à {formatCurrency(clientOffer)}.</span>
                      </div>
                      <button
                        onClick={() => handleAcceptCourse(delivery.id)}
                        className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-2.5 rounded-xl text-xs shadow-md transition-all"
                      >
                        Accepter au prix client ({formatCurrency(clientOffer)})
                      </button>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-3 pt-1">
                      <button
                        onClick={() => setSelectedForCounter(delivery)}
                        className="w-full py-2.5 px-2 rounded-xl border-2 border-blue-600 text-blue-600 hover:bg-blue-50 font-black text-xs transition-all active:scale-95 flex items-center justify-center gap-1"
                      >
                        <Tag className="w-3.5 h-3.5" />
                        <span>Proposer un prix</span>
                      </button>

                      <button
                        onClick={() => handleAcceptCourse(delivery.id)}
                        className="w-full py-2.5 px-2 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-xs shadow-md shadow-blue-500/25 transition-all active:scale-95 flex items-center justify-center gap-1"
                      >
                        <CheckCircle2 className="w-3.5 h-3.5" />
                        <span>Accepter ({formatCurrency(clientOffer)})</span>
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Driver Counter Offer Modal */}
      {selectedForCounter && (
        <CounterOfferModal
          delivery={selectedForCounter}
          isOpen={!!selectedForCounter}
          onClose={() => setSelectedForCounter(null)}
          onSubmit={handleSendCounterOffer}
        />
      )}

      {/* OTP Delivery Validation Modal */}
      {selectedForOtp && (
        <OtpModal
          isOpen={!!selectedForOtp}
          onClose={() => setSelectedForOtp(null)}
          onConfirm={handleConfirmOtp}
          recipientName={selectedForOtp.recipientName}
          expectedOtpForDemo={selectedForOtp.otpCode}
        />
      )}
    </div>
  );
};
