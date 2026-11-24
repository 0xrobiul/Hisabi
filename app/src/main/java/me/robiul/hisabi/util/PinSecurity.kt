package me.robiul.hisabi.util

import java.security.MessageDigest

object PinSecurity {
    private const val SALT = "cardvault_offline_vault_salt_v2"

    fun hashPin(pin: String): String {
        val bytes = MessageDigest.getInstance("SHA-256")
            .digest("$SALT:$pin".toByteArray(Charsets.UTF_8))
        return bytes.joinToString("") { "%02x".format(it) }
    }

    fun verifyPin(pin: String, storedHash: String?): Boolean {
        if (storedHash.isNullOrBlank()) return false
        return hashPin(pin) == storedHash
    }
}
