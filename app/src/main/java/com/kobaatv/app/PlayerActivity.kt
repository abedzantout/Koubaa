package com.kobaatv.app

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.View
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.lifecycle.lifecycleScope
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.PlaybackException
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.StyledPlayerView
import kotlinx.coroutines.launch

/**
 * Player with a YouTube-style quality selector built on ExoPlayer's
 * DefaultTrackSelector. For HLS streams (Xtream m3u8) ExoPlayer will
 * surface each variant as a TRACK_TYPE_VIDEO track group; we let the
 * user lock to one or pick "Auto".
 */
class PlayerActivity : BaseActivity() {

    companion object {
        const val EXTRA_URL = "url"
        const val EXTRA_TITLE = "title"
        const val EXTRA_STREAM_ID = "stream_id"

        // Treat playback as stalled if it stays buffering this long.
        private const val STALL_TIMEOUT_MS = 12_000L
        // Resume must hold this long before the stall episode is considered over.
        private const val RECOVERY_STABLE_MS = 5_000L
    }

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var playerView: StyledPlayerView
    private lateinit var btnQuality: Button

    private val failover by lazy { FailoverManager(applicationContext) }
    private val handler = Handler(Looper.getMainLooper())

    private var streamId: Int = -1
    private var currentAccountId: String? = null

    // Stall-episode bookkeeping. Reset only after playback stays stable, so a
    // late blip doesn't inherit a stale retry count.
    private var retriesDone = 0
    private val accountsTriedThisEpisode = mutableSetOf<String>()
    private var recovering = false

    /** One selectable video quality: which group it lives in and its index there. */
    private data class VideoQuality(
        val group: Tracks.Group,
        val trackIndex: Int,
        val format: Format,
    )

    private var qualities: List<VideoQuality> = emptyList()

