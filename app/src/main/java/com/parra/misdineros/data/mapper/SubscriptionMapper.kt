package com.parra.misdineros.data.mapper

import com.parra.misdineros.data.db.entity.SubscriptionEntity
import com.parra.misdineros.domain.model.BillingCycle
import com.parra.misdineros.domain.model.Subscription
import java.time.LocalDate

fun SubscriptionEntity.toDomain() = Subscription(
    id = id,
    name = name,
    iconRef = iconRef,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    billingCycle = BillingCycle.valueOf(billingCycle),
    nextRenewalDate = LocalDate.parse(nextRenewalDate),
    categoryId = categoryId,
    isPaused = isPaused,
    notifyDaysBefore = notifyDaysBefore,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)

fun Subscription.toEntity() = SubscriptionEntity(
    id = id,
    name = name,
    iconRef = iconRef,
    amountMinor = amountMinor,
    currencyCode = currencyCode,
    billingCycle = billingCycle.name,
    nextRenewalDate = nextRenewalDate.toString(),
    categoryId = categoryId,
    isPaused = isPaused,
    notifyDaysBefore = notifyDaysBefore,
    notes = notes,
    createdAt = createdAt,
    updatedAt = updatedAt,
)
