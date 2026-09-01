import React, { useState } from 'react';
import { DeliveryRequest, DeliveryStatus, DriverProfile } from '../../types';
import { formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import { storageService } from '../../services/storageService';
import { StatusBadge } from '../common/StatusBadge';
import { DeliveryMap } from '../map/DeliveryMap';
import { DriverVerificationModal } from './DriverVerificationModal';
import { 
  Bike, 
  Wallet, 
  CheckCircle2, 
  MapPin, 
  TrendingUp, 
  Phone, 
  ShieldCheck, 
  Navigation, 
  AlertTriangle, 
  X, 
  Check, 
  ArrowRight,
  Power,
  Clock,
  Sparkles
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface DriverDashboardProps {
  deliveries: DeliveryRequest[];
}

export const DriverDashboard: React.FC<DriverDashboardProps> = ({ deliveries }) => {
  const [driver, setDriver] = useState<DriverProfile>(storageService.getDriverProfile());
  const [showVerificationModal, setShowVerificationModal] = useState(false);

  // Counter-offer modal state
  const [counterOfferDelivery, setCounterOfferDelivery] = useState<DeliveryRequest | null>(null);
  const [counterPriceInput, setCounterPriceInput] = useState<string>('2000');
  const [counterError, setCounterError] = useState<string | null>(null);

  // OTP Validation modal / state
  const [otpDeliveryId, setOtpDeliveryId] = useState<string | null>(null);
  const [enteredOtp, setEnteredOtp] = useState<string>('');
  const [otpError, setOtpError] = useState<string | null>(null);

  // Filter deliveries
  // 1. My active ongoing mission
  const activeMission = deliveries.find(
    (d) => d.driverId === driver.id && d.status !== 'DELIVERED' && d.status !== 'CANCELLED'
  );

  // 2. Open incoming requests
  const availableRequests = deliveries.filter(
    (d) =>
      d.status === 'SEARCHING_DRIVER' ||
      d.status === 'COUNTER_OFFER_REJECTED' ||
      (d.status === 'DRIVER_COUNTER_OFFERED' && d.counterOfferDriverId === driver.id)
  );

  // 3. Completed missions
  const completedMissions = deliveries.filter(
    (d) => d.driverId === driver.id && d.status === 'DELIVERED'
  );

  const toggleOnline = () => {
    const updated = { ...driver, isOnline: !driver.isOnline };
    setDriver(updated);
    storageService.saveDriverProfile(updated);
  };

  const handleAcceptClientOffer = async (delivery: DeliveryRequest) => {
    try {
      await storageService.driverAcceptDelivery(delivery.id, driver);
      confetti({
        particleCount: 80,
        spread: 60,
        origin: { y: 0.6 },
      });
    } catch (e: any) {
      alert(e.message || 'Erreur lors de l’acceptation');
    }
  };

  const handleOpenCounterOfferDialog = (delivery: DeliveryRequest) => {
    setCounterOfferDelivery(delivery);
    const suggested = Math.max(1500, delivery.customerInitialOffer + 500);
    setCounterPriceInput(suggested.toString());
    setCounterError(null);
  };

  const handleSubmitCounterOffer = async () => {
    if (!counterOfferDelivery) return;
    const amount = parseInt(counterPriceInput, 10) || 0;
    if (amount < MIN_PRICE_XOF) {
      setCounterError('Le montant minimum est de 1 000 FCFA');
      return;
    }

    try {
      await storageService.driverSubmitCounterOffer(counterOfferDelivery.id, driver, amount);
      setCounterOfferDelivery(null);
      alert(`Contre-offre de ${formatCurrency(amount)} envoyée avec succès au client !`);
    } catch (e: any) {
      setCounterError(e.message || 'Erreur');
    }
  };

  const handleUpdateStatus = async (deliveryId: string, nextStatus: DeliveryStatus) => {
    try {
      await storageService.driverUpdateStatus(deliveryId, nextStatus);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const handleVerifyOtp = async () => {
    if (!otpDeliveryId) return;
    if (enteredOtp.length !== 4) {
      setOtpError('Veuillez entrer le code à 4 chiffres');
      return;
    }

    try {
      await storageService.driverCompleteWithOtp(otpDeliveryId, enteredOtp);
      confetti({
        particleCount: 120,
        spread: 80,
        origin: { y: 0.5 },
      });
      setOtpDeliveryId(null);
      setEnteredOtp('');
      setOtpError(null);
      setDriver(storageService.getDriverProfile());
    } catch (e: any) {
      setOtpError(e.message || 'Code OTP invalide');
    }
  };

  return (
    <div className="space-y-6 animate-fade-in pb-16">
      {/* Driver Header & Online Switch */}
      <div className="bg-slate-900 text-white rounded-3xl p-5 sm:p-6 shadow-xl space-y-4">
        <div className="flex flex-col sm:flex-row sm:items-center justify-between gap-4">
          <div className="flex items-center gap-3.5">
            <img
              src={driver.avatarUrl || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=120&auto=format&fit=crop&q=80'}
              alt={driver.name}
              className="w-14 h-14 rounded-2xl object-cover border-2 border-blue-500 shadow-md"
            />
            <div>
              <div className="flex items-center gap-2">
                <h1 className="text-lg font-black text-white">{driver.name}</h1>
                {driver.verificationStatus === 'VERIFIED' ? (
                  <span className="inline-flex items-center gap-1 text-[10px] font-black px-2 py-0.5 rounded-full bg-emerald-500/20 text-emerald-300 border border-emerald-500/30">
                    <CheckCircle2 className="w-3 h-3" /> Certifié
                  </span>
                ) : (
                  <button
                    onClick={() => setShowVerificationModal(true)}
                    className="text-[10px] font-black px-2 py-0.5 rounded-full bg-amber-500/20 text-amber-300 border border-amber-500/30 hover:bg-amber-500/30"
                  >
                    Vérification requise
                  </button>
                )}
              </div>
              <p className="text-xs text-slate-400 mt-0.5">
                {driver.vehicleType} • {driver.vehiclePlate} • ★ {driver.rating}
              </p>
            </div>
          </div>

          {/* Online Toggle */}
          <button
            onClick={toggleOnline}
            className={`px-4 py-2.5 rounded-2xl font-black text-xs sm:text-sm flex items-center justify-center gap-2 transition-all ${
              driver.isOnline
                ? 'bg-emerald-600 hover:bg-emerald-700 text-white shadow-lg shadow-emerald-600/30'
                : 'bg-slate-800 text-slate-400 hover:text-white border border-slate-700'
            }`}
          >
            <Power className="w-4 h-4" />
            <span>{driver.isOnline ? 'EN LIGNE (Disponible)' : 'HORS LIGNE'}</span>
          </button>
        </div>

        {/* Driver Stats & Wallet Grid */}
        <div className="grid grid-cols-3 gap-2.5 pt-2 border-t border-slate-800">
          <div className="bg-slate-800/80 rounded-2xl p-3 text-center sm:text-left">
            <div className="text-[11px] text-slate-400">Solde Portefeuille</div>
            <div className="text-base sm:text-xl font-black text-emerald-400 mt-0.5">
              {formatCurrency(driver.walletBalanceXof)}
            </div>
          </div>

          <div className="bg-slate-800/80 rounded-2xl p-3 text-center sm:text-left">
            <div className="text-[11px] text-slate-400">Courses livrées</div>
            <div className="text-base sm:text-xl font-black text-white mt-0.5">
              {driver.totalDeliveries}
            </div>
          </div>

          <div className="bg-slate-800/80 rounded-2xl p-3 text-center sm:text-left">
            <div className="text-[11px] text-slate-400">Commission Livreur</div>
            <div className="text-base sm:text-xl font-black text-blue-400 mt-0.5">
              90% <span className="text-[10px] text-slate-400 font-normal">net</span>
            </div>
          </div>
        </div>
      </div>

      {/* 🚀 ACTIVE ONGOING MISSION SECTION */}
      {activeMission && (
        <div className="bg-white border-2 border-blue-600 rounded-3xl p-5 sm:p-6 shadow-xl shadow-blue-500/10 space-y-5 animate-slide-up">
          <div className="flex items-center justify-between border-b border-slate-100 pb-3">
            <div className="flex items-center gap-2">
              <span className="w-3 h-3 rounded-full bg-blue-600 animate-ping"></span>
              <h2 className="text-base font-black text-slate-900 uppercase tracking-wide">
                Course en cours • #{activeMission.id}
              </h2>
            </div>
            <span className="text-base font-black text-emerald-600">
              +{formatCurrency(activeMission.driverEarningsXof)} net
            </span>
          </div>

          {/* Interactive Route Map */}
          <DeliveryMap
            pickupLat={activeMission.pickupLat}
            pickupLng={activeMission.pickupLng}
            pickupAddress={activeMission.pickupAddress}
            destinationLat={activeMission.destinationLat}
            destinationLng={activeMission.destinationLng}
            destinationAddress={activeMission.destinationAddress}
            driverLat={activeMission.currentDriverLat}
            driverLng={activeMission.currentDriverLng}
            driverName={driver.name}
            height="200px"
          />

          {/* Client & Recipient Contacts */}
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 text-xs">
            <div className="bg-slate-50 border border-slate-200 p-3 rounded-xl flex items-center justify-between">
              <div>
                <span className="text-slate-500 block text-[10px]">EXPÉDITEUR (CLIENT)</span>
                <span className="font-bold text-slate-900">{activeMission.clientName}</span>
                <span className="block text-slate-500 font-mono text-[11px]">{activeMission.clientPhone}</span>
              </div>
              <a
                href={`tel:${activeMission.clientPhone}`}
                className="p-2.5 rounded-xl bg-blue-600 text-white shadow-sm hover:bg-blue-700"
              >
                <Phone className="w-4 h-4" />
              </a>
            </div>

            <div className="bg-slate-50 border border-slate-200 p-3 rounded-xl flex items-center justify-between">
              <div>
                <span className="text-slate-500 block text-[10px]">DESTINATAIRE DU COLIS</span>
                <span className="font-bold text-slate-900">{activeMission.recipientName}</span>
                <span className="block text-slate-500 font-mono text-[11px]">{activeMission.recipientPhone}</span>
              </div>
              <a
                href={`tel:${activeMission.recipientPhone}`}
                className="p-2.5 rounded-xl bg-emerald-600 text-white shadow-sm hover:bg-emerald-700"
              >
                <Phone className="w-4 h-4" />
              </a>
            </div>
          </div>

          {/* Mission Progress Action Stepper */}
          <div className="space-y-2 pt-2">
            <div className="text-xs font-extrabold text-slate-700">Actions d'avancement :</div>

            {activeMission.status === 'DRIVER_ASSIGNED' && (
              <button
                onClick={() => handleUpdateStatus(activeMission.id, 'DRIVER_ARRIVING')}
                className="w-full py-3.5 rounded-2xl bg-blue-600 hover:bg-blue-700 text-white font-black text-xs sm:text-sm shadow-md shadow-blue-500/20 flex items-center justify-center gap-2"
              >
                <Navigation className="w-4 h-4" />
                <span>1. Je me dirige vers l'expéditeur</span>
              </button>
            )}

            {activeMission.status === 'DRIVER_ARRIVING' && (
              <button
                onClick={() => handleUpdateStatus(activeMission.id, 'PACKAGE_PICKED_UP')}
                className="w-full py-3.5 rounded-2xl bg-purple-600 hover:bg-purple-700 text-white font-black text-xs sm:text-sm shadow-md shadow-purple-500/20 flex items-center justify-center gap-2"
              >
                <Check className="w-4 h-4" />
                <span>2. Colis récupéré auprès du client</span>
              </button>
            )}

            {(activeMission.status === 'PACKAGE_PICKED_UP' || activeMission.status === 'IN_TRANSIT') && (
              <button
                onClick={() => handleUpdateStatus(activeMission.id, 'DRIVER_ARRIVED')}
                className="w-full py-3.5 rounded-2xl bg-orange-600 hover:bg-orange-700 text-white font-black text-xs sm:text-sm shadow-md shadow-orange-500/20 flex items-center justify-center gap-2"
              >
                <MapPin className="w-4 h-4" />
                <span>3. Je suis arrivé chez le destinataire</span>
              </button>
            )}

            {activeMission.status === 'DRIVER_ARRIVED' && (
              <button
                onClick={() => setOtpDeliveryId(activeMission.id)}
                className="w-full py-4 rounded-2xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-sm shadow-lg shadow-emerald-600/30 flex items-center justify-center gap-2 animate-pulse"
              >
                <ShieldCheck className="w-5 h-5" />
                <span>4. Valider la livraison avec le Code OTP</span>
              </button>
            )}
          </div>
        </div>
      )}

      {/* 📦 INCOMING REQUESTS MARKETPLACE */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-black text-slate-900 flex items-center gap-2">
            <span>Demandes disponibles</span>
            {availableRequests.length > 0 && (
              <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">
                {availableRequests.length}
              </span>
            )}
          </h2>
        </div>

        {!driver.isOnline ? (
          <div className="bg-amber-50 border border-amber-200 rounded-3xl p-6 text-center text-xs text-amber-800 space-y-2">
            <AlertTriangle className="w-6 h-6 mx-auto text-amber-600" />
            <div className="font-bold">Vous êtes actuellement Hors Ligne</div>
            <p>Passez en ligne en haut de l'écran pour voir et accepter les demandes de livraison en temps réel.</p>
          </div>
        ) : availableRequests.length === 0 ? (
          <div className="bg-white border border-dashed border-slate-300 rounded-3xl p-8 text-center text-xs text-slate-500 space-y-2">
            <Clock className="w-6 h-6 mx-auto text-slate-400" />
            <div className="font-bold text-slate-700 text-sm">En attente de nouvelles commandes</div>
            <p>Dès qu'un client passe une commande à proximité, elle apparaîtra immédiatement ici.</p>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {availableRequests.map((delivery) => {
              const clientOffer = delivery.customerInitialOffer;
              const driverNet = Math.round(clientOffer * 0.90);
              const isMyCounterOfferPending =
                delivery.status === 'DRIVER_COUNTER_OFFERED' &&
                delivery.counterOfferDriverId === driver.id;
              const isCounterOfferRejected =
                delivery.status === 'COUNTER_OFFER_REJECTED';

              return (
                <div
                  key={delivery.id}
                  className={`bg-white rounded-3xl p-5 border-2 space-y-4 shadow-sm transition-all ${
                    isMyCounterOfferPending
                      ? 'border-amber-400 bg-amber-50/20'
                      : 'border-slate-200 hover:border-blue-300'
                  }`}
                >
                  {/* Top Bar: Title & Price */}
                  <div className="flex items-center justify-between">
                    <div className="inline-flex items-center gap-1 text-[11px] font-black text-blue-700 bg-blue-50 px-2.5 py-1 rounded-full">
                      <Sparkles className="w-3.5 h-3.5 text-blue-600" />
                      <span>NOUVELLE DEMANDE</span>
                    </div>

                    <div className="text-right">
                      <div className="text-sm font-black text-slate-900">
                        Offre client : {formatCurrency(clientOffer)}
                      </div>
                      <div className="text-xs font-bold text-emerald-600">
                        Gain net (90%) : +{formatCurrency(driverNet)}
                      </div>
                    </div>
                  </div>

                  {/* Route & Info */}
                  <div className="space-y-2 text-xs text-slate-600 bg-slate-50 p-3.5 rounded-2xl">
                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-[9px] mt-0.5">
                        A
                      </span>
                      <div>
                        <span className="font-semibold text-slate-700">Enlèvement : </span>
                        <span>{delivery.pickupAddress}</span>
                      </div>
                    </div>

                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-[9px] mt-0.5">
                        B
                      </span>
                      <div>
                        <span className="font-semibold text-slate-700">Livraison : </span>
                        <span>{delivery.destinationAddress}</span>
                      </div>
                    </div>

                    <div className="pt-2 border-t border-slate-200/80 flex justify-between text-[11px] text-slate-500">
                      <span>Colis : {delivery.packageDescription} ({delivery.packageSize})</span>
                      <span>Paiement : {delivery.paymentMethod}</span>
                    </div>
                  </div>

                  {/* Driver Actions: [ACCEPTER L'OFFRE] ou [PROPOSER UN PRIX] */}
                  {isMyCounterOfferPending ? (
                    <div className="bg-amber-100 border border-amber-300 rounded-xl p-3 text-xs text-amber-900 flex items-center gap-2">
                      <Clock className="w-4 h-4 text-amber-700 animate-spin" />
                      <span>
                        Contre-offre de <strong>{formatCurrency(delivery.driverCounterOffer || 0)}</strong> envoyée. En attente de la décision du client...
                      </span>
                    </div>
                  ) : isCounterOfferRejected ? (
                    <div className="space-y-2">
                      <div className="bg-rose-50 border border-rose-200 text-rose-800 p-2.5 rounded-xl text-xs font-medium">
                        Le client a décliné votre proposition. Vous pouvez toujours accepter l'offre initiale.
                      </div>
                      <button
                        onClick={() => handleAcceptClientOffer(delivery)}
                        className="w-full py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white text-xs font-black flex items-center justify-center gap-1.5"
                      >
                        <Check className="w-4 h-4" />
                        <span>Accepter au prix client ({formatCurrency(clientOffer)})</span>
                      </button>
                    </div>
                  ) : (
                    <div className="grid grid-cols-2 gap-2.5">
                      <button
                        type="button"
                        onClick={() => handleOpenCounterOfferDialog(delivery)}
                        className="py-3 rounded-xl border-2 border-blue-600 text-blue-700 bg-white hover:bg-blue-50 font-black text-xs transition-all"
                      >
                        Proposer un prix
                      </button>

                      <button
                        type="button"
                        onClick={() => handleAcceptClientOffer(delivery)}
                        className="py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-xs shadow-md shadow-blue-500/20 flex items-center justify-center gap-1.5 transition-all"
                      >
                        <Check className="w-4 h-4" />
                        <span>Accepter (+{driverNet} F)</span>
                      </button>
                    </div>
                  )}
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* 🌟 COUNTER-OFFER DIALOG MODAL (LIVREUR) 🌟 */}
      {counterOfferDelivery && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl p-6 space-y-4">
            <div className="flex items-center justify-between">
              <h3 className="text-base font-extrabold text-slate-900">Proposer votre tarif</h3>
              <button
                onClick={() => setCounterOfferDelivery(null)}
                className="p-1 rounded-full hover:bg-slate-100 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Offre initiale du client : <strong>{formatCurrency(counterOfferDelivery.customerInitialOffer)}</strong>.
              <br />
              Entrez votre contre-proposition (Min. 1 000 FCFA). Vous ne pouvez faire qu'une seule proposition.
            </p>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Votre proposition (FCFA) :
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={counterPriceInput}
                  onChange={(e) => {
                    const digits = e.target.value.replace(/\D/g, '');
                    setCounterPriceInput(digits);
                    const val = parseInt(digits, 10) || 0;
                    setCounterError(val < MIN_PRICE_XOF ? 'Minimum 1 000 FCFA' : null);
                  }}
                  className="w-full text-base font-black px-4 py-3 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
                />
                <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-bold text-blue-600 bg-blue-50 px-2 py-1 rounded">
                  FCFA
                </span>
              </div>
              {counterError && <p className="text-xs text-rose-600 font-bold mt-1">⚠️ {counterError}</p>}
            </div>

            {/* Earnings preview */}
            <div className="bg-emerald-50 border border-emerald-200 rounded-xl p-3 text-xs text-emerald-800 space-y-1">
              <div className="flex justify-between">
                <span>Votre gain net (90%) :</span>
                <span className="font-bold">
                  {formatCurrency(Math.round((parseInt(counterPriceInput, 10) || 0) * 0.90))}
                </span>
              </div>
              <div className="flex justify-between text-[11px] text-emerald-700">
                <span>Frais WÀNDÉ (10%) :</span>
                <span>{formatCurrency(Math.round((parseInt(counterPriceInput, 10) || 0) * 0.10))}</span>
              </div>
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setCounterOfferDelivery(null)}
                className="flex-1 py-2.5 rounded-xl border border-slate-300 text-slate-700 font-bold text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={handleSubmitCounterOffer}
                className="flex-1 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-extrabold text-xs shadow-md shadow-blue-500/20"
              >
                Envoyer au client
              </button>
            </div>
          </div>
        </div>
      )}

      {/* 🔐 OTP VERIFICATION MODAL (LIVREUR) 🔐 */}
      {otpDeliveryId && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="w-full max-w-md bg-white rounded-3xl shadow-2xl p-6 space-y-5">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2">
                <ShieldCheck className="w-6 h-6 text-emerald-600" />
                <h3 className="text-base font-extrabold text-slate-900">Validation de la livraison</h3>
              </div>
              <button
                onClick={() => setOtpDeliveryId(null)}
                className="p-1 rounded-full hover:bg-slate-100 text-slate-400"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            <p className="text-xs text-slate-600">
              Demandez au destinataire son <strong>code secret OTP à 4 chiffres</strong> et saisissez-le ci-dessous pour finaliser la livraison et créditer vos gains.
            </p>

            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Code OTP (4 chiffres) :
              </label>
              <input
                type="text"
                maxLength={4}
                value={enteredOtp}
                onChange={(e) => setEnteredOtp(e.target.value.replace(/\D/g, ''))}
                placeholder="Ex: 4829"
                className="w-full text-2xl tracking-[0.5em] text-center font-mono font-black px-4 py-3 rounded-2xl border-2 border-blue-500 focus:outline-none focus:ring-4 focus:ring-blue-100"
                autoFocus
              />
              {otpError && <p className="text-xs text-rose-600 font-bold mt-1 text-center">⚠️ {otpError}</p>}
            </div>

            <div className="flex gap-2 pt-2">
              <button
                type="button"
                onClick={() => setOtpDeliveryId(null)}
                className="flex-1 py-3 rounded-xl border border-slate-300 text-slate-700 font-bold text-xs"
              >
                Annuler
              </button>
              <button
                type="button"
                onClick={handleVerifyOtp}
                className="flex-1 py-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs shadow-md shadow-emerald-600/25"
              >
                Confirmer la remise
              </button>
            </div>
          </div>
        </div>
      )}

      {/* Driver KYC verification modal */}
      <DriverVerificationModal
        isOpen={showVerificationModal}
        onClose={() => setShowVerificationModal(false)}
        driver={driver}
        onUpdated={(u) => setDriver(u)}
      />
    </div>
  );
};
