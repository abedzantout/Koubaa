package com.kobaatv.app

/** One Xtream account. `id` is stable so it can be referenced as last-good. */
data class Account(
    val id: String,
    val host: String,
    val username: String,
    val password: String,
)
