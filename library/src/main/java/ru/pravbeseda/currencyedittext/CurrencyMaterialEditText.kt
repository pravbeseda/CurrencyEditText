package ru.pravbeseda.currencyedittext

import android.content.Context
import android.util.AttributeSet
import com.google.android.material.textfield.TextInputLayout
import java.math.BigDecimal

class CurrencyMaterialEditText(context: Context, attrs: AttributeSet?) :
    TextInputLayout(context, attrs) {

    private val editText: CurrencyEditText

    init {
        editText = CurrencyEditText(context, null)
        addView(editText)
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