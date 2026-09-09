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
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.GridLayout
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

private const val PANEL_WIDTH_PX = 400

/**
 * The panel is four columns wide and five rows tall: dropping the parenthesis keys freed the cells
 * that let `=` share the bottom row with `0`, which is what keeps the panel one row shorter than
 * the field it hangs under.
 */
class CalculatorPanelLayoutTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theKeysAreLaidOutInFourColumns() {
        val keys = keys()

        assertEquals(4, keys.columnCount)
        assertEquals("19 key views fill 20 cells: the zero key takes two of them", 19, keys.childCount)
    }

    @Test
    fun theZeroKeyIsTwiceAsWideAsADigit() {
        val keys = keys()

        val zero = keys.key(R.id.currency_calculator_key_0)
        val one = keys.key(R.id.currency_calculator_key_1)
        assertTrue(
            "the zero key spans two columns: ${zero.width} against ${one.width}",
            zero.width in (2 * one.width - 2)..(2 * one.width + 2),
        )
    }

    @Test
    fun theEqualsKeySharesTheBottomRowWithTheZeroKey() {
        val keys = keys()

        val equals = keys.key(R.id.currency_calculator_key_equals)
        val zero = keys.key(R.id.currency_calculator_key_0)
        assertEquals("a row of its own is what the compact layout removed", zero.top, equals.top)
        assertTrue("equals closes the row", equals.left > keys.key(R.id.currency_calculator_key_decimal).left)
    }

    /** The inflated key grid, measured and laid out at a width a phone would give it. */
    private fun keys(): GridLayout {
        val themed = ContextThemeWrapper(context, androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        val content =
            LayoutInflater
                .from(themed)
                .inflate(R.layout.currency_calculator_panel, FrameLayout(themed), false)
        content.measure(
            View.MeasureSpec.makeMeasureSpec(PANEL_WIDTH_PX, View.MeasureSpec.EXACTLY),
            View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED),
        )
        content.layout(0, 0, content.measuredWidth, content.measuredHeight)
        return content.findViewById<View>(R.id.currency_calculator_key_0).parent as GridLayout
    }

    private fun ViewGroup.key(id: Int): View = findViewById(id)
}
