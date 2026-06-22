package com.kobaatv.app

import android.app.AlertDialog
import android.net.Uri
import android.os.Bundle
import android.view.View
import android.widget.Button
import android.widget.TextView
import com.google.android.exoplayer2.C
import com.google.android.exoplayer2.ExoPlayer
import com.google.android.exoplayer2.MediaItem
import com.google.android.exoplayer2.Player
import com.google.android.exoplayer2.Tracks
import com.google.android.exoplayer2.source.TrackGroup
import com.google.android.exoplayer2.trackselection.DefaultTrackSelector
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

    private var videoTracks: Tracks.Group? = null

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
                videoTracks = tracks.groups.firstOrNull { it.type == C.TRACK_TYPE_VIDEO }
                btnQuality.visibility = if ((videoTracks?.length ?: 0) > 1) View.VISIBLE else View.GONE
            }
        })

        player.setMediaItem(MediaItem.fromUri(Uri.parse(url)))
        player.prepare()
        player.playWhenReady = true

        btnQuality.setOnClickListener { showQualityDialog() }
    }

    private fun showQualityDialog() {
        val group = videoTracks ?: return
        val items = mutableListOf(getString(R.string.quality_auto))
        val heights = mutableListOf<Int>()
        for (i in 0 until group.length) {
            val f = group.getTrackFormat(i)
            val label = when {
                f.height > 0 -> "${f.height}p"
                f.bitrate > 0 -> "${f.bitrate / 1000} kbps"
                else -> "Track $i"
            }
            items.add(label)
            heights.add(i)
        }
        AlertDialog.Builder(this)
            .setTitle(R.string.quality_title)
            .setItems(items.toTypedArray()) { _, which ->
                val params = trackSelector.buildUponParameters()
                if (which == 0) {
                    // Auto: clear overrides for video
                    params.clearOverridesOfType(C.TRACK_TYPE_VIDEO)
                } else {
                    val trackIndex = heights[which - 1]
                    params.setOverrideForType(
                        com.google.android.exoplayer2.trackselection.TrackSelectionOverride(
                            group.mediaTrackGroup, trackIndex
                        )
                    )
                }
                trackSelector.setParameters(params)
            }
            .show()
    }

    override fun onPause() { super.onPause(); player.playWhenReady = false }
    override fun onResume() { super.onResume(); player.playWhenReady = true }
    override fun onDestroy() { super.onDestroy(); player.release() }
}
