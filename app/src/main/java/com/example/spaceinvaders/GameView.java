package com.example.spaceinvaders;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Rect;
import android.view.MotionEvent;
import android.view.SurfaceHolder;
import android.view.SurfaceView;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * GameView: clase principal del juego.
 * Extiende SurfaceView e implementa Runnable para ejecutar el bucle
 * de juego en un hilo secundario, separado del hilo principal de la UI.
 * Implementa SurfaceHolder.Callback para responder a los eventos del Surface.
 */
public class GameView extends SurfaceView implements Runnable, SurfaceHolder.Callback {

    // --- Hilo del juego ---
    private Thread gameThread;
    private boolean corriendo;

    // --- Surface ---
    private final SurfaceHolder holder;

    // --- Herramientas de dibujo ---
    private final Paint paint;

    // --- Dimensiones de la pantalla ---
    private int anchoP;
    private int altoP;

    // --- Entidades del juego ---
    private Jugador jugador;
    private List<Enemigo> enemigos;
    private List<Disparo> disparosJugador;
    private List<Disparo> disparosEnemigos;

    // --- Temporizadores ---
    private long tiempoUltimoDisparoEnemigo;
    private static final long INTERVALO_DISPARO_ENEMIGO = 1200; // ms

    // --- Estado del juego ---
    private boolean gameOver;
    private boolean victoria;
    private int puntuacion;

    // --- Control tactil ---
    private float touchX;

    // --- Temporizador de movimiento de enemigos ---
    private long tiempoUltimoMovimientoEnemigos;
    private static final long INTERVALO_MOVIMIENTO_ENEMIGOS = 600; // ms
    private int direccionEnemigos = 1; // 1 = derecha, -1 = izquierda

    // --- Fuente de texto ---
    private final Paint paintTexto;
    private final Paint paintGame;

    public GameView(Context context) {
        super(context);
        holder = getHolder();
        holder.addCallback(this);

        paint = new Paint();
        paint.setAntiAlias(true);

        paintTexto = new Paint();
        paintTexto.setColor(Color.WHITE);
        paintTexto.setAntiAlias(true);
        paintTexto.setTextAlign(Paint.Align.CENTER);

        paintGame = new Paint();
        paintGame.setColor(Color.RED);
        paintGame.setAntiAlias(true);
        paintGame.setTextAlign(Paint.Align.CENTER);

        corriendo = false;
        gameOver = false;
        victoria = false;
        puntuacion = 0;
        touchX = -1;
        tiempoUltimoDisparoEnemigo = System.currentTimeMillis();
        tiempoUltimoMovimientoEnemigos = System.currentTimeMillis();
    }

    /**
     * Inicializa todas las entidades del juego usando las dimensiones reales del Surface.
     */
    private void inicializarJuego() {
        int tamJugador = anchoP / 12;
        jugador = new Jugador(anchoP / 2 - tamJugador / 2,
                altoP - tamJugador * 2,
                tamJugador, tamJugador * 2);

        disparosJugador = new ArrayList<>();
        disparosEnemigos = new ArrayList<>();
        enemigos = new ArrayList<>();

        // Crear una cuadricula de enemigos (4 filas x 7 columnas)
        int cols = 7;
        int filas = 4;
        int tamEnemigo = anchoP / (cols + 4);
        int espacioH = tamEnemigo + tamEnemigo / 3;
        int espacioV = tamEnemigo + tamEnemigo / 4;
        int offsetX = (anchoP - (cols * espacioH)) / 2;
        int offsetY = (int) (altoP * 0.08f);

        for (int fila = 0; fila < filas; fila++) {
            for (int col = 0; col < cols; col++) {
                int ex = offsetX + col * espacioH;
                int ey = offsetY + fila * espacioV;
                enemigos.add(new Enemigo(ex, ey, tamEnemigo, tamEnemigo));
            }
        }

        gameOver = false;
        victoria = false;
        puntuacion = 0;
        tiempoUltimoDisparoEnemigo = System.currentTimeMillis();
        tiempoUltimoMovimientoEnemigos = System.currentTimeMillis();
    }

