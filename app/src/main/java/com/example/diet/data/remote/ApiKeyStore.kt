package com.example.diet.data.remote

import android.content.Context
import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

/**
 * API Key 安全存储：使用 EncryptedSharedPreferences，禁止明文或硬编码。
 */
@Singleton
class ApiKeyStore @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val masterKey = MasterKey.Builder(context)
        .setKeyGenParameterSpec(
            KeyGenParameterSpec.Builder(
                MasterKey.DEFAULT_MASTER_KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setKeySize(256)
                .build()
        ).build()

    private val prefs = EncryptedSharedPreferences.create(
        context,
        "diet_secure_prefs",
        masterKey,
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )

    private fun key(providerId: String) = "api_key_$providerId"

    fun getApiKey(providerId: String): String? {
        val v = prefs.getString(key(providerId), null)
        return if (v.isNullOrBlank()) null else v
    }

    fun setApiKey(providerId: String, apiKey: String) {
        prefs.edit().putString(key(providerId), apiKey.trim()).apply()
    }

    fun clear(providerId: String) {
        prefs.edit().remove(key(providerId)).apply()
    }

    /** 设置项持久化（非敏感）：选中的厂商 id、全局开关等 */
    fun getSetting(name: String, default: String = ""): String =
        prefs.getString("setting_$name", default) ?: default

    fun setSetting(name: String, value: String) {
        prefs.edit().putString("setting_$name", value).apply()
    }

    // ---------------- 多条目 API 管理 ----------------

    private val json = Json { ignoreUnknownKeys = true }

    private fun entriesKey() = "setting_api_entries"
    private fun selectedEntryKey() = "setting_selected_entry"

    fun getEntries(): List<ApiEntry> {
        val raw = prefs.getString(entriesKey(), null) ?: return emptyList()
        return runCatching { json.decodeFromString<List<ApiEntry>>(raw) }.getOrDefault(emptyList())
    }

    private fun setEntries(entries: List<ApiEntry>) {
        prefs.edit().putString(entriesKey(), json.encodeToString(entries)).apply()
    }

    /** 添加一条 API，并把最新添加的设为当前使用 */
    fun addEntry(entry: ApiEntry) {
        val list = getEntries().toMutableList()
        list.add(entry)
        setEntries(list)
        prefs.edit().putString(selectedEntryKey(), entry.id).apply()
    }

    fun deleteEntry(id: String) {
        val list = getEntries().filterNot { it.id == id }
        setEntries(list)
        if (prefs.getString(selectedEntryKey(), null) == id) {
            prefs.edit().putString(selectedEntryKey(), list.firstOrNull()?.id ?: "").apply()
        }
    }

    /** 当前用于分析的 API（选中的，否则第一条） */
    fun getSelectedEntry(): ApiEntry? {
        val entries = getEntries()
        if (entries.isEmpty()) return null
        val sel = prefs.getString(selectedEntryKey(), null)
        return entries.firstOrNull { it.id == sel } ?: entries.first()
    }

    fun setSelectedEntry(id: String) {
        prefs.edit().putString(selectedEntryKey(), id).apply()
    }
}
