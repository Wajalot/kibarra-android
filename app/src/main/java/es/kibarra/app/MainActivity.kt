package es.kibarra.app

import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// El teclado numérico en español manda coma decimal ("13,6"); Kotlin solo
// parsea punto. Normalizamos antes de convertir a Float.
fun String.toFloatOrNullEs(): Float? = trim().replace(',', '.').toFloatOrNull()

class MainActivity : ComponentActivity() {
    private val viewModel: KibarraViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = KibarraColorScheme) {
                Surface(color = MaterialTheme.colorScheme.background) {
                    KibarraApp(viewModel)
                }
            }
        }
    }
}

private enum class Pantalla { CONTROL, CREAR, PERFILES }

@Composable
fun KibarraApp(viewModel: KibarraViewModel) {
    var pantalla by remember { mutableStateOf(Pantalla.CONTROL) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                val coloresNav = NavigationBarItemDefaults.colors(indicatorColor = KibarraMagenta)
                NavigationBarItem(
                    selected = pantalla == Pantalla.CONTROL,
                    onClick = { pantalla = Pantalla.CONTROL },
                    icon = {}, label = { Text("Control") },
                    colors = coloresNav
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.CREAR,
                    onClick = { pantalla = Pantalla.CREAR },
                    icon = {}, label = { Text("Crear perfil") },
                    colors = coloresNav
                )
                NavigationBarItem(
                    selected = pantalla == Pantalla.PERFILES,
                    onClick = { pantalla = Pantalla.PERFILES },
                    icon = {}, label = { Text("Perfiles") },
                    colors = coloresNav
                )
            }
        }
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (pantalla) {
                Pantalla.CONTROL -> PantallaControl(viewModel)
                Pantalla.CREAR -> PantallaCrearPerfil(onGuardado = { pantalla = Pantalla.PERFILES })
                Pantalla.PERFILES -> PantallaPerfiles(viewModel)
            }
        }
    }
}

private val AmarilloMedida = Color(0xFFE8D44D)
private val CianMedida = Color(0xFF4DD0E1)
private val VerdeMedida = Color(0xFF66BB6A)

