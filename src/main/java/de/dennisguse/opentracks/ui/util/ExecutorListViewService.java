package de.dennisguse.opentracks.ui.util;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

/**
 * ExecutorService wrap that avoid the execution of the same object in a ListView.
 *
 * @param <T> the type of the object that will be used to identified the Runnable.
 */
public class ExecutorListViewService<T> {

    private final List<T> enqueueObjects = new ArrayList<>();
    private final ExecutorService executorService;

    public ExecutorListViewService(int numThreads) {
        executorService = Executors.newFixedThreadPool(numThreads);
    }

    public void shutdown() {
        enqueueObjects.clear();
        executorService.shutdown();
    }

    /**
     * Execute the runnable for the object.
     *
     * @param object   the Object.
     * @param runnable the Runnable.
     */
    public void execute(T object, Runnable runnable) {
        if (!preExecute(object)) {
            return;
        }

        new Thread(() -> {
            Future<?> future = executorService.submit(runnable);
            try {
                future.get();
            } catch (ExecutionException | InterruptedException e) {
                e.printStackTrace();
            }
            postExecute(object);
        }).start();
    }

    /**
     * Before execution it must checks if the object is already enqueued.
     *
     * @param object the object to check.
     * @return       true if it can be executed or false otherwise.
     */
    private boolean preExecute(T object) {
        synchronized (enqueueObjects) {
            if (!enqueueObjects.contains(object)) {
                enqueueObjects.add(object);
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * After execution remove the object from the queue.
     *
     * @param object the object to be removed from the queue.
     */
    private void postExecute(T object) {
        synchronized (enqueueObjects) {
            enqueueObjects.remove(object);
        }
    }
}
