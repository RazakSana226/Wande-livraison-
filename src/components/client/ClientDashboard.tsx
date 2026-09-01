import React, { useState } from 'react';
import { DeliveryRequest } from '../../types';
import { formatCurrency } from '../../services/pricingService';
import { StatusBadge } from '../common/StatusBadge';
import { 
  Plus, 
  Package, 
  MapPin, 
  Clock, 
  TrendingUp, 
  ChevronRight, 
  ShieldCheck, 
  Sparkles,
  Bike,
  CheckCircle2
} from 'lucide-react';

interface ClientDashboardProps {
  deliveries: DeliveryRequest[];
  onOpenCreate: () => void;
  onSelectDelivery: (delivery: DeliveryRequest) => void;
}

export const ClientDashboard: React.FC<ClientDashboardProps> = ({
  deliveries,
  onOpenCreate,
  onSelectDelivery,
}) => {
  const activeDeliveries = deliveries.filter((d) => d.status !== 'DELIVERED' && d.status !== 'CANCELLED');
  const pastDeliveries = deliveries.filter((d) => d.status === 'DELIVERED' || d.status === 'CANCELLED');

  return (
    <div className="space-y-6 animate-fade-in pb-16">
      {/* Hero Action Banner */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-blue-700 via-blue-600 to-indigo-700 text-white p-6 sm:p-8 shadow-xl shadow-blue-500/20">
        <div className="relative z-10 max-w-xl space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/20 backdrop-blur-md text-white text-xs font-bold border border-white/30">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Livraison express à Abidjan & environs</span>
          </div>

          <h1 className="text-2xl sm:text-4xl font-black tracking-tight text-white leading-tight">
            Envoyez vos colis au meilleur prix.
          </h1>

          <p className="text-sm sm:text-base text-blue-100 font-medium">
            Fixez votre tarif dès <strong>1 000 FCFA</strong>. Un coursier vérifié prend en charge votre livraison en quelques minutes.
          </p>

          <div className="pt-2 flex flex-wrap items-center gap-3">
            <button
              onClick={onOpenCreate}
              className="px-6 py-3.5 rounded-2xl bg-white text-blue-700 hover:bg-blue-50 font-black text-sm sm:text-base shadow-lg shadow-black/10 flex items-center gap-2.5 transition-all transform hover:-translate-y-0.5 active:scale-95"
            >
              <Plus className="w-5 h-5 text-blue-600" />
              <span>Commander un livreur</span>
            </button>
          </div>
        </div>

        {/* Decorative Graphic */}
        <div className="absolute right-4 bottom-2 sm:bottom-4 opacity-15 sm:opacity-25 pointer-events-none">
          <Bike className="w-48 h-48 sm:w-64 sm:h-64 text-white" />
        </div>
      </div>

      {/* Quick Metrics */}
      <div className="grid grid-cols-2 sm:grid-cols-3 gap-3 sm:gap-4">
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-sm">
          <div className="text-xs font-bold text-slate-500">Courses en cours</div>
          <div className="text-2xl font-black text-blue-600 mt-1">{activeDeliveries.length}</div>
        </div>
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-sm">
          <div className="text-xs font-bold text-slate-500">Livraisons effectuées</div>
          <div className="text-2xl font-black text-emerald-600 mt-1">{pastDeliveries.length}</div>
        </div>
        <div className="bg-white border border-slate-200/80 rounded-2xl p-4 shadow-sm col-span-2 sm:col-span-1">
          <div className="text-xs font-bold text-slate-500">Commission WÀNDÉ</div>
          <div className="text-2xl font-black text-slate-900 mt-1">10% <span className="text-xs font-normal text-slate-500">transparente</span></div>
        </div>
      </div>

      {/* Section: Courses Actives */}
      <div className="space-y-3">
        <div className="flex items-center justify-between">
          <h2 className="text-lg font-black text-slate-900 flex items-center gap-2">
            <span>Courses en cours</span>
            {activeDeliveries.length > 0 && (
              <span className="w-6 h-6 rounded-full bg-blue-600 text-white text-xs font-bold flex items-center justify-center">
                {activeDeliveries.length}
              </span>
            )}
          </h2>
        </div>

        {activeDeliveries.length === 0 ? (
          <div className="bg-white border border-dashed border-slate-300 rounded-3xl p-8 text-center space-y-3">
            <div className="w-12 h-12 rounded-2xl bg-blue-50 text-blue-600 flex items-center justify-center mx-auto">
              <Package className="w-6 h-6" />
            </div>
            <div className="text-slate-900 font-bold text-sm">Aucune course active</div>
            <p className="text-xs text-slate-500 max-w-sm mx-auto">
              Vous n'avez pas de livraison en cours actuellement. Cliquez sur le bouton ci-dessous pour lancer une course.
            </p>
            <button
              onClick={onOpenCreate}
              className="px-5 py-2.5 rounded-xl bg-blue-600 text-white text-xs font-extrabold hover:bg-blue-700 transition-colors shadow-md shadow-blue-500/20"
            >
              Créer une livraison
            </button>
          </div>
        ) : (
          <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
            {activeDeliveries.map((delivery) => {
              const hasCounterOffer = delivery.status === 'DRIVER_COUNTER_OFFERED';
              return (
                <div
                  key={delivery.id}
                  onClick={() => onSelectDelivery(delivery)}
                  className={`bg-white rounded-3xl p-5 border-2 transition-all cursor-pointer hover:shadow-lg ${
                    hasCounterOffer
                      ? 'border-amber-400 ring-2 ring-amber-400/20 bg-amber-50/20'
                      : 'border-slate-200 hover:border-blue-400'
                  }`}
                >
                  <div className="flex items-center justify-between mb-3">
                    <StatusBadge status={delivery.status} />
                    <span className="text-base font-black text-blue-600">
                      {formatCurrency(delivery.finalDeliveryPrice)}
                    </span>
                  </div>

                  {/* Counter Offer Alert Banner */}
                  {hasCounterOffer && (
                    <div className="mb-3 p-2.5 rounded-xl bg-amber-100 border border-amber-300 flex items-center justify-between text-xs text-amber-900 font-bold">
                      <div className="flex items-center gap-1.5">
                        <TrendingUp className="w-4 h-4 text-amber-700" />
                        <span>Contre-offre reçue : {formatCurrency(delivery.driverCounterOffer || 0)}</span>
                      </div>
                      <span className="underline text-amber-800">Voir & Décider</span>
                    </div>
                  )}

                  {/* Route Summary */}
                  <div className="space-y-2 text-xs text-slate-600">
                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-700 flex items-center justify-center font-bold text-[9px] mt-0.5">
                        A
                      </span>
                      <span className="truncate font-medium">{delivery.pickupAddress}</span>
                    </div>
                    <div className="flex items-start gap-2">
                      <span className="w-4 h-4 rounded-full bg-rose-100 text-rose-700 flex items-center justify-center font-bold text-[9px] mt-0.5">
                        B
                      </span>
                      <span className="truncate font-medium">{delivery.destinationAddress}</span>
                    </div>
                  </div>

                  {/* Footer info & OTP preview */}
                  <div className="mt-4 pt-3 border-t border-slate-100 flex items-center justify-between text-xs">
                    <div className="flex items-center gap-1.5 text-slate-500">
                      <ShieldCheck className="w-4 h-4 text-blue-600" />
                      <span>OTP : <strong>{delivery.otpCode}</strong></span>
                    </div>

                    <span className="font-extrabold text-blue-600 flex items-center gap-1">
                      <span>Suivre</span>
                      <ChevronRight className="w-4 h-4" />
                    </span>
                  </div>
                </div>
              );
            })}
          </div>
        )}
      </div>

      {/* Section: Historique des livraisons */}
      <div className="space-y-3 pt-4">
        <h2 className="text-lg font-black text-slate-900">Historique des courses</h2>

        {pastDeliveries.length === 0 ? (
          <div className="bg-white border border-slate-200 rounded-2xl p-6 text-center text-xs text-slate-500">
            Aucune course passée dans votre historique.
          </div>
        ) : (
          <div className="bg-white border border-slate-200 rounded-3xl divide-y divide-slate-100 overflow-hidden shadow-sm">
            {pastDeliveries.map((delivery) => (
              <div
                key={delivery.id}
                onClick={() => onSelectDelivery(delivery)}
                className="p-4 hover:bg-slate-50 transition-colors flex items-center justify-between cursor-pointer"
              >
                <div className="flex items-center gap-3">
                  <div className="w-10 h-10 rounded-2xl bg-emerald-50 text-emerald-600 flex items-center justify-center flex-shrink-0">
                    <CheckCircle2 className="w-5 h-5" />
                  </div>
                  <div>
                    <div className="font-extrabold text-xs sm:text-sm text-slate-900 truncate max-w-xs">
                      {delivery.packageDescription}
                    </div>
                    <div className="text-[11px] text-slate-500 truncate max-w-xs sm:max-w-md">
                      De {delivery.pickupAddress} à {delivery.destinationAddress}
                    </div>
                  </div>
                </div>

                <div className="text-right">
                  <div className="font-black text-xs sm:text-sm text-slate-900">
                    {formatCurrency(delivery.finalDeliveryPrice)}
                  </div>
                  <div className="text-[10px] text-emerald-600 font-bold">Livré</div>
                </div>
              </div>
            ))}
          </div>
        )}
      </div>
    </div>
  );
};
