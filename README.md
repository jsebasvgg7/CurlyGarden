# CurlyGarden

App Android privada para dos personas (pareja, amigos cercanos o familia): un espacio compartido para hablar, dibujar, jugar e imaginar juntos. Interfaz en español, dark mode.

## Estado de los módulos

| Módulo | Estado |
|---|---|
| Login con Google, perfil y vinculación por código de invitación | Funcional |
| Home | Funcional, conectado a Firestore |
| Perfil (estado de conexión, desvincular, racha, días juntos) | Funcional |
| Talk (muro de cartas, una a la vez) | v1 cerrada |
| Draw (rondas con temas cruzados y revelado simultáneo) | v1 cerrada |
| Imagine (Ideas, Listas, Metas, Cápsulas) | Concepto cerrado, sin implementar |
| Play | Sin implementar |
| Recuerdos | Sin implementar |
| Notificaciones push (FCM) | Sin implementar |

## Stack

| Capa | Tecnología |
|---|---|
| App | Kotlin 1.9.24 + Jetpack Compose (Material 3, BOM 2024.02.02) |
| Android | minSdk 26, targetSdk y compileSdk 34, JVM 17 |
| Build | Android Gradle Plugin 8.13.2, Gradle 9.3.0 |
| Backend | Firebase: Auth, Firestore, Storage, Cloud Functions (Node 20) |
| Login | Credential Manager + Google Sign-In |
| Imágenes | Coil |
| Iconos | Feather + Material Icons Extended |
| Tipografía | Quicksand, local en `res/font/` |

## Requisitos

- Android Studio reciente y JDK 17.
- Un proyecto de Firebase con Authentication (proveedor Google), Firestore y Storage activos.
- Para las Cloud Functions: Node 20 y Firebase CLI.

## Ejecutar la app

1. Abre la carpeta en Android Studio y deja que Gradle sincronice.
2. `app/google-services.json` ya está en el repo. Si usas tu propio proyecto de Firebase, reemplázalo y registra el SHA-1 de tu keystore de debug en la app Android de Firebase.
3. Ejecuta la variante `debug` en un dispositivo o emulador con Google Play Services.

## Build de release

El build de release se firma con un keystore local que no se versiona (`*.keystore` y `keystore.properties` están en `.gitignore`). Crea `keystore.properties` en la raíz con estas claves:

```
storeFile=ruta/al/keystore
storePassword=...
keyAlias=...
keyPassword=...
```

Luego:

```
./gradlew assembleRelease
```

El SHA-1 de release también debe estar registrado en Firebase para que el login con Google funcione.

## Backend

Todo vive en la raíz del repo:

| Archivo | Contenido |
|---|---|
| `firestore.rules` | Reglas de seguridad de Firestore |
| `firestore.indexes.json` | Índices (hoy vacío, no hacen falta) |
| `functions/index.js` | Cloud Functions: `createInviteCode`, `redeemInviteCode`, `cancelPendingInvite`, `unlinkCouple` |
| `firebase.json` | Configuración del CLI |

Despliegue:

```
firebase deploy --only firestore:rules
firebase deploy --only functions
```

Nota: `firebase.json` referencia `storage.rules`, pero ese archivo no está en el repo. Las reglas de Storage hoy se gestionan desde la consola de Firebase.

## Estructura

```
app/src/main/java/com/curlygarden/app/
├── MainActivity.kt
├── auth/              login con Google y Credential Manager
├── couple/            onboarding: crear y canjear código de invitación
├── profile/           perfil propio y estado de conexión
├── startup/           decide la pantalla inicial según Firestore
├── data/streak/       cálculo de la racha a partir de streakLog
└── ui/
    ├── theme/         colores, tipografía y tema Compose
    └── screens/
        ├── auth/      Splash y Login
        ├── couple/    CoupleOnboardingScreen
        ├── draw/      Draw: tema, canvas, revelado e historial
        ├── home/      Home y su repositorio
        ├── profile/   CreateProfileScreen
        └── talk/      Talk: carta actual, redactar e historial
```

## Documentación

El diseño, el modelo de datos de Firestore y el historial de decisiones están en `CurlyGardenDoc.md`, un documento maestro que se mantiene fuera del repo (está en `.gitignore`).