import React, { useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { PackageSize, PaymentMethod } from '../../types';
import { storageService } from '../../services/storageService';
import { InteractiveMap } from '../../components/map/InteractiveMap';
import { PriceSelector } from '../../components/delivery/PriceSelector';
import { calculateDistanceKm, estimatePricing, formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import {
  ArrowLeft,
  MapPin,
  Package,
  CreditCard,
  CheckCircle2,
  Sparkles,
  ChevronRight,
  ShieldCheck,
  Phone,
  User,
  Info,
  Banknote
} from 'lucide-react';

interface CreateDeliveryScreenProps {
  onNavigate: (screen: string, deliveryId?: string) => void;
}

const POPULAR_HUBS = [
  { name: 'Plateau, Rue du Commerce', lat: 5.325, lng: -4.018 },
  { name: 'Cocody Deux-Plateaux Vallon', lat: 5.365, lng: -3.992 },
  { name: 'Zone 4, Boulevard de Marseille', lat: 5.298, lng: -3.985 },
  { name: 'Yopougon Selmer / Sideci', lat: 5.342, lng: -4.078 },
  { name: 'Marcory Résidentiel', lat: 5.302, lng: -3.992 },
  { name: 'Riviera Palmeraie', lat: 5.378, lng: -3.955 },
];

export const CreateDeliveryScreen: React.FC<CreateDeliveryScreenProps> = ({ onNavigate }) => {
  const { currentUser } = useAuth();
  const [step, setStep] = useState<number>(1);
  const [isSubmitting, setIsSubmitting] = useState<boolean>(false);

  // Step 1: Addresses
  const [pickupAddress, setPickupAddress] = useState('Plateau, Rue du Commerce, Abidjan');
  const [pickupLat, setPickupLat] = useState(5.325);
  const [pickupLng, setPickupLng] = useState(-4.018);

  const [destinationAddress, setDestinationAddress] = useState('Cocody Deux-Plateaux Vallon, Abidjan');
  const [destinationLat, setDestinationLat] = useState(5.365);
  const [destinationLng, setDestinationLng] = useState(-3.992);

  // Step 2: Parcel & Recipient
  const [recipientName, setRecipientName] = useState('Jean-Paul Kouamé');
  const [recipientPhone, setRecipientPhone] = useState('+225 07 12 34 56 78');
  const [packageDescription, setPackageDescription] = useState('Colis documents & clés express');
  const [packageSize, setPackageSize] = useState<PackageSize>('PETIT');
  const [specialNotes, setSpecialNotes] = useState('Appeler en arrivant au portail.');

  // Step 3: Pricing (Strict 1000 FCFA min & 3 suggestions)
  const distanceKm = calculateDistanceKm(pickupLat, pickupLng, destinationLat, destinationLng);
  const pricingEstimates = estimatePricing(distanceKm, packageSize);

  const [proposedPrice, setProposedPrice] = useState<number>(pricingEstimates.recommendedPriceXof);
  const [customPriceInput, setCustomPriceInput] = useState<string>(pricingEstimates.recommendedPriceXof.toString());
  const [priceError, setPriceError] = useState<string | null>(null);

  // Step 4: Payment
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ORANGE_MONEY');

  const handlePriceSuggestion = (val: number) => {
    setProposedPrice(val);
    setCustomPriceInput(val.toString());
    setPriceError(null);
  };

  const handleCustomPriceChange = (raw: string) => {
    const digitsOnly = raw.replace(/\D/g, '');
    setCustomPriceInput(digitsOnly);
    const num = parseInt(digitsOnly, 10) || 0;
    if (digitsOnly.length > 0 && num < MIN_PRICE_XOF) {
      setPriceError(`Le montant minimum est de ${formatCurrency(MIN_PRICE_XOF)}`);
    } else {
      setPriceError(null);
      if (num >= MIN_PRICE_XOF) {
        setProposedPrice(num);
      }
    }
  };

  const handleCreateOrder = async () => {
    const finalPrice = Math.max(MIN_PRICE_XOF, proposedPrice || MIN_PRICE_XOF);

    try {
      setIsSubmitting(true);
      const created = await storageService.createDelivery({
        clientId: currentUser?.id || 'client_demo_1',
        clientName: currentUser?.name || 'Awa Traoré',
        clientPhone: currentUser?.phone || '+225 07 88 12 34 56',
        pickupAddress,
        pickupLat,
        pickupLng,
        destinationAddress,
        destinationLat,
        destinationLng,
        recipientName,
        recipientPhone,
        packageDescription,
        packageSize,
        specialNotes,
        proposedPriceXof: finalPrice,
        paymentMethod,
      });

      setIsSubmitting(false);
      onNavigate('tracking', created.id);
    } catch (e) {
      setIsSubmitting(false);
      console.error(e);
      alert('Erreur lors de la création de la livraison');
    }
  };

  return (
    <div className="max-w-2xl mx-auto px-4 py-4 space-y-5 pb-24">
      {/* Top Bar */}
      <div className="flex items-center justify-between">
        <button
          onClick={() => (step > 1 ? setStep(step - 1) : onNavigate('client_home'))}
          className="flex items-center gap-1.5 text-xs font-bold text-slate-600 hover:text-slate-900 bg-white px-3 py-2 rounded-xl border border-slate-200 shadow-sm"
        >
          <ArrowLeft className="w-4 h-4" />
          <span>{step > 1 ? 'Étape précédente' : 'Retour'}</span>
        </button>

        <span className="text-xs font-black text-blue-600 bg-blue-50 px-3 py-1 rounded-full border border-blue-100">
          Étape {step} sur 4
        </span>
      </div>

      {/* Progress Bar */}
      <div className="w-full bg-slate-200 h-2 rounded-full overflow-hidden">
        <div
          className="bg-blue-600 h-full transition-all duration-300 rounded-full"
          style={{ width: `${(step / 4) * 100}%` }}
        ></div>
      </div>

      {/* STEP 1: ITINERARY & ADDRESSES */}
      {step === 1 && (
        <div className="space-y-4 animate-fade-in">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900">
              1. Trajet de livraison
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Indiquez le lieu de récupération et de destination
            </p>
          </div>

          {/* Interactive Map */}
          <InteractiveMap
            pickupLat={pickupLat}
            pickupLng={pickupLng}
            destinationLat={destinationLat}
            destinationLng={destinationLng}
            pickupAddress={pickupAddress}
            destinationAddress={destinationAddress}
            className="h-60 sm:h-72"
          />

          {/* Address Inputs */}
          <div className="bg-white rounded-2xl border border-slate-200 p-4 shadow-card space-y-4">
            {/* Pickup */}
            <div className="space-y-1.5">
              <label className="text-xs font-extrabold text-blue-700 flex items-center gap-1.5">
                <span className="w-4 h-4 rounded-full bg-blue-600 text-white flex items-center justify-center text-[10px]">
                  A
                </span>
                <span>Lieu de récupération (Expéditeur)</span>
              </label>
              <input
                type="text"
                value={pickupAddress}
                onChange={(e) => setPickupAddress(e.target.value)}
                placeholder="Ex: Plateau Rue du Commerce, Immeuble Alpha"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            {/* Destination */}
            <div className="space-y-1.5">
              <label className="text-xs font-extrabold text-emerald-700 flex items-center gap-1.5">
                <span className="w-4 h-4 rounded-full bg-emerald-600 text-white flex items-center justify-center text-[10px]">
                  B
                </span>
                <span>Lieu de livraison (Destinataire)</span>
              </label>
              <input
                type="text"
                value={destinationAddress}
                onChange={(e) => setDestinationAddress(e.target.value)}
                placeholder="Ex: Cocody Angré 8ème Tranche"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2.5 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          {/* Quick Hub presets */}
          <div className="space-y-2">
            <span className="text-xs font-bold text-slate-500">Destinations rapides fréquentes :</span>
            <div className="flex flex-wrap gap-1.5">
              {POPULAR_HUBS.map((hub) => (
                <button
                  key={hub.name}
                  type="button"
                  onClick={() => {
                    setDestinationAddress(hub.name);
                    setDestinationLat(hub.lat);
                    setDestinationLng(hub.lng);
                  }}
                  className="bg-white hover:bg-blue-50 hover:border-blue-300 border border-slate-200 text-slate-700 px-2.5 py-1.5 rounded-lg text-xs font-semibold transition-all"
                >
                  📍 {hub.name.split(',')[0]}
                </button>
              ))}
            </div>
          </div>

          <button
            onClick={() => setStep(2)}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3.5 rounded-2xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 text-sm sm:text-base transition-all"
          >
            <span>Continuer vers le colis</span>
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      )}

      {/* STEP 2: PARCEL & RECIPIENT */}
      {step === 2 && (
        <div className="space-y-4 animate-fade-in">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900">
              2. Colis & Destinataire
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Décrivez ce que vous confiez au livreur
            </p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-card space-y-4">
            {/* Recipient Details */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                  <User className="w-3.5 h-3.5 text-slate-400" />
                  <span>Nom du destinataire :</span>
                </label>
                <input
                  type="text"
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  placeholder="Ex: Kouassi Marc"
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>

              <div className="space-y-1">
                <label className="text-xs font-bold text-slate-700 flex items-center gap-1">
                  <Phone className="w-3.5 h-3.5 text-slate-400" />
                  <span>Téléphone destinataire :</span>
                </label>
                <input
                  type="tel"
                  value={recipientPhone}
                  onChange={(e) => setRecipientPhone(e.target.value)}
                  placeholder="+225 07..."
                  className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
                />
              </div>
            </div>

            {/* Package Size Selection */}
            <div className="space-y-2">
              <label className="text-xs font-bold text-slate-700 block">Format du colis :</label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'PETIT', label: 'Petit', desc: 'Enveloppe, clés, repas', icon: '✉️' },
                  { id: 'MOYEN', label: 'Moyen', desc: 'Carton, sac, vêtements', icon: '📦' },
                  { id: 'VOLUMINEUX', label: 'Volumineux', desc: 'Gros carton, électro', icon: '🚚' },
                ].map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => setPackageSize(s.id as PackageSize)}
                    className={`p-3 rounded-xl border-2 text-center transition-all flex flex-col items-center justify-center ${
                      packageSize === s.id
                        ? 'border-blue-600 bg-blue-50/60 font-black text-blue-700 ring-2 ring-blue-500/20'
                        : 'border-slate-200 bg-slate-50 text-slate-700 font-semibold'
                    }`}
                  >
                    <span className="text-xl mb-1">{s.icon}</span>
                    <span className="text-xs font-extrabold">{s.label}</span>
                    <span className="text-[10px] text-slate-500 line-clamp-1">{s.desc}</span>
                  </button>
                ))}
              </div>
            </div>

            {/* Package Description */}
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700 block">Description du contenu :</label>
              <input
                type="text"
                value={packageDescription}
                onChange={(e) => setPackageDescription(e.target.value)}
                placeholder="Ex: Pochette avec documents originaux & chargeur"
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-sm font-semibold text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>

            {/* Special Instructions */}
            <div className="space-y-1">
              <label className="text-xs font-bold text-slate-700 block">
                Instructions pour le livreur (optionnel) :
              </label>
              <textarea
                rows={2}
                value={specialNotes}
                onChange={(e) => setSpecialNotes(e.target.value)}
                placeholder="Ex: Demander le bureau 204 au 2ème étage..."
                className="w-full bg-slate-50 border border-slate-300 rounded-xl px-3.5 py-2 text-xs font-medium text-slate-900 focus:ring-2 focus:ring-blue-500 focus:outline-none"
              />
            </div>
          </div>

          <button
            onClick={() => setStep(3)}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3.5 rounded-2xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 text-sm sm:text-base transition-all"
          >
            <span>Passer à la proposition de prix</span>
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      )}

      {/* STEP 3: PROPOSE PRICE (MINIMUM 1000 FCFA & 3 SUGGESTIONS) */}
      {step === 3 && (
        <div className="space-y-4 animate-fade-in">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900">
              3. Tarification & Offre
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Distance estimée : <strong>{distanceKm} km</strong> (~{pricingEstimates.estimatedMinutes} min)
            </p>
          </div>

          <PriceSelector
            proposedPrice={proposedPrice}
            customPriceInput={customPriceInput}
            onPriceSelect={handlePriceSuggestion}
            onCustomInputChange={handleCustomPriceChange}
            errorMessage={priceError}
          />

          <button
            disabled={proposedPrice < MIN_PRICE_XOF || !!priceError}
            onClick={() => setStep(4)}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-3.5 rounded-2xl shadow-lg shadow-blue-500/25 flex items-center justify-center gap-2 text-sm sm:text-base transition-all disabled:opacity-50"
          >
            <span>Confirmer l'offre et choisir le paiement</span>
            <ChevronRight className="w-5 h-5" />
          </button>
        </div>
      )}

      {/* STEP 4: PAYMENT & CONFIRMATION */}
      {step === 4 && (
        <div className="space-y-4 animate-fade-in">
          <div>
            <h2 className="text-xl sm:text-2xl font-black text-slate-900">
              4. Mode de Paiement
            </h2>
            <p className="text-xs text-slate-500 mt-0.5">
              Choisissez comment régler votre livraison
            </p>
          </div>

          <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-card space-y-3">
            {[
              { id: 'ORANGE_MONEY', name: 'Orange Money Côte d’Ivoire', color: 'text-orange-600', badge: 'Recommandé' },
              { id: 'WAVE', name: 'Wave Mobile Money', color: 'text-sky-500', badge: 'Sans frais' },
              { id: 'MTN_MOMO', name: 'MTN Mobile Money', color: 'text-yellow-600', badge: 'Direct' },
              { id: 'MOOV_MONEY', name: 'Moov Money Flooz', color: 'text-blue-600', badge: 'Direct' },
              { id: 'CASH', name: 'Espèces à la livraison (Cash)', color: 'text-emerald-600', badge: 'À l’arrivée' },
            ].map((method) => (
              <label
                key={method.id}
                className={`flex items-center justify-between p-3.5 rounded-xl border-2 cursor-pointer transition-all ${
                  paymentMethod === method.id
                    ? 'border-blue-600 bg-blue-50/50 shadow-sm'
                    : 'border-slate-200 hover:border-slate-300'
                }`}
              >
                <div className="flex items-center gap-3">
                  <input
                    type="radio"
                    name="payment_method"
                    value={method.id}
                    checked={paymentMethod === method.id}
                    onChange={() => setPaymentMethod(method.id as PaymentMethod)}
                    className="w-4 h-4 text-blue-600 focus:ring-blue-500"
                  />
                  <div>
                    <span className="text-sm font-bold text-slate-900 block">{method.name}</span>
                    <span className="text-[11px] text-slate-500 font-medium">{method.badge}</span>
                  </div>
                </div>
                <Banknote className={`w-5 h-5 ${method.color}`} />
              </label>
            ))}
          </div>

          {/* Final Order Recap Card */}
          <div className="bg-slate-900 text-white rounded-2xl p-5 shadow-xl space-y-3">
            <div className="flex justify-between items-center pb-2 border-b border-slate-800">
              <span className="text-xs font-semibold text-slate-400">Total course + service (10%) :</span>
              <span className="text-xl font-black text-blue-400">
                {formatCurrency(proposedPrice + Math.round(proposedPrice * 0.10))}
              </span>
            </div>

            <div className="text-xs text-slate-300 space-y-1">
              <p>📍 <strong>Départ :</strong> {pickupAddress}</p>
              <p>📍 <strong>Arrivée :</strong> {destinationAddress}</p>
              <p>📦 <strong>Colis :</strong> {packageDescription} ({packageSize})</p>
            </div>
          </div>

          <button
            disabled={isSubmitting}
            onClick={handleCreateOrder}
            className="w-full bg-blue-600 hover:bg-blue-700 text-white font-black py-4 rounded-2xl shadow-xl shadow-blue-600/30 flex items-center justify-center gap-2 text-base transition-all disabled:opacity-50"
          >
            {isSubmitting ? (
              <span>Publication de votre demande...</span>
            ) : (
              <>
                <Sparkles className="w-5 h-5 text-amber-300" />
                <span>Publier et trouver un livreur</span>
              </>
            )}
          </button>
        </div>
      )}
    </div>
  );
};
