export type UserRole = 'client' | 'driver' | 'admin';

export type PackageSize = 'PETIT' | 'MOYEN' | 'VOLUMINEUX';

export type PaymentMethod = 'ORANGE_MONEY' | 'WAVE' | 'MTN_MOMO' | 'MOOV_MONEY' | 'CASH';

export type DriverVerificationStatus = 'NOT_SUBMITTED' | 'PENDING' | 'VERIFIED' | 'REJECTED';

export type DeliveryStatus =
  | 'SEARCHING_DRIVER'        // En recherche de livreur
  | 'DRIVER_COUNTER_OFFERED'   // Le livreur a fait une contre-offre (attente réponse client)
  | 'COUNTER_OFFER_ACCEPTED'   // Le client a accepté la contre-offre
  | 'COUNTER_OFFER_REJECTED'   // Le client a refusé la contre-offre
  | 'DRIVER_ASSIGNED'          // Livreur confirmé
  | 'DRIVER_ARRIVING'          // Livreur en route vers l'expéditeur
  | 'PACKAGE_PICKED_UP'        // Colis récupéré
  | 'IN_TRANSIT'               // En cours de livraison
  | 'DRIVER_ARRIVED'           // Livreur arrivé à destination
  | 'DELIVERED'                // Course terminée avec code OTP validé
  | 'CANCELLED';               // Course annulée

export interface LocationPoint {
  address: string;
  name?: string;
  lat: number;
  lng: number;
  city?: string;
}

export interface PricingBreakdown {
  basePriceXof: number;
  distancePriceXof: number;
  packageSurchargeXof: number;
  minPriceXof: number;
  recommendedPriceXof: number;
  attractivePriceXof: number;
  distanceKm: number;
  estimatedMinutes: number;
}

export interface DeliveryRequest {
  id: string;
  clientId: string;
  clientName: string;
  clientPhone: string;
  
  // Addresses
  pickupAddress: string;
  pickupLat: number;
  pickupLng: number;
  
  destinationAddress: string;
  destinationLat: number;
  destinationLng: number;
  
  // Recipient
  recipientName: string;
  recipientPhone: string;
  
  // Package
  packageDescription: string;
  packageSize: PackageSize;
  specialNotes?: string;
  
  // Negotiation & Pricing (Minimum 1000 FCFA rule)
  customerInitialOffer: number;  // Offre proposée par le client (>= 1000)
  driverCounterOffer?: number;    // Contre-offre unique du livreur (>= 1000)
  counterOfferDriverId?: string;
  counterOfferDriverName?: string;
  finalDeliveryPrice: number;    // Prix final retenu pour la course
  
  platformCommissionXof: number; // 10%
  driverEarningsXof: number;     // 90%
  customerTotalPaidXof: number;  // 100% + 10% = 110%
  
  // Payment
  paymentMethod: PaymentMethod;
  isPaid: boolean;
  
  // Status & Security
  status: DeliveryStatus;
  otpCode: string;               // 4 chiffres
  
  // Assigned Driver
  driverId?: string;
  driverName?: string;
  driverPhone?: string;
  driverPhoto?: string;
  driverVehicle?: string;
  driverRating?: number;
  currentDriverLat?: number;
  currentDriverLng?: number;
  
  // Timestamps
  createdAt: number;
  updatedAt: number;
  completedAt?: number;
}

export interface UserProfile {
  id: string;
  name: string;
  email: string;
  phone: string;
  role: UserRole;
  avatarUrl?: string;
  defaultCity?: string;
  savedAddresses?: Array<{ label: string; address: string; lat: number; lng: number }>;
}

export interface DriverProfile extends UserProfile {
  verificationStatus: DriverVerificationStatus;
  vehicleType: 'MOTO' | 'VOITURE' | 'TRICYCLE';
  vehiclePlate: string;
  walletBalanceXof: number;
  rating: number;
  totalDeliveries: number;
  isOnline: boolean;
  documents?: {
    cniUrl?: string;
    licenseUrl?: string;
    vehiclePhotoUrl?: string;
  };
}
