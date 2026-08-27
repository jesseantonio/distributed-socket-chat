package socketchat;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

public class ChatSession {

    private static final String QUIT_COMMAND = "/quit";
    private static final String LIST_COMMAND = "/list";
    private static final String PRIVATE_MSG_COMMAND = "/msg";

    public static void receiveLoop(PeerConnection peer, PeerTable table) {
        try {
            while (true) {
                byte[] frame;
                try {
                    frame = Frames.read(peer.in);
                } catch (SocketTimeoutException e) {
                    continue;
                }
                if (frame == null) {
                    break;
                }
                System.out.println(peer.nickname + ": " + new String(frame, StandardCharsets.UTF_8));
            }
        } catch (IOException e) {
        }

        table.remove(peer.nickname);
        System.out.println("[" + peer.nickname + " saiu da conversa]");
        peer.close();
    }

    public static void consoleLoop(PeerTable table, String myNickname) {
        System.out.println("Você é '" + myNickname + "'. Digite uma mensagem e pressione Enter.");
        System.out.println("Comandos: " + LIST_COMMAND + " (participantes), " + PRIVATE_MSG_COMMAND
                + " apelido texto (mensagem privada), " + QUIT_COMMAND + " (sair).");
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
                if (isPrivateMessageCommand(line)) {
                    privateMessage(table, line);
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

    private static boolean isPrivateMessageCommand(String line) {
        int len = PRIVATE_MSG_COMMAND.length();

        return line.regionMatches(true, 0, PRIVATE_MSG_COMMAND, 0, len)
                && (line.length() == len || line.charAt(len) == ' ');
    }

    private static void privateMessage(PeerTable table, String line) {
        String rest = line.substring(PRIVATE_MSG_COMMAND.length()).trim();
        int spaceIndex = rest.indexOf(' ');
        String targetNickname = spaceIndex < 0 ? rest : rest.substring(0, spaceIndex);
        String text = spaceIndex < 0 ? "" : rest.substring(spaceIndex + 1).trim();

        if (targetNickname.isEmpty() || text.isEmpty()) {
            System.out.println("[uso: " + PRIVATE_MSG_COMMAND + " apelido texto]");
            return;
        }

        PeerConnection target = table.find(targetNickname);
        if (target == null) {
            System.out.println("[" + targetNickname + " não está na conversa]");
            return;
        }

        byte[] payload = ("[privado] " + text).getBytes(StandardCharsets.UTF_8);
        if (!withinSizeLimit(payload)) {
            return;
        }

        target.send(payload);
        System.out.println("[privado pra " + targetNickname + "] " + text);
    }

    private static void broadcast(PeerTable table, String line) {
        byte[] payload = line.getBytes(StandardCharsets.UTF_8);
        if (!withinSizeLimit(payload)) {
            return;
        }

        for (PeerConnection peer : table.all()) {
            peer.send(payload);
        }
    }

    private static boolean withinSizeLimit(byte[] payload) {
        if (payload.length > Frames.MAX_MESSAGE_SIZE) {
            System.out.println("[mensagem não enviada: " + payload.length + " bytes excede o limite de " + Frames.MAX_MESSAGE_SIZE + " bytes]");
            return false;
        }
        return true;
    }
}
