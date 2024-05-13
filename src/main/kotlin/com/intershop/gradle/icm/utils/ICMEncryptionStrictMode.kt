package com.intershop.gradle.icm.utils

import com.intershop.gradle.icm.extension.DevelopmentConfiguration

/**
 * Purpose of this class is to ensure the ICM encryption strictMode is disabled by default for development systems (enabled by default in ICM code).
 * So if `icm.properties`:
 *  * does not contain the property `intershop.encryption.strictMode.enabled` the strictMode is disabled in ICM
 *  * contains the property `intershop.encryption.strictMode.enabled` set to `true` the strictMode is enabled in ICM (explicit developer decision)
 *
 * @see com.intershop.beehive.core.internal.encryption.GlobalEncryptionConfigurationImpl
 */
open class ICMEncryptionStrictMode(val isStrictModeEnabled: (Unit) -> Boolean) {
    companion object {
        const val PROP_STRICT_MODE_ENABLED = "intershop.encryption.strictMode.enabled"

        fun fromDevelopmentConfiguration(developmentConfiguration: DevelopmentConfiguration) : ICMEncryptionStrictMode {
            return ICMEncryptionStrictMode { developmentConfiguration.getConfigProperty(PROP_STRICT_MODE_ENABLED, false.toString()).toBoolean() }
        }

    }

    fun isStrictModeEnabled() : Boolean = isStrictModeEnabled.invoke(Unit)

    fun disableInICM(applyICMFlag : (key: String, value: Boolean) -> Unit) {
        applyICMFlag.invoke(PROP_STRICT_MODE_ENABLED, false)
    }

    fun applyICMParameterIfNecessary(actualApply : (key: String, value: Boolean) -> Unit) {
        if (!isStrictModeEnabled()){
            disableInICM(actualApply);
        }
    }

}