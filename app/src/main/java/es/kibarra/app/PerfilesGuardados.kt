package es.kibarra.app

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import org.json.JSONArray
import org.json.JSONObject

// Un perfil tal como lo maneja la app: los dos de fábrica (grabados en el
// firmware, "idFirmware" = nombre exacto que reconoce profile/start) y los
// que el usuario va creando (pasosJson != null, idFirmware siempre
// "personalizado" porque el ESP32 solo tiene un hueco de perfil a medida:
// la app reenvía la definición completa justo antes de arrancarlo).
data class PerfilGuardado(
    val nombre: String,
    val fuente: String,
    val pasosJson: String?,
    val idFirmware: String,
    val borrable: Boolean = true,
)

val PERFILES_INTEGRADOS = listOf(
    PerfilGuardado("Carga AGM", "dc605s", null, "carga_csb_agm", borrable = false),
    PerfilGuardado("Carga SAI", "dc605s", null, "carga_sai", borrable = false),
)

private const val PREFS = "kibarra_perfiles"
private const val CLAVE_LISTA = "lista_json"

object AlmacenPerfiles {
    fun cargar(context: Context): List<PerfilGuardado> {
        val raw = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(CLAVE_LISTA, null)
            ?: return emptyList()
        val arr = JSONArray(raw)
        return (0 until arr.length()).map { i ->
            val o = arr.getJSONObject(i)
            PerfilGuardado(
                nombre = o.getString("nombre"),
                fuente = o.getString("fuente"),
                pasosJson = o.getString("pasosJson"),
                idFirmware = "personalizado",
                borrable = true,
            )
        }
    }

    fun guardar(context: Context, lista: List<PerfilGuardado>) {
        val arr = JSONArray()
        for (p in lista) {
            val o = JSONObject()
            o.put("nombre", p.nombre)
            o.put("fuente", p.fuente)
            o.put("pasosJson", p.pasosJson)
            arr.put(o)
        }
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putString(CLAVE_LISTA, arr.toString()).apply()
    }
}

@Composable
fun PantallaCrearPerfil(onGuardado: () -> Unit) {
    val context = LocalContext.current
    var nombre by remember { mutableStateOf("") }
    var fuente by remember { mutableStateOf("dc605s") }
    val pasos = remember { mutableStateListOf(PasoEditable()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text("Crear perfil", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KibarraAzul)

        SeccionCard("Datos del perfil") {
            OutlinedTextField(
                value = nombre,
                onValueChange = { nombre = it },
                label = { Text("Nombre del perfil") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Text("Fuente a la que está pensado aplicarlo:", style = MaterialTheme.typography.labelSmall)
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FuenteChip("DC605S", "dc605s", fuente, Modifier.weight(1f)) { fuente = "dc605s" }
                FuenteChip("DC310S", "dc310s", fuente, Modifier.weight(1f)) { fuente = "dc310s" }
            }
        }

        pasos.forEachIndexed { index, paso ->
            PasoEditorCard(
                indice = index,
                paso = paso,
                permitirBorrar = pasos.size > 1,
                onBorrar = { pasos.removeAt(index) }
            )
        }

        OutlinedButton(onClick = { pasos.add(PasoEditable()) }, modifier = Modifier.fillMaxWidth()) {
            Text("+ Añadir paso")
        }

        Button(
            onClick = {
                val nombreFinal = nombre.trim().ifEmpty { "Perfil sin nombre" }
                val json = construirJsonPerfil(nombreFinal, pasos)
                val actuales = AlmacenPerfiles.cargar(context)
                AlmacenPerfiles.guardar(context, actuales + PerfilGuardado(nombreFinal, fuente, json, "personalizado"))
                nombre = ""
                fuente = "dc605s"
                pasos.clear()
                pasos.add(PasoEditable())
                onGuardado()
            },
            modifier = Modifier.fillMaxWidth()
        ) { Text("Guardar perfil") }
    }
}

@Composable
fun PantallaPerfiles(viewModel: KibarraViewModel) {
    val context = LocalContext.current
    var personalizados by remember { mutableStateOf(AlmacenPerfiles.cargar(context)) }
    val todos = PERFILES_INTEGRADOS + personalizados

    Column(modifier = Modifier.fillMaxSize().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text("Perfiles", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold, color = KibarraAzul)

        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(todos) { perfil ->
                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                            Text(perfil.nombre, style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                            AssistChip(onClick = {}, label = { Text(perfil.fuente.uppercase()) })
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Button(
                                onClick = {
                                    viewModel.setPerfilFuente(perfil.fuente)
                                    perfil.pasosJson?.let { viewModel.definirPerfil(it) }
                                    viewModel.startProfile(perfil.idFirmware)
                                },
                                modifier = Modifier.weight(1f)
                            ) { Text("Enviar y arrancar") }

                            if (perfil.borrable) {
                                OutlinedButton(
                                    onClick = {
                                        val nuevaLista = personalizados.filter { it !== perfil }
                                        AlmacenPerfiles.guardar(context, nuevaLista)
                                        personalizados = nuevaLista
                                    }
                                ) { Text("Borrar") }
                            }
                        }
                    }
                }
            }
        }
    }
}
