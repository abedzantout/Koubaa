package com.kobaatv.app

import org.junit.Assert.assertEquals
import org.junit.Test

class FailoverTest {

    private fun account(id: String) = Account(id, "http://h$id", "u$id", "p$id")

    private val a = account("a")
    private val b = account("b")
    private val c = account("c")
    private val all = listOf(a, b, c)

    @Test
    fun noAccounts_returnsNoAccounts() {
        assertEquals(
            Failover.Decision.NoAccounts,
            Failover.next(emptyList(), activeId = null, tried = emptySet(), hasValidatedInternet = true),
        )
    }

    @Test
    fun noInternet_neverCycles_evenWithAccounts() {
        // The thrash guard: without validated internet we stop, regardless of
        // how many accounts exist or what has been tried.
        assertEquals(
            Failover.Decision.NoNetwork,
            Failover.next(all, activeId = "a", tried = emptySet(), hasValidatedInternet = false),
        )
    }

    @Test
    fun firstTry_picksActiveAccount() {
        val d = Failover.next(all, activeId = "b", tried = emptySet(), hasValidatedInternet = true)
        assertEquals(Failover.Decision.Try(b), d)
    }

    @Test
    fun firstTry_noActive_picksFirstInList() {
        val d = Failover.next(all, activeId = null, tried = emptySet(), hasValidatedInternet = true)
        assertEquals(Failover.Decision.Try(a), d)
    }

    @Test
    fun afterActiveFails_triesNextInListOrder() {
        // active b tried -> next should be a (list order, excluding b).
        val d = Failover.next(all, activeId = "b", tried = setOf("b"), hasValidatedInternet = true)
        assertEquals(Failover.Decision.Try(a), d)
    }

    @Test
    fun allTried_returnsAllFailed() {
        val d = Failover.next(all, activeId = "a", tried = setOf("a", "b", "c"), hasValidatedInternet = true)
        assertEquals(Failover.Decision.AllFailed, d)
    }

    @Test
    fun allFailed_isDistinctFromNoNetwork() {
        // Same "everything tried" set, but the terminal state flips purely on
        // connectivity — proving the two cases are not conflated.
        val online = Failover.next(all, activeId = "a", tried = setOf("a", "b", "c"), hasValidatedInternet = true)
        val offline = Failover.next(all, activeId = "a", tried = setOf("a", "b", "c"), hasValidatedInternet = false)
        assertEquals(Failover.Decision.AllFailed, online)
        assertEquals(Failover.Decision.NoNetwork, offline)
    }

    @Test
    fun staleActiveId_fallsBackToListOrder() {
        // active id no longer in the list (e.g. removed) -> first untried wins.
        val d = Failover.next(all, activeId = "gone", tried = emptySet(), hasValidatedInternet = true)
        assertEquals(Failover.Decision.Try(a), d)
    }

    @Test
    fun orderedCandidates_activeFirstThenRest() {
        assertEquals(listOf(c, a, b), Failover.orderedCandidates(all, activeId = "c"))
    }

    // orderedCandidates also backs Accounts.active(), so cover its edges.

    @Test
    fun orderedCandidates_empty_returnsEmpty() {
        assertEquals(emptyList<Account>(), Failover.orderedCandidates(emptyList(), activeId = "a"))
    }

    @Test
    fun orderedCandidates_nullActive_keepsListOrder() {
        assertEquals(all, Failover.orderedCandidates(all, activeId = null))
    }

    @Test
    fun orderedCandidates_staleActiveId_keepsListOrder() {
        assertEquals(all, Failover.orderedCandidates(all, activeId = "removed"))
    }

    @Test
    fun orderedCandidates_doesNotDuplicateActive() {
        val result = Failover.orderedCandidates(all, activeId = "b")
        assertEquals(listOf(b, a, c), result)
        assertEquals(all.size, result.size) // active not duplicated
    }

    @Test
    fun orderedCandidates_singleAccount() {
        assertEquals(listOf(a), Failover.orderedCandidates(listOf(a), activeId = "a"))
        assertEquals(listOf(a), Failover.orderedCandidates(listOf(a), activeId = null))
    }

    // --- stall recovery ladder ---

    @Test
    fun stall_retriesSameStreamUpToMax() {
        assertEquals(Failover.StallAction.RETRY_SAME, Failover.stallAction(0, accountsAvailable = 3, accountsTriedThisEpisode = 1))
        assertEquals(Failover.StallAction.RETRY_SAME, Failover.stallAction(1, accountsAvailable = 3, accountsTriedThisEpisode = 1))
    }

    @Test
    fun stall_afterMaxRetries_switchesAccountIfAnotherIsUntried() {
        // retriesDone == maxRetries(2): move on from retrying.
        assertEquals(
            Failover.StallAction.SWITCH_ACCOUNT,
            Failover.stallAction(2, accountsAvailable = 3, accountsTriedThisEpisode = 1),
        )
    }

    @Test
    fun stall_allAccountsTriedThisEpisode_givesUp() {
        // Every account already attempted this episode -> stop, don't loop.
        assertEquals(
            Failover.StallAction.GIVE_UP,
            Failover.stallAction(2, accountsAvailable = 3, accountsTriedThisEpisode = 3),
        )
    }

    @Test
    fun stall_singleAccount_collapsesToRetryThenGiveUp() {
        // One account: retry twice, then give up (no other account to switch to).
        assertEquals(Failover.StallAction.RETRY_SAME, Failover.stallAction(0, accountsAvailable = 1, accountsTriedThisEpisode = 1))
        assertEquals(Failover.StallAction.RETRY_SAME, Failover.stallAction(1, accountsAvailable = 1, accountsTriedThisEpisode = 1))
        assertEquals(Failover.StallAction.GIVE_UP, Failover.stallAction(2, accountsAvailable = 1, accountsTriedThisEpisode = 1))
    }
}
