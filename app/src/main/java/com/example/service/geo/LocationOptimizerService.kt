package com.example.service.geo

import com.example.model.LatLngPoint
import kotlin.math.*

/**
 * Intelligent Geolocation Optimizer for WÀNDÉ Couriers
 * Ensures battery life preservation and prevents Firestore database write flooding.
 * Rules:
 * 1. Only track when driver is explicitly ONLINE or in ACTIVE DELIVERY
 * 2. Minimum interval between network dispatches: 15 to 30 seconds
 * 3. Minimum displacement delta: > 50 meters
 */
class LocationOptimizerService(
    private val minTimeIntervalMs: Long = 15_000L, // 15s throttle
    private val minDistanceThresholdMeters: Double = 45.0 // 45m threshold
) {
    private var lastDispatchedLat: Double? = null
    private var lastDispatchedLng: Double? = null
    private var lastDispatchedTimestamp: Long = 0L

    /**
     * Determines whether a location update warrants a database write
     */
    fun shouldPublishLocation(
        isOnline: Boolean,
        hasActiveDelivery: Boolean,
        newLat: Double,
        newLng: Double,
        currentTimeMs: Long = System.currentTimeMillis()
    ): Boolean {
        if (!isOnline && !hasActiveDelivery) {
            return false
        }

        val lastLat = lastDispatchedLat
        val lastLng = lastDispatchedLng

        if (lastLat == null || lastLng == null) {
            recordDispatch(newLat, newLng, currentTimeMs)
            return true
        }

        val elapsedMs = currentTimeMs - lastDispatchedTimestamp
        val distanceMeters = calculateHaversineDistanceMeters(lastLat, lastLng, newLat, newLng)

        // Publish if active delivery and moved > 45m, or elapsed > 30s
        val timeConditionMet = elapsedMs >= minTimeIntervalMs
        val distanceConditionMet = distanceMeters >= minDistanceThresholdMeters

        if (hasActiveDelivery) {
            if (distanceConditionMet || elapsedMs >= 20_000L) {
                recordDispatch(newLat, newLng, currentTimeMs)
                return true
            }
        } else {
            if (timeConditionMet && distanceConditionMet) {
                recordDispatch(newLat, newLng, currentTimeMs)
                return true
            }
        }

        return false
    }

    private fun recordDispatch(lat: Double, lng: Double, timestampMs: Long) {
        lastDispatchedLat = lat
        lastDispatchedLng = lng
        lastDispatchedTimestamp = timestampMs
    }

    fun reset() {
        lastDispatchedLat = null
        lastDispatchedLng = null
        lastDispatchedTimestamp = 0L
    }

    companion object {
        fun calculateHaversineDistanceMeters(
            lat1: Double, lon1: Double,
            lat2: Double, lon2: Double
        ): Double {
            val r = 6371000.0 // Earth radius in meters
            val dLat = Math.toRadians(lat2 - lat1)
            val dLon = Math.toRadians(lon2 - lon1)
            val a = sin(dLat / 2) * sin(dLat / 2) +
                    cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                    sin(dLon / 2) * sin(dLon / 2)
            val c = 2 * atan2(sqrt(a), sqrt(1 - a))
            return r * c
        }

        // Popular landmarks in Burkina Faso (Ouagadougou, Bobo-Dioulasso, Koudougou, Banfora)
        val BURKINA_POPULAR_LANDMARKS = listOf(
            LatLngPoint(12.3280, -1.5030, "Ouaga 2000, Salle des Banquets", "Près du Monument des Héros"),
            LatLngPoint(12.3685, -1.5270, "Avenue Kwame Nkrumah, Centre-Ville", "Face à l'Hôtel Silmandé"),
            LatLngPoint(12.3533, -1.5098, "Patte d'Oie, Échangeur de Ouaga", "Près de la Gare TSR"),
            LatLngPoint(12.3812, -1.5540, "Gounghin, Secteur 9", "Près de l'Église Saint-Pierre"),
            LatLngPoint(12.4120, -1.5310, "Tampouy, Secteur 21", "Près du Marché de Tampouy"),
            LatLngPoint(12.3650, -1.4920, "Wayalghin, Secteur 42", "Boulevard Charles de Gaulle"),
            LatLngPoint(12.3780, -1.4810, "Somgandé, Zone Industrielle", "Près de la Mairie"),
            LatLngPoint(11.1761, -4.2968, "Bobo-Dioulasso, Grand Marché", "Avenue de la Nation"),
            LatLngPoint(11.1850, -4.3020, "Bobo-Dioulasso, Koko", "Près de la Cathédrale")
        )
    }
}
