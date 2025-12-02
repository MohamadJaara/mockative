package io.mockative

/**
 * Disposes a single mock instance, removing it from the internal registry and releasing
 * all associated state including stubs and recorded invocations.
 *
 * This is particularly important on Kotlin/Native when mocking interfaces that wrap
 * native resources (e.g., crypto libraries), as it ensures mock state doesn't hold
 * references to potentially freed native memory.
 *
 * Usage in tests:
 * ```
 * @AfterTest
 * fun cleanup() {
 *     dispose(myMock)
 * }
 * ```
 *
 * @param receiver The mock instance to dispose
 * @return true if the mock was found and disposed, false if it wasn't registered
 */
fun <T : Any> dispose(receiver: T): Boolean {
    return MockState.dispose(receiver)
}

/**
 * Disposes all mock instances, clearing the entire internal registry.
 *
 * This is useful for cleaning up all mocks in a test suite's teardown phase,
 * especially on Kotlin/Native where mock state can hold references to native resources.
 *
 * **Important for Kotlin/Native:** When mocking interfaces that wrap native libraries
 * (e.g., crypto libraries like CoreCrypto), the mock's internal state may hold references
 * to native memory. If the native library deallocates its resources before the mock is
 * disposed, subsequent tests may encounter memory corruption (Bus errors, signal 10).
 * Calling `disposeAll()` in `@AfterTest` prevents this by ensuring mock state is cleared
 * before native resources are freed.
 *
 * Usage in tests:
 * ```
 * @AfterTest
 * fun cleanup() {
 *     disposeAll()
 * }
 * ```
 *
 * This function is thread-safe and can be called from any thread.
 */
fun disposeAll() {
    MockState.disposeAll()
}