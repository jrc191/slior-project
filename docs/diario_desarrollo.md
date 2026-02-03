# Diario de Desarrollo — SLIOR

> Registro detallado de cada decisión técnica, comando ejecutado y problema
> encontrado durante el desarrollo. Fuente primaria para la Memoria del TFG.

---

## FASE 0: Inicialización del Proyecto

**Fecha:** 09/03/2026  
**Duración estimada:** 30 minutos  
**Estado:**  Completada

---

### 0.1 Entorno de Trabajo

| Elemento | Valor |
|----------|-------|
| Sistema Operativo | Windows 11 |
| IDE | Cursor (basado en VSCode) con agente IA |
| Herramienta IA | GitHub Copilot CLI (Claude Sonnet 4.6) |
| Ruta del proyecto | `C:\Users\User\Documents\PROYECTO-TFG\slior-project\` |
| Control de versiones | Git |

---

### 0.2 Creación de la Estructura de Directorios

Se creó la estructura de carpetas del monorepo con el siguiente comando PowerShell:

```powershell
New-Item -ItemType Directory -Path "slior-project"
New-Item -ItemType Directory -Path "slior-project\backend"
New-Item -ItemType Directory -Path "slior-project\mobile-app"
New-Item -ItemType Directory -Path "slior-project\docs"
New-Item -ItemType Directory -Path "slior-project\scripts"
```

**Estructura resultante:**
```
slior-project/
 backend/      → API REST Spring Boot
 mobile-app/   → Aplicación Android
 docs/         → Documentación técnica
 scripts/      → Scripts de utilidad (BD, arranque)
```

**Decisión:** Se optó por un **monorepo** (un único repositorio Git para backend y app móvil) en lugar de repositorios separados. Ventajas para un proyecto académico:
- Un solo `git clone` para tener todo el código
- Commits coordinados entre backend y frontend
- Más sencillo para el tribunal evaluador

---

### 0.3 Inicialización de Git

```bash
cd slior-project
git init
git branch -M main   # Rama principal llamada 'main' (convención moderna)
```

**Resultado:** Repositorio Git vacío inicializado en `.git/`.

---

### 0.4 Archivo `.gitignore`

Se creó `.gitignore` con exclusiones para:
- **Java/Maven:** carpeta `target/`, archivos `.class`, `.jar`
- **Android/Gradle:** carpeta `build/`, `.gradle/`, `.apk`, `.aab`
- **IDEs:** `.idea/`, `.vscode/`, `.cursor/`
- **Seguridad:** `*.env`, `application-prod.properties`, `keystore.properties`, `google-services.json`

**Decisión importante:** Se incluyó `application-prod.properties` en el gitignore para evitar exponer credenciales de producción en el repositorio público.

---

### 0.5 Licencia MIT

Se eligió la **licencia MIT** por ser la más permisiva y adecuada para un proyecto académico que puede servir de referencia a otros estudiantes.

---

### 0.6 Configuración Backend (Spring Boot)

#### Dependencias Maven (`pom.xml`)

| Dependencia | Versión | Motivo |
|-------------|---------|--------|
| spring-boot-starter-web | 3.2.4 | API REST con Tomcat embebido |
| spring-boot-starter-data-jpa | 3.2.4 | ORM con Hibernate y PostgreSQL |
| spring-boot-starter-security | 3.2.4 | Autenticación y autorización |
| spring-boot-starter-validation | 3.2.4 | Validación de DTOs (Jakarta) |
| spring-boot-starter-mail | 3.2.4 | Envío de emails (confirmaciones) |
| postgresql | runtime | Driver JDBC para PostgreSQL 15 |
| lombok | opcional | Reducir código boilerplate |
| jjwt-api/impl/jackson | 0.12.3 | Generación y validación de JWT |
| spring-boot-starter-test | test | JUnit 5 + Mockito |

**¿Por qué jjwt 0.12.3?** Es la versión más reciente de la librería JJWT con API modernizada. Las versiones anteriores (<0.11) tienen una API diferente y menos segura.

**¿Por qué UUID como ID?** Los UUIDs son independientes de la base de datos (no necesitan autoincremento), son globalmente únicos (útil para sincronización offline) y no exponen información sobre el volumen de datos (a diferencia de IDs numéricos secuenciales).

#### Estructura de paquetes backend

```
com.slior
 config/          → SecurityConfig, CorsConfig
 controller/      → AuthController, RouteController, PackageController
 service/         → AuthService, RouteService, RouteOptimizationService, PackageService
 repository/      → UserRepository, RouteRepository, StopRepository, PackageRepository
 model/           → Entidades JPA (User, Route, Stop, Package)
    enums/       → UserRole, RouteStatus, StopStatus, PackageStatus
 dto/             → Data Transfer Objects (entrada/salida de API)
    auth/        → LoginRequest, RegisterRequest, AuthResponse
    route/       → CreateRouteRequest, RouteResponse, etc.
    mapper/      → RouteMapper
 security/        → JwtUtil, JwtAuthenticationFilter, CustomUserDetailsService
 exception/       → GlobalExceptionHandler + excepciones custom
 util/            → Haversine, helpers
