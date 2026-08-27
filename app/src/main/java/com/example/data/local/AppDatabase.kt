package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        UserEntity::class,
        DriverEntity::class,
        DeliveryEntity::class,
        PaymentEntity::class,
        TransactionEntity::class,
        PayoutEntity::class,
        ReviewEntity::class,
        DisputeEntity::class,
        PlatformSettingsEntity::class,
        EmailOtpEntity::class,
        DeliveryOtpEntity::class,
        AuditLogEntity::class
    ],
    version = 2,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun wandeDao(): WandeDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context, scope: CoroutineScope): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "wande_delivery.db"
                )
                    .addCallback(AppDatabaseCallback(scope))
                    .fallbackToDestructiveMigration(dropAllTables = true)
                    .build()
                INSTANCE = instance
                instance
            }
        }

        private class AppDatabaseCallback(
            private val scope: CoroutineScope
        ) : RoomDatabase.Callback() {
            override fun onCreate(db: SupportSQLiteDatabase) {
                super.onCreate(db)
                INSTANCE?.let { database ->
                    scope.launch(Dispatchers.IO) {
                        populateDatabase(database.wandeDao())
                    }
                }
            }
        }

        suspend fun populateDatabase(dao: WandeDao) {
            // 1. Settings
            val settings = PlatformSettingsEntity(
                id = 1,
                basePriceXof = 500,
                pricePerKmXof = 250,
                minimumPriceXof = 1000,
                commissionPercent = 10,
                searchRadiusKm = 10.0,
                currency = "FCFA",
                mockPaymentEnabled = true,
                mockMapsEnabled = false,
                mockNotificationsEnabled = true
            )
            dao.saveSettings(settings)

            // 2. Users
            val clientUser = UserEntity(
                id = "usr_client_1",
                role = UserRole.CLIENT,
                name = "Amadou Ouédraogo",
                phone = "+226 70 12 34 56",
                email = "amadou.ouedraogo@wande.bf",
                isEmailVerified = true,
                status = "ACTIVE"
            )
            val driverUser = UserEntity(
                id = "usr_driver_1",
                role = UserRole.DRIVER,
                name = "Ibrahim Traoré",
                phone = "+226 76 98 76 54",
                email = "ibrahim.traore@wande.bf",
                isEmailVerified = true,
                status = "ACTIVE"
            )
            val driverUser2 = UserEntity(
                id = "usr_driver_2",
                role = UserRole.DRIVER,
                name = "Seydou Sawadogo",
                phone = "+226 65 44 33 22",
                email = "seydou.sawadogo@wande.bf",
                isEmailVerified = true,
                status = "ACTIVE"
            )
            val adminUser = UserEntity(
                id = "usr_admin_1",
                role = UserRole.ADMIN,
                name = "Admin WÀNDÉ",
                phone = "+226 25 30 00 00",
                email = "contact@wande.bf",
                isEmailVerified = true,
                status = "ACTIVE"
            )

            dao.insertUser(clientUser)
            dao.insertUser(driverUser)
            dao.insertUser(driverUser2)
            dao.insertUser(adminUser)

            // 3. Drivers
            val driver1 = DriverEntity(
                id = "drv_1",
                userId = "usr_driver_1",
                name = "Ibrahim Traoré",
                phone = "+226 76 98 76 54",
                vehicleType = VehicleType.MOTO,
                vehicleModel = "Yamaha 125cc",
                vehicleNumber = "11-AB-2044-BF",
                verificationStatus = DriverVerificationStatus.VERIFIED,
                isOnline = true,
                currentLat = 12.3714,
                currentLng = -1.5197,
                rating = 4.9,
                ratingCount = 28,
                totalDeliveries = 48,
                balanceXof = 18500,
                mobileMoneyNumber = "+226 76 98 76 54",
                habitualZone = "Ouaga Centre / Ouaga 2000"
            )
            val driver2 = DriverEntity(
                id = "drv_2",
                userId = "usr_driver_2",
                name = "Seydou Sawadogo",
                phone = "+226 65 44 33 22",
                vehicleType = VehicleType.TRICYCLE,
                vehicleModel = "Tricycle Apsonic 200cc",
                vehicleNumber = "11-TR-8891-BF",
                verificationStatus = DriverVerificationStatus.PENDING_VERIFICATION,
                isOnline = false,
                currentLat = 12.3522,
                currentLng = -1.4850,
                rating = 5.0,
                ratingCount = 0,
                totalDeliveries = 0,
                balanceXof = 0,
                mobileMoneyNumber = "+226 65 44 33 22",
                habitualZone = "Somgandé / Gounghin"
            )
            dao.insertDriver(driver1)
            dao.insertDriver(driver2)

            // 4. Sample Deliveries
            val pastDelivery = DeliveryEntity(
                id = "del_demo_completed",
                trackingNumber = "WD-847291",
                clientId = "usr_client_1",
                clientName = "Amadou Ouédraogo",
                clientPhone = "+226 70 12 34 56",
                driverId = "drv_1",
                driverName = "Ibrahim Traoré",
                driverPhone = "+226 76 98 76 54",
                driverVehicle = "Moto Yamaha 125cc",
                driverRating = 4.9,
                pickupAddress = "Ouaga 2000, près de la Salle des Banquets",
                pickupLat = 12.3280,
                pickupLng = -1.5030,
                pickupInstructions = "Portail vert, sonner à l'interphone",
                destinationAddress = "Avenue Kwame Nkrumah, Centre-ville",
                destinationLat = 12.3685,
                destinationLng = -1.5270,
                recipientName = "Fatimata Kaboré",
                recipientPhone = "+226 78 55 44 33",
                packageDescription = "Dossier confidentiel et clés",
                packageSize = PackageSize.PETIT,
                distanceKm = 5.2,
                estimatedMinutes = 18,
                basePriceXof = 500,
                distancePriceXof = 1300,
                packageSurchargeXof = 0,
                totalPriceXof = 1800,
                platformFeeXof = 180,
                driverEarningsXof = 1620,
                otpCode = "7392",
                status = DeliveryStatus.DELIVERED,
                isPaid = true,
                paymentProvider = PaymentMethod.ORANGE_MONEY,
                createdAt = System.currentTimeMillis() - 86400000L,
                acceptedAt = System.currentTimeMillis() - 85000000L,
                pickedUpAt = System.currentTimeMillis() - 84000000L,
                deliveredAt = System.currentTimeMillis() - 82000000L
            )
            dao.insertDelivery(pastDelivery)

            // Sample Review
            val review = ReviewEntity(
                id = "rev_1",
                deliveryId = "del_demo_completed",
                fromUserId = "usr_client_1",
                toUserId = "drv_1",
                fromName = "Amadou Ouédraogo",
                rating = 5,
                comment = "Livreur très poli, arrivé en avance et colis impeccable !",
                createdAt = System.currentTimeMillis() - 80000000L
            )
            dao.insertReview(review)

            // Sample Transaction
            dao.insertTransaction(
                TransactionEntity(
                    deliveryId = "del_demo_completed",
                    driverId = "drv_1",
                    type = TransactionType.DELIVERY_EARNING,
                    amountXof = 1620,
                    description = "Gain course #WD-847291",
                    status = "COMPLETED"
                )
            )
            dao.insertTransaction(
                TransactionEntity(
                    deliveryId = "del_demo_completed",
                    driverId = "drv_1",
                    type = TransactionType.PLATFORM_FEE,
                    amountXof = 180,
                    description = "Commission WÀNDÉ 10% course #WD-847291",
                    status = "COMPLETED"
                )
            )
        }
    }
}
