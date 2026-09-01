import { PackageSize, PricingBreakdown } from '../types';

export const MIN_PRICE_XOF = 1000;
export const RECOMMENDED_PRICE_XOF = 1500;
export const ATTRACTIVE_PRICE_XOF = 2000;
export const PLATFORM_COMMISSION_RATE = 0.10; // 10%
export const DRIVER_SHARE_RATE = 0.90; // 90%

/**
 * Calculates distance in kilometers between two GPS points using Haversine formula
 */
export function calculateDistanceKm(lat1: number, lon1: number, lat2: number, lon2: number): number {
  if (!lat1 || !lon1 || !lat2 || !lon2) return 3.5;
  const R = 6371; // Rayon terrestre en km
  const dLat = ((lat2 - lat1) * Math.PI) / 180;
  const dLon = ((lon2 - lon1) * Math.PI) / 180;
  const a =
    Math.sin(dLat / 2) * Math.sin(dLat / 2) +
    Math.cos((lat1 * Math.PI) / 180) *
      Math.cos((lat2 * Math.PI) / 180) *
      Math.sin(dLon / 2) *
      Math.sin(dLon / 2);
  const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
  const d = R * c;
  return Math.max(0.8, Math.round(d * 10) / 10);
}

/**
 * Estimates pricing breakdown based on distance and package size
 */
export function estimatePricing(distanceKm: number, packageSize: PackageSize): PricingBreakdown {
  const basePrice = 500;
  const perKmRate = 250;
  const distanceCost = Math.round(distanceKm * perKmRate);

  let packageSurcharge = 0;
  if (packageSize === 'MOYEN') packageSurcharge = 300;
  if (packageSize === 'VOLUMINEUX') packageSurcharge = 700;

  const rawRecommended = basePrice + distanceCost + packageSurcharge;
  const recommendedPrice = Math.max(MIN_PRICE_XOF, Math.ceil(rawRecommended / 250) * 250);
  const attractivePrice = Math.max(recommendedPrice + 500, ATTRACTIVE_PRICE_XOF);

  return {
    basePriceXof: basePrice,
    distancePriceXof: distanceCost,
    packageSurchargeXof: packageSurcharge,
    minPriceXof: MIN_PRICE_XOF,
    recommendedPriceXof: recommendedPrice,
    attractivePriceXof: attractivePrice,
    distanceKm,
    estimatedMinutes: Math.max(10, Math.round(distanceKm * 4 + 8)),
  };
}

/**
 * Calculates commissions and totals for a given delivery price
 */
export function calculateBreakdown(coursePriceXof: number) {
  const validCoursePrice = Math.max(MIN_PRICE_XOF, Math.round(coursePriceXof));
  const platformFee = Math.round(validCoursePrice * PLATFORM_COMMISSION_RATE);
  const driverEarnings = validCoursePrice - Math.round(validCoursePrice * 0.10); // 90% net
  const totalCustomerPaid = validCoursePrice + platformFee;

  return {
    coursePrice: validCoursePrice,
    platformFee,
    driverEarnings,
    totalCustomerPaid,
  };
}

/**
 * Formats a number to currency string (e.g. 1 500 FCFA)
 */
export function formatCurrency(amount: number): string {
  return `${amount.toString().replace(/\B(?=(\d{3})+(?!\d))/g, ' ')} FCFA`;
}
