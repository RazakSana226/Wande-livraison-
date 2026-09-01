import React from 'react';
import { formatCurrency, MIN_PRICE_XOF } from '../../services/pricingService';
import { Sparkles, Info, ShieldCheck, Check } from 'lucide-react';

interface PriceSelectorProps {
  proposedPrice: number;
  customPriceInput: string;
  onPriceSelect: (price: number) => void;
  onCustomInputChange: (val: string) => void;
  errorMessage?: string | null;
}

export const PriceSelector: React.FC<PriceSelectorProps> = ({
  proposedPrice,
  customPriceInput,
  onPriceSelect,
  onCustomInputChange,
  errorMessage,
}) => {
  const suggestions = [
    { price: 1000, label: 'Minimum', subtitle: 'Prix minimum', badge: 'Éco' },
    { price: 1500, label: 'Recommandé', subtitle: 'Prix conseillé', badge: 'Populaire' },
    { price: 2000, label: 'Express', subtitle: 'Offre attractive', badge: 'Rapide' },
  ];

  const validPrice = Math.max(MIN_PRICE_XOF, proposedPrice || MIN_PRICE_XOF);
  const commission = Math.round(validPrice * 0.10);
  const totalWithCommission = validPrice + commission;

  return (
    <div className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-card space-y-4">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h3 className="font-extrabold text-slate-900 text-base sm:text-lg flex items-center gap-1.5">
            <span>Proposez votre prix</span>
            <Sparkles className="w-4 h-4 text-amber-500" />
          </h3>
          <p className="text-xs text-slate-500">
            Fixez votre tarif ou choisissez l'une des 3 suggestions :
          </p>
        </div>
        <span className="bg-blue-50 text-blue-700 text-xs font-black px-2.5 py-1 rounded-lg border border-blue-100">
          Min : 1 000 FCFA
        </span>
      </div>

      {/* 3 Quick Suggestions Cards */}
      <div className="grid grid-cols-3 gap-2.5">
        {suggestions.map((item) => {
          const isSelected = proposedPrice === item.price;
          return (
            <button
              key={item.price}
              type="button"
              onClick={() => onPriceSelect(item.price)}
              className={`relative p-3 rounded-xl text-center border-2 transition-all flex flex-col items-center justify-between ${
                isSelected
                  ? 'border-blue-600 bg-blue-50/60 shadow-sm ring-2 ring-blue-500/20'
                  : 'border-slate-200 hover:border-slate-300 bg-slate-50/50'
              }`}
            >
              {/* Top Badge */}
              <span
                className={`text-[10px] font-extrabold px-1.5 py-0.5 rounded-md uppercase tracking-wider mb-1 ${
                  isSelected ? 'bg-blue-600 text-white' : 'bg-slate-200 text-slate-600'
                }`}
              >
                {item.badge}
              </span>

              {/* Price text */}
              <div className="my-1">
                <span
                  className={`text-sm sm:text-base font-black ${
                    isSelected ? 'text-blue-700' : 'text-slate-900'
                  }`}
                >
                  {formatCurrency(item.price)}
                </span>
                <p className="text-[10px] sm:text-[11px] text-slate-500 font-medium">
                  {item.subtitle}
                </p>
              </div>

              {isSelected && (
                <div className="w-4 h-4 rounded-full bg-blue-600 text-white flex items-center justify-center mt-1">
                  <Check className="w-2.5 h-2.5 stroke-[3]" />
                </div>
              )}
            </button>
          );
        })}
      </div>

      {/* Custom Price Input */}
      <div className="space-y-1">
        <label className="text-xs font-bold text-slate-700 block">
          Ou entrez un montant libre (FCFA) :
        </label>
        <div className="relative">
          <input
            type="text"
            inputMode="numeric"
            value={customPriceInput}
            onChange={(e) => onCustomInputChange(e.target.value)}
            placeholder="Ex: 1750"
            className={`w-full bg-slate-50 border rounded-xl px-3.5 py-2.5 text-slate-900 font-bold text-sm focus:outline-none focus:ring-2 transition-all pr-16 ${
              errorMessage
                ? 'border-red-500 focus:ring-red-400 bg-red-50/20'
                : 'border-slate-300 focus:ring-blue-500 focus:border-blue-500'
            }`}
          />
          <span className="absolute right-3 top-1/2 -translate-y-1/2 text-xs font-black text-blue-600 bg-blue-50 px-2 py-1 rounded-md border border-blue-100">
            FCFA
          </span>
        </div>
        {errorMessage ? (
          <p className="text-xs text-red-600 font-semibold">{errorMessage}</p>
        ) : (
          <p className="text-[11px] text-slate-500">
            Montant libre supérieur ou égal à 1 000 FCFA.
          </p>
        )}
      </div>

      {/* Transparent Breakdown Card */}
      <div className="bg-slate-100/90 rounded-xl p-3.5 border border-slate-200/80 space-y-2">
        <div className="flex justify-between text-xs text-slate-600">
          <span>Prix de la course pour le livreur :</span>
          <span className="font-bold text-slate-900">{formatCurrency(validPrice)}</span>
        </div>
        <div className="flex justify-between text-xs text-slate-600">
          <span>Frais de service WÀNDÉ (10%) :</span>
          <span className="font-bold text-slate-900">{formatCurrency(commission)}</span>
        </div>
        <div className="border-t border-slate-200 pt-2 flex justify-between items-center">
          <span className="font-extrabold text-xs sm:text-sm text-slate-900 uppercase">
            Total à payer :
          </span>
          <span className="text-base sm:text-lg font-black text-blue-600">
            {formatCurrency(totalWithCommission)}
          </span>
        </div>
      </div>

      {/* Notice Banner */}
      <div className="flex items-start gap-2 bg-slate-50 p-2.5 rounded-xl border border-slate-200/60 text-[11px] text-slate-600 leading-relaxed">
        <Info className="w-4 h-4 text-blue-600 flex-shrink-0 mt-0.5" />
        <span>
          Les suggestions ne garantissent pas l'acceptation immédiate. Plus l'offre est attractive, plus vite un livreur à proximité acceptera votre demande.
        </span>
      </div>
    </div>
  );
};
