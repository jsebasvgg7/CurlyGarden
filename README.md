# CurlyGarden — proyecto Android

Base inicial del proyecto, generada a partir del documento maestro de diseno.

## Como abrir el proyecto

1. Abre Android Studio (version reciente, Koala o superior).
2. `File > Open` y selecciona esta carpeta (`CurlyGarden/`).
3. Deja que Gradle sincronice. Puede pedir instalar el SDK 34 si no lo tienes.
4. Ejecuta en un emulador o dispositivo fisico con `Run > Run 'app'`.

Con esto ya deberias ver la app corriendo con datos de ejemplo (mock), sin necesidad de configurar Firebase todavia.

## Que hay ya implementado

- Tema visual con la paleta oficial de CurlyGarden (`ui/theme/Theme.kt`)
- Navegacion inferior de 4 tabs: home, us, memories, me (`ui/navigation/BottomNav.kt`)
- Pantalla Home funcional con datos de ejemplo (`ui/screens/home/HomeScreen.kt`)
- Pantalla Talk (chat) funcional con mensajes de ejemplo (`ui/screens/talk/TalkScreen.kt`)
- Modelos de datos que reflejan 1:1 las colecciones de Firestore (`data/model/Models.kt`)

## Que falta (marcado con TODO en el codigo)

- Pantallas Draw, Play, Imagine y Memories (mismo patron que Home/Talk)
- Conectar Firebase real:
  1. Crear proyecto en https://console.firebase.google.com
  2. Descargar `google-services.json` y colocarlo en `app/`
  3. Activar Authentication (metodo por definir — email, telefono, Google)
  4. Activar Firestore y aplicar las reglas de seguridad (pendiente de definir en el documento maestro)
  5. Activar Storage para fotos de perfil y memories
- Reemplazar los avatares placeholder por imagenes reales (Coil o similar para carga de imagenes)
- Implementar el flujo de onboarding con codigo de invitacion (ver documento maestro, seccion 2.4)
- Logica real de dibujo colaborativo en Draw (sincronizacion de trazos via Firestore o Realtime Database)

## Estructura de carpetas

```
app/src/main/java/com/curlygarden/app/
├─ MainActivity.kt
├─ ui/
│  ├─ theme/          -> colores, tipografia, tema Compose
│  ├─ navigation/      -> bottom nav de 4 tabs
│  └─ screens/
│     ├─ home/
│     ├─ talk/
│     ├─ draw/         (vacio, pendiente)
│     ├─ play/         (vacio, pendiente)
│     ├─ imagine/      (vacio, pendiente)
│     └─ memories/     (vacio, pendiente)
└─ data/
   └─ model/          -> data classes que reflejan Firestore
```

Referencia completa de diseno, colores, modelo de datos y decisiones: ver `CurlyGarden_Documentacion.md`.
