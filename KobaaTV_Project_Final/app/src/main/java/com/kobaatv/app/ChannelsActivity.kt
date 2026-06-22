package com.kobaatv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ChannelsActivity : BaseActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView
    private var channels: List<XtreamClient.Channel> = emptyList()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)
        title = getString(R.string.channels_title)

        rv = findViewById(R.id.rvChannels)
        progress = findViewById(R.id.progress)
        empty = findViewById(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        load()
    }

    private fun load() {
        progress.visibility = View.VISIBLE
        empty.visibility = View.GONE
        val client = XtreamClient(Prefs.host(this), Prefs.user(this), Prefs.pass(this))
        CoroutineScope(Dispatchers.Main).launch {
            try {
                channels = withContext(Dispatchers.IO) { client.liveStreams() }
                rv.adapter = ChannelAdapter(channels) { ch ->
                    val intent = Intent(this@ChannelsActivity, PlayerActivity::class.java)
                    intent.putExtra(PlayerActivity.EXTRA_URL, client.hlsUrl(ch.streamId))
                    intent.putExtra(PlayerActivity.EXTRA_TITLE, ch.name)
                    startActivity(intent)
                }
                if (channels.isEmpty()) empty.visibility = View.VISIBLE
            } catch (e: Exception) {
                Toast.makeText(this@ChannelsActivity, e.message, Toast.LENGTH_LONG).show()
                empty.visibility = View.VISIBLE
            } finally {
                progress.visibility = View.GONE
            }
        }
    }

    private class ChannelAdapter(
        val items: List<XtreamClient.Channel>,
        val onClick: (XtreamClient.Channel) -> Unit,
    ) : RecyclerView.Adapter<ChannelAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val name: TextView = v.findViewById(R.id.tvName)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_channel, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val ch = items[position]
            holder.name.text = ch.name
            holder.itemView.setOnClickListener { onClick(ch) }
        }

        override fun getItemCount() = items.size
    }
}
