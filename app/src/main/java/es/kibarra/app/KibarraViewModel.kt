package es.kibarra.app

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

// Igual que el badge de la GUI del PC: solo se considera "conectado" si ha
// llegado un mensaje de estado en los últimos segundos (el ESP32 publica a 1Hz).
private const val TIMEOUT_MS = 5_000L

class KibarraViewModel : ViewModel() {
    private val mqtt = MqttController()
    val state: StateFlow<KibarraState> = mqtt.state

    private val _now = MutableStateFlow(System.currentTimeMillis())

    val conectado: StateFlow<Boolean> = combine(state, _now) { s, now ->
        s.mqttConnected && (now - s.lastMessageAt) < TIMEOUT_MS
    }.stateIn(viewModelScope, kotlinx.coroutines.flow.SharingStarted.Eagerly, false)

    init {
        mqtt.connect()
        viewModelScope.launch {
            while (true) {
                delay(1000)
                _now.value = System.currentTimeMillis()
            }
        }
    }

    fun setVoltage(v: Float) = mqtt.setVoltage(v)
    fun setCurrent(i: Float) = mqtt.setCurrent(i)
    fun setCurrentLimit(i: Float) = mqtt.setCurrentLimit(i)
    fun setVoltageLimit(v: Float) = mqtt.setVoltageLimit(v)
    fun setOutput(on: Boolean) = mqtt.setOutput(on)
    fun startProfile(nombre: String) = mqtt.startProfile(nombre)
    fun stopProfile() = mqtt.stopProfile()
    fun setPerfilFuente(nombre: String) = mqtt.setPerfilFuente(nombre)
    fun definirPerfil(json: String) = mqtt.definirPerfil(json)

    override fun onCleared() {
        mqtt.disconnect()
    }
}
