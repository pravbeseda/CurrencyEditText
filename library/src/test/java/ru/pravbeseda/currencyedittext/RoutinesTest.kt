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
                BigDecimal("123456789.123456789"),
                CurrencyFormatConfig(
                    decimalSeparator = ',',
                    groupingSeparator = ' ',
                    decimalLength = 2
                ),
                "123 456 789,12"
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
                    decimalLength = 1
                ),
                "123 456 789,1"
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
                    decimalLength = 6
                ),
                "123 456 789,123456"
            )
        )
        for (case in cases) {
            val result = bigDecimalToString(case.bigDecimal, case.currencyFormatConfig)
            Assert.assertEquals(case.result, result)
        }
    }
}
