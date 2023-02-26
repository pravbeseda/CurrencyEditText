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
package ru.pravbeseda.currencyedittext

import java.math.BigDecimal
import org.junit.Assert
import org.junit.Test
import ru.pravbeseda.currencyedittext.model.CurrencyFormatConfig
import ru.pravbeseda.currencyedittext.util.Routines.Companion.bigDecimalToString

data class BigDecimalToStringTestCase(
    var bigDecimal: BigDecimal,
    var currencyFormatConfig: CurrencyFormatConfig,
    var result: String
)

class RoutinesTest {
    @Test
    fun testBigDecimalToString() {
        val cases = listOf(
            BigDecimalToStringTestCase(
                BigDecimal("123456789.129456789"),
                CurrencyFormatConfig(
                    decimalSeparator = '.',
                    groupingSeparator = ',',
                    decimalLength = 2
                ),
                "123,456,789.12"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = '.',
                    groupingSeparator = ',',
                    decimalLength = 3
                ),
                "123,456,789.123"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = '.',
                    decimalLength = 2
                ),
                "123.456.789,12"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = '.',
                    decimalLength = 3
                ),
                "123.456.789,123"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("-123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 2
                ),
                "-123 456 789,13"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 3
                ),
                "123 456 789,123"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 0
                ),
                "123 456 789"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 1,
                    showPlusSign = true
                ),
                "+123 456 789,1"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 4
                ),
                "123 456 789,1234"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 5
                ),
                "123 456 789,12345"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 6,
                    showPlusSign = true
                ),
                "+123 456 789,123456"
            ),
            BigDecimalToStringTestCase(
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = 'n', // from none
                    decimalLength = 6,
                    showPlusSign = true
                ),
                "+123\u0000456\u0000789,123456"
            )
        )
        for (case in cases) {
            val result = bigDecimalToString(case.bigDecimal, case.currencyFormatConfig)
            Assert.assertEquals("\u200E" + case.result, result)
        }
    }
}
