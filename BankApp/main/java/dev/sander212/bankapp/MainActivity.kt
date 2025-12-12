package dev.sander212.bankapp

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.tasks.Task
import com.google.android.gms.wallet.*
import com.google.android.gms.wallet.contract.TaskResultContracts
import org.json.JSONArray
import org.json.JSONObject
import java.util.Locale
// ---------------- ЛОГІ ТЕГ ----------------

private const val GPayTag = "GPayDebug"

// ---------------- МОДЕЛІ ----------------

data class DonationTransaction(
    val from: String,
    val to: String,
    val amount: Double
)

// ---------------- ACTIVITY ----------------

class MainActivity : ComponentActivity() {

    private lateinit var paymentsClient: PaymentsClient

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        paymentsClient = createPaymentsClient()

        setContent {
            BankAppTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    DonationScreenWithGooglePay(paymentsClient = paymentsClient)
                }
            }
        }
    }

    private fun createPaymentsClient(): PaymentsClient {
        val walletOptions = Wallet.WalletOptions.Builder()
            .setEnvironment(WalletConstants.ENVIRONMENT_TEST) // тільки TEST, бойові гроші не йдуть
            .build()
        val client = Wallet.getPaymentsClient(this, walletOptions)
        Log.d(GPayTag, "PaymentsClient created with ENVIRONMENT_TEST")
        return client
    }
}

// ---------------- ТЕМА “ПІД МОНОБАНК” ----------------

@Composable
fun BankAppTheme(content: @Composable () -> Unit) {
    val darkBackground = Color(0xFF0E0E11)
    val darkSurface = Color(0xFF1A1A1E)
    val accentYellow = Color(0xFFFFC107)
    val onDark = Color(0xFFF5F5F7)

    val colorScheme = darkColorScheme(
        primary = accentYellow,
        onPrimary = Color.Black,
        background = darkBackground,
        onBackground = onDark,
        surface = darkSurface,
        onSurface = onDark,
        surfaceVariant = Color(0xFF2A2A30),
        onSurfaceVariant = onDark
    )

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}

// ---------------- UI ЕКРАН ----------------

