package com.kobaatv.app

import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * TV home screen: branded header, account status, and a row of focusable tiles
 * (Live / Filme / Series / Favorites / Settings / Add Account / Manage Accounts).
 * Tiles whose features are not built yet open a "coming soon" notice.
 */
class HomeActivity : BaseActivity() {

    private lateinit var statusDot: View
    private lateinit var tvClock: TextView
    private val handler = Handler(Looper.getMainLooper())

    private val clockFormat = SimpleDateFormat("HH:mm", Locale.getDefault())

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // No account yet -> send the user to add one first.
        if (Accounts.active(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        setContentView(R.layout.activity_home)
        statusDot = findViewById(R.id.statusDot)
        tvClock = findViewById(R.id.tvClock)

        Accounts.active(this)?.let { account ->
            findViewById<TextView>(R.id.tvServer).text =
                getString(R.string.server_label, account.host.substringAfter("://").substringBefore("/"))
        }

        buildTiles()
    }

    override fun onResume() {
        super.onResume()
        // The account may have changed (added/removed) while away.
        if (Accounts.active(this) == null) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }
        // Health is shown optimistically here; the channel screen verifies on load.
        setDot(R.color.status_online)
        startClock()
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(clockTick)
    }

    private val clockTick = object : Runnable {
        override fun run() {
            tvClock.text = clockFormat.format(Date())
            handler.postDelayed(this, 1000)
        }
    }

    private fun startClock() {
        handler.removeCallbacks(clockTick)
        handler.post(clockTick)
    }

    private fun setDot(@ColorRes colorRes: Int) {
        statusDot.background?.mutate()?.setTint(ContextCompat.getColor(this, colorRes))
    }

    private fun buildTiles() {
        val row = findViewById<LinearLayout>(R.id.tileRow)
        val tiles = listOf(
            Tile(R.string.home_live, R.color.tile_live) { open(ChannelsActivity::class.java) },
            Tile(R.string.home_filme, R.color.tile_filme) { comingSoon() },
            Tile(R.string.home_series, R.color.tile_series) { comingSoon() },
            Tile(R.string.home_favorites, R.color.tile_favorites) { comingSoon() },
            Tile(R.string.settings_title, R.color.tile_settings) { open(SettingsActivity::class.java) },
            Tile(R.string.home_add_account, R.color.tile_account) { open(LoginActivity::class.java) },
            Tile(R.string.home_manage_accounts, R.color.tile_account) { open(AccountsActivity::class.java) },
        )
        tiles.forEach { tile ->
            val view = LayoutInflater.from(this).inflate(R.layout.item_tile, row, false)
            // mutate() so each tile tints its own copy, not the shared drawable.
            view.background?.mutate()?.setTint(ContextCompat.getColor(this, tile.colorRes))
            view.findViewById<TextView>(R.id.tileLabel).setText(tile.labelRes)
            view.setOnClickListener { tile.onClick() }
            row.addView(view)
        }
        // Focus the first tile so the D-pad has a clear starting point.
        row.getChildAt(0)?.requestFocus()
    }

    private fun open(target: Class<*>) = startActivity(Intent(this, target))

    private fun comingSoon() =
        Toast.makeText(this, R.string.coming_soon, Toast.LENGTH_SHORT).show()

    private data class Tile(
        @StringRes val labelRes: Int,
        @ColorRes val colorRes: Int,
        val onClick: () -> Unit,
    )
}
