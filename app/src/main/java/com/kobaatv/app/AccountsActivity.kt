package com.kobaatv.app

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch
import java.text.DateFormat
import java.util.Date

class AccountsActivity : BaseActivity() {

    private lateinit var rv: RecyclerView
    private lateinit var emptyView: TextView

    private val viewModel: AccountsViewModel by viewModels()

    private val adapter = AccountAdapter(
        onRemove = { account -> confirmRemove(account) },
    )

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_accounts)
        title = getString(R.string.accounts_title)

        rv = findViewById(R.id.rvAccounts)
        emptyView = findViewById(R.id.tvEmptyAccounts)
        rv.layoutManager = LinearLayoutManager(this)
        rv.adapter = adapter

        findViewById<Button>(R.id.btnAddAccount).setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.rows.collect { rows ->
                    emptyView.visibility = if (rows.isEmpty()) View.VISIBLE else View.GONE
                    rv.visibility = if (rows.isEmpty()) View.GONE else View.VISIBLE
                    adapter.submitList(rows)
                }
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // A newly added account (returning from LoginActivity) should appear.
        viewModel.refresh()
    }

    private fun confirmRemove(account: Account) {
        AlertDialog.Builder(this)
            .setMessage(R.string.accounts_remove_confirm)
            .setPositiveButton(R.string.accounts_remove) { _, _ -> viewModel.remove(account.id) }
            .setNegativeButton(android.R.string.cancel, null)
            .show()
    }

    private class AccountAdapter(
        val onRemove: (Account) -> Unit,
    ) : RecyclerView.Adapter<AccountAdapter.VH>() {

        private var items: List<AccountsViewModel.Row> = emptyList()

        fun submitList(newItems: List<AccountsViewModel.Row>) {
            items = newItems
            notifyDataSetChanged()
        }

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val user: TextView = v.findViewById(R.id.tvAccountUser)
            val host: TextView = v.findViewById(R.id.tvAccountHost)
            val activeBadge: TextView = v.findViewById(R.id.tvActiveBadge)
            val remove: Button = v.findViewById(R.id.btnRemoveAccount)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_account, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val row = items[position]
            val ctx = holder.itemView.context
            holder.user.text = row.account.username
            holder.host.text = statusLine(ctx, row.probe)
            holder.activeBadge.visibility = if (row.isActive) View.VISIBLE else View.GONE
            holder.remove.setOnClickListener { onRemove(row.account) }
        }

        override fun getItemCount() = items.size

        /** Second line: live expiry / connection info once probed. */
        private fun statusLine(ctx: android.content.Context, probe: AccountsViewModel.Probe): String {
            return when (probe) {
                is AccountsViewModel.Probe.Checking -> ctx.getString(R.string.account_checking)
                is AccountsViewModel.Probe.Unreachable -> ctx.getString(R.string.account_unreachable)
                is AccountsViewModel.Probe.Ok -> {
                    val info = probe.info
                    val expiry = info.expiryEpochSeconds?.let {
                        ctx.getString(
                            R.string.account_expires,
                            DateFormat.getDateInstance().format(Date(it * 1000)),
                        )
                    } ?: ctx.getString(R.string.account_expires_never)
                    val cons = ctx.getString(
                        R.string.account_connections,
                        info.activeConnections,
                        info.maxConnections,
                    )
                    "$expiry · $cons"
                }
            }
        }
    }
}
