package com.kobaatv.app

import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ChannelsActivity : BaseActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var progress: ProgressBar
    private lateinit var empty: TextView

    private val viewModel: ChannelsViewModel by viewModels { ChannelsViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)
        title = getString(R.string.channels_title)

        rv = findViewById(R.id.rvChannels)
        progress = findViewById(R.id.progress)
        empty = findViewById(R.id.tvEmpty)
        rv.layoutManager = LinearLayoutManager(this)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { render(it) }
            }
        }
    }

    private fun render(state: ChannelsViewModel.State) {
        when (state) {
            is ChannelsViewModel.State.Loading -> {
                progress.visibility = View.VISIBLE
                empty.visibility = View.GONE
            }
            is ChannelsViewModel.State.Loaded -> {
                progress.visibility = View.GONE
                empty.visibility = if (state.channels.isEmpty()) View.VISIBLE else View.GONE
                rv.adapter = ChannelAdapter(state.channels) { ch ->
                    val intent = Intent(this, PlayerActivity::class.java)
                    intent.putExtra(PlayerActivity.EXTRA_URL, viewModel.hlsUrl(ch.streamId))
                    intent.putExtra(PlayerActivity.EXTRA_TITLE, ch.name)
                    startActivity(intent)
                }
            }
            is ChannelsViewModel.State.Error -> {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                Toast.makeText(this, state.message, Toast.LENGTH_LONG).show()
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
