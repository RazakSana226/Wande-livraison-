import React from 'react';
import { DeliveryRequest } from '../../types';
import { formatCurrency } from '../../services/pricingService';
import { Tag, Check, X, ShieldAlert, AlertTriangle } from 'lucide-react';

interface CounterOfferCardProps {
  delivery: DeliveryRequest;
  onAccept: () => void;
  onReject: () => void;
  isLoading?: boolean;
}

export const CounterOfferCard: React.FC<CounterOfferCardProps> = ({
  delivery,
  onAccept,
  onReject,
  isLoading = false,
}) => {
  const counterPrice = delivery.driverCounterOffer || 1000;
  const commission = Math.round(counterPrice * 0.10);
  const totalCustomer = counterPrice + commission;

  return (
    <div className="bg-amber-50/90 border-2 border-amber-400 rounded-2xl p-4 sm:p-5 shadow-lg shadow-amber-500/10 space-y-4 animate-slide-up">
      {/* Top Banner */}
      <div className="flex items-center justify-between">
        <span className="inline-flex items-center gap-1.5 bg-amber-500 text-white text-xs font-black px-2.5 py-1 rounded-lg uppercase tracking-wider shadow-sm">
          <Tag className="w-3.5 h-3.5" />
          Proposition du livreur
        </span>
        <span className="text-base sm:text-xl font-black text-amber-900">
          {formatCurrency(counterPrice)}
        </span>
      </div>

      {/* Description */}
      <div className="text-xs sm:text-sm text-amber-900 leading-relaxed font-medium">
        <p>
          <strong className="font-extrabold text-amber-950">
            {delivery.counterOfferDriverName || 'Un livreur disponible'}
          </strong>{' '}
          propose de prendre en charge votre course pour{' '}
          <strong className="text-amber-950 underline">{formatCurrency(counterPrice)}</strong>{' '}
          (votre offre initiale : {formatCurrency(delivery.customerInitialOffer)}).
        </p>
      </div>

      {/* Fee Breakdown */}
      <div className="bg-white/80 rounded-xl p-3 border border-amber-200/80 text-xs space-y-1.5">
        <div className="flex justify-between text-slate-700">
          <span>Prix proposé par le livreur :</span>
          <span className="font-bold">{formatCurrency(counterPrice)}</span>
        </div>
        <div className="flex justify-between text-slate-700">
          <span>Frais de service WÀNDÉ (10%) :</span>
          <span className="font-bold">{formatCurrency(commission)}</span>
        </div>
        <div className="border-t border-amber-200 pt-1.5 flex justify-between items-center text-slate-900">
          <span className="font-extrabold uppercase text-xs">Nouveau total à payer :</span>
          <span className="font-black text-sm text-blue-600">
            {formatCurrency(totalCustomer)}
          </span>
        </div>
      </div>

      {/* 2 Strict Actions */}
      <div className="grid grid-cols-2 gap-3 pt-1">
        <button
          type="button"
          disabled={isLoading}
          onClick={onReject}
          className="w-full flex items-center justify-center gap-1.5 py-3 px-3 rounded-xl border-2 border-red-500 text-red-600 hover:bg-red-50 font-bold text-xs sm:text-sm transition-all active:scale-95 disabled:opacity-50"
        >
          <X className="w-4 h-4 stroke-[2.5]" />
          <span>Refuser</span>
        </button>

        <button
          type="button"
          disabled={isLoading}
          onClick={onAccept}
          className="w-full flex items-center justify-center gap-1.5 py-3 px-3 rounded-xl bg-emerald-600 hover:bg-emerald-700 text-white font-black text-xs sm:text-sm shadow-md shadow-emerald-600/20 transition-all active:scale-95 disabled:opacity-50"
        >
          <Check className="w-4 h-4 stroke-[3]" />
          <span>Accepter ({formatCurrency(totalCustomer)})</span>
        </button>
      </div>
    </div>
  );
};
