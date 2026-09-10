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

import android.view.View
import android.widget.LinearLayout
import androidx.test.core.app.launchActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertSame
import org.junit.Test
import ru.pravbeseda.currencyedittext.calculator.CalculatorStyle

/**
 * The panel is four keys wide and five rows tall, and every key is the same square: it no longer
 * stretches to the width of the field it hangs under, so a wide field gets the same panel as a
 * narrow one.
 */
class CalculatorPanelLayoutTest {
    @Test
    fun theKeysAreLaidOutInFiveRowsOfFour() {
        onAPanel { keys, _ ->
            assertEquals(5, keys.childCount)
            (0 until keys.childCount).forEach {
                assertEquals("row $it", 4, (keys.getChildAt(it) as LinearLayout).childCount)
            }
        }
    }

    @Test
    fun theParenthesisKeyOpensTheBottomRow() {
        onAPanel { keys, _ ->
            val bottom = keys.getChildAt(keys.childCount - 1) as LinearLayout

            assertEquals(
                "the parenthesis key sits in the bottom-left corner",
                R.id.currency_calculator_key_parenthesis,
                bottom.getChildAt(0).id,
            )
            assertSame(
                "the zero key gave up its second cell to it",
                bottom.getChildAt(1),
                bottom.findViewById(R.id.currency_calculator_key_0),
            )
        }
    }

    @Test
    fun everyKeyIsTheSameSquare() {
        onAPanel { keys, content ->
            val side = CalculatorStyle.of(content.context).keySize

            keys.everyKey().forEach {
                assertEquals("key ${it.id} is one key wide", side, it.measuredWidth)
                assertEquals("key ${it.id} is as tall as it is wide", side, it.measuredHeight)
            }
        }
    }

    @Test
    fun thePanelIsAsWideAsItsKeypadAndNotAsTheFieldUnderIt() {
        onAPanel { keys, content ->
            val padded = keys.parent as View

            assertEquals(
                "a panel wider than its keys is a panel stretched to something else",
                keys.measuredWidth + padded.paddingLeft + padded.paddingRight,
                content.measuredWidth,
            )
        }
    }

    @Test
    fun aLineIsDrawnBetweenTheKeysAndNowhereElse() {
        onAPanel { keys, _ ->
            assertEquals(LinearLayout.SHOW_DIVIDER_MIDDLE, keys.showDividers)
            assertNotNull("the line between the rows", keys.dividerDrawable)
            (0 until keys.childCount).map { keys.getChildAt(it) as LinearLayout }.forEach {
                assertEquals("row ${it.id}", LinearLayout.SHOW_DIVIDER_MIDDLE, it.showDividers)
                assertNotNull("the line between the keys of a row", it.dividerDrawable)
            }
        }
    }

    /** Opens a real panel, measures its content, and hands the key grid and that content over. */
    private fun onAPanel(assertions: (keys: LinearLayout, content: View) -> Unit) {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val field = activity.addField()
                field.showCalculator(field)

                val content = checkNotNull(field.calculatorPanel?.window?.contentView) { "the panel is not up" }
                val unconstrained = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
                content.measure(unconstrained, unconstrained)
                assertions(content.findViewById(R.id.currency_calculator_keys), content)
            }
        }
    }

    private fun LinearLayout.everyKey(): List<View> =
        (0 until childCount).flatMap { row ->
            (getChildAt(row) as LinearLayout).let { line -> (0 until line.childCount).map { line.getChildAt(it) } }
        }
}
