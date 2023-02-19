package ru.pravbeseda.currencyedittext.model

import java.util.*

data class CurrencyInputWatcherConfig(
    val locale: Locale = Locale.getDefault(),
    val currencySymbol: String = "",
    val decimalSeparator: String? = null,
    val groupingSeparator: String? = null,
    val maxNumberOfDecimalPlaces: Int = 2,
    val decimalZerosPadding: Boolean = false,
    val negativeValueAllow: Boolean = false,
    val onValueChanged: ((String?) -> Unit)? = null
)
