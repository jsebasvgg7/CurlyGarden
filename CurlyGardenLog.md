# CurlyGarden — Log de cambios

Único archivo que crece. Lo vigente y explicativo vive en `CurlyGardenDoc.md` y `docs/`.
Última actualización: 20/10/2026.

---

## 1. Changelog (más reciente primero)

- **20/10/2026** — Imagine, fases 1 y 2. Fase 1: patch de reglas de Firestore para las 4 colecciones (sustituye el bloque `ideas`) y `storage.rules` versionado con `idea_attachments`; sin probar con emulador y pendiente de aplicar y desplegar. Fase 2: `ImagineModels.kt`, `ImagineRepository.kt`, `ImagineViewModel.kt`, `ImagineViewModelFactory.kt` e `ImagineAttachments.kt`. Modelo de cápsulas cambiado a subcolección `entries`. Docs `imagine.md` y `datos-y-backend.md` actualizados.
- **19/10/2026** — Documento maestro dividido en índice + `docs/` + log. Carpeta `patch/` añadida al `.gitignore`. Confirmado con `firebase functions:list` que las 4 Cloud Functions están desplegadas (`cancelPendingInvite`, `createInviteCode`, `redeemInviteCode`, `unlinkCouple`; v2, callable, us-central1, nodejs20). `cancelPendingInvite` no tiene llamada en el cliente y se decide dejarla así (el flujo actual no la necesita).
- **17/10/2026** — Revisión del repo tras una pausa. Se repuso la sección de Racha. Limpieza en 3 fases: comentarios Kotlin (`4ad06fe`), `Models.kt` y `logo.png` (`d826ff9`), README (`0a4b2b5`); parche final aplicado en `d1cc7ca` (comentarios de `functions/index.js` y `firestore.rules` fuera, dependencia `ui-text-google-fonts` fuera). `racha.txt` ya no está en el repo.
- **21/09/2026** — Concepto de Imagine cerrado: 4 tipos, gesto "Unirse", candado/corazón/cámara, checklist real, fecha libre en Metas, adjuntos de hasta 20 MB.
- **20/09/2026** — Racha cerrada (`streakLog` real, cálculo derivado). Talk v1 cerrado y probado en dos dispositivos. Draw v1 cerrado (confirmación mutua, reveal de un dibujo a la vez). Reglas de `messages` endurecidas.
- **19/09/2026** — Home, branding, onboarding dark y firma de release cerrados.
- **18/09/2026** — Header del Home rediseñado, card hero de Dibujar aprobada, `ColorScheme` oscuro real.
- **07/09/2026** — Bug de `createInviteCode` (`INTERNAL`) resuelto reordenando lecturas antes de escrituras.
- **06/09/2026** — Bug del botón de Google resuelto (Credential Manager requiere contexto de Activity).

---

## 2. Decisiones vigentes

**Producto y navegación**

| Decisión | Motivo |
|---|---|
| 4 módulos: Talk, Draw, Play, Imagine | Traducción directa de la idea original |
| Sin bottom nav; perfil desde los avatares del header | Solo 4 módulos + perfil |
| Bento con jerarquía, hero fijo (Dibujar), tamaños fijos | Sin lógica dinámica |
| Interfaz en español, lenguaje neutral | La app es para "dos personas" en general |
| Pestaña "Nosotros": solo crece, nunca retrocede | Que la pantalla nunca se sienta punitiva |

**Técnicas generales**

| Decisión | Motivo |
|---|---|
| Firebase / Firestore sobre SQL | Sincronización en tiempo real sin backend propio |
| Login con Google vía Credential Manager | Sin contraseñas; `GoogleSignInClient` está deprecado |
| Usuario nuevo detectado por `users/{uid}` en Firestore | Más confiable si se cerró la app a mitad del perfil |
| Crear/canjear código vía Cloud Functions | Operaciones atómicas, sin falsificación de campos |
| Home lee `coupleId` desde `users/{uid}` en cada apertura | No duplica la fuente de verdad de las reglas |
| Storage en modo producción | Reglas propias desde el inicio |
| Quicksand local y Feather + Material Icons Extended | Consistencia visual, sin Google Fonts en runtime |
| Sin comentarios en el código (Kotlin, JS, reglas) | Preferencia de estilo; el razonamiento vive en los docs |
| Splash `#12101F` distinto al icono `#BFB9FD` | El icono destaca en el escritorio; el splash combina con la app |
| Cards de Home en colores sólidos de acento (excepción al dark puro) | El mockup con color se sentía mejor |

