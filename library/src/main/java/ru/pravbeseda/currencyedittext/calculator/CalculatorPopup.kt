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
package ru.pravbeseda.currencyedittext.calculator

import android.graphics.Rect
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.PopupWindow
import android.widget.TextView
import ru.pravbeseda.currencyedittext.R
import java.math.BigDecimal

/** Which key each button of the panel presses. The button's label comes from the key itself. */
private val KEY_BUTTONS =
    mapOf(
        R.id.currency_calculator_key_0 to CalculatorKey.DIGIT_0,
        R.id.currency_calculator_key_1 to CalculatorKey.DIGIT_1,
        R.id.currency_calculator_key_2 to CalculatorKey.DIGIT_2,
        R.id.currency_calculator_key_3 to CalculatorKey.DIGIT_3,
        R.id.currency_calculator_key_4 to CalculatorKey.DIGIT_4,
        R.id.currency_calculator_key_5 to CalculatorKey.DIGIT_5,
        R.id.currency_calculator_key_6 to CalculatorKey.DIGIT_6,
        R.id.currency_calculator_key_7 to CalculatorKey.DIGIT_7,
        R.id.currency_calculator_key_8 to CalculatorKey.DIGIT_8,
        R.id.currency_calculator_key_9 to CalculatorKey.DIGIT_9,
        R.id.currency_calculator_key_decimal to CalculatorKey.DECIMAL,
        R.id.currency_calculator_key_plus to CalculatorKey.PLUS,
        R.id.currency_calculator_key_minus to CalculatorKey.MINUS,
        R.id.currency_calculator_key_multiply to CalculatorKey.MULTIPLY,
        R.id.currency_calculator_key_divide to CalculatorKey.DIVIDE,
        R.id.currency_calculator_key_sign to CalculatorKey.SIGN,
        R.id.currency_calculator_key_backspace to CalculatorKey.BACKSPACE,
        R.id.currency_calculator_key_clear to CalculatorKey.CLEAR,
    )

private const val EQUALS_LABEL = "="

/**
 * The calculator panel: the only class here that touches Android. It inflates the keys, hands each
 * press to [CalculatorState] and, on `=`, gives the value back to the field through [onAccept].
 *
 * An expression that does not evaluate — incomplete, dividing by zero, or negative where the field
 * takes no negative values — leaves the panel open in an error state and writes nothing.
 */
