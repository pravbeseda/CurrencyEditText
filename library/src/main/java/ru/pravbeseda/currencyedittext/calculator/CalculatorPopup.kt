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
package ru.pravbeseda.currencyedittext.calculator

import android.graphics.Rect
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
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
        R.id.currency_calculator_key_parenthesis to CalculatorKey.PARENTHESIS,
        R.id.currency_calculator_key_sign to CalculatorKey.SIGN,
        R.id.currency_calculator_key_backspace to CalculatorKey.BACKSPACE,
        R.id.currency_calculator_key_clear to CalculatorKey.CLEAR,
    )

private const val EQUALS_LABEL = "="

/** What the panel needs to know about the field it was opened from. */
internal data class CalculatorField(
    val decimalSeparator: Char,
    val scale: Int,
    val negativeValueAllow: Boolean,
    val value: BigDecimal,
)

/**
 * The calculator panel: the only class here that touches Android. It inflates the keys, hands each
 * press to [CalculatorState] and, on `=`, gives the value back to the field through [onAccept].
 *
 * An expression that does not evaluate — incomplete, dividing by zero, or negative where the field
 * takes no negative values — leaves the panel open in an error state and writes nothing.
 *
 * The panel hangs under [anchor] and hugs the end edge of [alignTo], which is the same view for a
 * plain field and the layout around it for a `CurrencyMaterialEditText`, whose calculator button
 * is an end icon sitting outside the field.
 */
internal class CalculatorPopup(
    val anchor: View,
    val alignTo: View,
    private val field: CalculatorField,
    private val onAccept: (BigDecimal) -> Unit,
) {
    private val style = CalculatorStyle.of(anchor.context)
    private var state = CalculatorState.seededWith(field.value)
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
        buildKeypad(content)
        expressionView =
            content.findViewById<TextView>(R.id.currency_calculator_expression).apply {
                setBackgroundColor(style.expressionBackground.defaultColor)
            }
        render(failed = false)
        window =
            PopupWindow(content, ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT, true).apply {
                setBackgroundDrawable(style.panelDrawable(anchor.context))
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
        val unconstrained = View.MeasureSpec.makeMeasureSpec(0, View.MeasureSpec.UNSPECIFIED)
        content.measure(unconstrained, unconstrained)
        val wanted = content.measuredHeight
        val placement =
            CalculatorPlacement.choose(
                spaceBelow = visible.bottom - (anchorTop + anchor.height),
                spaceAbove = anchorTop - visible.top,
                wanted = wanted,
            )

        // An exact width, not WRAP_CONTENT: aligning the end edges is arithmetic on the width the
        // popup was given, and PopupWindow does that arithmetic with the constant itself.
        window.width = content.measuredWidth
        window.height = placement.height ?: ViewGroup.LayoutParams.WRAP_CONTENT
        val below = if (placement.above) -(anchor.height + (placement.height ?: wanted)) else 0
        window.showAsDropDown(anchor, alignmentOffset(), below, Gravity.END)
    }

    /** Reads the two views off the screen and asks [CalculatorPlacement] for the offset between. */
    private fun alignmentOffset(): Int =
        CalculatorPlacement.endOffset(
            anchorStart = IntArray(2).also { anchor.getLocationOnScreen(it) }[0],
            anchorWidth = anchor.width,
            alignStart = IntArray(2).also { alignTo.getLocationOnScreen(it) }[0],
            alignWidth = alignTo.width,
            rightToLeft = anchor.layoutDirection == View.LAYOUT_DIRECTION_RTL,
        )

    /**
     * Labels, colours and the square every key is laid out at, and the line drawn between them: a
     * `LinearLayout` divider, one between the rows and one between the keys of each row. Each
     * container gets a drawable of its own, since a divider is given bounds as it is drawn.
     */
    private fun buildKeypad(content: View) {
        KEY_BUTTONS.forEach { (id, key) ->
            content.findViewById<Button>(id).apply {
                text = key.label(field.decimalSeparator)
                setTextColor(if (key.isOperator) style.operatorText else style.keyText)
                // Kept visible but dead, so the panel does not change shape between fields.
                isEnabled = key != CalculatorKey.SIGN || field.negativeValueAllow
                setOnClickListener { press(key) }
                squareOff()
            }
        }
        content.findViewById<Button>(R.id.currency_calculator_key_equals).apply {
            text = EQUALS_LABEL
            setTextColor(style.equalsText)
            // The ripple moves on top of the fill, and only once the fill is in place: setting a
            // background clears the callback of the one it replaces, and that is this ripple.
            val ripple = background
            setBackgroundColor(style.equalsBackground.defaultColor)
            foreground = ripple
            setOnClickListener { accept() }
            squareOff()
        }
        val rows = content.findViewById<LinearLayout>(R.id.currency_calculator_keys)
        rows.dividerDrawable = style.keyBorderDrawable(anchor.context)
        for (index in 0 until rows.childCount) {
            (rows.getChildAt(index) as LinearLayout).dividerDrawable = style.keyBorderDrawable(anchor.context)
        }
    }

    /** Every key is the same square, so the panel is four of them wide however wide the field is. */
    private fun View.squareOff() {
        layoutParams =
            layoutParams.apply {
                width = style.keySize
                height = style.keySize
            }
    }

    private fun press(key: CalculatorKey) {
        state = state.press(key)
        render(failed = false)
    }

    private fun accept() {
        when (val result = state.result(field.scale, field.negativeValueAllow)) {
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
        expressionView.text = state.display(field.decimalSeparator)
        expressionView.setTextColor(if (failed) style.errorText else style.expressionText)
    }

    private companion object {
        const val POPUP_ELEVATION_DP = 8f
        const val KEYBOARD_CLOSE_TIMEOUT_MS = 300L
    }
}
