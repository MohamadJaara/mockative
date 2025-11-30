package io.github

import io.mockative.any
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import io.mockative.once
import io.mockative.verify
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Tests to reproduce Bus Error (Signal 10) on iOS ARM64 Simulator.
 *
 * Issue: When running tests on iosSimulatorArm64Test, tests that mock a function
 * with lambda parameters and return an error response cause the test process to crash.
 *
 * Root Cause: The `makeValueOf` function in nativeMain returns `Unit as T` for
 * unknown types (including function types). This creates a corrupted function pointer
 * that causes Bus Error when accessed during garbage collection on ARM64.
 *
 * See: docs/BUS_ERROR_IOS_ARM64_INVESTIGATION.md
 *
 * Expected behavior after fix:
 * - All tests should pass on both iosSimulatorArm64 and iosX64
 * - OR tests should fail with a clear ValueCreationNotSupportedException
 */
class BusErrorReproductionTests {

    private val apiService = mock(of<ApiService>())

    @Test
    fun suspendFunction_withLambdaParam_returnsSuccess_shouldPass() = runTest {
        // Given
        coEvery { apiService.uploadData(any(), any(), any()) }
            .returns(ApiResult.Success("upload-complete"))

        // When
        val result = apiService.uploadData(
            metadata = "test-metadata",
            dataProvider = { byteArrayOf(1, 2, 3) },
            size = 3L
        )

        // Then
        assertTrue(result is ApiResult.Success)
        assertEquals("upload-complete", (result as ApiResult.Success).value)

        coVerify { apiService.uploadData(any(), any(), any()) }
            .wasInvoked(exactly = once)
    }

    /**
     * This test causes BUS ERROR on iosSimulatorArm64.
     * Returning Error with a lambda parameter triggers the crash.
     *
     * Error: Child process terminated with signal 10: Bus error
     */
    @Test
    fun suspendFunction_withLambdaParam_returnsError_causesBusError() = runTest {
        // Given - This setup causes Bus Error on iOS ARM64
        coEvery { apiService.uploadData(any(), any(), any()) }
            .returns(ApiResult.Error(RuntimeException("Upload failed")))

        // When
        val result = apiService.uploadData(
            metadata = "test-metadata",
            dataProvider = { byteArrayOf(1, 2, 3) },
            size = 3L
        )

        // Then
        assertTrue(result is ApiResult.Error)
        assertEquals("Upload failed", (result as ApiResult.Error).exception.message)

        coVerify { apiService.uploadData(any(), any(), any()) }
            .wasInvoked(exactly = once)
    }

    /**
     * Test with a different lambda signature (ByteArray) -> Unit.
     * This should also reproduce the issue.
     */
    @Test
    fun suspendFunction_withProcessorLambda_returnsError_causesBusError() = runTest {
        // Given
        coEvery { apiService.downloadData(any(), any()) }
            .returns(ApiResult.Error(RuntimeException("Download failed")))

        // When
        val result = apiService.downloadData(
            id = "resource-123",
            processor = { data -> println("Processing ${data.size} bytes") }
        )

        // Then
        assertTrue(result is ApiResult.Error)
        assertEquals("Download failed", (result as ApiResult.Error).exception.message)
    }

    /**
     * Multiple sequential calls to verify the issue is consistent.
     */
    @Test
    fun suspendFunction_multipleErrorReturns_causesBusError() = runTest {
        // Given
        coEvery { apiService.uploadData(any(), any(), any()) }
            .returns(ApiResult.Error(RuntimeException("Error 1")))

        // When/Then - First call
        val result1 = apiService.uploadData("meta1", { byteArrayOf(1) }, 1L)
        assertTrue(result1 is ApiResult.Error)

        // Given - Change stub to different error
        coEvery { apiService.uploadData(any(), any(), any()) }
            .returns(ApiResult.Error(IllegalStateException("Error 2")))

        // When/Then - Second call
        val result2 = apiService.uploadData("meta2", { byteArrayOf(2) }, 2L)
        assertTrue(result2 is ApiResult.Error)
    }

    // =========================================================================
    // BLOCKING FUNCTION TESTS (for comparison)
    // =========================================================================

    /**
     * Blocking version - Success case.
     */
    @Test
    fun blockingFunction_withLambdaParam_returnsSuccess_shouldPass() = ignoreKotlinWasm {
        // Given
        every { apiService.uploadDataSync(any(), any(), any()) }
            .returns(ApiResult.Success("sync-upload-complete"))

        // When
        val result = apiService.uploadDataSync(
            metadata = "test-metadata",
            dataProvider = { byteArrayOf(1, 2, 3) },
            size = 3L
        )

        // Then
        assertTrue(result is ApiResult.Success)
        assertEquals("sync-upload-complete", (result as ApiResult.Success).value)

        verify { apiService.uploadDataSync(any(), any(), any()) }
            .wasInvoked(exactly = once)
    }

    /**
     * Blocking version - Error case.
     * This may also cause Bus Error on iOS ARM64.
     */
    @Test
    fun blockingFunction_withLambdaParam_returnsError_causesBusError() = ignoreKotlinWasm {
        // Given
        every { apiService.uploadDataSync(any(), any(), any()) }
            .returns(ApiResult.Error(RuntimeException("Sync upload failed")))

        // When
        val result = apiService.uploadDataSync(
            metadata = "test-metadata",
            dataProvider = { byteArrayOf(1, 2, 3) },
            size = 3L
        )

        // Then
        assertTrue(result is ApiResult.Error)
        assertEquals("Sync upload failed", (result as ApiResult.Error).exception.message)
    }

    // =========================================================================
    // WORKAROUND TESTS
    // These tests demonstrate the workaround: providing explicit placeholders
    // =========================================================================

    /**
     * WORKAROUND: Provide explicit placeholder for the lambda parameter.
     * This avoids the corrupted `Unit as T` placeholder.
     */
    @Test
    fun workaround_explicitPlaceholder_returnsError_shouldPass() = runTest {
        // Given - Use explicit placeholder for the lambda
        coEvery {
            apiService.uploadData(
                any(),
                any({ byteArrayOf() }),  // Explicit placeholder
                any()
            )
        }.returns(ApiResult.Error(RuntimeException("Upload failed")))

        // When
        val result = apiService.uploadData(
            metadata = "test-metadata",
            dataProvider = { byteArrayOf(1, 2, 3) },
            size = 3L
        )

        // Then - This should work without Bus Error
        assertTrue(result is ApiResult.Error)
        assertEquals("Upload failed", (result as ApiResult.Error).exception.message)
    }

    /**
     * WORKAROUND: Provide explicit placeholder for processor lambda.
     */
    @Test
    fun workaround_explicitProcessorPlaceholder_returnsError_shouldPass() = runTest {
        // Given - Use explicit placeholder for the processor lambda
        coEvery {
            apiService.downloadData(
                any(),
                any({ _: ByteArray -> })  // Explicit placeholder
            )
        }.returns(ApiResult.Error(RuntimeException("Download failed")))

        // When
        val result = apiService.downloadData(
            id = "resource-123",
            processor = { data -> println("Processing ${data.size} bytes") }
        )

        // Then - This should work without Bus Error
        assertTrue(result is ApiResult.Error)
        assertEquals("Download failed", (result as ApiResult.Error).exception.message)
    }
}
