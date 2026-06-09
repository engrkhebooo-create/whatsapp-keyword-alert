package com.keywordalert.wa

import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.keywordalert.wa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsStore
    private lateinit var matchStore: MatchStore
    private lateinit var adapter: MatchAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        settings = SettingsStore(this)
        matchStore = MatchStore(this)

        // طلب صلاحية إظهار الإشعارات (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            ActivityCompat.requestPermissions(
                this, arrayOf(android.Manifest.permission.POST_NOTIFICATIONS), 1
            )
        }

        // تحميل القيم في الواجهة
        binding.etKeywords.setText(settings.keywordsRaw)
        binding.switchEnabled.isChecked = settings.enabled
        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchVibrate.isChecked = settings.vibrateEnabled
        binding.etTgToken.setText(settings.telegramToken)
        binding.etTgChat.setText(settings.telegramChatId)
        binding.etWebhook.setText(settings.webhookUrl)

        // قائمة الرسائل المحفوظة
        adapter = MatchAdapter(matchStore.all())
        binding.rvMatches.layoutManager = LinearLayoutManager(this)
        binding.rvMatches.adapter = adapter

        binding.btnAccess.setOnClickListener {
            startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
        }

        binding.btnSave.setOnClickListener { saveSettings() }

        binding.btnClear.setOnClickListener {
            matchStore.clear()
            adapter.update(matchStore.all())
            Toast.makeText(this, "تم مسح السجل", Toast.LENGTH_SHORT).show()
        }

        binding.btnRefresh.setOnClickListener {
            adapter.update(matchStore.all())
        }
    }

    override fun onResume() {
        super.onResume()
        updateStatus()
        adapter.update(matchStore.all())
    }

    private fun saveSettings() {
        settings.keywordsRaw = binding.etKeywords.text.toString()
        settings.enabled = binding.switchEnabled.isChecked
        settings.soundEnabled = binding.switchSound.isChecked
        settings.vibrateEnabled = binding.switchVibrate.isChecked
        settings.telegramToken = binding.etTgToken.text.toString()
        settings.telegramChatId = binding.etTgChat.text.toString()
        settings.webhookUrl = binding.etWebhook.text.toString()
        Toast.makeText(this, "تم الحفظ ✓", Toast.LENGTH_SHORT).show()
    }

    private fun updateStatus() {
        val enabled = isNotificationServiceEnabled()
        if (enabled) {
            binding.tvStatus.text = "✅ صلاحية الإشعارات مفعّلة"
            binding.tvStatus.setBackgroundColor(0xFF1B5E20.toInt())
        } else {
            binding.tvStatus.text = "⚠️ صلاحية الإشعارات غير مفعّلة — اضغط الزر بالأسفل"
            binding.tvStatus.setBackgroundColor(0xFFB71C1C.toInt())
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat.isNullOrEmpty()) return false
        return flat.split(":").any { it.contains(pkgName) }
    }
}