internal class CalculatorPopup(
    val anchor: View,
    private val decimalSeparator: Char,
    private val scale: Int,
    private val negativeValueAllow: Boolean,
    initialValue: BigDecimal,
    private val onAccept: (BigDecimal) -> Unit,
) {
    private val colors = CalculatorColors.of(anchor.context)
    private var state = CalculatorState.seededWith(initialValue)
    private var awaitingPlacement = true
    private lateinit var expressionView: TextView
    lateinit var window: PopupWindow
        private set

    /**
     * True from the moment the panel is asked for until it has left the screen. Placement waits for
     * the keyboard to go, so a panel can be on its way and not yet showing, and the field that owns
     * it must count that window as open too. It starts true, so the popup is only read back once
     * [show] has built it.
     */
    val isActive: Boolean get() = awaitingPlacement || window.isShowing

    fun show() {
        val content =
            LayoutInflater
                .from(anchor.context)
                .inflate(R.layout.currency_calculator_panel, FrameLayout(anchor.context), false)
        bindKeys(content)
        expressionView =
            content.findViewById<TextView>(R.id.currency_calculator_expression).apply {
                setBackgroundColor(colors.expressionBackground.defaultColor)
            }
        render(failed = false)
        window =
            PopupWindow(content, panelWidth(), ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                setBackgroundDrawable(colors.panelDrawable(anchor.context))
                isOutsideTouchable = true
                elevation = anchor.resources.displayMetrics.density * POPUP_ELEVATION_DP
            }
        placeOnceTheKeyboardIsOutOfTheWay(content)
    }

    /**
     * The panel is measured against the visible window frame, and the popup itself takes focus and
     * so closes the keyboard: measured before that, the panel is placed against a screen that no
     * longer exists — flipped above the field and capped to the room the keyboard left. Ask the
     * keyboard to close first, and place the panel once the frame has answered.
     */
    private fun placeOnceTheKeyboardIsOutOfTheWay(content: View) {
        val frameWithKeyboard = visibleFrame()
        val keyboardIsClosing =
            anchor.context
                .getSystemService(InputMethodManager::class.java)
                ?.hideSoftInputFromWindow(anchor.windowToken, 0) == true
        if (!keyboardIsClosing) {
            awaitingPlacement = false
            showAnchored(content)
            return
        }

        var onLayout: ViewTreeObserver.OnGlobalLayoutListener? = null
        val place = {
            if (awaitingPlacement) {
                awaitingPlacement = false
                onLayout?.let { anchor.viewTreeObserver.removeOnGlobalLayoutListener(it) }
                if (anchor.isAttachedToWindow) showAnchored(content)
            }
        }
        onLayout =
            ViewTreeObserver.OnGlobalLayoutListener {
                if (visibleFrame() != frameWithKeyboard) place()
            }
        anchor.viewTreeObserver.addOnGlobalLayoutListener(onLayout)
        // A keyboard that was already on its way out, or refuses to go, must not hold the panel back.
        anchor.postDelayed({ place() }, KEYBOARD_CLOSE_TIMEOUT_MS)
    }

    private fun visibleFrame(): Rect = Rect().also { anchor.getWindowVisibleDisplayFrame(it) }

    /** Measures the panel, asks [CalculatorPlacement] where it goes, and shows it there. */
    private fun showAnchored(content: View) {
        val visible = visibleFrame()
        val anchorTop = IntArray(2).also { anchor.getLocationOnScreen(it) }[1]
        val wanted = measureHeight(content)
        val placement =
            CalculatorPlacement.choose(
                spaceBelow = visible.bottom - (anchorTop + anchor.height),
                spaceAbove = anchorTop - visible.top,
                wanted = wanted,
            )

        window.height = placement.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        if (placement.above) {
            window.showAsDropDown(anchor, 0, -(anchor.height + (placement.height ?: wanted)))
        } else {
            window.showAsDropDown(anchor)
        }
    }

    private fun measureHeight(content: View): Int {
        val width = panelWidth()
        val widthSpec =
            if (width > 0) {
                View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
            } else {
                View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
            }
        content.measure(widthSpec, View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED))
        return content.measuredHeight
    }

    /** The panel is as wide as the field it belongs to, unless that field is not laid out yet. */
    private fun panelWidth(): Int = if (anchor.width > 0) anchor.width else ViewGroup.LayoutParams.WRAP_CONTENT

    private fun bindKeys(content: View) {
        KEY_BUTTONS.forEach { (id, key) ->
            content.findViewById<Button>(id).apply {
                text = key.label(decimalSeparator)
                setTextColor(if (key.isOperator) colors.operatorText else colors.keyText)
                // Kept visible but dead, so the panel does not change shape between fields.
                isEnabled = key != CalculatorKey.SIGN || negativeValueAllow
                setOnClickListener { press(key) }
            }
        }
        content.findViewById<Button>(R.id.currency_calculator_key_equals).apply {
            text = EQUALS_LABEL
            setTextColor(colors.equalsText)
            // The ripple moves on top of the fill, and only once the fill is in place: setting a
            // background clears the callback of the one it replaces, and that is this ripple.
            val ripple = background
            setBackgroundColor(colors.equalsBackground.defaultColor)
            foreground = ripple
            setOnClickListener { accept() }
        }
    }

    private fun press(key: CalculatorKey) {
        state = state.press(key)
        render(failed = false)
    }

    private fun accept() {
        when (val result = state.result(scale, negativeValueAllow)) {
            is EvalResult.Success -> {
                window.dismiss()
                onAccept(result.value)
            }

            EvalResult.Failure -> {
                render(failed = true)
            }
        }
    }

    private fun render(failed: Boolean) {
        expressionView.text = state.display(decimalSeparator)
        expressionView.setTextColor(if (failed) colors.errorText else colors.expressionText)
    }

    private companion object {
        const val POPUP_ELEVATION_DP = 8f
        const val KEYBOARD_CLOSE_TIMEOUT_MS = 300L
    }
}
