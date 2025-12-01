package io.github

import io.mockative.any
import io.mockative.coEvery
import io.mockative.coVerify
import io.mockative.eq
import io.mockative.mock
import io.mockative.of
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Tests for MixedArgumentMatcherException bug fix on Kotlin/Native.
 *
 * On Kotlin/Native (iOS Simulator), coEvery was throwing MixedArgumentMatcherException when mocking
 * a suspend function that has zero parameters. The issue was that the global Matchers queue was not
 * being properly cleared between mock setups, causing leftover matchers from previous calls to
 * pollute subsequent calls.
 *
 * The fix ensures that:
 * 1. Matchers are cleared at the START of every/coEvery/verify/coVerify calls
 * 2. Matchers are cleared at the END (in finally block) of all these calls
 *
 * See: Invocation.kt toExpectation() - throws MixedArgumentMatcherException when
 *      arguments.size != Matchers.size
 */
internal class NoArgFunctionMatcherStateTests {

    private val github: GitHubAPI = mock(of<GitHubAPI>())

    @Test
    fun testNoArgSuspendFunctionShouldNotThrowMixedMatcherException() = runTest {
        // This should NOT throw MixedArgumentMatcherException since there are no parameters
        // Previously on Native, this would fail if any matchers were left in the global queue
        coEvery { github.repositories() }.returns(emptyList())

        val result = github.repositories()
        assertEquals(emptyList(), result)
    }

    @Test
    fun testSequentialMockSetupWithMatcherThenNoArg() = runTest {
        // First setup with a matcher - this adds a matcher to the global Matchers queue
        coEvery { github.repository(any()) }.returns(Repository("id", "name"))

        // Second setup with no params - should NOT be affected by previous matcher state
        // This was failing on Native with MixedArgumentMatcherException before the fix
        coEvery { github.repositories() }.returns(emptyList())

        assertEquals(emptyList(), github.repositories())
        assertEquals(Repository("id", "name"), github.repository("test"))
    }

    @Test
    fun testSequentialMockSetupWithEqMatcherThenNoArg() = runTest {
        // First setup with eq() matcher
        coEvery { github.repository(eq("specific-id")) }.returns(Repository("specific-id", "Specific"))

        // Second setup with no params - should NOT be affected by previous matcher state
        coEvery { github.repositories() }.returns(listOf(Repository("all", "All")))

        assertEquals(listOf(Repository("all", "All")), github.repositories())
        assertEquals(Repository("specific-id", "Specific"), github.repository("specific-id"))
    }

    @Test
    fun testMultipleMocksDoNotShareMatcherState() = runTest {
        val github1: GitHubAPI = mock(of<GitHubAPI>())
        val github2: GitHubAPI = mock(of<GitHubAPI>())

        // Setup github1 with matcher
        coEvery { github1.repository(eq("specific")) }.returns(Repository("1", "github1"))

        // Setup github2 with no-arg function - should not be affected by github1's matcher state
        coEvery { github2.repositories() }.returns(listOf(Repository("2", "github2")))

        assertEquals(listOf(Repository("2", "github2")), github2.repositories())
        assertEquals(Repository("1", "github1"), github1.repository("specific"))
    }

    @Test
    fun testVerifyWithMatcherThenCoEveryNoArg() = runTest {
        // Setup initial expectation
        coEvery { github.repository(any()) }.returns(Repository("id", "name"))

        // Call the function
        github.repository("test-id")

        // Verify with a matcher - this previously could leave matchers in the queue
        coVerify { github.repository(any()) }.wasInvoked()

        // Now setup a no-arg function - should NOT be affected by verify's matcher state
        coEvery { github.repositories() }.returns(emptyList())

        assertEquals(emptyList(), github.repositories())
    }

    @Test
    fun testMultipleSequentialNoArgCalls() = runTest {
        // Multiple no-arg setups in sequence should all work
        coEvery { github.repositories() }.returns(listOf(Repository("1", "First")))

        assertEquals(listOf(Repository("1", "First")), github.repositories())

        // Override with new expectation
        coEvery { github.repositories() }.returns(listOf(Repository("2", "Second")))

        assertEquals(listOf(Repository("2", "Second")), github.repositories())
    }
}
