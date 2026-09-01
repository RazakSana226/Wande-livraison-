import React, { useState, useMemo } from 'react';
import { PackageSize, PaymentMethod } from '../../types';
import { estimatePricing, formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import { storageService } from '../../services/storageService';
import { useAuth } from '../../context/AuthContext';
import { 
  X, 
  MapPin, 
  Package, 
  CreditCard, 
  Info, 
  ArrowRight, 
  Check, 
  ShieldCheck,
  Building,
  Home,
  Store
} from 'lucide-react';
import confetti from 'canvas-confetti';

interface CreateDeliveryModalProps {
  isOpen: boolean;
  onClose: () => void;
  onSuccess: (deliveryId: string) => void;
}

const PRESET_LOCATIONS = [
  { label: 'Plateau (Centre des Affaires)', address: 'Plateau, Rue du Commerce, Abidjan', lat: 5.325, lng: -4.018, icon: Building },
  { label: 'Cocody Vallon', address: 'Cocody Deux-Plateaux Vallon, Abidjan', lat: 5.365, lng: -3.992, icon: Home },
  { label: 'Marcory Zone 4', address: 'Zone 4, Rue Paul Langevin, Marcory', lat: 5.298, lng: -3.985, icon: Store },
  { label: 'Yopougon Sideci', address: 'Yopougon Sideci, Face Pharmacie, Abidjan', lat: 5.342, lng: -4.078, icon: Home },
  { label: 'Treichville Avenue 8', address: 'Treichville Avenue 8, Grand Marché, Abidjan', lat: 5.305, lng: -4.008, icon: Store },
];

export const CreateDeliveryModal: React.FC<CreateDeliveryModalProps> = ({
  isOpen,
  onClose,
  onSuccess,
}) => {
  const { currentUser } = useAuth();

  // Form State
  const [pickupAddress, setPickupAddress] = useState('Plateau, Rue du Commerce, Abidjan');
  const [pickupLat, setPickupLat] = useState(5.325);
  const [pickupLng, setPickupLng] = useState(-4.018);

  const [destAddress, setDestAddress] = useState('Cocody Deux-Plateaux Vallon, Abidjan');
  const [destLat, setDestLat] = useState(5.365);
  const [destLng, setDestLng] = useState(-3.992);

  const [recipientName, setRecipientName] = useState('Kouassi Marcel');
  const [recipientPhone, setRecipientPhone] = useState('+225 07 44 22 11 00');

  const [packageDesc, setPackageDesc] = useState('Dossier urgent & clés');
  const [packageSize, setPackageSize] = useState<PackageSize>('PETIT');
  const [specialNotes, setSpecialNotes] = useState('');

  // Pricing State
  const [proposedPrice, setProposedPrice] = useState<number>(1500);
  const [customPriceInput, setCustomPriceInput] = useState<string>('1500');
  const [priceError, setPriceError] = useState<string | null>(null);

  // Payment State
  const [paymentMethod, setPaymentMethod] = useState<PaymentMethod>('ORANGE_MONEY');
  const [isSubmitting, setIsSubmitting] = useState(false);

  // Auto calculate estimated distance
  const pricingEst = useMemo(() => {
    return estimatePricing(4.2, packageSize);
  }, [packageSize]);

  if (!isOpen) return null;

  const handleSelectPricePreset = (amount: number) => {
    setProposedPrice(amount);
    setCustomPriceInput(amount.toString());
    setPriceError(null);
  };

  const handleCustomPriceChange = (val: string) => {
    const numeric = val.replace(/\D/g, '');
    setCustomPriceInput(numeric);
    const parsed = parseInt(numeric, 10) || 0;
    setProposedPrice(parsed);

    if (numeric.length > 0 && parsed < MIN_PRICE_XOF) {
      setPriceError('Le prix minimum obligatoire est de 1 000 FCFA');
    } else {
      setPriceError(null);
    }
  };

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault();
    if (proposedPrice < MIN_PRICE_XOF) {
      setPriceError('Le prix minimum d’une course est de 1 000 FCFA');
      return;
    }

    if (!recipientName || !recipientPhone || !packageDesc) {
      alert('Veuillez remplir tous les champs obligatoires.');
      return;
    }

    setIsSubmitting(true);

    try {
      const newDelivery = await storageService.createDelivery({
        clientId: currentUser?.id || 'client_demo',
        clientName: currentUser?.name || 'Client WÀNDÉ',
        clientPhone: currentUser?.phone || '+225 07 00 00 00 00',
        pickupAddress,
        pickupLat,
        pickupLng,
        destinationAddress: destAddress,
        destinationLat: destLat,
        destinationLng: destLng,
        recipientName,
        recipientPhone,
        packageDescription: packageDesc,
        packageSize,
        specialNotes,
        proposedPriceXof: proposedPrice,
        paymentMethod,
      });

      confetti({
        particleCount: 80,
        spread: 60,
        origin: { y: 0.6 },
        colors: ['#0066FF', '#FFB800', '#16A34A'],
      });

      setIsSubmitting(false);
      onSuccess(newDelivery.id);
      onClose();
    } catch (err: any) {
      setIsSubmitting(false);
      alert(err.message || 'Erreur lors de la création de la livraison.');
    }
  };

  // Commission calculations
  const platformFee = Math.round(proposedPrice * 0.10);
  const totalToPay = proposedPrice + platformFee;

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 sm:p-6 overflow-y-auto bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="relative w-full max-w-2xl bg-white rounded-3xl shadow-2xl overflow-hidden my-auto max-h-[92vh] flex flex-col">
        {/* Modal Header */}
        <div className="px-6 py-4 bg-slate-900 text-white flex items-center justify-between sticky top-0 z-10">
          <div className="flex items-center gap-3">
            <div className="w-10 h-10 rounded-2xl bg-blue-600 flex items-center justify-center font-black text-white text-lg shadow-md shadow-blue-500/30">
              W
            </div>
            <div>
              <h2 className="text-lg font-extrabold text-white">Commander un coursier</h2>
              <p className="text-xs text-slate-300">WÀNDÉ • Proposez votre prix en toute liberté</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-2 rounded-full hover:bg-slate-800 text-slate-400 hover:text-white transition-colors"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Modal Body / Scrollable Form */}
        <form onSubmit={handleSubmit} className="p-6 overflow-y-auto flex-1 space-y-6">
          {/* Section 1: Itinéraire */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-4">
            <div className="flex items-center gap-2 text-slate-900 font-bold text-sm">
              <MapPin className="w-4 h-4 text-blue-600" />
              <span>1. Adresses d'enlèvement et de livraison</span>
            </div>

            {/* Départ */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Lieu d'enlèvement (Expéditeur) :
              </label>
              <input
                type="text"
                value={pickupAddress}
                onChange={(e) => setPickupAddress(e.target.value)}
                className="w-full text-sm px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                required
              />
              {/* Quick Pick Preset */}
              <div className="flex flex-wrap gap-1.5 mt-2">
                {PRESET_LOCATIONS.slice(0, 3).map((p, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setPickupAddress(p.address);
                      setPickupLat(p.lat);
                      setPickupLng(p.lng);
                    }}
                    className="text-[11px] font-medium bg-white hover:bg-blue-50 hover:text-blue-700 text-slate-600 border border-slate-200 rounded-lg px-2.5 py-1 transition-colors"
                  >
                    📍 {p.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Arrivée */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Lieu de destination (Destinataire) :
              </label>
              <input
                type="text"
                value={destAddress}
                onChange={(e) => setDestAddress(e.target.value)}
                className="w-full text-sm px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 font-medium"
                required
              />
              {/* Quick Pick Preset */}
              <div className="flex flex-wrap gap-1.5 mt-2">
                {PRESET_LOCATIONS.slice(1, 4).map((p, idx) => (
                  <button
                    key={idx}
                    type="button"
                    onClick={() => {
                      setDestAddress(p.address);
                      setDestLat(p.lat);
                      setDestLng(p.lng);
                    }}
                    className="text-[11px] font-medium bg-white hover:bg-blue-50 hover:text-blue-700 text-slate-600 border border-slate-200 rounded-lg px-2.5 py-1 transition-colors"
                  >
                    🎯 {p.label}
                  </button>
                ))}
              </div>
            </div>

            {/* Destinataire Contact */}
            <div className="grid grid-cols-1 sm:grid-cols-2 gap-3 pt-1">
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Nom du destinataire :
                </label>
                <input
                  type="text"
                  value={recipientName}
                  onChange={(e) => setRecipientName(e.target.value)}
                  placeholder="ex: Marcel Kouassi"
                  className="w-full text-sm px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
                  required
                />
              </div>
              <div>
                <label className="block text-xs font-semibold text-slate-600 mb-1">
                  Téléphone du destinataire :
                </label>
                <input
                  type="tel"
                  value={recipientPhone}
                  onChange={(e) => setRecipientPhone(e.target.value)}
                  placeholder="+225 07..."
                  className="w-full text-sm px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500 font-mono text-xs"
                  required
                />
              </div>
            </div>
          </div>

          {/* Section 2: Détails du colis */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-4">
            <div className="flex items-center gap-2 text-slate-900 font-bold text-sm">
              <Package className="w-4 h-4 text-blue-600" />
              <span>2. Description du colis</span>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Contenu du colis :
              </label>
              <input
                type="text"
                value={packageDesc}
                onChange={(e) => setPackageDesc(e.target.value)}
                placeholder="Ex: Clés, repas, documents, vêtements..."
                className="w-full text-sm px-3.5 py-2.5 rounded-xl border border-slate-300 focus:outline-none focus:ring-2 focus:ring-blue-500"
                required
              />
            </div>

            {/* Package Size Choices */}
            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-2">
                Taille du colis :
              </label>
              <div className="grid grid-cols-3 gap-2">
                {[
                  { id: 'PETIT', label: 'Petit', desc: 'Enveloppe, clés, repas' },
                  { id: 'MOYEN', label: 'Moyen', desc: 'Carton moyen, habits' },
                  { id: 'VOLUMINEUX', label: 'Volumineux', desc: 'Gros colis, équipement' },
                ].map((s) => (
                  <button
                    key={s.id}
                    type="button"
                    onClick={() => setPackageSize(s.id as PackageSize)}
                    className={`p-3 text-left rounded-xl border transition-all ${
                      packageSize === s.id
                        ? 'border-blue-600 bg-blue-50/70 ring-2 ring-blue-500/20 text-blue-900'
                        : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300'
                    }`}
                  >
                    <div className="font-bold text-xs">{s.label}</div>
                    <div className="text-[10px] text-slate-500 mt-0.5 leading-tight">{s.desc}</div>
                  </button>
                ))}
              </div>
            </div>

            <div>
              <label className="block text-xs font-semibold text-slate-600 mb-1">
                Instructions spéciales (facultatif) :
              </label>
              <input
                type="text"
                value={specialNotes}
                onChange={(e) => setSpecialNotes(e.target.value)}
                placeholder="Ex: Appeler en bas de l'immeuble..."
                className="w-full text-xs px-3.5 py-2 rounded-xl border border-slate-200 bg-white focus:outline-none focus:ring-2 focus:ring-blue-500"
              />
            </div>
          </div>

          {/* Section 3: TARIFICATION & NÉGOCIATION (Min 1000 FCFA) */}
          <div className="bg-gradient-to-br from-blue-50/80 via-white to-amber-50/50 border-2 border-blue-200 rounded-2xl p-5 space-y-4 shadow-sm">
            <div className="flex items-center justify-between">
              <div className="flex items-center gap-2 text-slate-900 font-extrabold text-sm sm:text-base">
                <span>3. Proposez votre prix</span>
              </div>
              <span className="text-[11px] font-black uppercase tracking-wider px-2.5 py-1 rounded-full bg-blue-600 text-white shadow-sm">
                Min. 1 000 FCFA
              </span>
            </div>

            <p className="text-xs text-slate-600">
              Choisissez l'une des 3 suggestions ou entrez un montant personnalisé :
            </p>

            {/* 3 Suggestions */}
            <div className="grid grid-cols-3 gap-2 sm:gap-3">
              {[
                { price: 1000, label: 'Minimum', sub: 'Prix plancher', tagBg: 'bg-slate-200 text-slate-700' },
                { price: 1500, label: 'Recommandé', sub: 'Tarif standard', tagBg: 'bg-blue-600 text-white' },
                { price: 2000, label: 'Express', sub: 'Plus attractif', tagBg: 'bg-amber-500 text-white' },
              ].map((s) => {
                const isSelected = proposedPrice === s.price;
                return (
                  <button
                    key={s.price}
                    type="button"
                    onClick={() => handleSelectPricePreset(s.price)}
                    className={`p-3 rounded-2xl border-2 text-center transition-all flex flex-col items-center justify-between ${
                      isSelected
                        ? 'border-blue-600 bg-white shadow-md shadow-blue-500/10 scale-[1.02]'
                        : 'border-slate-200 bg-white/70 hover:border-slate-300'
                    }`}
                  >
                    <span className={`text-[10px] font-extrabold px-2 py-0.5 rounded-md ${s.tagBg}`}>
                      {s.label}
                    </span>
                    <span className="text-base sm:text-lg font-black text-slate-900 mt-1">
                      {s.price} F
                    </span>
                    <span className="text-[10px] text-slate-500">{s.sub}</span>
                  </button>
                );
              })}
            </div>

            {/* Custom Input */}
            <div>
              <label className="block text-xs font-semibold text-slate-700 mb-1">
                Ou montant libre :
              </label>
              <div className="relative">
                <input
                  type="text"
                  value={customPriceInput}
                  onChange={(e) => handleCustomPriceChange(e.target.value)}
                  placeholder="1000"
                  className={`w-full text-base font-extrabold px-4 py-2.5 rounded-xl border ${
                    priceError ? 'border-rose-500 focus:ring-rose-200' : 'border-slate-300 focus:ring-blue-500'
                  } focus:outline-none focus:ring-2`}
                />
                <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-bold text-blue-600 bg-blue-50 px-2 py-1 rounded-md">
                  FCFA
                </span>
              </div>
              {priceError && (
                <p className="text-xs text-rose-600 font-bold mt-1.5 flex items-center gap-1">
                  ⚠️ {priceError}
                </p>
              )}
            </div>

            {/* Transparent Breakdown Card */}
            <div className="bg-white border border-slate-200 rounded-xl p-3.5 space-y-2 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Prix de la course proposé :</span>
                <span className="font-bold text-slate-900">{formatCurrency(proposedPrice)}</span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Frais de service WÀNDÉ (10%) :</span>
                <span className="font-bold text-slate-900">{formatCurrency(platformFee)}</span>
              </div>
              <div className="border-t border-slate-100 pt-2 flex justify-between items-center">
                <span className="font-extrabold text-slate-900 text-sm">TOTAL À PAYER :</span>
                <span className="font-black text-blue-600 text-base">{formatCurrency(totalToPay)}</span>
              </div>
            </div>

            <div className="flex items-start gap-2 text-[11px] text-slate-500 bg-slate-100/80 p-2.5 rounded-xl">
              <Info className="w-4 h-4 text-blue-600 flex-shrink-0 mt-0.5" />
              <span>
                Les livreurs à proximité recevront immédiatement votre offre de {formatCurrency(proposedPrice)}. Plus l'offre est attractive, plus vite votre course est acceptée !
              </span>
            </div>
          </div>

          {/* Section 4: Mode de paiement */}
          <div className="bg-slate-50 border border-slate-200 rounded-2xl p-4 space-y-3">
            <div className="flex items-center gap-2 text-slate-900 font-bold text-sm">
              <CreditCard className="w-4 h-4 text-blue-600" />
              <span>4. Mode de paiement</span>
            </div>

            <div className="grid grid-cols-2 sm:grid-cols-4 gap-2">
              {[
                { id: 'ORANGE_MONEY', name: 'Orange Money', color: 'bg-orange-500' },
                { id: 'WAVE', name: 'Wave CI', color: 'bg-cyan-500' },
                { id: 'MTN_MOMO', name: 'MTN MoMo', color: 'bg-yellow-500' },
                { id: 'CASH', name: 'Espèces', color: 'bg-emerald-600' },
              ].map((p) => (
                <button
                  key={p.id}
                  type="button"
                  onClick={() => setPaymentMethod(p.id as PaymentMethod)}
                  className={`p-3 rounded-xl border text-center transition-all ${
                    paymentMethod === p.id
                      ? 'border-blue-600 bg-blue-50/70 text-blue-900 ring-2 ring-blue-500/20 font-bold'
                      : 'border-slate-200 bg-white text-slate-700 hover:border-slate-300 font-medium'
                  }`}
                >
                  <div className="text-xs truncate">{p.name}</div>
                </button>
              ))}
            </div>
          </div>
        </form>

        {/* Modal Footer / Submit Button */}
        <div className="px-6 py-4 bg-white border-t border-slate-200 flex items-center justify-between sticky bottom-0 z-10">
          <div className="text-left">
            <div className="text-[11px] text-slate-500">Montant total</div>
            <div className="text-lg font-black text-blue-600">{formatCurrency(totalToPay)}</div>
          </div>

          <div className="flex items-center gap-3">
            <button
              type="button"
              onClick={onClose}
              className="px-4 py-2.5 rounded-xl border border-slate-300 text-slate-700 text-sm font-bold hover:bg-slate-50 transition-colors"
            >
              Annuler
            </button>
            <button
              type="button"
              disabled={isSubmitting || proposedPrice < MIN_PRICE_XOF}
              onClick={handleSubmit}
              className="px-6 py-2.5 rounded-xl bg-blue-600 hover:bg-blue-700 disabled:opacity-50 text-white text-sm font-extrabold shadow-lg shadow-blue-500/25 flex items-center gap-2 transition-all"
            >
              {isSubmitting ? (
                <span>Envoi...</span>
              ) : (
                <>
                  <span>Lancer la course</span>
                  <ArrowRight className="w-4 h-4" />
                </>
              )}
            </button>
          </div>
        </div>
      </div>
    </div>
  );
};
