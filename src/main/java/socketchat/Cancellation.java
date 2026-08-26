package socketchat;

import java.io.Closeable;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

/**
 * Ctrl+C support for blocking socket calls: registers a JVM shutdown hook that closes
 * whatever socket is currently active, which unblocks accept/connect/read/write with an
 * IOException so the program can shut down instead of being killed outright.
 */
public final class Cancellation {

    private static final AtomicReference<Closeable> active = new AtomicReference<>();
    private static final AtomicBoolean cancelled = new AtomicBoolean(false);

    static {
        Runtime.getRuntime().addShutdownHook(new Thread(Cancellation::cancel));
    }

    private Cancellation() {
    }

    public static void register(Closeable closeable) {
        active.set(closeable);
    }

    public static void clear() {
        active.set(null);
    }

    public static boolean isCancelled() {
        return cancelled.get();
    }

    private static void cancel() {
        cancelled.set(true);
        Closeable c = active.getAndSet(null);
        if (c != null) {
            try {
                c.close();
            } catch (IOException ignored) {
            }
        }
    }
}
