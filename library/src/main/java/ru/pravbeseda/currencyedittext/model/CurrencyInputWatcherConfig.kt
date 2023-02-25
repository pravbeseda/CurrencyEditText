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
package ru.pravbeseda.currencyedittext.model

import java.util.*

data class CurrencyInputWatcherConfig(
    val locale: Locale = Locale.getDefault(),
    val currencySymbol: String = "",
    val decimalSeparator: Char? = null,
    val groupingSeparator: Char? = null,
    val maxNumberOfDecimalPlaces: Int = 2,
    val decimalZerosPadding: Boolean = false,
    val negativeValueAllow: Boolean = false,
    val onValueChanged: ((String?) -> Unit)? = null
)