**Draw**

| Decisión | Motivo |
|---|---|
| Rondas con temas cruzados y revelado simultáneo (no canvas en vivo) | Más simple de sincronizar y más divertido |
| Trazos normalizados 0..1 | El dibujo se ve igual en cualquier pantalla |
| Slots `user1`/`user2` en vez de uids | Las claves no dependen del uid |
| Nueva ronda con confirmación mutua (`readyForNext`) | Evita duplicar el historial y quitar el reveal al otro |
| Reveal de un dibujo a la vez | Más enfocado, sin scroll |
| El dibujo del otro no se descarga hasta `revealed` | Mantiene la sorpresa |

**Talk**

| Decisión | Motivo |
|---|---|
| Cartas en vez de chat clásico | Se siente como dejar una nota, no como un flujo de mensajes |
| Respuesta = carta nueva con `replyTo` | Carrusel lineal; el hilo se reconstruye en el historial |
| `replyPreview` guardado en la carta | La cita se ve aunque la original no esté cargada |
| Mensajes inmutables, 140 caracteres validados en reglas | Coherente con `delete: false` y con la idea de recuerdos |
| Paginación 30 + 30; botón "más reciente" | Reemplaza el máximo de 15 y el "recargar" |
| Push diferidas hasta terminar todas las pantallas | Evita reescribir FCM en cada módulo |

**Imagine**

| Decisión | Motivo |
|---|---|
| Contenido de cápsulas en subcolección `entries/{uid}` | Las reglas no pueden ocultar campos; así se bloquea la lectura hasta `unlockDate` |
| `contributors` en la cápsula; `sealed` eliminado; `title` añadido | Saber quién aportó; `sealed` se deduce de la fecha; el mockup muestra título |
| Cápsulas sin borrado (`delete: false`) | No perder la parte del otro en una compartida |
| Cada quien ve su propia parte antes de la fecha | Coherente con el mockup; ocultarla es una línea en las reglas |
| Cápsula individual visible para la pareja tras `unlockDate` (botón "Abrir") | Coincide con el mockup; si debe ser privada para siempre, cambia una línea |
| Storage sin `update` ni `delete` | Mantiene la inmutabilidad de las cápsulas selladas |
| Filtros según el doc; "Pendientes" eliminado | Sin dato propio que lo distinga de "Activas"; la cápsula sellada no tiene "finalizada" |
| Estado de Listas derivado de los ítems; Metas manual | Una lista completa se finaliza sola; una meta la decide el dueño |
| Contadores del índice en tiempo real (listeners) | Pocas colecciones, datos pequeños, consistente con la app |
| Texto del índice "Imagina / Ideas que construimos juntos." | Tomado del mockup |
| Un solo `ImagineViewModel` para índice y categorías | Mismo patrón que Talk |

**Racha**

| Decisión | Motivo |
|---|---|
| Cálculo derivado de `streakLog`, sin Cloud Function programada | Sin proceso nocturno que falle ni plan de pago |
| Solo actividades bilaterales (hoy Draw); Talk excluido | Una carta de uno solo no es actividad compartida |
| Una zona horaria por pareja (`America/Bogota` por defecto) | Nadie pierde el día por diferencia horaria |
| Desvincular no archiva nada | Racha y tiempo de vínculo se reinician |

---

## 3. Próximos pasos

