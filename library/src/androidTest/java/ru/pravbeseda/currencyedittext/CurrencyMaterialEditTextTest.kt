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
import androidx.test.platform.app.InstrumentationRegistry
import com.google.android.material.textfield.TextInputLayout
import org.junit.Assert
import org.junit.Before
import org.junit.Test
import java.util.Locale

class CurrencyMaterialEditTextTest {
    private var context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private lateinit var currencyEditText: CurrencyMaterialEditText

    @Before
    fun init() {
        context.setTheme(com.google.android.material.R.style.Theme_MaterialComponents_Light)
        currencyEditText = CurrencyMaterialEditText(context, null)
    }

    @Test
    fun calculatorIsOffByDefault() {
        Assert.assertFalse(currencyEditText.isCalculatorEnabled())
        Assert.assertEquals(TextInputLayout.END_ICON_NONE, currencyEditText.endIconMode)
    }

    @Test
    fun enablingTheCalculatorShowsTheEndIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setCalculatorEnabled(true)
        }
        Assert.assertTrue(currencyEditText.isCalculatorEnabled())
        Assert.assertEquals(TextInputLayout.END_ICON_CUSTOM, currencyEditText.endIconMode)
        Assert.assertNotNull(currencyEditText.endIconDrawable)

        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setCalculatorEnabled(false)
        }
        Assert.assertFalse(currencyEditText.isCalculatorEnabled())
        Assert.assertEquals(TextInputLayout.END_ICON_NONE, currencyEditText.endIconMode)
    }

    @Test
    fun disablingACalculatorTheLayoutNeverHadKeepsTheHostsEndIcon() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.endIconMode = TextInputLayout.END_ICON_CLEAR_TEXT
            currencyEditText.setCalculatorEnabled(false)
        }

        Assert.assertEquals(TextInputLayout.END_ICON_CLEAR_TEXT, currencyEditText.endIconMode)
    }

    @Test
    fun shouldSetText() {
        val samples =
            listOf(
                arrayOf("100", "100"),
                arrayOf("4321.76", "4 321.76"),
                arrayOf("0.", "0."),
                arrayOf("", ""),
            )
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setSeparators(' ', '.')
        }
        samples.forEach {
            testSetText(it[0], it[1])
        }
    }

    @Test
    fun shouldSetLocale() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setLocale(Locale.ENGLISH)
        }
        testSetText("1000.45", "1,000.45")
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setLocale(Locale("ru", "RU"))
        }
        testSetText("1000,45", "1 000,45")
    }

    @Test
    fun shouldNegativeValueAllow() {
        currencyEditText.setNegativeValueAllow(false)
        testSetText("-100", "100")
        currencyEditText.setNegativeValueAllow(true)
        testSetText("-100", "-100")
    }

    @Test
    fun shouldSetDecimalZerosPadding() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setSeparators(' ', '.')
        }
        currencyEditText.setDecimalZerosPadding(true)
        testSetText("100.1", "100.10")
        currencyEditText.setDecimalZerosPadding(false)
        testSetText("100.1", "100.1")
    }

    /** Issue #44 — the styleable declares both attributes; the init block has to read them. */
    @Test
    fun currencySymbolFromXmlPrefixesTheText() {
        val fromXml = inflateWithSymbol()
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            fromXml.setText("100")
        }
        Assert.assertEquals("$ 100", fromXml.text.toString())
    }

    /** Issue #44 — the layout takes the field's hint as its label when the host names none. */
    @Test
    fun useCurrencySymbolAsHintFromXmlLabelsTheLayout() {
        Assert.assertEquals("$ ", inflateWithSymbol().hint.toString())
    }

    @Test
    fun noCurrencySymbolByDefault() {
        testSetText("100", "100")
        Assert.assertNull(currencyEditText.hint)
    }

    @Test
    fun setCurrencySymbolPrefixesTheText() {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setCurrencySymbol("$")
        }
        testSetText("100", "$ 100")
    }

    private fun inflateWithSymbol(): CurrencyMaterialEditText =
        inflateCurrencySymbolAttrs(context, ru.pravbeseda.currencyedittext.test.R.id.material_with_symbol)

    private fun testSetText(
        text: String,
        expected: String,
    ) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setText(text)
        }
        Assert.assertEquals(expected, currencyEditText.text.toString())
    }
}
