package es.kibarra.app

import com.hivemq.client.mqtt.datatypes.MqttQos
import com.hivemq.client.mqtt.mqtt3.Mqtt3AsyncClient
import com.hivemq.client.mqtt.mqtt3.Mqtt3Client
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import java.nio.charset.StandardCharsets
import java.util.Locale
import java.util.UUID

// Config fija del bridge ESP32 (mismo esquema que bridge/service.py y main.cpp).
// BROKER_HOST/BROKER_PORT viven en BrokerConfig.kt (no se sube a git).
private const val ALIAS = "kiprim_dc605s"
private const val TOPIC_PREFIX = "lab"
private val TOPIC_STATE_BASE = "$TOPIC_PREFIX/$ALIAS/state"
private val TOPIC_CMD_BASE = "$TOPIC_PREFIX/$ALIAS/cmd"

data class KibarraState(
    val mqttConnected: Boolean = false,
    val lastMessageAt: Long = 0L,
    val outputOn: Boolean? = null,
    val voltageSet: Float? = null,
    val currentSet: Float? = null,
    val currentLimit: Float? = null,
    val voltageLimit: Float? = null,
    val voltageMeasured: Float? = null,
    val currentMeasured: Float? = null,
    val powerMeasured: Float? = null,
    val profile: String? = null,
    val profileStep: Int? = null,
    val perfilFuente: String? = null,
    val voltageMax: Float? = null,
    val currentMax: Float? = null,
    val perfilPersonalizadoDisponible: Boolean = false,
)

class MqttController {
    private val client: Mqtt3AsyncClient = Mqtt3Client.builder()
        .identifier("kibarra-android-${UUID.randomUUID()}")
        .serverHost(BROKER_HOST)
        .serverPort(BROKER_PORT)
        .automaticReconnectWithDefaultConfig()
        .buildAsync()

    private val _state = MutableStateFlow(KibarraState())
    val state: StateFlow<KibarraState> = _state

    fun connect() {
        client.connectWith()
            .send()
            .whenComplete { _, throwable ->
                _state.update { it.copy(mqttConnected = throwable == null) }
                if (throwable == null) subscribe()
            }
    }

    private fun subscribe() {
        client.subscribeWith()
            .topicFilter("$TOPIC_STATE_BASE/#")
            .qos(MqttQos.AT_MOST_ONCE)
            .callback { publish ->
                val topic = publish.topic.toString()
                val payload = String(publish.payloadAsBytes, StandardCharsets.UTF_8)
                onStateMessage(topic.removePrefix("$TOPIC_STATE_BASE/"), payload)
            }
            .send()
    }

    private fun onStateMessage(sufijo: String, payload: String) {
        _state.update { s ->
            val base = s.copy(mqttConnected = true, lastMessageAt = System.currentTimeMillis())
            when (sufijo) {
                "output" -> base.copy(outputOn = payload == "ON")
                "voltage_set" -> base.copy(voltageSet = payload.toFloatOrNull())
                "current_set" -> base.copy(currentSet = payload.toFloatOrNull())
                "current_limit" -> base.copy(currentLimit = payload.toFloatOrNull())
                "voltage_limit" -> base.copy(voltageLimit = payload.toFloatOrNull())
                "voltage_measured" -> base.copy(voltageMeasured = payload.toFloatOrNull())
                "current_measured" -> base.copy(currentMeasured = payload.toFloatOrNull())
                "power_measured" -> base.copy(powerMeasured = payload.toFloatOrNull())
                "profile" -> base.copy(profile = payload)
                "profile_step" -> base.copy(profileStep = payload.toIntOrNull())
                "perfil_fuente" -> base.copy(perfilFuente = payload)
                "voltage_max" -> base.copy(voltageMax = payload.toFloatOrNull())
                "current_max" -> base.copy(currentMax = payload.toFloatOrNull())
                "perfil_personalizado" -> base.copy(perfilPersonalizadoDisponible = payload == "disponible")
                else -> base
            }
        }
    }

    private fun publish(sufijo: String, payload: String) {
        client.publishWith()
            .topic("$TOPIC_CMD_BASE/$sufijo")
            .payload(payload.toByteArray(StandardCharsets.UTF_8))
            .qos(MqttQos.AT_LEAST_ONCE)
            .send()
    }

    // Locale.ROOT fuerza punto decimal siempre: el ESP32 usa atof() en C,
    // que trunca en cualquier carácter no numérico (una coma perdería los decimales).
    fun setVoltage(v: Float) = publish("voltage", String.format(Locale.ROOT, "%.3f", v))
    fun setCurrent(i: Float) = publish("current", String.format(Locale.ROOT, "%.3f", i))
    fun setCurrentLimit(i: Float) = publish("current_limit", String.format(Locale.ROOT, "%.3f", i))
    fun setVoltageLimit(v: Float) = publish("voltage_limit", String.format(Locale.ROOT, "%.3f", v))
    fun setOutput(on: Boolean) = publish("output", if (on) "ON" else "OFF")
    fun startProfile(nombre: String) = publish("profile/start", nombre)
    fun stopProfile() = publish("profile/stop", "stop")
    fun setPerfilFuente(nombre: String) = publish("config/perfil_fuente", nombre)
    fun definirPerfil(json: String) = publish("profile/definir", json)

    fun disconnect() {
        client.disconnect()
    }
}
