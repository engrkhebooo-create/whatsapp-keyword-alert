package com.keywordalert.wa

import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.recyclerview.widget.LinearLayoutManager
import com.keywordalert.wa.databinding.ActivityMainBinding

class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private lateinit var settings: SettingsStore
    private lateinit var matchStore: MatchStore
    private lateinit var adapter: MatchAdapter
    private lateinit var soundPicker: ActivityResultLauncher<Intent>

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

        // مستقبِل نتيجة اختيار النغمة
        soundPicker = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == RESULT_OK) {
                @Suppress("DEPRECATION")
                val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
                settings.soundUri = uri?.toString() ?: ""
                settings.bumpChannelVersion()
                updateSoundName()
                Toast.makeText(this, "تم اختيار النغمة ✓", Toast.LENGTH_SHORT).show()
            }
        }

        // تحميل القيم في الواجهة
        binding.etKeywords.setText(settings.keywordsRaw)
        binding.etExclude.setText(settings.excludeRaw)
        binding.switchEnabled.isChecked = settings.enabled
        binding.switchSound.isChecked = settings.soundEnabled
        binding.switchVibrate.isChecked = settings.vibrateEnabled
        binding.etTgToken.setText(settings.telegramToken)
        binding.etTgChat.setText(settings.telegramChatId)
        binding.etWebhook.setText(settings.webhookUrl)
        updateSoundName()

        binding.btnSound.setOnClickListener { pickSound() }

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
        settings.excludeRaw = binding.etExclude.text.toString()
        settings.enabled = binding.switchEnabled.isChecked
        settings.soundEnabled = binding.switchSound.isChecked
        settings.vibrateEnabled = binding.switchVibrate.isChecked
        settings.telegramToken = binding.etTgToken.text.toString()
        settings.telegramChatId = binding.etTgChat.text.toString()
        settings.webhookUrl = binding.etWebhook.text.toString()
        // إعادة بناء قناة الإشعار حتى تُطبَّق خيارات الصوت/الاهتزاز فوراً
        settings.bumpChannelVersion()
        Toast.makeText(this, "تم الحفظ ✓", Toast.LENGTH_SHORT).show()
    }

    private fun pickSound() {
        val current = if (settings.soundUri.isNotEmpty()) {
            Uri.parse(settings.soundUri)
        } else {
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        }
        val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_TYPE,
                RingtoneManager.TYPE_NOTIFICATION or RingtoneManager.TYPE_RINGTONE or RingtoneManager.TYPE_ALARM
            )
            putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة التنبيه")
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, current)
        }
        soundPicker.launch(intent)
    }

    private fun updateSoundName() {
        val uriStr = settings.soundUri
        val name = if (uriStr.isEmpty()) {
            "الافتراضية"
        } else {
            try {
                RingtoneManager.getRingtone(this, Uri.parse(uriStr))?.getTitle(this) ?: "مخصصة"
            } catch (e: Exception) {
                "مخصصة"
            }
        }
        binding.tvSoundName.text = "النغمة الحالية: $name"
    }

    private fun updateStatus() {
        val enabled = isNotificationServiceEnabled()
        if (enabled) {
            binding.tvStatus.text = "✅ صلاحية الإشعارات مفعّلة"
            binding.tvStatus.setTextColor(0xFF58E0DC.toInt())
        } else {
            binding.tvStatus.text = "⚠️ صلاحية الإشعارات غير مفعّلة — اضغط الزر بالأسفل"
            binding.tvStatus.setTextColor(0xFFFF6B6B.toInt())
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        if (flat.isNullOrEmpty()) return false
        return flat.split(":").any { it.contains(pkgName) }
    }
}
