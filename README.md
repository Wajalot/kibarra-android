# KiBarra Android

App nativa Android (Kotlin + Jetpack Compose) para controlar una fuente de laboratorio **Kiprim/OWON** desde el móvil, hablando MQTT con [KiBarra-ESP32](https://github.com/Wajalot/kibarra-esp32) (o con el bridge Python de [KiBarra](https://github.com/Wajalot/lab_Kiprim), da igual cuál esté corriendo — comparten el mismo esquema de topics).

Nace para no depender ni del PC ni de apps MQTT genéricas de terceros (con publicidad o versión de pago) para controlar el instrumento desde cualquier sitio de casa.

## Pantallas

- **Control**: medidas en vivo (V/I/P), consignas actuales del instrumento, envío de SET (voltage/current) y de límites de protección (OVP/OCP) por separado, botón de salida ON/OFF, indicador de conexión al bridge, y selector de qué fuente está conectada ahora mismo (DC605S/DC310S — cada una con sus límites de seguridad correctos).
- **Crear perfil**: editor de perfiles de carga paso a paso (voltage, current, límites, estado de salida y condición de avance — tiempo, corriente/voltaje por encima o por debajo de un umbral), con nombre y fuente asociada.
- **Perfiles**: lista unificada de los perfiles de fábrica del ESP32 y los que vayas creando desde el móvil. Cada uno tiene un botón "Enviar y arrancar" que ajusta la fuente correcta, envía la definición (si es uno personalizado) y lo arranca — todo en un solo toque.

## Requisitos

- Un bridge corriendo (ESP32 o Python) publicando en el esquema `lab/<alias>/state/...` / `lab/<alias>/cmd/...` sobre un broker MQTT sin TLS.
- Android 8.0+ (minSdk 26).

## Compilar

```bash
cp app/src/main/java/es/kibarra/app/BrokerConfig.kt.example app/src/main/java/es/kibarra/app/BrokerConfig.kt
# edita BrokerConfig.kt con la IP real de tu broker
./gradlew assembleDebug
```

El APK queda en `app/build/outputs/apk/debug/app-debug.apk`. `BrokerConfig.kt` está en `.gitignore` — nunca se sube al repo.

No está pensada para publicarse en Play Store (uso personal); se instala directamente desde el APK de debug.

## Arquitectura

```
App Android (cliente MQTT) ── MQTT ── ESP32 / bridge Python ── USB serie ── Kiprim/OWON
```

La app es solo un cliente MQTT más — no tiene lógica de control del instrumento, toda la seguridad (ciclo OFF→set→ON, límites por fuente) vive en el bridge/firmware.

## Perfiles personalizados: cómo funcionan

Los perfiles que creas en el móvil se guardan **localmente en el teléfono** (no en el ESP32). Al arrancar uno, la app reenvía su definición completa en JSON al bridge justo antes de lanzarlo — el ESP32 solo tiene un hueco de "perfil personalizado" que se sobreescribe cada vez.