@Composable
fun DonationScreenWithGooglePay(
    paymentsClient: PaymentsClient
) {
    // Баланси
    var myBalance by remember { mutableStateOf(10_000.0) }
    var zsuBalance by remember { mutableStateOf(0.0) }

    val fromAccountName = "Мій рахунок"
    val toAccountName = "Збір на ЗСУ"

    var amountText by remember { mutableStateOf("500") }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    var transactions by remember { mutableStateOf(listOf<DonationTransaction>()) }

    // Launcher для Google Pay
    val paymentDataLauncher = rememberLauncherForActivityResult(
        contract = TaskResultContracts.GetPaymentData()
    ) { paymentData: PaymentData? ->

        Log.d(GPayTag, "paymentDataLauncher called. paymentData = $paymentData")

        paymentData?.toJson()?.let {
            Log.d(GPayTag, "PaymentData JSON: $it")
        } ?: Log.w(GPayTag, "PaymentData is NULL (user canceled or error)")

        val amount = amountText.toDoubleOrNull()

        if (paymentData != null && amount != null && amount > 0 && amount <= myBalance) {
            Log.d(GPayTag, "GPay SUCCESS: amount=$amount, moving money between accounts")
            // ✅ УСПІШНА ОПЛАТА ЧЕРЕЗ GOOGLE PAY
            myBalance -= amount
            zsuBalance += amount
            val tx = DonationTransaction(fromAccountName, toAccountName, amount)
            transactions = listOf(tx) + transactions
            errorMessage = null
        } else {
            // ❌ Google Pay повернув помилку або користувач скасував оплату
            Log.w(
                GPayTag,
                "GPay FAIL: paymentData=$paymentData, amount=$amount, myBalance=$myBalance"
            )
            errorMessage = "Оплата скасована або сталась помилка Google Pay"
        }
    }

    // Перевіряємо доступність Google Pay
    var isGPayAvailable by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        isGooglePayAvailable(paymentsClient) { available ->
            Log.d(GPayTag, "isGooglePayAvailable callback: $available")
            isGPayAvailable = available
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        Text(
            text = "Збір на ЗСУ",
            style = MaterialTheme.typography.headlineMedium.copy(
                fontWeight = FontWeight.Bold,
                fontSize = 28.sp,
                color = MaterialTheme.colorScheme.onBackground
            ),
            modifier = Modifier.fillMaxWidth(),
            textAlign = TextAlign.Center
        )

        // Картка збору (як у монобанку)
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "На дрони для ЗСУ",
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp
                )
            )

            Text(
                text = "Мета: 1 000 000 грн",
                style = MaterialTheme.typography.bodyMedium
            )

            // Прогрес збору
            val goal = 1_000_000.0
            val progress = (zsuBalance / goal).coerceIn(0.0, 1.0)
            Column {
                LinearProgressIndicator(
                    progress = progress.toFloat(),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(999.dp)),
                    color = MaterialTheme.colorScheme.primary
                )
                Text(
                    text = String.format(
                        "Зібрано: %.2f грн (%.2f %%)",
                        zsuBalance,
                        progress * 100
                    ),
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }

        // Дві “картки” рахунків
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            AccountCard(
                title = fromAccountName,
                balance = myBalance,
                highlight = true,
                modifier = Modifier.weight(1f)
            )
            AccountCard(
                title = toAccountName,
                balance = zsuBalance,
                highlight = false,
                modifier = Modifier.weight(1f)
            )
        }

        // Сума донату
        Column(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Сума донату",
                style = MaterialTheme.typography.bodyMedium
            )
            OutlinedTextField(
                value = amountText,
                onValueChange = { amountText = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                textStyle = LocalTextStyle.current.copy(
                    fontSize = 18.sp
                ),
                trailingIcon = {
                    Text(
                        text = "₴",
                        style = MaterialTheme.typography.titleMedium
                    )
                }
            )
        }

        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color(0xFFFF5252),
                style = MaterialTheme.typography.bodyMedium
            )
        }

        Spacer(modifier = Modifier.height(4.dp))

        // ОДНА КНОПКА: тільки Google Pay
        if (isGPayAvailable) {
            Button(
                onClick = {
                    errorMessage = null
                    val amount = amountText.toDoubleOrNull()

                    Log.d(GPayTag, "GPay button clicked, amountText='$amountText', parsed=$amount")

                    if (amount == null || amount <= 0.0) {
                        errorMessage = "Введи коректну суму (> 0)"
                        Log.w(GPayTag, "Invalid amount")
                        return@Button
                    }
                    if (amount > myBalance) {
                        errorMessage = "Недостатньо коштів на рахунку"
                        Log.w(
                            GPayTag,
                            "Not enough funds: amount=$amount, balance=$myBalance"
                        )
                        return@Button
                    }

                    val paymentDataRequestJson =
                        createPaymentDataRequest(amount = amount, currencyCode = "UAH")
                    if (paymentDataRequestJson == null) {
                        errorMessage = "Не вдалося створити запит на оплату"
                        Log.e(GPayTag, "createPaymentDataRequest() returned null")
                        return@Button
                    }

                    Log.d(GPayTag, "PaymentDataRequest JSON: $paymentDataRequestJson")

                    val request = PaymentDataRequest.fromJson(paymentDataRequestJson.toString())
                    if (request != null) {
                        val task: Task<PaymentData> =
                            paymentsClient.loadPaymentData(request)

                        Log.d(GPayTag, "loadPaymentData() started")

                        task.addOnSuccessListener {
                            Log.d(GPayTag, "loadPaymentData: Task success (before launcher)")
                        }

                        task.addOnFailureListener { e ->
                            Log.e(GPayTag, "loadPaymentData: Task failure BEFORE launcher", e)
                        }

                        task.addOnCompleteListener { completedTask ->
                            Log.d(
                                GPayTag,
                                "loadPaymentData: complete, isSuccessful=${completedTask.isSuccessful}, ex=${completedTask.exception}"
                            )

                            try {
                                paymentDataLauncher.launch(completedTask)
                            } catch (e: Exception) {
                                Log.e(GPayTag, "Error launching paymentDataLauncher", e)
                                errorMessage =
                                    "Помилка при виклику Google Pay: ${e.localizedMessage}"
                            }
                        }
                    } else {
                        errorMessage = "Помилка формування PaymentDataRequest"
                        Log.e(GPayTag, "PaymentDataRequest.fromJson() returned null")
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp),
                shape = RoundedCornerShape(999.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text(
                    text = "Оплатити через Google Pay",
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp
                )
            }
        } else {
            Text(
                text = "Google Pay недоступний на цьому пристрої",
                style = MaterialTheme.typography.bodySmall
            )
        }

        Text(
            text = "Останні транзакції",
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.SemiBold
            )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(transactions) { tx ->
                TransactionRow(tx)
            }
        }
    }
}

