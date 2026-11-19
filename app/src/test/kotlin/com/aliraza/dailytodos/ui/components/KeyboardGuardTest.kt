package com.aliraza.dailytodos.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsFocused
import androidx.compose.ui.test.assertIsNotFocused
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** While a field has the keyboard, a tap elsewhere only closes it; it never reaches the row below. */
@RunWith(AndroidJUnit4::class)
class KeyboardGuardTest {

    @get:Rule
    val compose = createComposeRule()

    private var opened = 0

    private fun show() = compose.setContent {
        KeyboardGuardBox(alsoWhen = false) {
            Column {
                Text("Pay rent", Modifier.fillMaxWidth().height(48.dp).clickable { opened++ })
                var text by remember { mutableStateOf("") }
                BasicTextField(text, { text = it }, Modifier.fillMaxWidth().guarded(rememberGuardedField()))
            }
        }
    }

    @Test
    fun aTapOutsideTheFieldOnlyClosesTheKeyboard() {
        show()
        val field = compose.onNode(hasSetTextAction())
        field.performClick()
        field.assertIsFocused()
        compose.onNodeWithText("Pay rent").performClick()
        field.assertIsNotFocused()
        assertEquals(0, opened)

        compose.onNodeWithText("Pay rent").performClick()
        assertEquals(1, opened)
    }

    @Test
    fun aTapOnTheFieldItselfKeepsTheKeyboard() {
        show()
        val field = compose.onNode(hasSetTextAction())
        field.performClick()
        field.performClick()
        field.assertIsFocused()
    }
}
