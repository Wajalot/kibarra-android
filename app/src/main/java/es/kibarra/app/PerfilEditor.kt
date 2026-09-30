package es.kibarra.app

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

// Opciones de condición "hasta" tal como las entiende el firmware (perfil.hpp).
private val CONDICIONES = listOf(
    "ninguna" to "Sin condición (solo al arrancar)",
    "tiempo" to "Tiempo (segundos)",
    "corriente_baja" to "Corriente por debajo de",
    "corriente_alta" to "Corriente por encima de",
    "voltaje_bajo" to "Voltaje por debajo de",
    "voltaje_alto" to "Voltaje por encima de",
)

class PasoEditable {
    var voltage by mutableStateOf("")
    var current by mutableStateOf("")
    var currentLimit by mutableStateOf("")
    var voltageLimit by mutableStateOf("")
    var output by mutableStateOf("sin_cambiar") // sin_cambiar | on | off
    var condicionTipo by mutableStateOf("ninguna")
    var condicionValor by mutableStateOf("")
}

fun construirJsonPerfil(nombre: String, pasos: List<PasoEditable>): String {
    val root = JSONObject()
    root.put("nombre", nombre)
    val arr = JSONArray()
    for (p in pasos) {
        val o = JSONObject()
        p.voltage.toFloatOrNullEs()?.let { o.put("voltage", it) }
        p.current.toFloatOrNullEs()?.let { o.put("current", it) }
        p.currentLimit.toFloatOrNullEs()?.let { o.put("current_limit", it) }
        p.voltageLimit.toFloatOrNullEs()?.let { o.put("voltage_limit", it) }
        when (p.output) {
            "on" -> o.put("output", true)
            "off" -> o.put("output", false)
        }
        val hasta = JSONObject()
        hasta.put("tipo", p.condicionTipo)
        p.condicionValor.toFloatOrNullEs()?.let { hasta.put("valor", it) }
        o.put("hasta", hasta)
        arr.put(o)
    }
    root.put("pasos", arr)
    return root.toString()
}

@Composable
fun PasoEditorCard(indice: Int, paso: PasoEditable, permitirBorrar: Boolean, onBorrar: () -> Unit) {
    ElevatedCard(modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                Text("Paso ${indice + 1}", style = MaterialTheme.typography.labelLarge, modifier = Modifier.weight(1f))
                if (permitirBorrar) {
                    TextButton(onClick = onBorrar) { Text("Quitar") }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CampoOpcional("Voltage", paso.voltage, { paso.voltage = it }, Modifier.weight(1f))
                CampoOpcional("Current", paso.current, { paso.current = it }, Modifier.weight(1f))
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                CampoOpcional("V límite", paso.voltageLimit, { paso.voltageLimit = it }, Modifier.weight(1f))
                CampoOpcional("I límite", paso.currentLimit, { paso.currentLimit = it }, Modifier.weight(1f))
            }

            Text("Salida en este paso:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("sin_cambiar" to "Sin cambiar", "on" to "ON", "off" to "OFF").forEach { (valor, etiqueta) ->
                    FilterChip(
                        selected = paso.output == valor,
                        onClick = { paso.output = valor },
                        label = { Text(etiqueta) }
                    )
                }
            }

            var menuAbierto by remember { mutableStateOf(false) }
            val etiquetaCondicion = CONDICIONES.first { it.first == paso.condicionTipo }.second
            Box {
                OutlinedButton(onClick = { menuAbierto = true }, modifier = Modifier.fillMaxWidth()) {
                    Text("Hasta: $etiquetaCondicion")
                }
                DropdownMenu(expanded = menuAbierto, onDismissRequest = { menuAbierto = false }) {
                    CONDICIONES.forEach { (valor, etiqueta) ->
                        DropdownMenuItem(
                            text = { Text(etiqueta) },
                            onClick = { paso.condicionTipo = valor; menuAbierto = false }
                        )
                    }
                }
            }
            if (paso.condicionTipo != "ninguna") {
                CampoOpcional("Valor de la condición", paso.condicionValor, { paso.condicionValor = it }, Modifier.fillMaxWidth())
            }
        }
    }
}

@Composable
private fun CampoOpcional(etiqueta: String, valor: String, onValueChange: (String) -> Unit, modifier: Modifier = Modifier) {
    OutlinedTextField(
        value = valor,
        onValueChange = onValueChange,
        label = { Text(etiqueta) },
        modifier = modifier,
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal)
    )
}
