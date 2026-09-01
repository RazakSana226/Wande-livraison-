import React, { useEffect, useState } from 'react';
import { MapContainer, TileLayer, Marker, Popup, Polyline, useMap } from 'react-leaflet';
import L from 'leaflet';
import { Navigation, MapPin, Bike, Crosshair, Sparkles } from 'lucide-react';

interface InteractiveMapProps {
  pickupLat?: number;
  pickupLng?: number;
  destinationLat?: number;
  destinationLng?: number;
  driverLat?: number;
  driverLng?: number;
  pickupAddress?: string;
  destinationAddress?: string;
  driverName?: string;
  isInteractive?: boolean;
  onLocationSelect?: (lat: number, lng: number, type: 'pickup' | 'destination') => void;
  className?: string;
}

// Custom Leaflet DivIcons using styled SVG HTML
const createCustomIcon = (color: string, iconType: 'pickup' | 'destination' | 'driver') => {
  let innerHtml = '';
  if (iconType === 'pickup') {
    innerHtml = `<div style="background-color: #0066FF; color: white; width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 3px solid white; box-shadow: 0 4px 10px rgba(0,102,255,0.4); font-weight: bold; font-size: 13px;">A</div>`;
  } else if (iconType === 'destination') {
    innerHtml = `<div style="background-color: #16A34A; color: white; width: 34px; height: 34px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 3px solid white; box-shadow: 0 4px 10px rgba(22,163,74,0.4); font-weight: bold; font-size: 13px;">B</div>`;
  } else {
    innerHtml = `<div style="position: relative;">
      <div style="position: absolute; width: 44px; height: 44px; top: -6px; left: -6px; border-radius: 50%; background: rgba(0, 102, 255, 0.25); animation: ping 1.5s cubic-bezier(0, 0, 0.2, 1) infinite;"></div>
      <div style="background-color: #004AD7; color: white; width: 32px; height: 32px; border-radius: 50%; display: flex; align-items: center; justify-content: center; border: 2px solid white; box-shadow: 0 4px 12px rgba(0,0,0,0.3); font-size: 14px;">🛵</div>
    </div>`;
  }

  return L.divIcon({
    html: innerHtml,
    className: 'custom-leaflet-marker',
    iconSize: [34, 34],
    iconAnchor: [17, 17],
    popupAnchor: [0, -17],
  });
};

// Map View auto-adjuster
const ChangeMapView: React.FC<{ bounds: L.LatLngBoundsExpression | null; center: [number, number] }> = ({
  bounds,
  center,
}) => {
  const map = useMap();
  useEffect(() => {
    if (bounds) {
      map.fitBounds(bounds, { padding: [40, 40], maxZoom: 15 });
    } else {
      map.setView(center, 14);
    }
  }, [bounds, center, map]);
  return null;
};

