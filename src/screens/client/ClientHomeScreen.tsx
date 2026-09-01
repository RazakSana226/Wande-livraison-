import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { DeliveryRequest } from '../../types';
import { storageService } from '../../services/storageService';
import { StatusBadge } from '../../components/common/StatusBadge';
import { formatCurrency } from '../../services/pricingService';
import {
  Package,
  Utensils,
  FileText,
  ShoppingBag,
  ArrowRight,
  Sparkles,
  MapPin,
  Clock,
  ChevronRight,
  Plus,
  ShieldCheck,
  Bike,
  Tag
} from 'lucide-react';

interface ClientHomeScreenProps {
  onNavigate: (screen: string, deliveryId?: string) => void;
}

export const ClientHomeScreen: React.FC<ClientHomeScreenProps> = ({ onNavigate }) => {
  const { currentUser } = useAuth();
  const [deliveries, setDeliveries] = useState<DeliveryRequest[]>([]);

  useEffect(() => {
    const unsub = storageService.subscribe((list) => {
      setDeliveries(list);
    });
    return () => unsub();
  }, []);

  const activeDeliveries = deliveries.filter(
    (d) => d.status !== 'DELIVERED' && d.status !== 'CANCELLED'
  );
  const recentCompleted = deliveries.filter((d) => d.status === 'DELIVERED').slice(0, 3);

  const categories = [
    {
      id: 'express',
      title: 'Colis Express',
      desc: 'Clés, cadeaux, paquets',
      icon: Package,
      color: 'bg-blue-500 text-white',
      badge: 'Instantané',
    },
    {
      id: 'food',
      title: 'Repas & Nourriture',
      desc: 'Restaurants & traiteurs',
      icon: Utensils,
      color: 'bg-amber-500 text-white',
      badge: 'Chaud',
    },
    {
      id: 'docs',
      title: 'Documents Urgents',
      desc: 'Contrats, courriers, CNI',
      icon: FileText,
      color: 'bg-emerald-500 text-white',
      badge: 'Sécurisé',
    },
    {
      id: 'shop',
      title: 'E-Commerce & Pro',
      desc: 'Boutiques en ligne & livraisons',
      icon: ShoppingBag,
      color: 'bg-indigo-500 text-white',
      badge: 'Suivi live',
    },
  ];

  return (
    <div className="space-y-6 pb-20 max-w-4xl mx-auto px-4 pt-4">
      {/* Hero Welcome & Quick Action */}
      <div className="relative overflow-hidden rounded-3xl bg-gradient-to-br from-blue-700 via-blue-600 to-blue-800 text-white p-6 sm:p-8 shadow-xl shadow-blue-600/20">
        <div className="absolute top-0 right-0 w-64 h-64 bg-white/10 rounded-full blur-3xl -mr-20 -mt-20 pointer-events-none"></div>

        <div className="relative z-10 max-w-xl space-y-4">
          <div className="inline-flex items-center gap-2 px-3 py-1 rounded-full bg-white/15 backdrop-blur-md text-xs font-semibold border border-white/20">
            <Sparkles className="w-3.5 h-3.5 text-amber-300" />
            <span>Nouveau : Proposez votre prix dès 1 000 FCFA</span>
          </div>

          <div>
            <h1 className="text-2xl sm:text-3xl md:text-4xl font-black tracking-tight leading-tight">
              Bonjour {currentUser?.name?.split(' ')[0] || 'Awa'} 👋
            </h1>
            <p className="text-blue-100 text-sm sm:text-base mt-1 font-medium leading-relaxed">
              Un coursier récupère et livre votre colis partout dans la ville en quelques minutes.
            </p>
          </div>

          <div className="pt-2">
            <button
              onClick={() => onNavigate('create_delivery')}
              className="inline-flex items-center gap-2.5 bg-white text-blue-700 hover:bg-blue-50 font-black px-6 py-3.5 rounded-2xl text-sm sm:text-base shadow-lg shadow-black/10 transition-all hover:scale-[1.02] active:scale-95"
            >
              <Plus className="w-5 h-5 stroke-[3]" />
              <span>Commander une livraison</span>
            </button>
          </div>
        </div>
      </div>

      {/* Active Delivery Highlight Alert (If any) */}
      {activeDeliveries.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-black text-slate-900 text-base sm:text-lg flex items-center gap-2">
              <span className="w-2.5 h-2.5 rounded-full bg-blue-600 animate-ping"></span>
              <span>Course en cours ({activeDeliveries.length})</span>
            </h2>
            <button
              onClick={() => onNavigate('history')}
              className="text-xs font-bold text-blue-600 hover:underline"
            >
              Tout voir
            </button>
          </div>

          <div className="space-y-3">
            {activeDeliveries.map((delivery) => (
              <div
                key={delivery.id}
                onClick={() => onNavigate('tracking', delivery.id)}
                className={`bg-white rounded-2xl border p-4 sm:p-5 shadow-card hover:border-blue-300 transition-all cursor-pointer space-y-3 ${
                  delivery.status === 'DRIVER_COUNTER_OFFERED'
                    ? 'border-amber-400 bg-amber-50/40'
                    : 'border-slate-200'
                }`}
              >
                {/* Header */}
                <div className="flex items-center justify-between gap-2">
                  <div className="flex items-center gap-2">
                    <span className="text-xs font-black text-slate-400">#{delivery.id}</span>
                    <StatusBadge status={delivery.status} size="sm" />
                  </div>
                  <span className="text-sm font-black text-blue-600">
                    {formatCurrency(delivery.finalDeliveryPrice)}
                  </span>
                </div>

                {/* Counter Offer Alert Banner */}
                {delivery.status === 'DRIVER_COUNTER_OFFERED' && (
                  <div className="bg-amber-100/90 border border-amber-300 text-amber-900 p-2.5 rounded-xl text-xs font-bold flex items-center justify-between">
                    <div className="flex items-center gap-1.5">
                      <Tag className="w-4 h-4 text-amber-700" />
                      <span>Contre-offre de {formatCurrency(delivery.driverCounterOffer || 0)} reçue !</span>
                    </div>
                    <span className="bg-amber-600 text-white px-2 py-0.5 rounded-md text-[10px] font-black">
                      Décider
                    </span>
                  </div>
                )}

                {/* Addresses */}
                <div className="space-y-2 text-xs">
                  <div className="flex items-start gap-2">
                    <div className="w-4 h-4 rounded-full bg-blue-100 text-blue-600 flex items-center justify-center font-bold text-[10px] mt-0.5">
                      A
                    </div>
                    <p className="text-slate-700 font-medium truncate">{delivery.pickupAddress}</p>
                  </div>
                  <div className="flex items-start gap-2">
                    <div className="w-4 h-4 rounded-full bg-emerald-100 text-emerald-600 flex items-center justify-center font-bold text-[10px] mt-0.5">
                      B
                    </div>
                    <p className="text-slate-700 font-medium truncate">{delivery.destinationAddress}</p>
                  </div>
                </div>

                {/* Footer Action */}
                <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                  <span className="text-slate-500 font-medium">
                    Destinataire : <strong className="text-slate-800">{delivery.recipientName}</strong>
                  </span>
                  <div className="flex items-center gap-1 font-bold text-blue-600">
                    <span>Suivre en direct</span>
                    <ChevronRight className="w-4 h-4" />
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}

      {/* Categories Grid */}
      <div className="space-y-3">
        <h2 className="font-black text-slate-900 text-base sm:text-lg">
          Que souhaitez-vous envoyer ?
        </h2>
        <div className="grid grid-cols-2 sm:grid-cols-4 gap-3">
          {categories.map((cat) => {
            const Icon = cat.icon;
            return (
              <button
                key={cat.id}
                onClick={() => onNavigate('create_delivery')}
                className="bg-white rounded-2xl border border-slate-200 p-4 text-left shadow-soft hover:shadow-card hover:border-blue-300 transition-all group flex flex-col justify-between h-36"
              >
                <div className="flex items-center justify-between w-full">
                  <div
                    className={`w-10 h-10 rounded-xl ${cat.color} flex items-center justify-center shadow-md group-hover:scale-110 transition-transform`}
                  >
                    <Icon className="w-5 h-5" />
                  </div>
                  <span className="text-[10px] font-bold text-slate-400 bg-slate-100 px-1.5 py-0.5 rounded-md">
                    {cat.badge}
                  </span>
                </div>

                <div>
                  <h3 className="font-extrabold text-slate-900 text-sm group-hover:text-blue-600 transition-colors">
                    {cat.title}
                  </h3>
                  <p className="text-[11px] text-slate-500 line-clamp-1 mt-0.5">{cat.desc}</p>
                </div>
              </button>
            );
          })}
        </div>
      </div>

      {/* Key Guarantees */}
      <div className="bg-slate-100 rounded-2xl p-4 border border-slate-200 grid grid-cols-1 sm:grid-cols-3 gap-3 text-xs">
        <div className="flex items-center gap-2.5 bg-white p-3 rounded-xl border border-slate-200/60 shadow-sm">
          <div className="w-8 h-8 rounded-lg bg-blue-50 text-blue-600 flex items-center justify-center font-bold">
            ⚡
          </div>
          <div>
            <p className="font-bold text-slate-900">Prix Libre dès 1 000 F</p>
            <p className="text-slate-500 text-[11px]">Négociation transparente</p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 bg-white p-3 rounded-xl border border-slate-200/60 shadow-sm">
          <div className="w-8 h-8 rounded-lg bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
            🔒
          </div>
          <div>
            <p className="font-bold text-slate-900">Sécurité Code OTP</p>
            <p className="text-slate-500 text-[11px]">Validation 4 chiffres</p>
          </div>
        </div>

        <div className="flex items-center gap-2.5 bg-white p-3 rounded-xl border border-slate-200/60 shadow-sm">
          <div className="w-8 h-8 rounded-lg bg-indigo-50 text-indigo-600 flex items-center justify-center font-bold">
            🛵
          </div>
          <div>
            <p className="font-bold text-slate-900">Livreurs Vérifiés</p>
            <p className="text-slate-500 text-[11px]">Identité & permis validés</p>
          </div>
        </div>
      </div>

      {/* Recent Completed Deliveries */}
      {recentCompleted.length > 0 && (
        <div className="space-y-3">
          <div className="flex items-center justify-between">
            <h2 className="font-black text-slate-900 text-base">Dernières livraisons effectuées</h2>
            <button
              onClick={() => onNavigate('history')}
              className="text-xs font-bold text-blue-600 hover:underline"
            >
              Historique complet
            </button>
          </div>

          <div className="space-y-2">
            {recentCompleted.map((del) => (
              <div
                key={del.id}
                onClick={() => onNavigate('tracking', del.id)}
                className="bg-white rounded-xl border border-slate-200 p-3.5 flex items-center justify-between gap-3 hover:bg-slate-50 transition-colors cursor-pointer text-xs"
              >
                <div className="flex items-center gap-3">
                  <div className="w-9 h-9 rounded-xl bg-emerald-50 text-emerald-600 flex items-center justify-center font-bold">
                    ✓
                  </div>
                  <div>
                    <p className="font-bold text-slate-900">{del.packageDescription}</p>
                    <p className="text-slate-500 text-[11px] truncate max-w-xs">
                      {del.destinationAddress}
                    </p>
                  </div>
                </div>

                <div className="text-right">
                  <p className="font-black text-slate-900">{formatCurrency(del.finalDeliveryPrice)}</p>
                  <span className="text-[10px] text-emerald-600 font-bold bg-emerald-50 px-1.5 py-0.5 rounded">
                    Livré
                  </span>
                </div>
              </div>
            ))}
          </div>
        </div>
      )}
    </div>
  );
};
