/**
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

import android.text.Editable
import android.text.TextWatcher
import android.widget.EditText
import ru.pravbeseda.currencyedittext.model.LocaleVars
import ru.pravbeseda.currencyedittext.watchers.CurrencyInputWatcher
import java.lang.ref.WeakReference
import java.util.*

fun TextWatcher.runAllWatcherMethods(
    s: Editable,
    start: Int = 0,
    count: Int = 0,
    before: Int = 0,
    after: Int = 0
) {
    beforeTextChanged(s, start, count, after)
    onTextChanged(s, start, before, count)
    afterTextChanged(s)
}

fun String.removeCharAt(i: Int) = removeRange(i..i)

fun String.removeLastChar() = removeCharAt(length - 1)

fun String.removeFirstChar() = removeCharAt(0)

fun String.toLocale(): Locale = Locale.Builder().setLanguageTag(this).build()

fun LocaleVars.toWatcher(
    editText: EditText,
    decimalPlaces: Int = 2,
    decimalSeparator: String? = null,
    groupingSeparator: String? = null,
    negativeValueAllow: Boolean
) = CurrencyInputWatcher(
    WeakReference(editText),
    currencySymbol,
    tag.toLocale(),
    decimalSeparator,
    groupingSeparator,
    decimalPlaces,
    negativeValueAllow
)
