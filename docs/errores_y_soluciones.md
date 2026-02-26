# Registro de Errores y Soluciones — SLIOR

> Documento actualizado a lo largo de todo el proyecto.
> Cada error encontrado queda registrado con su causa exacta y la solución aplicada.

---

## Índice de Errores

| # | Fase | Componente | Error resumido | Estado |
|---|------|-----------|---------------|--------|
| 1 | Fase 0 | Android / Gradle | `fileCollection(Spec)` incompatibilidad kapt + Gradle 8.x |  Resuelto |
| 2 | Fase 1 Android | `MainActivity` / appcompat | `addMenuProvider` firma incompatible appcompat 1.6.1 vs activity-compose 1.9.2 |  Resuelto |
| 3 | Fase 1 Android | D8 / Gradle JVM | `OutOfMemoryError: Java heap space` durante compilación |  Resuelto |

---

## ERROR #1

**Fecha:** 09/03/2026  
**Fase:** Fase 0 — Inicialización  
**Componente:** Proyecto Android (Gradle)  
**Severidad:**  Bloqueo (el proyecto no podía sincronizar)

### Mensaje de error

```
A problem occurred configuring project ':app'.
> Failed to notify project evaluation listener.
   > 'org.gradle.api.file.FileCollection
     org.gradle.api.artifacts.Configuration.fileCollection(
     org.gradle.api.specs.Spec)'
```

### ¿Qué estaba pasando?

El proyecto Android fue configurado inicialmente con el plugin `kotlin-kapt` (Kotlin Annotation Processing Tool) para que librerías como Room y Hilt pudieran generar código en tiempo de compilación.

El problema es que `kapt` llama internamente al método `Configuration.fileCollection(Spec)` de la API de Gradle. 
Este método fue marcado como deprecated en Gradle 7.x y **eliminado definitivamente en Gradle 8.3**.

Android Studio 2025.1.3 (Meerkat) utiliza Gradle 8.13 como versión predeterminada, muy por encima del umbral donde `kapt` deja de funcionar. 
Por tanto, en el momento de sincronizar el proyecto por primera vez, Gradle intentó ejecutar código que ya no existe en su API y lanzó el error.

### Diagrama de la causa

```
build.gradle.kts
   plugin: kotlin-kapt
         usa internamente: Configuration.fileCollection(Spec)
               método ELIMINADO en Gradle 8.3
                     Android Studio usa Gradle 8.13
                            ERROR al sincronizar
```

### Solución aplicada

Se migró de `kapt` a **KSP** (Kotlin Symbol Processing), el sucesor oficial recomendado por Google y JetBrains.

**Cambios en `build.gradle.kts` (raíz):**
```kotlin
// ANTES
id("org.jetbrains.kotlin.android") version "1.9.22" apply false
id("com.google.dagger.hilt.android") version "2.48" apply false

// DESPUÉS
id("org.jetbrains.kotlin.android") version "2.0.21" apply false
id("com.google.dagger.hilt.android") version "2.51.1" apply false
id("com.google.devtools.ksp") version "2.0.21-1.0.28" apply false  // ← nuevo
```

**Cambios en `app/build.gradle.kts`:**
```kotlin
// ANTES (plugins)
id("kotlin-kapt")

// DESPUÉS (plugins)
id("com.google.devtools.ksp")

// ANTES (dependencias)
kapt("androidx.room:room-compiler:2.6.1")
kapt("com.google.dagger:hilt-compiler:2.48")
kapt("androidx.hilt:hilt-compiler:1.1.0")

// DESPUÉS (dependencias)
ksp("androidx.room:room-compiler:2.6.1")
ksp("com.google.dagger:hilt-compiler:2.51.1")
ksp("androidx.hilt:hilt-compiler:1.2.0")
```

Se añadió también `gradle/wrapper/gradle-wrapper.properties` para fijar explícitamente Gradle 8.13.

### ¿Por qué KSP es mejor que kapt?

