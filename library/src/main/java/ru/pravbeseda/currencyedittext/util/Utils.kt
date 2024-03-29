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
package ru.pravbeseda.currencyedittext.util

import android.os.Build
import androidx.annotation.RequiresApi
import java.math.BigDecimal
import java.text.DecimalFormat
import java.text.DecimalFormatSymbols
import java.text.NumberFormat
import java.text.ParseException
import java.util.IllformedLocaleException
import java.util.Locale

internal fun parseMoneyValue(
    value: String,
    groupingSeparator: Char,
    decimalSeparator: Char,
    currencySymbol: String
): String =
    value.replace(currencySymbol, "")
        .replace(groupingSeparator.toString(), "")
        .replace(decimalSeparator.toString(), ".")

internal fun parseMoneyValueWithLocale(
    value: String,
    groupingSeparator: Char,
    decimalSeparator: Char,
    currencySymbol: String
): Number {
    val valueWithoutSeparator =
        parseMoneyValue(value, groupingSeparator, decimalSeparator, currencySymbol)
    return try {
        NumberFormat.getInstance(Locale.ENGLISH).parse(valueWithoutSeparator)!!
    } catch (exception: ParseException) {
        0
    }
}

internal fun formatMoneyValue(
    value: BigDecimal,
    groupingSeparator: Char,
    decimalSeparator: Char
): String {
    val symbols = DecimalFormatSymbols(Locale.ROOT)
    symbols.decimalSeparator = decimalSeparator
    symbols.groupingSeparator = groupingSeparator
    val df = DecimalFormat("#,##0.##", symbols)
    return df.format(value)
}

@RequiresApi(Build.VERSION_CODES.LOLLIPOP)
internal fun getLocaleFromTag(localeTag: String): Locale {
    return try {
        Locale.Builder().setLanguageTag(localeTag).build()
    } catch (e: IllformedLocaleException) {
        Locale.getDefault()
    }
}

internal fun isLollipopAndAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP
internal fun isApi26AndAbove(): Boolean = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