**Plan por etapas**
- [ ] Imagine: aplicar y desplegar reglas (`git apply imagine-firestore-rules.patch`, `firebase deploy --only firestore:rules`, `firebase deploy --only storage`) y comparar `profile_photos` con la consola
- [ ] Imagine fase 3: índice 2x2 con contadores y `HomeDestination.Imagine` en `MainActivity`
- [ ] Imagine fase 4: pantalla de categoría (carta a la vez, tabs, 3 iconos); Ideas y Metas primero
- [ ] Imagine fase 5: crear, editar y Unirse; Cápsulas compartidas al final
- [ ] Imagine: texto de la card "Imaginar" del Home -> "Imagina / Ideas que construimos juntos."
- [ ] Imagine: manejar en la UI la cápsula compartida sin aporte del otro
- [ ] Play: cerrar concepto e implementar
- [ ] Recuerdos: implementar y desbloquear el historial de Draw
- [ ] Notificaciones push (FCM) para Talk y Draw, al terminar todas las pantallas
- [ ] Pestaña "Nosotros": boceto visual final antes de codificar

**Mejoras de módulos ya cerrados**
- [ ] Draw: botón de descarga de Reveal (rasterizar a PNG y guardar vía `MediaStore`)
- [ ] Draw: "guardar en recuerdos" debe crear el documento en `memories`
- [ ] Talk: buscador del historial
- [ ] Talk: contador de no leídos en el Home

**Calidad y publicación**
- [ ] Manejo de errores en los listeners de Draw (2), Home (4) y Perfil (3)
- [ ] Versionar `storage.rules` en el repo (entregado; falta colocarlo y desplegar)
- [ ] Restringir la API key de `google-services.json` (paquete + SHA-1) y evaluar App Check
- [ ] Activar y revisar R8 (`isMinifyEnabled`) antes de publicar

**Hechos**
- [x] Imagine fase 1 (reglas, entregadas) y fase 2 (modelos, repositorio, ViewModel) (20/10/2026)
- [x] Confirmar despliegue de `cancelPendingInvite` y `unlinkCouple` (19/10/2026)
- [x] Limpieza en 3 fases + parche final (hasta `d1cc7ca`)
- [x] `README.md` reescrito (`0a4b2b5`)
- [x] Borrar `racha.txt`
- [x] Quitar `ui-text-google-fonts`
- [x] Regla "sin comentarios" extendida a `functions/index.js` y `firestore.rules`

---

## 4. Deuda técnica y riesgos abiertos

- **Reglas de Firestore:** el patch de Imagine (sustituye el bloque `ideas`) no está probado con emulador ni desplegado; el `update` de `couples/{coupleId}` permite a cualquier miembro escribir un `streakLog` arbitrario (aceptable para uso privado; se puede acotar exigiendo que la clave nueva sea hoy).
- **Contenido huérfano al desvincular:** `couples/{coupleId}` y su contenido no se borran; como las reglas usan `users/{uid}.coupleId`, ese contenido queda sin dueño accesible. Decisión pendiente.
- **Componentes duplicados:** `TalkCommon.kt` duplica `AvatarCircle` y los blobs de `HomeScreen.kt`; sacarlos a un archivo compartido si se vuelven a necesitar.
- **Observaciones abiertas de Draw:** ver `docs/draw.md`, sección "Observaciones abiertas".
- **Listeners sin error:** ver Próximos pasos.
- **Imagine:** adjuntos de cápsulas protegidos por el secreto de la URL (no por reglas de Storage); adjuntos huérfanos al no permitir `delete`; la forma de los ítems de Listas solo la valida el cliente. Detalle en `docs/imagine.md`.

---

## 5. Bugs resueltos (referencia)

| Fecha | Bug | Causa / solución |
|---|---|---|
| 06/09 | Botón de Google sin respuesta | Se pasaba `applicationContext`; Credential Manager necesita el contexto de la Activity |
| 07/09 | `createInviteCode` fallaba con `INTERNAL` | Lecturas después de escrituras en la transacción; se reordenó |
| 18/09 | Imaginar/Hablar sin título en el bento | Dependían de un composable genérico; ahora tienen el suyo |
| 19/09 | Icono del launcher como robot genérico | Faltaban `android:icon` y `android:roundIcon` en el `<application>` |
| 20/09 | Historial de Draw duplicado y doble navegación en Reveal | Se reemplazó `archiveAndStartNewRound` por `confirmNextRound` (transacción + confirmación mutua) |
| 20/09 | Talk permitía editar el texto del otro | Reglas de `update` limitadas al campo `seen` |
