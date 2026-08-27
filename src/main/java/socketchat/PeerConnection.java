package socketchat;

import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;

public class PeerConnection {

    private static final int HANDSHAKE_TIMEOUT_MS = 5_000;
    public static final int READ_TIMEOUT_MS = 20_000;
    private static final long WRITE_TIMEOUT_MS = 5_000;

    public final String nickname;
    public final Socket socket;
    public final DataInputStream in;
    public final DataOutputStream out;

    private PeerConnection(String nickname, Socket socket, DataInputStream in, DataOutputStream out) {
        this.nickname = nickname;
        this.socket = socket;
        this.in = in;
        this.out = out;
    }

    /** Troca apelidos com quem está do outro lado antes de mais nada, pra saber quem é quem. */
    public static PeerConnection handshake(Socket socket, String myNickname) throws IOException {
        socket.setSoTimeout(HANDSHAKE_TIMEOUT_MS);

        DataInputStream in = new DataInputStream(socket.getInputStream());
        DataOutputStream out = new DataOutputStream(socket.getOutputStream());

        byte[] myNicknameBytes = myNickname.getBytes(StandardCharsets.UTF_8);
        if (writeWithTimeout(socket, out, myNicknameBytes, HANDSHAKE_TIMEOUT_MS)) {
            throw new IOException("Tempo esgotado mandando o apelido no handshake.");
        }

        byte[] frame = Frames.read(in);
        if (frame == null) {
            throw new IOException("O par desconectou durante o handshake.");
        }

        String peerNickname = new String(frame, StandardCharsets.UTF_8);

        // handshake passou; a partir daqui o prazo de leitura é o da conversa em si, bem mais folgado
        socket.setSoTimeout(READ_TIMEOUT_MS);
        return new PeerConnection(peerNickname, socket, in, out);
    }

    /**
     * Manda um frame pra esse par com prazo. Se travar (par parou de consumir), fecha a conexão
     * em vez de deixar quem está mandando mensagem travado esperando — a receiveLoop desse par
     * percebe a queda e cuida da remoção/anúncio normalmente, do mesmo jeito que qualquer outra
     * desconexão.
     */
    public void send(byte[] payload) {
        if (writeWithTimeout(socket, out, payload, WRITE_TIMEOUT_MS)) {
            System.out.println("[" + nickname + " não está consumindo mensagens; desconectando]");
        }
    }

    /**
     * Socket normal do Java não tem timeout de escrita embutido (só de leitura), então a escrita
     * roda numa thread à parte: se não terminar dentro do prazo, o socket é fechado à força (o
     * que também destrava a thread de escrita, já que ela vai falhar ao tentar escrever num
     * socket fechado). Retorna true se deu timeout.
     */
    private static boolean writeWithTimeout(Socket socket, DataOutputStream out, byte[] payload, long timeoutMs) {
        Thread writer = new Thread(() -> {
            try {
                Frames.write(out, payload);
            } catch (IOException ignored) {
                // a conexão já vai cair (ou já caiu) por outro caminho; nada a fazer aqui
            }
        });
        writer.setDaemon(true);
        writer.start();

        try {
            writer.join(timeoutMs);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        boolean timedOut = writer.isAlive();
        if (timedOut) {
            try {
                socket.close();
            } catch (IOException ignored) {
            }
        }
        return timedOut;
    }

    public void close() {
        try {
            socket.close();
        } catch (IOException ignored) {
        }
    }
}
