package com.example.spaceinvaders;

import android.graphics.Rect;

/**
 * Disparo: representa un proyectil en vuelo, ya sea del jugador o de un enemigo.
 * La direccion del movimiento viene determinada por el parametro esDelJugador:
 *   - Jugador: el disparo sube (velocidad negativa en Y).
 *   - Enemigo: el disparo baja (velocidad positiva en Y).
 */
public class Disparo {

    private final Rect bounds;
    private final int velocidadY;
    private final int altoPantalla;

    // Dimensiones del proyectil en pixeles
    private static final int ANCHO_DISPARO = 6;

    public Disparo(int cx, int cy, int velocidad, int altoPantalla, boolean esDelJugador) {
        int altoDisparo = altoPantalla / 25;
        bounds = new Rect(
                cx - ANCHO_DISPARO / 2,
                cy - altoDisparo,
                cx + ANCHO_DISPARO / 2,
                cy
        );
        // El jugador dispara hacia arriba (Y decrece), el enemigo hacia abajo (Y crece).
        this.velocidadY = esDelJugador ? -velocidad : velocidad;
        this.altoPantalla = altoPantalla;
    }

    /**
     * Actualiza la posicion del disparo desplazandolo segun su velocidad.
     * Llamado una vez por frame desde el game loop.
     */
    public void actualizar() {
        bounds.offset(0, velocidadY);
    }

    /**
     * Indica si el disparo ha salido de la pantalla (arriba o abajo).
     *
     * @return true si ya no es visible y debe eliminarse de la lista.
     */
    public boolean fueraDePantalla() {
        return bounds.bottom < 0 || bounds.top > altoPantalla;
    }

    public Rect getBounds() {
        return bounds;
    }
}