import { DeliveryRequest, DeliveryStatus, DriverProfile, PackageSize, PaymentMethod, UserProfile } from '../types';
import { db, isFirebaseConfigured } from './firebase';
import { collection, doc, setDoc, getDocs, onSnapshot, query, orderBy } from 'firebase/firestore';
import { calculateBreakdown, estimatePricing, calculateDistanceKm } from './pricingService';

const LOCAL_STORAGE_KEY = 'wande_deliveries_v2';
const DRIVER_PROFILE_KEY = 'wande_driver_profile_v2';
const EVENT_DELIVERY_CHANGED = 'wande:deliveries_changed';

// Sample initial data for immediate discovery & demo
const INITIAL_DEMO_DELIVERIES: DeliveryRequest[] = [
  {
    id: 'WND-84920',
    clientId: 'client_demo_1',
    clientName: 'Awa Traoré',
    clientPhone: '+225 07 88 12 34 56',
    pickupAddress: 'Plateau, Rue du Commerce, Abidjan',
    pickupLat: 5.325,
    pickupLng: -4.018,
    destinationAddress: 'Cocody Deux-Plateaux Vallon, Abidjan',
    destinationLat: 5.365,
    destinationLng: -3.992,
    recipientName: 'Kouamé Jean',
    recipientPhone: '+225 05 44 98 76 12',
    packageDescription: 'Documents confidentiels & clés de bureau',
    packageSize: 'PETIT',
    specialNotes: 'Sonner au portail noir en arrivant.',
    customerInitialOffer: 1500,
    finalDeliveryPrice: 1500,
    platformCommissionXof: 150,
    driverEarningsXof: 1350,
    customerTotalPaidXof: 1650,
    paymentMethod: 'ORANGE_MONEY',
    isPaid: true,
    status: 'SEARCHING_DRIVER',
    otpCode: '4829',
    createdAt: Date.now() - 1000 * 60 * 12,
    updatedAt: Date.now() - 1000 * 60 * 12,
  },
  {
    id: 'WND-73105',
    clientId: 'client_demo_2',
    clientName: 'Mamadou Diallo',
    clientPhone: '+225 01 23 45 67 89',
    pickupAddress: 'Zone 4, Boulevard de Marseille, Marcory',
    pickupLat: 5.298,
    pickupLng: -3.985,
    destinationAddress: 'Yopougon Sideci, Abidjan',
    destinationLat: 5.342,
    destinationLng: -4.078,
    recipientName: 'Fatou Sow',
    recipientPhone: '+225 07 11 22 33 44',
    packageDescription: 'Colis de vêtements & chaussures express',
    packageSize: 'MOYEN',
    customerInitialOffer: 2000,
    driverCounterOffer: 2500,
    counterOfferDriverId: 'driver_demo_1',
    counterOfferDriverName: 'Bakary Koné',
    finalDeliveryPrice: 2000,
    platformCommissionXof: 200,
    driverEarningsXof: 1800,
    customerTotalPaidXof: 2200,
    paymentMethod: 'WAVE',
    isPaid: true,
    status: 'DRIVER_COUNTER_OFFERED',
    otpCode: '7194',
    createdAt: Date.now() - 1000 * 60 * 45,
    updatedAt: Date.now() - 1000 * 60 * 10,
  },
  {
    id: 'WND-61984',
    clientId: 'client_demo_1',
    clientName: 'Awa Traoré',
    clientPhone: '+225 07 88 12 34 56',
    pickupAddress: 'Treichville Avenue 8, Abidjan',
    pickupLat: 5.305,
    pickupLng: -4.008,
    destinationAddress: 'Koumassi Remblais, Abidjan',
    destinationLat: 5.301,
    destinationLng: -3.955,
    recipientName: 'Alain Bamba',
    recipientPhone: '+225 05 88 77 66 55',
    packageDescription: 'Repas traiteur chaud',
    packageSize: 'PETIT',
    customerInitialOffer: 1500,
    finalDeliveryPrice: 1500,
    platformCommissionXof: 150,
    driverEarningsXof: 1350,
    customerTotalPaidXof: 1650,
    paymentMethod: 'CASH',
    isPaid: true,
    status: 'DELIVERED',
    otpCode: '3310',
    driverId: 'driver_demo_1',
    driverName: 'Bakary Koné',
    driverPhone: '+225 07 99 88 77 66',
    driverPhoto: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
    driverVehicle: 'Yamaha YBR 125 (Bleu)',
    driverRating: 4.9,
    createdAt: Date.now() - 1000 * 60 * 180,
    updatedAt: Date.now() - 1000 * 60 * 90,
    completedAt: Date.now() - 1000 * 60 * 90,
  }
];

