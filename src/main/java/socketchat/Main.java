package socketchat;

import java.io.IOException;
import java.io.PrintStream;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class Main {

    public static void main(String[] args) {
        System.setOut(new PrintStream(System.out, true, StandardCharsets.UTF_8));
        System.setErr(new PrintStream(System.err, true, StandardCharsets.UTF_8));

        try {
            NodeConfig config = NodeConfigLoader.load(args);
            PeerTable table = new PeerTable();

            ServerSocket server = Connection.openServer(config.port);
            startAcceptorThread(server, config.nickname, table);

            for (PeerAddress address : config.knownPeers) {
                connectToPeer(address, config.nickname, table);
            }

            ChatSession.consoleLoop(table, config.nickname);

        } catch (Exception e) {
            System.err.println("Erro: " + e.getMessage());
            printUsage();
            System.exit(1);
        }
    }

    /** Fica aceitando conexões de novos pares o processo inteiro, uma por vez. */
    private static void startAcceptorThread(ServerSocket server, String myNickname, PeerTable table) {
        Thread acceptor = new Thread(() -> {
            while (true) {
                Socket socket;
                try {
                    socket = server.accept();
                } catch (SocketTimeoutException e) {
                    // ninguém tentou conectar dentro do prazo; segue esperando
                    continue;
                } catch (IOException e) {
                    // a própria porta de escuta caiu; não tem mais o que aceitar
                    break;
                }

                // uma conexão aceita com handshake ruim (ou lento demais) não pode impedir
                // que a gente continue aceitando as próximas — por isso esse try é separado
                try {
                    PeerConnection peer = PeerConnection.handshake(socket, myNickname);
                    table.add(peer);
                    System.out.println("[" + peer.nickname + " entrou na conversa]");

                    Thread receiver = new Thread(() -> ChatSession.receiveLoop(peer, table));
                    receiver.setDaemon(true);
                    receiver.start();
                } catch (IOException e) {
                    System.out.println("[conexão recusada: handshake falhou - " + e.getMessage() + "]");
                    closeQuietly(socket);
                }
            }
        });
        acceptor.setDaemon(true);
        acceptor.start();
    }

    private static void connectToPeer(PeerAddress address, String myNickname, PeerTable table) {
        Socket socket;
        try {
            socket = Connection.connect(address.host, address.port);
        } catch (IOException e) {
            System.out.println("[não foi possível conectar a " + address + ": " + e.getMessage() + "]");
            return;
        }

        try {
            PeerConnection peer = PeerConnection.handshake(socket, myNickname);
            table.add(peer);
            System.out.println("[conectado a " + peer.nickname + "]");

            Thread receiver = new Thread(() -> ChatSession.receiveLoop(peer, table));
            receiver.setDaemon(true);
            receiver.start();
        } catch (IOException e) {
            System.out.println("[não foi possível conectar a " + address + ": " + e.getMessage() + "]");
            closeQuietly(socket);
        }
    }

    private static void closeQuietly(Socket socket) {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }

    private static void printUsage() {
        System.out.println();
        System.out.println("Uso:");
        System.out.println("  --port <porta> --nick <apelido> [--peers host:porta,host:porta,...]");
        System.out.println("  --config <arquivo>   (arquivo properties com port=, nick=, peers=)");
        System.out.println();
        System.out.println("Exemplo:");
        System.out.println("  java -jar socket-chat.jar --port 9001 --nick alice --peers 127.0.0.1:9002,127.0.0.1:9003");
    }
}
