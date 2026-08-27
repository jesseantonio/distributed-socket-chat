package socketchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class ChatSession {

    private static final String QUIT_COMMAND = "/quit";
    private static final String LIST_COMMAND = "/list";

    /** Uma thread dessas por par conectado: fica escutando o que aquele par manda. */
    public static void receiveLoop(PeerConnection peer, PeerTable table) {
        try {
            while (true) {
                byte[] frame;
                try {
                    frame = Frames.read(peer.in);
                } catch (SocketTimeoutException e) {
                    // ninguém mandou nada dentro do prazo; o par pode só estar quieto, tenta de novo
                    continue;
                }
                if (frame == null) {
                    break;
                }
                System.out.println(peer.nickname + ": " + new String(frame, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
            // conexão caiu de verdade; tratado abaixo como se o par tivesse saído
        }

        table.remove(peer.nickname);
        System.out.println("[" + peer.nickname + " saiu da conversa]");
        peer.close();
    }

    /** Só uma thread dessas no processo inteiro: lê o teclado e manda pra todo mundo. */
    public static void consoleLoop(PeerTable table, String myNickname) {
        System.out.println("Você é '" + myNickname + "'. Digite uma mensagem e pressione Enter.");
        System.out.println("Comandos: " + LIST_COMMAND + " (participantes), " + QUIT_COMMAND + " (sair).");
        System.out.println();

        try {
            BufferedReader stdin = new BufferedReader(new InputStreamReader(System.in, StandardCharsets.UTF_8));
            String line;
            while ((line = stdin.readLine()) != null) {
                if (line.equalsIgnoreCase(QUIT_COMMAND)) {
                    break;
                }
                if (line.equalsIgnoreCase(LIST_COMMAND)) {
                    printParticipants(table, myNickname);
                    continue;
                }
                if (!line.isEmpty()) {
                    broadcast(table, line);
                }
            }
        } catch (IOException ignored) {
        }
    }

    private static void printParticipants(PeerTable table, String myNickname) {
        System.out.println("Participantes:");
        System.out.println("  - " + myNickname + " (você)");
        for (PeerConnection peer : table.all()) {
            System.out.println("  - " + peer.nickname);
        }
    }

    private static void broadcast(PeerTable table, String line) {
        byte[] payload = line.getBytes(StandardCharsets.UTF_8);

        if (payload.length > Frames.MAX_MESSAGE_SIZE) {
            System.out.println("[mensagem não enviada: " + payload.length + " bytes excede o limite de " + Frames.MAX_MESSAGE_SIZE + " bytes]");
            return;
        }

        for (PeerConnection peer : table.all()) {
            peer.send(payload);
        }
    }
}
