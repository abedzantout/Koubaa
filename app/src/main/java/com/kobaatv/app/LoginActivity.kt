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
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class LoginActivity : BaseActivity() {

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

        // Pre-fill
        host.setText(Prefs.host(this))
        user.setText(Prefs.user(this))
        pass.setText(Prefs.pass(this))

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
            val normalized = if (h.startsWith("http")) h else "http://$h"
            progress.visibility = View.VISIBLE
            btn.isEnabled = false
            lifecycleScope.launch {
                val ok = try {
                    withContext(Dispatchers.IO) {
                        XtreamClient(normalized, u, p).login()
                    }
                    true
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, getString(R.string.err_login, e.message ?: ""), Toast.LENGTH_LONG).show()
                    false
                }
                progress.visibility = View.GONE
                btn.isEnabled = true
                if (ok) {
                    Prefs.saveXtream(this@LoginActivity, normalized, u, p)
                    startActivity(Intent(this@LoginActivity, ChannelsActivity::class.java))
                    finish()
                }
            }
        }
    }
}
