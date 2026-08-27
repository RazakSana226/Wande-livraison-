package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.DeliveryStatus
import com.example.model.DriverVerificationStatus
import com.example.model.PackageSize
import com.example.model.PaymentMethod
import com.example.model.PaymentStatus
import com.example.model.PayoutStatus
import com.example.model.TransactionType
import com.example.model.UserRole
import com.example.model.VehicleType

class Converters {
    @TypeConverter
    fun fromUserRole(value: UserRole): String = value.name

    @TypeConverter
    fun toUserRole(value: String): UserRole = runCatching { UserRole.valueOf(value) }.getOrDefault(UserRole.CLIENT)

    @TypeConverter
    fun fromDriverVerificationStatus(value: DriverVerificationStatus): String = value.name

    @TypeConverter
    fun toDriverVerificationStatus(value: String): DriverVerificationStatus =
        runCatching { DriverVerificationStatus.valueOf(value) }.getOrDefault(DriverVerificationStatus.PENDING_VERIFICATION)

    @TypeConverter
    fun fromIdentityDocumentType(value: com.example.model.IdentityDocumentType): String = value.name

    @TypeConverter
    fun toIdentityDocumentType(value: String): com.example.model.IdentityDocumentType =
        runCatching { com.example.model.IdentityDocumentType.valueOf(value) }.getOrDefault(com.example.model.IdentityDocumentType.CNI)

    @TypeConverter
    fun fromVehicleType(value: VehicleType): String = value.name

    @TypeConverter
    fun toVehicleType(value: String): VehicleType =
        runCatching { VehicleType.valueOf(value) }.getOrDefault(VehicleType.MOTO)

    @TypeConverter
    fun fromPackageSize(value: PackageSize): String = value.name

    @TypeConverter
    fun toPackageSize(value: String): PackageSize =
        runCatching { PackageSize.valueOf(value) }.getOrDefault(PackageSize.PETIT)

    @TypeConverter
    fun fromDeliveryStatus(value: DeliveryStatus): String = value.name

    @TypeConverter
    fun toDeliveryStatus(value: String): DeliveryStatus =
        runCatching { DeliveryStatus.valueOf(value) }.getOrDefault(DeliveryStatus.REQUESTED)

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod): String = value.name

    @TypeConverter
    fun toPaymentMethod(value: String): PaymentMethod =
        runCatching { PaymentMethod.valueOf(value) }.getOrDefault(PaymentMethod.ORANGE_MONEY)

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus): String = value.name

    @TypeConverter
    fun toPaymentStatus(value: String): PaymentStatus =
        runCatching { PaymentStatus.valueOf(value) }.getOrDefault(PaymentStatus.SUCCESS)

    @TypeConverter
    fun fromTransactionType(value: TransactionType): String = value.name

    @TypeConverter
    fun toTransactionType(value: String): TransactionType =
        runCatching { TransactionType.valueOf(value) }.getOrDefault(TransactionType.DELIVERY_EARNING)

    @TypeConverter
    fun fromPayoutStatus(value: PayoutStatus): String = value.name

    @TypeConverter
    fun toPayoutStatus(value: String): PayoutStatus =
        runCatching { PayoutStatus.valueOf(value) }.getOrDefault(PayoutStatus.PAYOUT_PENDING)

    @TypeConverter
    fun fromOtpPurpose(value: com.example.model.OtpPurpose): String = value.name

    @TypeConverter
    fun toOtpPurpose(value: String): com.example.model.OtpPurpose =
        runCatching { com.example.model.OtpPurpose.valueOf(value) }.getOrDefault(com.example.model.OtpPurpose.EMAIL_VERIFICATION)

    @TypeConverter
    fun fromDeliveryOtpStatus(value: com.example.model.DeliveryOtpStatus): String = value.name

    @TypeConverter
    fun toDeliveryOtpStatus(value: String): com.example.model.DeliveryOtpStatus =
        runCatching { com.example.model.DeliveryOtpStatus.valueOf(value) }.getOrDefault(com.example.model.DeliveryOtpStatus.PENDING)

    @TypeConverter
    fun fromAuditAction(value: com.example.model.AuditAction): String = value.name

    @TypeConverter
    fun toAuditAction(value: String): com.example.model.AuditAction =
        runCatching { com.example.model.AuditAction.valueOf(value) }.getOrDefault(com.example.model.AuditAction.LOGIN)

    @TypeConverter
    fun fromAuditSeverity(value: com.example.model.AuditSeverity): String = value.name

    @TypeConverter
    fun toAuditSeverity(value: String): com.example.model.AuditSeverity =
        runCatching { com.example.model.AuditSeverity.valueOf(value) }.getOrDefault(com.example.model.AuditSeverity.INFO)
}
