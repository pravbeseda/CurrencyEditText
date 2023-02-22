package ru.pravbeseda.currencyedittext.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import ru.pravbeseda.currencyedittext.model.CurrencyFormatConfig

class Routines {
    companion object {
        fun bigDecimalToString(value: BigDecimal, config: CurrencyFormatConfig): String {
            val separators = DecimalFormatSymbols()
            separators.decimalSeparator = config.decimalSeparator
            separators.groupingSeparator = config.groupingSeparator
            var pattern = "#,##0"
            if (config.decimalLength > 0) {
                pattern += "." + "0".repeat(config.decimalLength)
            }
            val df = DecimalFormat(pattern, separators)
            return df.format(value.setScale(config.decimalLength, RoundingMode.FLOOR))
        }
    }
}
