package com.example.gymtracker.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.sp

@Composable
fun LargeNumberInput(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    isDecimal: Boolean = true
) {
    OutlinedTextField(
        value = value,
        onValueChange = { raw ->
            val input = if (isDecimal) raw.replace(',', '.') else raw
            val isValid = if (isDecimal) {
                input.isEmpty() || input.matches(Regex("^\\d*\\.?\\d*$"))
            } else {
                input.isEmpty() || input.matches(Regex("^\\d*$"))
            }
            if (isValid) {
                onValueChange(input)
            }
        },
        placeholder = {
            Text(
                placeholder,
                style = TextStyle(fontSize = 18.sp, textAlign = TextAlign.Center),
                modifier = Modifier.fillMaxWidth()
            )
        },
        textStyle = TextStyle(
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        ),
        keyboardOptions = KeyboardOptions(
            keyboardType = if (isDecimal) KeyboardType.Decimal else KeyboardType.Number
        ),
        singleLine = true,
        modifier = modifier
    )
}
