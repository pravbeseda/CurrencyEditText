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
import android.text.Editable
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import ru.pravbeseda.currencyedittext.util.getLocaleFromTag
import ru.pravbeseda.currencyedittext.util.isApi26AndAbove
import java.math.BigDecimal
import java.util.Locale

open class CurrencyMaterialEditText(
    context: Context,
    attrs: AttributeSet?,
) : TextInputLayout(context, attrs) {
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
    private var calculatorEnabled: Boolean = false

    init {
        val attributes = CurrencyViewAttributes.read(context, attrs)
        val text: String?
        val selectAllOnFocus: Boolean
        context.theme
            .obtainStyledAttributes(
                attrs,
                R.styleable.CurrencyMaterialEditText,
                0,
                0,
            ).run {
                try {
                    // The two attributes this layout has and the field inside it does not.
                    text = getString(R.styleable.CurrencyMaterialEditText_text)
                    selectAllOnFocus =
                        getBoolean(R.styleable.CurrencyMaterialEditText_selectAllOnFocus, false)
                } finally {
                    recycle()
                }
            }
        if (!attributes.localeTag.isNullOrBlank()) {
            setLocale(getLocaleFromTag(attributes.localeTag))
        }
        if (isApi26AndAbove()) {
            // bugfix api26
            importantForAutofill = IMPORTANT_FOR_AUTOFILL_NO_EXCLUDE_DESCENDANTS
        }
        // Attached before it is configured: the layout takes the field's hint as its floating label
        // while attaching it, and the currency symbol belongs inside the field, not on that label.
        this.addView(editText)
        setCurrencySymbol(attributes.currencySymbol, attributes.useCurrencySymbolAsHint)
        if (!text.isNullOrBlank()) setText(text)
        editText.applySeparators(attributes.groupingSeparator, attributes.decimalSeparator)
        setNegativeValueAllow(attributes.negativeValueAllow)
        setSelectAllOnFocus(selectAllOnFocus)
        setMaxNumberOfDecimalPlaces(attributes.maxNumberOfDecimalPlaces)
        setDecimalZerosPadding(attributes.decimalZerosPadding)
        setEmptyStringForZero(attributes.emptyStringForZero)
        // Only when asked for: the end icon belongs to the host until the calculator claims it.
        if (attributes.calculatorEnabled) setCalculatorEnabled(true)
    }

    fun setValue(value: BigDecimal) {
        editText.setValue(value)
    }

    /** The number in the field. A field holding no number — an empty one — reads as zero. */
    fun getValue(): BigDecimal = editText.getValue()

    fun setText(text: CharSequence) {
        editText.setText(text)
    }

    fun setLocale(locale: Locale) {
        editText.setLocale(locale)
    }

    fun setLocale(localeTag: String) {
        editText.setLocale(localeTag)
    }

    fun setSeparators(
        newGroupingSeparator: Char,
        newDecimalSeparator: Char,
    ) {
        editText.setSeparators(newGroupingSeparator, newDecimalSeparator)
    }

    fun setCurrencySymbol(
        currencySymbol: String,
        useCurrencySymbolAsHint: Boolean = false,
    ) {
        editText.setCurrencySymbol(currencySymbol, useCurrencySymbolAsHint)
    }

    fun setSelectAllOnFocus(selectOnFocus: Boolean) {
        editText.setSelectAllOnFocus(selectOnFocus)
    }

    fun setMaxNumberOfDecimalPlaces(maxDecimalPlaces: Int) {
        editText.setMaxNumberOfDecimalPlaces(maxDecimalPlaces)
    }

    fun getNegativeValueAllow(): Boolean = editText.getNegativeValueAllow()

    fun setNegativeValueAllow(allow: Boolean) {
        editText.setNegativeValueAllow(allow)
    }

    fun getDecimalZerosPadding(): Boolean = editText.getDecimalZerosPadding()

    fun setDecimalZerosPadding(padding: Boolean) {
        editText.setDecimalZerosPadding(padding)
    }

    fun getEmptyStringForZero(): Boolean = editText.getEmptyStringForZero()

    fun setEmptyStringForZero(newValue: Boolean) {
        editText.setEmptyStringForZero(newValue)
    }

    /**
     * Shows or hides the calculator button. Unlike [CurrencyEditText], which draws it as a
     * compound drawable, the layout renders it as its own end icon. The panel hangs from the field
     * inside the layout rather than from the layout itself, whose bottom sits some 21dp lower, past
     * the strip kept for the error text; an error there is covered while the panel is up. It is
     * aligned to the layout, though, since the end icon it belongs to sits outside the field.
     */
    fun setCalculatorEnabled(enabled: Boolean) {
        // Nothing to do when the state does not change: the end icon is the host's until the
        // calculator claims it, and switching a calculator off that was never on must not take it.
        if (calculatorEnabled == enabled) return
        calculatorEnabled = enabled
        if (enabled) {
            endIconMode = END_ICON_CUSTOM
            setEndIconDrawable(R.drawable.currency_edit_text_ic_calculator)
            setEndIconContentDescription(R.string.currency_edit_text_calculator)
            setEndIconOnClickListener { editText.showCalculator(this) }
        } else {
            setEndIconOnClickListener(null)
            endIconMode = END_ICON_NONE
        }
    }

    fun isCalculatorEnabled(): Boolean = calculatorEnabled

    fun onValueChanged(action: (BigDecimal?, state: CurrencyEditText.Companion.State, textError: String) -> Unit) {
        editText.onValueChanged(action)
    }

    fun setValidator(validator: ((BigDecimal) -> String?)?) {
        editText.setValidator(handleValidator(validator))
    }

    fun validate() {
        editText.validate()
    }

    fun isValidState(): Boolean = editText.isValidState()

    // wrapper fo validate function to add some logic
    private fun handleValidator(validate: ((BigDecimal) -> String?)?): (BigDecimal) -> String? =
        { input: BigDecimal ->
            val result = if (validate !== null) validate(input) else null
            if (result != null) {
                // set TextInputLayout property to show error description
                this.error = result.ifEmpty { null }
            }
            result
        }
}
