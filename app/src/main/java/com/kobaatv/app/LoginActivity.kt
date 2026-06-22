package com.kobaatv.app

import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.ProgressBar
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.viewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.launch

class LoginActivity : BaseActivity() {

    companion object {
        private const val DEFAULT_HOST = "http://asmrasmr.live:8080"
    }

    private val viewModel: LoginViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val host = findViewById<EditText>(R.id.etHost)
        val user = findViewById<EditText>(R.id.etUser)
        val pass = findViewById<EditText>(R.id.etPass)
        val btn = findViewById<Button>(R.id.btnLogin)
        val btnSettings = findViewById<Button>(R.id.btnSettings)
        val progress = findViewById<ProgressBar>(R.id.progress)
        val langSpinner = findViewById<Spinner>(R.id.spLang)

        // This screen adds an account, so start fresh with just the default
        // host; username/password are entered each time.
        host.setText(DEFAULT_HOST)

        // Language spinner: System / العربية / English
        val langs = listOf(
            getString(R.string.lang_system) to "system",
            "العربية" to "ar",
            "English" to "en",
        )
        val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, langs.map { it.first })
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        langSpinner.adapter = adapter
        val current = Prefs.lang(this)
        langSpinner.setSelection(langs.indexOfFirst { it.second == current }.coerceAtLeast(0))
        langSpinner.onItemSelectedListener = object : android.widget.AdapterView.OnItemSelectedListener {
            override fun onItemSelected(p: android.widget.AdapterView<*>?, v: View?, pos: Int, id: Long) {
                val picked = langs[pos].second
                if (picked != Prefs.lang(this@LoginActivity)) {
                    Prefs.setLang(this@LoginActivity, picked)
                    recreate()
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }

        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }

        btn.setOnClickListener {
            val h = host.text.toString().trim().trimEnd('/')
            val u = user.text.toString().trim()
            val p = pass.text.toString().trim()
            if (h.isEmpty() || u.isEmpty() || p.isEmpty()) {
                Toast.makeText(this, R.string.err_fill_all, Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }
            // Match an actual scheme, not just hosts that happen to start with
            // "http" (e.g. "httpstream.example.tv"), which would otherwise be
            // left scheme-less and fail to parse.
            val hasScheme = h.startsWith("http://", ignoreCase = true) ||
                h.startsWith("https://", ignoreCase = true)
            val normalized = if (hasScheme) h else "http://$h"
            viewModel.login(normalized, u, p)
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.state.collect { state ->
                    val submitting = state is LoginViewModel.State.Submitting
                    progress.visibility = if (submitting) View.VISIBLE else View.GONE
                    btn.isEnabled = !submitting
                    when (state) {
                        is LoginViewModel.State.Success -> {
                            Accounts.upsertAndActivate(
                                this@LoginActivity, state.host, state.user, state.pass,
                            )
                            // Return to Home (creating it on first run, or back to
                            // the existing instance when adding an account).
                            startActivity(
                                Intent(this@LoginActivity, HomeActivity::class.java)
                                    .addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP),
                            )
                            finish()
                        }
                        is LoginViewModel.State.Error -> {
                            Toast.makeText(
                                this@LoginActivity,
                                getString(R.string.err_login, state.message ?: ""),
                                Toast.LENGTH_LONG,
                            ).show()
                            viewModel.clearError()
                        }
                        else -> Unit
                    }
                }
            }
        }
    }
}