@Composable
fun PantallaControl(viewModel: KibarraViewModel) {
    val state by viewModel.state.collectAsState()
    val conectado by viewModel.conectado.collectAsState()
    val context = LocalContext.current

    var voltageInput by remember { mutableStateOf("") }
    var currentInput by remember { mutableStateOf("") }
    var voltageLimitInput by remember { mutableStateOf("") }
    var currentLimitInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(color = KibarraAzul)) { append("K") }
                withStyle(SpanStyle(color = KibarraMagenta)) { append("i") }
                withStyle(SpanStyle(color = KibarraAzul)) { append("Barra") }
            },
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold
        )

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.fillMaxWidth()) {
            MedidaCard("V MEDIDA", state.voltageMeasured, "voltios", AmarilloMedida, Modifier.weight(1f))
            MedidaCard("I MEDIDA", state.currentMeasured, "amperios", CianMedida, Modifier.weight(1f))
            MedidaCard("POTENCIA", state.powerMeasured, "vatios", VerdeMedida, Modifier.weight(1f))
        }

        SeccionCard("Consignas actuales en el Kiprim") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ConsignaChip("V set", state.voltageSet, Modifier.weight(1f))
                ConsignaChip("I set", state.currentSet, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                ConsignaChip("V límite", state.voltageLimit, Modifier.weight(1f))
                ConsignaChip("I límite (OCP)", state.currentLimit, Modifier.weight(1f))
            }
        }

        SeccionCard("Enviar consigna (SET) — corriente/tensión objetivo") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = voltageInput, onValueChange = { voltageInput = it },
                    label = { Text("Voltage") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = currentInput, onValueChange = { currentInput = it },
                    label = { Text("Current") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Button(
                onClick = {
                    var enviado = false
                    voltageInput.toFloatOrNullEs()?.let { viewModel.setVoltage(it); enviado = true }
                    currentInput.toFloatOrNullEs()?.let { viewModel.setCurrent(it); enviado = true }
                    if (!enviado) Toast.makeText(context, "Valor no válido", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32))
            ) { Text("Enviar SET") }
        }

        SeccionCard("Enviar límite (protección) — OVP/OCP") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(
                    value = voltageLimitInput, onValueChange = { voltageLimitInput = it },
                    label = { Text("Voltage límite") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
                OutlinedTextField(
                    value = currentLimitInput, onValueChange = { currentLimitInput = it },
                    label = { Text("Current límite") }, modifier = Modifier.weight(1f), singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
                )
            }
            Button(
                onClick = {
                    var enviado = false
                    voltageLimitInput.toFloatOrNullEs()?.let { viewModel.setVoltageLimit(it); enviado = true }
                    currentLimitInput.toFloatOrNullEs()?.let { viewModel.setCurrentLimit(it); enviado = true }
                    if (!enviado) Toast.makeText(context, "Valor no válido", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB71C1C))
            ) { Text("Enviar LÍMITE") }
        }

        SeccionCard("Salida y perfil") {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                val on = state.outputOn == true
                Button(
                    onClick = { viewModel.setOutput(!on) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (on) Color(0xFF6A1B9A) else Color(0xFF424242)
                    )
                ) { Text(if (on) "Salida ON" else "Salida OFF") }

                Spacer(Modifier.width(12.dp))

                AssistChip(
                    onClick = {},
                    label = { Text(if (conectado) "Bridge: Conectado" else "Bridge: Desconectado") },
                    colors = AssistChipDefaults.assistChipColors(
                        containerColor = if (conectado) Color(0xFF2E7D32) else Color(0xFFB71C1C)
                    )
                )
            }

            Spacer(Modifier.height(8.dp))
            Text("Perfil: ${state.profile ?: "ninguno"}  ·  paso ${state.profileStep ?: "-"}")
            Text(
                "Para arrancar un perfil, ve a la pestaña \"Perfiles\".",
                style = MaterialTheme.typography.labelSmall
            )

            Button(
                onClick = { viewModel.stopProfile() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF424242))
            ) { Text("Parar perfil") }
        }

        SeccionCard("Fuente conectada") {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FuenteChip("DC605S", "dc605s", state.perfilFuente, Modifier.weight(1f)) {
                    viewModel.setPerfilFuente("dc605s")
                }
                FuenteChip("DC310S", "dc310s", state.perfilFuente, Modifier.weight(1f)) {
                    viewModel.setPerfilFuente("dc310s")
                }
            }
            Text(
                "Límites activos: ${state.voltageMax?.let { "%.0f".format(it) } ?: "--"} V / " +
                    "${state.currentMax?.let { "%.0f".format(it) } ?: "--"} A",
                style = MaterialTheme.typography.labelSmall
            )
        }
    }
}

@Composable
fun FuenteChip(etiqueta: String, valor: String, activo: String?, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val seleccionado = activo == valor
    FilterChip(
        selected = seleccionado,
        onClick = onClick,
        label = { Text(etiqueta) },
        modifier = modifier
    )
}

@Composable
private fun MedidaCard(titulo: String, valor: Float?, unidad: String, color: Color, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(12.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelMedium)
            Text(
                text = valor?.let { "%.2f".format(it) } ?: "--",
                color = color,
                fontSize = 28.sp,
                fontWeight = FontWeight.Bold
            )
            Text(unidad, style = MaterialTheme.typography.labelSmall)
        }
    }
}

@Composable
private fun ConsignaChip(titulo: String, valor: Float?, modifier: Modifier = Modifier) {
    ElevatedCard(modifier = modifier) {
        Column(Modifier.padding(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.labelSmall)
            Text(valor?.let { "%.3f".format(it) } ?: "--", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun SeccionCard(titulo: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(titulo, style = MaterialTheme.typography.titleSmall)
            content()
        }
    }
}