| Característica | kapt | KSP |
|---------------|------|-----|
| Velocidad de compilación | Lenta | Hasta 2× más rápida |
| Compatibilidad Gradle 8.x |  Problemas desde 8.3 |  Completa |
| Estado oficial | En mantenimiento | Activo, recomendado |
| Soporte de Room | Sí | Sí (preferido) |
| Soporte de Hilt | Sí | Sí (preferido) |

### Lección aprendida

Al iniciar un proyecto Android nuevo con versiones recientes de Android Studio (2024+), usar siempre **KSP** en lugar de `kapt`. 
Verificar que las versiones del plugin KSP y Kotlin coincidan (misma versión base, ej. `2.0.21-1.0.28` para Kotlin `2.0.21`).

---

## ERROR #2

**Fecha:** 09/03/2026
**Fase:** Fase 1 — Android
**Componente:** `MainActivity.kt` / `appcompat`

### Mensaje de error

```
Class 'MainActivity' is not abstract and does not implement abstract member:
fun addMenuProvider(p0: MenuProvider, p1: LifecycleOwner, p2: State): Unit
Cannot access 'LifecycleOwner' which is a supertype of 'MainActivity'.
Check your module classpath for missing or conflicting dependencies.
```

### Causa

Incompatibilidad de versiones entre `appcompat:1.6.1` y `activity-compose:1.9.2`.

`activity-compose:1.9.2` usa una versión de `MenuHost` (de `androidx.core`) con una firma del método `addMenuProvider` diferente a la que espera `appcompat:1.6.1`. Ambas librerías implementan o extienden `ComponentActivity`, y la firma incompatible hace que el compilador de Kotlin no pueda satisfacer la interfaz abstracta.

### Solución

Actualizar `appcompat` a la versión `1.7.0`, que es compatible con `activity-compose:1.9.2` y usa la misma versión de `androidx.core`.

```kotlin
// build.gradle.kts (app)
// ANTES
implementation("androidx.appcompat:appcompat:1.6.1")

// DESPUÉS
implementation("androidx.appcompat:appcompat:1.7.0")
```

### Lección aprendida

Al usar `activity-compose`, mantener `appcompat` en su versión más reciente (`1.7.0+`). Las versiones de `appcompat`, `activity-compose` y `androidx.core` deben ser compatibles entre sí. Usar el [BOM de Compose](https://developer.android.com/jetpack/compose/bom) ayuda a alinear las versiones de las librerías Compose, pero las librerías AndroidX externas (como `appcompat`) deben actualizarse manualmente.

---

## ERROR #3

**Fecha:** 09/03/2026
**Fase:** Fase 1 — Android
**Componente:** Build Android (D8 / dexer)

### Mensaje de error

```
AGPBI: {"kind":"error","text":"java.lang.OutOfMemoryError: Java heap space","tool":"D8"}
Caused by: java.lang.OutOfMemoryError: Java heap space
```

### Causa

El proceso de compilación de Gradle (específicamente D8, el compilador de bytecode a DEX de Android) necesita más memoria heap de la que Java tiene asignada por defecto.

El heap predeterminado de la JVM suele ser de 512 MB. Proyectos Android con muchas dependencias (Hilt, Compose, Room, Retrofit, WorkManager, etc.) pueden superar fácilmente ese límite durante la compilación.

### Solución

Crear el archivo `gradle.properties` en la raíz del módulo `mobile-app/` con una asignación de memoria mayor:

```properties
# gradle.properties
org.gradle.jvmargs=-Xmx2048m -XX:MaxMetaspaceSize=512m
org.gradle.parallel=true
org.gradle.caching=true
android.useAndroidX=true
kotlin.code.style=official
```

`-Xmx2048m` asigna 2 GB de heap máximo a la JVM de Gradle.

### Lección aprendida

En proyectos Android con múltiples librerías que usan procesamiento de anotaciones (KSP, Hilt, Room), crear siempre `gradle.properties` con `org.gradle.jvmargs=-Xmx2048m` desde el inicio. También es recomendable activar `org.gradle.parallel=true` y `org.gradle.caching=true` para reducir los tiempos de compilación.

---

*— Nuevos errores se añadirán aquí a medida que avance el proyecto —*
