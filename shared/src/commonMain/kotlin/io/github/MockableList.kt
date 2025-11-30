package io.github

import io.mockative.Mockable

/**
 * Wrapper interface for MutableList to work around KSP issue where
 * getAllFunctions()/getAllProperties() doesn't return all inherited
 * abstract members from Collection on Native targets.
 *
 * Use this instead of mocking MutableList directly.
 * Explicitly re-declares the missing Collection members.
 */
@Mockable
interface MockableList<E> : MutableList<E> {
    // Explicitly declare members that KSP misses from Collection
    override val size: Int
    override fun isEmpty(): Boolean
    override fun contains(element: E): Boolean
    override fun iterator(): MutableIterator<E>
    override fun containsAll(elements: Collection<E>): Boolean
}
