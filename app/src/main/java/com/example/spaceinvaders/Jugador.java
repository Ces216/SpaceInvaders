package com.example.spaceinvaders;

import android.graphics.Rect;

/**
 * Jugador: representa la nave controlada por el usuario.
 * Almacena su posicion y dimension mediante un Rect (Bounding Box),
 * que tambien se usa para la deteccion de colisiones.
 */
public class Jugador {

    private final Rect bounds;
    private final int velocidad;

    public Jugador(int x, int y, int ancho, int alto) {
        bounds = new Rect(x, y, x + ancho, y + alto);
        // La velocidad de desplazamiento es proporcional al ancho del jugador.
        velocidad = ancho / 3;
    }

    /**
     * Mueve la nave horizontalmente hacia la posicion tactil indicada.
     * Se evita que la nave salga de los limites de la pantalla.
     *
     * @param objetivoX  coordenada X del punto de toque.
     * @param anchoPantalla  ancho total del Surface.
     */
    public void moverHacia(float objetivoX, int anchoPantalla) {
        int cx = bounds.centerX();
        int mitadAncho = bounds.width() / 2;

        if (Math.abs(objetivoX - cx) > velocidad) {
            if (objetivoX < cx) {
                bounds.offset(-velocidad, 0);
            } else {
                bounds.offset(velocidad, 0);
            }
        }

        // Clamp: no salir por el borde izquierdo
        if (bounds.left < 0) {
            bounds.offsetTo(0, bounds.top);
        }
        // Clamp: no salir por el borde derecho
        if (bounds.right > anchoPantalla) {
            bounds.offsetTo(anchoPantalla - bounds.width(), bounds.top);
        }
    }

    public Rect getBounds() {
        return bounds;
    }
}