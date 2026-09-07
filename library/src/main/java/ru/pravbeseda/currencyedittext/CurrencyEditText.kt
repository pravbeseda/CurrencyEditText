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

import android.content.Context
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.InputType
import android.text.method.DigitsKeyListener
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import androidx.appcompat.content.res.AppCompatResources
import androidx.appcompat.widget.AppCompatEditText
import ru.pravbeseda.currencyedittext.calculator.CalculatorPopup
import ru.pravbeseda.currencyedittext.model.CurrencyInputWatcherConfig
import ru.pravbeseda.currencyedittext.util.firstChar
import ru.pravbeseda.currencyedittext.util.formatMoneyValue
import ru.pravbeseda.currencyedittext.util.getLocaleFromTag
import ru.pravbeseda.currencyedittext.util.parseMoneyValueWithLocale
import ru.pravbeseda.currencyedittext.watchers.CurrencyInputWatcher
import java.lang.ref.WeakReference
import java.math.BigDecimal
import java.util.Locale

open class CurrencyEditText(
    context: Context,
    attrs: AttributeSet?,
) : AppCompatEditText(context, attrs) {
    private lateinit var currencySymbolPrefix: String // lateinit is important!
    private var textWatcher: CurrencyInputWatcher
    private var locale: Locale = Locale.getDefault()
    private var decimalSeparator: Char? = null
    private var groupingSeparator: Char? = null
    private var negativeValueAllow: Boolean = false
    private var decimalZerosPadding: Boolean = false
    private var emptyStringForZero: Boolean = true
    private var maxDecimalPlaces: Int
    private var calculatorEnabled: Boolean = false
    private var calculatorIcon: Drawable? = null

    private var onValueChanged: OnValueChanged? = null
    private var validator: ((BigDecimal) -> String?)? = null
    private var state: State = State.OK
    private var textError: String = ""

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

    init {
        var useCurrencySymbolAsHint = false
        inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_FLAG_DECIMAL or InputType.TYPE_NUMBER_FLAG_SIGNED
        keyListener = DigitsKeyListener.getInstance("0123456789.,-")
        textDirection = TEXT_DIRECTION_LTR
        var localeTag: String?
        val prefix: String
        context.theme
            .obtainStyledAttributes(
                attrs,
                R.styleable.CurrencyEditText,
                0,
                0,
            ).run {
                try {
                    prefix = getString(R.styleable.CurrencyEditText_currencySymbol).orEmpty()
                    localeTag = getString(R.styleable.CurrencyEditText_localeTag)
                    decimalSeparator =
                        getString(R.styleable.CurrencyEditText_decimalSeparator).firstChar()
                    groupingSeparator =
                        getString(R.styleable.CurrencyEditText_groupingSeparator).firstChar()
                    useCurrencySymbolAsHint =
                        getBoolean(R.styleable.CurrencyEditText_useCurrencySymbolAsHint, false)
                    maxDecimalPlaces = getInt(R.styleable.CurrencyEditText_maxNumberOfDecimalPlaces, 2)
                    negativeValueAllow =
                        getBoolean(R.styleable.CurrencyEditText_negativeValueAllow, false)
                    decimalZerosPadding =
                        getBoolean(R.styleable.CurrencyEditText_decimalZerosPadding, false)
                    emptyStringForZero =
                        getBoolean(R.styleable.CurrencyEditText_emptyStringForZero, true)
                    calculatorEnabled =
                        getBoolean(R.styleable.CurrencyEditText_calculatorEnabled, false)
                } finally {
                    recycle()
                }
            }
        currencySymbolPrefix = if (prefix.isBlank()) "" else "$prefix "
        if (useCurrencySymbolAsHint) hint = currencySymbolPrefix
        if (!localeTag.isNullOrBlank()) {
            locale = getLocaleFromTag(localeTag!!)
        }
        textWatcher = createTextWatcher()
        this.addTextChangedListener(textWatcher)
        text = this.text // to apply text watcher formatting
        if (calculatorEnabled) updateCalculatorButton()
    }

    fun setValue(value: BigDecimal) {
        if (emptyStringForZero && value == BigDecimal.ZERO) {
            setText("")
            return
        }
        setText(
            formatMoneyValue(
                value,
                textWatcher.getGroupingSeparator(),
                textWatcher.getDecimalSeparator(),
            ),
        )
    }

    /** The number in the field. A field holding no number — an empty one — reads as zero. */
    fun getValue(): BigDecimal = stringToBigDecimal(text.toString())

    fun setLocale(locale: Locale) {
        val value = getValue()
        setText("")
        this.locale = locale
        invalidateTextWatcher()
        setValue(value)
    }

    fun setLocale(localeTag: String) {
        val value = getValue()
        setText("")
        locale = getLocaleFromTag(localeTag)
        invalidateTextWatcher()
        setValue(value)
    }

    fun setSeparators(
        newGroupingSeparator: Char,
        newDecimalSeparator: Char,
    ) {
        var decimal = newDecimalSeparator
        if (newGroupingSeparator == newDecimalSeparator) {
            decimal = if (newDecimalSeparator == '.') ',' else '.'
        }
        val value = getValue()
        setText("")
        decimalSeparator = decimal
        groupingSeparator = newGroupingSeparator
        invalidateTextWatcher()
        setValue(value)
    }

    fun getDecimalSeparator(): Char = textWatcher.getDecimalSeparator()

    fun getGroupingSeparator(): Char = textWatcher.getGroupingSeparator()

    fun setNegativeValueAllow(newValue: Boolean) {
        negativeValueAllow = newValue
        invalidateTextWatcher()
    }

    fun getNegativeValueAllow(): Boolean = negativeValueAllow

    fun setDecimalZerosPadding(newValue: Boolean) {
        decimalZerosPadding = newValue
        invalidateTextWatcher()
    }

    fun getDecimalZerosPadding(): Boolean = decimalZerosPadding

    fun setEmptyStringForZero(newValue: Boolean) {
        emptyStringForZero = newValue
        setValue(getValue())
    }

    fun getEmptyStringForZero(): Boolean = emptyStringForZero

    fun setCurrencySymbol(
        currencySymbol: String,
        useCurrencySymbolAsHint: Boolean = false,
    ) {
        currencySymbolPrefix = "$currencySymbol "
        if (useCurrencySymbolAsHint) hint = currencySymbolPrefix
        invalidateTextWatcher()
    }

    fun setMaxNumberOfDecimalPlaces(maxDP: Int) {
        this.maxDecimalPlaces = maxDP
        invalidateTextWatcher()
    }

    fun isValidState(): Boolean = state == State.OK

    /**
     * Shows or hides the calculator button. The panel reads the field's configuration when it
     * opens, so this changes nothing about the watcher and must not invalidate it.
     */
    fun setCalculatorEnabled(enabled: Boolean) {
        if (calculatorEnabled == enabled) return
        calculatorEnabled = enabled
        updateCalculatorButton()
    }

    fun isCalculatorEnabled(): Boolean = calculatorEnabled

    /**
     * Opens the calculator panel under [anchor], which is the field itself unless it sits inside a
     * [CurrencyMaterialEditText] — there the panel hangs below the whole layout.
     */
    internal fun showCalculator(anchor: View) {
        CalculatorPopup(
            anchor = anchor,
            decimalSeparator = getDecimalSeparator(),
            scale = maxDecimalPlaces,
            negativeValueAllow = negativeValueAllow,
            initialValue = getValue(),
            onAccept = ::setValue,
        ).show()
    }

    /** The button takes the end slot only; whatever the host put in the other three stays. */
    private fun updateCalculatorButton() {
        val drawables = compoundDrawablesRelative
        calculatorIcon =
            if (calculatorEnabled) {
                AppCompatResources.getDrawable(context, R.drawable.currency_edit_text_ic_calculator)
            } else {
                null
            }
        setCompoundDrawablesRelativeWithIntrinsicBounds(
            drawables[START_DRAWABLE],
            drawables[TOP_DRAWABLE],
            calculatorIcon,
            drawables[BOTTOM_DRAWABLE],
        )
    }

    /** A tap opens the panel only where the field is live and the button is the one this view drew. */
    internal fun opensCalculatorOn(event: MotionEvent): Boolean =
        isEnabled &&
            calculatorEnabled &&
            event.action == MotionEvent.ACTION_UP &&
            isTouchOnCalculatorButton(event.x)

    private fun isTouchOnCalculatorButton(x: Float): Boolean {
        val iconWidth = calculatorIcon?.bounds?.width() ?: return false
        return if (layoutDirection == LAYOUT_DIRECTION_RTL) {
            x <= paddingLeft + iconWidth
        } else {
            x >= width - paddingRight - iconWidth
        }
    }

    private fun invalidateTextWatcher() {
        removeTextChangedListener(textWatcher)
        textWatcher = createTextWatcher()
        addTextChangedListener(textWatcher)
    }

    private fun createTextWatcher(): CurrencyInputWatcher {
        val config =
            CurrencyInputWatcherConfig(
                locale = locale,
                currencySymbol = currencySymbolPrefix,
                decimalSeparator = decimalSeparator,
                groupingSeparator = groupingSeparator,
                maxNumberOfDecimalPlaces = maxDecimalPlaces,
                negativeValueAllow = negativeValueAllow,
                decimalZerosPadding = decimalZerosPadding,
                onValueChanged = {
                    val value = stringToBigDecimal(it)
                    validate(value)
                    onValueChanged?.onValueChanged(value, state, textError)
                },
            )
        return CurrencyInputWatcher(
            WeakReference(this),
            config,
        )
    }

    fun validate(value: BigDecimal? = null) {
        val checkedValue = value ?: getValue()
        textError = (validator?.let { it1 -> it1(checkedValue) }) ?: ""
        state =
            if (textError.isEmpty()) {
                State.OK
            } else {
                State.ERROR
            }
    }

    private fun stringToBigDecimal(str: String?): BigDecimal =
        BigDecimal(
            parseMoneyValueWithLocale(
                str ?: "",
                textWatcher.getGroupingSeparator(),
                textWatcher.getDecimalSeparator(),
                currencySymbolPrefix,
            ).toString(),
        )

    override fun setText(
        text: CharSequence?,
        type: BufferType?,
    ) {
        super.setText(text, type)
        getText()?.length?.let { setSelection(it) }
    }

    /**
     * The calculator button is a compound drawable rather than a view of its own, so the tap on it
     * is recognised here, by where it lands. Everything else is ordinary text editing.
     */
    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (opensCalculatorOn(event)) {
            performClick()
            showCalculator(this)
            return true
        }
        return super.onTouchEvent(event)
    }

    /** Overridden only because [onTouchEvent] is: an accessibility click has to reach the view. */
    override fun performClick(): Boolean = super.performClick()

    override fun onFocusChanged(
        focused: Boolean,
        direction: Int,
        previouslyFocusedRect: Rect?,
    ) {
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

    override fun onSelectionChanged(
        selStart: Int,
        selEnd: Int,
    ) {
        if (::currencySymbolPrefix.isInitialized.not()) return
        val symbolLength = currencySymbolPrefix.length
        if (selEnd < symbolLength && text.toString().length >= symbolLength) {
            setSelection(symbolLength)
        } else {
            super.onSelectionChanged(selStart, selEnd)
        }
    }

    fun onValueChanged(action: (BigDecimal, state: State, textError: String) -> Unit) {
        val that = this
        onValueChanged =
            object : OnValueChanged {
                override fun onValueChanged(
                    newValue: BigDecimal,
                    state: State,
                    textError: String,
                ) {
                    that.state = state
                    action.invoke(newValue, state, textError)
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
        fun onValueChanged(
            newValue: BigDecimal,
            state: State,
            textError: String,
        )
    }

    companion object {
        /** Indices in `compoundDrawablesRelative`. */
        private const val START_DRAWABLE = 0
        private const val TOP_DRAWABLE = 1
        private const val BOTTOM_DRAWABLE = 3

        /**
         * Component's state values
         */
        enum class State {
            OK, // Valid
            ERROR, // Invalid value
        }
    }
}
