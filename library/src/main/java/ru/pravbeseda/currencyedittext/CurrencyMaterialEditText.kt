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

import android.content.Context
import android.text.Editable
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import java.math.BigDecimal

class CurrencyMaterialEditText(context: Context, attrs: AttributeSet?) :
    TextInputLayout(context, attrs) {

    var text: Editable?
        get() {
            return editText.text
        }
        set(value) {
            editText.text = value
        }

    private val editText: CurrencyEditText

    init {
        editText = CurrencyEditText(context, null)
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CurrencyMaterialEditText,
            0,
            0
        ).run {
            // editText.setText("11")
            editText.setText(getString(R.styleable.CurrencyMaterialEditText_text))
        }
        addView(editText)
    }

    fun setText(text: CharSequence) {
        editText.setText(text)
    }

    fun setNegativeValueAllow(allow: Boolean) {
        editText.setNegativeValueAllow(allow)
    }

    fun getNegativeValueAllow(): Boolean {
        return editText.getNegativeValueAllow()
    }

    fun setOnValueChanged(action: (BigDecimal?, state: CurrencyEditText.Companion.State) -> Unit) {
        editText.setOnValueChanged(action)
    }

    fun setValidator(validator: ((BigDecimal) -> String?)?) {
        editText.setValidator(handleValidator(validator))
    }

    // wrapper fo validate function to add some logic
    private fun handleValidator(validate: ((BigDecimal) -> String?)?): (BigDecimal) -> String? {
        return { input: BigDecimal ->
            val result = if (validate !== null) validate(input) else null
            if (result != null) {
                // set TextInputLayout property to show error description
                this.error = result.ifEmpty { null }
            }
            result
        }
    }
}