```

**¿Por qué DTOs separados de las entidades?** Las entidades JPA representan la estructura interna de la BD. Exponer directamente puede filtrar datos sensibles, crear dependencias del schema en el contrato de la API, y dificultar la evolución independiente. Los DTOs actúan como contrato explícito de la API.

#### `application.properties`

Se configuraron:
- Conexión a PostgreSQL en `localhost:5432` (base de datos `slior_db`)
- `ddl-auto=update` para que Hibernate cree/actualice tablas automáticamente en desarrollo
- JWT secret placeholder (se debe cambiar en producción por una clave de mínimo 256 bits)
- Access token con expiración de **30 minutos** (valor `1800000` ms)

---

### 0.7 Configuración Mobile App (Android)

#### Dependencias Android (`build.gradle.kts`)

| Dependencia | Versión | Motivo |
|-------------|---------|--------|
| Room | 2.6.1 | Base de datos SQLite local (offline-first) |
| Retrofit | 2.9.0 | Cliente HTTP type-safe para la API REST |
| OkHttp logging-interceptor | 4.12.0 | Log de peticiones HTTP en desarrollo |
| Hilt | 2.48 | Inyección de dependencias (simplifica DI vs Dagger) |
| Coroutines | 1.7.3 | Asincronía sin callbacks (más legible) |
| DataStore | 1.0.0 | Almacenamiento del JWT (reemplaza SharedPreferences) |
| WorkManager | 2.9.0 | Tareas en background (sincronización offline) |
| OSMDroid | 6.1.17 | Mapas offline con OpenStreetMap (sin clave API) |
| Play Services Location | 21.1.0 | GPS/red del dispositivo (FusedLocationProvider) |
| ZXing Android Embedded | 4.3.0 | Escáner de códigos de barras con cámara |

**¿Por qué Hilt en vez de Koin?** Hilt está integrado con el ciclo de vida de Android, tiene mejor soporte para ViewModel y WorkManager, y es la solución oficial recomendada por Google. Koin usa reflexión en runtime (más lento).

**¿Por qué OSMDroid en vez de Google Maps?** OpenStreetMap es completamente gratuito sin límites de uso. Google Maps SDK requiere clave API con facturación activada y tiene un límite de peticiones gratuitas que puede ser problemático en un entorno académico.

**¿Por qué minSdk=24 (Android 7.0)?** Cubre aproximadamente el 95% de los dispositivos Android activos, excluyendo solo terminales muy antiguos. Android 7.0 tiene soporte nativo para Java 8 (streams, lambdas).

#### Estructura de paquetes Android

```
com.slior
 ui/
    auth/        → LoginActivity, RegisterActivity
    routes/      → RoutesListActivity, RouteDetailActivity, CreateRouteActivity
    packages/    → PackageDetailActivity, BarcodeScannerActivity, ScanPackageFragment
    map/         → MapFragment
    components/  → NetworkStatusBar
 viewmodel/       → AuthViewModel, RouteViewModel, PackageViewModel
 data/
    local/
       entity/  → UserEntity, RouteEntity, StopEntity, PackageEntity, SyncQueue
       dao/     → UserDao, RouteDao, StopDao, PackageDao, SyncQueueDao
    remote/
       dto/     → DTOs de red (LoginRequest, AuthResponse, etc.)
    repository/  → AuthRepository, RouteRepository, PackageRepository, SyncRepository
 model/           → Data classes del dominio
 di/              → DatabaseModule, NetworkModule, WorkerModule, AppModule
 util/            → LocationHelper, NetworkMonitor, Result
