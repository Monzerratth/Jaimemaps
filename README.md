# jaimemaps

Aplicación Android (Java) que muestra un mapa de Google Maps con la **ubicación actual del usuario**, permite **añadir marcadores** y **capturar y mostrar una imagen** con la cámara.

## Funcionalidades

- Mostrar la ubicación actual en un mapa (punto azul y marcador azul "Ubicación actual").
- Tocar cualquier punto del mapa para añadir un marcador con su latitud y longitud.
- Botón para eliminar los marcadores añadidos.
- Botón de cámara: captura una imagen y la muestra sobre el mapa (al tocarla se oculta).
- Botón para volver a centrar el mapa en la ubicación actual.
- Permisos de ubicación solicitados en tiempo de ejecución.

## Instrucciones de Ejecución

1. Clona el repositorio: `git clone https://github.com/USERNAME/NOMBRE_REPO.git`
2. Abre el proyecto en Android Studio y sincroniza Gradle.
3. Añade tu API Key de Google Maps en `app/src/main/res/values/google_maps_api.xml`
   (reemplaza `TU_API_KEY_AQUI`; debe tener habilitado *Maps SDK for Android*).
4. Ejecuta la aplicación en un dispositivo o emulador Android con Google Play Services.
   En el emulador, fija una ubicación en *Extended controls → Location*.

## Tecnologías

Java 11 · AGP 9.3.3 · Gradle 9.5.0 · compileSdk/targetSdk 37 · minSdk 30 ·
play-services-maps 19.0.0 · play-services-location 21.3.0

## Archivos principales

- `AndroidManifest.xml`: permisos de ubicación y API Key.
- `MainActivity.java`: mapa, ubicación, marcadores y cámara.
- `res/layout/activity_main.xml`: mapa y botones.
- `res/values/google_maps_api.xml`: API Key de Google Maps.