export class StorageService {
  private static instance: StorageService;
  private deliveries: DeliveryRequest[] = [];
  private driverProfile: DriverProfile;

  private constructor() {
    this.driverProfile = this.loadDriverProfile();
    this.deliveries = this.loadDeliveries();
    this.initFirestoreSync();
  }

  public static getInstance(): StorageService {
    if (!StorageService.instance) {
      StorageService.instance = new StorageService();
    }
    return StorageService.instance;
  }

  private loadDeliveries(): DeliveryRequest[] {
    try {
      const raw = localStorage.getItem(LOCAL_STORAGE_KEY);
      if (raw) {
        return JSON.parse(raw);
      }
    } catch (e) {
      console.warn('Could not parse local deliveries', e);
    }
    this.saveDeliveriesLocal(INITIAL_DEMO_DELIVERIES);
    return INITIAL_DEMO_DELIVERIES;
  }

  private saveDeliveriesLocal(data: DeliveryRequest[]) {
    this.deliveries = data;
    try {
      localStorage.setItem(LOCAL_STORAGE_KEY, JSON.stringify(data));
      window.dispatchEvent(new CustomEvent(EVENT_DELIVERY_CHANGED, { detail: data }));
    } catch (e) {
      console.error('Error saving deliveries to local storage', e);
    }
  }

  private loadDriverProfile(): DriverProfile {
    try {
      const raw = localStorage.getItem(DRIVER_PROFILE_KEY);
      if (raw) {
        return JSON.parse(raw);
      }
    } catch (e) {
      console.warn('Could not parse driver profile', e);
    }
    const defaultProfile: DriverProfile = {
      id: 'driver_demo_1',
      name: 'Bakary Koné',
      email: 'bakary.livreur@wande.ci',
      phone: '+225 07 99 88 77 66',
      role: 'driver',
      avatarUrl: 'https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150&auto=format&fit=crop&q=80',
      verificationStatus: 'VERIFIED',
      vehicleType: 'MOTO',
      vehiclePlate: 'CI-8492-AB01',
      walletBalanceXof: 24750,
      rating: 4.9,
      totalDeliveries: 42,
      isOnline: true,
    };
    localStorage.setItem(DRIVER_PROFILE_KEY, JSON.stringify(defaultProfile));
    return defaultProfile;
  }

  public saveDriverProfile(profile: DriverProfile) {
    this.driverProfile = profile;
    localStorage.setItem(DRIVER_PROFILE_KEY, JSON.stringify(profile));
  }

  public getDriverProfile(): DriverProfile {
    return this.driverProfile;
  }

  private initFirestoreSync() {
    if (isFirebaseConfigured() && db) {
      try {
        const q = query(collection(db, 'deliveries'), orderBy('createdAt', 'desc'));
        onSnapshot(q, (snapshot) => {
          if (!snapshot.empty) {
            const remoteDeliveries: DeliveryRequest[] = [];
            snapshot.forEach((d) => remoteDeliveries.push(d.data() as DeliveryRequest));
            this.saveDeliveriesLocal(remoteDeliveries);
          }
        }, (error) => {
          console.warn('[Firestore] Sync warning, running in local fallback:', error);
        });
      } catch (err) {
        console.warn('[Firestore] Exception in snapshot listener:', err);
      }
    }
  }

  // --- CRUD OPERATIONS ---

  public getDeliveries(): DeliveryRequest[] {
    return [...this.deliveries];
  }

  public getDeliveryById(id: string): DeliveryRequest | undefined {
    return this.deliveries.find((d) => d.id === id);
  }

