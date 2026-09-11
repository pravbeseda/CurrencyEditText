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
package ru.pravbeseda.currencyedittext.watchers

import android.widget.EditText
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import java.lang.ref.WeakReference
import java.text.DecimalFormatSymbols

class CurrencyInputWatcher(
    private val editTextRef: WeakReference<EditText>,
    private val config: CurrencyInputWatcherConfig,
) : EasyTextWatcher() {
    init {
        if (config.maxNumberOfDecimalPlaces < 0) {
            throw IllegalArgumentException(
                "Maximum number of Decimal Places must be a positive integer",
            )
        }
    }

    private val editText: EditText? get() = editTextRef.get()

    private val separators = resolveSeparators(config)
    private val decimalSeparator: Char = separators.decimal
    private val groupingSeparator: Char = separators.grouping

    private val formatter = CurrencyTextFormatter(config, decimalSeparator, groupingSeparator)

    override fun onTextModified(
        newPartOfText: String?,
        newText: String?,
        oldText: String?,
        editPosition: Int?,
    ) {
        val formatted = formatter.format(newPartOfText, newText, oldText, editPosition)

        editText?.setText(formatted.text)
        editText?.setSelection(formatted.cursorPosition)

        config.onValueChanged?.invoke(formatted.text)
    }

    fun getDecimalSeparator(): Char = decimalSeparator

    fun getGroupingSeparator(): Char = groupingSeparator
}

/** The pair the field formats with, once the locale has filled in whatever the config left out. */
private data class Separators(
    val decimal: Char,
    val grouping: Char,
)

/**
 * Two equal separators leave a number unreadable — `1,234,56` reads as 123456 — so one of them
 * moves to the other of the point and the comma. The one the locale filled in yields, since the
 * one that was asked for is the caller's intent; where both were asked for, the decimal one does,
 * which is what `setSeparators` has always done.
 */
private fun resolveSeparators(config: CurrencyInputWatcherConfig): Separators {
    val symbols = DecimalFormatSymbols.getInstance(config.locale)
    val decimal = config.decimalSeparator ?: symbols.decimalSeparator
    val grouping = config.groupingSeparator ?: symbols.groupingSeparator

    return when {
        decimal != grouping -> Separators(decimal, grouping)
        config.groupingSeparator == null -> Separators(decimal, theOtherOf(decimal))
        else -> Separators(theOtherOf(grouping), grouping)
    }
}

private fun theOtherOf(separator: Char): Char = if (separator == '.') ',' else '.'
