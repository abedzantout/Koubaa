package com.kobaatv.app

/**
 * Pure failover decision logic, with no Android or network dependencies so it
 * can be unit-tested directly. The caller performs the actual connectivity
 * check and login/playback attempts and feeds the outcomes back in.
 *
 * The thrash guard lives here: if the device has no validated internet we never
 * cycle accounts (switching cannot fix a local network problem). With internet
 * present we try accounts in order (active first) until one works or all fail —
 * "all accounts failed" is a distinct terminal state from "no network".
 */
object Failover {

    /** What the caller should do next. */
    sealed interface Decision {
        /** Attempt this account next. */
        data class Try(val account: Account) : Decision
        /** Stop: no validated internet, so cycling would not help. */
        data object NoNetwork : Decision
        /** Stop: internet is present but every account was tried and failed. */
        data object AllFailed : Decision
        /** Stop: there are no accounts configured at all. */
        data object NoAccounts : Decision
    }

    /**
     * Decides the next step.
     *
     * @param accounts all configured accounts.
     * @param activeId the last-good account id, tried first if present.
     * @param tried ids already attempted (and failed) this round.
     * @param hasValidatedInternet whether the device currently has working internet.
     */
    fun next(
        accounts: List<Account>,
        activeId: String?,
        tried: Set<String>,
        hasValidatedInternet: Boolean,
    ): Decision {
        if (accounts.isEmpty()) return Decision.NoAccounts
        if (!hasValidatedInternet) return Decision.NoNetwork

        // Active (last-good) account first, then the rest in list order.
        val ordered = orderedCandidates(accounts, activeId)
        val nextAccount = ordered.firstOrNull { it.id !in tried }
        return if (nextAccount != null) Decision.Try(nextAccount) else Decision.AllFailed
    }

    /** Active account first (if still present), then the others in list order. */
    fun orderedCandidates(accounts: List<Account>, activeId: String?): List<Account> {
        val active = accounts.firstOrNull { it.id == activeId }
        return if (active == null) accounts else listOf(active) + accounts.filter { it.id != active.id }
    }
}