  public async createDelivery(params: {
    clientId: string;
    clientName: string;
    clientPhone: string;
    pickupAddress: string;
    pickupLat: number;
    pickupLng: number;
    destinationAddress: string;
    destinationLat: number;
    destinationLng: number;
    recipientName: string;
    recipientPhone: string;
    packageDescription: string;
    packageSize: PackageSize;
    specialNotes?: string;
    proposedPriceXof: number;
    paymentMethod: PaymentMethod;
  }): Promise<DeliveryRequest> {
    const validPrice = Math.max(1000, params.proposedPriceXof);
    const breakdown = calculateBreakdown(validPrice);
    const otpCode = Math.floor(1000 + Math.random() * 9000).toString();
    const id = `WND-${Math.floor(10000 + Math.random() * 90000)}`;

    const newDelivery: DeliveryRequest = {
      id,
      clientId: params.clientId,
      clientName: params.clientName,
      clientPhone: params.clientPhone,
      pickupAddress: params.pickupAddress,
      pickupLat: params.pickupLat,
      pickupLng: params.pickupLng,
      destinationAddress: params.destinationAddress,
      destinationLat: params.destinationLat,
      destinationLng: params.destinationLng,
      recipientName: params.recipientName,
      recipientPhone: params.recipientPhone,
      packageDescription: params.packageDescription,
      packageSize: params.packageSize,
      specialNotes: params.specialNotes,
      customerInitialOffer: validPrice,
      finalDeliveryPrice: validPrice,
      platformCommissionXof: breakdown.platformFee,
      driverEarningsXof: breakdown.driverEarnings,
      customerTotalPaidXof: breakdown.totalCustomerPaid,
      paymentMethod: params.paymentMethod,
      isPaid: true,
      status: 'SEARCHING_DRIVER',
      otpCode,
      createdAt: Date.now(),
      updatedAt: Date.now(),
    };

    const updated = [newDelivery, ...this.deliveries];
    this.saveDeliveriesLocal(updated);

    if (isFirebaseConfigured() && db) {
      try {
        await setDoc(doc(db, 'deliveries', newDelivery.id), newDelivery);
      } catch (err) {
        console.warn('Failed to sync new delivery to Firestore:', err);
      }
    }

    return newDelivery;
  }

  /**
   * Driver directly accepts customer offer at current price
   */
  public async driverAcceptDelivery(deliveryId: string, driver: DriverProfile): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: 'DRIVER_ASSIGNED',
      driverId: driver.id,
      driverName: driver.name,
      driverPhone: driver.phone,
      driverPhoto: driver.avatarUrl,
      driverVehicle: `${driver.vehicleType} - ${driver.vehiclePlate}`,
      driverRating: driver.rating,
      currentDriverLat: delivery.pickupLat + 0.005,
      currentDriverLng: delivery.pickupLng - 0.005,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Driver submits a single counter-offer (>= 1000 FCFA)
   */
  public async driverSubmitCounterOffer(deliveryId: string, driver: DriverProfile, counterPriceXof: number): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');
    if (counterPriceXof < 1000) throw new Error('Le prix minimum est de 1 000 FCFA');

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: 'DRIVER_COUNTER_OFFERED',
      driverCounterOffer: counterPriceXof,
      counterOfferDriverId: driver.id,
      counterOfferDriverName: driver.name,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Customer accepts driver's counter-offer
   */
  public async clientAcceptCounterOffer(deliveryId: string): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');
    if (!delivery.driverCounterOffer || !delivery.counterOfferDriverId) {
      throw new Error('Aucune contre-offre active');
    }

    const newPrice = delivery.driverCounterOffer;
    const breakdown = calculateBreakdown(newPrice);

