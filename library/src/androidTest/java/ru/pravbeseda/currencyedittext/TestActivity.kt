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

import android.app.Activity
import android.os.Bundle
import android.view.WindowManager
import android.widget.FrameLayout

/** An empty window to attach fields to: a panel is a [android.widget.PopupWindow] and needs one. */
class TestActivity : Activity() {
    lateinit var container: FrameLayout
        private set

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // The keyboard would make the panel's placement wait for the frame to settle, and these
        // tests are about the guard, not about that wait.
        window.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
        container = FrameLayout(this)
        setContentView(container)
    }

    fun addField(): CurrencyEditText =
        CurrencyEditText(this, null).also {
            it.setCalculatorEnabled(true)
            container.addView(it)
        }
}
