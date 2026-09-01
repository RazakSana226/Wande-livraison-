import React, { useState } from 'react';
import { DeliveryRequest } from '../../types';
import { formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import { Tag, Sparkles, X, Check, ShieldCheck } from 'lucide-react';

interface CounterOfferModalProps {
  delivery: DeliveryRequest;
  isOpen: boolean;
  onClose: () => void;
  onSubmit: (amountXof: number) => void;
}

export const CounterOfferModal: React.FC<CounterOfferModalProps> = ({
  delivery,
  isOpen,
  onClose,
  onSubmit,
}) => {
  const defaultAmount = Math.max(MIN_PRICE_XOF, delivery.customerInitialOffer + 500);
  const [inputVal, setInputVal] = useState<string>(defaultAmount.toString());
  const [errorMsg, setErrorMsg] = useState<string | null>(null);

  if (!isOpen) return null;

  const numericVal = parseInt(inputVal.replace(/\D/g, ''), 10) || 0;
  const netEarnings = Math.round(numericVal * 0.90);
  const platformFee = Math.round(numericVal * 0.10);

  const handleSubmit = (e: React.FormEvent) => {
    e.preventDefault();
    if (numericVal < MIN_PRICE_XOF) {
      setErrorMsg(`Le montant minimum d'une course est de ${formatCurrency(MIN_PRICE_XOF)}`);
      return;
    }
    onSubmit(numericVal);
    onClose();
  };

  return (
    <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
      <div className="bg-white rounded-3xl max-w-md w-full p-5 sm:p-6 shadow-2xl border border-slate-200 animate-slide-up space-y-4">
        {/* Header */}
        <div className="flex items-center justify-between border-b border-slate-100 pb-3">
          <div className="flex items-center gap-2">
            <div className="w-8 h-8 rounded-xl bg-amber-100 text-amber-700 flex items-center justify-center font-bold">
              <Tag className="w-4 h-4" />
            </div>
            <div>
              <h3 className="font-extrabold text-slate-900 text-base sm:text-lg">
                Proposer votre prix
              </h3>
              <p className="text-xs text-slate-500">Contre-offre unique pour cette course</p>
            </div>
          </div>
          <button
            onClick={onClose}
            className="p-1 rounded-lg text-slate-400 hover:text-slate-700 hover:bg-slate-100"
          >
            <X className="w-5 h-5" />
          </button>
        </div>

        {/* Current Client Offer Reference */}
        <div className="bg-slate-50 p-3 rounded-2xl border border-slate-200 flex justify-between items-center text-xs">
          <div>
            <span className="text-slate-500">Offre actuelle du client :</span>
            <p className="font-bold text-slate-800">{delivery.packageDescription}</p>
          </div>
          <span className="font-black text-sm text-blue-600 bg-blue-50 px-2.5 py-1 rounded-lg border border-blue-100">
            {formatCurrency(delivery.customerInitialOffer)}
          </span>
        </div>

        {/* Price Input Form */}
        <form onSubmit={handleSubmit} className="space-y-4">
          <div className="space-y-1">
            <label className="text-xs font-bold text-slate-700 block">
              Votre proposition tarifaire (FCFA) :
            </label>
            <div className="relative">
              <input
                type="text"
                inputMode="numeric"
                autoFocus
                value={inputVal}
                onChange={(e) => {
                  const cleaned = e.target.value.replace(/\D/g, '');
                  setInputVal(cleaned);
                  const num = parseInt(cleaned, 10) || 0;
                  if (num < MIN_PRICE_XOF && cleaned.length > 0) {
                    setErrorMsg(`Le montant minimum est de ${formatCurrency(MIN_PRICE_XOF)}`);
                  } else {
                    setErrorMsg(null);
                  }
                }}
                placeholder="Ex: 2000"
                className={`w-full bg-slate-50 border rounded-2xl px-4 py-3 text-slate-900 font-extrabold text-lg focus:outline-none focus:ring-2 transition-all pr-20 ${
                  errorMsg ? 'border-red-500 focus:ring-red-400' : 'border-slate-300 focus:ring-blue-500'
                }`}
              />
              <span className="absolute right-3.5 top-1/2 -translate-y-1/2 text-xs font-black text-blue-600 bg-blue-50 px-2 py-1 rounded-lg border border-blue-100">
                FCFA
              </span>
            </div>
            {errorMsg && <p className="text-xs text-red-600 font-semibold">{errorMsg}</p>}
          </div>

          {/* Quick preset buttons */}
          <div className="flex gap-2">
            {[delivery.customerInitialOffer + 500, delivery.customerInitialOffer + 1000, 2500].map(
              (p) => {
                const validP = Math.max(MIN_PRICE_XOF, p);
                return (
                  <button
                    key={validP}
                    type="button"
                    onClick={() => {
                      setInputVal(validP.toString());
                      setErrorMsg(null);
                    }}
                    className="flex-1 py-1.5 px-2 rounded-xl bg-slate-100 hover:bg-blue-50 hover:text-blue-700 hover:border-blue-200 border border-slate-200 text-xs font-bold text-slate-700 transition-all"
                  >
                    +{formatCurrency(validP - delivery.customerInitialOffer)}
                  </button>
                );
              }
            )}
          </div>

          {/* Net Calculation summary */}
          {numericVal >= MIN_PRICE_XOF && (
            <div className="bg-emerald-50 border border-emerald-200 rounded-2xl p-3 text-xs space-y-1 text-emerald-900">
              <div className="flex justify-between font-medium">
                <span>Votre gain net (90%) :</span>
                <span className="font-extrabold text-emerald-700">{formatCurrency(netEarnings)}</span>
              </div>
              <div className="flex justify-between text-emerald-800">
                <span>Commission WÀNDÉ (10%) :</span>
                <span>{formatCurrency(platformFee)}</span>
              </div>
            </div>
          )}

          {/* Actions */}
          <div className="flex gap-3 pt-2">
            <button
              type="button"
              onClick={onClose}
              className="flex-1 py-3 rounded-xl border border-slate-200 text-slate-700 font-bold text-sm hover:bg-slate-50 transition-all"
            >
              Annuler
            </button>
            <button
              type="submit"
              disabled={numericVal < MIN_PRICE_XOF}
              className="flex-1 py-3 rounded-xl bg-blue-600 hover:bg-blue-700 text-white font-black text-sm shadow-md shadow-blue-500/25 transition-all disabled:opacity-50 flex items-center justify-center gap-1.5"
            >
              <Check className="w-4 h-4 stroke-[3]" />
              <span>Envoyer l'offre</span>
            </button>
          </div>
        </form>
      </div>
    </div>
  );
};