```

#### `AndroidManifest.xml` — Permisos declarados

| Permiso | Necesario para |
|---------|---------------|
| INTERNET | Comunicación con backend REST |
| ACCESS_NETWORK_STATE | Detectar si hay conexión (NetworkMonitor) |
| ACCESS_FINE_LOCATION | GPS preciso para optimización de ruta |
| ACCESS_COARSE_LOCATION | Ubicación aproximada (fallback) |
| CAMERA | Escáner de códigos de barras ZXing |
| WRITE_EXTERNAL_STORAGE | Caché de tiles OSMDroid (solo hasta API 28) |

**Nota:** `android:usesCleartextTraffic="true"` está activado solo para desarrollo (peticiones HTTP a localhost). En producción se debe desactivar y usar HTTPS.

---

### 0.8 Clase Application (`SliorApplication.kt`)

Se creó la clase Application con:
- **`@HiltAndroidApp`**: dispara la generación de código de Hilt al compilar
- **`Configuration.Provider`**: permite a WorkManager usar `HiltWorkerFactory` para inyectar dependencias en los Workers

---

### 0.9 Scripts de Utilidad

| Archivo | Propósito |
|---------|-----------|
| `scripts/setup_database.sql` | Crea usuario y BD en PostgreSQL |
| `scripts/start_backend.sh` | Arranca el servidor Spring Boot |

---

### 0.10 Primer Commit

```bash
git add .
git commit -m "chore: inicializar proyecto SLIOR

- Estructura monorepo: backend/, mobile-app/, docs/, scripts/
- .gitignore completo para Java/Maven + Android/Gradle + IDEs
- README.md con descripción, tecnologías y setup
- pom.xml Spring Boot 3.2.4 con dependencias de la fase 0
- build.gradle.kts Android con Hilt, Room, Retrofit, OSMDroid, ZXing
- AndroidManifest.xml con permisos declarados
- SliorBackendApplication.java (clase main)
- SliorApplication.kt (Application class con Hilt + WorkManager)
- Recursos base: strings.xml, colors.xml, themes.xml
- Scripts: setup_database.sql, start_backend.sh"
```

---

### 0.11 Resumen de Archivos Creados en Fase 0

| Archivo | Descripción |
|---------|-------------|
| `.gitignore` | Exclusiones Git |
| `LICENSE` | Licencia MIT |
| `README.md` | Documentación principal |
| `backend/pom.xml` | Dependencias Maven |
| `backend/src/main/resources/application.properties` | Configuración Spring Boot |
| `backend/src/main/java/com/slior/SliorBackendApplication.java` | Main class |
| `mobile-app/settings.gradle.kts` | Módulos del proyecto Android |
| `mobile-app/build.gradle.kts` | Plugins raíz |
| `mobile-app/app/build.gradle.kts` | Dependencias Android |
| `mobile-app/app/src/main/AndroidManifest.xml` | Manifest con permisos |
| `mobile-app/app/src/main/java/com/slior/SliorApplication.kt` | Application class |
| `mobile-app/app/src/main/res/values/strings.xml` | Textos de la UI |
| `mobile-app/app/src/main/res/values/colors.xml` | Paleta de colores |
| `mobile-app/app/src/main/res/values/themes.xml` | Tema Material Design |
| `scripts/setup_database.sql` | Script PostgreSQL |
| `scripts/start_backend.sh` | Script arranque backend |
| `docs/diario_desarrollo.md` | Este archivo |

---

*Próxima fase: [FASE 1 — Autenticación y Fundamentos](../README.md#fases-de-desarrollo)*
