package com.movtery.zalithlauncher.setting.enums

import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.movtery.zalithlauncher.R

enum class AppLanguage(
    val tag: String,
    @param:StringRes val textRes: Int
) {
    FOLLOW_SYSTEM("", R.string.language_follow_system),
    ENGLISH("en", R.string.language_english),
    SIMPLIFIED_CHINESE("zh-CN", R.string.language_simplified_chinese),
    TRADITIONAL_CHINESE("zh-TW", R.string.language_traditional_chinese),
    JAPANESE("ja", R.string.language_japanese),
    RUSSIAN("ru", R.string.language_russian),
    VIETNAMESE("vi", R.string.language_vietnamese),
    INDONESIAN("id", R.string.language_indonesian),
    TURKISH("tr", R.string.language_turkish),
    SPANISH("es", R.string.language_spanish),
    PORTUGUESE("pt", R.string.language_portuguese),
    ARABIC("ar", R.string.language_arabic),
    ITALIAN("it", R.string.language_italian),
    GERMAN("de", R.string.language_german),
    HUNGARIAN("hu", R.string.language_hungarian),
    BRAZILIAN_PORTUGUESE("pt-BR", R.string.language_brazilian_portuguese)
}

fun applyLanguage(language: AppLanguage) {
    val appLocale = if (language != AppLanguage.FOLLOW_SYSTEM) {
        LocaleListCompat.forLanguageTags(language.tag)
    } else {
        LocaleListCompat.getEmptyLocaleList()
    }
    AppCompatDelegate.setApplicationLocales(appLocale)
}
