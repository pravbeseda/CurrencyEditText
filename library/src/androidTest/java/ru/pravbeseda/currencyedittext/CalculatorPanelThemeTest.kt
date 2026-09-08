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
import android.content.res.ColorStateList
import android.graphics.Color
import android.util.TypedValue
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.widget.FrameLayout
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import ru.pravbeseda.currencyedittext.calculator.CalculatorColors

/**
 * The panel's colours come from the host: its own theme where it says nothing, and the style it
 * points [R.attr.currencyCalculatorStyle] at where it does. The defaults must resolve under any
 * theme — a Material-only attribute crashes the inflater in an AppCompat app.
 */
class CalculatorPanelThemeTest {
    private val context: Context = InstrumentationRegistry.getInstrumentation().targetContext

    @Test
    fun theDefaultsComeFromTheHostThemeUnderAppCompat() {
        val themed = themed(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        val colors = CalculatorColors.of(themed)

        assertEquals(themed.color(android.R.attr.colorBackground), colors.panelBackground.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.keyText.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.expressionText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorPrimary), colors.operatorText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorError), colors.errorText.defaultColor)
    }

    @Test
    fun theErrorColourFollowsADarkMaterialTheme() {
        val themed = themed(com.google.android.material.R.style.Theme_Material3_Dark)

        assertEquals(
            themed.color(androidx.appcompat.R.attr.colorError),
            CalculatorColors.of(themed).errorText.defaultColor,
        )
    }

    @Test
    fun theErrorColourFollowsADarkAppCompatTheme() {
        val themed = themed(androidx.appcompat.R.style.Theme_AppCompat_NoActionBar)

        assertEquals(
            themed.color(androidx.appcompat.R.attr.colorError),
            CalculatorColors.of(themed).errorText.defaultColor,
        )
    }

    @Test
    fun theDefaultsFollowAMaterialTheme() {
        val themed = themed(com.google.android.material.R.style.Theme_Material3_DayNight)
        val colors = CalculatorColors.of(themed)

        assertEquals(
            themed.color(com.google.android.material.R.attr.colorOnSurface),
            colors.keyText.defaultColor,
        )
        assertEquals(
            themed.color(androidx.appcompat.R.attr.colorPrimary),
            colors.operatorText.defaultColor,
        )
    }

    @Test
    fun aHostStyleRepaintsTheRolesItNamesAndLeavesTheRest() {
        val themed = themed(ru.pravbeseda.currencyedittext.test.R.style.TestThemeWithCalculatorOverride)
        val colors = CalculatorColors.of(themed)

        assertEquals(Color.parseColor("#FF102030"), colors.panelBackground.defaultColor)
        assertEquals(Color.parseColor("#FF00FF00"), colors.operatorText.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.keyText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorError), colors.errorText.defaultColor)
    }

    @Test
    fun aDisabledKeyIsDimmed() {
        val colors = CalculatorColors.of(themed(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar))

        assertTrue(
            "the sign key is disabled where negative values are not allowed, and must look it",
            Color.alpha(colors.operatorText.disabled()) < Color.alpha(colors.operatorText.defaultColor),
        )
    }

    @Test
    fun thePanelInflatesUnderAnAppCompatTheme() {
        val themed = themed(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)

        LayoutInflater
            .from(themed)
            .inflate(R.layout.currency_calculator_panel, FrameLayout(themed), false)
    }

    private fun themed(theme: Int) = ContextThemeWrapper(context, theme)

    private fun Context.color(attr: Int): Int {
        val value = TypedValue()
        check(theme.resolveAttribute(attr, value, true)) { "attribute $attr is not in the theme" }
        return if (value.resourceId != 0) {
            requireNotNull(
                androidx.core.content.ContextCompat
                    .getColorStateList(this, value.resourceId),
            ).defaultColor
        } else {
            value.data
        }
    }

    private fun ColorStateList.disabled(): Int = getColorForState(intArrayOf(-android.R.attr.state_enabled), defaultColor)
}
