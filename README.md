# Extras (Fabric 1.21.1)

Mobs y objetos nuevos:

| Cosa | Resumen |
|---|---|
| **Charger** | 26 corazones. Golpe: 3 corazones. Embestida: si agarra a un jugador le quita 6 corazones cada 2 s. Si choca con un bloque se aturde ~5 s. |
| **Paloma** | 7 corazones, vuela alto. Elige un jugador al azar y le suelta excremento explosivo (explosión de TNT, 4 corazones). |
| **Lanzallamas del Pyro** | 5 corazones + prende fuego. 100 de gas, gasta 2 cada 3 s. No se rompe. Destello dorado con Loot Beams Up. |
| **Balón de gas** | Recarga 100 de gas. |
| **Tótem de la Paloma** | Como el normal + Resistencia II (3 s) + Salto II (5 s). |
| **Tótem Miku** | Como el normal + Fuerza II (4 s) + Velocidad II (6 s). |

Todo sale en la pestaña **Extras** del inventario creativo (los mobs con su huevo generador).

## Cómo usarlos
- **Lanzallamas**: mantén clic derecho. Recargar: *Shift + clic derecho* (gasta un balón del inventario) o balón en una mano y lanzallamas en la otra.
- **Tótems**: sostenlos en cualquier mano; se activan solos cuando vas a morir.

## Compilar
Necesitas Java 21. Fácil: sube esta carpeta a GitHub y la pestaña *Actions* te da el `.jar`
(el archivo `.github/workflows/build.yml` ya está incluido). O abre la carpeta con IntelliJ IDEA y ejecuta la tarea Gradle `build`.
El mod queda en `build/libs/extras-1.0.0.jar` (no uses el `-sources`).

## Instalar
Pon el `.jar` en la carpeta `mods` junto con **Fabric API** y **GeckoLib 4.7+** (para Minecraft 1.21.1).
Opcional: **Loot Beams Up** para el destello dorado del lanzallamas.

## Ajustar números
Todo está en `src/main/java/com/extras/Balance.java`: vida, daños, tiempos, velocidad, si la bomba rompe bloques, etc.

## Si los mobs se ven raros
Los modelos y animaciones de los mobs están convertidos desde Blockbench. Si algo se ve al revés o con la textura cambiada de lado:
1. Abre `blockbench/charger.bbmodel` (o `paloma.bbmodel`) en Blockbench.
2. *File → Export → Export Bedrock Geometry* y reemplaza `src/main/resources/assets/extras/geo/entity/<mob>.geo.json`.
3. En el panel de animaciones: *Export Animations* y reemplaza `.../animations/entity/<mob>.animation.json`.