    const driver = this.driverProfile; // Or looked up driver

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: 'DRIVER_ASSIGNED',
      finalDeliveryPrice: newPrice,
      platformCommissionXof: breakdown.platformFee,
      driverEarningsXof: breakdown.driverEarnings,
      customerTotalPaidXof: breakdown.totalCustomerPaid,
      driverId: delivery.counterOfferDriverId,
      driverName: delivery.counterOfferDriverName || driver.name,
      driverPhone: driver.phone,
      driverPhoto: driver.avatarUrl,
      driverVehicle: `${driver.vehicleType} - ${driver.vehiclePlate}`,
      driverRating: driver.rating,
      currentDriverLat: delivery.pickupLat + 0.005,
      currentDriverLng: delivery.pickupLng - 0.005,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Customer rejects driver's counter-offer
   */
  public async clientRejectCounterOffer(deliveryId: string): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: 'COUNTER_OFFER_REJECTED',
      driverCounterOffer: undefined,
      counterOfferDriverId: undefined,
      counterOfferDriverName: undefined,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Customer boosts or updates their offer price
   */
  public async clientUpdateOffer(deliveryId: string, newOfferXof: number): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');
    if (newOfferXof < 1000) throw new Error('Le prix minimum est de 1 000 FCFA');

    const breakdown = calculateBreakdown(newOfferXof);

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      customerInitialOffer: newOfferXof,
      finalDeliveryPrice: newOfferXof,
      platformCommissionXof: breakdown.platformFee,
      driverEarningsXof: breakdown.driverEarnings,
      customerTotalPaidXof: breakdown.totalCustomerPaid,
      status: 'SEARCHING_DRIVER',
      driverCounterOffer: undefined,
      counterOfferDriverId: undefined,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Driver updates progress status
   */
  public async driverUpdateStatus(deliveryId: string, nextStatus: DeliveryStatus): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');

    let driverLat = delivery.currentDriverLat || delivery.pickupLat;
    let driverLng = delivery.currentDriverLng || delivery.pickupLng;

    if (nextStatus === 'PACKAGE_PICKED_UP' || nextStatus === 'IN_TRANSIT') {
      driverLat = (delivery.pickupLat + delivery.destinationLat) / 2;
      driverLng = (delivery.pickupLng + delivery.destinationLng) / 2;
    } else if (nextStatus === 'DRIVER_ARRIVED') {
      driverLat = delivery.destinationLat;
      driverLng = delivery.destinationLng;
    }

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: nextStatus,
      currentDriverLat: driverLat,
      currentDriverLng: driverLng,
      updatedAt: Date.now(),
    };

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  /**
   * Driver completes delivery by verifying 4-digit OTP
   */
  public async driverCompleteWithOtp(deliveryId: string, enteredOtp: string): Promise<DeliveryRequest> {
    const delivery = this.getDeliveryById(deliveryId);
    if (!delivery) throw new Error('Livraison introuvable');

    if (delivery.otpCode.trim() !== enteredOtp.trim()) {
      throw new Error('Code OTP incorrect. Veuillez demander le code à 4 chiffres au destinataire.');
    }

    const updatedDelivery: DeliveryRequest = {
      ...delivery,
      status: 'DELIVERED',
      completedAt: Date.now(),
      updatedAt: Date.now(),
    };

    // Credit driver wallet
    const driver = this.getDriverProfile();
    driver.walletBalanceXof += delivery.driverEarningsXof;
    driver.totalDeliveries += 1;
    this.saveDriverProfile(driver);

    this.updateDelivery(updatedDelivery);
    return updatedDelivery;
  }

  public subscribe(callback: (deliveries: DeliveryRequest[]) => void): () => void {
    const handler = (e: Event) => {
      const customEvent = e as CustomEvent<DeliveryRequest[]>;
      callback(customEvent.detail || this.deliveries);
    };
    window.addEventListener(EVENT_DELIVERY_CHANGED, handler);
    // Trigger immediately
    callback(this.deliveries);
    return () => window.removeEventListener(EVENT_DELIVERY_CHANGED, handler);
  }

  private async updateDelivery(delivery: DeliveryRequest) {
    const index = this.deliveries.findIndex((d) => d.id === delivery.id);
    let updated: DeliveryRequest[];
    if (index >= 0) {
      updated = [...this.deliveries];
      updated[index] = delivery;
    } else {
      updated = [delivery, ...this.deliveries];
    }
    this.saveDeliveriesLocal(updated);

    if (isFirebaseConfigured() && db) {
      try {
        await setDoc(doc(db, 'deliveries', delivery.id), delivery, { merge: true });
      } catch (err) {
        console.warn('Failed to update delivery on Firestore:', err);
      }
    }
  }
}

export const storageService = StorageService.getInstance();
