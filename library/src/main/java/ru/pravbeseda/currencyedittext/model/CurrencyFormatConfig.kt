package ru.pravbeseda.currencyedittext.model

import java.text.DecimalFormatSymbols

data class CurrencyFormatConfig(
    val decimalSeparator: Char = DecimalFormatSymbols().decimalSeparator,
    val groupingSeparator: Char = DecimalFormatSymbols().groupingSeparator,
    val decimalLength: Int = 2
)
