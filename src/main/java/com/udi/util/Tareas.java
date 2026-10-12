package com.udi.util;

import com.udi.exception.DatosException;
import com.udi.exception.ReglaNegocioException;
import javafx.concurrent.Task;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

/**
 * Ejecuta llamadas a los servicios fuera del hilo de la interfaz para que la
 * ventana no se congele mientras Oracle responde. Las tareas se ejecutan una
 * a la vez y en orden, porque la aplicación comparte una sola conexión.
 */
public final class Tareas {

    private static final ExecutorService HILO = Executors.newSingleThreadExecutor(r -> {
        Thread hilo = new Thread(r, "base-de-datos");
        hilo.setDaemon(true);
        return hilo;
    });

    private Tareas() {
    }

    /**
     * Ejecuta {@code trabajo} en segundo plano y entrega el resultado a
     * {@code alTerminar} en el hilo de la interfaz. Si falla, muestra el error.
     */
    public static <T> void ejecutar(Callable<T> trabajo, Consumer<T> alTerminar) {
        Task<T> tarea = new Task<>() {
            @Override
            protected T call() throws Exception {
                return trabajo.call();
            }
        };
        tarea.setOnSucceeded(e -> alTerminar.accept(tarea.getValue()));
        tarea.setOnFailed(e -> mostrarError(tarea.getException()));
        HILO.execute(tarea);
    }

    /** Igual que {@link #ejecutar(Callable, Consumer)} para operaciones sin resultado. */
    public static void accion(Runnable trabajo, Runnable alTerminar) {
        ejecutar(() -> {
            trabajo.run();
            return null;
        }, r -> alTerminar.run());
    }

    private static void mostrarError(Throwable error) {
        if (error instanceof ReglaNegocioException) {
            Alertas.error("No se pudo completar la operación", error.getMessage());
        } else if (error instanceof DatosException) {
            Alertas.error("Error de base de datos", error.getMessage());
        } else {
            Alertas.error("Error inesperado", String.valueOf(error));
        }
    }
}
