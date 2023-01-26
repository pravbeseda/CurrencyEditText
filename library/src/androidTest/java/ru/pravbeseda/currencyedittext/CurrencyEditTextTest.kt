package ru.pravbeseda.currencyedittext

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class CurrencyEditTextTest {
    // Alternative getting context: private var context: Context = InstrumentationRegistry.getInstrumentation().targetContext
    private val context: Context = ApplicationProvider.getApplicationContext()
    private lateinit var currencyEditText: CurrencyEditText

    @Before
    fun init() {
        currencyEditText = CurrencyEditText(context, null)
    }

    @Test
    fun shouldSetText() {
        val samples = listOf(
            arrayOf("100", "100"),
            arrayOf("0.", "0.")
        )
        samples.forEach {
            testSetText(it[0], it[1])
        }
    }

    @Test
    fun shouldNegativeValueAllow() {
        currencyEditText.setNegativeValueAllow(false)
        testSetText("-100", "100")
        currencyEditText.setNegativeValueAllow(true)
        testSetText("-100", "-100")
    }

    private fun testSetText(text: String, expected: String) {
        InstrumentationRegistry.getInstrumentation().runOnMainSync {
            currencyEditText.setText(text)
        }
        assertEquals(expected, currencyEditText.text.toString())
    }
}
