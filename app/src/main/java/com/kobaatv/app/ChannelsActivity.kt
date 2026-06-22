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
import androidx.annotation.ColorRes
import androidx.annotation.StringRes
import androidx.core.content.ContextCompat
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
    private lateinit var statusDot: View
    private lateinit var tvStatus: TextView

    // Created once and reused so the scroll position survives a state re-emission
    // (e.g. returning from the player); only its data is swapped out.
    private val adapter = ChannelAdapter { ch ->
        WatchStore.recordWatched(this, ch)
        val intent = Intent(this, PlayerActivity::class.java)
        intent.putExtra(PlayerActivity.EXTRA_STREAM_ID, ch.streamId)
        intent.putExtra(PlayerActivity.EXTRA_TITLE, ch.name)
        startActivity(intent)
    }

    private val viewModel: ChannelsViewModel by viewModels { ChannelsViewModelFactory(this) }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_channels)
        title = getString(R.string.channels_title)

        rv = findViewById(R.id.rvChannels)
        progress = findViewById(R.id.progress)
        empty = findViewById(R.id.tvEmpty)
        statusDot = findViewById(R.id.statusDot)
        tvStatus = findViewById(R.id.tvStatus)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { viewModel.state.collect { render(it) } }
                launch {
                    viewModel.events.collect { reason ->
                        Toast.makeText(this@ChannelsActivity, messageFor(reason), Toast.LENGTH_LONG).show()
                    }
                }
            }
        }
    }

    private fun render(state: ChannelsViewModel.State) {
        when (state) {
            is ChannelsViewModel.State.Loading -> {
                progress.visibility = View.VISIBLE
                empty.visibility = View.GONE
                setStatus(R.color.status_unknown, R.string.status_unknown)
            }
            is ChannelsViewModel.State.Loaded -> {
                progress.visibility = View.GONE
                empty.visibility = if (state.channels.isEmpty()) View.VISIBLE else View.GONE
                adapter.submitList(state.channels)
                setStatus(R.color.status_online, R.string.status_online)
            }
            is ChannelsViewModel.State.Error -> {
                progress.visibility = View.GONE
                empty.visibility = View.VISIBLE
                setStatus(R.color.status_offline, R.string.status_offline)
            }
        }
    }

    private fun setStatus(@ColorRes colorRes: Int, @StringRes labelRes: Int) {
        val color = ContextCompat.getColor(this, colorRes)
        statusDot.background?.mutate()?.setTint(color)
        tvStatus.setText(labelRes)
    }

    private fun messageFor(reason: ChannelsViewModel.FailReason): String = getString(
        when (reason) {
            ChannelsViewModel.FailReason.NO_NETWORK -> R.string.fail_no_network
            ChannelsViewModel.FailReason.ALL_FAILED -> R.string.fail_all_accounts
            ChannelsViewModel.FailReason.NO_ACCOUNTS -> R.string.fail_no_accounts
            ChannelsViewModel.FailReason.LOAD_ERROR -> R.string.fail_load_error
        },
    )

    private class ChannelAdapter(
        val onClick: (XtreamClient.Channel) -> Unit,
    ) : RecyclerView.Adapter<ChannelAdapter.VH>() {

        private var items: List<XtreamClient.Channel> = emptyList()

        fun submitList(newItems: List<XtreamClient.Channel>) {
            items = newItems
            notifyDataSetChanged()
        }

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
