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
    fun shouldSetText() {
        val samples = listOf(
            arrayOf("100", "100"),
            arrayOf("4321.76", "4 321.76"),
            arrayOf("0.", "0.")
        )
        currencyEditText.setGroupingSeparator(" ")
        currencyEditText.setDecimalSeparator(".")
        samples.forEach {
            testSetText(it[0], it[1])
        }
    }

    @Test
    fun shouldNegativeValueAllow() {
        currencyEditText.setNegativeValueAllow(false)
        testSetText("-100", "100")
        currencyEditText.setNegativeValueAllow(true)
        testSetText("-100", "-100")
    }

    @Test
    fun shouldSetLocale() {
        currencyEditText.setLocale(Locale.ENGLISH)
        testSetText("1000.45", "1,000.45")
        currencyEditText.setLocale(Locale("ru", "RU"))
        testSetText("1000,45", "1 000,45")
    }

    @Test
    fun shouldSetSeparators() {
        val samples = listOf(
            arrayOf("100", "100"),
            arrayOf("4321.76", "4 321.76"),
            arrayOf("0.", "0.")
        )
        currencyEditText.setGroupingSeparator(" ")
        currencyEditText.setDecimalSeparator(".")
        samples.forEach {
            testSetText(it[0], it[1])
        }
    }

    private fun testSetText(text: String, expected: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setText(text)
        }
        assertEquals(expected, currencyEditText.text.toString())
    }
}
