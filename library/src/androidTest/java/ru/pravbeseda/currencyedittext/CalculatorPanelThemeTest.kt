/*
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
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.FrameLayout
import android.widget.TextView
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The panel must take its colours from the host app's Material theme, not from the platform's. */
class CalculatorPanelThemeTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    private val themed =
        ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_Material3_DayNight,
        )

    private val panel: View =
        LayoutInflater
            .from(themed)
            .inflate(R.layout.currency_calculator_panel, FrameLayout(themed), false)

    @Test
    fun digitKeysUseTheThemesOnSurfaceColor() {
        val key = panel.findViewById<Button>(R.id.currency_calculator_key_7)
        assertEquals(colorOf(com.google.android.material.R.attr.colorOnSurface), key.currentTextColor)
    }

    @Test
    fun operatorKeysUseTheThemesPrimaryColor() {
        val key = panel.findViewById<Button>(R.id.currency_calculator_key_plus)
        assertEquals(colorOf(androidx.appcompat.R.attr.colorPrimary), key.currentTextColor)
    }

    @Test
    fun theExpressionLineUsesTheThemesOnSurfaceColor() {
        val expression = panel.findViewById<TextView>(R.id.currency_calculator_expression)
        assertEquals(
            colorOf(com.google.android.material.R.attr.colorOnSurface),
            expression.textColors.defaultColor,
        )
    }

    @Test
    fun aDisabledKeyIsDimmed() {
        val key = panel.findViewById<Button>(R.id.currency_calculator_key_sign)
        val enabled = key.currentTextColor
        // The button's state list animator refuses to run off a Looper thread.
        InstrumentationRegistry.getInstrumentation().runOnMainSync { key.isEnabled = false }
        assertTrue(
            "a disabled key must not look like a live one",
            Color.alpha(key.currentTextColor) < Color.alpha(enabled),
        )
    }

    private fun colorOf(attr: Int): Int {
        val value = TypedValue()
        check(themed.theme.resolveAttribute(attr, value, true)) { "attribute $attr is not in the theme" }
        return if (value.resourceId != 0) themed.getColor(value.resourceId) else value.data
    }
}
