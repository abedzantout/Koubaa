package com.kobaatv.app

import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import android.view.View

class SettingsActivity : BaseActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        title = getString(R.string.settings_title)

        val langSpinner = findViewById<Spinner>(R.id.spLang)
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
                if (picked != Prefs.lang(this@SettingsActivity)) {
                    Prefs.setLang(this@SettingsActivity, picked)
                    Toast.makeText(this@SettingsActivity, R.string.lang_changed, Toast.LENGTH_SHORT).show()
                    recreate()
                }
            }
            override fun onNothingSelected(p: android.widget.AdapterView<*>?) {}
        }
    }
}