    // =====================================================================
    // GAME LOOP: nucleo del juego
    // Este bucle se ejecuta en un hilo separado. En cada iteracion:
    //   1. Actualiza la logica (fisica, colisiones, IA).
    //   2. Dibuja el estado actual en el canvas.
    //   3. Limita la velocidad a aproximadamente 60 FPS.
    // =====================================================================
    @Override
    public void run() {
        final long FPS_OBJETIVO = 60;
        final long MS_POR_FRAME = 1000 / FPS_OBJETIVO;

        while (corriendo) {
            if (!holder.getSurface().isValid()) continue;

            long inicioFrame = System.currentTimeMillis();

            Canvas canvas = holder.lockCanvas();
            try {
                synchronized (holder) {
                    if (!gameOver && !victoria) {
                        actualizarFisica();
                    }
                    dibujar(canvas);
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas);
                }
            }

            // Control de velocidad de frames
            long tiempoFrame = System.currentTimeMillis() - inicioFrame;
            if (tiempoFrame < MS_POR_FRAME) {
                try {
                    Thread.sleep(MS_POR_FRAME - tiempoFrame);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
    }

    /**
     * Actualiza la logica del juego: posiciones, disparos y deteccion de colisiones.
     */
    private void actualizarFisica() {
        // Mover jugador hacia la posicion tactil
        if (touchX >= 0) {
            jugador.moverHacia(touchX, anchoP);
        }

        // Mover disparos del jugador
        Iterator<Disparo> itDJ = disparosJugador.iterator();
        while (itDJ.hasNext()) {
            Disparo d = itDJ.next();
            d.actualizar();
            if (d.fueraDePantalla()) {
                itDJ.remove();
            }
        }

        // Mover disparos de enemigos
        Iterator<Disparo> itDE = disparosEnemigos.iterator();
        while (itDE.hasNext()) {
            Disparo d = itDE.next();
            d.actualizar();
            if (d.fueraDePantalla()) {
                itDE.remove();
            }
        }

        // Mover enemigos en bloque cada cierto tiempo
        long ahora = System.currentTimeMillis();
        if (ahora - tiempoUltimoMovimientoEnemigos > INTERVALO_MOVIMIENTO_ENEMIGOS) {
            tiempoUltimoMovimientoEnemigos = ahora;
            moverBloqueEnemigos();
        }

        // Disparos aleatorios de enemigos
        if (ahora - tiempoUltimoDisparoEnemigo > INTERVALO_DISPARO_ENEMIGO && !enemigos.isEmpty()) {
            tiempoUltimoDisparoEnemigo = ahora;
            int indice = (int) (Math.random() * enemigos.size());
            Enemigo tirador = enemigos.get(indice);
            int cx = tirador.getBounds().centerX();
            int cy = tirador.getBounds().bottom;
            disparosEnemigos.add(new Disparo(cx, cy, altoP / 100 * 2, altoP, false));
        }

        // Colision: disparos del jugador vs enemigos
        Iterator<Disparo> itD = disparosJugador.iterator();
        while (itD.hasNext()) {
            Disparo d = itD.next();
            Iterator<Enemigo> itE = enemigos.iterator();
            while (itE.hasNext()) {
                Enemigo e = itE.next();
                // Deteccion de colision mediante Bounding Box (Rect.intersect)
                if (Rect.intersects(d.getBounds(), e.getBounds())) {
                    itD.remove();
                    itE.remove();
                    puntuacion += 10;
                    break;
                }
            }
        }

        // Colision: disparos enemigos vs jugador
        Iterator<Disparo> itDE2 = disparosEnemigos.iterator();
        while (itDE2.hasNext()) {
            Disparo d = itDE2.next();
            if (Rect.intersects(d.getBounds(), jugador.getBounds())) {
                gameOver = true;
                return;
            }
        }

        // Colision: enemigos llegan al jugador (invasion completada)
        for (Enemigo e : enemigos) {
            if (e.getBounds().bottom >= jugador.getBounds().top) {
                gameOver = true;
                return;
            }
        }

        // Victoria
        if (enemigos.isEmpty()) {
            victoria = true;
        }
    }

    /**
     * Mueve todos los enemigos a la derecha o izquierda en bloque.
     * Cuando alguno toca el borde de la pantalla, el bloque baja y cambia de direccion.
     */
    private void moverBloqueEnemigos() {
        int paso = anchoP / 30;
        boolean bajar = false;

        for (Enemigo e : enemigos) {
            int nuevaX = e.getBounds().left + (paso * direccionEnemigos);
            if (nuevaX <= 0 || nuevaX + e.getBounds().width() >= anchoP) {
                bajar = true;
                break;
            }
        }

        if (bajar) {
            direccionEnemigos *= -1;
            int bajada = altoP / 25;
            for (Enemigo e : enemigos) {
                e.mover(0, bajada);
            }
        } else {
            for (Enemigo e : enemigos) {
                e.mover(paso * direccionEnemigos, 0);
            }
        }
    }

    /**
     * Dibuja todos los elementos del juego sobre el canvas.
     */
    private void dibujar(Canvas canvas) {
        // Fondo negro (espacio)
        canvas.drawColor(Color.BLACK);

        if (gameOver) {
            dibujarPantallaFin(canvas, false);
            return;
        }
        if (victoria) {
            dibujarPantallaFin(canvas, true);
            return;
        }

        // Dibujar jugador (nave)
        paint.setColor(Color.CYAN);
        canvas.drawRect(jugador.getBounds(), paint);

        // Detalle visual: "canion" de la nave
        Rect b = jugador.getBounds();
        paint.setColor(Color.WHITE);
        int cx = b.centerX();
        canvas.drawRect(cx - b.width() / 10, b.top - b.height() / 4,
                cx + b.width() / 10, b.top, paint);

        // Dibujar enemigos
        paint.setColor(Color.GREEN);
        for (Enemigo e : enemigos) {
            canvas.drawRect(e.getBounds(), paint);
            // Detalle visual: "antenas"
            Rect eb = e.getBounds();
            int ecx = eb.centerX();
            paint.setColor(Color.YELLOW);
            canvas.drawRect(ecx - eb.width() / 3, eb.top - eb.height() / 4,
                    ecx - eb.width() / 6, eb.top, paint);
            canvas.drawRect(ecx + eb.width() / 6, eb.top - eb.height() / 4,
                    ecx + eb.width() / 3, eb.top, paint);
            paint.setColor(Color.GREEN);
        }

        // Dibujar disparos del jugador
        paint.setColor(Color.YELLOW);
        for (Disparo d : disparosJugador) {
            canvas.drawRect(d.getBounds(), paint);
        }

        // Dibujar disparos de enemigos
        paint.setColor(Color.RED);
        for (Disparo d : disparosEnemigos) {
            canvas.drawRect(d.getBounds(), paint);
        }

        // HUD: puntuacion
        paintTexto.setTextSize(altoP / 28f);
        canvas.drawText("Puntuacion: " + puntuacion, anchoP / 2f, altoP / 20f, paintTexto);

        // Instrucciones
        paintTexto.setTextSize(altoP / 45f);
        canvas.drawText("Arrastra para mover | Toca para disparar", anchoP / 2f, altoP - altoP / 30f, paintTexto);
    }

    /**
     * Muestra la pantalla de fin de partida (game over o victoria).
     */
    private void dibujarPantallaFin(Canvas canvas, boolean gano) {
        paintGame.setTextSize(altoP / 12f);
        if (gano) {
            paintGame.setColor(Color.GREEN);
            canvas.drawText("VICTORIA", anchoP / 2f, altoP / 3f, paintGame);
        } else {
            paintGame.setColor(Color.RED);
            canvas.drawText("GAME OVER", anchoP / 2f, altoP / 3f, paintGame);
        }
        paintGame.setColor(Color.WHITE);
        paintGame.setTextSize(altoP / 22f);
        canvas.drawText("Puntuacion: " + puntuacion, anchoP / 2f, altoP / 2f, paintGame);
        paintGame.setTextSize(altoP / 30f);
        canvas.drawText("Toca para reiniciar", anchoP / 2f, altoP * 2 / 3f, paintGame);
    }

    // =====================================================================
    // GESTION DEL CICLO DE VIDA DEL SURFACE
    // =====================================================================

    @Override
    public void surfaceCreated(SurfaceHolder surfaceHolder) {
        // El Surface esta listo. Se obtienen las dimensiones reales y se inicializa el juego.
        anchoP = getWidth();
        altoP = getHeight();
        inicializarJuego();
        resume();
    }

    @Override
    public void surfaceChanged(SurfaceHolder surfaceHolder, int format, int width, int height) {
        // No se necesita accion especial ante cambios de superficie.
    }

    @Override
    public void surfaceDestroyed(SurfaceHolder surfaceHolder) {
        // El Surface va a ser destruido. Se detiene el hilo para evitar fugas de memoria.
        pause();
    }

    /**
     * Pausa el hilo del juego de forma segura.
     * Se usa join() para asegurar que el hilo ha terminado antes de continuar.
     */
    public void pause() {
        corriendo = false;
        if (gameThread != null) {
            try {
                gameThread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
            gameThread = null;
        }
    }

    /**
     * Reanuda el hilo del juego creando una nueva instancia de Thread.
     */
    public void resume() {
        corriendo = true;
        gameThread = new Thread(this);
        gameThread.start();
    }

    // =====================================================================
    // ENTRADA TACTIL
    // =====================================================================

    @Override
    public boolean onTouchEvent(MotionEvent event) {
        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
            case MotionEvent.ACTION_MOVE:
                // Si hay game over o victoria, un toque reinicia la partida.
                if (gameOver || victoria) {
                    inicializarJuego();
                    return true;
                }
                touchX = event.getX();

                // Disparar al tocar (ACTION_DOWN unicamente)
                if (event.getAction() == MotionEvent.ACTION_DOWN) {
                    int cx = jugador.getBounds().centerX();
                    int cy = jugador.getBounds().top;
                    int velDisparo = altoP / 100 * 3;
                    disparosJugador.add(new Disparo(cx, cy, velDisparo, altoP, true));
                }
                break;

            case MotionEvent.ACTION_UP:
                touchX = -1;
                break;
        }
        return true;
    }
}