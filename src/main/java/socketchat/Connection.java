package socketchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public final class Connection {

    private static final int CONNECT_TIMEOUT_MS = 10_000;

    private Connection() {
    }

    public static Socket listen(int port) throws IOException {
        ServerSocket listener = new ServerSocket();
        Cancellation.register(listener);
        try {
            listener.setReuseAddress(true);
            listener.bind(new InetSocketAddress(port));

            System.out.println("Listening on " + listener.getLocalSocketAddress() + ". Waiting for a peer...");

            Socket peer = listener.accept();
            peer.setTcpNoDelay(true);
            Cancellation.register(peer);
            return peer;
        } finally {
            listener.close();
        }
    }

    public static Socket connect(String host, int port) throws IOException {
        Socket socket = new Socket();
        Cancellation.register(socket);

        System.out.println("Connecting to " + host + ":" + port + "...");

        try {
            socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
            socket.setTcpNoDelay(true);
            return socket;
        } catch (IOException e) {
            socket.close();
            throw e;
        }
    }
}
