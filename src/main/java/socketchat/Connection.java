package socketchat;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class Connection {

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int ACCEPT_TIMEOUT_MS = 2_000;

    public static ServerSocket openServer(int port) throws IOException {
        ServerSocket server = new ServerSocket(port);
        server.setSoTimeout(ACCEPT_TIMEOUT_MS);
        System.out.println("Escutando na porta " + port + ".");
        return server;
    }

    public static Socket connect(String host, int port) throws IOException {
        System.out.println("Conectando a " + host + ":" + port + "...");

        Socket socket = new Socket();
        socket.connect(new InetSocketAddress(host, port), CONNECT_TIMEOUT_MS);
        return socket;
    }
}
