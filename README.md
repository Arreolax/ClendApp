# Clend App

<p align="center">
  <img src="app/src/main/res/drawable/logosin.png" alt="Clend App Logo" width="120" />
</p>

<p align="center">
  <b>Tu compañero integral de productividad, enfoque y gamificación para estudiantes y profesionales.</b>
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Platform-Android-3DDC84?style=for-the-badge&logo=android&logoColor=white" alt="Platform Android" />
  <img src="https://img.shields.io/badge/Language-Kotlin-7F52FF?style=for-the-badge&logo=kotlin&logoColor=white" alt="Kotlin" />
  <img src="https://img.shields.io/badge/Min%20SDK-29-informational?style=for-the-badge" alt="Min SDK 29" />
  <img src="https://img.shields.io/badge/Target%20SDK-35-success?style=for-the-badge" alt="Target SDK 35" />
  <img src="https://img.shields.io/badge/Database-Room-orange?style=for-the-badge&logo=sqlite&logoColor=white" alt="Room SQLite" />
</p>

---

## Descripción General

**Clend App** es una aplicación móvil para Android desarrollada en Kotlin que combina la gestión de tareas, la toma de notas y técnicas avanzadas de enfoque y concentración con un innovador sistema de **gamificación**. 

Diseñada para evitar distracciones y fomentar hábitos de estudio saludables, Clend App recompensa el esfuerzo diario con puntos, rachas de actividad continua y ascensos en una escala de rangos competitiva, desbloqueando además temas musicales ambientales para el estudio.

---

## Equipo y Módulos

1. **Guadalupe:** Tiempo de estudio (Modo enfoque), Calculadora (Juego).
2. **Alejandro:** Calendario / Agenda (Tareas) / Notificaciones, Notas, Dashboard, Puntaje y Base de Datos (DB).
4. **Euen:** Rachas.

---

## Características Principales

### Gestión de Tareas y Calendario
* **Planificación flexible**: Creación de tareas con fecha límite, categorización personalizada (*Personal*, *Estudio*, etc.) y soporte para recordatorios periódicos.
* **Control de vencimientos**: Detección y marcado automático de tareas retrasadas (*late*).
* **Vista de calendario interactivo**: Visualización cronológica diaria para consultar rápidamente los compromisos pendientes.
* **Creación ágil**: Modal tipo *Bottom Sheet* para agregar tareas de forma rápida y contextual.

### Modo Enfoque / Estudio (Anti-Distracciones)
* **Temporizador configurable**: Presets rápidos o configuración manual en horas, minutos y segundos.
* **Bloqueo de distracciones**: Servicio en segundo plano diseñado para mantener el foco en la sesión de estudio.
* **Música ambiental integrada**: Pistas de audio relajantes para acompañar las sesiones de estudio.
* **Recompensas inmediatas**: Suma de puntos automáticos al completar con éxito cada bloque de estudio.

### Libreta de Notas y Carpetas
* **Organización estructurada**: Clasificación de notas por carpetas temáticas.
* **Formatos versátiles**: Soporte tanto para notas libres de texto como para listas de verificación.

### Gamificación, Rachas y Rangos
* **Seguimiento de racha diaria**: Registro continuo de días activos mediante seguimiento de bits semanales.
* **Sistema de Rangos Progresivos**:
  * **Potato** (0 pts)
  * **Brainy** (100 pts)
  * **Nerd** (250 pts)
  * **Master** (500 pts)
  * **God** (1000 pts)
  * **Devil** (2500 pts)
  * **Top Global** (5000+ pts)
* **Tabla de Ranking**: Clasificación para comparar puntajes y competir saludablemente.
* **Desbloqueo de Recompensas**: Nuevos temas musicales y personalizaciones a medida que se acumulan puntos.

### Calculadora & Minijuego Mental
* **Calculadora estándar**: Herramienta práctica para cálculos rápidos del día a día.
* **Modo Juego Contrarreloj**: Desafíos matemáticos interactivos con tiempo límite para ejercitar la mente y ganar puntos de racha adicionales.

