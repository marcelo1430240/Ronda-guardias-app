# Ronda de Guardias - App Android

Wrapper Android (WebView + Kotlin) de la plataforma `RondaGuardias`
(el servidor FastAPI, ver ese proyecto hermano). No duplica logica: el
escaneo de QR, los formularios y el guardado en la base pasan por la
pagina web del servidor, esta app solo le da un icono propio, pantalla
completa, y resuelve 2 cosas que un navegador comun no hace solo:

1. **Permiso de camara**: sin este puente, la pagina pide la camara via
   `getUserMedia` y Android nunca responde.
2. **Certificado autofirmado**: el servidor de la tienda corre con
   HTTPS autofirmado (necesario para que la camara funcione); esta app
   lo acepta directo sin mostrarle al guardia ningun dialogo tecnico.

## Por que no es una app nativa "de verdad"

Para no duplicar la logica de escaneo QR en dos lugares (la pagina web
y una app nativa en Kotlin), lo que mantendria 2 versiones del mismo
codigo sincronizadas a mano, esta app simplemente muestra
`https://rondaguardias.local:8800/escanear` a pantalla completa. Si el
dia de mañana el servidor cambia, esta app no necesita ningun cambio.

## Configuracion del servidor

Por defecto apunta a `rondaguardias.local:8800` (el nombre que el
servidor publica solo en la red via mDNS, no depende de la IP de la
PC). Si en algun local eso no funciona, desde el menu (3 puntitos) ->
**Configurar servidor** se puede poner la IP de la PC a mano como
respaldo.

## Compilar

Igual que `ConteoTMR-App`: push a `main` -> GitHub Actions compila el
`.apk` -> se baja como artifact "ronda-guardias-debug-apk" (no se
puede compilar localmente en este entorno porque el Android SDK esta
bloqueado por la red corporativa).

Para compilar localmente (fuera de este entorno) hace falta Gradle +
Android SDK instalados:

```
gradle assembleDebug
```
