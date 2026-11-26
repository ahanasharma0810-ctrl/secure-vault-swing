package com.securevault.ui;

import javax.swing.SwingWorker;
import java.util.concurrent.ExecutionException;
import java.util.function.Consumer;

/**
 * Runs slow work (password hashing, file access) on a background thread so the
 * window never freezes, then calls back on the Swing thread with the result.
 */
public final class BackgroundTask {

    private BackgroundTask() {
    }

    /** Work that returns a value and may throw. */
    @FunctionalInterface
    public interface Work<T> {
        T run() throws Exception;
    }

    public static <T> void run(Work<T> work, Consumer<T> onSuccess, Consumer<Throwable> onError) {
        new SwingWorker<T, Void>() {
            @Override
            protected T doInBackground() throws Exception {
                return work.run();
            }

            @Override
            protected void done() {
                try {
                    onSuccess.accept(get());
                } catch (ExecutionException e) {
                    onError.accept(e.getCause() != null ? e.getCause() : e);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    onError.accept(e);
                }
            }
        }.execute();
    }
}
