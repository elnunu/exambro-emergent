package com.secureexam.browser.data.remote

import com.secureexam.browser.data.remote.dto.MoodleTokenResponse
import com.secureexam.browser.data.remote.dto.MoodleUserResponse
import com.secureexam.browser.data.remote.dto.ViolationReportRequest
import com.secureexam.browser.data.remote.dto.ViolationReportResponse
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

/**
 * Retrofit service interface for Moodle Web Services API.
 */
interface MoodleApiService {
    
    /**
     * Get Moodle token via username/password authentication.
     */
    @FormUrlEncoded
    @POST("login/token.php")
    suspend fun getToken(
        @Field("username") username: String,
        @Field("password") password: String,
        @Field("service") service: String = "moodle_mobile_app"
    ): Response<MoodleTokenResponse>
    
    /**
     * Get site info using token.
     */
    @GET("webservice/rest/server.php")
    suspend fun getSiteInfo(
        @Query("wstoken") token: String,
        @Query("wsfunction") wsFunction: String = "core_webservice_get_site_info",
        @Query("moodlewsrestformat") format: String = "json"
    ): Response<MoodleUserResponse>
    
    /**
     * Report violation to Moodle SEB plugin.
     * Note: This requires the SEB plugin to be installed on Moodle.
     */
    @POST("webservice/rest/server.php")
    suspend fun reportViolation(
        @Query("wstoken") token: String,
        @Query("wsfunction") wsFunction: String = "quizaccess_seb_report_violation",
        @Query("moodlewsrestformat") format: String = "json",
        @Body request: ViolationReportRequest
    ): Response<ViolationReportResponse>
    
    /**
     * Validate SEB config key.
     */
    @GET("webservice/rest/server.php")
    suspend fun validateConfigKey(
        @Query("wstoken") token: String,
        @Query("wsfunction") wsFunction: String = "quizaccess_seb_validate_config_key",
        @Query("moodlewsrestformat") format: String = "json",
        @Query("configkey") configKey: String,
        @Query("quizid") quizId: Int
    ): Response<Boolean>
}
