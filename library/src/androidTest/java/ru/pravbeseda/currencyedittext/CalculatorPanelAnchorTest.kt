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
import android.view.ViewGroup
import androidx.test.core.app.launchActivity
import org.junit.Assert.assertSame
import org.junit.Test

/**
 * A [android.widget.PopupWindow] hangs from the bottom of the view it is anchored to. Anchored to
 * the whole [com.google.android.material.textfield.TextInputLayout], the panel starts below the
 * strip that layout keeps for its error text — some 21dp of empty space between the field and the
 * keys — so it is anchored to the field inside it instead. Sideways it takes the other view: the
 * end icon it belongs to is the layout's, and stands past the end of the field.
 */
class CalculatorPanelAnchorTest {
    @Test
    fun theMaterialLayoutAnchorsThePanelToTheFieldAndNotToItsErrorStrip() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val layout = activity.addMaterialField()
                val field = layout.editText as CurrencyEditText

                layout.calculatorButton().performClick()

                assertSame(field, field.calculatorPanel?.anchor)
            }
        }
    }

    @Test
    fun theMaterialLayoutAlignsThePanelWithItselfAndNotWithTheFieldInsideIt() {
        launchActivity<TestActivity>().use { scenario ->
            scenario.onActivity { activity ->
                val layout = activity.addMaterialField()
                val field = layout.editText as CurrencyEditText

                layout.calculatorButton().performClick()

                assertSame(layout, field.calculatorPanel?.alignTo)
            }
        }
    }

    /** The end icon, found the way a user finds it: by what it says it is. */
    private fun ViewGroup.calculatorButton(): View {
        val label = context.getString(R.string.currency_edit_text_calculator)

        fun search(view: View): View? =
            when {
                view.contentDescription == label -> view
                view is ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { search(view.getChildAt(it)) }
                else -> null
            }
        return checkNotNull(search(this)) { "the calculator button is not on the layout" }
    }
}
