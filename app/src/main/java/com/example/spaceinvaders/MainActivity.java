package com.example.spaceinvaders;

import android.app.Activity;
import android.os.Bundle;

/**
 * MainActivity: punto de entrada de la aplicacion.
 * Unicamente se encarga de establecer el GameView como vista principal
 * y de delegar la gestion del ciclo de vida al propio SurfaceView.
 */
public class MainActivity extends Activity {

    private GameView gameView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Se instancia el GameView y se establece como contenido de la Activity.
        // No se usa ningún layout XML complejo para la pantalla de juego.
        gameView = new GameView(this);
        setContentView(gameView);
    }

    @Override
    protected void onPause() {
        super.onPause();
        // Cuando la Activity pasa a segundo plano, se pausa el hilo del juego.
        gameView.pause();
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Cuando la Activity vuelve al primer plano, se reanuda el hilo del juego.
        gameView.resume();
    }
}