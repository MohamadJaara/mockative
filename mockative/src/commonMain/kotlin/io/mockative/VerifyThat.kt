package io.mockative

fun verifyNoUnverifiedExpectations(receiver: Any) {
    MockState.mock(receiver).confirmVerified()
}

fun verifyNoUnmetExpectations(receiver: Any) {
    MockState.mock(receiver).verifyNoUnmetExpectations()
}

fun <R> verify(block: () -> R): Verification {
    // Clear any leftover matchers from previous operations to prevent MixedArgumentMatcherException
    // for no-arg functions when spurious matchers remain in the global queue (especially on Native)
    Matchers.clear()
    try {
        val (mock, invocation) = MockState.record(block)
        val expectation = invocation.toExpectation()
        return Verification(mock, expectation)
    } finally {
        Matchers.clear()
    }
}

suspend fun <R> coVerify(block: suspend () -> R): Verification {
    // Clear any leftover matchers from previous operations to prevent MixedArgumentMatcherException
    // for no-arg functions when spurious matchers remain in the global queue (especially on Native)
    Matchers.clear()
    try {
        val (mock, invocation) = MockState.record(block)
        val expectation = invocation.toExpectation()
        return Verification(mock, expectation)
    } finally {
        Matchers.clear()
    }
}
