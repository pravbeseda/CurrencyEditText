/**
 * Copyright (c) 2022-2023 Alexander Ivanov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ru.pravbeseda.currencyedittext.util

import java.math.BigDecimal
import java.math.RoundingMode
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import ru.pravbeseda.currencyedittext.model.CurrencyFormatConfig

const val emptyChar = 'n' // from null

class Routines {
    companion object {
        fun bigDecimalToString(value: BigDecimal, config: CurrencyFormatConfig): String {
            val symbols = DecimalFormatSymbols()
            symbols.zeroDigit = '0'
            symbols.digit = '0'
            symbols.decimalSeparator = config.decimalSeparator
            symbols.groupingSeparator =
                if (config.groupingSeparator == emptyChar) '\u0000' else config.groupingSeparator
            var pattern = "#,##0"
            if (config.decimalLength > 0) {
                pattern += "." + "0".repeat(config.decimalLength)
            }
            val df = DecimalFormat(pattern, symbols)
            val result = df.format(value.setScale(config.decimalLength, RoundingMode.FLOOR))
            val prefix =
                if (config.addLTRPrefix) "\u200E" else "" // u200E - LTR mark, need for arabic
            return if (config.showPlusSign && value > BigDecimal.ZERO) {
                "$prefix+$result"
            } else {
                prefix + result
            }
        }
    }
}

fun String?.firstChar(): Char? {
    return if (!this.isNullOrEmpty()) this[0] else null
}