### Notificaciones y Sincronización
* **Recordatorios precisos**: Notificaciones locales para alertas exactas de entrega de tareas.
* **Restauración automática tras reinicio**: Receptor que reprograma las notificaciones pendientes cuando el dispositivo se reinicia.

---

## Stack Tecnológico y Arquitectura

* **Lenguaje**: [Kotlin](https://kotlinlang.org/)
* **Plataforma Android**: Min SDK 29 (Android 10) | Target SDK 35 (Android 15)
* **Patrón de Arquitectura**: MVVM (Model-View-ViewModel) con componentes de Jetpack
* **Persistencia Local**: [Room Database](https://developer.android.com/training/data-storage/room) con compilador [KSP](https://kotlinlang.org/docs/ksp-overview.html)
* **Navegación**: Jetpack Navigation Component (`NavHostFragment`, `BottomNavigationView`, `Navigation Drawer`)
* **Diseño e Interfaz**:
  * [Material Design 3](https://m3.material.io/)
  * ViewBinding para manipulación segura de vistas
  * SwipeRefreshLayout para actualización fluida de pantallas
  * Custom Drawables y Temas Adaptables
* **Concurrencia**: Coroutines & Flow de Kotlin
* **Servicios & Notificaciones**:
  * Android Foreground Service (`specialUse`)
  * BroadcastReceivers para eventos del temporizador y reinicio del sistema

---

## Modelo de Base de Datos Local

La base de datos SQLite gestionada por **Room** (`clend_app_database`) se compone de las siguientes entidades:

| Entidad | Descripción |
| :--- | :--- |
| **`User`** | Credenciales y perfil del usuario (`fullName`, `email`, `username`, `password`). |
| **`Tasks`** | Tareas con título, descripción, fecha programada, fecha límite, estado de completitud y categoría. |
| **`Notes`** | Notas personales y listas de pendientes vinculadas a carpetas (`Folders`). |
| **`Folders`** | Agrupadores organizativos para la sección de notas. |
| **`Categories`** | Etiquetas para clasificación de tareas (ej. Personal, Estudio). |
| **`Scores`** | Registro de puntuación acumulada, contador de racha, bits semanales y estado de música desbloqueada. |
| **`Ranks`** | Niveles de rango y umbral de puntos requerido para alcanzarlos. |

---

## Permisos Requeridos

La aplicación utiliza permisos específicos para garantizar su funcionamiento:

* `POST_NOTIFICATIONS`: Envío de recordatorios de tareas próximas o vencidas.
* `SCHEDULE_EXACT_ALARM` / `USE_EXACT_ALARM`: Programación precisa de alarmas para las tareas.
* `RECEIVE_BOOT_COMPLETED`: Restauración de las alarmas programadas al encender el teléfono.
* `FOREGROUND_SERVICE` & `FOREGROUND_SERVICE_SPECIAL_USE`: Ejecución ininterrumpida de la sesión de estudio con música ambiental.
* `SYSTEM_ALERT_WINDOW` & `REORDER_TASKS`: Control de pantalla durante el modo de concentración anti-distracciones.

---

## Instalación y Compilación

### Requisitos Previos
* **Android Studio**: Ladybug / Koala o superior recomendado.
* **JDK**: Versión 11 o 17 compatible con Gradle 8+.
* **Dispositivo físico o emulador**: Android 10.0 (API nivel 29) o superior.

### Pasos

 **Abrir en Android Studio**:
   * Abre Android Studio y selecciona **Open**.
   * Navega hasta la carpeta del proyecto y haz clic en **OK**.

 **Sincronizar Gradle**:
   * Espera a que Gradle descargue las dependencias y compile los esquemas de Room con KSP.

 **Ejecutar en el dispositivo**:
   * Conecta tu dispositivo Android con depuración USB habilitada o inicia un emulador.
   * Presiona el botón verde de **Run** (`Shift + F10`).