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
import android.os.Build
import android.text.Editable
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import com.google.android.material.textfield.TextInputLayout
import java.math.BigDecimal
import java.util.*
import ru.pravbeseda.currencyedittext.util.getLocaleFromTag
import ru.pravbeseda.currencyedittext.util.isLollipopAndAbove

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
        var localeTag: String?
        var text: String?
        var negativeValueAllow: Boolean
        var selectAllOnFocus: Boolean
        var decimalSeparator: String?
        var groupingSeparator: String?
        var maxDecimalPlaces: Int
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CurrencyMaterialEditText,
            0,
            0
        ).run {
            localeTag = getString(R.styleable.CurrencyMaterialEditText_localeTag)
            text = getString(R.styleable.CurrencyMaterialEditText_text)
            decimalSeparator = getString(R.styleable.CurrencyMaterialEditText_decimalSeparator)
            groupingSeparator = getString(R.styleable.CurrencyMaterialEditText_groupingSeparator)
            negativeValueAllow = getBoolean(
                R.styleable.CurrencyMaterialEditText_negativeValueAllow,
                false
            )
            selectAllOnFocus =
                getBoolean(R.styleable.CurrencyMaterialEditText_selectAllOnFocus, false)
            maxDecimalPlaces =
                getInt(R.styleable.CurrencyMaterialEditText_maxNumberOfDecimalPlaces, 2)
        }
        if (isLollipopAndAbove() && !localeTag.isNullOrBlank()) {
            setLocale(
                getLocaleFromTag(
                    localeTag!!
                )
            )
        }
        setNegativeValueAllow(negativeValueAllow)
        if (!text.isNullOrBlank()) setText(text!!)
        if (!decimalSeparator.isNullOrBlank()) setDecimalSeparator(decimalSeparator!!)
        if (groupingSeparator !== null) setGroupingSeparator(groupingSeparator!!)
        setSelectAllOnFocus(selectAllOnFocus)
        setMaxNumberOfDecimalPlaces(maxDecimalPlaces)
        addView(editText)
    }

    fun setValue(value: BigDecimal) {
        editText.setValue(value)
    }

    fun setText(text: CharSequence) {
        editText.setText(text)
    }

    fun setLocale(locale: Locale) {
        editText.setLocale(locale)
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setLocale(localeTag: String) {
        editText.setLocale(localeTag)
    }

    fun setDecimalSeparator(newSeparator: String) {
        editText.setDecimalSeparator(newSeparator)
    }

    fun setGroupingSeparator(newSeparator: String) {
        editText.setGroupingSeparator(newSeparator)
    }

    fun setNegativeValueAllow(allow: Boolean) {
        editText.setNegativeValueAllow(allow)
    }

    fun setSelectAllOnFocus(selectOnFocus: Boolean) {
        editText.setSelectAllOnFocus(selectOnFocus)
    }

    fun setMaxNumberOfDecimalPlaces(maxDecimalPlaces: Int) {
        editText.setMaxNumberOfDecimalPlaces(maxDecimalPlaces)
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
