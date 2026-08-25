package com.goveye.app.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "interests")
data class InterestEntity(
    @PrimaryKey val id: Int,
    val memberId: Int,
    val summary: String,
    val categoryId: Int,
    val categoryNumber: String,
    val categoryName: String,
    val registrationDate: String? = null,
    val publishedDate: String? = null,
    val rectified: Boolean,
    val fieldsJson: String,
    val lastUpdated: Long,
    val parsedAmountPence: Long? = null,
    val currencyCode: String? = null,
    val bucket: String? = null,
    // Phase 18: 16 structured fields parsed from summary text
    val donorName: String? = null,
    val paymentType: String? = null,
    val paymentDescription: String? = null,
    val donorStatus: String? = null,
    val donorAddress: String? = null,
    val donorCompanyIdentifier: String? = null,
    val destination: String? = null,
    val visitPurpose: String? = null,
    val organisationName: String? = null,
    val organisationDescription: String? = null,
    val propertyLocation: String? = null,
    val propertyType: String? = null,
    val hoursWorked: String? = null,
    val familyMemberName: String? = null,
    val familyMemberRelationship: String? = null,
    val familyMemberRole: String? = null
)
