package io.github

import io.mockative.Mockable

/**
 * Sealed class representing an API result.
 * Used to reproduce Bus Error on iOS ARM64 when returning Error variant.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val value: T) : ApiResult<T>()
    data class Error(val exception: Exception) : ApiResult<Nothing>()
}

/**
 * Interface for testing Bus Error reproduction on iOS ARM64.
 *
 * The issue occurs when:
 * 1. A mocked function has a lambda parameter (e.g., dataProvider)
 * 2. The mock returns an error/failure response
 * 3. Running on iosSimulatorArm64 (works on iosX64)
 *
 * See: docs/BUS_ERROR_IOS_ARM64_INVESTIGATION.md
 */
@Mockable
interface ApiService {
    /**
     * Upload data with a lambda parameter that provides the data.
     *
     * @param metadata Metadata string
     * @param dataProvider Lambda that provides the byte array data
     * @param size Size of the data
     * @return ApiResult indicating success or failure
     */
    suspend fun uploadData(
        metadata: String,
        dataProvider: () -> ByteArray,
        size: Long
    ): ApiResult<String>

    /**
     * Download data with a lambda parameter for processing.
     *
     * @param id Resource identifier
     * @param processor Lambda that processes the downloaded data
     * @return ApiResult indicating success or failure
     */
    suspend fun downloadData(
        id: String,
        processor: (ByteArray) -> Unit
    ): ApiResult<Int>

    /**
     * Blocking version for comparison testing.
     */
    fun uploadDataSync(
        metadata: String,
        dataProvider: () -> ByteArray,
        size: Long
    ): ApiResult<String>
}
