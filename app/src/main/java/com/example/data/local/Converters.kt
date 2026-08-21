package com.example.data.local

import androidx.room.TypeConverter
import com.example.model.DeliveryStatus
import com.example.model.DriverVerificationStatus
import com.example.model.PackageSize
import com.example.model.PaymentProvider
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
    fun fromPaymentProvider(value: PaymentProvider): String = value.name

    @TypeConverter
    fun toPaymentProvider(value: String): PaymentProvider =
        runCatching { PaymentProvider.valueOf(value) }.getOrDefault(PaymentProvider.ORANGE_MONEY)

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
}
