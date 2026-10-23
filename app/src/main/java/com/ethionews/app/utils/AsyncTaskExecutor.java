package com.ethionews.app.utils;

import android.os.Handler;
import android.os.Looper;

import androidx.annotation.NonNull;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public abstract class AsyncTaskExecutor<P, B, R> {

    ExecutorService executor;
    private Handler handler;

    protected AsyncTaskExecutor() {
        executor = Executors.newSingleThreadExecutor(r -> {
            Thread t = new Thread(r);
            t.setDaemon(true);
            return t;
        });
    }

    public ExecutorService getExecutor() {
        return executor;
    }

    public Handler getHandler() {
        if (handler == null) {
            synchronized (AsyncTaskExecutor.class) {
                handler = new Handler(Looper.getMainLooper());
            }
        }
        return handler;
    }

    protected void onPreExecute() {
        // Override this method wherever you want to perform task before background execution get started
    }

    protected abstract R doInBackground(P params);

    protected abstract void onPostExecute(R result);

    protected void onProgressUpdate(@NonNull B value) {
        // Override this method wherever you want update a progress result
    }

    // used for push progress report to UI
    public void publishProgress(@NonNull B value) {
        getHandler().post(() -> onProgressUpdate(value));
    }

    public void execute() {
        execute(null);
    }

    public void execute(P params) {
        getHandler().post(() -> {
            onPreExecute();
            executor.execute(() -> {
                R result = doInBackground(params);
                getHandler().post(() -> onPostExecute(result));
            });
        });
    }

    // Cancel the task
    public void shutDown() {
        if (executor != null && !executor.isShutdown()) {
            executor.shutdownNow();
        }
    }

    // Check if the task is cancelled
    public boolean isCancelled() {
        return executor == null || executor.isTerminated() || executor.isShutdown();
    }
}