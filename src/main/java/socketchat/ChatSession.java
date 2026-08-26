package socketchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

public final class ChatSession {

    private static final String QUIT_COMMAND = "/quit";

    private ChatSession() {
    }

    public static void run(Socket socket, String name) {
        System.out.println("Connected to " + socket.getRemoteSocketAddress() + ".");
        System.out.println("You are '" + name + "'. Type a message and press Enter. Type " + QUIT_COMMAND + " to leave.");
        System.out.println();

        CountDownLatch done = new CountDownLatch(1);

        Thread receiver = new Thread(() -> receiveLoop(socket, done), "chat-receive");
        Thread sender = new Thread(() -> sendLoop(socket, name, done), "chat-send");
        receiver.setDaemon(true);
        sender.setDaemon(true);
        receiver.start();
        sender.start();

        try {
            done.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        try {
            socket.shutdownInput();
        } catch (IOException ignored) {
        }
        try {
            socket.shutdownOutput();
        } catch (IOException ignored) {
        }

        System.out.println("Session closed.");
    }

    private static void receiveLoop(Socket socket, CountDownLatch done) {
        try {
            InputStream in = socket.getInputStream();
            while (true) {
                byte[] frame = Frames.read(in);
                if (frame == null) {
                    System.out.println("[peer left the conversation]");
                    return;
                }
                System.out.println(new String(frame, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("[receive failed: " + e.getClass().getSimpleName() + " - " + e.getMessage() + "]");
            }
        } finally {
            done.countDown();
        }
    }

    private static void sendLoop(Socket socket, String name, CountDownLatch done) {
        try {
            OutputStream out = socket.getOutputStream();
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            while (true) {
                String line = stdin.readLine();

                if (line == null || line.equalsIgnoreCase(QUIT_COMMAND)) {
                    return;
                }
                if (line.isEmpty()) {
                    continue;
                }

                byte[] payload = (name + ": " + line).getBytes(StandardCharsets.UTF_8);
                Frames.write(out, payload);
            }
        } catch (IOException e) {
            if (!socket.isClosed()) {
                System.err.println("[send failed: " + e.getClass().getSimpleName() + " - " + e.getMessage() + "]");
            }
        } finally {
            done.countDown();
        }
    }
}
