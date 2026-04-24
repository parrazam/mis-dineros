package com.parra.misdineros.data.db.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "subscriptions",
    foreignKeys = [
        ForeignKey(
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.SET_DEFAULT,
        )
    ],
    indices = [Index("categoryId")],
)
data class SubscriptionEntity(
    @PrimaryKey val id: String,
    val name: String,
    val iconRef: String,
    val amountMinor: Long,
    val currencyCode: String,
    val billingCycle: String,
    val nextRenewalDate: String,
    val categoryId: String,
    val isPaused: Boolean,
    val notifyDaysBefore: Int?,
    val notes: String?,
    val createdAt: Long,
    val updatedAt: Long,
)
