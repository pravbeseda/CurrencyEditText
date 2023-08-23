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

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import java.math.BigDecimal
import java.util.*
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CurrencyEditTextTest {
    // Alternative getting context: private val context: Context = ApplicationProvider.getApplicationContext()
    private var context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var currencyEditText: CurrencyEditText

    @Before
    fun init() {
        currencyEditText = CurrencyEditText(context, null)
    }

    @Test
    fun testSetValue() {
        val samples = {
            val decimalSeparator = currencyEditText.getDecimalSeparator()
            val groupingSeparator = currencyEditText.getGroupingSeparator()
            currencyEditText.setEmptyStringForZero(false)
            listOf(
                arrayOf(BigDecimal(4321.76), "4${groupingSeparator}321${decimalSeparator}76"),
                arrayOf(BigDecimal(100.0), "100"),
                arrayOf(BigDecimal(0.0), "0")
            )
        }
        // Run all checks
        val valuesAssertEquals = {
            samples().forEach {
                setValueAssertEquals(it[0] as BigDecimal, it[1] as String)
            }
        }

        // For default separators
        valuesAssertEquals()

        // Custom locales
        currencyEditText.setLocale(Locale("ru", "RU"))
        valuesAssertEquals()

        // Custom separators
        currencyEditText.setLocale(Locale.ENGLISH)
        currencyEditText.setSeparators(' ', ',')
        valuesAssertEquals()
        currencyEditText.setSeparators('^', '_')
        valuesAssertEquals()
    }

    @Test
    fun testSetText() {
        currencyEditText.setSeparators(' ', '.')
        setText("1 000.45")
        assertEquals("1 000.45", currencyEditText.text.toString())
    }

    @Test
    fun shouldNegativeValueAllow() {
        currencyEditText.setNegativeValueAllow(false)
        setTextAssertEquals("-100", "100")
        currencyEditText.setNegativeValueAllow(true)
        setTextAssertEquals("-100", "-100")
    }

    @Test
    fun shouldSetLocale() {
        setValue(BigDecimal(1000.45))
        currencyEditText.setLocale(Locale("ru", "RU"))
        assertEquals("1 000,45", currencyEditText.text.toString())
        currencyEditText.setLocale(Locale.ENGLISH)
        assertEquals("1,000.45", currencyEditText.text.toString())
    }

    @Test
    fun shouldSetSeparators() {
        setValue(BigDecimal(1000.45))
        currencyEditText.setSeparators(' ', ',')
        assertEquals("1 000,45", currencyEditText.text.toString())
        currencyEditText.setSeparators(',', '.')
        assertEquals("1,000.45", currencyEditText.text.toString())
    }

    private fun setValue(value: BigDecimal) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setValue(value)
        }
    }

    private fun setText(text: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setText(text)
        }
    }

    private fun setValueAssertEquals(value: BigDecimal, expected: String) {
        setValue(value)
        assertEquals(expected, currencyEditText.text.toString())
    }

    private fun setTextAssertEquals(text: String, expected: String) {
        setText(text)
        assertEquals(expected, currencyEditText.text.toString())
    }
}
