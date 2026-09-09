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
import android.graphics.Bitmap
import android.graphics.Canvas
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
        assertEquals(themed.color(android.R.attr.colorControlHighlight), colors.panelStroke.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.keyText.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.expressionText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorPrimary), colors.operatorText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorError), colors.errorText.defaultColor)
    }

    /** Issue #46 — the fixed error red gave 2.54:1 on a dark panel, below the WCAG AA bound. */
    @Test
    fun theErrorColourFollowsADarkMaterialTheme() {
        val themed = themed(com.google.android.material.R.style.Theme_Material3_Dark)

        assertEquals(
            themed.color(androidx.appcompat.R.attr.colorError),
            CalculatorColors.of(themed).errorText.defaultColor,
        )
    }

    /** Issue #46 — the same, under the plainest theme the panel supports. */
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
        assertEquals(Color.parseColor("#FF405060"), colors.panelStroke.defaultColor)
        assertEquals(Color.parseColor("#FF00FF00"), colors.operatorText.defaultColor)
        assertEquals(themed.color(android.R.attr.textColorPrimary), colors.keyText.defaultColor)
        assertEquals(themed.color(androidx.appcompat.R.attr.colorError), colors.errorText.defaultColor)
    }

    @Test
    fun theFillsAreTransparentUntilAHostAsksForThem() {
        val colors = CalculatorColors.of(themed(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar))

        assertEquals(Color.TRANSPARENT, colors.expressionBackground.defaultColor)
        assertEquals(Color.TRANSPARENT, colors.equalsBackground.defaultColor)
    }

    /** A host that recolours the operators gets the equals key with them, without naming it. */
    @Test
    fun theEqualsTextFollowsTheOperatorRoleWhereNoHostNamesIt() {
        val themed = themed(ru.pravbeseda.currencyedittext.test.R.style.TestThemeWithCalculatorOverride)
        val colors = CalculatorColors.of(themed)

        assertEquals(Color.parseColor("#FF00FF00"), colors.equalsText.defaultColor)
    }

    @Test
    fun aHostStyleFillsTheExpressionLineAndTheEqualsKey() {
        val themed = themed(ru.pravbeseda.currencyedittext.test.R.style.TestThemeWithFilledEquals)
        val colors = CalculatorColors.of(themed)

        assertEquals(Color.parseColor("#FF112233"), colors.expressionBackground.defaultColor)
        assertEquals(Color.parseColor("#FF445566"), colors.equalsBackground.defaultColor)
        assertEquals(Color.parseColor("#FF778899"), colors.equalsText.defaultColor)
    }

    /**
     * Issue #46 — the panel had no edge, so on a window of its own colour it was invisible. A
     * [android.graphics.drawable.GradientDrawable] hands no stroke back, so the edge is asserted
     * where it matters: the pixels the panel is drawn from.
     */
    @Test
    fun thePanelBackgroundIsDrawnWithAnEdge() {
        val themed = themed(androidx.appcompat.R.style.Theme_AppCompat_Light_NoActionBar)
        val colors =
            CalculatorColors.of(themed).copy(
                panelBackground = ColorStateList.valueOf(Color.WHITE),
                panelStroke = ColorStateList.valueOf(Color.RED),
            )
        val size = 128
        val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        colors.panelDrawable(themed).apply {
            setBounds(0, 0, size, size)
            draw(Canvas(bitmap))
        }

        assertEquals(Color.WHITE, bitmap.getPixel(size / 2, size / 2))
        assertEquals(
            "the panel needs an edge of its own, or it vanishes into a window of its own colour",
            Color.RED,
            bitmap.getPixel(size / 2, 0),
        )
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
