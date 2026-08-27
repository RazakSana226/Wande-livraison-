package com.example.data.local

import androidx.room.*
import com.example.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface WandeDao {

    // Users
    @Query("SELECT * FROM users")
    fun getAllUsers(): Flow<List<UserEntity>>

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserEntity?

    @Query("SELECT * FROM users WHERE email = :email LIMIT 1")
    suspend fun getUserByEmail(email: String): UserEntity?

    @Query("SELECT * FROM users WHERE firebaseUid = :uid LIMIT 1")
    suspend fun getUserByFirebaseUid(uid: String): UserEntity?

    @Query("UPDATE users SET isEmailVerified = :isVerified WHERE id = :userId")
    suspend fun updateEmailVerification(userId: String, isVerified: Boolean)

    @Query("UPDATE users SET email = :email, isEmailVerified = 0 WHERE id = :userId")
    suspend fun updateUserEmail(userId: String, email: String)

    @Query("SELECT * FROM users WHERE role = :role")
    fun getUsersByRole(role: UserRole): Flow<List<UserEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserEntity)

    @Update
    suspend fun updateUser(user: UserEntity)

    // Drivers
    @Query("SELECT * FROM drivers")
    fun getAllDrivers(): Flow<List<DriverEntity>>

    @Query("SELECT * FROM drivers WHERE id = :id LIMIT 1")
    suspend fun getDriverById(id: String): DriverEntity?

    @Query("SELECT * FROM drivers WHERE userId = :userId LIMIT 1")
    suspend fun getDriverByUserId(userId: String): DriverEntity?

    @Query("SELECT * FROM drivers WHERE verificationStatus = 'VERIFIED' AND isOnline = 1")
    fun getAvailableDrivers(): Flow<List<DriverEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDriver(driver: DriverEntity)

    @Update
    suspend fun updateDriver(driver: DriverEntity)

    @Query("UPDATE drivers SET isOnline = :isOnline WHERE id = :driverId")
    suspend fun setDriverOnline(driverId: String, isOnline: Boolean)

    @Query("UPDATE drivers SET currentLat = :lat, currentLng = :lng WHERE id = :driverId")
    suspend fun updateDriverLocation(driverId: String, lat: Double, lng: Double)

    @Query("UPDATE drivers SET balanceXof = balanceXof + :amount WHERE id = :driverId")
    suspend fun creditDriverBalance(driverId: String, amount: Int)

    @Query("UPDATE drivers SET balanceXof = balanceXof - :amount WHERE id = :driverId")
    suspend fun debitDriverBalance(driverId: String, amount: Int)

    // Deliveries
    @Query("SELECT * FROM deliveries ORDER BY createdAt DESC")
    fun getAllDeliveries(): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE id = :id LIMIT 1")
    suspend fun getDeliveryById(id: String): DeliveryEntity?

    @Query("SELECT * FROM deliveries WHERE id = :id LIMIT 1")
    fun getDeliveryFlowById(id: String): Flow<DeliveryEntity?>

    @Query("SELECT * FROM deliveries WHERE clientId = :clientId ORDER BY createdAt DESC")
    fun getDeliveriesForClient(clientId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE driverId = :driverId ORDER BY createdAt DESC")
    fun getDeliveriesForDriver(driverId: String): Flow<List<DeliveryEntity>>

    @Query("SELECT * FROM deliveries WHERE driverId IS NULL AND status IN ('SEARCHING_DRIVER', 'DRIVER_COUNTER_OFFERED', 'COUNTER_OFFER_REJECTED', 'REQUESTED') ORDER BY createdAt DESC")
    fun getOpenDeliveries(): Flow<List<DeliveryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDelivery(delivery: DeliveryEntity)

    @Update
    suspend fun updateDelivery(delivery: DeliveryEntity)

    @Query("UPDATE deliveries SET status = :status WHERE id = :deliveryId")
    suspend fun updateDeliveryStatus(deliveryId: String, status: DeliveryStatus)

    @Query("UPDATE deliveries SET currentDriverLat = :lat, currentDriverLng = :lng WHERE id = :deliveryId")
    suspend fun updateDeliveryDriverCoordinates(deliveryId: String, lat: Double, lng: Double)

    // Payments
    @Query("SELECT * FROM payments ORDER BY createdAt DESC")
    fun getAllPayments(): Flow<List<PaymentEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayment(payment: PaymentEntity)

    // Transactions (Ledger)
    @Query("SELECT * FROM transactions ORDER BY createdAt DESC")
    fun getAllTransactions(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE driverId = :driverId ORDER BY createdAt DESC")
    fun getTransactionsForDriver(driverId: String): Flow<List<TransactionEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTransaction(transaction: TransactionEntity)

    // Payouts
    @Query("SELECT * FROM payouts ORDER BY createdAt DESC")
    fun getAllPayouts(): Flow<List<PayoutEntity>>

    @Query("SELECT * FROM payouts WHERE driverId = :driverId ORDER BY createdAt DESC")
    fun getPayoutsForDriver(driverId: String): Flow<List<PayoutEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPayout(payout: PayoutEntity)

    @Update
    suspend fun updatePayout(payout: PayoutEntity)

    // Reviews
    @Query("SELECT * FROM reviews WHERE toUserId = :toUserId ORDER BY createdAt DESC")
    fun getReviewsForUser(toUserId: String): Flow<List<ReviewEntity>>

    @Query("SELECT * FROM reviews ORDER BY createdAt DESC")
    fun getAllReviews(): Flow<List<ReviewEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReview(review: ReviewEntity)

    // Disputes
    @Query("SELECT * FROM disputes ORDER BY createdAt DESC")
    fun getAllDisputes(): Flow<List<DisputeEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDispute(dispute: DisputeEntity)

    @Update
    suspend fun updateDispute(dispute: DisputeEntity)

    // Platform Settings
    @Query("SELECT * FROM platform_settings WHERE id = 1 LIMIT 1")
    fun getSettings(): Flow<PlatformSettingsEntity?>

    @Query("SELECT * FROM platform_settings WHERE id = 1 LIMIT 1")
    suspend fun getSettingsSnapshot(): PlatformSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: PlatformSettingsEntity)

    // Email OTPs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertEmailOtp(otp: EmailOtpEntity)

    @Update
    suspend fun updateEmailOtp(otp: EmailOtpEntity)

    @Query("SELECT * FROM email_otps WHERE email = :email AND isInvalidated = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestActiveEmailOtp(email: String): EmailOtpEntity?

    @Query("SELECT * FROM email_otps WHERE userId = :userId AND purpose = :purpose AND isInvalidated = 0 ORDER BY createdAt DESC LIMIT 1")
    suspend fun getLatestEmailOtpForUser(userId: String, purpose: OtpPurpose): EmailOtpEntity?

    @Query("SELECT * FROM email_otps WHERE email = :email AND createdAt > :sinceTimestamp")
    suspend fun getRecentEmailOtps(email: String, sinceTimestamp: Long): List<EmailOtpEntity>

    @Query("UPDATE email_otps SET isInvalidated = 1 WHERE email = :email")
    suspend fun invalidateEmailOtps(email: String)

    // Delivery OTPs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDeliveryOtp(deliveryOtp: DeliveryOtpEntity)

    @Update
    suspend fun updateDeliveryOtp(deliveryOtp: DeliveryOtpEntity)

    @Query("SELECT * FROM delivery_otps WHERE deliveryId = :deliveryId LIMIT 1")
    suspend fun getDeliveryOtpByDeliveryId(deliveryId: String): DeliveryOtpEntity?

    // Audit Logs
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAuditLog(log: AuditLogEntity)

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC")
    fun getAllAuditLogs(): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs WHERE userId = :userId ORDER BY timestamp DESC")
    fun getAuditLogsForUser(userId: String): Flow<List<AuditLogEntity>>

    @Query("SELECT * FROM audit_logs ORDER BY timestamp DESC LIMIT :limit")
    suspend fun getRecentAuditLogs(limit: Int): List<AuditLogEntity>
}
