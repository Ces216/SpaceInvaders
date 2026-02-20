package com.example.spaceinvaders;

import android.graphics.Rect;

/**
 * Enemigo: representa cada uno de los invasores.
 * Su movimiento horizontal es gestionado en bloque desde GameView.
 * Solo necesita almacenar su Bounding Box para posicion y colision.
 */
public class Enemigo {

    private final Rect bounds;

    public Enemigo(int x, int y, int ancho, int alto) {
        bounds = new Rect(x, y, x + ancho, y + alto);
    }

    /**
     * Desplaza al enemigo la cantidad indicada en X e Y.
     *
     * @param dx  desplazamiento horizontal en pixeles.
     * @param dy  desplazamiento vertical en pixeles.
     */
    public void mover(int dx, int dy) {
        bounds.offset(dx, dy);
    }

    public Rect getBounds() {
        return bounds;
    }
}