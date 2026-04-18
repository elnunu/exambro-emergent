package com.secureexam.browser.data.remote.dto

import com.google.gson.annotations.SerializedName

/**
 * Response from Moodle token endpoint.
 */
data class MoodleTokenResponse(
    @SerializedName("token")
    val token: String?,
    
    @SerializedName("privatetoken")
    val privateToken: String?,
    
    @SerializedName("error")
    val error: String?,
    
    @SerializedName("errorcode")
    val errorCode: String?
)

/**
 * Response from site info endpoint.
 */
data class MoodleUserResponse(
    @SerializedName("sitename")
    val siteName: String?,
    
    @SerializedName("username")
    val username: String?,
    
    @SerializedName("firstname")
    val firstName: String?,
    
    @SerializedName("lastname")
    val lastName: String?,
    
    @SerializedName("fullname")
    val fullName: String?,
    
    @SerializedName("userid")
    val userId: Int?,
    
    @SerializedName("siteurl")
    val siteUrl: String?,
    
    @SerializedName("userpictureurl")
    val userPictureUrl: String?
)

/**
 * Request body for reporting violations to Moodle.
 */
data class ViolationReportRequest(
    @SerializedName("quizid")
    val quizId: Int,
    
    @SerializedName("cmid")
    val cmId: Int,
    
    @SerializedName("sessionid")
    val sessionId: String,
    
    @SerializedName("violations")
    val violations: List<ViolationDto>
)

/**
 * Individual violation in report request.
 */
data class ViolationDto(
    @SerializedName("type")
    val type: String,
    
    @SerializedName("timestamp")
    val timestamp: Long,
    
    @SerializedName("description")
    val description: String,
    
    @SerializedName("details")
    val details: String?,
    
    @SerializedName("severity")
    val severity: Int
)

/**
 * Response from violation report endpoint.
 */
data class ViolationReportResponse(
    @SerializedName("success")
    val success: Boolean,
    
    @SerializedName("message")
    val message: String?,
    
    @SerializedName("warnings")
    val warnings: List<MoodleWarning>?
)

/**
 * Moodle warning object.
 */
data class MoodleWarning(
    @SerializedName("item")
    val item: String?,
    
    @SerializedName("itemid")
    val itemId: Int?,
    
    @SerializedName("warningcode")
    val warningCode: String?,
    
    @SerializedName("message")
    val message: String?
)
