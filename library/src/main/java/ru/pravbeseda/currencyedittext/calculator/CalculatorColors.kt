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
package ru.pravbeseda.currencyedittext.calculator

import android.content.Context
import android.content.res.ColorStateList
import android.content.res.TypedArray
import android.graphics.drawable.GradientDrawable
import androidx.appcompat.content.res.AppCompatResources
import ru.pravbeseda.currencyedittext.R
import kotlin.math.roundToInt

/**
 * The panel's colours, read from the host's [R.attr.currencyCalculatorStyle] over the library's own
 * defaults, so a host that sets nothing still gets a panel that resolves under any theme.
 */
internal data class CalculatorColors(
    val panelBackground: ColorStateList,
    val panelStroke: ColorStateList,
    val expressionText: ColorStateList,
    val expressionBackground: ColorStateList,
    val errorText: ColorStateList,
    val keyText: ColorStateList,
    val operatorText: ColorStateList,
    val equalsBackground: ColorStateList,
    val equalsText: ColorStateList,
) {
    /**
     * The panel's background: a rounded rectangle with an edge of its own, because the fill
     * defaults to the colour of the window behind it and an elevation shadow is invisible on a dark
     * one. Both colours are set here rather than in the shape XML, which cannot name a role, and
     * not through a tint, which would paint the fill and the edge alike.
     */
    fun panelDrawable(context: Context): GradientDrawable {
        val shape =
            checkNotNull(
                AppCompatResources
                    .getDrawable(context, R.drawable.currency_calculator_panel_background)
                    ?.mutate() as? GradientDrawable,
            ) { "the calculator panel background must inflate to a shape" }
        shape.setColor(panelBackground)
        shape.setStroke(
            (context.resources.displayMetrics.density * PANEL_STROKE_DP).roundToInt(),
            panelStroke,
        )
        return shape
    }

    companion object {
        private const val PANEL_STROKE_DP = 1f

        /**
         * A host names the roles it cares about and leaves the rest: `defStyleRes` is consulted
         * only when `defStyleAttr` resolves to nothing, so the defaults are read as their own pass
         * and the host's style is laid over them role by role.
         */
        fun of(context: Context): CalculatorColors {
            val defaults =
                context.obtainStyledAttributes(
                    null,
                    R.styleable.CurrencyCalculator,
                    0,
                    R.style.Widget_CurrencyEditText_Calculator,
                )
            val host =
                context.obtainStyledAttributes(
                    null,
                    R.styleable.CurrencyCalculator,
                    R.attr.currencyCalculatorStyle,
                    0,
                )
            try {
                return read(host, defaults)
            } finally {
                host.recycle()
                defaults.recycle()
            }
        }

        /** The host's answer for every role it names, and the library's default for the rest. */
        private fun read(
            host: TypedArray,
            defaults: TypedArray,
        ): CalculatorColors {
            fun named(index: Int): ColorStateList? = host.getColorStateList(index) ?: defaults.getColorStateList(index)

            fun role(
                index: Int,
                name: String,
            ): ColorStateList =
                checkNotNull(named(index)) {
                    "the library's default calculator style leaves $name unset"
                }

            val operatorText =
                role(
                    R.styleable.CurrencyCalculator_calculatorOperatorTextColor,
                    "calculatorOperatorTextColor",
                )
            return CalculatorColors(
                panelBackground =
                    role(
                        R.styleable.CurrencyCalculator_calculatorPanelBackgroundColor,
                        "calculatorPanelBackgroundColor",
                    ),
                panelStroke =
                    role(
                        R.styleable.CurrencyCalculator_calculatorPanelStrokeColor,
                        "calculatorPanelStrokeColor",
                    ),
                expressionText =
                    role(
                        R.styleable.CurrencyCalculator_calculatorExpressionTextColor,
                        "calculatorExpressionTextColor",
                    ),
                expressionBackground =
                    role(
                        R.styleable.CurrencyCalculator_calculatorExpressionBackgroundColor,
                        "calculatorExpressionBackgroundColor",
                    ),
                errorText =
                    role(
                        R.styleable.CurrencyCalculator_calculatorErrorTextColor,
                        "calculatorErrorTextColor",
                    ),
                keyText =
                    role(
                        R.styleable.CurrencyCalculator_calculatorKeyTextColor,
                        "calculatorKeyTextColor",
                    ),
                operatorText = operatorText,
                equalsBackground =
                    role(
                        R.styleable.CurrencyCalculator_calculatorEqualsBackgroundColor,
                        "calculatorEqualsBackgroundColor",
                    ),
                // The one role with no default of its own: a host that recolours the operators
                // gets the equals key with them, and only a host that fills the key names it.
                equalsText = named(R.styleable.CurrencyCalculator_calculatorEqualsTextColor) ?: operatorText,
            )
        }
    }
}
