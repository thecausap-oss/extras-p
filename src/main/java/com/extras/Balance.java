package com.extras;

/** Todos los numeros del mod en un solo lugar. 1 corazon = 2 de vida. */
public final class Balance {
    private Balance() {}

    // ---------- Lanzallamas ----------
    public static final int MAX_GAS = 100;               // municion maxima del lanzallamas y del balon
    public static final int GAS_COST = 2;                // gas que gasta ...
    public static final int GAS_INTERVAL_TICKS = 60;     // ... cada 3 segundos de uso (20 ticks = 1 s)
    public static final float FLAME_DAMAGE = 10.0f;      // 5 corazones
    public static final int FLAME_DAMAGE_INTERVAL = 20;  // el dano se aplica 1 vez por segundo a cada objetivo
    public static final double FLAME_RANGE = 8.0;        // alcance de las llamas en bloques
    public static final int BURN_TICKS = 120;            // 6 s ardiendo

    // ---------- Charger ----------
    public static final float CHARGER_HEALTH = 52.0f;    // 26 corazones
    public static final float CHARGER_HIT_DAMAGE = 6.0f; // 3 corazones por golpe
    public static final double CHARGER_SPEED = 0.34;     // mas rapido que un jugador caminando/corriendo
    public static final double CHARGE_SPEED = 0.62;      // bloques por tick durante la embestida
    public static final int CHARGE_WINDUP_TICKS = 25;
    public static final int CHARGE_MAX_TICKS = 50;
    public static final int CHARGE_COOLDOWN = 160;       // 8 s entre embestidas
    public static final float GRAB_DAMAGE = 12.0f;       // 6 corazones
    public static final int GRAB_FIRST_HIT = 20;         // primer golpe al segundo de agarrar
    public static final int GRAB_INTERVAL = 40;          // despues cada 2 segundos
    public static final int STUN_TICKS = 100;            // 5 s aturdido si se choca

    // ---------- Paloma ----------
    public static final float PALOMA_HEALTH = 14.0f;     // 7 corazones
    public static final double PALOMA_HEIGHT = 9.0;      // altura sobre el jugador
    public static final int BOMB_COOLDOWN_MIN = 80;
    public static final int BOMB_COOLDOWN_MAX = 140;
    public static final float BOMB_POWER = 4.0f;         // radio de explosion igual al TNT
    public static final float BOMB_DAMAGE = 8.0f;        // 4 corazones por explosion
    public static final double BOMB_DAMAGE_RADIUS = 6.0;
    public static final boolean BOMB_BREAKS_BLOCKS = true; // false = solo hace dano, no rompe bloques
}
