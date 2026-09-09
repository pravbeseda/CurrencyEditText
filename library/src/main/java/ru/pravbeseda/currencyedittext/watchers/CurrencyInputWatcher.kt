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

    private val decimalSeparator: Char =
        if (config.decimalSeparator !== null) {
            config.decimalSeparator
        } else {
            DecimalFormatSymbols.getInstance(config.locale).decimalSeparator
        }
    private val groupingSeparator =
        if (config.groupingSeparator !== null) {
            config.groupingSeparator
        } else {
            DecimalFormatSymbols.getInstance(config.locale).groupingSeparator
        }

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
