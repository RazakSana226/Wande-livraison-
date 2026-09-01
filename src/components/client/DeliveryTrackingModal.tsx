import React, { useState } from 'react';
import { DeliveryRequest } from '../../types';
import { storageService } from '../../services/storageService';
import { formatCurrency } from '../../services/pricingService';
import { DeliveryMap } from '../map/DeliveryMap';
import { StatusBadge } from '../common/StatusBadge';
import { 
  X, 
  Phone, 
  MapPin, 
  ShieldCheck, 
  TrendingUp, 
  Check, 
  XCircle, 
  Copy, 
  AlertCircle, 
  Clock, 
  CheckCircle2, 
  ArrowUpRight,
  Sparkles
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface DeliveryTrackingModalProps {
  delivery: DeliveryRequest;
  onClose: () => void;
}

export const DeliveryTrackingModal: React.FC<DeliveryTrackingModalProps> = ({
  delivery,
  onClose,
}) => {
  const [copiedOtp, setCopiedOtp] = useState(false);
  const [showBooster, setShowBooster] = useState(false);
  const [boostInput, setBoostInput] = useState((delivery.customerInitialOffer + 500).toString());

  const handleCopyOtp = () => {
    navigator.clipboard.writeText(delivery.otpCode);
    setCopiedOtp(true);
    setTimeout(() => setCopiedOtp(false), 2000);
  };

  const handleAcceptCounterOffer = async () => {
    try {
      await storageService.clientAcceptCounterOffer(delivery.id);
      confetti({
        particleCount: 70,
        spread: 50,
        origin: { y: 0.7 },
      });
    } catch (e: any) {
      alert(e.message || 'Erreur lors de l’acceptation');
    }
  };

  const handleRejectCounterOffer = async () => {
    try {
      await storageService.clientRejectCounterOffer(delivery.id);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const handleBoostOffer = async (amount: number) => {
    try {
      await storageService.clientUpdateOffer(delivery.id, amount);
      setShowBooster(false);
    } catch (e: any) {
      alert(e.message || 'Erreur');
    }
  };

  const isCounterOfferActive =
    delivery.status === 'DRIVER_COUNTER_OFFERED' && delivery.driverCounterOffer;

  const counterPrice = delivery.driverCounterOffer || 0;
  const counterCommission = Math.round(counterPrice * 0.10);
  const counterTotal = counterPrice + counterCommission;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-3 sm:p-6 overflow-y-auto bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[95vh] flex flex-col">
        {/* Modal Header */}
        <div className="px-5 py-4 bg-slate-900 text-white flex items-center justify-between sticky top-0 z-20">
          <div className="flex items-center gap-3">
            <div className="w-9 h-9 rounded-xl bg-blue-600 flex items-center justify-center font-black text-white text-base">
              W
            </div>
            <div>
              <div className="flex items-center gap-2">
                <h2 className="text-base font-extrabold text-white">Suivi de course</h2>
                <span className="text-xs font-mono text-slate-300 bg-slate-800 px-2 py-0.5 rounded">
                  #{delivery.id}
                </span>
              </div>
              <p className="text-[11px] text-slate-300">WÀNDÉ • Temps réel</p>
            </div>
          </div>

          <div className="flex items-center gap-3">
            <StatusBadge status={delivery.status} size="sm" />
            <button
              onClick={onClose}
              className="p-1.5 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
            >
              <X className="w-5 h-5" />
            </button>
          </div>
        </div>

        {/* Modal Body */}
        <div className="p-4 sm:p-6 overflow-y-auto flex-1 space-y-5">
          {/* Map Preview */}
          <DeliveryMap
            pickupLat={delivery.pickupLat}
            pickupLng={delivery.pickupLng}
            pickupAddress={delivery.pickupAddress}
            destinationLat={delivery.destinationLat}
            destinationLng={delivery.destinationLng}
            destinationAddress={delivery.destinationAddress}
            driverLat={delivery.currentDriverLat}
            driverLng={delivery.currentDriverLng}
            driverName={delivery.driverName}
            height="220px"
          />

          {/* 🌟 PROPOSITION LIVREUR / CONTRE-OFFRE ACTIVE (DECISION CLIENT) 🌟 */}
          {isCounterOfferActive && (
            <div className="bg-gradient-to-br from-amber-500/10 via-amber-50 to-yellow-100/50 border-2 border-amber-400 rounded-2xl p-4 sm:p-5 shadow-md animate-slide-up space-y-4">
              <div className="flex items-center justify-between">
                <span className="text-xs font-black uppercase tracking-wider px-2.5 py-1 rounded-full bg-amber-500 text-white shadow-sm flex items-center gap-1.5">
                  <TrendingUp className="w-3.5 h-3.5" />
                  PROPOSITION DU LIVREUR
                </span>
                <span className="text-xl font-black text-amber-900">
                  {formatCurrency(counterPrice)}
                </span>
              </div>

              <p className="text-xs sm:text-sm text-amber-900 font-medium">
                <strong>{delivery.counterOfferDriverName || 'Un livreur disponible'}</strong> vous propose de prendre votre course pour <strong>{formatCurrency(counterPrice)}</strong> (au lieu de votre offre initiale de {formatCurrency(delivery.customerInitialOffer)}).
              </p>

              {/* Fee Breakdown */}
              <div className="bg-white/90 border border-amber-200 rounded-xl p-3 text-xs space-y-1.5 text-slate-700">
                <div className="flex justify-between">
                  <span>Prix de la course proposé :</span>
                  <span className="font-bold">{formatCurrency(counterPrice)}</span>
                </div>
                <div className="flex justify-between">
                  <span>Frais de service WÀNDÉ (10%) :</span>
                  <span className="font-bold">{formatCurrency(counterCommission)}</span>
                </div>
                <div className="border-t border-amber-100 pt-1.5 flex justify-between items-center text-slate-900">
                  <span className="font-extrabold">TOTAL À PAYER :</span>
                  <span className="font-black text-blue-600 text-sm">{formatCurrency(counterTotal)}</span>
                </div>
              </div>

              {/* Strict 2 Actions: Refuse or Accept */}
              <div className="grid grid-cols-2 gap-3 pt-1">
                <button
                  onClick={handleRejectCounterOffer}
                  className="py-3 px-4 rounded-xl border-2 border-rose-500 text-rose-700 bg-white hover:bg-rose-50 font-extrabold text-xs sm:text-sm flex items-center justify-center gap-2 transition-all"
                >
                  <XCircle className="w-4 h-4" />
                  <span>Refuser l'offre</span>
                </button>

                <button
                  onClick={handleAcceptCounterOffer}
                  className="py-3 px-4 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-extrabold text-xs sm:text-sm shadow-md shadow-emerald-600/30 flex items-center justify-center gap-2 transition-all"
                >
                  <Check className="w-4 h-4" />
                  <span>Accepter ({formatCurrency(counterTotal)})</span>
                </button>
              </div>
            </div>
          )}

          {/* OTP Code Secret Banner (Essentiel pour valider la livraison) */}
          <div className="bg-slate-900 text-white rounded-2xl p-4 sm:p-5 flex flex-col sm:flex-row items-center justify-between gap-4 shadow-lg">
            <div className="space-y-1 text-center sm:text-left">
              <div className="flex items-center justify-center sm:justify-start gap-1.5 text-xs text-blue-400 font-bold uppercase tracking-wider">
                <ShieldCheck className="w-4 h-4 text-blue-400" />
                <span>Code de Sécurité OTP</span>
              </div>
              <p className="text-xs text-slate-300">
                À communiquer au livreur <strong>uniquement</strong> lors de la remise finale du colis.
              </p>
            </div>

            <div className="flex items-center gap-3">
              <div className="bg-blue-600/30 border border-blue-500/50 rounded-2xl px-5 py-2 text-center">
                <span className="text-2xl sm:text-3xl font-mono font-black tracking-widest text-white">
                  {delivery.otpCode}
                </span>
              </div>

              <button
                onClick={handleCopyOtp}
                className="p-2.5 rounded-xl bg-slate-800 hover:bg-slate-700 text-slate-300 hover:text-white border border-slate-700 transition-colors"
                title="Copier le code"
              >
                {copiedOtp ? <Check className="w-5 h-5 text-emerald-400" /> : <Copy className="w-5 h-5" />}
              </button>
            </div>
          </div>

          {/* Livreur Info (if assigned) */}
          {delivery.driverId ? (
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 flex items-center justify-between">
              <div className="flex items-center gap-3">
                <img
                  src={delivery.driverPhoto || 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=100&auto=format&fit=crop&q=80'}
                  alt={delivery.driverName}
                  className="w-12 h-12 rounded-full object-cover border-2 border-blue-600"
                />
                <div>
                  <div className="font-extrabold text-sm text-slate-900 flex items-center gap-1.5">
                    <span>{delivery.driverName}</span>
                    <span className="text-[11px] font-bold px-1.5 py-0.5 rounded bg-amber-100 text-amber-800">
                      ★ {delivery.driverRating || 4.9}
                    </span>
                  </div>
                  <div className="text-xs text-slate-500 mt-0.5">
                    {delivery.driverVehicle || 'Moto coursier WÀNDÉ'}
                  </div>
                </div>
              </div>

              {delivery.driverPhone && (
                <a
                  href={`tel:${delivery.driverPhone}`}
                  className="p-3 rounded-xl bg-blue-600 text-white shadow-md shadow-blue-500/20 hover:bg-blue-700 transition-transform active:scale-95"
                >
                  <Phone className="w-4 h-4" />
                </a>
              )}
            </div>
          ) : (
            /* Searching driver booster */
            <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-3">
              <div className="flex items-center justify-between">
                <div>
                  <div className="text-xs text-slate-500">Votre offre actuelle</div>
                  <div className="text-base font-black text-slate-900">
                    {formatCurrency(delivery.customerInitialOffer)}
                  </div>
                </div>

                {!showBooster ? (
                  <button
                    onClick={() => setShowBooster(true)}
                    className="text-xs font-bold text-blue-600 bg-blue-50 hover:bg-blue-100 px-3 py-1.5 rounded-lg border border-blue-200 transition-colors flex items-center gap-1"
                  >
                    <Sparkles className="w-3.5 h-3.5" />
                    <span>Augmenter l'offre</span>
                  </button>
                ) : null}
              </div>

              {showBooster && (
                <div className="pt-2 border-t border-slate-200 space-y-2">
                  <div className="text-xs text-slate-600 font-semibold">
                    Booster votre offre pour attirer un livreur immédiatement :
                  </div>
                  <div className="flex gap-2">
                    {[1500, 2000, 2500].map((amt) => (
                      <button
                        key={amt}
                        onClick={() => handleBoostOffer(amt)}
                        className="flex-1 py-1.5 rounded-lg bg-white border border-blue-300 text-blue-700 text-xs font-bold hover:bg-blue-50"
                      >
                        {amt} F
                      </button>
                    ))}
                  </div>
                </div>
              )}
            </div>
          )}

          {/* Details & Addresses */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-3 text-xs">
            <div className="font-bold text-slate-800 text-sm border-b border-slate-200 pb-2">
              Détails de l'expédition
            </div>

            <div className="space-y-2">
              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
                  A
                </span>
                <div>
                  <span className="font-semibold text-slate-700">Départ : </span>
                  <span className="text-slate-600">{delivery.pickupAddress}</span>
                </div>
              </div>

              <div className="flex items-start gap-2.5">
                <span className="w-5 h-5 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-[10px] flex-shrink-0 mt-0.5">
                  B
                </span>
                <div>
                  <span className="font-semibold text-slate-700">Arrivée : </span>
                  <span className="text-slate-600">{delivery.destinationAddress}</span>
                  <div className="text-[11px] text-slate-500 font-medium mt-0.5">
                    Destinataire : {delivery.recipientName} ({delivery.recipientPhone})
                  </div>
                </div>
              </div>
            </div>

            <div className="border-t border-slate-200 pt-2 flex justify-between text-slate-600">
              <span>Colis : {delivery.packageDescription} ({delivery.packageSize})</span>
              <span>Paiement : {delivery.paymentMethod}</span>
            </div>
          </div>
        </div>

        {/* Modal Footer */}
        <div className="px-6 py-3.5 bg-slate-100 border-t border-slate-200 flex justify-end">
          <button
            onClick={onClose}
            className="px-5 py-2 rounded-xl bg-slate-900 text-white text-xs font-bold hover:bg-slate-800 transition-colors"
          >
            Fermer
          </button>
        </div>
      </div>
    </div>
  );
};
