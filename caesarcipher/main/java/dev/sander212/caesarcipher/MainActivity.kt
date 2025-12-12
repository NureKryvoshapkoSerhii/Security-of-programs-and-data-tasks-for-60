package dev.sander212.caesarcipher

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// Наш український алфавіт
private const val UK_ALPHABET = "абвгґдеєжзиіїйклмнопрстуфхцчшщьюя"

// Логіка шифру Цезаря
fun caesarCipher(
    text: String,
    shift: Int,
    encrypt: Boolean = true,
    alphabet: String = UK_ALPHABET
): String {
    if (alphabet.isEmpty()) return text

    val normalizedShift =
        ((if (encrypt) shift else -shift) % alphabet.length + alphabet.length) % alphabet.length

    val result = StringBuilder()

    for (ch in text) {
        val isUpper = ch.isUpperCase()
        val lowerChar = ch.lowercaseChar()

        val index = alphabet.indexOf(lowerChar)
        if (index == -1) {
            // Не літера з нашого алфавіту — не чіпаємо (пробіли, крапки тощо)
            result.append(ch)
        } else {
            val newIndex = (index + normalizedShift) % alphabet.length
            var newChar = alphabet[newIndex]
            if (isUpper) {
                newChar = newChar.uppercaseChar()
            }
            result.append(newChar)
        }
    }

    return result.toString()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    CaesarScreen()
                }
            }
        }
    }
}

@Composable
fun CaesarScreen() {
    var inputText by remember {
        mutableStateOf(
            "й рицтв чцфкрґссг цкрюґхтет йґщтупискщ цифкцтфлн етфтє сґрґєґіцбхг чофлукцк уифяч плслв тдтфтск ч зтсиьболн цґ йґутфлйболн тдпґхцгщ"
        )
    }
    var shiftValue by remember { mutableStateOf(4f) }
    var isEncrypt by remember { mutableStateOf(false) }
    val alphabetLength = UK_ALPHABET.length

    val clipboardManager = LocalClipboardManager.current

    // Результат: якщо в режимі розшифрування зсув = 0 — текст НЕ змінюємо
    val resultText by remember(inputText, shiftValue, isEncrypt) {
        mutableStateOf(
            if (!isEncrypt && shiftValue.toInt() == 0) {
                // Режим розшифрування + зсув 0 → лишаємо як є
                inputText
            } else {
                caesarCipher(
                    text = inputText,
                    shift = shiftValue.toInt().mod(alphabetLength),
                    encrypt = isEncrypt
                )
            }
        )
    }

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Шифр Цезаря",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 26.sp
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        Text(
            text = "Введи текст, вибери зсув і режим — шифрувати або розшифрувати.\n" +
                    "У режимі розшифрування при зсуві 0 текст не змінюється.",
            style = MaterialTheme.typography.bodyMedium
        )

        OutlinedTextField(
            value = inputText,
            onValueChange = { inputText = it },
            label = { Text("Вхідний текст") },
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = 120.dp),
            maxLines = 6
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Режим:",
                style = MaterialTheme.typography.bodyMedium
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { isEncrypt = true },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isEncrypt)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Шифрувати")
                }

                Button(
                    onClick = { isEncrypt = false },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (!isEncrypt)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Text("Розшифрувати")
                }
            }
        }

        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text("Зсув: ${shiftValue.toInt()} (0–${alphabetLength - 1})")

            Slider(
                value = shiftValue,
                onValueChange = { shiftValue = it },
                valueRange = 0f..(alphabetLength - 1).toFloat(),
                steps = alphabetLength - 2
            )
        }

        Text(
            text = "Результат:",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold)
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
            ) {
                IconButton(
                    onClick = {
                        clipboardManager.setText(AnnotatedString(resultText))
                    },
                    modifier = Modifier.align(Alignment.TopEnd)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = "Скопіювати результат"
                    )
                }
                SelectionContainer {
                    Text(
                        text = resultText,
                        modifier = Modifier
                            .padding(12.dp)
                            .fillMaxWidth(),
                        style = MaterialTheme.typography.bodyLarge
                    )
                }
            }
        }
    }
}
