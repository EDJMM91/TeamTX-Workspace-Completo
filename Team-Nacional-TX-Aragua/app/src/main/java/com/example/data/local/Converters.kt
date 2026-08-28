package com.example.data.local

import androidx.room.TypeConverter
import com.example.data.model.*

class Converters {
    @TypeConverter
    fun fromMemberRole(value: MemberRole?): String? = value?.name

    @TypeConverter
    fun toMemberRole(value: String?): MemberRole? = value?.let { enumValueOf<MemberRole>(it) }

    @TypeConverter
    fun fromNoticeCategory(value: NoticeCategory?): String? = value?.name

    @TypeConverter
    fun toNoticeCategory(value: String?): NoticeCategory? = value?.let { enumValueOf<NoticeCategory>(it) }

    @TypeConverter
    fun fromNoticePriority(value: NoticePriority?): String? = value?.name

    @TypeConverter
    fun toNoticePriority(value: String?): NoticePriority? = value?.let { enumValueOf<NoticePriority>(it) }

    @TypeConverter
    fun fromRideStatus(value: RideStatus?): String? = value?.name

    @TypeConverter
    fun toRideStatus(value: String?): RideStatus? = value?.let { enumValueOf<RideStatus>(it) }

    @TypeConverter
    fun fromPaymentCategory(value: PaymentCategory?): String? = value?.name

    @TypeConverter
    fun toPaymentCategory(value: String?): PaymentCategory? = value?.let { enumValueOf<PaymentCategory>(it) }

    @TypeConverter
    fun fromTransactionType(value: TransactionType?): String? = value?.name

    @TypeConverter
    fun toTransactionType(value: String?): TransactionType? = value?.let { enumValueOf<TransactionType>(it) }

    @TypeConverter
    fun fromPaymentMethod(value: PaymentMethod?): String? = value?.name

    @TypeConverter
    fun toPaymentMethod(value: String?): PaymentMethod? = value?.let { enumValueOf<PaymentMethod>(it) }

    @TypeConverter
    fun fromPaymentStatus(value: PaymentStatus?): String? = value?.name

    @TypeConverter
    fun toPaymentStatus(value: String?): PaymentStatus? = value?.let { enumValueOf<PaymentStatus>(it) }

    @TypeConverter
    fun fromItemCategory(value: ItemCategory?): String? = value?.name

    @TypeConverter
    fun toItemCategory(value: String?): ItemCategory? = value?.let { enumValueOf<ItemCategory>(it) }

    @TypeConverter
    fun fromLongList(value: List<Long>?): String? {
        return value?.joinToString(",")
    }

    @TypeConverter
    fun toLongList(value: String?): List<Long> {
        if (value.isNullOrBlank()) return emptyList()
        return value.split(",").mapNotNull { it.toLongOrNull() }
    }

    @TypeConverter
    fun fromItemCondition(value: ItemCondition?): String? = value?.name

    @TypeConverter
    fun toItemCondition(value: String?): ItemCondition? = value?.let { enumValueOf<ItemCondition>(it) }

    @TypeConverter
    fun fromLoanStatus(value: LoanStatus?): String? = value?.name

    @TypeConverter
    fun toLoanStatus(value: String?): LoanStatus? = value?.let { enumValueOf<LoanStatus>(it) }

    @TypeConverter
    fun fromEmergencyType(value: EmergencyType?): String? = value?.name

    @TypeConverter
    fun toEmergencyType(value: String?): EmergencyType? = value?.let { enumValueOf<EmergencyType>(it) }

    @TypeConverter
    fun fromEmergencyStatus(value: EmergencyStatus?): String? = value?.name

    @TypeConverter
    fun toEmergencyStatus(value: String?): EmergencyStatus? = value?.let { enumValueOf<EmergencyStatus>(it) }

    @TypeConverter
    fun fromConvoyRoleType(value: ConvoyRoleType?): String? = value?.name

    @TypeConverter
    fun toConvoyRoleType(value: String?): ConvoyRoleType? = value?.let { enumValueOf<ConvoyRoleType>(it) }

    @TypeConverter
    fun fromMessageType(value: MessageType?): String? = value?.name

    @TypeConverter
    fun toMessageType(value: String?): MessageType? = value?.let { enumValueOf<MessageType>(it) }
}
