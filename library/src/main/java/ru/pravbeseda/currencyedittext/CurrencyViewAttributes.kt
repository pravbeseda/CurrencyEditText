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
import android.util.AttributeSet
import ru.pravbeseda.currencyedittext.util.firstChar

/**
 * The XML attributes [CurrencyEditText] and [CurrencyMaterialEditText] have in common, read once
 * for either of them. The two views declare their own `declare-styleable` block, but an attribute
 * id is global, so reading the shared ones through `R.styleable.CurrencyEditText` resolves them on
 * either tag — and a default is written here once instead of once per view, which is what let the
 * two drift apart (issue #58).
 */
internal data class CurrencyViewAttributes(
    val currencySymbol: String,
    val useCurrencySymbolAsHint: Boolean,
    val localeTag: String?,
    val decimalSeparator: Char?,
    val groupingSeparator: Char?,
    val maxNumberOfDecimalPlaces: Int,
    val negativeValueAllow: Boolean,
    val decimalZerosPadding: Boolean,
    val emptyStringForZero: Boolean,
    val calculatorEnabled: Boolean,
) {
    companion object {
        fun read(
            context: Context,
            attrs: AttributeSet?,
        ): CurrencyViewAttributes =
            context.theme
                .obtainStyledAttributes(
                    attrs,
                    R.styleable.CurrencyEditText,
                    0,
                    0,
                ).run {
                    try {
                        CurrencyViewAttributes(
                            currencySymbol = getString(R.styleable.CurrencyEditText_currencySymbol).orEmpty(),
                            useCurrencySymbolAsHint =
                                getBoolean(R.styleable.CurrencyEditText_useCurrencySymbolAsHint, false),
                            localeTag = getString(R.styleable.CurrencyEditText_localeTag),
                            decimalSeparator =
                                getString(R.styleable.CurrencyEditText_decimalSeparator).firstChar(),
                            groupingSeparator =
                                getString(R.styleable.CurrencyEditText_groupingSeparator).firstChar(),
                            maxNumberOfDecimalPlaces =
                                getInt(R.styleable.CurrencyEditText_maxNumberOfDecimalPlaces, 2),
                            negativeValueAllow =
                                getBoolean(R.styleable.CurrencyEditText_negativeValueAllow, false),
                            decimalZerosPadding =
                                getBoolean(R.styleable.CurrencyEditText_decimalZerosPadding, false),
                            emptyStringForZero =
                                getBoolean(R.styleable.CurrencyEditText_emptyStringForZero, true),
                            calculatorEnabled =
                                getBoolean(R.styleable.CurrencyEditText_calculatorEnabled, false),
                        )
                    } finally {
                        recycle()
                    }
                }
    }
}
