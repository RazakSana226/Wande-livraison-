import React, { useEffect } from 'react';
import { MapContainer, TileLayer, Marker, Popup, useMap, Polyline } from 'react-leaflet';
import L from 'leaflet';

interface DeliveryMapProps {
  pickupLat: number;
  pickupLng: number;
  pickupAddress?: string;
  destinationLat: number;
  destinationLng: number;
  destinationAddress?: string;
  driverLat?: number;
  driverLng?: number;
  driverName?: string;
  height?: string;
  className?: string;
  interactive?: boolean;
}

// Custom icons using SVG data URIs
const createCustomIcon = (color: string, label: string, emoji: string) => {
  return L.divIcon({
    className: 'custom-map-marker',
    html: `
      <div style="
        display: flex;
        flex-direction: column;
        align-items: center;
        transform: translate(-50%, -100%);
      ">
        <div style="
          background-color: ${color};
          color: white;
          width: 36px;
          height: 36px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          box-shadow: 0 4px 12px rgba(0,0,0,0.3);
          border: 3px solid white;
          font-size: 16px;
        ">
          ${emoji}
        </div>
        <div style="
          background-color: #0F172A;
          color: white;
          padding: 2px 6px;
          border-radius: 4px;
          font-size: 10px;
          font-weight: 700;
          white-space: nowrap;
          margin-top: 2px;
          box-shadow: 0 2px 4px rgba(0,0,0,0.2);
        ">
          ${label}
        </div>
      </div>
    `,
    iconSize: [36, 36],
    iconAnchor: [18, 36],
  });
};

const pickupIcon = createCustomIcon('#16A34A', 'Départ', '📦');
const destIcon = createCustomIcon('#EF4444', 'Arrivée', '📍');
const driverIcon = createCustomIcon('#0066FF', 'Livreur', '🛵');

// Helper to auto-fit map bounds
const MapBoundsAdjuster: React.FC<{
  points: [number, number][];
}> = ({ points }) => {
  const map = useMap();

  useEffect(() => {
    if (points.length > 0) {
      const bounds = L.latLngBounds(points);
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
    }
  }, [map, points]);

  return null;
};

export const DeliveryMap: React.FC<DeliveryMapProps> = ({
  pickupLat,
  pickupLng,
  pickupAddress = 'Point de départ',
  destinationLat,
  destinationLng,
  destinationAddress = 'Destination',
  driverLat,
  driverLng,
  driverName = 'Livreur WÀNDÉ',
  height = '240px',
  className = '',
  interactive = true,
}) => {
  const centerLat = (pickupLat + destinationLat) / 2 || 5.348;
  const centerLng = (pickupLng + destinationLng) / 2 || -4.015;

  const points: [number, number][] = [
    [pickupLat, pickupLng],
    [destinationLat, destinationLng],
  ];

  if (driverLat && driverLng) {
    points.push([driverLat, driverLng]);
  }

  const polylineCoords: [number, number][] = [
    [pickupLat, pickupLng],
    [destinationLat, destinationLng],
  ];

  return (
    <div
      style={{ height }}
      className={`relative w-full rounded-2xl overflow-hidden border border-slate-200 shadow-inner bg-slate-100 ${className}`}
    >
      <MapContainer
        center={[centerLat, centerLng]}
        zoom={13}
        scrollWheelZoom={interactive}
        dragging={interactive}
        zoomControl={interactive}
        attributionControl={false}
        className="h-full w-full"
      >
        <TileLayer
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
          maxZoom={19}
        />

        <MapBoundsAdjuster points={points} />

        {/* Route Line */}
        <Polyline
          positions={polylineCoords}
          color="#0066FF"
          weight={4}
          opacity={0.8}
          dashArray="8, 8"
        />

        {/* Pickup Marker */}
        <Marker position={[pickupLat, pickupLng]} icon={pickupIcon}>
          <Popup>
            <div className="text-xs">
              <strong className="text-emerald-700">Départ / Expéditeur</strong>
              <p className="mt-1 text-slate-600">{pickupAddress}</p>
            </div>
          </Popup>
        </Marker>

        {/* Destination Marker */}
        <Marker position={[destinationLat, destinationLng]} icon={destIcon}>
          <Popup>
            <div className="text-xs">
              <strong className="text-rose-700">Arrivée / Destinataire</strong>
              <p className="mt-1 text-slate-600">{destinationAddress}</p>
            </div>
          </Popup>
        </Marker>

        {/* Driver Live Marker */}
        {driverLat && driverLng && (
          <Marker position={[driverLat, driverLng]} icon={driverIcon}>
            <Popup>
              <div className="text-xs">
                <strong className="text-blue-700">{driverName}</strong>
                <p className="mt-1 text-slate-600">Position en temps réel</p>
              </div>
            </Popup>
          </Marker>
        )}
      </MapContainer>
    </div>
  );
};
