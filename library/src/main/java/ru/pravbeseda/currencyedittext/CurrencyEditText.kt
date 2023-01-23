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
import android.graphics.Rect
import android.os.Build
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import androidx.annotation.RequiresApi
import androidx.appcompat.widget.AppCompatEditText
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ru.pravbeseda.currencyedittext.util.getLocaleFromTag
import ru.pravbeseda.currencyedittext.util.isLollipopAndAbove
import ru.pravbeseda.currencyedittext.util.parseMoneyValueWithLocale
import ru.pravbeseda.currencyedittext.watchers.CurrencyInputWatcher
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.*

class CurrencyEditText(
    context: Context,
    attrs: AttributeSet?
) : AppCompatEditText(context, attrs) {
    private lateinit var currencySymbolPrefix: String // lateinit is important!
    private var textWatcher: CurrencyInputWatcher
    private var locale: Locale = Locale.ENGLISH
    private var decimalSeparator: String? = null
    private var groupingSeparator: String? = null
    private var negativeValueAllow: Boolean = false
    private var maxDP: Int

    private val _liveTextError = MutableLiveData<String?>()
    val liveTextError: LiveData<String?> = _liveTextError

    private var onValueChanged: OnValueChanged? = null
    private var validator: ((BigDecimal) -> String?)? = null

    init {
        var useCurrencySymbolAsHint = false
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL
        keyListener = DigitsKeyListener.getInstance("0123456789.,-")
        var localeTag: String?
        val prefix: String
        context.theme.obtainStyledAttributes(
            attrs,
            R.styleable.CurrencyEditText,
            0, 0
        ).run {
            try {
                prefix = getString(R.styleable.CurrencyEditText_currencySymbol).orEmpty()
                localeTag = getString(R.styleable.CurrencyEditText_localeTag)
                decimalSeparator = getString(R.styleable.CurrencyEditText_decimalSeparator)
                groupingSeparator = getString(R.styleable.CurrencyEditText_groupingSeparator)
                useCurrencySymbolAsHint =
                    getBoolean(R.styleable.CurrencyEditText_useCurrencySymbolAsHint, false)
                maxDP = getInt(R.styleable.CurrencyEditText_maxNumberOfDecimalDigits, 2)
                negativeValueAllow =
                    getBoolean(R.styleable.CurrencyEditText_negativeValueAllow, false);
            } finally {
                recycle()
            }
        }
        currencySymbolPrefix = if (prefix.isBlank()) "" else "$prefix "
        if (useCurrencySymbolAsHint) hint = currencySymbolPrefix
        if (isLollipopAndAbove() && !localeTag.isNullOrBlank()) locale =
            getLocaleFromTag(localeTag!!)
        textWatcher = createTextWatcher()
        addTextChangedListener(textWatcher)
    }

    fun setLocale(locale: Locale) {
        this.locale = locale
        invalidateTextWatcher()
    }

    @RequiresApi(Build.VERSION_CODES.LOLLIPOP)
    fun setLocale(localeTag: String) {
        locale = getLocaleFromTag(localeTag)
        invalidateTextWatcher()
    }

    fun setDecimalSeparator(newSeparator: String) {
        decimalSeparator = newSeparator
        invalidateTextWatcher()
    }

    fun setGroupingSeparator(newSeparator: String) {
        groupingSeparator = newSeparator
        invalidateTextWatcher()
    }

    fun setNegativeValueAllow(newValue: Boolean) {
        negativeValueAllow = newValue
        invalidateTextWatcher()
    }

    fun setCurrencySymbol(currencySymbol: String, useCurrencySymbolAsHint: Boolean = false) {
        currencySymbolPrefix = "$currencySymbol "
        if (useCurrencySymbolAsHint) hint = currencySymbolPrefix
        invalidateTextWatcher()
    }

    fun setMaxNumberOfDecimalDigits(maxDP: Int) {
        this.maxDP = maxDP
        invalidateTextWatcher()
    }

    private fun invalidateTextWatcher() {
        removeTextChangedListener(textWatcher)
        textWatcher = createTextWatcher()
        addTextChangedListener(textWatcher)
    }

    private fun createTextWatcher(): CurrencyInputWatcher {
        return CurrencyInputWatcher(
            WeakReference(this),
            currencySymbolPrefix,
            locale,
            decimalSeparator,
            groupingSeparator,
            maxDP,
            negativeValueAllow
        ) {
            var state: State = State.OK
            val value = stringToBigDecimal(it)
            val textError = (validator?.let { it1 -> it1(value) })
            if (textError !== null) {
                state = State.ERROR
            }
            _liveTextError.value = textError
            onValueChanged?.onValueChanged(value, state)
        }
    }

    fun getNumericValue(): Double {
        return parseMoneyValueWithLocale(
            text.toString(),
            textWatcher.decimalFormatSymbols.groupingSeparator.toString(),
            textWatcher.decimalFormatSymbols.decimalSeparator.toString(),
            currencySymbolPrefix
        ).toDouble()
    }

    fun getNumericValueBigDecimal(): BigDecimal {
        return stringToBigDecimal(text.toString())
    }

    private fun stringToBigDecimal(str: String?): BigDecimal {
        return BigDecimal(
            parseMoneyValueWithLocale(
                str ?: "",
                textWatcher.decimalFormatSymbols.groupingSeparator.toString(),
                textWatcher.decimalFormatSymbols.decimalSeparator.toString(),
                currencySymbolPrefix
            ).toString()
        )
    }

    override fun setText(text: CharSequence?, type: BufferType?) {
        super.setText(text, type)
        getText()?.length?.let { setSelection(it) }
    }

    override fun onFocusChanged(focused: Boolean, direction: Int, previouslyFocusedRect: Rect?) {
        super.onFocusChanged(focused, direction, previouslyFocusedRect)
        if (focused) {
            removeTextChangedListener(textWatcher)
            addTextChangedListener(textWatcher)
            if (text.toString().isEmpty()) setText(currencySymbolPrefix)
        } else {
            removeTextChangedListener(textWatcher)
            if (text.toString() == currencySymbolPrefix) setText("")
        }
    }

    override fun onSelectionChanged(selStart: Int, selEnd: Int) {
        if (::currencySymbolPrefix.isInitialized.not()) return
        val symbolLength = currencySymbolPrefix.length
        if (selEnd < symbolLength && text.toString().length >= symbolLength) {
            setSelection(symbolLength)
        } else {
            super.onSelectionChanged(selStart, selEnd)
        }
    }

    fun setOnValueChanged(action: (BigDecimal?, state: State) -> Unit) {
        onValueChanged = object : OnValueChanged {
            override fun onValueChanged(newValue: BigDecimal?, state: State) {
                action.invoke(newValue, state)
            }
        }
    }

    fun setValidator(newValidator: ((BigDecimal) -> String?)?) {
        validator = newValidator
    }

    /**
     * Interface for value and state change callback
     */
    interface OnValueChanged {
        fun onValueChanged(newValue: BigDecimal?, state: State)
    }

    companion object {
        /**
         * Component's state values
         */
        enum class State {
            OK,             // Valid, not changed
            ERROR,          // Invalid value
            DIRTY           // Valid, not saved
        }
    }
}
