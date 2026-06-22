package com.kobaatv.app

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.Format
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
import com.google.android.exoplayer2.trackselection.TrackSelectionOverride
import com.google.android.exoplayer2.ui.StyledPlayerView

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
    }

    private lateinit var player: ExoPlayer
    private lateinit var trackSelector: DefaultTrackSelector
    private lateinit var playerView: StyledPlayerView
    private lateinit var btnQuality: Button

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
        })

        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true

        btnQuality.setOnClickListener { showQualityDialog() }
    }

    private fun showQualityDialog() {
        if (qualities.isEmpty()) return
        // First entry is "Auto"; the rest map 1:1 to `qualities` by index.
        val items = buildList {
            add(getString(R.string.quality_auto))
            qualities.forEach { q ->
                val f = q.format
                add(
                    when {
                        f.height > 0 -> "${f.height}p"
                        f.bitrate > 0 -> "${f.bitrate / 1000} kbps"
                        else -> getString(R.string.quality_auto)
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
                    val q = qualities[which - 1]
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

    override fun onDestroy() { super.onDestroy(); player.release() }
}