    // The user's intended play/pause state, preserved across onPause/onResume so
    // returning to the foreground does not override a manual pause. Starts true
    // so the stream autoplays on first open.
    private var playWhenReadyIntent = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_player)

        playerView = findViewById(R.id.playerView)
        btnQuality = findViewById(R.id.btnQuality)
        val tvTitle = findViewById<TextView>(R.id.tvTitle)

        val url = intent.getStringExtra(EXTRA_URL) ?: run { finish(); return }
        streamId = intent.getIntExtra(EXTRA_STREAM_ID, -1)
        currentAccountId = Accounts.active(this)?.id
        tvTitle.text = intent.getStringExtra(EXTRA_TITLE).orEmpty()

        trackSelector = DefaultTrackSelector(this)
        player = ExoPlayer.Builder(this).setTrackSelector(trackSelector).build()
        playerView.player = player

        player.addListener(object : Player.Listener {
            override fun onTracksChanged(tracks: Tracks) {
                // HLS quality variants may be spread across several video track
                // groups (not just one), so collect every supported video format
                // across all of them instead of looking at only the first group.
                qualities = tracks.groups
                    .filter { it.type == C.TRACK_TYPE_VIDEO }
                    .flatMap { group ->
                        (0 until group.length)
                            .filter { group.isTrackSupported(it) }
                            .map { VideoQuality(group, it, group.getTrackFormat(it)) }
                    }
                btnQuality.visibility = if (qualities.size > 1) View.VISIBLE else View.GONE
            }

            override fun onPlaybackStateChanged(state: Int) {
                when (state) {
                    Player.STATE_BUFFERING -> scheduleStallCheck()
                    Player.STATE_READY -> onPlaybackHealthy()
                    else -> {}
                }
            }

            override fun onPlayerError(error: PlaybackException) {
                // A hard error is an immediate stall.
                cancelStallCheck()
                recoverFromStall()
            }
        })

        play(url)
        btnQuality.setOnClickListener { showQualityDialog() }
    }

    private fun play(url: String) {
        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true
    }

    // --- stall watchdog ---

    private fun scheduleStallCheck() {
        cancelStallCheck()
        handler.postDelayed(stallRunnable, STALL_TIMEOUT_MS)
    }

    private fun cancelStallCheck() {
        handler.removeCallbacks(stallRunnable)
    }

    private val stallRunnable = Runnable {
        // Still buffering when the timer fires -> treat as a stall.
        if (player.playbackState == Player.STATE_BUFFERING) recoverFromStall()
    }

    private fun onPlaybackHealthy() {
        cancelStallCheck()
        // Only end the stall episode once playback has held steady for a while,
        // so a brief recovery between blips doesn't reset the retry count early.
        handler.removeCallbacks(stableRunnable)
        if (!recovering) return
        handler.postDelayed(stableRunnable, RECOVERY_STABLE_MS)
    }

    private val stableRunnable = Runnable {
        if (player.playbackState == Player.STATE_READY && player.isPlaying) {
            retriesDone = 0
            accountsTriedThisEpisode.clear()
            recovering = false
        }
    }

    private fun recoverFromStall() {
        recovering = true
        currentAccountId?.let { accountsTriedThisEpisode.add(it) }
        val action = Failover.stallAction(
            retriesDone = retriesDone,
            accountsAvailable = Accounts.list(this).size,
            accountsTriedThisEpisode = accountsTriedThisEpisode.size,
        )
        when (action) {
            Failover.StallAction.RETRY_SAME -> {
                retriesDone++
                player.prepare()
                player.playWhenReady = true
            }
            Failover.StallAction.SWITCH_ACCOUNT -> switchAccount()
            Failover.StallAction.GIVE_UP -> {
                Toast.makeText(this, R.string.fail_all_accounts, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun switchAccount() {
        if (streamId < 0) {
            Toast.makeText(this, R.string.fail_all_accounts, Toast.LENGTH_LONG).show()
            return
        }
        lifecycleScope.launch {
            when (val outcome = failover.connect(excludeId = currentAccountId)) {
                is FailoverManager.Outcome.Connected -> {
                    currentAccountId = outcome.account.id
                    accountsTriedThisEpisode.add(outcome.account.id)
                    retriesDone = 0
                    val repo = XtreamRepository(
                        outcome.account.host, outcome.account.username, outcome.account.password,
                    )
                    play(repo.hlsUrl(streamId))
                    Toast.makeText(
                        this@PlayerActivity,
                        getString(R.string.switched_account, outcome.account.username),
                        Toast.LENGTH_SHORT,
                    ).show()
                }
                else -> Toast.makeText(this@PlayerActivity, R.string.fail_all_accounts, Toast.LENGTH_LONG).show()
            }
        }
    }

    private fun showQualityDialog() {
        // Snapshot the current qualities so a later onTracksChanged that mutates
        // the field cannot desync the displayed rows from the click handler.
        val snapshot = qualities
        if (snapshot.isEmpty()) return
        // First entry is "Auto"; the rest map 1:1 to `snapshot` by index.
        val items = buildList {
            add(getString(R.string.quality_auto))
            snapshot.forEachIndexed { i, q ->
                val f = q.format
                add(
                    when {
                        f.height > 0 -> "${f.height}p"
                        f.bitrate > 0 -> "${f.bitrate / 1000} kbps"
                        else -> "Track ${i + 1}"
                    },
                )
            }
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.quality_title)
            .setItems(items.toTypedArray()) { _, which ->
                val params = trackSelector.buildUponParameters()
                if (which == 0) {
                    // Auto: clear overrides for video
                    params.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                } else {
                    val q = snapshot[which - 1]
                    params.setOverrideForType(
                        TrackSelectionOverride(q.group.mediaTrackGroup, q.trackIndex),
                    )
                }
                trackSelector.setParameters(params)
            }
            .show()
    }

    override fun onPause() {
        super.onPause()
        // Capture the user's current play/pause intent before stopping playback
        // for the background, so onResume can restore it instead of forcing play.
        playWhenReadyIntent = player.playWhenReady
        player.playWhenReady = false
    }

    override fun onResume() {
        super.onResume()
        // Resume only if the user had not manually paused before leaving.
        player.playWhenReady = playWhenReadyIntent
    }

    override fun onDestroy() {
        super.onDestroy()
        // Drop any pending stall/recovery callbacks before releasing the player,
        // so they can't fire against a released instance.
        handler.removeCallbacks(stallRunnable)
        handler.removeCallbacks(stableRunnable)
        player.release()
    }
}
