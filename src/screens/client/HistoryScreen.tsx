import React, { useEffect, useState } from 'react';
import { useAuth } from '../../context/AuthContext';
import { DeliveryRequest } from '../../types';
import { storageService } from '../../services/storageService';
import { StatusBadge } from '../../components/common/StatusBadge';
import { formatCurrency } from '../../services/pricingService';
import {
  History,
  FileText,
  Search,
  ChevronRight,
  Download,
  Calendar,
  X,
  Package,
  CheckCircle2,
  Bike,
  Receipt
} from 'lucide-react';

interface HistoryScreenProps {
  onNavigate: (screen: string, deliveryId?: string) => void;
}

export const HistoryScreen: React.FC<HistoryScreenProps> = ({ onNavigate }) => {
  const { activeRole } = useAuth();
  const [deliveries, setDeliveries] = useState<DeliveryRequest[]>([]);
  const [filter, setFilter] = useState<'all' | 'active' | 'delivered'>('all');
  const [selectedReceipt, setSelectedReceipt] = useState<DeliveryRequest | null>(null);

  useEffect(() => {
    const unsub = storageService.subscribe((list) => {
      setDeliveries(list);
    });
    return () => unsub();
  }, []);

  const filtered = deliveries.filter((d) => {
    if (filter === 'active') return d.status !== 'DELIVERED' && d.status !== 'CANCELLED';
    if (filter === 'delivered') return d.status === 'DELIVERED';
    return true;
  });

  return (
    <div className="max-w-4xl mx-auto px-4 py-4 space-y-5 pb-24">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl sm:text-2xl font-black text-slate-900">
            Historique des Livraisons
          </h1>
          <p className="text-xs text-slate-500 mt-0.5">
            Retrouvez tous vos colis, reçus de paiement et suivis
          </p>
        </div>
        <span className="bg-slate-100 text-slate-700 text-xs font-black px-3 py-1.5 rounded-xl border border-slate-200">
          {deliveries.length} courses
        </span>
      </div>

      {/* Filter Tabs */}
      <div className="flex gap-2 bg-slate-100 p-1 rounded-2xl border border-slate-200">
        {[
          { id: 'all', label: 'Toutes les courses' },
          { id: 'active', label: 'En cours' },
          { id: 'delivered', label: 'Livrées avec succès' },
        ].map((tab) => (
          <button
            key={tab.id}
            onClick={() => setFilter(tab.id as any)}
            className={`flex-1 py-2 rounded-xl text-xs font-bold transition-all ${
              filter === tab.id
                ? 'bg-white text-blue-600 shadow-sm'
                : 'text-slate-600 hover:text-slate-900'
            }`}
          >
            {tab.label}
          </button>
        ))}
      </div>

      {/* List */}
      {filtered.length === 0 ? (
        <div className="bg-white rounded-2xl border border-slate-200 p-8 text-center space-y-3">
          <Package className="w-12 h-12 text-slate-300 mx-auto" />
          <h3 className="font-bold text-slate-800 text-sm">Aucune course trouvée</h3>
          <p className="text-xs text-slate-500">
            Vous n'avez pas encore de livraisons dans cette catégorie.
          </p>
        </div>
      ) : (
        <div className="space-y-3">
          {filtered.map((del) => (
            <div
              key={del.id}
              className="bg-white rounded-2xl border border-slate-200 p-4 sm:p-5 shadow-card hover:border-blue-300 transition-all space-y-3"
            >
              {/* Header */}
              <div className="flex items-center justify-between">
                <div className="flex items-center gap-2">
                  <span className="text-xs font-black text-slate-400">#{del.id}</span>
                  <StatusBadge status={del.status} size="sm" />
                </div>

                <div className="text-right">
                  <span className="text-sm sm:text-base font-black text-blue-600">
                    {formatCurrency(del.finalDeliveryPrice)}
                  </span>
                </div>
              </div>

              {/* Description */}
              <div className="flex items-center gap-2">
                <span className="text-xs font-extrabold text-slate-900">
                  {del.packageDescription}
                </span>
                <span className="text-[10px] text-slate-400 font-semibold bg-slate-100 px-1.5 py-0.5 rounded">
                  {del.packageSize}
                </span>
              </div>

              {/* Itinerary */}
              <div className="space-y-1.5 text-xs text-slate-600">
                <p className="truncate">
                  📍 <strong>Départ :</strong> {del.pickupAddress}
                </p>
                <p className="truncate">
                  📍 <strong>Arrivée :</strong> {del.destinationAddress}
                </p>
              </div>

              {/* Footer Actions */}
              <div className="flex items-center justify-between pt-2 border-t border-slate-100 text-xs">
                <span className="text-[11px] text-slate-400 font-medium">
                  {new Date(del.createdAt).toLocaleDateString('fr-FR', {
                    day: 'numeric',
                    month: 'short',
                    hour: '2-digit',
                    minute: '2-digit',
                  })}
                </span>

                <div className="flex items-center gap-2">
                  <button
                    onClick={() => setSelectedReceipt(del)}
                    className="flex items-center gap-1 text-slate-700 hover:text-blue-600 bg-slate-100 hover:bg-blue-50 px-2.5 py-1.5 rounded-lg font-bold text-xs transition-colors"
                  >
                    <Receipt className="w-3.5 h-3.5" />
                    <span>Reçu</span>
                  </button>

                  <button
                    onClick={() => onNavigate('tracking', del.id)}
                    className="flex items-center gap-1 bg-blue-600 hover:bg-blue-700 text-white px-3 py-1.5 rounded-lg font-black text-xs transition-all shadow-sm"
                  >
                    <span>Suivi</span>
                    <ChevronRight className="w-3.5 h-3.5" />
                  </button>
                </div>
              </div>
            </div>
          ))}
        </div>
      )}

      {/* Digital Receipt Modal */}
      {selectedReceipt && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4 bg-slate-900/60 backdrop-blur-sm animate-fade-in">
          <div className="bg-white rounded-3xl max-w-md w-full p-6 shadow-2xl border border-slate-200 animate-slide-up space-y-4">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-slate-100 pb-3">
              <div className="flex items-center gap-2">
                <div className="w-9 h-9 rounded-xl bg-blue-600 text-white font-black flex items-center justify-center">
                  W
                </div>
                <div>
                  <h3 className="font-extrabold text-slate-900 text-base">
                    Reçu Digital de Livraison
                  </h3>
                  <p className="text-xs text-slate-400">Course #{selectedReceipt.id}</p>
                </div>
              </div>
              <button
                onClick={() => setSelectedReceipt(null)}
                className="p-1 rounded-lg text-slate-400 hover:text-slate-700"
              >
                <X className="w-5 h-5" />
              </button>
            </div>

            {/* Receipt Details */}
            <div className="bg-slate-50 rounded-2xl p-4 border border-slate-200 text-xs space-y-2.5">
              <div className="flex justify-between">
                <span className="text-slate-500">Date d'expédition :</span>
                <span className="font-bold text-slate-800">
                  {new Date(selectedReceipt.createdAt).toLocaleString('fr-FR')}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Expéditeur :</span>
                <span className="font-bold text-slate-800">{selectedReceipt.clientName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Destinataire :</span>
                <span className="font-bold text-slate-800">{selectedReceipt.recipientName}</span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Code OTP :</span>
                <span className="font-black text-slate-900 tracking-wider">
                  {selectedReceipt.otpCode}
                </span>
              </div>
              <div className="flex justify-between">
                <span className="text-slate-500">Paiement :</span>
                <span className="font-bold text-slate-800">{selectedReceipt.paymentMethod}</span>
              </div>
            </div>

            {/* Amount Breakdown */}
            <div className="space-y-1.5 text-xs">
              <div className="flex justify-between text-slate-600">
                <span>Prix de la course :</span>
                <span className="font-bold text-slate-900">
                  {formatCurrency(selectedReceipt.finalDeliveryPrice)}
                </span>
              </div>
              <div className="flex justify-between text-slate-600">
                <span>Commission WÀNDÉ (10%) :</span>
                <span className="font-bold text-slate-900">
                  {formatCurrency(selectedReceipt.platformCommissionXof)}
                </span>
              </div>
              <div className="border-t border-slate-200 pt-2 flex justify-between items-center">
                <span className="font-black text-slate-900 uppercase">Total payé :</span>
                <span className="text-lg font-black text-blue-600">
                  {formatCurrency(selectedReceipt.customerTotalPaidXof)}
                </span>
              </div>
            </div>

            {/* Print / Close */}
            <button
              onClick={() => {
                window.print();
              }}
              className="w-full bg-slate-900 hover:bg-slate-800 text-white font-bold py-3 rounded-xl text-xs flex items-center justify-center gap-2"
            >
              <Download className="w-4 h-4" />
              <span>Télécharger ou Imprimer le reçu</span>
            </button>
          </div>
        </div>
      )}
    </div>
  );
};