export const InteractiveMap: React.FC<InteractiveMapProps> = ({
  pickupLat = 5.325,
  pickupLng = -4.018,
  destinationLat,
  destinationLng,
  driverLat,
  driverLng,
  pickupAddress = 'Point de récupération',
  destinationAddress = 'Point de livraison',
  driverName = 'Livreur WÀNDÉ',
  className = 'h-64 sm:h-80 w-full',
}) => {
  const [currentGps, setCurrentGps] = useState<[number, number] | null>(null);
  const [isLocating, setIsLocating] = useState(false);

  const defaultCenter: [number, number] = [pickupLat, pickupLng];

  // Calculate Bounds if multiple markers exist
  const points: [number, number][] = [[pickupLat, pickupLng]];
  if (destinationLat && destinationLng) points.push([destinationLat, destinationLng]);
  if (driverLat && driverLng) points.push([driverLat, driverLng]);

  const bounds: L.LatLngBoundsExpression | null =
    points.length > 1
      ? L.latLngBounds(points.map(([lat, lng]) => L.latLng(lat, lng)))
      : null;

  const handleGetCurrentLocation = () => {
    if (!navigator.geolocation) {
      alert('La géolocalisation n’est pas supportée par votre navigateur.');
      return;
    }
    setIsLocating(true);
    navigator.geolocation.getCurrentPosition(
      (pos) => {
        setIsLocating(false);
        setCurrentGps([pos.coords.latitude, pos.coords.longitude]);
      },
      (err) => {
        setIsLocating(false);
        console.warn('Geolocation error:', err);
        // Fallback default coordinates (Abidjan Plateau)
        setCurrentGps([5.325, -4.018]);
      },
      { enableHighAccuracy: true, timeout: 7000 }
    );
  };

  return (
    <div className={`relative rounded-2xl overflow-hidden border border-slate-200 shadow-card bg-slate-100 ${className}`}>
      <MapContainer
        center={defaultCenter}
        zoom={13}
        scrollWheelZoom={false}
        className="w-full h-full"
      >
        <TileLayer
          attribution='&copy; <a href="https://www.openstreetmap.org/copyright">OpenStreetMap</a> contributors'
          url="https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png"
        />

        <ChangeMapView bounds={bounds} center={currentGps || defaultCenter} />

        {/* Pickup Marker (A) */}
        {pickupLat && pickupLng && (
          <Marker position={[pickupLat, pickupLng]} icon={createCustomIcon('#0066FF', 'pickup')}>
            <Popup>
              <div className="p-1">
                <span className="text-[10px] font-black uppercase text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                  Point A (Départ)
                </span>
                <p className="font-bold text-xs mt-1 text-slate-800">{pickupAddress}</p>
              </div>
            </Popup>
          </Marker>
        )}

        {/* Destination Marker (B) */}
        {destinationLat && destinationLng && (
          <Marker position={[destinationLat, destinationLng]} icon={createCustomIcon('#16A34A', 'destination')}>
            <Popup>
              <div className="p-1">
                <span className="text-[10px] font-black uppercase text-emerald-600 bg-emerald-50 px-1.5 py-0.5 rounded">
                  Point B (Arrivée)
                </span>
                <p className="font-bold text-xs mt-1 text-slate-800">{destinationAddress}</p>
              </div>
            </Popup>
          </Marker>
        )}

        {/* Driver Marker (🛵) */}
        {driverLat && driverLng && (
          <Marker position={[driverLat, driverLng]} icon={createCustomIcon('#004AD7', 'driver')}>
            <Popup>
              <div className="p-1">
                <span className="text-[10px] font-black uppercase text-blue-600 bg-blue-50 px-1.5 py-0.5 rounded">
                  Livreur en direct
                </span>
                <p className="font-bold text-xs mt-1 text-slate-800">{driverName}</p>
              </div>
            </Popup>
          </Marker>
        )}

        {/* Trajectory Polyline */}
        {pickupLat && pickupLng && destinationLat && destinationLng && (
          <Polyline
            positions={[
              [pickupLat, pickupLng],
              ...(driverLat && driverLng ? [[driverLat, driverLng] as [number, number]] : []),
              [destinationLat, destinationLng],
            ]}
            color="#0066FF"
            weight={4}
            opacity={0.75}
            dashArray="6, 8"
          />
        )}
      </MapContainer>

      {/* Floating Geolocation Button */}
      <button
        onClick={handleGetCurrentLocation}
        disabled={isLocating}
        title="Ma position actuelle"
        className="absolute bottom-3 right-3 z-[400] bg-white/95 backdrop-blur-sm text-slate-700 hover:text-blue-600 p-2.5 rounded-xl shadow-md border border-slate-200 flex items-center gap-1.5 text-xs font-bold transition-all active:scale-95"
      >
        <Crosshair className={`w-4 h-4 text-blue-600 ${isLocating ? 'animate-spin' : ''}`} />
        <span className="hidden sm:inline">Ma position</span>
      </button>

      {/* Route Info Pill */}
      {destinationLat && (
        <div className="absolute top-3 left-3 z-[400] bg-white/95 backdrop-blur-sm px-3 py-1.5 rounded-xl shadow-md border border-slate-200 flex items-center gap-2">
          <span className="w-2 h-2 rounded-full bg-emerald-500 animate-pulse"></span>
          <span className="text-xs font-bold text-slate-800">Itinéraire optimisé WÀNDÉ</span>
        </div>
      )}
    </div>
  );
};
