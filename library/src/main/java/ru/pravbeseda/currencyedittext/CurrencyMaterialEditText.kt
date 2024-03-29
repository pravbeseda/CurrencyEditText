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
import ru.pravbeseda.currencyedittext.util.firstChar
import ru.pravbeseda.currencyedittext.util.getLocaleFromTag
import ru.pravbeseda.currencyedittext.util.isApi26AndAbove
import ru.pravbeseda.currencyedittext.util.isLollipopAndAbove

open class CurrencyMaterialEditText(context: Context, attrs: AttributeSet?) :
    TextInputLayout(context, attrs) {

    var text: Editable?
        get() {
            return editText.text
        }
        set(value) {
            editText.text = value
        }

    @set:JvmName("setValue0")
    @get:JvmName("getValue0")
    var value: BigDecimal
        set(value) {
            setValue(value)
        }
        get() {
            return getValue()
        }
    val isValid: Boolean
        get() = isValidState()

    private val editText = CurrencyEditText(context, null)

    init {
        var localeTag: String?
        var text: String?
        var negativeValueAllow: Boolean
        var selectAllOnFocus: Boolean
        var decimalSeparator: Char?
        var groupingSeparator: Char?
        var maxDecimalPlaces: Int
        var decimalZerosPadding: Boolean
        var emptyStringForZero: Boolean
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CurrencyMaterialEditText,
            0,
            0
        ).run {
            localeTag = getString(R.styleable.CurrencyMaterialEditText_localeTag)
            text = getString(R.styleable.CurrencyMaterialEditText_text)
            decimalSeparator =
                getString(R.styleable.CurrencyMaterialEditText_decimalSeparator).firstChar()
            groupingSeparator =
                getString(R.styleable.CurrencyMaterialEditText_groupingSeparator).firstChar()
            negativeValueAllow = getBoolean(
                R.styleable.CurrencyMaterialEditText_negativeValueAllow,
                false
            )
            decimalZerosPadding =
                getBoolean(R.styleable.CurrencyMaterialEditText_decimalZerosPadding, false)
            selectAllOnFocus =
                getBoolean(R.styleable.CurrencyMaterialEditText_selectAllOnFocus, false)
            maxDecimalPlaces =
                getInt(R.styleable.CurrencyMaterialEditText_maxNumberOfDecimalPlaces, 2)
            emptyStringForZero =
                getBoolean(R.styleable.CurrencyMaterialEditText_emptyStringForZero, true)
        }
        if (isLollipopAndAbove() && !localeTag.isNullOrBlank()) {
            setLocale(
                getLocaleFromTag(
                    localeTag!!
                )
            )
        }
        if (isApi26AndAbove()) {
            // bugfix api26
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        if (!text.isNullOrBlank()) setText(text!!)
        if (decimalSeparator !== null && groupingSeparator !== null) {
            setSeparators(
                groupingSeparator!!,
                decimalSeparator!!
            )
        }
        setNegativeValueAllow(negativeValueAllow)
        setSelectAllOnFocus(selectAllOnFocus)
        setMaxNumberOfDecimalPlaces(maxDecimalPlaces)
        setDecimalZerosPadding(decimalZerosPadding)
        setEmptyStringForZero(emptyStringForZero)
        this.addView(editText)
    }

    fun setValue(value: BigDecimal) {
        editText.setValue(value)
    }

    fun getValue(): BigDecimal {
        return editText.getValue()
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

    fun setSeparators(newGroupingSeparator: Char, newDecimalSeparator: Char) {
        editText.setSeparators(newGroupingSeparator, newDecimalSeparator)
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

    fun setNegativeValueAllow(allow: Boolean) {
        editText.setNegativeValueAllow(allow)
    }

    fun getDecimalZerosPadding(): Boolean {
        return editText.getDecimalZerosPadding()
    }

    fun setDecimalZerosPadding(padding: Boolean) {
        editText.setDecimalZerosPadding(padding)
    }

    fun getEmptyStringForZero(): Boolean {
        return editText.getEmptyStringForZero()
    }

    fun setEmptyStringForZero(newValue: Boolean) {
        editText.setEmptyStringForZero(newValue)
    }

    fun onValueChanged(
        action: (BigDecimal?, state: CurrencyEditText.Companion.State, textError: String) -> Unit
    ) {
        editText.onValueChanged(action)
    }

    fun setValidator(validator: ((BigDecimal) -> String?)?) {
        editText.setValidator(handleValidator(validator))
    }

    fun validate() {
        editText.validate()
    }

    fun isValidState(): Boolean {
        return editText.isValidState()
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
