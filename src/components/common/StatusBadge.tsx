import React from 'react';
import { DeliveryStatus } from '../../types';
import { 
  Clock, 
  CheckCircle2, 
  Bike, 
  Package, 
  MapPin, 
  TrendingUp, 
  XCircle, 
  UserCheck, 
  Radio
} from 'lucide-react';

interface StatusBadgeProps {
  status: DeliveryStatus;
  className?: string;
  size?: 'sm' | 'md';
}

export const StatusBadge: React.FC<StatusBadgeProps> = ({ status, className = '', size = 'md' }) => {
  const sizeClasses = size === 'sm' ? 'px-2 py-0.5 text-xs' : 'px-2.5 py-1 text-xs';

  switch (status) {
    case 'SEARCHING_DRIVER':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-amber-50 text-amber-700 border border-amber-200 ${sizeClasses} ${className}`}>
          <Radio className="w-3.5 h-3.5 animate-pulse text-amber-600" />
          <span>Recherche livreur</span>
        </span>
      );

    case 'DRIVER_COUNTER_OFFERED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-yellow-50 text-yellow-800 border border-yellow-300 animate-pulse ${sizeClasses} ${className}`}>
          <TrendingUp className="w-3.5 h-3.5 text-yellow-600" />
          <span>Contre-offre livreur</span>
        </span>
      );

    case 'COUNTER_OFFER_ACCEPTED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-emerald-50 text-emerald-700 border border-emerald-200 ${sizeClasses} ${className}`}>
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          <span>Offre acceptée</span>
        </span>
      );

    case 'COUNTER_OFFER_REJECTED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-rose-50 text-rose-700 border border-rose-200 ${sizeClasses} ${className}`}>
          <XCircle className="w-3.5 h-3.5 text-rose-600" />
          <span>Contre-offre déclinée</span>
        </span>
      );

    case 'DRIVER_ASSIGNED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-blue-50 text-blue-700 border border-blue-200 ${sizeClasses} ${className}`}>
          <UserCheck className="w-3.5 h-3.5 text-blue-600" />
          <span>Livreur assigné</span>
        </span>
      );

    case 'DRIVER_ARRIVING':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-indigo-50 text-indigo-700 border border-indigo-200 ${sizeClasses} ${className}`}>
          <Bike className="w-3.5 h-3.5 text-indigo-600 animate-bounce" />
          <span>Livreur en approche</span>
        </span>
      );

    case 'PACKAGE_PICKED_UP':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-purple-50 text-purple-700 border border-purple-200 ${sizeClasses} ${className}`}>
          <Package className="w-3.5 h-3.5 text-purple-600" />
          <span>Colis récupéré</span>
        </span>
      );

    case 'IN_TRANSIT':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-cyan-50 text-cyan-700 border border-cyan-200 ${sizeClasses} ${className}`}>
          <Bike className="w-3.5 h-3.5 text-cyan-600" />
          <span>En cours de livraison</span>
        </span>
      );

    case 'DRIVER_ARRIVED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-orange-50 text-orange-700 border border-orange-200 ${sizeClasses} ${className}`}>
          <MapPin className="w-3.5 h-3.5 text-orange-600" />
          <span>Livreur sur place</span>
        </span>
      );

    case 'DELIVERED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-emerald-100 text-emerald-800 border border-emerald-300 ${sizeClasses} ${className}`}>
          <CheckCircle2 className="w-3.5 h-3.5 text-emerald-600" />
          <span>Livré avec succès</span>
        </span>
      );

    case 'CANCELLED':
      return (
        <span className={`inline-flex items-center gap-1.5 font-bold rounded-full bg-slate-100 text-slate-700 border border-slate-200 ${sizeClasses} ${className}`}>
          <Clock className="w-3.5 h-3.5 text-slate-500" />
          <span>Annulée</span>
        </span>
      );

    default:
      return null;
  }
};
