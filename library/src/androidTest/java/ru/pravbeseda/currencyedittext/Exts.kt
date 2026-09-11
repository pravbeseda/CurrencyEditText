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
import android.view.ContextThemeWrapper
import android.view.LayoutInflater
import android.view.View
import androidx.test.platform.app.InstrumentationRegistry
import ru.pravbeseda.currencyedittext.test.R

/**
 * Inflates the layout that carries `currencySymbol` and `useCurrencySymbolAsHint`, and hands back
 * the view with [viewId]. Only an inflated view is given the attributes; a constructed one is not.
 */
internal fun <T : View> inflateCurrencySymbolAttrs(
    context: Context,
    viewId: Int,
): T {
    val themed =
        ContextThemeWrapper(
            context,
            com.google.android.material.R.style.Theme_MaterialComponents_Light,
        )
    lateinit var view: T
    InstrumentationRegistry.getInstrumentation().runOnMainSync {
        view =
            LayoutInflater
                .from(themed)
                .inflate(R.layout.currency_symbol_attrs, null)
                .findViewById(viewId)
    }
    return view
}
