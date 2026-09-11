/*
 * Copyright (c) Alexander Ivanov
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
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.SystemClock
import android.view.MotionEvent
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.math.BigDecimal
import java.text.DecimalFormatSymbols
import java.util.Locale
import ru.pravbeseda.currencyedittext.test.R as TestR

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
                BigDecimal(4321.76) to "4${groupingSeparator}321${decimalSeparator}76",
                BigDecimal(100.0) to "100",
                BigDecimal(0.0) to "0",
            )
        }
        // Run all checks
        val valuesAssertEquals = {
            samples().forEach {
                setValueAssertEquals(it.first, it.second)
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
    fun calculatorIsOffByDefault() {
        Assert.assertFalse(currencyEditText.isCalculatorEnabled())
        Assert.assertNull(currencyEditText.compoundDrawablesRelative[END_DRAWABLE])
    }

    @Test
    fun enablingTheCalculatorShowsTheButton() {
        currencyEditText.setCalculatorEnabled(true)
        Assert.assertTrue(currencyEditText.isCalculatorEnabled())
        Assert.assertNotNull(currencyEditText.compoundDrawablesRelative[END_DRAWABLE])

        currencyEditText.setCalculatorEnabled(false)
        Assert.assertFalse(currencyEditText.isCalculatorEnabled())
        Assert.assertNull(currencyEditText.compoundDrawablesRelative[END_DRAWABLE])
    }

    @Test
    fun theHostsOwnDrawablesSurviveTheCalculatorButton() {
        val hostIcon = ColorDrawable(Color.RED)
        currencyEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(hostIcon, null, null, null)

        currencyEditText.setCalculatorEnabled(true)
        Assert.assertSame(hostIcon, currencyEditText.compoundDrawablesRelative[START_DRAWABLE])

        currencyEditText.setCalculatorEnabled(false)
        Assert.assertSame(hostIcon, currencyEditText.compoundDrawablesRelative[START_DRAWABLE])
        Assert.assertNull(currencyEditText.compoundDrawablesRelative[END_DRAWABLE])
    }

    @Test
    fun disablingACalculatorTheFieldNeverHadLeavesTheHostsEndDrawableAlone() {
        val hostIcon = ColorDrawable(Color.RED)
        currencyEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(null, null, hostIcon, null)

        currencyEditText.setCalculatorEnabled(false)

        Assert.assertSame(hostIcon, currencyEditText.compoundDrawablesRelative[END_DRAWABLE])
    }

    @Test
    fun onlyAnEnabledFieldWithTheCalculatorOnOpensThePanel() {
        currencyEditText.layout(0, 0, 200, 60)
        currencyEditText.setCalculatorEnabled(true)
        val onTheIcon = tapAt(currencyEditText.width.toFloat())

        Assert.assertTrue(currencyEditText.opensCalculatorOn(onTheIcon))

        currencyEditText.isEnabled = false
        Assert.assertFalse(currencyEditText.opensCalculatorOn(onTheIcon))

        currencyEditText.isEnabled = true
        currencyEditText.setCalculatorEnabled(false)
        currencyEditText.setCompoundDrawablesRelativeWithIntrinsicBounds(
            null,
            null,
            ColorDrawable(Color.RED).apply { setBounds(0, 0, 24, 24) },
            null,
        )
        Assert.assertFalse(currencyEditText.opensCalculatorOn(onTheIcon))
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

    // Backtick names with spaces cannot be dexed below DEX 040, so instrumented
    // tests spell the issue number out instead (#39).
    @Test
    fun issue23EmptyFieldReadsAsZero() {
        setText("")
        assertEquals(BigDecimal.ZERO, currencyEditText.getValue())
    }

    @Test
    fun currencySymbolFromXmlPrefixesTheTextAndHints() {
        val fromXml = inflateWithSymbol()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            fromXml.setText("100")
        }
        assertEquals("$ 100", fromXml.text.toString())
        assertEquals("$ ", fromXml.hint.toString())
    }

    /** Issue #44 — an empty symbol has to leave no prefix, the way the init block reads it. */
    @Test
    fun setCurrencySymbolWithAnEmptySymbolLeavesNoPrefix() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setCurrencySymbol("")
        }
        setTextAssertEquals("100", "100")
    }

    /** A hint this setter put there follows the symbol, so dropping the symbol drops it too. */
    @Test
    fun clearingTheSymbolClearsAHintItSet() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setCurrencySymbol("$", useCurrencySymbolAsHint = true)
            currencyEditText.setCurrencySymbol("", useCurrencySymbolAsHint = false)
        }
        assertEquals("", currencyEditText.hint.toString())
    }

    @Test
    fun setCurrencySymbolKeepsAHintItDidNotSet() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.hint = "Amount"
            currencyEditText.setCurrencySymbol("$", useCurrencySymbolAsHint = false)
        }
        assertEquals("Amount", currencyEditText.hint.toString())
    }

    /** Issue #58 — a field naming one separator keeps the locale's own for the other. */
    @Test
    fun decimalSeparatorAloneFromXmlIsApplied() {
        val fromXml = inflateSeparators(TestR.id.plain_with_decimal_separator)

        assertEquals('.', fromXml.getDecimalSeparator())
        assertEquals(russian.groupingSeparator, fromXml.getGroupingSeparator())
    }

    /** Issue #58 — the same the other way round: a grouping separator on its own. */
    @Test
    fun groupingSeparatorAloneFromXmlIsApplied() {
        val fromXml = inflateSeparators(TestR.id.plain_with_grouping_separator)

        assertEquals('\'', fromXml.getGroupingSeparator())
        assertEquals(russian.decimalSeparator, fromXml.getDecimalSeparator())
    }

    /** Issue #58 — separators that clash leave the number unreadable, so the decimal one moves. */
    @Test
    fun clashingSeparatorsFromXmlMoveTheDecimalOne() {
        val fromXml = inflateSeparators(TestR.id.plain_with_clashing_separators)

        assertEquals('.', fromXml.getGroupingSeparator())
        assertEquals(',', fromXml.getDecimalSeparator())
    }

    private val russian: DecimalFormatSymbols
        get() = DecimalFormatSymbols.getInstance(Locale.forLanguageTag("ru"))

    private fun inflateSeparators(viewId: Int): CurrencyEditText = inflateAttrs(context, TestR.layout.separator_attrs, viewId)

    private fun inflateWithSymbol(): CurrencyEditText =
        inflateAttrs(context, TestR.layout.currency_symbol_attrs, TestR.id.plain_with_symbol)

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

    private fun setValueAssertEquals(
        value: BigDecimal,
        expected: String,
    ) {
        setValue(value)
        assertEquals(expected, currencyEditText.text.toString())
    }

    private fun setTextAssertEquals(
        text: String,
        expected: String,
    ) {
        setText(text)
        assertEquals(expected, currencyEditText.text.toString())
    }

    private fun tapAt(x: Float): MotionEvent {
        val now = SystemClock.uptimeMillis()
        return MotionEvent.obtain(now, now, MotionEvent.ACTION_UP, x, 0f, 0)
    }

    private companion object {
        /** Indices in `compoundDrawablesRelative`. */
        const val START_DRAWABLE = 0
        const val END_DRAWABLE = 2
    }
}