// ---------------- ДОПОМІЖНІ COMPOSABLE ----------------

@Composable
fun AccountCard(
    title: String,
    balance: Double,
    highlight: Boolean,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(20.dp),
        colors = if (highlight)
            CardDefaults.cardColors(containerColor = Color(0xFF26262B))
        else
            CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E))
    ) {
        Column(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.labelLarge.copy(
                    color = Color(0xFFB0B0B5)
                )
            )
            Text(
                text = String.format("%.2f ₴", balance),
                style = MaterialTheme.typography.titleMedium.copy(
                    fontWeight = FontWeight.Bold,
                    fontSize = 18.sp
                )
            )
        }
    }
}

@Composable
fun TransactionRow(tx: DonationTransaction) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1E)),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(12.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = "З: ${tx.from}",
                    style = MaterialTheme.typography.bodySmall
                )
                Text(
                    text = "На: ${tx.to}",
                    style = MaterialTheme.typography.bodySmall.copy(
                        color = Color(0xFFB0B0B5)
                    )
                )
            }
            Text(
                text = "-${String.format("%.2f ₴", tx.amount)}",
                style = MaterialTheme.typography.bodyLarge.copy(
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.primary
                )
            )
        }
    }
}

// ---------------- GOOGLE PAY ЛОГІКА ----------------

// Перевірити, чи є на пристрої Google Pay і підтримка карт
fun isGooglePayAvailable(
    paymentsClient: PaymentsClient,
    callback: (Boolean) -> Unit
) {
    val isReadyToPayJson = getIsReadyToPayRequest() ?: run {
        Log.e(GPayTag, "getIsReadyToPayRequest() == null")
        callback(false)
        return
    }

    Log.d(GPayTag, "isReadyToPay request: $isReadyToPayJson")

    val request = IsReadyToPayRequest.fromJson(isReadyToPayJson.toString())
    val task = paymentsClient.isReadyToPay(request)
    task.addOnCompleteListener { completedTask ->
        val success = completedTask.isSuccessful
        val result = completedTask.result
        val ex = completedTask.exception

        Log.d(
            GPayTag,
            "isReadyToPay complete: success=$success, result=$result, ex=$ex"
        )

        callback(success && result == true)
    }
}

// Базові allowedAuthMethods / cardNetworks
private fun baseCardPaymentMethod(): JSONObject {
    val parameters = JSONObject()
        .put("allowedAuthMethods", JSONArray().put("PAN_ONLY").put("CRYPTOGRAM_3DS"))
        .put("allowedCardNetworks", JSONArray().put("MASTERCARD").put("VISA"))

    return JSONObject()
        .put("type", "CARD")
        .put("parameters", parameters)
        .put("tokenizationSpecification", gatewayTokenizationSpecification())
}

// Умовний gateway (шифрований токен у реальному світі пішов би сюди)
private fun gatewayTokenizationSpecification(): JSONObject {
    return JSONObject()
        .put("type", "PAYMENT_GATEWAY")
        .put(
            "parameters",
            JSONObject()
                .put("gateway", "example")
                .put("gatewayMerchantId", "exampleMerchantId")
        )
}

// JSON для isReadyToPay
private fun getIsReadyToPayRequest(): JSONObject? =
    try {
        JSONObject().put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
    } catch (_: Exception) {
        null
    }

// Основний PaymentDataRequest
fun createPaymentDataRequest(amount: Double, currencyCode: String): JSONObject? {
    return try {
        val transactionInfo = JSONObject()
            .put("totalPrice", String.format(Locale.US, "%.2f", amount)) // 👈 КРАПКА, не КОМА
            .put("totalPriceStatus", "FINAL")
            .put("currencyCode", currencyCode)

        val merchantInfo = JSONObject()
            .put("merchantName", "ZSU Support Donation")

        val root = JSONObject()
            .put("apiVersion", 2)
            .put("apiVersionMinor", 0)
            .put("allowedPaymentMethods", JSONArray().put(baseCardPaymentMethod()))
            .put("transactionInfo", transactionInfo)
            .put("merchantInfo", merchantInfo)

        Log.d(GPayTag, "createPaymentDataRequest(): $root")
        root
    } catch (e: Exception) {
        Log.e(GPayTag, "createPaymentDataRequest() exception", e)
        null
    }
}

