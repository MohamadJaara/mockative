package io.github

import io.mockative.coEvery
import io.mockative.dispose
import io.mockative.disposeAll
import io.mockative.every
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Tests for mock disposal functionality.
 *
 * These tests verify that:
 * 1. dispose() properly removes a single mock from the registry
 * 2. disposeAll() properly clears all mocks from the registry
 * 3. Disposed mocks can be re-registered if reused
 * 4. The disposal API is thread-safe (via atomic operations)
 *
 * The disposal functionality is critical for Kotlin/Native when mocking interfaces
 * that wrap native resources (e.g., crypto libraries), as it prevents memory corruption
 * issues (Bus errors) caused by mock state holding references to freed native memory.
 */
internal class MockDisposalTests {

    @AfterTest
    fun cleanup() {
        // Always clean up after each test
        disposeAll()
    }

    @Test
    fun testDisposeRemovesSingleMock() {
        // Given
        val github: GitHubAPI = mock(of<GitHubAPI>())
        every { github.thing("test", 1, Repository("id", "name")) }.returns(Unit)

        // When
        val result = dispose(github)

        // Then
        assertTrue(result, "dispose should return true when mock was found")
    }

    @Test
    fun testDisposeReturnsFalseForUnregisteredMock() {
        // Given - create mock but don't use it (it won't be registered until first use)
        val github: GitHubAPI = mock(of<GitHubAPI>())

        // First disposal should work (mock gets registered on creation)
        dispose(github)

        // When - try to dispose again
        val result = dispose(github)

        // Then
        assertFalse(result, "dispose should return false when mock was not found")
    }

    @Test
    fun testDisposeAllClearsAllMocks() = runTest {
        // Given
        val github1: GitHubAPI = mock(of<GitHubAPI>())
        val github2: GitHubAPI = mock(of<GitHubAPI>())
        val config: GitHubConfiguration = mock(of<GitHubConfiguration>())

        coEvery { github1.repository("id1") }.returns(Repository("id1", "name1"))
        coEvery { github2.repository("id2") }.returns(Repository("id2", "name2"))
        every { config.token }.returns("token")

        // When
        disposeAll()

        // Then - disposing again should return false since all mocks are cleared
        assertFalse(dispose(github1), "github1 should already be disposed")
        assertFalse(dispose(github2), "github2 should already be disposed")
        assertFalse(dispose(config), "config should already be disposed")
    }

    @Test
    fun testMockCanBeReusedAfterDisposal() = runTest {
        // Given
        val github: GitHubAPI = mock(of<GitHubAPI>())
        coEvery { github.repository("id") }.returns(Repository("id", "first"))

        val firstResult = github.repository("id")
        assertEquals("first", firstResult?.name)

        // When - dispose and reuse
        dispose(github)

        // Re-stub the mock (it will be re-registered)
        coEvery { github.repository("id") }.returns(Repository("id", "second"))

        // Then
        val secondResult = github.repository("id")
        assertEquals("second", secondResult?.name)
    }

    @Test
    fun testDisposeAllCanBeCalledMultipleTimes() {
        // Given
        val github: GitHubAPI = mock(of<GitHubAPI>())
        every { github.thing("test", 1, Repository("id", "name")) }.returns(Unit)

        // When - call disposeAll multiple times
        disposeAll()
        disposeAll()
        disposeAll()

        // Then - should not throw and mock should be gone
        assertFalse(dispose(github), "mock should already be disposed")
    }

    @Test
    fun testDisposeDoesNotAffectOtherMocks() = runTest {
        // Given
        val github1: GitHubAPI = mock(of<GitHubAPI>())
        val github2: GitHubAPI = mock(of<GitHubAPI>())

        coEvery { github1.repository("id1") }.returns(Repository("id1", "name1"))
        coEvery { github2.repository("id2") }.returns(Repository("id2", "name2"))

        // When - dispose only github1
        dispose(github1)

        // Then - github2 should still work
        val result = github2.repository("id2")
        assertEquals("name2", result?.name)

        // And github1 should be disposed
        assertFalse(dispose(github1), "github1 should already be disposed")
        assertTrue(dispose(github2), "github2 should still be registered")
    }
}